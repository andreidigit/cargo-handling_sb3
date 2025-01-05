package com.example.microservices.core.cargo.invariant;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleCargoUpdateStatus implements RuleCargoUpdate {
    private final boolean noOneCare = true;
    public boolean apply(CargoEntity old, CargoEntity anew) {
        if(noOneCare){
            old.setStatus(anew.getStatus());
            return true;
        }
        return false;
    }
}
