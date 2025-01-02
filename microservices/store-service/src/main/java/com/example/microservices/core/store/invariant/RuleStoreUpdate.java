package com.example.microservices.core.store.invariant;


import com.example.microservices.core.store.persistence.StoreEntity;

public interface RuleStoreUpdate {
    boolean apply(StoreEntity old, StoreEntity anew);
}
