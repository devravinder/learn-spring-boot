package com.paravar.order;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    // the builder from Boot carries the observation interceptor, so trace headers propagate
    @Bean
    RestClient productClient(RestClient.Builder builder, @Value("${product.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    // the logback appender is created before Spring; hand it the SDK once it exists
    @Bean
    InitializingBean installOtelAppender(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
