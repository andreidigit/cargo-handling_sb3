package com.example.microservices.core.route;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.persistence.RouteRepository;
import com.example.microservices.core.route.util.ContainersTestBase;
import com.example.microservices.core.route.util.RouteEntityHelper;
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
    private RouteRepository repository;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
    }

    @Test
    void testCreate() {
        int routeId = 1;
        RouteEntity newEntity = RouteEntityHelper.getEntity(routeId);
        repository.save(newEntity);

        RouteEntity foundEntity = repository.findById(newEntity.getId()).get();
        RouteEntityHelper.assertEqualsRoute(newEntity, foundEntity);
    }

    @Test
    void testUpdate() {
        int routeId = 2;
        RouteEntity newEntity = RouteEntityHelper.getEntity(routeId);
        repository.save(newEntity);
        RouteEntity foundEntity = repository.findById(newEntity.getId()).get();

        foundEntity.setDistanceFromTo(99);
        repository.save(foundEntity);

        foundEntity = repository.findById(foundEntity.getId()).get();
        assertEquals(1, (long)foundEntity.getVersion());
        assertEquals(99, foundEntity.getDistanceFromTo());
    }

    @Test
    void testDelete() {
        int routeId = 3;
        RouteEntity newEntity = RouteEntityHelper.getEntity(routeId);
        repository.save(newEntity);
        RouteEntity foundEntity = repository.findById(newEntity.getId()).get();
        repository.delete(foundEntity);
        assertFalse(repository.existsById(foundEntity.getId()));
    }

    @Test
    void testGetByRouteId() {
        int routeId = 4;
        RouteEntity newEntity = RouteEntityHelper.getEntity(routeId);
        repository.save(newEntity);

        Optional<RouteEntity> byRouteId = repository.findByRouteId(newEntity.getRouteId());

        assertTrue(byRouteId.isPresent());
        RouteEntityHelper.assertEqualsRoute(newEntity, byRouteId.get());
    }

    @Test
    void duplicateError() {
        int routeId = 5;
        int routeIdNext = 6;
        RouteEntity newEntity = RouteEntityHelper.getEntity(routeId);
        repository.save(newEntity);
        // не должно сохранять новую сущность с тем же routeId
        assertThrows(DataIntegrityViolationException.class, () -> {
            RouteEntity entity = RouteEntityHelper.getEntity(routeIdNext);
            entity.setRouteId(routeId);
            repository.save(entity);
        });

    }

    @Test
    void optimisticLockError() {
        int routeId = 7;
        RouteEntity newEntity = RouteEntityHelper.getEntity(routeId);
        repository.save(newEntity);
        RouteEntity entity1 = repository.findById(newEntity.getId()).get();
        RouteEntity entity2 = repository.findById(newEntity.getId()).get();
        entity1.setDistanceFromTo(99);
        repository.save(entity1);

        // Версия блокировки меньше чем текущая и ожидаем что со старой версией не запишет
        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setDistanceFromTo(101);
            repository.save(entity2);
        });
        RouteEntity updatedEntity = repository.findById(newEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals(99, updatedEntity.getDistanceFromTo());
    }
}
