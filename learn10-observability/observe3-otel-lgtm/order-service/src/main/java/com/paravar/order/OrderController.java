package com.paravar.order;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestClient products;
    private final ObservationRegistry observations;
    private final MeterRegistry meters;
    private final Tracer tracer;

    public OrderController(RestClient products, ObservationRegistry observations, MeterRegistry meters, Tracer tracer) {
        this.products = products;
        this.observations = observations;
        this.meters = meters;
        this.tracer = tracer;
    }

    public record OrderRequest(String productId, int quantity) {
    }

    public record Product(String id, String name, double price) {
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> create(@RequestBody OrderRequest request) {
        String traceId = tracer.currentSpan().context().traceId();
        log.info("creating order for product {} x{}", request.productId(), request.quantity());
        try {
            Product product = products.get().uri("/products/{id}", request.productId()).retrieve().body(Product.class);
            // one Observation gives a child span and an "order.pricing" timer
            double total = Observation.createNotStarted("order.pricing", observations)
                    .lowCardinalityKeyValue("product", product.id())
                    .observe(() -> product.price() * request.quantity());
            meters.counter("orders.created", "product", product.id()).increment();
            log.info("order created, total {}", total);
            return ResponseEntity.ok(Map.of("product", product.name(), "total", total, "traceId", traceId));
        } catch (RestClientResponseException e) {
            meters.counter("orders.failed", "reason", "product-" + e.getStatusCode().value()).increment();
            log.error("product lookup failed with {}", e.getStatusCode());
            return ResponseEntity.status(502).body(Map.of("error", "product service failed", "traceId", traceId));
        }
    }
}
