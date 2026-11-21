package com.paravar.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// plain Spring MVC: every span, metric and exported log line comes from the agent
@RestController
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    public record Product(String id, String name, double price) {
    }

    private static final Map<String, Product> CATALOG = Map.of(
            "p1", new Product("p1", "Book", 300),
            "p2", new Product("p2", "Pen", 20),
            "slow", new Product("slow", "Slow laptop", 70000));

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> get(@PathVariable String id) throws InterruptedException {
        log.info("looking up product {}", id);
        Thread.sleep(id.equals("slow") ? 800 : ThreadLocalRandom.current().nextInt(5, 40));
        if (id.equals("broken")) {
            log.error("catalog backend failed for {}", id);
            return ResponseEntity.internalServerError().build();
        }
        Product product = CATALOG.get(id);
        return product == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(product);
    }
}
