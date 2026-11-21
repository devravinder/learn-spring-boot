package com.paravar.versioning;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

// header and query names, supported versions and the default live in application.yaml
@Configuration
public class VersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        StandardApiVersionDeprecationHandler deprecation = new StandardApiVersionDeprecationHandler();
        // RFC 9745 Deprecation + RFC 8594 Sunset headers tell clients when 1.x goes away
        deprecation.configureVersion("1.0")
                .setDeprecationDate(ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .setDeprecationLink(URI.create("https://example.test/docs/migrate-to-v2"))
                .setSunsetDate(ZonedDateTime.of(2027, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        deprecation.configureVersion("1.1")
                .setDeprecationDate(ZonedDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .setSunsetDate(ZonedDateTime.of(2027, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        configurer.setDeprecationHandler(deprecation);
    }
}
