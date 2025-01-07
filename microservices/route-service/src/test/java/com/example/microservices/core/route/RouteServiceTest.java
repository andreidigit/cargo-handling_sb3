package com.example.microservices.core.route;

import com.example.microservices.core.route.broker.ConsumersConfig;
import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.persistence.RouteRepository;
import com.example.microservices.core.route.services.RouteMapper;
import com.example.microservices.core.route.util.ContainersTestBase;
import com.example.microservices.core.route.util.ReaderProducedMessages;
import com.example.mutual.api.core.route.Route;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
@Import({TestChannelBinderConfiguration.class, ConsumersConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RouteServiceTest extends ContainersTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private RouteRepository repository;
    private final ReaderProducedMessages readerRouteChanges;
    private final RouteMapper mapper;

    @Autowired
    public RouteServiceTest(OutputDestination target, RouteMapper mapper) {
        this.readerRouteChanges = new ReaderProducedMessages(target, "route-revise");
        this.mapper = mapper;
    }

    @Autowired
    private Consumer<Event<Integer, Route>> consumerCrudFunc;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
        readerRouteChanges.purgeMessages();
    }

    @Test
    void getRouteByRouteId() {
        // сущности нет
        int routeId = 1;
        assertTrue(repository.findByRouteId(routeId).isEmpty());
        assertEquals(0, repository.count());
        // отправим событие на создание
        sendCreateRouteEvent(routeId);
        //сущность создана
        assertNotNull(repository.findByRouteId(routeId));
        assertEquals(1, repository.count());
        // с тем айди
        getAndVerifyRoute(routeId, OK)
                .jsonPath("$.routeId").isEqualTo(routeId);
    }

    @Test
    void getRouteNotFound() {
        int routeIdNotFound = 13;
        getAndVerifyRoute(routeIdNotFound, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/route/" + routeIdNotFound)
                .jsonPath("$.message").value(matchesPattern("^No route found for routeId:.*"));
    }

    @Test
    void getRouteInvalidId() {
        int routeIdInvalid = -1;
        getAndVerifyRoute(routeIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/route/" + routeIdInvalid)
                .jsonPath("$.message").value(matchesPattern("^Invalid routeId.*"));
    }

    @Test
    void getRouteInvalidParameter() {
        // отправляем не правильный запрос вместо номера строку
        getAndVerifyRoute("/string", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/route/string")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    void duplicateError() {
        // создаем сущность
        int routeId = 2;
        assertTrue(repository.findByRouteId(routeId).isEmpty());
        sendCreateRouteEvent(routeId);
        assertTrue(repository.findByRouteId(routeId).isPresent());
        // и пытаемся создать сущность с тем же айди
        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendCreateRouteEvent(routeId),
                "Expected an InvalidInputException here!"
        );
        assertEquals("Duplicate key, Route Id: " + routeId, thrown.getMessage());
    }

    @Test
    void updateRoute() {
        readerRouteChanges.purgeMessages();
        assertEquals(0, readerRouteChanges.getMessages().size());
        int routeId = 3;
        sendCreateRouteEvent(routeId);                          // 1. Event CREATE

        assertEquals(1, repository.count());
        RouteEntity entity = repository.findByRouteId(routeId).get();
        // обновим путь от одного склада к другому
        String newPath = "прямо налево и за аптекой направо";
        entity.setPathFromTo(newPath);
        sendUpdateRouteEvent(mapper.entityToApi(entity));      // 2. Event UPDATE
        // на обработчике сообщений на моно объекте используется block() и await() можно не использовать
        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            Optional<RouteEntity> routeOpt = repository.findByRouteId(routeId);
            assertTrue(routeOpt.isPresent());
            assertEquals(routeOpt.get().getPathFromTo(), newPath);
        });

        getAndVerifyRoute(routeId, OK)
                .jsonPath("$.pathFromTo").isEqualTo(newPath);
        assertEquals(2, readerRouteChanges.getMessages().size());
    }

    @Test
    void deleteRoute() {
        // событие на создание
        readerRouteChanges.purgeMessages();
        int routeId = 4;
        sendCreateRouteEvent(routeId);
        // ждем подтверждения о добавлении в БД
        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByRouteId(routeId).isPresent());
        });
        // событие на удаление и ждем что выполнится
        sendDeleteRouteEvent(routeId);
        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByRouteId(routeId).isEmpty());
        });

        assertEquals(2, readerRouteChanges.getMessages().size());
    }

    private WebTestClient.BodyContentSpec getAndVerifyRoute(int routeId, HttpStatus expectedStatus) {
        return getAndVerifyRoute("/" + routeId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRoute(String routeIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/route" + routeIdPath)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateRouteEvent(int routeId) {
        Route route = new Route(routeId, 10, 11, "ехай прямо и на право", 100, 10);
        Event<Integer, Route> event = new Event<>(CREATE, routeId, route);
        consumerCrudFunc.accept(event);
    }

    private void sendUpdateRouteEvent(Route route) {
        Event<Integer, Route> event = new Event<>(UPDATE, route.getRouteId(), route);
        consumerCrudFunc.accept(event);
    }

    private void sendDeleteRouteEvent(int routeId) {
        Event<Integer, Route> event = new Event<>(DELETE, routeId, null);
        consumerCrudFunc.accept(event);
    }
}
