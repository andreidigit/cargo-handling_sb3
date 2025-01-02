package com.example.microservices.core.route.broker;

import com.example.mutual.api.core.route.Route;
import com.example.mutual.api.core.route.RouteService;
import com.example.mutual.api.core.route.RouteTaskService;
import com.example.mutual.api.core.route.RouteTaskPayload;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.event.EventTask;
import com.example.mutual.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class ConsumersConfig {

    private final RouteService routeService;
    private final RouteTaskService routeTaskService;

    @Autowired
    public ConsumersConfig(RouteService routeService, RouteTaskService routeTaskService) {
        this.routeService = routeService;
        this.routeTaskService = routeTaskService;
    }

    @Bean
    public Consumer<Event<Integer, Route>> consumerCrud() {
        return event -> {
            log.info("Process message created at {}...", event.getEventCreatedAt());
            Route route;

            switch (event.getEventType()) {

                case CREATE:
                    route = event.getData();
                    log.info("Create route with ID: {}", route.getRouteId());
                    routeService.createRoute(route).block();
                    break;

                case UPDATE:
                    route = event.getData();
                    log.info("Update route with routeID: {}", route.getRouteId());
                    routeService.updateRoute(route).block();
                    break;

                case DELETE:
                    log.info("Delete route with routeID: {}", event.getKey());
                    routeService.deleteRoute(event.getKey()).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }

            log.info("Message processing done!");
        };
    }
    @Bean
    public Consumer<EventTask<Integer, RouteTaskPayload>> consumerTask() {
        return event -> {
            log.info("Process message created at {}...", event.getEventCreatedAt());
            RouteTaskPayload routeTaskPayload;

            switch (event.getEventType()) {

                case FIND_ROUTE:
                    routeTaskPayload = event.getData();
                    log.info("Find route fore order ID: {}", routeTaskPayload.getOrderId());
                    routeTaskService.findRoute(routeTaskPayload).block();
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
