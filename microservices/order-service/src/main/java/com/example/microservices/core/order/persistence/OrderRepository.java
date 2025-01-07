package com.example.microservices.core.order.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OrderRepository extends CrudRepository<OrderEntity, Integer> {
    @Transactional(readOnly = true)
    Optional<OrderEntity> findByOrderId(int orderId);
}
