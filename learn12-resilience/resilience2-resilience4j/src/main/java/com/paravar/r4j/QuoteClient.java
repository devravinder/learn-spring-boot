package com.paravar.r4j;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Component
public class QuoteClient {

    private final RestClient flaky;
    private final Map<String, Map<String, Object>> lastKnown = new ConcurrentHashMap<>();

    public QuoteClient(RestClient flaky) {
        this.flaky = flaky;
    }

    // aspect order: Retry( CircuitBreaker( call ) ); the fallback goes on the OUTER aspect,
    // a fallback on the breaker would swallow the error before Retry could see it
    @Retry(name = "prices", fallbackMethod = "cachedPrice")
    @CircuitBreaker(name = "prices")
    @SuppressWarnings("unchecked")
    public Map<String, Object> price(String id) {
        Map<String, Object> price = flaky.get().uri("/prices/{id}", id).retrieve().body(Map.class);
        lastKnown.put(id, price);
        return withSource(price, "live");
    }

    // fallback: same args + the exception; a stale price beats an error page
    Map<String, Object> cachedPrice(String id, CallNotPermittedException e) {
        return fallback(id, "circuit open");
    }

    Map<String, Object> cachedPrice(String id, Exception e) {
        return fallback(id, e.getClass().getSimpleName());
    }

    @RateLimiter(name = "search")
    @SuppressWarnings("unchecked")
    public Map<String, Object> search(String q) {
        return flaky.get().uri("/prices/{id}", q).retrieve().body(Map.class);
    }

    @Bulkhead(name = "reports")
    @SuppressWarnings("unchecked")
    public Map<String, Object> report(int ms) {
        return flaky.get().uri("/work?ms={ms}", ms).retrieve().body(Map.class);
    }

    // TimeLimiter needs an async return type; it cancels the wait, not the remote work
    @TimeLimiter(name = "slow", fallbackMethod = "slowFallback")
    @Bulkhead(name = "slow", type = Bulkhead.Type.THREADPOOL)
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> slow(int ms) {
        return CompletableFuture.completedFuture(flaky.get().uri("/work?ms={ms}", ms).retrieve().body(Map.class));
    }

    CompletableFuture<Map<String, Object>> slowFallback(int ms, TimeoutException e) {
        return CompletableFuture.completedFuture(Map.of("source", "fallback", "reason", "timed out"));
    }

    private Map<String, Object> fallback(String id, String reason) {
        Map<String, Object> cached = lastKnown.get(id);
        if (cached == null) {
            return Map.of("id", id, "source", "none", "reason", reason);
        }
        Map<String, Object> stale = withSource(cached, "cache");
        stale.put("reason", reason);
        return stale;
    }

    private static Map<String, Object> withSource(Map<String, Object> body, String source) {
        Map<String, Object> copy = new HashMap<>(body);
        copy.put("source", source);
        return copy;
    }
}
