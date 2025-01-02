package com.example.microservices.core.route.invariant;


import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.mutual.api.core.route.RouteRuleType;

import java.util.List;
import java.util.Optional;

/**
 * Правило возвращает одно или ноль сущностей проверяя список путей по одному типу правила
 * */
public interface RuleRouteSelect {
    Optional<RouteEntity> find(List<RouteEntity> routs, RouteRuleType ruleType);
}