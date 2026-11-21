package com.paravar.metrics.inventory;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class Inventory {

    private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>(Map.of(
            "book", new AtomicInteger(20),
            "pen", new AtomicInteger(100),
            "laptop", new AtomicInteger(5)));
    private final AtomicLong restocks = new AtomicLong();

    public Set<String> products() {
        return stock.keySet();
    }

    public int stock(String product) {
        return stock.get(product).get();
    }

    public int take(String product, int units) {
        return stock.get(product).addAndGet(-units);
    }

    public int restock(String product, int units) {
        restocks.incrementAndGet();
        return stock.get(product).addAndGet(units);
    }

    public long restockCount() {
        return restocks.get();
    }
}
