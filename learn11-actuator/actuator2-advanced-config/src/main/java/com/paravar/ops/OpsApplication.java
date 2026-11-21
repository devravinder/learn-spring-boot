package com.paravar.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class OpsApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OpsApplication.class);
        // keeps startup steps in memory so /startup can show what was slow
        app.setApplicationStartup(new BufferingApplicationStartup(2048));
        app.run(args);
    }
}
