package com.example.microservices.core.store.broker;

import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.core.store.StoreService;
import com.example.mutual.api.event.Event;
import com.example.mutual.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.DltHandler;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class ConsumersConfig {

    private final StoreService storeService;

    @Autowired
    public ConsumersConfig(StoreService storeService) {
        this.storeService = storeService;
    }

    @Bean
    public Consumer<Event<Integer, Store>> consumerCrud() {
        return event -> {
            log.info("Process message created at {}...", event.getEventCreatedAt());
            Store store;

            switch (event.getEventType()) {

                case CREATE:
                    store = event.getData();
                    log.info("Create store with ID: {}", store.getStoreId());
                    storeService.createStore(store).block();
                    break;

                case UPDATE:
                    store = event.getData();
                    log.info("Update store with storeID: {}", store.getStoreId());
                    storeService.updateStore(store).block();
                    break;

                case DELETE:
                    log.info("Delete store with storeID: {}", event.getKey());
                    storeService.deleteStore(event.getKey()).block();
                    break;

                default:
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
            }

            log.info("Message processing done!");
        };
    }

    @DltHandler
    public void listenDLT(Event<Integer, Store> event) {
        log.error(" !-----!  DLT with event {}", event.getEventType());
    }
}
