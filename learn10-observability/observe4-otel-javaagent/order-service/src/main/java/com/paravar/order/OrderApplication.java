package com.paravar.order;

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

    // no observation setup needed: the agent instruments the HTTP client bytecode
    @Bean
    RestClient productClient(RestClient.Builder builder, @Value("${product.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
