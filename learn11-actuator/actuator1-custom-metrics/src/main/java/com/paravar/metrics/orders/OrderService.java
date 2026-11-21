package com.paravar.metrics.orders;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final MeterRegistry registry;
    private final DistributionSummary amounts;
    private final Timer processing;

    public OrderService(MeterRegistry registry) {
        this.registry = registry;
        this.amounts = DistributionSummary.builder("shop.order.amount")
                .description("Order value")
                .baseUnit("inr")
                // SLO buckets make a histogram; Prometheus computes percentiles from it server-side
                .serviceLevelObjectives(500, 1000, 5000)
                .register(registry);
        this.processing = Timer.builder("shop.order.processing")
                .description("Time to validate and store an order")
                // client-side percentiles: cheap to read, but cannot be averaged across instances
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Counted(value = "shop.orders.failed", recordFailuresOnly = true)
    public Order place(OrderRequest request) {
        Timer.Sample sample = Timer.start(registry);
        try {
            if (request.amount() > 100_000) {
                throw new IllegalArgumentException("amount over limit");
            }
            sleep(ThreadLocalRandom.current().nextInt(5, 60));
            Order order = new Order(UUID.randomUUID().toString(), request.product(), request.paymentMethod(), request.amount());
            orders.put(order.id(), order);
            // registry caches the counter per tag set, so lookup per call is cheap
            Counter.builder("shop.orders.placed")
                    .description("Orders placed")
                    .tag("payment", request.paymentMethod())
                    .register(registry)
                    .increment();
            amounts.record(request.amount());
            return order;
        } finally {
            sample.stop(processing);
        }
    }

    @Timed(value = "shop.order.lookup", description = "Order lookup by id")
    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
