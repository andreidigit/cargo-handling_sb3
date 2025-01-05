package com.example.microservices.core.cargo;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.microservices.core.cargo.persistence.CargoRepository;
import com.example.microservices.core.cargo.services.CargoMapper;
import com.example.microservices.core.cargo.util.ContainersTestBase;
import com.example.microservices.core.cargo.util.ReaderProducedMessages;
import com.example.mutual.api.core.cargo.Cargo;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
class CargoServiceTest extends ContainersTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private CargoRepository repository;
    private final ReaderProducedMessages readerCargoRevise;
    private final CargoMapper mapper;


    @Autowired
    public CargoServiceTest(
            OutputDestination target,
            CargoMapper mapper
    ) {
        this.readerCargoRevise = new ReaderProducedMessages(target, "cargo-revise");
        this.mapper = mapper;
    }

    @Autowired
    @Qualifier("consumerCrud")
    private Consumer<Event<Integer, Cargo>> consumerCrud;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
        readerCargoRevise.purgeMessages();
    }

    @Test
    void getCargoByCargoId() {
        int cargoId = 1;
        assertTrue(repository.findByCargoId(cargoId).isEmpty());
        assertEquals(0, repository.count());

        sendCreateCargoEvent(cargoId);

        assertNotNull(repository.findByCargoId(cargoId));
        assertEquals(1, repository.count());

        getAndVerifyCargo(cargoId, OK)
                .jsonPath("$.cargoId").isEqualTo(cargoId);
    }

    @Test
    void getCargoNotFound() {
        int cargoIdNotFound = 13;
        getAndVerifyCargo(cargoIdNotFound, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/cargo/" + cargoIdNotFound)
                .jsonPath("$.message").value(matchesPattern("^No cargo found for cargoId:.*"));
    }

    @Test
    void getCargoInvalidId() {
        int cargoIdInvalid = -1;
        getAndVerifyCargo(cargoIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/cargo/" + cargoIdInvalid)
                .jsonPath("$.message").value(matchesPattern("^Invalid cargoId.*"));
    }

    @Test
    void getCargoInvalidParameter() {
        getAndVerifyCargo("/string", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/cargo/string")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    void duplicateError() {
        int cargoId = 1;
        assertTrue(repository.findByCargoId(cargoId).isEmpty());
        sendCreateCargoEvent(cargoId);
        assertTrue(repository.findByCargoId(cargoId).isPresent());

        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendCreateCargoEvent(cargoId),
                "Expected an InvalidInputException here!"
        );
        assertEquals("Duplicate key, Cargo Id: " + cargoId, thrown.getMessage());
    }

    @Test
    void updateCargoOk() {
        readerCargoRevise.purgeMessages();
        assertEquals(0, readerCargoRevise.getMessages().size());
        int cargoId = 1;
        sendCreateCargoEvent(cargoId);                          // 1. Event CREATE

        assertEquals(1, repository.count());
        CargoEntity entity = repository.findByCargoId(cargoId).get();

        String name = "donat";
        entity.setName(name);
        sendUpdateCargoEvent(mapper.entityToApi(entity));      // 2. Event UPDATE

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            Optional<CargoEntity> cargoOpt = repository.findByCargoId(cargoId);
            assertTrue(cargoOpt.isPresent());
            assertEquals(cargoOpt.get().getName(), name);
        });

        getAndVerifyCargo(cargoId, OK)
                .jsonPath("$.name").isEqualTo(name);
        assertEquals(2, readerCargoRevise.getMessages().size());
    }

    @Test
    void updateCargoBad() {
        readerCargoRevise.purgeMessages();
        assertEquals(0, readerCargoRevise.getMessages().size());
        int cargoId = 1;
        sendCreateCargoEventForBad(cargoId);                            // 1. Event CREATE

        assertEquals(1, repository.count());
        CargoEntity entity = repository.findByCargoId(cargoId).get();
        entity.setWeight(501);

        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendUpdateCargoEvent(mapper.entityToApi(entity)),     // 2. UPDATE Event is escaped
                "Expected an InvalidInputException here!"
        );
        assertEquals("There is a broken Update Rule, Cargo Id: " + cargoId, thrown.getMessage());

        assertEquals(1, readerCargoRevise.getMessages().size());
    }

    @Test
    void deleteCargoOk() {
        readerCargoRevise.purgeMessages();
        int cargoId = 1;
        sendCreateCargoEvent(cargoId);

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByCargoId(cargoId).isPresent());
        });

        sendDeleteCargoEvent(cargoId);
        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByCargoId(cargoId).isEmpty());
        });

        assertEquals(2, readerCargoRevise.getMessages().size());
    }
    @Test
    void deleteCargoBadRule() {
        readerCargoRevise.purgeMessages();
        int cargoId = 2;
        sendCreateCargoEventForBad(cargoId);                  // 1. Create Event

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByCargoId(cargoId).isPresent());
        });
        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendDeleteCargoEvent(cargoId),                // 2. Delete Event is escaped
                "Expected an InvalidInputException here!"
        );
        assertEquals("There is a broken Delete Rule, cargo Id: " + cargoId, thrown.getMessage());

        assertEquals(1, readerCargoRevise.getMessages().size());
    }

    private WebTestClient.BodyContentSpec getAndVerifyCargo(int cargoId, HttpStatus expectedStatus) {
        return getAndVerifyCargo("/" + cargoId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyCargo(String cargoIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/cargo" + cargoIdPath)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateCargoEvent(int cargoId) {
        Cargo cargo = new Cargo(cargoId, "cookies", 10, Cargo.Status.STOCK, "SA");
        Event<Integer, Cargo> event = new Event<>(CREATE, cargoId, cargo);
        consumerCrud.accept(event);
    }
    private void sendCreateCargoEventForBad(int cargoId) {
        Cargo cargo = new Cargo(cargoId, "bad cookies", 10, Cargo.Status.TRANSIT, "SA");
        Event<Integer, Cargo> event = new Event<>(CREATE, cargoId, cargo);
        consumerCrud.accept(event);
    }

    private void sendUpdateCargoEvent(Cargo cargo) {
        Event<Integer, Cargo> event = new Event<>(UPDATE, cargo.getCargoId(), cargo);
        consumerCrud.accept(event);
    }

    private void sendDeleteCargoEvent(int cargoId) {
        Event<Integer, Cargo> event = new Event<>(DELETE, cargoId, null);
        consumerCrud.accept(event);
    }
}
