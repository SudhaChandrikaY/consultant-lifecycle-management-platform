package com.ensar.clmp.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fixes "now" at 2026-10-15T15:00:00Z (11:00 in America/New_York) for deterministic date rules. */
@TestConfiguration
public class FixedClockConfig {

    public static final Instant NOW = Instant.parse("2026-10-15T15:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.of("America/New_York"));
    }
}
