package com.paravar.clients.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

// declarative client: Spring generates the implementation from these signatures
@HttpExchange("/upstream/catalog")
public interface CatalogClient {

    record Product(long id, String name, long price) {
    }

    record NewProduct(String name, long price) {
    }

    @GetExchange("/products/{id}")
    Product get(@PathVariable long id);

    @GetExchange("/products")
    List<Product> search(@RequestParam String q, @RequestParam(required = false) Integer limit);

    // ResponseEntity return type exposes status and headers, not just the body
    @PostExchange("/products")
    ResponseEntity<Product> create(@RequestBody NewProduct product, @RequestHeader("Idempotency-Key") String key);

    @DeleteExchange("/products/{id}")
    void delete(@PathVariable long id);

    @GetExchange("/slow")
    Map<String, Object> slow(@RequestParam int ms);

    @GetExchange("/echo")
    Map<String, String> echo();
}
