package com.paravar.metrics.inventory;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

// a MeterBinder groups related meters and binds them once the registry is ready
@Component
public class InventoryMetrics implements MeterBinder {

    private final Inventory inventory;

    public InventoryMetrics(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        inventory.products().forEach(product ->
                // gauges sample on scrape; hold the source object, not a copied number
                Gauge.builder("shop.inventory.stock", inventory, inv -> inv.stock(product))
                        .description("Units in stock")
                        .tag("product", product)
                        .register(registry));

        // FunctionCounter reads a monotonic value we already keep in the domain
        FunctionCounter.builder("shop.inventory.restocks", inventory, Inventory::restockCount)
                .description("Restock operations")
                .register(registry);
    }
}
