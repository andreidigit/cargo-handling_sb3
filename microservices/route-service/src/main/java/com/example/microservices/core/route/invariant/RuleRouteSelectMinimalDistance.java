package com.example.microservices.core.route.invariant;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.mutual.api.core.route.RouteRuleType;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@NoArgsConstructor
public class RuleRouteSelectMinimalDistance implements RuleRouteSelect {
    private final RouteRuleType ruleType = RouteRuleType.MINIMAL_DISTANCE;
    public Optional<RouteEntity> find(List<RouteEntity> routs, RouteRuleType ruleType) {
        if (this.ruleType != ruleType) {
            return Optional.empty();
        }
        return routs.stream().min(Comparator.comparingInt(RouteEntity::getDistanceFromTo));
    }
}
