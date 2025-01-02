package com.example.microservices.core.route.services;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.mutual.api.core.route.Route;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RouteMapper {

    RouteEntity cloneRouteEntity(RouteEntity storeEntity);
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Route entityToApi(RouteEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "updatedOn", ignore = true),
            @Mapping(target = "createdOn", ignore = true)
    })
    RouteEntity apiToEntity(Route api);

    List<Route> entityListToApiList(List<RouteEntity> entity);
    List<RouteEntity> apiListToEntityList(List<Route> api);

}
