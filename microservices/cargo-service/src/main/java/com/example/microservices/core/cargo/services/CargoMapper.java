package com.example.microservices.core.cargo.services;

import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.mutual.api.core.cargo.Cargo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CargoMapper {

    CargoEntity cloneCargoEntity(CargoEntity cargoEntity);
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Cargo entityToApi(CargoEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "updatedOn", ignore = true),
            @Mapping(target = "createdOn", ignore = true)
    })
    CargoEntity apiToEntity(Cargo api);

    List<Cargo> entityListToApiList(List<CargoEntity> entity);
    List<CargoEntity> apiListToEntityList(List<Cargo> api);

}
