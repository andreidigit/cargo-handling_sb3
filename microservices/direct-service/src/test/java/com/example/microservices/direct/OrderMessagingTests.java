package com.example.microservices.direct;

import com.example.microservices.direct.util.ReaderProducedMessages;
import com.example.mutual.api.core.order.Order;
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
class OrderMessagingTests {

    @Autowired
    private WebTestClient client;
    private final ReaderProducedMessages readerOrderChanges;
    private final ObjectMapper objectMapper;

    private final String appApi = "/api/v1/orders";

    @Autowired
    public OrderMessagingTests(OutputDestination target, ObjectMapper objectMapper) {
        this.readerOrderChanges = new ReaderProducedMessages(target, "order-crud");
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        readerOrderChanges.purgeMessages();
    }


    @Test
    void createOrder() {
        Order order = new Order(1, 1, 1, 2, Order.Status.NEW, null);

        postAndVerifyOrder(order, ACCEPTED);
        final List<String> messages = readerOrderChanges.getMessages();

        // Assert one expected new order event queued up
        assertEquals(1, messages.size());

        Event<Integer, Order> expectedEvent = new Event<>(
                CREATE,
                order.getOrderId(),
                new Order(
                        order.getOrderId(),
                        order.getCargoId(),
                        order.getFromStoreId(),
                        order.getToStoreId(),
                        order.getStatus(),
                        order.getServiceAddress()
                )
        );
        assertThat(messages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void updateOrder() throws JsonProcessingException {
        Order order = new Order(1, 1, 1, 2, Order.Status.TRANSIT, null);
        postUpdateAndVerifyOrder(order, ACCEPTED);
        final List<String> messages = readerOrderChanges.getMessages();

        // Assert one expected new order event queued up
        assertEquals(1, messages.size());
        Event<Integer, Order> eventOrder = objectMapper.readValue(messages.get(0), new TypeReference<>() {});
        Order payload = eventOrder.getData();
        assertEquals(Order.Status.TRANSIT, payload.getStatus());

        Event<Integer, Order> expectedEvent = new Event<>(
                UPDATE,
                order.getOrderId(),
                new Order(
                        order.getOrderId(),
                        order.getCargoId(),
                        order.getFromStoreId(),
                        order.getToStoreId(),
                        order.getStatus(),
                        order.getServiceAddress()
                )
        );
        assertThat(messages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void deleteOrder() {
        deleteAndVerifyOrder(1, ACCEPTED);
        final List<String> messages = readerOrderChanges.getMessages();

        // Assert one expected new order event queued up
        assertEquals(1, messages.size());

        Event<Integer, Order> expectedEvent = new Event<>(
                DELETE,
                1,
                null
        );
        assertThat(messages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    private void postAndVerifyOrder(Order order, HttpStatus expectedStatus) {
        client.post()
                .uri(appApi + "/order")
                .body(just(order), Order.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void postUpdateAndVerifyOrder(Order order, HttpStatus expectedStatus) {
        client.post()
                .uri(appApi + "/order/update")
                .body(just(order), Order.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyOrder(int orderId, HttpStatus expectedStatus) {
        client.delete()
                .uri(appApi + "/order/" + orderId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}
