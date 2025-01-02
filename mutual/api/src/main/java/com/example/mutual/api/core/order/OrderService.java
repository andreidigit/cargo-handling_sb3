package com.example.mutual.api.core.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface OrderService {

    /**
     * Sample usage: "curl $HOST:$PORT/order?orderId=1".
     *
     * @param orderId Id of the order
     * @return the reviews of the order
     */
    @GetMapping(value = "/review", produces = "application/json")
    List<Order> getReviews(@RequestParam(value = "orderId") int orderId);
}
