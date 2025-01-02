package com.example.microservices.direct.service;

import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.direct.StoreDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class StoreDirectServiceImpl implements StoreDirectService {

    private final StoreDirectIntegration integration;

    @Autowired
    public StoreDirectServiceImpl(StoreDirectIntegration integration) {
        this.integration = integration;
    }

    @Override
    public Mono<Store> getStore(int storeId) {
        return integration.getStore(storeId)
                .doOnError(ex -> log.warn("getStore failed: {}", ex.toString()))
                .log(log.getName(), FINE);
    }

    @Override
    public Mono<Void> createStore(Store body) {
        try {
            log.debug("create a new store for storeId: {}", body.getStoreId());
            Store store = new Store(body.getStoreId(), body.getLocation(), body.getCapacity(), body.getUsedCapacity(), null);

            return integration.createStore(store)
                    .doOnError(ex -> log.warn("createStore failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createStore failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> updateStore(Store body) {
        try {
            log.debug("update the store for storeId: {}", body.getStoreId());
            Store store = new Store(body.getStoreId(), body.getLocation(), body.getCapacity(), body.getUsedCapacity(), null);

            return integration.updateStore(store)
                    .doOnError(ex -> log.warn("createStore failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createStore failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> deleteStore(int storeId) {
        try {
            return integration.deleteStore(storeId)
                    .doOnError(ex -> log.warn("deleteStore failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("deleteStore failed: {}", re.toString());
            throw re;
        }
    }
}
