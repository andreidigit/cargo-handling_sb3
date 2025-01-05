package com.example.microservices.core.cargo;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.microservices.core.cargo.services.CargoMapper;
import com.example.mutual.api.core.cargo.Cargo;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class MapperTests {

    private CargoMapper mapper = Mappers.getMapper(CargoMapper.class);

    @Test
    void mapperTests() {

        assertNotNull(mapper);

        Cargo cargo1 = new Cargo(1, "n", 1000, Cargo.Status.STOCK, "sa");

        CargoEntity cargoEntity = mapper.apiToEntity(cargo1);

        assertEquals(cargo1.getCargoId(), cargoEntity.getCargoId());
        assertEquals(cargo1.getName(), cargoEntity.getName());
        assertEquals(cargo1.getWeight(), cargoEntity.getWeight());
        assertEquals(cargo1.getStatus(), cargoEntity.getStatus());

        Cargo cargo2 = mapper.entityToApi(cargoEntity);

        assertEquals(cargo1.getCargoId(), cargo2.getCargoId());
        assertEquals(cargo1.getName(), cargo2.getName());
        assertEquals(cargo1.getWeight(), cargo2.getWeight());
        assertEquals(cargo1.getStatus(), cargo2.getStatus());
        assertNull(cargo2.getServiceAddress());

        CargoEntity clonedEntity = mapper.cloneCargoEntity(cargoEntity);

        assertEquals(cargoEntity.getCargoId(), clonedEntity.getCargoId());
        assertEquals(cargoEntity.getName(), clonedEntity.getName());
        assertEquals(cargoEntity.getWeight(), clonedEntity.getWeight());
        assertEquals(cargoEntity.getStatus(), clonedEntity.getStatus());
        assertEquals(cargoEntity.getCreatedOn(), clonedEntity.getCreatedOn());
    }

}
