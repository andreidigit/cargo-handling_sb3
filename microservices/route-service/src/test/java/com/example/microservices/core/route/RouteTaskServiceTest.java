package com.example.microservices.core.route;

import com.example.microservices.core.route.broker.ConsumersConfig;
import com.example.microservices.core.route.persistence.RouteRepository;
import com.example.microservices.core.route.util.ContainersTestBase;
import com.example.microservices.core.route.util.ReaderProducedMessages;
import com.example.microservices.core.route.util.RouteEntityHelper;
import com.example.mutual.api.core.route.RouteRuleType;
import com.example.mutual.api.core.route.RouteTaskPayload;
import com.example.mutual.api.event.EventTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@Slf4j
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class, ConsumersConfig.class})
@Transactional(propagation = NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RouteTaskServiceTest extends ContainersTestBase {

    @Autowired
    private RouteRepository repository;
    private final ReaderProducedMessages readerRouteFound;
    @Autowired
    private final ObjectMapper objectMapper;

    @Autowired
    public RouteTaskServiceTest(OutputDestination target, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.readerRouteFound = new ReaderProducedMessages(target, "route-find");
    }

    @Autowired
    private Consumer<EventTask<Integer, RouteTaskPayload>> consumerTask;

    @Test
    void updateRoute() throws JsonProcessingException {
        // записываем 3 сущности
        repository.save(RouteEntityHelper.getEntity(2));
        int MINIMAL_DISTANCE = 3;
        repository.save(RouteEntityHelper.getEntity(MINIMAL_DISTANCE));
        int MINIMAL_MINUTES = 4;
        repository.save(RouteEntityHelper.getEntity(MINIMAL_MINUTES));

        assertEquals(3, repository.count());

        readerRouteFound.purgeMessages();
        assertEquals(0, readerRouteFound.getMessages().size());
        // создадим задачу на поиск пути по критерию - минимальное время в дороге
        int orderId = 1;
        sendFindRouteEvent(orderId, RouteRuleType.MINIMAL_MINUTES);         // 1. Event FIND

        // ловим в ответ, выполненную задачу с найденным путем
        List<String> messages = readerRouteFound.getMessages();
        assertEquals(1, messages.size());
        EventTask<Integer, RouteTaskPayload> eventTask = objectMapper.readValue(messages.get(0), new TypeReference<>() {
        });
        RouteTaskPayload payload = eventTask.getData();
        // проверяем что найденный путь действительно с ожидаемым айди
        assertEquals(EventTask.Type.ROUTE_FOUND, eventTask.getEventType());
        assertEquals(1, payload.getOrderId());
        assertEquals(MINIMAL_MINUTES, payload.getRoute().getRouteId());

        // создаем задачу на поиск по кратчайшему пути
        sendFindRouteEvent(orderId, RouteRuleType.MINIMAL_DISTANCE);         // 1. Event FIND
        // получаем выполненную задачу с найденным путем
        messages = readerRouteFound.getMessages();
        assertEquals(1, messages.size());
        eventTask = objectMapper.readValue(messages.get(0), new TypeReference<EventTask<Integer, RouteTaskPayload>>() {
        });
        payload = eventTask.getData();
        // проверяем что путь с нужным айди
        assertEquals(EventTask.Type.ROUTE_FOUND, eventTask.getEventType());
        assertEquals(1, payload.getOrderId());
        assertEquals(MINIMAL_DISTANCE, payload.getRoute().getRouteId());
    }

    private void sendFindRouteEvent(int orderId, RouteRuleType ruleType) {
        RouteTaskPayload payload = new RouteTaskPayload(orderId, 10, 15, null, ruleType);
        EventTask<Integer, RouteTaskPayload> event = new EventTask<>(EventTask.Type.FIND_ROUTE, orderId, payload);
        consumerTask.accept(event);
    }
}
