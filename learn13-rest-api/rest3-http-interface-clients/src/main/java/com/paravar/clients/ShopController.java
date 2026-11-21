package com.paravar.clients;

import com.paravar.clients.catalog.CatalogClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// our API; it uses CatalogClient like a local bean, the HTTP details live in the client
@RestController
@RequestMapping("/shop")
public class ShopController {

    private final CatalogClient catalog;

    public ShopController(CatalogClient catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products/{id}")
    public CatalogClient.Product get(@PathVariable long id) {
        return catalog.get(id);
    }

    @GetMapping("/products")
    public List<CatalogClient.Product> search(@RequestParam String q, @RequestParam(required = false) Integer limit) {
        return catalog.search(q, limit);
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CatalogClient.NewProduct product) {
        ResponseEntity<CatalogClient.Product> upstream = catalog.create(product, UUID.randomUUID().toString());
        return ResponseEntity.status(upstream.getStatusCode()).body(Map.of(
                "product", upstream.getBody(), "upstreamLocation", String.valueOf(upstream.getHeaders().getLocation())));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        catalog.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slow")
    public Map<String, Object> slow(@RequestParam int ms) {
        return catalog.slow(ms);
    }

    @GetMapping("/echo")
    public Map<String, String> echo() {
        return catalog.echo();
    }

    @ExceptionHandler(NotFoundUpstreamException.class)
    ProblemDetail notFound(NotFoundUpstreamException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // read timeout from the group config surfaces as ResourceAccessException
    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail timeout(ResourceAccessException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, "catalog did not answer in time");
    }
}
