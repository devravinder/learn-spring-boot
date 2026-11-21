package com.paravar.ops.health;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

// bean name "paymentGatewayHealthIndicator" shows up as "paymentGateway" in /health
@Component
public class PaymentGatewayHealthIndicator extends AbstractHealthIndicator {

    private final AtomicBoolean reachable = new AtomicBoolean(true);

    public void setReachable(boolean value) {
        reachable.set(value);
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (reachable.get()) {
            builder.up().withDetail("latencyMs", 12);
        } else {
            builder.down().withDetail("error", "connect timed out");
        }
    }
}
