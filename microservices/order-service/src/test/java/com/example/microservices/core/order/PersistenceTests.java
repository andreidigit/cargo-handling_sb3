package com.example.microservices.core.order;

import com.example.microservices.core.order.persistence.OrderEntity;
import com.example.microservices.core.order.persistence.OrderRepository;
import com.example.microservices.core.order.util.ContainersTestBase;
import com.example.mutual.api.core.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
@Transactional(propagation = NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PersistenceTests extends ContainersTestBase {
    @Autowired
    private OrderRepository repository;
    private OrderEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        OrderEntity entity = new OrderEntity(1, 1, 1, 2, Order.Status.NEW);
        savedEntity = repository.save(entity);

        assertEqualsStore(entity, savedEntity);
    }

    @Test
    void create() {

        OrderEntity newEntity = new OrderEntity(2, 2,1, 2, Order.Status.NEW);
        repository.save(newEntity);

        OrderEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsStore(newEntity, foundEntity);

        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setStatus(Order.Status.COMPLETED);
        repository.save(savedEntity);

        OrderEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (long) foundEntity.getVersion());
        assertEquals(Order.Status.COMPLETED, foundEntity.getStatus());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByStoreId() {
        Optional<OrderEntity> entityById = repository.findByOrderId(savedEntity.getOrderId());

        assertTrue(entityById.isPresent());
        assertEqualsStore(savedEntity, entityById.get());
    }

    @Test
    void duplicateError() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            OrderEntity entity = new OrderEntity(1, 1,1,2, Order.Status.COMPLETED);
            repository.save(entity);
        });

    }

    @Test
    void optimisticLockError() {
        OrderEntity entity1 = repository.findById(savedEntity.getId()).get();
        OrderEntity entity2 = repository.findById(savedEntity.getId()).get();
        entity1.setStatus(Order.Status.TRANSIT);
        repository.save(entity1);

        // Ожидаем что со старой версией блокировки не запишет
        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setStatus(Order.Status.COMPLETED);
            repository.save(entity2);
        });
        OrderEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals(Order.Status.TRANSIT, updatedEntity.getStatus());
    }

    private void assertEqualsStore(OrderEntity expectedEntity, OrderEntity actualEntity) {
        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedEntity.getVersion(), actualEntity.getVersion());
        assertEquals(expectedEntity.getOrderId(), actualEntity.getOrderId());
        assertEquals(expectedEntity.getCargoId(), actualEntity.getCargoId());
        assertEquals(expectedEntity.getFromStoreId(), actualEntity.getFromStoreId());
        assertEquals(expectedEntity.getToStoreId(), actualEntity.getToStoreId());
        assertEquals(expectedEntity.getStatus(), actualEntity.getStatus());
    }
}
