package com.example.microservices.core.store.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface StoreRepository extends CrudRepository<StoreEntity, Integer> {
    @Transactional(readOnly = true)
    Optional<StoreEntity> findByStoreId(int storeId);
}
