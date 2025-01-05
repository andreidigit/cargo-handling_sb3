package com.example.microservices.direct;

import com.example.microservices.direct.util.ReaderProducedMessages;
import com.example.mutual.api.core.cargo.Cargo;
import com.example.mutual.api.event.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        classes = {TestSecurityConfig.class},
        properties = {
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "spring.main.allow-bean-definition-overriding=true"
        })
@Import({TestChannelBinderConfiguration.class})
class CargoMessagingTests {

    @Autowired
    private WebTestClient client;
    private final ReaderProducedMessages readerCargoChanges;
    private final ObjectMapper objectMapper;

    private final String appApi = "/api/v1/cargoes";

    @Autowired
    public CargoMessagingTests(OutputDestination target, ObjectMapper objectMapper) {
        this.readerCargoChanges = new ReaderProducedMessages(target, "cargo-crud");
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        readerCargoChanges.purgeMessages();
    }


    @Test
    void createCargo() {
        Cargo cargo = new Cargo(1, "Cargo name", 10, Cargo.Status.STOCK, null);

        postAndVerifyCargo(cargo, ACCEPTED);
        final List<String> messages = readerCargoChanges.getMessages();

        // Assert one expected new cargo event queued up
        assertEquals(1, messages.size());

        Event<Integer, Cargo> expectedEvent = new Event<>(
                CREATE,
                cargo.getCargoId(),
                new Cargo(cargo.getCargoId(), cargo.getName(), cargo.getWeight(), cargo.getStatus(), cargo.getServiceAddress())
        );
        assertThat(messages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void updateCargo() throws JsonProcessingException {
        Cargo cargo = new Cargo(1, "Cargo name", 11, Cargo.Status.STOCK, null);
        postUpdateAndVerifyCargo(cargo, ACCEPTED);
        final List<String> messages = readerCargoChanges.getMessages();

        // Assert one expected new cargo event queued up
        assertEquals(1, messages.size());
        Event<Integer, Cargo> eventCargo = objectMapper.readValue(messages.get(0), new TypeReference<>() {});
        Cargo payload = eventCargo.getData();
        assertEquals(11, payload.getWeight());

        Event<Integer, Cargo> expectedEvent = new Event<>(
                UPDATE,
                cargo.getCargoId(),
                new Cargo(cargo.getCargoId(), cargo.getName(), cargo.getWeight(), cargo.getStatus(), cargo.getServiceAddress())
        );
        assertThat(messages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void deleteCargo() {
        deleteAndVerifyCargo(1, ACCEPTED);
        final List<String> messages = readerCargoChanges.getMessages();

        // Assert one expected new cargo event queued up
        assertEquals(1, messages.size());

        Event<Integer, Cargo> expectedEvent = new Event<>(
                DELETE,
                1,
                null
        );
        assertThat(messages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    private void postAndVerifyCargo(Cargo cargo, HttpStatus expectedStatus) {
        client.post()
                .uri(appApi + "/cargo")
                .body(just(cargo), Cargo.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void postUpdateAndVerifyCargo(Cargo cargo, HttpStatus expectedStatus) {
        client.post()
                .uri(appApi + "/cargo/update")
                .body(just(cargo), Cargo.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyCargo(int cargoId, HttpStatus expectedStatus) {
        client.delete()
                .uri(appApi + "/cargo/" + cargoId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}
