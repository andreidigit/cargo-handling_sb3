package com.example.microservices.core.order.invariant;


import com.example.microservices.core.order.persistence.OrderEntity;

public interface RuleOrderUpdate {
    boolean apply(OrderEntity old, OrderEntity anew);
}
