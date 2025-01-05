package com.example.microservices.core.cargo.invariant;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleCargoUpdateName implements RuleCargoUpdate {
    public boolean apply(CargoEntity old, CargoEntity anew) {
        if(!anew.getName().isEmpty() && anew.getName().length() < 255){
            old.setName(anew.getName());
            return true;
        }
        return false;
    }
}
