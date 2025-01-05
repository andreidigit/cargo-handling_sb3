package com.example.microservices.direct.util;

import com.example.mutual.api.exceptions.InvalidInputException;
import com.example.mutual.api.exceptions.NotFoundException;
import com.example.mutual.util.http.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
public class CustomExceptionResolver {
    private final ObjectMapper mapper;

    public CustomExceptionResolver(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Throwable handleException(Throwable ex) {
        if (!(ex instanceof WebClientResponseException wcre)) {
            log.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }
        switch (Objects.requireNonNull(HttpStatus.resolve(wcre.getStatusCode().value()))) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                log.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                log.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }
    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
