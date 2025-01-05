package com.example.microservices.core.cargo.invariant;


import com.example.microservices.core.cargo.persistence.CargoEntity;

public interface RuleCargoUpdate {
    boolean apply(CargoEntity old, CargoEntity anew);
}
