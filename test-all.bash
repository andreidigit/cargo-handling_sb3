#!/usr/bin/env bash
#
#
: ${HOST=localhost}
: ${PORT=8443}
: ${FIRST_ID=1}
: ${API_STORES="api/v1/stores"}
: ${API_ROUTES="api/v1/routes"}
: ${API_CARGOES="api/v1/cargoes"}
: ${NOT_FOUND_ID=13}

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

  failedExit $expectedHttpCode $httpCode $curlCmd
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
    failedExit $expectedHttpCode $httpCode $curlCmd
  fi
}
function failedExit() {
    local expectedHttpCode=$1
    local httpCode=$2
    local curlCmd=$3
    echo  "Test FAILED, EXPECTED HTTP Code: $expectedHttpCode, GOT: $httpCode, WILL ABORT!"
    echo  "- Failing command: $curlCmd"
    echo  "- Response Body: $RESPONSE"
    exit 1
}
function assertCurlRetryEqual(){
    local threshold=$1
    local expectedHttpCode=$2
    local curlCmd="$3 -w \"%{http_code}\""
    local expectedResponse=$4
    local jqCmd="echo \$RESPONSE | jq $5"
    local result
    local httpCode
    local n=0
    local jqResponse
    while [[ $n -le $threshold ]]
    do
      n=$((n + 1))
      result=$(eval $curlCmd)
      httpCode="${result:(-3)}"
      RESPONSE='' && (( ${#result} > 3 )) && RESPONSE="${result%???}"
      jqResponse=$(eval "$jqCmd")
      jqResponse=$(echo "$jqResponse" | xargs)

      if [[ "$httpCode" = "$expectedHttpCode" && "$jqResponse" = "$expectedResponse" ]]
      then
        echo "Test OK Equal response (HTTP Code: $httpCode, $jqResponse)"
        return 0
      else
        sleep 2
        echo -n ", retry #$n "
      fi
    done

    failedExit $expectedHttpCode $httpCode $curlCmd
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
function recreateModel() {
  local model=$1
  local id=$2
  local composite=$3

  assertCurl 202 "curl -k -X DELETE https://$HOST:$PORT/${model}/${id} -s"
  assertEqual 202 $(curl -k -X POST https://$HOST:$PORT/store -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")
  #assertEqual 202 $(curl -k -X POST -s -k https://$HOST:$PORT/store -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")

# curl -k -X POST http://localhost:8080/store -H "Content-Type: application/json" --data '{"storeId":1,"location":"location A","capacity":100, "usedCapacity":10}'
# curl -k http://localhost:8080/store/1
}
function setupTestdata() {

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

waitForService curl https://$HOST:$PORT/actuator/health

ACCESS_TOKEN=$(curl -k https://writer:secret-writer@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="general:read general:write" -s | jq .access_token -r)
echo ACCESS_TOKEN=$ACCESS_TOKEN
AUTH="-H \"Authorization: Bearer $ACCESS_TOKEN\""

#setupTestdata
echo "Data has set successfully data has set and ready for integration testing"
echo "The data has set and ready for integration testing"

echo "          ----------------"

echo "          CRUD Store tests"
echo "          send event Delete the store with id: $FIRST_ID -->"
assertCurl 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_STORES/store/$FIRST_ID"
T_DATA='{"storeId":'$FIRST_ID',"location":"location A","capacity":100, "usedCapacity":10}'
assertCurl 202 "curl $AUTH -ks -X POST https://$HOST:$PORT/$API_STORES/store -H 'Content-Type: application/json' --data '$T_DATA'"

echo "          Wait For Message Processing getting Existing Store -->"
assertCurlRetry 13 200 "curl $AUTH -ks https://$HOST:$PORT/$API_STORES/store/$FIRST_ID"
assertEqual $FIRST_ID $(echo $RESPONSE | jq .storeId)

echo "          Store is Not found 404 -->"
assertCurl 404 "curl $AUTH -ks https://$HOST:$PORT/$API_STORES/store/$NOT_FOUND_ID"
echo $RESPONSE | jq -r '.message'
assertEqual "No store found for storeId: $NOT_FOUND_ID" "$(echo $RESPONSE | jq -r .message)"

echo "          send event Delete the store with used capacity then try to get and it's still there -->"
assertCurl 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_STORES/store/$FIRST_ID"
assertCurlRetry 3 200 "curl $AUTH -ks https://$HOST:$PORT/$API_STORES/store/$FIRST_ID"

echo "          send event Update the store to reduce used capacity to 0 -->"
T_DATA='{"storeId":1,"location":"location A","capacity":100, "usedCapacity":0}'
assertCurl 202 "curl $AUTH -ks -X POST https://$HOST:$PORT/$API_STORES/store/update -H 'Content-Type: application/json' --data '$T_DATA'"

echo "          send event Delete the store with used capacity = 0 -->"
assertCurlRetry 3 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_STORES/store/$FIRST_ID"

echo "          Store has deleted and it is Not found 404 -->"
assertCurlRetry 3 404 "curl $AUTH -ks https://$HOST:$PORT/$API_STORES/store/$FIRST_ID"

echo "          ----------------"

echo "          CRUD Route tests"
echo "          send event Delete the route with id: $FIRST_ID -->"
assertCurl 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_ROUTES/route/$FIRST_ID"
T_DATA='{"routeId":'$FIRST_ID',"fromStoreId":1,"toStoreId":2,"pathFromTo":"pathFromTo","distanceFromTo":"220","minutesFromTo":"22"}'
assertCurl 202 "curl $AUTH -ks -X POST https://$HOST:$PORT/$API_ROUTES/route -H 'Content-Type: application/json' --data '$T_DATA'"

echo "          Get created route -->"
assertCurlRetry 3 200 "curl $AUTH -ks https://$HOST:$PORT/$API_ROUTES/route/$FIRST_ID"
assertEqual $FIRST_ID $(echo $RESPONSE | jq .routeId)

echo "          Route is Not found 404 -->"
assertCurl 404 "curl $AUTH -ks https://$HOST:$PORT/$API_ROUTES/route/$NOT_FOUND_ID"
echo $RESPONSE | jq -r '.message'
assertEqual "No route found for routeId: $NOT_FOUND_ID" "$(echo $RESPONSE | jq -r .message)"

echo "          send event Update the route path -->"
T_DATA='{"routeId":'$FIRST_ID',"fromStoreId":1,"toStoreId":2,"pathFromTo":"NEW Path to","distanceFromTo":"220","minutesFromTo":"22"}'
assertCurl 202 "curl $AUTH -ks -X POST https://$HOST:$PORT/$API_ROUTES/route/update -H 'Content-Type: application/json' --data '$T_DATA'"
echo "          Get updated route -->"
assertCurlRetryEqual 3 200 "curl $AUTH -ks https://$HOST:$PORT/$API_ROUTES/route/$FIRST_ID" "NEW Path to" ".pathFromTo"

echo "          send event Delete the route -->"
assertCurlRetry 3 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_ROUTES/route/$FIRST_ID"

echo "          Route has deleted and it is Not found 404 -->"
assertCurlRetry 3 404 "curl $AUTH -ks https://$HOST:$PORT/$API_ROUTES/route/$FIRST_ID"

echo "          ----------------"

echo "          CRUD Cargo tests"
echo "          send event Delete the cargo with id: $FIRST_ID -->"
assertCurl 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_CARGOES/cargo/$FIRST_ID"
T_DATA='{"cargoId":'$FIRST_ID',"name":"cookies","weight":20,"status":"STOCK"}'
assertCurl 202 "curl $AUTH -ks -X POST https://$HOST:$PORT/$API_CARGOES/cargo -H 'Content-Type: application/json' --data '$T_DATA'"

echo "          Get created cargo -->"
assertCurlRetry 3 200 "curl $AUTH -ks https://$HOST:$PORT/$API_CARGOES/cargo/$FIRST_ID"
assertEqual $FIRST_ID $(echo $RESPONSE | jq .cargoId)

echo "          Cargo is Not found 404 -->"
assertCurl 404 "curl $AUTH -ks https://$HOST:$PORT/$API_CARGOES/cargo/$NOT_FOUND_ID"
echo $RESPONSE | jq -r '.message'
assertEqual "No cargo found for cargoId: $NOT_FOUND_ID" "$(echo $RESPONSE | jq -r .message)"

echo "          send event Update the cargo name -->"
T_DATA='{"cargoId":'$FIRST_ID',"name":"nice cookies","weight":20,"status":"STOCK"}'
assertCurl 202 "curl $AUTH -ks -X POST https://$HOST:$PORT/$API_CARGOES/cargo/update -H 'Content-Type: application/json' --data '$T_DATA'"
echo "          Get updated cargo -->"
assertCurlRetryEqual 3 200 "curl $AUTH -ks https://$HOST:$PORT/$API_CARGOES/cargo/$FIRST_ID" "nice cookies" ".name"

echo "          send event Delete the cargo -->"
assertCurlRetry 3 202 "curl $AUTH -ks -X DELETE https://$HOST:$PORT/$API_CARGOES/cargo/$FIRST_ID"

echo "          Cargo has deleted and it is Not found 404 -->"
assertCurlRetry 3 404 "curl $AUTH -ks https://$HOST:$PORT/$API_CARGOES/cargo/$FIRST_ID"

#
echo "          ----------------"

echo "          Verify access to Swagger and OpenAPI URLs -->"
echo "          1) get redirect to html 2) redirected to html page"
assertCurl 302 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ksL https://$HOST:$PORT/openapi/swagger-ui.html"
echo "          1) access to swagger-config 2) to api-docs"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/webjars/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs"
echo "          OpenAPI version"
assertEqual "3.0.1" "$(echo $RESPONSE | jq -r .openapi)"
echo "          api-docs.yaml file"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs.yaml"

echo "="
echo "="
echo "-->       Tests are completed: $(date +'%Y-%m-%d %H:%M')"

if [[ $@ == *"stop"* ]]
then
    echo "Stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi
