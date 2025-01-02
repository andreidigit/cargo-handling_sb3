package com.example.microservices.core.store.invariant;

import com.example.microservices.core.store.persistence.StoreEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleStoreUpdateCapacity implements RuleStoreUpdate {
    public boolean apply(StoreEntity old, StoreEntity anew) {
        if(anew.getCapacity() > old.getUsedCapacity()){
            old.setCapacity(anew.getCapacity());
            return true;
        }
        return false;
    }
}
