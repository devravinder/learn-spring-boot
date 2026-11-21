package com.paravar.r4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@SpringBootApplication
public class R4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(R4jApplication.class, args);
    }

    @Bean
    RestClient flaky(RestClient.Builder builder, @Value("${flaky.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
