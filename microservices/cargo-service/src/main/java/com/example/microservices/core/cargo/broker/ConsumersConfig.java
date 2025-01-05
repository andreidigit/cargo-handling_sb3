package com.example.microservices.core.cargo.broker;

import com.example.mutual.api.core.cargo.Cargo;
import com.example.mutual.api.core.cargo.CargoService;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class ConsumersConfig {

    private final CargoService cargoService;

    @Autowired
    public ConsumersConfig(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @Bean
    public Consumer<Event<Integer, Cargo>> consumerCrud() {
        return event -> {
            log.info("Process message created at {}...", event.getEventCreatedAt());
            Cargo store;

            switch (event.getEventType()) {

                case CREATE:
                    store = event.getData();
                    log.info("Create store with ID: {}", store.getCargoId());
                    cargoService.createCargo(store).block();
                    break;

                case UPDATE:
                    store = event.getData();
                    log.info("Update store with storeID: {}", store.getCargoId());
                    cargoService.updateCargo(store).block();
                    break;

                case DELETE:
                    log.info("Delete store with storeID: {}", event.getKey());
                    cargoService.deleteCargo(event.getKey()).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }

            log.info("Message processing done!");
        };
    }
}
