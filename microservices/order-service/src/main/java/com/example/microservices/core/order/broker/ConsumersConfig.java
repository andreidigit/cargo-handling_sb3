package com.example.microservices.core.order.broker;

import com.example.mutual.api.core.order.Order;
import com.example.mutual.api.core.order.OrderService;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class ConsumersConfig {

    private final OrderService orderService;

    @Autowired
    public ConsumersConfig(OrderService orderService) {
        this.orderService = orderService;
    }

    @Bean
    public Consumer<Event<Integer, Order>> consumerCrud() {
        return event -> {
            log.info("Process message created at {}...", event.getEventCreatedAt());
            Order store;

            switch (event.getEventType()) {

                case CREATE:
                    store = event.getData();
                    log.info("Create store with ID: {}", store.getOrderId());
                    orderService.createOrder(store).block();
                    break;

                case UPDATE:
                    store = event.getData();
                    log.info("Update store with storeID: {}", store.getOrderId());
                    orderService.updateOrder(store).block();
                    break;

                case DELETE:
                    log.info("Delete store with storeID: {}", event.getKey());
                    orderService.deleteOrder(event.getKey()).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }

            log.info("Message processing done!");
        };
    }
}
