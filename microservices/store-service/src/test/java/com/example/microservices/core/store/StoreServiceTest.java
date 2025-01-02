package com.example.microservices.core.store;

import com.example.microservices.core.store.persistence.StoreEntity;
import com.example.microservices.core.store.persistence.StoreRepository;
import com.example.microservices.core.store.services.StoreMapper;
import com.example.microservices.core.store.util.ContainersTestBase;
import com.example.microservices.core.store.util.ReaderProducedMessages;
import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import static com.example.mutual.api.event.Event.Type.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
class StoreServiceTest extends ContainersTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private StoreRepository repository;
    private final ReaderProducedMessages readerStoreRevise;
    private final StoreMapper mapper;


    @Autowired
    public StoreServiceTest(
            @Value("${channelsOut.revise.topic}") String topicName,
            OutputDestination target,
            StoreMapper mapper
    ) {
        this.readerStoreRevise = new ReaderProducedMessages(target, topicName);
        this.mapper = mapper;
    }

    @Autowired
    @Qualifier("consumerCrud")
    private Consumer<Event<Integer, Store>> consumerCrud;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
        readerStoreRevise.purgeMessages();
    }

    @Test
    void getStoreByStoreId() {
        int storeId = 1;
        assertTrue(repository.findByStoreId(storeId).isEmpty());
        assertEquals(0, repository.count());

        sendCreateStoreEvent(storeId);

        assertNotNull(repository.findByStoreId(storeId));
        assertEquals(1, repository.count());

        getAndVerifyStore(storeId, OK)
                .jsonPath("$.storeId").isEqualTo(storeId);
    }

    @Test
    void getStoreNotFound() {
        int storeIdNotFound = 13;
        getAndVerifyStore(storeIdNotFound, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/store/" + storeIdNotFound)
                .jsonPath("$.message").value(matchesPattern("^No store found for storeId:.*"));
    }

    @Test
    void getProductInvalidId() {
        int storeIdInvalid = -1;
        getAndVerifyStore(storeIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/store/" + storeIdInvalid)
                .jsonPath("$.message").value(matchesPattern("^Invalid storeId.*"));
    }

    @Test
    void getProductInvalidParameter() {
        getAndVerifyStore("/string", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/store/string")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    void duplicateError() {
        int storeId = 1;
        assertTrue(repository.findByStoreId(storeId).isEmpty());
        sendCreateStoreEvent(storeId);
        assertTrue(repository.findByStoreId(storeId).isPresent());

        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendCreateStoreEvent(storeId),
                "Expected an InvalidInputException here!"
        );
        assertEquals("Duplicate key, Store Id: " + storeId, thrown.getMessage());
    }

    @Test
    void updateStoreOk() {
        readerStoreRevise.purgeMessages();
        assertEquals(0, readerStoreRevise.getMessages().size());
        int storeId = 1;
        sendCreateStoreEvent(storeId);                          // 1. Event CREATE

        assertEquals(1, repository.count());
        StoreEntity entity = repository.findByStoreId(storeId).get();

        String location = "a better place";
        entity.setLocation(location);
        sendUpdateStoreEvent(mapper.entityToApi(entity));      // 2. Event UPDATE

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            Optional<StoreEntity> storeOpt = repository.findByStoreId(storeId);
            assertTrue(storeOpt.isPresent());
            assertEquals(storeOpt.get().getLocation(), location);
        });

        getAndVerifyStore(storeId, OK)
                .jsonPath("$.location").isEqualTo(location);
        assertEquals(2, readerStoreRevise.getMessages().size());
    }

    @Test
    void updateStoreBad() {
        readerStoreRevise.purgeMessages();
        assertEquals(0, readerStoreRevise.getMessages().size());
        int storeId = 1;
        sendCreateStoreEventForBad(storeId);                            // 1. Event CREATE

        assertEquals(1, repository.count());
        StoreEntity entity = repository.findByStoreId(storeId).get();
        entity.setCapacity(9);

        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendUpdateStoreEvent(mapper.entityToApi(entity)),     // 2. UPDATE Event is escaped
                "Expected an InvalidInputException here!"
        );
        assertEquals("There is a broken Update Rule, Store Id: " + storeId, thrown.getMessage());

        assertEquals(1, readerStoreRevise.getMessages().size());
    }

    @Test
    void deleteStoreOk() {
        readerStoreRevise.purgeMessages();
        int storeId = 1;
        sendCreateStoreEvent(storeId);

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByStoreId(storeId).isPresent());
        });

        sendDeleteStoreEvent(storeId);
        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByStoreId(storeId).isEmpty());
        });

        assertEquals(2, readerStoreRevise.getMessages().size());
    }
    @Test
    void deleteStoreBadRule() {
        readerStoreRevise.purgeMessages();
        int storeId = 2;
        sendCreateStoreEventForBad(storeId);                  // 1. Create Event

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByStoreId(storeId).isPresent());
        });
        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendDeleteStoreEvent(storeId),                // 2. Delete Event is escaped
                "Expected an InvalidInputException here!"
        );
        assertEquals("There is a broken Delete Rule, store Id: " + storeId, thrown.getMessage());

        assertEquals(1, readerStoreRevise.getMessages().size());
    }

    private WebTestClient.BodyContentSpec getAndVerifyStore(int storeId, HttpStatus expectedStatus) {
        return getAndVerifyStore("/" + storeId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyStore(String storeIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/store" + storeIdPath)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateStoreEvent(int storeId) {
        Store store = new Store(storeId, "dom 33 ul Centralnaya, gorod Minsk, Minsk region", 1000, 0, "SA");
        Event<Integer, Store> event = new Event<>(CREATE, storeId, store);
        consumerCrud.accept(event);
    }
    private void sendCreateStoreEventForBad(int storeId) {
        Store store = new Store(storeId, "dom 33 ul Centralnaya, gorod Minsk, Minsk region", 1000, 10, "SA");
        Event<Integer, Store> event = new Event<>(CREATE, storeId, store);
        consumerCrud.accept(event);
    }

    private void sendUpdateStoreEvent(Store store) {
        Event<Integer, Store> event = new Event<>(UPDATE, store.getStoreId(), store);
        consumerCrud.accept(event);
    }

    private void sendDeleteStoreEvent(int storeId) {
        Event<Integer, Store> event = new Event<>(DELETE, storeId, null);
        consumerCrud.accept(event);
    }
}
