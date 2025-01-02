package com.example.microservices.core.store.invariant;

import com.example.microservices.core.store.persistence.StoreEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleStoreUpdateLocation implements RuleStoreUpdate {
    public boolean apply(StoreEntity old, StoreEntity anew) {
        if(anew.getLocation().length() < 255){
            old.setLocation(anew.getLocation());
            return true;
        }
        return false;
    }
}
