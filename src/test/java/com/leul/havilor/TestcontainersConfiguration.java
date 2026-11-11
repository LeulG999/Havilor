package com.leul.havilor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Every database test imports this: a throwaway PostgreSQL 18 with pgvector, and a Redis, in Docker (as compose.yaml).
// @ServiceConnection points Spring's datasource (and so Flyway) at the container, overriding application.yml.
// The container is static, so all test contexts in one run share it: started once, migrated once.
// Tests no longer need `docker compose up` and never touch the dev database; Docker must be running.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // The pgvector image is PostgreSQL plus one extension, so Testcontainers may treat it as "postgres".
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"));

    // Redis for the playbook cache (Flow 21), the same image as compose.yaml.
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8").withExposedPorts(6379);

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return REDIS;
    }
}
