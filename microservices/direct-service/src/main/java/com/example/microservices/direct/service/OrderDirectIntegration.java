package com.example.microservices.direct.service;

import com.example.microservices.direct.util.CustomExceptionResolver;
import com.example.microservices.direct.util.StreamMessageCook;
import com.example.mutual.api.core.order.Order;
import com.example.mutual.api.core.order.OrderService;
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
public class OrderDirectIntegration implements OrderService {

    private static final String ORDER_SERVICE_URL = "http://order";
    private final CustomExceptionResolver exceptionResolver;
    private final WebClient webClient;
    private final Scheduler publishEventScheduler;
    private final StreamMessageCook messageCook;
    private final String bindingName="order-crud";

    @Autowired
    public OrderDirectIntegration(
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
    public Mono<Order> getOrder(int orderId) {
        URI url = UriComponentsBuilder
                .fromUriString(ORDER_SERVICE_URL + "/order/" + orderId)
                .build(orderId);
        log.debug("Will call the getProduct API on URL: {}", url);

        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Order.class)
                .log(log.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, exceptionResolver::handleException);
    }

    @Override
    public Mono<Order> createOrder(Order body) {
        return Mono.fromCallable(() -> {
            messageCook.sendMessage(bindingName, new Event<>(CREATE, body.getOrderId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Order> updateOrder(Order body) {
        return Mono.fromCallable(() -> {
            messageCook.sendMessage(bindingName, new Event<>(UPDATE, body.getOrderId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteOrder(int orderId) {
        return Mono.fromRunnable(
                () -> messageCook.sendMessage(bindingName, new Event<>(DELETE, orderId, null))
        ).subscribeOn(publishEventScheduler).then();
    }
}
