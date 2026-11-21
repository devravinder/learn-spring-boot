package com.paravar.metrics.orders;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public Order place(@Valid @RequestBody OrderRequest request) {
        return orders.place(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> find(@PathVariable String id) {
        return ResponseEntity.of(orders.find(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> rejected(IllegalArgumentException e) {
        return ResponseEntity.unprocessableContent().body(Map.of("error", e.getMessage()));
    }
}
