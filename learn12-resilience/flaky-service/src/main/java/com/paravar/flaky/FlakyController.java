package com.paravar.flaky;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RestController
public class FlakyController {

    private final Chaos chaos;
    private final Map<String, Map<String, Object>> payments = new ConcurrentHashMap<>();
    private final AtomicInteger charges = new AtomicInteger();

    public FlakyController(Chaos chaos) {
        this.chaos = chaos;
    }

    @GetMapping("/prices/{id}")
    public ResponseEntity<Map<String, Object>> price(@PathVariable String id) {
        if (id.equals("missing")) {
            return ResponseEntity.notFound().build(); // a 4xx that must never be retried
        }
        return guarded(() -> Map.of("id", id, "price", 100 + Math.abs(id.hashCode() % 900)));
    }

    // same key, same result: the client may retry a POST without charging twice
    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> pay(@RequestHeader("Idempotency-Key") String key, @RequestParam long amount) {
        Map<String, Object> earlier = payments.get(key);
        if (earlier != null) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(earlier);
        }
        int status = chaos.enter();
        try {
            Map<String, Object> result = payments.computeIfAbsent(key, k -> {
                charges.incrementAndGet();
                return Map.of("paymentId", UUID.randomUUID().toString(), "amount", amount);
            });
            // the charge happened, but the reply is lost: the worst case for a retry
            return status == 0 ? ResponseEntity.ok(result) : ResponseEntity.status(status).build();
        } finally {
            chaos.exit();
        }
    }

    @GetMapping("/work")
    public ResponseEntity<Map<String, Object>> work(@RequestParam(defaultValue = "300") int ms) {
        return guarded(() -> {
            Chaos.sleep(ms);
            return Map.of("done", true);
        });
    }

    @PostMapping("/chaos")
    public Map<String, Object> chaos(@RequestBody Chaos.Settings settings) {
        chaos.apply(settings);
        return chaos.stats();
    }

    @PostMapping("/chaos/reset")
    public Map<String, Object> reset() {
        chaos.reset();
        payments.clear();
        charges.set(0);
        return chaos.stats();
    }

    @GetMapping("/chaos/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new java.util.HashMap<>(chaos.stats());
        stats.put("charges", charges.get());
        return stats;
    }

    private ResponseEntity<Map<String, Object>> guarded(Supplier<Map<String, Object>> body) {
        int status = chaos.enter();
        try {
            return status == 0 ? ResponseEntity.ok(body.get()) : ResponseEntity.status(status).body(Map.of("error", "chaos"));
        } finally {
            chaos.exit();
        }
    }
}
