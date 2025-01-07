package com.example.microservices.core.order.invariant;

import com.example.microservices.core.order.persistence.OrderEntity;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class RuleOrderUpdateStatus implements RuleOrderUpdate {
    private final boolean noOneCare = true;
    public boolean apply(OrderEntity old, OrderEntity anew) {
        if(noOneCare){
            old.setStatus(anew.getStatus());
            return true;
        }
        return false;
    }
}
