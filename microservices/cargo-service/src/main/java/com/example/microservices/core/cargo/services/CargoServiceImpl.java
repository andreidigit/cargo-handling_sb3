package com.example.microservices.core.cargo.services;

import com.example.microservices.core.cargo.broker.ProducerRevise;
import com.example.microservices.core.cargo.invariant.RuleCargoDelete;
import com.example.microservices.core.cargo.invariant.RuleCargoUpdate;
import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.microservices.core.cargo.persistence.CargoRepository;
import com.example.mutual.api.core.cargo.Cargo;
import com.example.mutual.api.core.cargo.CargoService;
import com.example.mutual.api.exceptions.InvalidInputException;
import com.example.mutual.api.exceptions.NotFoundException;
import com.example.mutual.util.http.ServiceUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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
public class CargoServiceImpl implements CargoService {

    private final CargoRepository repository;
    private final CargoMapper mapper;
    private final ServiceUtil serviceUtil;
    private final Scheduler jdbcScheduler;
    private final ProducerRevise producer;
    private final List<RuleCargoUpdate> rulesUpdate;
    private final List<RuleCargoDelete> rulesDelete;

    @Autowired
    public CargoServiceImpl(
            CargoRepository repository,
            CargoMapper mapper,
            ServiceUtil serviceUtil,
            @Qualifier("jdbcScheduler") Scheduler jdbcScheduler,
            ProducerRevise producer,
            List<RuleCargoUpdate> rulesUpdate,
            List<RuleCargoDelete> rulesDelete
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
        this.jdbcScheduler = jdbcScheduler;
        this.producer = producer;
        this.rulesUpdate = rulesUpdate;
        this.rulesDelete = rulesDelete;
    }

    @Override
    public Mono<Cargo> getCargo(int cargoId) {
        if (cargoId < 1) {
            throw new InvalidInputException("Invalid cargoId: " + cargoId);
        }
        log.info("Will get cargo with id={}", cargoId);
        return Mono.fromCallable(() -> internalGetStore(cargoId))
                .switchIfEmpty(Mono.error(new NotFoundException("No cargo found for cargoId: " + cargoId)))
                .log(log.getName(), FINE)
                .subscribeOn(jdbcScheduler);
    }

    private Cargo internalGetStore(int cargoId) {
        return repository.findByCargoId(cargoId)
                .map(cargoEntity -> {
                    Cargo cargo = mapper.entityToApi(cargoEntity);
                    cargo.setServiceAddress(serviceUtil.getServiceAddress());
                    return cargo;
                })
                .orElse(null);
    }

    @Transactional
    @Override
    public Mono<Cargo> createCargo(Cargo body) {
        if (body.getCargoId() < 1) {
            throw new InvalidInputException("Invalid cargoId: " + body.getCargoId());
        }
        return Mono.fromCallable(()-> internalCreateCargo(body))
                .subscribeOn(jdbcScheduler);
    }

    private Cargo internalCreateCargo(Cargo body) {
        try {
            CargoEntity entity = mapper.apiToEntity(body);
            CargoEntity newEntity = repository.save(entity);
            producer.cargoCreated(newEntity);
            log.debug("createCargo: created a cargo entity: {}", body.getCargoId());
            return mapper.entityToApi(newEntity);
        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Cargo Id: " + body.getCargoId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Cargo> updateCargo(Cargo body) {
        if (body.getCargoId() < 1) {
            throw new InvalidInputException("Invalid cargoId: " + body.getCargoId());
        }
        return Mono.fromCallable(()-> internalUpdateCargo(body))
                .subscribeOn(jdbcScheduler);
    }
    private Cargo internalUpdateCargo(Cargo body) {
        Optional<CargoEntity> entityOpt = repository.findByCargoId(body.getCargoId());
        if (entityOpt.isPresent()) {
            CargoEntity entity = entityOpt.get();
            CargoEntity oldCargoEntity = mapper.cloneCargoEntity(entity);
            boolean isBrokenRule = rulesUpdate.stream()
                    .map(rule -> rule.apply(entity, mapper.apiToEntity(body)))
                    .anyMatch(bool -> !bool);
            if (isBrokenRule) {
                log.warn("updateCargo: there is broken Rule of entity cargoId: {}", body.getCargoId());
                throw new InvalidInputException("There is a broken Update Rule, Cargo Id: " + body.getCargoId());
            }
            repository.save(entity);
            producer.cargoUpdated(oldCargoEntity);
            log.debug("updateCargo: updated a cargo entity: {}", body.getCargoId());
            return mapper.entityToApi(entity);
        } else {
            throw new NotFoundException("There is no Cargo with cargoId: " + body.getCargoId());
        }
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    @Override
    public Mono<Void> deleteCargo(int cargoId) {
        return Mono.fromRunnable(()->internalDeleteCargo(cargoId)).subscribeOn(jdbcScheduler).then();
    }

    private void internalDeleteCargo(int cargoId) {
        log.debug("deleteCargo: tries to delete with cargoId: {}", cargoId);
        repository.findByCargoId(cargoId).ifPresent(
            cargoEntity->{
                boolean isBrokenRule = rulesDelete.stream()
                    .map(rule -> rule.check(cargoEntity))
                    .anyMatch(bool -> !bool);
                if (isBrokenRule) {
                    log.warn("deleteCargo: there is broken Rule of entity cargoId: {}", cargoId);
                    throw new InvalidInputException("There is a broken Delete Rule, cargo Id: " + cargoId);
                }
                repository.delete(cargoEntity);
                producer.cargoDeleted(cargoEntity);
            }
        );
    }
}
