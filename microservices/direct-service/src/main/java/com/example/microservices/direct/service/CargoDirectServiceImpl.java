package com.example.microservices.direct.service;

import com.example.microservices.direct.util.AuthLog;
import com.example.mutual.api.core.cargo.Cargo;
import com.example.mutual.api.direct.CargoDirectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class CargoDirectServiceImpl implements CargoDirectService {
    private final AuthLog authLog;
    private final CargoDirectIntegration integration;

    @Autowired
    public CargoDirectServiceImpl(AuthLog authLog, CargoDirectIntegration integration) {
        this.authLog = authLog;
        this.integration = integration;
    }

    @Override
    public Mono<Cargo> getCargo(int cargoId) {
        return integration.getCargo(cargoId)
                .doOnError(ex -> log.warn("getCargo failed: {}", ex.toString()))
                .log(log.getName(), FINE);
    }

    @Override
    public Mono<Void> createCargo(Cargo body) {
        try {
            log.debug("create a new cargo for cargoId: {}", body.getCargoId());
            Cargo cargo = new Cargo(body.getCargoId(), body.getName(), body.getWeight(), body.getStatus(), body.getServiceAddress());

            return integration.createCargo(cargo)
                    .doOnError(ex -> log.warn("createCargo failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createCargo failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> updateCargo(Cargo body) {
        try {
            log.debug("update the cargo for cargoId: {}", body.getCargoId());
            Cargo cargo = new Cargo(body.getCargoId(), body.getName(), body.getWeight(), body.getStatus(), body.getServiceAddress());
            cargo.setStatus(body.getStatus());
            return integration.updateCargo(cargo)
                    .doOnError(ex -> log.warn("createCargo failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createCargo failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> deleteCargo(int cargoId) {
        return authLog.getLogAuthorizationInfoMono()
                .flatMap(sc -> integration.deleteCargo(cargoId)
                        .doOnError(ex -> log.warn("deleteCargo failed: {}", ex.toString()))
                        .onErrorResume(Mono::error)
                        .then()
                );
    }
}
