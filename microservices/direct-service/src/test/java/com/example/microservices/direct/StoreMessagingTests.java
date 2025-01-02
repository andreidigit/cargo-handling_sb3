package com.example.microservices.direct;

import com.example.microservices.direct.util.ReaderProducedMessages;
import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static com.example.microservices.direct.util.IsSameEvent.sameEventExceptCreatedAt;
import static com.example.mutual.api.event.Event.Type.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static reactor.core.publisher.Mono.just;

@Slf4j
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
class StoreMessagingTests {

    @Autowired
    private WebTestClient client;
    private final ReaderProducedMessages readerStoreChanges;

    @Autowired
    public StoreMessagingTests(
            @Value("${channelsOut.store.topic}") String topicName,
            OutputDestination target
    ) {
        this.readerStoreChanges = new ReaderProducedMessages(target, topicName);
    }

    @BeforeEach
    void setUp() {
        readerStoreChanges.purgeMessages();
    }


    @Test
    void createStore() {
        Store store = new Store(1, "location", 10, 1, null);
        postAndVerifyStore(store, ACCEPTED);
        final List<String> storeMessages = readerStoreChanges.getMessages();

        // Assert one expected new store event queued up
        assertEquals(1, storeMessages.size());

        Event<Integer, Store> expectedEvent = new Event<>(
                CREATE,
                store.getStoreId(),
                new Store(store.getStoreId(), store.getLocation(), store.getCapacity(), store.getUsedCapacity(), null)
        );
        assertThat(storeMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void updateStore() {
        Store store = new Store(1, "location", 10, 1, null);
        postUpdateAndVerifyStore(store, ACCEPTED);
        final List<String> storeMessages = readerStoreChanges.getMessages();

        // Assert one expected new store event queued up
        assertEquals(1, storeMessages.size());

        Event<Integer, Store> expectedEvent = new Event<>(
                UPDATE,
                store.getStoreId(),
                new Store(store.getStoreId(), store.getLocation(), store.getCapacity(), store.getUsedCapacity(), null)
        );
        assertThat(storeMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void deleteStore() {
        deleteAndVerifyStore(1, ACCEPTED);
        final List<String> storeMessages = readerStoreChanges.getMessages();

        // Assert one expected new store event queued up
        assertEquals(1, storeMessages.size());

        Event<Integer, Store> expectedEvent = new Event<>(
                DELETE,
                1,
                null
        );
        assertThat(storeMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    private void postAndVerifyStore(Store store, HttpStatus expectedStatus) {
        client.post()
                .uri("/store")
                .body(just(store), Store.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void postUpdateAndVerifyStore(Store store, HttpStatus expectedStatus) {
        client.post()
                .uri("/store/update")
                .body(just(store), Store.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyStore(int storeId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/store/" + storeId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}
