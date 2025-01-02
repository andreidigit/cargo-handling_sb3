package com.example.mutual.api.core.store;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@Component
public interface StoreService {
        /**
     * Sample usage: "curl $HOST:$PORT/store/1".
     *
     * @param storeId Id of the store
     * @return the store, if found, else null
     */
    @GetMapping(value = "/store/{storeId}", produces = "application/json")
    Mono<Store> getStore(@PathVariable int storeId);
    Mono<Store> createStore(@RequestBody Store body);
    Mono<Store> updateStore(@RequestBody Store body);
    Mono<Void> deleteStore(@PathVariable int storeId);
}
