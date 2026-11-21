package com.paravar.cap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class CapController {

    private final Cluster cluster;

    public CapController(Cluster cluster) {
        this.cluster = cluster;
    }

    @PutMapping("/nodes/{node}/kv/{key}")
    public ResponseEntity<Map<String, Object>> put(@PathVariable String node, @PathVariable String key, @RequestBody String value) {
        if (!Cluster.NODES.contains(node)) {
            return ResponseEntity.notFound().build();
        }
        Cluster.WriteResult r = cluster.write(node, key, value);
        Map<String, Object> body = new HashMap<>(Map.of("accepted", r.accepted(), "acks", r.acks(), "reason", r.reason()));
        if (r.version() != null) {
            body.put("version", r.version());
        }
        return ResponseEntity.status(r.accepted() ? 200 : 503).body(body);
    }

    @GetMapping("/nodes/{node}/kv/{key}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String node, @PathVariable String key) {
        if (!Cluster.NODES.contains(node)) {
            return ResponseEntity.notFound().build();
        }
        Cluster.ReadResult r = cluster.read(node, key);
        Map<String, Object> body = new HashMap<>(Map.of("ok", r.ok(), "replicasAsked", r.replicasAsked(), "reason", r.reason()));
        if (r.value() != null) {
            body.put("value", r.value().value());
            body.put("version", r.value());
        }
        return ResponseEntity.status(r.ok() ? 200 : 503).body(body);
    }

    public record PartitionRequest(List<Set<String>> groups) {
    }

    @PostMapping("/partition")
    public Map<String, Object> partition(@RequestBody PartitionRequest request) {
        cluster.partition(request.groups());
        return cluster.view();
    }

    @PostMapping("/heal")
    public Cluster.HealReport heal() {
        return cluster.heal();
    }

    @PostMapping("/settings")
    public Map<String, Object> settings(@RequestBody Cluster.Settings settings) {
        cluster.configure(settings);
        return cluster.view();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        cluster.reset();
        return cluster.view();
    }

    @GetMapping("/cluster")
    public Map<String, Object> view() {
        return cluster.view();
    }
}
