package com.example.microservices.core.route.util;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.services.RouteMapper;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RouteEntityHelper {
    private static final RouteMapper mapper = Mappers.getMapper(RouteMapper.class);
    static Map<Integer, RouteEntity> map = new HashMap<>();

    static {
        map.put(1, buildNew(1, 20, 30, "ехай", 210, 20));
        map.put(2, buildNew(2, 10, 15, "ехай тудой", 200, 20));
        map.put(3, buildNew(3, 10, 15, "ехай тама", 180, 25));
        map.put(4, buildNew(4, 10, 15, "ехай здеся", 220, 15));
        map.put(5, buildNew(5, 50, 15, "ехай", 220, 15));
        map.put(6, buildNew(6, 60, 15, "ехай", 220, 15));
        map.put(7, buildNew(7, 70, 15, "ехай", 220, 15));
    }

    public static RouteEntity buildNew(int routeId, int fromStoreId, int toStoreId, String pathFromTo, int distanceFromTo, int minutesFromTo){
        RouteEntity entity = new RouteEntity(routeId, fromStoreId, toStoreId);
        entity.setPathFromTo(pathFromTo);
        entity.setDistanceFromTo(distanceFromTo);
        entity.setMinutesFromTo(minutesFromTo);
        return entity;
    }

    public static RouteEntity getEntity(int i) {
        return map.get(i);
    }

    public  static void assertEqualsRoute(RouteEntity expectedEntity, RouteEntity actualEntity) {
        assertEquals(expectedEntity.getId(),        actualEntity.getId());
        assertEquals(expectedEntity.getVersion(),   actualEntity.getVersion());
        assertEquals(expectedEntity.getRouteId(),   actualEntity.getRouteId());
        assertEquals(expectedEntity.getFromStoreId(),   actualEntity.getFromStoreId());
        assertEquals(expectedEntity.getToStoreId(),   actualEntity.getToStoreId());
        assertEquals(expectedEntity.getPathFromTo(),   actualEntity.getPathFromTo());
        assertEquals(expectedEntity.getDistanceFromTo(),   actualEntity.getDistanceFromTo());
        assertEquals(expectedEntity.getMinutesFromTo(),   actualEntity.getMinutesFromTo());
    }
}
