package com.example.microservices.direct;

import com.example.mutual.api.core.store.Store;
import com.example.mutual.api.event.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static com.example.microservices.direct.util.IsSameEvent.sameEventExceptCreatedAt;
import static com.example.mutual.api.event.Event.Type.CREATE;
import static com.example.mutual.api.event.Event.Type.DELETE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class IsSameEventTests {

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void testEventObjectCompare() throws JsonProcessingException {

        // Event #1 and #2 are the same event, but occurs as different times
        // Event #3 and #4 are different events
        Event<Integer, Store> event1 = new Event<>(CREATE, 1, new Store(1, "location", 10, 1, null));
        Event<Integer, Store> event2 = new Event<>(CREATE, 1, new Store(1, "location", 10, 1, null));
        Event<Integer, Store> event3 = new Event<>(DELETE, 1, null);
        Event<Integer, Store> event4 = new Event<>(CREATE, 1, new Store(2, "location", 10, 1, null));

        String event1Json = mapper.writeValueAsString(event1);

        assertThat(event1Json, is(sameEventExceptCreatedAt(event2)));
        assertThat(event1Json, not(sameEventExceptCreatedAt(event3)));
        assertThat(event1Json, not(sameEventExceptCreatedAt(event4)));
    }
}
