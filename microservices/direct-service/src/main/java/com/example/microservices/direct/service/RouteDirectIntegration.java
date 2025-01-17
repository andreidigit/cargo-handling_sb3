package com.example.microservices.direct.service;

import com.example.mutual.api.core.route.Route;
import com.example.mutual.api.core.route.RouteService;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.InvalidInputException;
import com.example.mutual.api.exceptions.NotFoundException;
import com.example.mutual.util.http.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import static com.example.mutual.api.event.Event.Type.*;
import static java.util.logging.Level.FINE;

@Slf4j
@Component
public class RouteDirectIntegration implements RouteService {

    private static final String STORE_SERVICE_URL = "http://route";

    private final ObjectMapper mapper;
    private final WebClient webClient;
    private final Scheduler publishEventScheduler;
    private final StreamBridge streamBridge;
    private final String bindingName = "route-crud";

    @Autowired
    public RouteDirectIntegration(
            ObjectMapper mapper,
            WebClient webClient,
            Scheduler publishEventScheduler,
            StreamBridge streamBridge
    ) {
        this.mapper = mapper;
        this.webClient = webClient;
        this.publishEventScheduler = publishEventScheduler;
        this.streamBridge = streamBridge;
    }

    @Override
    public Mono<Route> getRoute(int routeId) {
        URI url = UriComponentsBuilder
                .fromUriString(STORE_SERVICE_URL + "/route/" + routeId)
                .build(routeId);
        log.debug("Will call the getProduct API on URL: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Route.class)
                .log(log.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, this::handleException);
    }

    private Throwable handleException(Throwable ex) {
        if (!(ex instanceof WebClientResponseException wcre)) {
            log.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }
        switch (Objects.requireNonNull(HttpStatus.resolve(wcre.getStatusCode().value()))) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                log.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                log.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    @Override
    public Mono<Route> createRoute(Route body) {
        return Mono.fromCallable(() -> {
            sendMessage(bindingName, new Event<>(CREATE, body.getRouteId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Route> updateRoute(Route body) {
        return Mono.fromCallable(() -> {
            sendMessage(bindingName, new Event<>(UPDATE, body.getRouteId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteRoute(int routeId) {
        return Mono.fromRunnable(
                () -> sendMessage(bindingName, new Event<>(DELETE, routeId, null))
        ).subscribeOn(publishEventScheduler).then();
    }

    private void sendMessage(String bindingName, Event event) {
        log.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        log.debug("Sent success: {}, Name: {}, Event: {}",
                streamBridge.send(bindingName, message),
                bindingName,
                event.getEventType()
        );
    }
}
