package com.example.microservices.core.cargo.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CargoRepository extends CrudRepository<CargoEntity, Integer> {
    @Transactional(readOnly = true)
    Optional<CargoEntity> findByCargoId(int cargoId);
}
