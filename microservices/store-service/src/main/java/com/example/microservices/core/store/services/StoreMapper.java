package com.example.microservices.core.store.services;

import com.example.microservices.core.store.persistence.StoreEntity;
import com.example.mutual.api.core.store.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    StoreEntity cloneStoreEntity(StoreEntity storeEntity);
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Store entityToApi(StoreEntity entity);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "updatedOn", ignore = true),
            @Mapping(target = "createdOn", ignore = true)
    })
    StoreEntity apiToEntity(Store api);

    List<Store> entityListToApiList(List<StoreEntity> entity);
    List<StoreEntity> apiListToEntityList(List<Store> api);

}
