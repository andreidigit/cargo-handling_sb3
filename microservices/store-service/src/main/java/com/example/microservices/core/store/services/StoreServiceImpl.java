package com.example.microservices.core.store.services;

import com.example.microservices.core.store.broker.ProducerRevise;
import com.example.microservices.core.store.invariant.RuleStoreDelete;
import com.example.microservices.core.store.invariant.RuleStoreUpdate;
import com.example.microservices.core.store.persistence.StoreEntity;
import com.example.microservices.core.store.persistence.StoreRepository;
import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.core.store.StoreService;
import com.example.mutual.api.exceptions.InvalidInputException;
import com.example.mutual.api.exceptions.NotFoundException;
import com.example.mutual.util.http.ServiceUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Optional;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class StoreServiceImpl implements StoreService {

    private final StoreRepository repository;
    private final StoreMapper mapper;
    private final ServiceUtil serviceUtil;
    private final Scheduler jdbcScheduler;
    private final ProducerRevise producer;
    private final List<RuleStoreUpdate> rulesUpdate;
    private final List<RuleStoreDelete> rulesDelete;

    @Autowired
    public StoreServiceImpl(
            StoreRepository repository,
            StoreMapper mapper,
            ServiceUtil serviceUtil,
            @Qualifier("jdbcScheduler") Scheduler jdbcScheduler,
            ProducerRevise producer,
            List<RuleStoreUpdate> rulesUpdate,
            List<RuleStoreDelete> rulesDelete
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
        this.jdbcScheduler = jdbcScheduler;
        this.producer = producer;
        this.rulesUpdate = rulesUpdate;
        this.rulesDelete = rulesDelete;
    }

    @Override
    public Mono<Store> getStore(int storeId) {
        if (storeId < 1) {
            throw new InvalidInputException("Invalid storeId: " + storeId);
        }
        log.info("Will get store with id={}", storeId);
        return Mono.fromCallable(() -> internalGetStore(storeId))
                .switchIfEmpty(Mono.error(new NotFoundException("No store found for storeId: " + storeId)))
                .log(log.getName(), FINE)
                .subscribeOn(jdbcScheduler);
    }

    private Store internalGetStore(int storeId) {
        return repository.findByStoreId(storeId)
                .map(storeEntity -> {
                    Store store = mapper.entityToApi(storeEntity);
                    store.setServiceAddress(serviceUtil.getServiceAddress());
                    return store;
                })
                .orElse(null);
    }

    @Transactional
    @Override
    public Mono<Store> createStore(Store body) {
        if (body.getStoreId() < 1) {
            throw new InvalidInputException("Invalid storeId: " + body.getStoreId());
        }
        return Mono.fromCallable(()-> internalCreateStore(body))
                .subscribeOn(jdbcScheduler);
    }

    private Store internalCreateStore(Store body) {
        try {
            StoreEntity entity = mapper.apiToEntity(body);
            StoreEntity newEntity = repository.save(entity);
            producer.storeCreated(newEntity);
            log.debug("createStore: created a store entity: {}", body.getStoreId());
            return mapper.entityToApi(newEntity);
        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Store Id: " + body.getStoreId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Store> updateStore(Store body) {
        if (body.getStoreId() < 1) {
            throw new InvalidInputException("Invalid storeId: " + body.getStoreId());
        }
        return Mono.fromCallable(()-> internalUpdateStore(body))
                .subscribeOn(jdbcScheduler);
    }
    private Store internalUpdateStore(Store body) {
        Optional<StoreEntity> entityOpt = repository.findByStoreId(body.getStoreId());
        if (entityOpt.isPresent()) {
            StoreEntity entity = entityOpt.get();
            StoreEntity oldStoreEntity = mapper.cloneStoreEntity(entity);
            boolean isBrokenRule = rulesUpdate.stream()
                    .map(rule -> rule.apply(entity, mapper.apiToEntity(body)))
                    .anyMatch(bool -> !bool);
            if (isBrokenRule) {
                log.warn("updateStore: there is broken Rule of entity storeId: {}", body.getStoreId());
                throw new InvalidInputException("There is a broken Update Rule, Store Id: " + body.getStoreId());
            }
            repository.save(entity);
            producer.storeUpdated(oldStoreEntity);
            log.debug("updateStore: updated a store entity: {}", body.getStoreId());
            return mapper.entityToApi(entity);
        } else {
            throw new NotFoundException("There is no Store with storeId: " + body.getStoreId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Void> deleteStore(int storeId) {
        return Mono.fromRunnable(()->internalDeleteStore(storeId)).subscribeOn(jdbcScheduler).then();
    }

    private void internalDeleteStore(int storeId) {
        log.debug("deleteStore: tries to delete with storeId: {}", storeId);
        repository.findByStoreId(storeId).ifPresent(
            storeEntity->{
                boolean isBrokenRule = rulesDelete.stream()
                    .map(rule -> rule.check(storeEntity))
                    .anyMatch(bool -> !bool);
                if (isBrokenRule) {
                    log.warn("deleteStore: there is broken Rule of entity storeId: {}", storeId);
                    throw new InvalidInputException("There is a broken Delete Rule, store Id: " + storeId);
                }
                repository.delete(storeEntity);
                producer.storeDeleted(storeEntity);
            }
        );
    }
}
