package com.paravar.order;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
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
    private static final AttributeKey<String> PRODUCT = AttributeKey.stringKey("product");

    private final RestClient products;
    private final PricingService pricing;
    // OTel metrics API instead of Micrometer; GlobalOpenTelemetry is wired by the agent
    private final LongCounter created = GlobalOpenTelemetry.getMeter("order-service").counterBuilder("orders.created").build();
    private final LongCounter failed = GlobalOpenTelemetry.getMeter("order-service").counterBuilder("orders.failed").build();

    public OrderController(RestClient products, PricingService pricing) {
        this.products = products;
        this.pricing = pricing;
    }

    public record OrderRequest(String productId, int quantity) {
    }

    public record Product(String id, String name, double price) {
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> create(@RequestBody OrderRequest request) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.info("creating order for product {} x{}", request.productId(), request.quantity());
        try {
            Product product = products.get().uri("/products/{id}", request.productId()).retrieve().body(Product.class);
            double total = pricing.total(product.id(), product.price(), request.quantity());
            created.add(1, Attributes.of(PRODUCT, product.id()));
            log.info("order created, total {}", total);
            return ResponseEntity.ok(Map.of("product", product.name(), "total", total, "traceId", traceId));
        } catch (RestClientResponseException e) {
            failed.add(1, Attributes.of(AttributeKey.stringKey("reason"), "product-" + e.getStatusCode().value()));
            log.error("product lookup failed with {}", e.getStatusCode());
            return ResponseEntity.status(502).body(Map.of("error", "product service failed", "traceId", traceId));
        }
    }
}
