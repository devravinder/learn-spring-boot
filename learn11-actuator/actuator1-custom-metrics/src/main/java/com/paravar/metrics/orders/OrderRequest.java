package com.paravar.metrics.orders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotBlank String product,
        // a closed set keeps the "payment" tag low-cardinality
        @Pattern(regexp = "card|upi|cash") String paymentMethod,
        @Positive double amount) {
}
