package com.paravar.clients.catalog;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// stands in for a remote catalog service, so the client calls a real HTTP endpoint locally
@RestController
@RequestMapping("/upstream/catalog")
public class CatalogUpstream {

    private final Map<Long, CatalogClient.Product> products = new ConcurrentHashMap<>(Map.of(
            1L, new CatalogClient.Product(1, "Book", 300),
            2L, new CatalogClient.Product(2, "Notebook", 120)));
    private final AtomicLong ids = new AtomicLong(2);

    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogClient.Product> get(@PathVariable long id) {
        return ResponseEntity.of(java.util.Optional.ofNullable(products.get(id)));
    }

    @GetMapping("/products")
    public List<CatalogClient.Product> search(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        return products.values().stream().filter(p -> p.name().toLowerCase().contains(q.toLowerCase())).limit(limit).toList();
    }

    @PostMapping("/products")
    public ResponseEntity<CatalogClient.Product> create(@RequestBody CatalogClient.NewProduct p, @RequestHeader("Idempotency-Key") String key) {
        CatalogClient.Product created = new CatalogClient.Product(ids.incrementAndGet(), p.name(), p.price());
        products.put(created.id(), created);
        return ResponseEntity.created(URI.create("/upstream/catalog/products/" + created.id())).header("Idempotency-Key", key).body(created);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return products.remove(id) == null ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    @GetMapping("/slow")
    public Map<String, Object> slow(@RequestParam int ms) throws InterruptedException {
        Thread.sleep(ms);
        return Map.of("sleptMs", ms);
    }

    // shows what the client actually sent
    @GetMapping("/echo")
    public Map<String, String> echo(@RequestHeader HttpHeaders headers) {
        // HttpHeaders looks names up case-insensitively, a plain Map does not
        return Map.of("apiVersion", String.valueOf(headers.getFirst("API-Version")), "client", String.valueOf(headers.getFirst("X-Client")));
    }
}
