package com.example.mutual.api.core.route;

import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

public interface RouteTaskService {
    Mono<Route> findRoute(@PathVariable RouteTaskPayload routeTaskPayload);
}
