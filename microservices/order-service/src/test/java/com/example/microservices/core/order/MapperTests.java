package com.example.microservices.core.order;

import com.example.microservices.core.order.persistence.OrderEntity;
import com.example.microservices.core.order.services.OrderMapper;
import com.example.mutual.api.core.order.Order;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class MapperTests {

    private OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void mapperTests() {

        assertNotNull(mapper);

        Order order1 = new Order(1, 1, 1,2, Order.Status.NEW, "sa");

        OrderEntity orderEntity = mapper.apiToEntity(order1);

        assertEquals(order1.getOrderId(), orderEntity.getOrderId());
        assertEquals(order1.getCargoId(), orderEntity.getCargoId());
        assertEquals(order1.getFromStoreId(), orderEntity.getFromStoreId());
        assertEquals(order1.getToStoreId(), orderEntity.getToStoreId());
        assertEquals(order1.getStatus(), orderEntity.getStatus());

        Order order2 = mapper.entityToApi(orderEntity);

        assertEquals(order1.getOrderId(), order2.getOrderId());
        assertEquals(order1.getCargoId(), order2.getCargoId());
        assertEquals(order1.getFromStoreId(), order2.getFromStoreId());
        assertEquals(order1.getFromStoreId(), order2.getFromStoreId());
        assertEquals(order1.getToStoreId(), order2.getToStoreId());
        assertEquals(order1.getStatus(), order2.getStatus());
        assertNull(order2.getServiceAddress());

        OrderEntity clonedEntity = mapper.cloneOrderEntity(orderEntity);

        assertEquals(orderEntity.getOrderId(), clonedEntity.getOrderId());
        assertEquals(orderEntity.getCargoId(), clonedEntity.getCargoId());
        assertEquals(orderEntity.getFromStoreId(), clonedEntity.getFromStoreId());
        assertEquals(orderEntity.getToStoreId(), clonedEntity.getToStoreId());
        assertEquals(orderEntity.getStatus(), clonedEntity.getStatus());
        assertEquals(orderEntity.getCreatedOn(), clonedEntity.getCreatedOn());
    }

}
