package com.paravar.openapi;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @GetMapping("/stats")
    @Operation(summary = "Catalog statistics")
    public Map<String, Integer> stats() {
        return Map.of("books", 1);
    }

    // @Hidden keeps internal endpoints out of the published contract
    @Hidden
    @PostMapping("/reindex")
    public Map<String, String> reindex() {
        return Map.of("status", "started");
    }
}
