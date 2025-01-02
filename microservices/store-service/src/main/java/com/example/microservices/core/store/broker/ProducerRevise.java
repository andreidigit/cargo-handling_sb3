package com.example.microservices.core.store.broker;


import com.example.microservices.core.store.persistence.StoreEntity;
import com.example.mutual.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static com.example.mutual.api.event.Event.Type.*;

@Slf4j
@Component
public class ProducerRevise {

    private final StreamBridge streamBridge;
    private final String bindingName;

    @Autowired
    public ProducerRevise(
            @Value("${channelsOut.revise.bind}") String bindingName,
            StreamBridge streamBridge
    ) {
        this.bindingName = bindingName;
        this.streamBridge = streamBridge;
    }

    public void storeCreated(StoreEntity storeEntity) {
        sendMessage(bindingName, new Event<>(CREATE, storeEntity.getStoreId(), storeEntity));
    }

    public void storeUpdated(StoreEntity storeEntity) {
        sendMessage(bindingName, new Event<>(UPDATE, storeEntity.getStoreId(), storeEntity));
    }

    public void storeDeleted(StoreEntity storeEntity) {
        sendMessage(bindingName, new Event<>(DELETE, storeEntity.getStoreId(), storeEntity));
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
