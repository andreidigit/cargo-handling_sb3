package com.example.microservices.core.store.invariant;

import com.example.microservices.core.store.persistence.StoreEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleStoreDeleteUsedCapacity implements RuleStoreDelete {

    @Override
    public boolean check(StoreEntity storeEntity) {
        return storeEntity.getUsedCapacity() == 0;
    }
}
