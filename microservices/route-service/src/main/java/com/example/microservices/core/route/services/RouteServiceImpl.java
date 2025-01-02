package com.example.microservices.core.route.services;

import com.example.microservices.core.route.broker.ProducerRevise;
import com.example.microservices.core.route.broker.ProducerTask;
import com.example.microservices.core.route.invariant.RuleRouteSelect;
import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.microservices.core.route.persistence.RouteRepository;
import com.example.mutual.api.core.route.Route;
import com.example.mutual.api.core.route.RouteService;
import com.example.mutual.api.core.route.RouteTaskPayload;
import com.example.mutual.api.core.route.RouteTaskService;
import com.example.mutual.api.exceptions.InvalidInputException;
import com.example.mutual.api.exceptions.NotFoundException;
import com.example.mutual.util.http.ServiceUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.Optional;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class RouteServiceImpl implements RouteService, RouteTaskService {

    private final RouteMapper mapper;
    private final ServiceUtil serviceUtil;
    private final Scheduler jdbcScheduler;
    private final ProducerRevise producer;
    private final ProducerTask producerTask;
    private final List<RuleRouteSelect> rulesSelect;
    private final RouteServiceCacheable serviceRepoCacheable;

    @Autowired
    public RouteServiceImpl(
            RouteRepository repository,
            RouteMapper mapper,
            ServiceUtil serviceUtil,
            @Qualifier("jdbcScheduler") Scheduler jdbcScheduler,
            ProducerRevise producer, ProducerTask producerTask,
            List<RuleRouteSelect> rulesSelect, RouteServiceCacheable serviceRepoCacheable
    ) {
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
        this.jdbcScheduler = jdbcScheduler;
        this.producer = producer;
        this.producerTask = producerTask;
        this.rulesSelect = rulesSelect;
        this.serviceRepoCacheable = serviceRepoCacheable;
    }

    @Override
    public Mono<Route> getRoute(int routeId) {
        if (routeId < 1) {
            throw new InvalidInputException("Invalid routeId: " + routeId);
        }
        log.info("Will get route with id={}", routeId);
        return Mono.fromCallable(() -> internalGetRoute(routeId))
                .switchIfEmpty(Mono.error(new NotFoundException("No route found for routeId: " + routeId)))
                .log(log.getName(), FINE)
                .subscribeOn(jdbcScheduler);
    }

    private Route internalGetRoute(int routeId) {
        return serviceRepoCacheable.findByRouteId(routeId)
                .map(routeEntity -> {
                    Route route = mapper.entityToApi(routeEntity);
                    route.setServiceAddress(serviceUtil.getServiceAddress());
                    return route;
                })
                .orElse(null);
    }

    @Override
    public Mono<Route> findRoute(RouteTaskPayload payload) {
        return Mono.fromCallable(() -> internalFindRoute(payload))
                .switchIfEmpty(
                        Mono.error(new NotFoundException("No route found for storeId={"
                                + payload.getFromStoreId() + "} from and storeId={" + payload.getToStoreId() + "} to."))
                )
                .log(log.getName(), FINE)
                .subscribeOn(jdbcScheduler);
    }

    private Route internalFindRoute(RouteTaskPayload payload) {
        List<RouteEntity> routes = serviceRepoCacheable
                .findByFromStoreIdAndToStoreId(payload.getFromStoreId(), payload.getToStoreId());
        return rulesSelect.stream()
                .map(rule -> rule.find(routes, payload.getRuleType()))
                .filter(Optional::isPresent)
                .map(routeOpt -> {
                    Route route = mapper.entityToApi(routeOpt.get());
                    route.setServiceAddress(serviceUtil.getServiceAddress());
                    payload.setRoute(route);
                    producerTask.routeFound(payload);
                    return route;
                })
                .findFirst()
                .orElse(null);
    }

    @Transactional
    @Override
    public Mono<Route> createRoute(Route body) {
        if (body.getRouteId() < 1) {
            throw new InvalidInputException("Invalid routeId: " + body.getRouteId());
        }
        return Mono.fromCallable(() -> internalCreateRoute(body))
                .subscribeOn(jdbcScheduler);
    }

    private Route internalCreateRoute(Route body) {
        try {
            RouteEntity entity = mapper.apiToEntity(body);
            Optional<RouteEntity> newEntity = serviceRepoCacheable.save(entity);
            producer.routeCreated(newEntity.orElseThrow());
            log.debug("createRoute: created a route entity: {}", body.getRouteId());
            return mapper.entityToApi(newEntity.get());
        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Route Id: " + body.getRouteId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Route> updateRoute(Route body) {
        if (body.getRouteId() < 1) {
            throw new InvalidInputException("Invalid routeId: " + body.getRouteId());
        }
        return Mono.fromCallable(() -> internalUpdateRoute(body))
                .switchIfEmpty(
                        Mono.error(new NotFoundException("There is no Route with routeId: " + body.getRouteId()))
                )
                .subscribeOn(jdbcScheduler);
    }

    private Route internalUpdateRoute(Route body) {
        return serviceRepoCacheable
                .findByRouteId(body.getRouteId())
                .map(
                        entity -> {
                            RouteEntity oldRouteEntity = mapper.cloneRouteEntity(entity);
                            BeanUtils.copyProperties(body, entity, "routeId");
                            serviceRepoCacheable.save(entity);
                            producer.routeUpdated(oldRouteEntity);
                            log.debug("updateRoute: updated a route entity: {}", body.getRouteId());
                            return mapper.entityToApi(entity);
                        }
                )
                .orElse(null);
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Void> deleteRoute(int routeId) {
        return Mono.fromRunnable(() -> internalDeleteRoute(routeId)).subscribeOn(jdbcScheduler).then();
    }

    private void internalDeleteRoute(int routeId) {
        log.debug("deleteRoute: tries to delete with routeId: {}", routeId);
        serviceRepoCacheable.findByRouteId(routeId).ifPresent(
                routeEntity -> {
                    serviceRepoCacheable.delete(routeEntity);
                    producer.routeDeleted(routeEntity);
                }
        );
    }
}
