package com.example.microservices.core.route.broker;


import com.example.mutual.api.core.route.RouteTaskPayload;
import com.example.mutual.api.event.EventTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
@Configuration
public class ProducerTask {

    private final StreamBridge streamBridge;
    private final String bindingName = "routeFind-out-0";

    @Autowired
    public ProducerTask(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void routeFound(RouteTaskPayload payload) {
        sendMessage(bindingName, new EventTask<>(EventTask.Type.ROUTE_FOUND, payload.getOrderId(), payload));
    }

    private void sendMessage(String bindingName, EventTask<Integer, RouteTaskPayload> event) {
        log.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message<EventTask<Integer, RouteTaskPayload>> message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        log.debug("Sent success: {}, Name: {}, Event: {}",
                streamBridge.send(bindingName, message),
                bindingName,
                event.getEventType()
        );
    }
}
