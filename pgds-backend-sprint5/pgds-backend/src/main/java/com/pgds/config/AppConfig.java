package com.pgds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@org.springframework.scheduling.annotation.EnableScheduling
public class AppConfig {
    /** All "current month" logic uses Indian time; injectable so tests can fix the date. */
    @Bean
    Clock clock() { return Clock.system(ZoneId.of("Asia/Kolkata")); }
}
