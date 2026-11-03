package com.leul.havilor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Every database test imports this: a throwaway PostgreSQL 18 in Docker, the same major version as compose.yaml.
// @ServiceConnection points Spring's datasource (and so Flyway) at the container, overriding application.yml.
// The container is static, so all test contexts in one run share it: started once, migrated once.
// Tests no longer need `docker compose up` and never touch the dev database; Docker must be running.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
