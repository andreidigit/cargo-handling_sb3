package com.example.microservices.core.route.broker;


import com.example.microservices.core.route.persistence.RouteEntity;
import com.example.mutual.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static com.example.mutual.api.event.Event.Type.*;

@Slf4j
@Configuration
public class ProducerRevise {

    private final StreamBridge streamBridge;
    private final String bindingName;

    @Autowired
    public ProducerRevise(
            @Value("${channelsOut.revise.bind}") String bindingName,
            StreamBridge streamBridge
    ) {
        this.streamBridge = streamBridge;
        this.bindingName = bindingName;
    }

    public void routeCreated(RouteEntity routeEntity) {
        sendMessage(bindingName, new Event<>(CREATE, routeEntity.getRouteId(), routeEntity));
    }

    public void routeUpdated(RouteEntity routeEntity) {
        sendMessage(bindingName, new Event<>(UPDATE, routeEntity.getRouteId(), routeEntity));
    }

    public void routeDeleted(RouteEntity routeEntity) {
        sendMessage(bindingName, new Event<>(DELETE, routeEntity.getRouteId(), routeEntity));
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
