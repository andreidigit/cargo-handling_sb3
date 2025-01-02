package com.example.microservices.direct;

import com.example.microservices.direct.util.ReaderProducedMessages;
import com.example.mutual.api.core.route.Route;
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
class RouteMessagingTests {

    @Autowired
    private WebTestClient client;
    private final ReaderProducedMessages readerRouteChanges;

    @Autowired
    public RouteMessagingTests(
            @Value("${channelsOut.route.topic}") String topicName,
            OutputDestination target
    ) {
        this.readerRouteChanges = new ReaderProducedMessages(target, topicName);
    }

    @BeforeEach
    void setUp() {
        readerRouteChanges.purgeMessages();
    }


    @Test
    void createRoute() {
       Route route = new Route(1, 20, 30, "form to there", 210, 20);
        postAndVerifyRoute(route, ACCEPTED);
        final List<String> routeMessages = readerRouteChanges.getMessages();

        // Assert one expected new route event queued up
        assertEquals(1, routeMessages.size());

        Event<Integer, Route> expectedEvent = new Event<>(
                CREATE,
                route.getRouteId(),
                new Route(
                        route.getRouteId(),
                        route.getFromStoreId(),
                        route.getToStoreId(),
                        route.getPathFromTo(),
                        route.getDistanceFromTo(),
                        route.getMinutesFromTo()
                )
        );
        assertThat(routeMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void updateRoute() {
        Route route = new Route(1, 20, 30, "from to there", 210, 20);
        postUpdateAndVerifyRoute(route, ACCEPTED);
        final List<String> routeMessages = readerRouteChanges.getMessages();

        // Assert one expected new route event queued up
        assertEquals(1, routeMessages.size());

        Event<Integer, Route> expectedEvent = new Event<>(
                UPDATE,
                route.getRouteId(),
                new Route(
                        route.getRouteId(),
                        route.getFromStoreId(),
                        route.getToStoreId(),
                        route.getPathFromTo(),
                        route.getDistanceFromTo(),
                        route.getMinutesFromTo()
                )
        );
        assertThat(routeMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    @Test
    void deleteRoute() {
        deleteAndVerifyRoute(1, ACCEPTED);
        final List<String> routeMessages = readerRouteChanges.getMessages();

        // Assert one expected new route event queued up
        assertEquals(1, routeMessages.size());

        Event<Integer, Route> expectedEvent = new Event<>(
                DELETE,
                1,
                null
        );
        assertThat(routeMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));
    }

    private void postAndVerifyRoute(Route route, HttpStatus expectedStatus) {
        client.post()
                .uri("/route")
                .body(just(route), Route.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void postUpdateAndVerifyRoute(Route route, HttpStatus expectedStatus) {
        client.post()
                .uri("/route/update")
                .body(just(route), Route.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyRoute(int routeId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/route/" + routeId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}
