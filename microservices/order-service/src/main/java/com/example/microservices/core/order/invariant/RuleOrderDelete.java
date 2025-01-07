package com.example.microservices.core.order.invariant;

import com.example.microservices.core.order.persistence.OrderEntity;

public interface RuleOrderDelete {
    boolean check(OrderEntity orderEntity);
}
