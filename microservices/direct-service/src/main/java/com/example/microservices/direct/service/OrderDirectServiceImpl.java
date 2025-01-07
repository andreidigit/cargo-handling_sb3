package com.example.microservices.direct.service;

import com.example.microservices.direct.util.AuthLog;
import com.example.mutual.api.core.order.Order;
import com.example.mutual.api.direct.OrderDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class OrderDirectServiceImpl implements OrderDirectService {
    private final AuthLog authLog;
    private final OrderDirectIntegration integration;

    @Autowired
    public OrderDirectServiceImpl(AuthLog authLog, OrderDirectIntegration integration) {
        this.authLog = authLog;
        this.integration = integration;
    }

    @Override
    public Mono<Order> getOrder(int orderId) {
        return integration.getOrder(orderId)
                .doOnError(ex -> log.warn("getOrder failed: {}", ex.toString()))
                .log(log.getName(), FINE);
    }

    @Override
    public Mono<Void> createOrder(Order body) {
        try {
            log.debug("create a new order for orderId: {}", body.getOrderId());
            Order order = new Order(
                    body.getOrderId(),
                    body.getCargoId(),
                    body.getFromStoreId(),
                    body.getToStoreId(),
                    body.getStatus(),
                    body.getServiceAddress()
            );

            return integration.createOrder(order)
                    .doOnError(ex -> log.warn("createOrder failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createOrder failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> updateOrder(Order body) {
        try {
            log.debug("update the order for orderId: {}", body.getOrderId());
            Order order = new Order(
                    body.getOrderId(),
                    body.getCargoId(),
                    body.getFromStoreId(),
                    body.getToStoreId(),
                    body.getStatus(),
                    body.getServiceAddress()
            );
            order.setStatus(body.getStatus());
            return integration.updateOrder(order)
                    .doOnError(ex -> log.warn("createOrder failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createOrder failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> deleteOrder(int orderId) {
        return authLog.getLogAuthorizationInfoMono()
                .flatMap(sc -> integration.deleteOrder(orderId)
                        .doOnError(ex -> log.warn("deleteOrder failed: {}", ex.toString()))
                        .onErrorResume(Mono::error)
                        .then()
                );
    }
}
