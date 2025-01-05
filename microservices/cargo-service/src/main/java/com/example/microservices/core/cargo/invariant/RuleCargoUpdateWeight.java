package com.example.microservices.core.cargo.invariant;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleCargoUpdateWeight implements RuleCargoUpdate {
    private final int restrictWeight = 500;
    public boolean apply(CargoEntity old, CargoEntity anew) {
        if(anew.getWeight() > 0 && anew.getWeight() < restrictWeight){
            old.setWeight(anew.getWeight());
            return true;
        }
        return false;
    }
}
