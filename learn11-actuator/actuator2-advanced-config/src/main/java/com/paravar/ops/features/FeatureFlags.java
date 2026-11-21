package com.paravar.ops.features;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FeatureFlags {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>(Map.of("new-checkout", false, "dark-mode", true));

    public Map<String, Boolean> all() {
        return new TreeMap<>(flags);
    }

    public Boolean get(String name) {
        return flags.get(name);
    }

    public void set(String name, boolean enabled) {
        flags.put(name, enabled);
    }

    public void remove(String name) {
        flags.remove(name);
    }

    public boolean isOn(String name) {
        return flags.getOrDefault(name, false);
    }
}
