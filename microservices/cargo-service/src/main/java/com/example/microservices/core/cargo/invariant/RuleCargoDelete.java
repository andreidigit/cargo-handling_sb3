package com.example.microservices.core.cargo.invariant;

import com.example.microservices.core.cargo.persistence.CargoEntity;

public interface RuleCargoDelete {
    boolean check(CargoEntity cargoEntity);
}
