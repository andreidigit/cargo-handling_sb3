package com.example.microservices.core.cargo;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.microservices.core.cargo.persistence.CargoRepository;
import com.example.microservices.core.cargo.util.ContainersTestBase;
import com.example.mutual.api.core.cargo.Cargo;
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
    private CargoRepository repository;
    private CargoEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        CargoEntity entity = new CargoEntity(1, "just cookies", 10, Cargo.Status.TRANSIT);
        savedEntity = repository.save(entity);

        assertEqualsStore(entity, savedEntity);
    }

    @Test
    void create() {

        CargoEntity newEntity = new CargoEntity(2, "nice cookies", 10, Cargo.Status.TRANSIT);
        repository.save(newEntity);

        CargoEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsStore(newEntity, foundEntity);

        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setName("a2");
        repository.save(savedEntity);

        CargoEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (long)foundEntity.getVersion());
        assertEquals("a2", foundEntity.getName());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByStoreId() {
        Optional<CargoEntity> entityById = repository.findByCargoId(savedEntity.getCargoId());

        assertTrue(entityById.isPresent());
        assertEqualsStore(savedEntity, entityById.get());
    }

    @Test
    void duplicateError() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            CargoEntity entity = new CargoEntity(1, "bad cookies", 10, Cargo.Status.STOCK);
            repository.save(entity);
        });

    }

    @Test
    void optimisticLockError() {
        CargoEntity entity1 = repository.findById(savedEntity.getId()).get();
        CargoEntity entity2 = repository.findById(savedEntity.getId()).get();
        entity1.setName("a1");
        repository.save(entity1);

        // Ожидаем что со старой версией блокировки не запишет
        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setName("a2");
            repository.save(entity2);
        });
        CargoEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("a1", updatedEntity.getName());
    }

    private void assertEqualsStore(CargoEntity expectedEntity, CargoEntity actualEntity) {
        assertEquals(expectedEntity.getId(),        actualEntity.getId());
        assertEquals(expectedEntity.getVersion(),   actualEntity.getVersion());
        assertEquals(expectedEntity.getCargoId(),   actualEntity.getCargoId());
        assertEquals(expectedEntity.getName(),   actualEntity.getName());
        assertEquals(expectedEntity.getWeight(),   actualEntity.getWeight());
        assertEquals(expectedEntity.getStatus(),   actualEntity.getStatus());
    }
}
