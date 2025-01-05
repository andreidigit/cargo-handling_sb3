package com.example.microservices.core.cargo.broker;


import com.example.microservices.core.cargo.persistence.CargoEntity;
import com.example.mutual.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static com.example.mutual.api.event.Event.Type.*;

@Slf4j
@Component
public class ProducerRevise {

    private final StreamBridge streamBridge;
    private final String bindingName = "cargo-revise-out-0";

    @Autowired
    public ProducerRevise(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void cargoCreated(CargoEntity cargoEntity) {
        sendMessage(bindingName, new Event<>(CREATE, cargoEntity.getCargoId(), cargoEntity));
    }

    public void cargoUpdated(CargoEntity cargoEntity) {
        sendMessage(bindingName, new Event<>(UPDATE, cargoEntity.getCargoId(), cargoEntity));
    }

    public void cargoDeleted(CargoEntity cargoEntity) {
        sendMessage(bindingName, new Event<>(DELETE, cargoEntity.getCargoId(), cargoEntity));
    }

    private void sendMessage(String bindingName, Event event) {
        log.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        log.debug("Sent success: {}, Name: {}, Event: {}",
                streamBridge.send(bindingName, message),
                bindingName,
                event.getEventType()
        );
    }
}
