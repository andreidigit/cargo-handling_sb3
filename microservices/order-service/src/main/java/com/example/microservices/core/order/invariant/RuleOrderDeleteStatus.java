package com.example.microservices.core.order.invariant;

import com.example.microservices.core.order.persistence.OrderEntity;
import com.example.mutual.api.core.order.Order;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleOrderDeleteStatus implements RuleOrderDelete {

    @Override
    public boolean check(OrderEntity orderEntity) {
        Order.Status status = orderEntity.getStatus();
        return !(status == Order.Status.TRANSIT) ;
    }
}
