package com.leul.havilor.common;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Turns on Spring's @Cacheable. The cache itself is Redis (spring.cache.type=redis, TTL in application.yml);
// Boot builds it. Only PlaybookService uses it (Flow 21).
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfiguration {
}
