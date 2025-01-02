package com.example.microservices.core.store.invariant;

import com.example.microservices.core.store.persistence.StoreEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleStoreUpdateUsedCapacity implements RuleStoreUpdate {
    public boolean apply(StoreEntity old, StoreEntity anew) {
        if(old.getCapacity() >= anew.getUsedCapacity()){
            old.setUsedCapacity(anew.getUsedCapacity());
            return true;
        }
        return false;
    }
}
