package com.example.microservices.core.order.services;

import com.example.microservices.core.order.persistence.OrderEntity;
import com.example.mutual.api.core.order.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderEntity cloneOrderEntity(OrderEntity orderEntity);
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Order entityToApi(OrderEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "updatedOn", ignore = true),
            @Mapping(target = "createdOn", ignore = true)
    })
    OrderEntity apiToEntity(Order api);

    List<Order> entityListToApiList(List<OrderEntity> entity);
    List<OrderEntity> apiListToEntityList(List<Order> api);

}
