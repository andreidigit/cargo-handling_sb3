package com.example.microservices.direct.service;

import com.example.microservices.direct.util.CustomExceptionResolver;
import com.example.microservices.direct.util.StreamMessageCook;
import com.example.mutual.api.core.cargo.Cargo;
import com.example.mutual.api.core.cargo.CargoService;
import com.example.mutual.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.net.URI;

import static com.example.mutual.api.event.Event.Type.*;
import static java.util.logging.Level.FINE;

@Slf4j
@Component
public class CargoDirectIntegration implements CargoService {

    private static final String CARGO_SERVICE_URL = "http://cargo";
    private final CustomExceptionResolver exceptionResolver;
    private final WebClient webClient;
    private final Scheduler publishEventScheduler;
    private final StreamMessageCook messageCook;
    private final String bindingName="cargo-crud";

    @Autowired
    public CargoDirectIntegration(
            CustomExceptionResolver exceptionResolver,
            WebClient webClient,
            Scheduler publishEventScheduler,
            StreamMessageCook messageCook
    ) {
        this.exceptionResolver = exceptionResolver;
        this.webClient = webClient;
        this.publishEventScheduler = publishEventScheduler;
        this.messageCook = messageCook;
    }
    @Override
    public Mono<Cargo> getCargo(int cargoId) {
        URI url = UriComponentsBuilder
                .fromUriString(CARGO_SERVICE_URL + "/cargo/" + cargoId)
                .build(cargoId);
        log.debug("Will call the getProduct API on URL: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Cargo.class)
                .log(log.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, exceptionResolver::handleException);
    }

    @Override
    public Mono<Cargo> createCargo(Cargo body) {
        return Mono.fromCallable(() -> {
            messageCook.sendMessage(bindingName, new Event<>(CREATE, body.getCargoId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Cargo> updateCargo(Cargo body) {
        return Mono.fromCallable(() -> {
            messageCook.sendMessage(bindingName, new Event<>(UPDATE, body.getCargoId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteCargo(int cargoId) {
        return Mono.fromRunnable(
                () -> messageCook.sendMessage(bindingName, new Event<>(DELETE, cargoId, null))
        ).subscribeOn(publishEventScheduler).then();
    }
}
