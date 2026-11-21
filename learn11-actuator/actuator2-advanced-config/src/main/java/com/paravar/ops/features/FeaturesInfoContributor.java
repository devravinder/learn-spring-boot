package com.paravar.ops.features;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class FeaturesInfoContributor implements InfoContributor {

    private final FeatureFlags flags;

    public FeaturesInfoContributor(FeatureFlags flags) {
        this.flags = flags;
    }

    @Override
    public void contribute(Info.Builder builder) {
        long on = flags.all().values().stream().filter(Boolean::booleanValue).count();
        builder.withDetail("features", java.util.Map.of("total", flags.all().size(), "enabled", on));
    }
}
