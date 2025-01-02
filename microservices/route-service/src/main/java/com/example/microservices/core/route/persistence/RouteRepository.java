package com.example.microservices.core.route.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends CrudRepository<RouteEntity, Integer> {
    @Transactional(readOnly = true)
    Optional<RouteEntity> findByRouteId(int storeId);

    @Transactional(readOnly = true)
    List<RouteEntity> findByFromStoreIdAndToStoreId(int fromStoreId, int toStoreId);
}
