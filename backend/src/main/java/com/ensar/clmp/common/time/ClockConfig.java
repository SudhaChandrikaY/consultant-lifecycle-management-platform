package com.ensar.clmp.common.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The organization clock (research R15). Tests replace it with a fixed clock. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${clmp.time-zone:America/New_York}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
