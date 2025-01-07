package com.example.microservices.core.order;

import com.example.microservices.core.order.persistence.OrderEntity;
import com.example.microservices.core.order.persistence.OrderRepository;
import com.example.microservices.core.order.services.OrderMapper;
import com.example.microservices.core.order.util.ContainersTestBase;
import com.example.microservices.core.order.util.ReaderProducedMessages;
import com.example.mutual.api.core.order.Order;
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
class OrderServiceTest extends ContainersTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private OrderRepository repository;
    private final ReaderProducedMessages readerOrderRevise;
    private final OrderMapper mapper;


    @Autowired
    public OrderServiceTest(
            OutputDestination target,
            OrderMapper mapper
    ) {
        this.readerOrderRevise = new ReaderProducedMessages(target, "order-revise");
        this.mapper = mapper;
    }

    @Autowired
    @Qualifier("consumerCrud")
    private Consumer<Event<Integer, Order>> consumerCrud;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
        readerOrderRevise.purgeMessages();
    }

    @Test
    void getOrderByOrderId() {
        int orderId = 1;
        assertTrue(repository.findByOrderId(orderId).isEmpty());
        assertEquals(0, repository.count());

        sendCreateOrderEvent(orderId);

        assertNotNull(repository.findByOrderId(orderId));
        assertEquals(1, repository.count());

        getAndVerifyOrder(orderId, OK)
                .jsonPath("$.orderId").isEqualTo(orderId);
    }

    @Test
    void getOrderNotFound() {
        int orderIdNotFound = 13;
        getAndVerifyOrder(orderIdNotFound, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/order/" + orderIdNotFound)
                .jsonPath("$.message").value(matchesPattern("^No order found for orderId:.*"));
    }

    @Test
    void getOrderInvalidId() {
        int orderIdInvalid = -1;
        getAndVerifyOrder(orderIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/order/" + orderIdInvalid)
                .jsonPath("$.message").value(matchesPattern("^Invalid orderId.*"));
    }

    @Test
    void getOrderInvalidParameter() {
        getAndVerifyOrder("/string", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/order/string")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    void duplicateError() {
        int orderId = 1;
        assertTrue(repository.findByOrderId(orderId).isEmpty());
        sendCreateOrderEvent(orderId);
        assertTrue(repository.findByOrderId(orderId).isPresent());

        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendCreateOrderEvent(orderId),
                "Expected an InvalidInputException here!"
        );
        assertEquals("Duplicate key, Order Id: " + orderId, thrown.getMessage());
    }

    @Test
    void updateOrderOk() {
        readerOrderRevise.purgeMessages();
        assertEquals(0, readerOrderRevise.getMessages().size());
        int orderId = 1;
        sendCreateOrderEvent(orderId);                          // 1. Event CREATE

        assertEquals(1, repository.count());
        OrderEntity entity = repository.findByOrderId(orderId).get();

        entity.setStatus(Order.Status.COMPLETED);
        sendUpdateOrderEvent(mapper.entityToApi(entity));      // 2. Event UPDATE

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            Optional<OrderEntity> orderOpt = repository.findByOrderId(orderId);
            assertTrue(orderOpt.isPresent());
            assertEquals(orderOpt.get().getStatus(), Order.Status.COMPLETED);
        });

        getAndVerifyOrder(orderId, OK)
                .jsonPath("$.status").isEqualTo("COMPLETED");
        assertEquals(2, readerOrderRevise.getMessages().size());
    }

    @Test
    void updateOrderBad() {
        readerOrderRevise.purgeMessages();
        assertEquals(0, readerOrderRevise.getMessages().size());
        int orderId = 1;
        sendCreateOrderEventForBad(orderId);                                // 1. Event CREATE

        assertEquals(1, repository.count());
        OrderEntity entity = repository.findByOrderId(orderId).get();
        entity.setFromStoreId(501);                                         // ids change is forbidden

        InvalidInputException thrown = assertThrows(
                InvalidInputException.class,
                () -> sendUpdateOrderEvent(mapper.entityToApi(entity)),     // 2. UPDATE Event is escaped
                "Expected an InvalidInputException here!"
        );
        assertEquals("There is a broken Update Rule, Order Id: " + orderId, thrown.getMessage());

        assertEquals(1, readerOrderRevise.getMessages().size());
    }

    @Test
    void deleteOrderOk() {
        readerOrderRevise.purgeMessages();
        int orderId = 1;
        sendCreateOrderEvent(orderId);

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByOrderId(orderId).isPresent());
        });

        sendDeleteOrderEvent(orderId);
        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByOrderId(orderId).isEmpty());
        });

        assertEquals(2, readerOrderRevise.getMessages().size());
    }
    @Test
    void deleteOrderBadRule() {
        readerOrderRevise.purgeMessages();
        int orderId = 2;
        sendCreateOrderEventForBad(orderId);                        // 1. Create Event

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, SECONDS).untilAsserted(() -> {
            assertTrue(repository.findByOrderId(orderId).isPresent());
        });
        InvalidInputException thrown = assertThrows(                // deleting while in TRANSIT is forbidden
                InvalidInputException.class,
                () -> sendDeleteOrderEvent(orderId),                // 2. Delete Event is escaped
                "Expected an InvalidInputException here!"
        );
        assertEquals("There is a broken Delete Rule, order Id: " + orderId, thrown.getMessage());

        assertEquals(1, readerOrderRevise.getMessages().size());
    }

    private WebTestClient.BodyContentSpec getAndVerifyOrder(int orderId, HttpStatus expectedStatus) {
        return getAndVerifyOrder("/" + orderId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyOrder(String orderIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/order" + orderIdPath)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateOrderEvent(int orderId) {
        Order order = new Order(orderId, 1, 1, 2, Order.Status.NEW, "SA");
        Event<Integer, Order> event = new Event<>(CREATE, orderId, order);
        consumerCrud.accept(event);
    }
    private void sendCreateOrderEventForBad(int orderId) {
        Order order = new Order(orderId, 1, 1, 2, Order.Status.TRANSIT, "SA");
        Event<Integer, Order> event = new Event<>(CREATE, orderId, order);
        consumerCrud.accept(event);
    }

    private void sendUpdateOrderEvent(Order order) {
        Event<Integer, Order> event = new Event<>(UPDATE, order.getOrderId(), order);
        consumerCrud.accept(event);
    }

    private void sendDeleteOrderEvent(int orderId) {
        Event<Integer, Order> event = new Event<>(DELETE, orderId, null);
        consumerCrud.accept(event);
    }
}
