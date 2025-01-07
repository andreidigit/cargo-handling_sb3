package com.example.microservices.core.order.broker;


import com.example.microservices.core.order.persistence.OrderEntity;
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
    private final String bindingName = "order-revise-out-0";

    @Autowired
    public ProducerRevise(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void orderCreated(OrderEntity orderEntity) {
        sendMessage(bindingName, new Event<>(CREATE, orderEntity.getOrderId(), orderEntity));
    }

    public void orderUpdated(OrderEntity orderEntity) {
        sendMessage(bindingName, new Event<>(UPDATE, orderEntity.getOrderId(), orderEntity));
    }

    public void orderDeleted(OrderEntity orderEntity) {
        sendMessage(bindingName, new Event<>(DELETE, orderEntity.getOrderId(), orderEntity));
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
