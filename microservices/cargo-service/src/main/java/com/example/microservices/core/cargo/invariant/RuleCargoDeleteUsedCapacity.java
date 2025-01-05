package com.example.microservices.core.cargo.invariant;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.mutual.api.core.cargo.Cargo;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleCargoDeleteUsedCapacity implements RuleCargoDelete {

    @Override
    public boolean check(CargoEntity cargoEntity) {
        Cargo.Status status = cargoEntity.getStatus();
        return !(status == Cargo.Status.WAIT || status == Cargo.Status.TRANSIT) ;
    }
}
