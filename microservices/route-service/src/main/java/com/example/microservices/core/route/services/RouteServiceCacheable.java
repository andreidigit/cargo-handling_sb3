package com.example.microservices.core.route.services;

import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.persistence.RouteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteServiceCacheable {
    private final RouteRepository repository;

    public RouteServiceCacheable(RouteRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "RouteService::findByRouteId", key = "#routeId")
    public Optional<RouteEntity> findByRouteId(int routeId) {
        return repository.findByRouteId(routeId);
    }

    @Cacheable(value = "RouteService::findByFromStoreIdAndToStoreId", key = "#fromStoreId + '_' + #toStoreId")
    public List<RouteEntity> findByFromStoreIdAndToStoreId(int fromStoreId, int toStoreId) {
        return repository.findByFromStoreIdAndToStoreId(fromStoreId, toStoreId);
    }

    @Caching(put = {@CachePut(value = "RouteService::findByRouteId", key = "#result.routeId")})
    @CacheEvict(
            value = "RouteService::findByFromStoreIdAndToStoreId",
            key = "#entity.fromStoreId + '_' + #entity.toStoreId"
    )
    public Optional<RouteEntity> save(RouteEntity entity) {
        return Optional.of(repository.save(entity));
    }

    @Caching(
            evict = {
                    @CacheEvict(
                            value = "RouteService::findByFromStoreIdAndToStoreId",
                            key = "#entity.fromStoreId + '_' + #entity.toStoreId"
                    ),
                    @CacheEvict(value = "RouteService::findByRouteId", key = "#entity.routeId")
            })
    public void delete(RouteEntity entity) {
        repository.delete(entity);
    }


}
