package com.paravar.metrics.reports;

import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecommendationService {

    // one Observation becomes a timer now and a span too once tracing is on the classpath
    @Observed(name = "shop.recommendations", contextualName = "recommend-products",
            lowCardinalityKeyValues = {"source", "catalog"})
    public List<String> recommend(String product) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 40));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return product.equals("laptop") ? List.of("mouse", "bag") : List.of("pen", "notebook");
    }
}
