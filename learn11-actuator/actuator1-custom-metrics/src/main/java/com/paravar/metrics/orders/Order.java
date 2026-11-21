package com.paravar.metrics.orders;

public record Order(String id, String product, String paymentMethod, double amount) {
}
