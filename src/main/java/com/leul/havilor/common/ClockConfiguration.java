package com.leul.havilor.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.util.Optional;

// The one source of "now" for the whole app. Code asks this Clock instead of calling Instant.now(),
// so tests can swap in a fixed clock and assert exact timestamps.
// JPA auditing (@CreatedDate / @LastModifiedDate on entities) reads the same Clock through the DateTimeProvider.
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
