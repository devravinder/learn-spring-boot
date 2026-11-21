package com.paravar.ops.config;

import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    // /httpexchanges only records when a repository bean exists; in-memory is for dev only
    @Bean
    HttpExchangeRepository httpExchanges() {
        InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
        repository.setCapacity(50);
        return repository;
    }

    // masks *token* keys in /env and /configprops even for users allowed to see values
    @Bean
    SanitizingFunction maskTokens() {
        return data -> data.getKey().toLowerCase().contains("token") ? data.withValue(SanitizableData.SANITIZED_VALUE) : data;
    }
}
