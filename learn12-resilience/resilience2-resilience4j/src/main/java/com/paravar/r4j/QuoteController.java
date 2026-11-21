package com.paravar.r4j;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@RestController
public class QuoteController {

    private final QuoteClient quotes;
    private final CircuitBreakerRegistry breakers;

    public QuoteController(QuoteClient quotes, CircuitBreakerRegistry breakers) {
        this.quotes = quotes;
        this.breakers = breakers;
    }

    @GetMapping("/prices/{id}")
    public Map<String, Object> price(@PathVariable String id) {
        return quotes.price(id);
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String q) {
        return quotes.search(q);
    }

    @GetMapping("/reports")
    public Map<String, Object> reports(@RequestParam(defaultValue = "5") int n) {
        List<CompletableFuture<String>> calls = new ArrayList<>();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                calls.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        quotes.report(400);
                        return "ok";
                    } catch (BulkheadFullException e) {
                        return "full";
                    }
                }, pool));
            }
        }
        List<String> r = calls.stream().map(CompletableFuture::join).toList();
        return Map.of("ok", r.stream().filter("ok"::equals).count(), "full", r.stream().filter("full"::equals).count());
    }

    @GetMapping("/slow")
    public CompletableFuture<Map<String, Object>> slow(@RequestParam(defaultValue = "100") int ms) {
        return quotes.slow(ms);
    }

    @GetMapping("/breaker")
    public Map<String, Object> breaker() {
        CircuitBreaker cb = breakers.circuitBreaker("prices");
        CircuitBreaker.Metrics m = cb.getMetrics();
        return Map.of("state", cb.getState().name(), "failureRate", m.getFailureRate(),
                "buffered", m.getNumberOfBufferedCalls(), "notPermitted", m.getNumberOfNotPermittedCalls());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    ResponseEntity<Map<String, String>> limited(RequestNotPermitted e) {
        return ResponseEntity.status(429).header("Retry-After", "1").body(Map.of("error", "rate limit"));
    }
}
