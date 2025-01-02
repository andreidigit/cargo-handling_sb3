package com.example.mutual.api.direct;

import com.example.mutual.api.core.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface RouteDirectService {

    @GetMapping(
            value = "/route/{routeId}",
            produces = "application/json")
    Mono<Route> getRoute(@PathVariable int routeId);

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/route",
            consumes = "application/json")
    Mono<Void> createRoute(@RequestBody Route body);

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/route/update",
            consumes = "application/json")
    Mono<Void> updateRoute(@RequestBody Route body);


    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/route/{routeId}")
    Mono<Void> deleteRoute(@PathVariable int routeId);
}
