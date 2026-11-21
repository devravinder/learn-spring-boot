package com.paravar.metrics.config;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    // drop meters we never chart; fewer series means cheaper storage
    @Bean
    MeterFilter denyTomcatSessions() {
        return MeterFilter.denyNameStartsWith("tomcat.sessions");
    }

    // guard rail: after 20 distinct users, new tag values are dropped instead of exploding series
    @Bean
    MeterFilter capUserTag() {
        return MeterFilter.maximumAllowableTags("shop.visits", "user", 20, MeterFilter.deny());
    }

    // rename a tag without touching call sites
    @Bean
    MeterFilter renamePaymentTag() {
        return MeterFilter.renameTag("shop.orders.placed", "payment", "payment.method");
    }
}
