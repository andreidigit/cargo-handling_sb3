package com.example.mutual.api.core.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public interface OrderService {

    /**
     * Sample usage: "curl $HOST:$PORT/order/1".
     *
     * @param orderId Id of the order
     * @return the order, if found, else null
     */
    @GetMapping(value = "/order/{orderId}", produces = "application/json")
    Mono<Order> getOrder(@PathVariable int orderId);
    Mono<Order> createOrder(@RequestBody Order body);
    Mono<Order> updateOrder(@RequestBody Order body);
    Mono<Void> deleteOrder(@PathVariable int orderId);
}
