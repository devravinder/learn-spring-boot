package com.paravar.retry;

import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PriceClient {

    private final RestClient flaky;
    private final AtomicInteger attempts = new AtomicInteger();

    public PriceClient(RestClient flaky) {
        this.flaky = flaky;
    }

    // a method, not a field: callers go through the retry proxy, whose fields are empty
    public int attempts() {
        return attempts.get();
    }

    // retry only what can heal: 5xx and timeouts; a 404 fails at once
    // backoff 100 ms, 200 ms, 400 ms (+-50 ms jitter so clients don't retry in lockstep)
    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetries = 3, delay = 100, multiplier = 2, jitter = 50, maxDelay = 1000)
    @SuppressWarnings("unchecked")
    public Map<String, Object> price(String id) {
        attempts.incrementAndGet();
        return flaky.get().uri("/prices/{id}", id).retrieve().body(Map.class);
    }

    // bulkhead: at most 2 calls in flight from this app; extra callers wait (BLOCK policy)
    @ConcurrencyLimit(2)
    @SuppressWarnings("unchecked")
    public Map<String, Object> slowWork(int ms) {
        return flaky.get().uri("/work?ms={ms}", ms).retrieve().body(Map.class);
    }

    // REJECT policy: fail fast instead of queueing when the limit is reached
    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
    @SuppressWarnings("unchecked")
    public Map<String, Object> exclusiveWork(int ms) {
        return flaky.get().uri("/work?ms={ms}", ms).retrieve().body(Map.class);
    }
}
