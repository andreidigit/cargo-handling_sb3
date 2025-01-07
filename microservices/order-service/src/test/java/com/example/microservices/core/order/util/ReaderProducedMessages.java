package com.example.microservices.core.order.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReaderProducedMessages {
    private final OutputDestination target;
    private final String bindingName;

    public ReaderProducedMessages(OutputDestination target, String bindingName) {
        this.target = target;
        this.bindingName = bindingName;
    }

    public void purgeMessages() {
        getMessages();
    }

    public List<String> getMessages() {
        List<String> messages = new ArrayList<>();
        boolean anyMoreMessages = true;

        while (anyMoreMessages) {
            Message<byte[]> message = getMessage();

            if (message == null) {
                anyMoreMessages = false;

            } else {
                messages.add(new String(message.getPayload()));
            }
        }
        return messages;
    }

    private Message<byte[]> getMessage() {
        try {
            return target.receive(0, this.bindingName);
        } catch (NullPointerException npe) {
            // If the messageQueues member variable in the target object contains no queues
            // when the receive method is called, it will cause a NPE to be thrown.
            // So we catch the NPE here and return null to indicate that no messages were found.
            log.error("getMessage() received a NPE with binding = {}", this.bindingName);
            return null;
        }
    }
}
