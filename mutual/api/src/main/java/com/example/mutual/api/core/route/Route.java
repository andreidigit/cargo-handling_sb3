package com.example.mutual.api.core.route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Route {
    private int routeId = 0;
    private int fromStoreId = 0;
    private int toStoreId = 0;
    private String pathFromTo = null;
    private int distanceFromTo = 0;
    private int minutesFromTo = 0;

    private String serviceAddress = null;

    public Route(int routeId, int fromStoreId, int toStoreId, String pathFromTo, int distanceFromTo, int minutesFromTo) {
        this.routeId = routeId;
        this.fromStoreId = fromStoreId;
        this.toStoreId = toStoreId;
        this.pathFromTo = pathFromTo;
        this.distanceFromTo = distanceFromTo;
        this.minutesFromTo = minutesFromTo;
    }
}
