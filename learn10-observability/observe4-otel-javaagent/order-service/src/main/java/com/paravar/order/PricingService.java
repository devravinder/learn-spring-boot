package com.paravar.order;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    // the agent turns @WithSpan into a child span; no Spring AOP involved
    @WithSpan("order.pricing")
    public double total(@SpanAttribute("product") String productId, double price, int quantity) {
        double total = price * quantity;
        Span.current().setAttribute("order.total", total);
        return total;
    }
}
