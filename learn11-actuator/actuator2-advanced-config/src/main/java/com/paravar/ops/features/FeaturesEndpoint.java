package com.paravar.ops.features;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

// technology-neutral endpoint: exposed over HTTP (GET/POST/DELETE) and JMX
@Component
@Endpoint(id = "features")
public class FeaturesEndpoint {

    private final FeatureFlags flags;

    public FeaturesEndpoint(FeatureFlags flags) {
        this.flags = flags;
    }

    @ReadOperation
    public Map<String, Boolean> features() {
        return flags.all();
    }

    // returning null from a read operation gives 404
    @ReadOperation
    public Map<String, Object> feature(@Selector String name) {
        Boolean enabled = flags.get(name);
        return enabled == null ? null : Map.of("name", name, "enabled", enabled);
    }

    @WriteOperation
    public Map<String, Object> configure(@Selector String name, boolean enabled) {
        flags.set(name, enabled);
        return Map.of("name", name, "enabled", enabled);
    }

    @DeleteOperation
    public void delete(@Selector String name) {
        flags.remove(name);
    }
}
