package com.example.mutual.api.core.route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RouteTaskPayload {
    private int orderId;
    private int fromStoreId;
    private int toStoreId;
    private Route route;
    private RouteRuleType ruleType;
}
