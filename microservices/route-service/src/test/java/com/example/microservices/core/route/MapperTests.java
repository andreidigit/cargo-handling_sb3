package com.example.microservices.core.route;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.services.RouteMapper;
import com.example.mutual.api.core.route.Route;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class MapperTests {

    private RouteMapper mapper = Mappers.getMapper(RouteMapper.class);

    @Test
    void mapperTests() {

        assertNotNull(mapper);

        Route route1 = new Route(1, 10, 11, "ехай прями и на право", 100, 25);

        RouteEntity routeEntity = mapper.apiToEntity(route1);

        assertEquals(route1.getRouteId(), routeEntity.getRouteId());
        assertEquals(route1.getFromStoreId(), routeEntity.getFromStoreId());
        assertEquals(route1.getToStoreId(), routeEntity.getToStoreId());
        assertEquals(route1.getPathFromTo(), routeEntity.getPathFromTo());
        assertEquals(route1.getDistanceFromTo(), routeEntity.getDistanceFromTo());
        assertEquals(route1.getMinutesFromTo(), routeEntity.getMinutesFromTo());

        Route route2 = mapper.entityToApi(routeEntity);

        assertEquals(route1.getRouteId(), route2.getRouteId());
        assertEquals(route1.getFromStoreId(), route2.getFromStoreId());
        assertEquals(route1.getToStoreId(), route2.getToStoreId());
        assertEquals(route1.getPathFromTo(), routeEntity.getPathFromTo());
        assertEquals(route1.getDistanceFromTo(), route2.getDistanceFromTo());
        assertEquals(route1.getMinutesFromTo(), route2.getMinutesFromTo());
        assertNull(route2.getServiceAddress());

        RouteEntity clonedEntity = mapper.cloneRouteEntity(routeEntity);

        assertEquals(routeEntity.getRouteId(), clonedEntity.getRouteId());
        assertEquals(routeEntity.getFromStoreId(), clonedEntity.getFromStoreId());
        assertEquals(routeEntity.getToStoreId(), clonedEntity.getToStoreId());
        assertEquals(routeEntity.getPathFromTo(), clonedEntity.getPathFromTo());
        assertEquals(routeEntity.getDistanceFromTo(), clonedEntity.getDistanceFromTo());
        assertEquals(routeEntity.getMinutesFromTo(), clonedEntity.getMinutesFromTo());
        assertEquals(routeEntity.getCreatedOn(), clonedEntity.getCreatedOn());
    }

}
