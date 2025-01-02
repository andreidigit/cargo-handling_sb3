package com.example.microservices.core.store;

import com.example.microservices.core.store.persistence.StoreEntity;
import com.example.microservices.core.store.services.StoreMapper;
import com.example.mutual.api.core.store.Store;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class MapperTests {

    private StoreMapper mapper = Mappers.getMapper(StoreMapper.class);

    @Test
    void mapperTests() {

        assertNotNull(mapper);

        Store store1 = new Store(1, "n", 1000, 0, "sa");

        StoreEntity storeEntity = mapper.apiToEntity(store1);

        assertEquals(store1.getStoreId(), storeEntity.getStoreId());
        assertEquals(store1.getLocation(), storeEntity.getLocation());
        assertEquals(store1.getCapacity(), storeEntity.getCapacity());
        assertEquals(store1.getUsedCapacity(), storeEntity.getUsedCapacity());

        Store store2 = mapper.entityToApi(storeEntity);

        assertEquals(store1.getStoreId(), store2.getStoreId());
        assertEquals(store1.getLocation(), store2.getLocation());
        assertEquals(store1.getCapacity(), store2.getCapacity());
        assertEquals(store1.getUsedCapacity(), store2.getUsedCapacity());
        assertNull(store2.getServiceAddress());

        StoreEntity clonedEntity = mapper.cloneStoreEntity(storeEntity);

        assertEquals(storeEntity.getStoreId(), clonedEntity.getStoreId());
        assertEquals(storeEntity.getLocation(), clonedEntity.getLocation());
        assertEquals(storeEntity.getCapacity(), clonedEntity.getCapacity());
        assertEquals(storeEntity.getUsedCapacity(), clonedEntity.getUsedCapacity());
        assertEquals(storeEntity.getCreatedOn(), clonedEntity.getCreatedOn());
    }

}
