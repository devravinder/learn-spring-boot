package com.paravar.metrics.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/inventory/{product}")
public class InventoryController {

    private final Inventory inventory;

    public InventoryController(Inventory inventory) {
        this.inventory = inventory;
    }

    @PostMapping("/take")
    public ResponseEntity<Map<String, Integer>> take(@PathVariable String product, @RequestParam(defaultValue = "1") int units) {
        if (!inventory.products().contains(product)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("stock", inventory.take(product, units)));
    }

    @PostMapping("/restock")
    public ResponseEntity<Map<String, Integer>> restock(@PathVariable String product, @RequestParam(defaultValue = "10") int units) {
        if (!inventory.products().contains(product)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("stock", inventory.restock(product, units)));
    }
}
