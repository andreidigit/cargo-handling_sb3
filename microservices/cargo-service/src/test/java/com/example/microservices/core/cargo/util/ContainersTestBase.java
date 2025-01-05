package com.example.microservices.core.cargo.util;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class ContainersTestBase {

    public static PostgreSQLContainer<?> DB_CONTAINER =
            new PostgreSQLContainer<>("postgres:11.22-alpine3.19")
                    .withStartupTimeoutSeconds(500);

    static {
        DB_CONTAINER.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", DB_CONTAINER::getUsername);
        registry.add("spring.datasource.password", DB_CONTAINER::getPassword);
    }
}
