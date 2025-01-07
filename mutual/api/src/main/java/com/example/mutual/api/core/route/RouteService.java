package com.example.mutual.api.core.route;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public interface RouteService {
        /**
     * Sample usage: "curl $HOST:$PORT/route/1".
     *
     * @param routeId Id of the route
     * @return the route, if found, else null
     */
    @GetMapping(value = "/route/{routeId}", produces = "application/json")
    Mono<Route> getRoute(@PathVariable int routeId);
    Mono<Route> createRoute(@RequestBody Route body);
    Mono<Route> updateRoute(@RequestBody Route body);
    Mono<Void> deleteRoute(@PathVariable int routeId);
}
