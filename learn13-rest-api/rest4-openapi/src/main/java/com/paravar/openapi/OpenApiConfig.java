package com.paravar.openapi;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// the scheme is only documented here; real token checks belong to Spring Security
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    OpenAPI api() {
        return new OpenAPI().info(new Info()
                .title("Library API")
                .version("1.0.0")
                .description("Books catalog used to learn OpenAPI with springdoc")
                .contact(new Contact().name("learn-spring-boot").email("dev@example.test"))
                .license(new License().name("MIT")));
    }

    // groups split one app into several documents, e.g. public vs admin consumers
    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder().group("public").pathsToMatch("/api/books/**").build();
    }

    @Bean
    GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("admin").pathsToMatch("/api/admin/**").build();
    }
}
