package com.example.microservices.direct.service;

import com.example.mutual.api.core.route.Route;
import com.example.mutual.api.direct.RouteDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class RouteDirectServiceImpl implements RouteDirectService {

    private final RouteDirectIntegration integration;

    @Autowired
    public RouteDirectServiceImpl(RouteDirectIntegration integration) {
        this.integration = integration;
    }

    @Override
    public Mono<Route> getRoute(int routeId) {
        return integration.getRoute(routeId)
                .doOnError(ex -> log.warn("getRoute failed: {}", ex.toString()))
                .log(log.getName(), FINE);
    }

    @Override
    public Mono<Void> createRoute(Route body) {
        try {
            log.debug("create a new route for routeId: {}", body.getRouteId());
            Route route = new Route(
                    body.getRouteId(),
                    body.getFromStoreId(),
                    body.getToStoreId(),
                    body.getPathFromTo(),
                    body.getDistanceFromTo(),
                    body.getMinutesFromTo()
            );

            return integration.createRoute(route)
                    .doOnError(ex -> log.warn("createRoute failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createRoute failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> updateRoute(Route body) {
        try {
            log.debug("update the route for routeId: {}", body.getRouteId());
            Route route = new Route(
                    body.getRouteId(),
                    body.getFromStoreId(),
                    body.getToStoreId(),
                    body.getPathFromTo(),
                    body.getDistanceFromTo(),
                    body.getMinutesFromTo()
            );

            return integration.updateRoute(route)
                    .doOnError(ex -> log.warn("createRoute failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createRoute failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> deleteRoute(int routeId) {
        try {
            return integration.deleteRoute(routeId)
                    .doOnError(ex -> log.warn("deleteRoute failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("deleteRoute failed: {}", re.toString());
            throw re;
        }
    }
}
