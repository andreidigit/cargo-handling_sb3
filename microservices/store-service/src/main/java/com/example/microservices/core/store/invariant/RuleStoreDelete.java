package com.example.microservices.core.store.invariant;

import com.example.microservices.core.store.persistence.StoreEntity;

public interface RuleStoreDelete {
    boolean check(StoreEntity storeEntity);
}
