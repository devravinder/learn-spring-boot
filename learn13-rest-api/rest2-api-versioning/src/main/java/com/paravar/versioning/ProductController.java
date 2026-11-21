package com.paravar.versioning;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Map<Long, String> NAMES = Map.of(1L, "Book", 2L, "Pen");
    private static final Map<Long, Long> PRICES = Map.of(1L, 300L, 2L, 20L);

    public record ProductV1(long id, String name, long price) {
    }

    public record Money(long amount, String currency) {
    }

    // v2 is a breaking change: "name" became "title", price became an object
    public record ProductV2(long id, String title, Money price) {
    }

    // "1.0+" is a baseline: it serves 1.0 and 1.1 until a higher mapping takes over
    @GetMapping(path = "/{id}", version = "1.0+")
    public ResponseEntity<ProductV1> v1(@PathVariable long id) {
        return NAMES.containsKey(id) ? ResponseEntity.ok(new ProductV1(id, NAMES.get(id), PRICES.get(id))) : ResponseEntity.notFound().build();
    }

    @GetMapping(path = "/{id}", version = "2.0")
    public ResponseEntity<ProductV2> v2(@PathVariable long id) {
        return NAMES.containsKey(id)
                ? ResponseEntity.ok(new ProductV2(id, NAMES.get(id), new Money(PRICES.get(id), "INR")))
                : ResponseEntity.notFound().build();
    }

    // listing was added in 1.1 and kept in 2.0
    @GetMapping(version = "1.1+")
    public List<Long> list() {
        return NAMES.keySet().stream().sorted().toList();
    }
}
