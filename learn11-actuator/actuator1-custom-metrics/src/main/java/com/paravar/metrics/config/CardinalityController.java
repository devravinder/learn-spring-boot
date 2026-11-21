package com.paravar.metrics.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// anti-pattern on purpose: a user id as a tag creates one time series per user
@RestController
public class CardinalityController {

    private final MeterRegistry registry;

    public CardinalityController(MeterRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/visits/{user}")
    public Map<String, Object> visit(@PathVariable String user) {
        registry.counter("shop.visits", "user", user).increment();
        long series = registry.find("shop.visits").counters().size();
        return Map.of("user", user, "series", series);
    }
}
