package com.paravar.retry;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PaymentClient {

    private final RestClient flaky;
    private final RetryTemplate retry;
    private final AtomicInteger retries = new AtomicInteger();

    public PaymentClient(RestClient flaky) {
        this.flaky = flaky;
        RetryPolicy policy = RetryPolicy.builder()
                .includes(HttpServerErrorException.class, ResourceAccessException.class)
                .maxRetries(4)
                .delay(Duration.ofMillis(100))
                .multiplier(2)
                .build();
        this.retry = new RetryTemplate(policy);
        // programmatic API: a listener sees every retry, handy for logs and metrics
        this.retry.setRetryListener(new RetryListener() {
            @Override
            public void beforeRetry(RetryPolicy p, org.springframework.core.retry.Retryable<?> r, RetryState state) {
                retries.incrementAndGet();
            }
        });
    }

    public record Result(Map<String, Object> payment, int retries) {
    }

    // the key is made ONCE per payment, outside the retry loop, so every attempt carries it
    @SuppressWarnings("unchecked")
    public Result pay(long amount) throws RetryException {
        String key = UUID.randomUUID().toString();
        int before = retries.get();
        Map<String, Object> payment = retry.execute(() -> flaky.post()
                .uri("/payments?amount={a}", amount)
                .header("Idempotency-Key", key)
                .retrieve()
                .body(Map.class));
        return new Result(payment, retries.get() - before);
    }
}
