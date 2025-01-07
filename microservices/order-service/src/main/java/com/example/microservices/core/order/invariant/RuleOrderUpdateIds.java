package com.example.microservices.core.order.invariant;

import com.example.microservices.core.order.persistence.OrderEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleOrderUpdateIds implements RuleOrderUpdate {
    public boolean apply(OrderEntity old, OrderEntity anew) {
        return old.getOrderId() == anew.getOrderId()
                && old.getFromStoreId() == anew.getFromStoreId()
                && old.getToStoreId() == anew.getToStoreId();
    }
}
