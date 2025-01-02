package com.example.microservices.core.store;

import com.example.microservices.core.store.persistence.StoreEntity;
import com.example.microservices.core.store.persistence.StoreRepository;
import com.example.microservices.core.store.util.ContainersTestBase;
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
    private StoreRepository repository;
    private StoreEntity savedEntity;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();

        StoreEntity entity = new StoreEntity(1, "a", 200, 1);
        savedEntity = repository.save(entity);

        assertEqualsStore(entity, savedEntity);
    }

    @Test
    void create() {

        StoreEntity newEntity = new StoreEntity(2, "a", 200, 1);
        repository.save(newEntity);

        StoreEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsStore(newEntity, foundEntity);

        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setLocation("a2");
        repository.save(savedEntity);

        StoreEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, (long)foundEntity.getVersion());
        assertEquals("a2", foundEntity.getLocation());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByStoreId() {
        Optional<StoreEntity> byStoreId = repository.findByStoreId(savedEntity.getStoreId());

        assertTrue(byStoreId.isPresent());
        assertEqualsStore(savedEntity, byStoreId.get());
    }

    @Test
    void duplicateError() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            StoreEntity entity = new StoreEntity(1, "a", 200, 1);
            repository.save(entity);
        });

    }

    @Test
    void optimisticLockError() {
        StoreEntity entity1 = repository.findById(savedEntity.getId()).get();
        StoreEntity entity2 = repository.findById(savedEntity.getId()).get();
        entity1.setLocation("a1");
        repository.save(entity1);

        // Ожидаем что со старой версией блокировки не запишет
        assertThrows(OptimisticLockingFailureException.class, () -> {
            entity2.setLocation("a2");
            repository.save(entity2);
        });
        StoreEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("a1", updatedEntity.getLocation());
    }

    private void assertEqualsStore(StoreEntity expectedEntity, StoreEntity actualEntity) {
        assertEquals(expectedEntity.getId(),        actualEntity.getId());
        assertEquals(expectedEntity.getVersion(),   actualEntity.getVersion());
        assertEquals(expectedEntity.getStoreId(),   actualEntity.getStoreId());
        assertEquals(expectedEntity.getLocation(),   actualEntity.getLocation());
        assertEquals(expectedEntity.getCapacity(),   actualEntity.getCapacity());
        assertEquals(expectedEntity.getUsedCapacity(),   actualEntity.getUsedCapacity());
    }
}
