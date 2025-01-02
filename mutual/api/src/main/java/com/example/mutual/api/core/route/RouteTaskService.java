package com.example.mutual.api.core.route;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Component
public interface RouteTaskService {
    Mono<Route> findRoute(@PathVariable RouteTaskPayload routeTaskPayload);
}
