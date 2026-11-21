package com.paravar.flaky;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// switches that decide how badly the service behaves; changed at runtime via POST /chaos
@Component
public class Chaos {

    public record Settings(double failRate, int failNext, int latencyMs, int status) {
        static Settings healthy() {
            return new Settings(0, 0, 0, 503);
        }
    }

    private volatile Settings settings = Settings.healthy();
    private final AtomicInteger failNext = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();

    public synchronized void apply(Settings s) {
        settings = s;
        failNext.set(s.failNext());
    }

    public synchronized void reset() {
        apply(Settings.healthy());
        calls.set(0);
        failures.set(0);
        maxInFlight.set(0);
    }

    public Settings settings() {
        return settings;
    }

    // returns the status to fail with, or 0 for success
    public int enter() {
        calls.incrementAndGet();
        maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max);
        sleep(settings.latencyMs());
        boolean fail = failNext.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0 || ThreadLocalRandom.current().nextDouble() < settings.failRate();
        if (fail) {
            failures.incrementAndGet();
            return settings.status();
        }
        return 0;
    }

    public void exit() {
        inFlight.decrementAndGet();
    }

    public Map<String, Object> stats() {
        return Map.of("calls", calls.get(), "failures", failures.get(), "inFlight", inFlight.get(), "maxInFlight", maxInFlight.get(), "settings", settings);
    }

    static void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
