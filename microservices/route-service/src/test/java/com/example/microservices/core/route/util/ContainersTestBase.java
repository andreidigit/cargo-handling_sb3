package com.example.microservices.core.route.util;

import net.bytebuddy.utility.RandomString;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class ContainersTestBase {

    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = RandomString.make(10);
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2.4-alpine");

    public static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD);

    public static PostgreSQLContainer<?> DB_CONTAINER =
            new PostgreSQLContainer<>("postgres:11.22-alpine3.19")
                    .withStartupTimeoutSeconds(500);

    static {
        REDIS_CONTAINER.start();
        DB_CONTAINER.start();
    }


    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(REDIS_PORT));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);

        registry.add("spring.datasource.url", DB_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", DB_CONTAINER::getUsername);
        registry.add("spring.datasource.password", DB_CONTAINER::getPassword);
    }
}
