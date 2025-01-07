package com.example.mutual.util.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JacksonLocalDateTime {
    /**
     *    Jackson Error: Java 8 date/time type not supported by default LocalDate, LocalTime, LocalDateTime
     *    add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling or use
     *    spring.jackson.deserialization.adjust-dates-to-context-time-zone: true
     */
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
