package com.leul.havilor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

// Tests run at a fixed moment, so timestamps (createdAt, closedAt, completedAt, error timestamps) are exact.
// @Primary: wins over the real Clock bean from ClockConfiguration.
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    public static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Bean
    @Primary
    Clock fixedClock() {
        return CLOCK;
    }
}
