package com.paravar.retry;

import org.springframework.core.retry.RetryException;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.InvocationRejectedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@RestController
public class RetryController {

    private final PriceClient prices;
    private final PaymentClient payments;

    public RetryController(PriceClient prices, PaymentClient payments) {
        this.prices = prices;
        this.payments = payments;
    }

    @GetMapping("/prices/{id}")
    public ResponseEntity<Map<String, Object>> price(@PathVariable String id) {
        int before = prices.attempts();
        try {
            Map<String, Object> price = prices.price(id);
            return ResponseEntity.ok(Map.of("price", price, "attempts", prices.attempts() - before));
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", "not retried: " + e.getStatusCode(), "attempts", prices.attempts() - before));
        } catch (RestClientException e) {
            return ResponseEntity.status(503).body(Map.of("error", "gave up after retries", "attempts", prices.attempts() - before));
        }
    }

    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> pay(@RequestParam long amount) {
        try {
            PaymentClient.Result result = payments.pay(amount);
            return ResponseEntity.ok(Map.of("payment", result.payment(), "retries", result.retries()));
        } catch (RetryException e) {
            return ResponseEntity.status(503).body(Map.of("error", "payment failed", "retries", e.getRetryCount()));
        }
    }

    // fires n calls at once; the downstream reports how many it saw in parallel
    @GetMapping("/bulkhead")
    public Map<String, Object> bulkhead(@RequestParam(defaultValue = "6") int n, @RequestParam(defaultValue = "block") String mode) {
        List<CompletableFuture<String>> calls = new ArrayList<>();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                calls.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        if (mode.equals("reject")) {
                            prices.exclusiveWork(300);
                        } else {
                            prices.slowWork(300);
                        }
                        return "ok";
                    } catch (InvocationRejectedException e) {
                        return "rejected";
                    }
                }, pool));
            }
        }
        List<String> results = calls.stream().map(CompletableFuture::join).toList();
        return Map.of("ok", results.stream().filter("ok"::equals).count(), "rejected", results.stream().filter("rejected"::equals).count());
    }

    @ExceptionHandler(InvocationRejectedException.class)
    ResponseEntity<Map<String, String>> rejected(InvocationRejectedException e) {
        return ResponseEntity.status(429).body(Map.of("error", "too busy"));
    }
}
