package com.paravar.metrics.reports;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final LongTaskTimer generation;

    public ReportService(MeterRegistry registry) {
        // a LongTaskTimer shows in-flight work (active count, running duration), not just finished calls
        this.generation = LongTaskTimer.builder("shop.report.generation")
                .description("Report generation in progress")
                .register(registry);
    }

    @Async
    public void generate(int seconds) {
        generation.record(() -> {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
