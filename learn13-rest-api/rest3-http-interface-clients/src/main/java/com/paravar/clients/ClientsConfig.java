package com.paravar.clients;

import com.paravar.clients.catalog.CatalogClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

// one group = one base URL, timeouts, headers and API version, set in application.yaml
@Configuration
@ImportHttpServices(group = "catalog", types = CatalogClient.class)
public class ClientsConfig {

    // code-level tweaks for the group: map an upstream 404 to our own exception
    @Bean
    RestClientHttpServiceGroupConfigurer catalogErrors() {
        return groups -> groups.filterByName("catalog").forEachClient((group, builder) ->
                builder.defaultStatusHandler(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                    throw new NotFoundUpstreamException(request.getURI().getPath());
                }));
    }
}
