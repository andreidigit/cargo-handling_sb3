#!/usr/bin/env bash
#
#
: ${HOST=localhost}
: ${PORT=8080}
: ${FIRST_ID=1}
: ${NOT_FOUND_ID=13}
#
: ${PROD_ID_REVIEWS_RECOMMENDATIONS=1}
: ${PROD_ID_NO_RECOMMENDATIONS=113}
: ${PROD_ID_NO_REVIEWS=213}

function assertCurlRetry() {

  local threshold=$1
  local expectedHttpCode=$2
  local curlCmd="$3 -w \"%{http_code}\""
  local result
  local httpCode
  local n=0

  while [[ $n -le $threshold ]]
  do
    n=$((n + 1))
    result=$(eval $curlCmd)
    httpCode="${result:(-3)}"
    RESPONSE='' && (( ${#result} > 3 )) && RESPONSE="${result%???}"

    if [ "$httpCode" = "$expectedHttpCode" ]
    then
      if [ "$httpCode" = "200" ]
      then
        echo "Test OK (HTTP Code: $httpCode)"
      else
        echo "Test OK (HTTP Code: $httpCode, $RESPONSE)"
      fi
      return 0
    else
      sleep 2
      echo -n ", retry #$n "
    fi
  done

  echo  "Test FAILED, EXPECTED HTTP Code: $expectedHttpCode, GOT: $httpCode, WILL ABORT!"
  echo  "- Failing command: $curlCmd"
  echo  "- Response Body: $RESPONSE"
  exit 1
}

function assertCurl() {

  local expectedHttpCode=$1
  local curlCmd="$2 -w \"%{http_code}\""
  local result=$(eval $curlCmd)
  local httpCode="${result:(-3)}"
  RESPONSE='' && (( ${#result} > 3 )) && RESPONSE="${result%???}"

  if [ "$httpCode" = "$expectedHttpCode" ]
  then
    if [ "$httpCode" = "200" ]
    then
      echo "Test OK (HTTP Code: $httpCode)"
    else
      echo "Test OK (HTTP Code: $httpCode, $RESPONSE)"
    fi
  else
    echo  "Test FAILED, EXPECTED HTTP Code: $expectedHttpCode, GOT: $httpCode, WILL ABORT!"
    echo  "- Failing command: $curlCmd"
    echo  "- Response Body: $RESPONSE"
    exit 1
  fi
}

function assertEqual() {

  local expected=$1
  local actual=$2

  if [ "$actual" = "$expected" ]
  then
    echo "Test OK (actual value: $actual)"
  else
    echo "Test FAILED, EXPECTED VALUE: $expected, ACTUAL VALUE: $actual, WILL ABORT"
    exit 1
  fi
}

function testUrl() {
  url=$@
  if curl $url -ks -f -o /dev/null
  then
    return 0
  else
    return 1
  fi;
}

function waitForService() {
  url=$@
  echo -n "Wait for: $url... "
  n=0
  until testUrl $url
  do
    n=$((n + 1))
    if [[ $n == 100 ]]
    then
      echo " Give up"
      exit 1
    else
      sleep 3
      echo -n ", retry #$n "
    fi
  done
  echo "DONE, continues..."
}

function testStoreCreated() {

    # Expect that the Store for storeId $FIRST_ID has been created
    if ! assertCurl 200 "curl http://$HOST:$PORT/store/$FIRST_ID -s"
    then
        echo -n "FAIL"
        return 1
    fi

    set +e
    assertEqual "$FIRST_ID" $(echo $RESPONSE | jq .storeId)
    if [ "$?" -eq "1" ] ; then return 1; fi

    set -e
}

function waitForMessageProcessing() {
    echo "Wait for messages to be processed... "

    # Give background processing some time to complete...
    sleep 1

    n=0
    until testStoreCreated
    do
        n=$((n + 1))
        if [[ $n == 40 ]]
        then
            echo " Give up"
            exit 1
        else

            sleep 6
            echo -n ", retry #$n "
        fi
    done
    echo "All messages are now processed!"
}

function recreateStores() {
  local storeId=$1
  local composite=$2

  assertCurl 202 "curl -X DELETE http://$HOST:$PORT/store/${storeId} -s"
  assertEqual 202 $(curl -X POST http://$HOST:$PORT/store -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")
  #assertEqual 202 $(curl -X POST -s -k https://$HOST:$PORT/store -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")

# curl -X POST http://localhost:8080/store -H "Content-Type: application/json" --data '{"storeId":1,"location":"location A","capacity":100, "usedCapacity":10}'
# curl http://localhost:8080/store/1
}

function recreateModel() {
  local model=$1
  local id=$2
  local composite=$3

  assertCurl 202 "curl -X DELETE http://$HOST:$PORT/${model}/${id} -s"
  assertEqual 202 $(curl -X POST http://$HOST:$PORT/store -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")
  #assertEqual 202 $(curl -X POST -s -k https://$HOST:$PORT/store -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")

# curl -X POST http://localhost:8080/store -H "Content-Type: application/json" --data '{"storeId":1,"location":"location A","capacity":100, "usedCapacity":10}'
# curl http://localhost:8080/store/1
}

function setupTestdata() {

  body="{\"storeId\":$FIRST_ID"
  body+=\
',"location":"location A","capacity":100,"usedCapacity":10}'

  recreateStores "$FIRST_ID" "$body"

  body="{\"storeId\":$FIRST_ID"
  body+=\
',"location":"location A","capacity":100,"usedCapacity":10}'

  recreateModel "store" "$FIRST_ID" "$body"
}

set -e

echo "Start Tests: $(date +'%Y-%m-%d %H:%M')    -->"

echo "HOST=${HOST}"
echo "PORT=${PORT}"

if [[ $@ == *"start"* ]]
then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  docker compose down --remove-orphans
  echo "$ docker compose up -d"
  docker compose up -d
fi

waitForService curl http://$HOST:$PORT/actuator/health

setupTestdata
echo "Data has set successfully data has set and ready for integration testing"
waitForMessageProcessing
echo "The data has set and ready for integration testing"

echo "          CRUD Store tests"
echo "          Existing Store -->"
assertCurlRetry 3 200 "curl http://$HOST:$PORT/store/$FIRST_ID -s"
assertEqual $FIRST_ID $(echo $RESPONSE | jq .storeId)

echo "          Store is Not found 404 -->"
assertCurl 404 "curl http://$HOST:$PORT/store/$NOT_FOUND_ID -s"
echo $RESPONSE | jq -r '.message'
assertEqual "No store found for storeId: $NOT_FOUND_ID" "$(echo $RESPONSE | jq -r .message)"

echo "          Delete the store with used capacity then try to get and it's still there-->"
assertCurl 202 "curl -X DELETE http://$HOST:$PORT/store/$FIRST_ID -s"
assertCurlRetry 3 200 "curl http://$HOST:$PORT/store/$FIRST_ID -s"

echo "          Update the store to reduce used capacity to 0"
assertEqual 202 $(curl -X POST http://$HOST:$PORT/store/update -H "Content-Type: application/json" --data '{"storeId":1,"location":"location A","capacity":100, "usedCapacity":0}' -w "%{http_code}")

echo "          Delete the store with used capacity = 0"
assertCurlRetry 3 202 "curl -X DELETE http://$HOST:$PORT/store/$FIRST_ID -s"
echo "          Store has deleted and it is Not found 404 -->"
assertCurlRetry 3 404 "curl http://$HOST:$PORT/store/$FIRST_ID -s"


echo "="
echo "="
echo "-->       Tests are completed: $(date +'%Y-%m-%d %H:%M')"
