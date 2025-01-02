package com.example.mutual.api.direct;

import com.example.mutual.api.core.store.Store;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface StoreDirectService {

    @GetMapping(
            value = "/store/{storeId}",
            produces = "application/json")
    Mono<Store> getStore(@PathVariable int storeId);

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/store",
            consumes = "application/json")
    Mono<Void> createStore(@RequestBody Store body);

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            value = "/store/update",
            consumes = "application/json")
    Mono<Void> updateStore(@RequestBody Store body);


    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping(value = "/store/{storeId}")
    Mono<Void> deleteStore(@PathVariable int storeId);
}
