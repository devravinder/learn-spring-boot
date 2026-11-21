package com.paravar.metrics.reports;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ReportController {

    private final ReportService reports;
    private final RecommendationService recommendations;

    public ReportController(ReportService reports, RecommendationService recommendations) {
        this.reports = reports;
        this.recommendations = recommendations;
    }

    @PostMapping("/reports")
    public ResponseEntity<Map<String, String>> start(@RequestParam(defaultValue = "20") int seconds) {
        reports.generate(seconds);
        return ResponseEntity.accepted().body(Map.of("status", "started", "seconds", String.valueOf(seconds)));
    }

    @GetMapping("/recommendations/{product}")
    public List<String> recommend(@PathVariable String product) {
        return recommendations.recommend(product);
    }
}
