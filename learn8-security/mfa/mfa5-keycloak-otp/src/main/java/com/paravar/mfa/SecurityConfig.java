package com.paravar.mfa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

// the app never sees a password or an OTP: Keycloak runs both factors and returns an ID token
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, ClientRegistrationRepository clients) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler logout = new OidcClientInitiatedLogoutSuccessHandler(clients);
        logout.setPostLogoutRedirectUri("{baseUrl}/");
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                // RP-initiated logout ends the Keycloak session too, not only the app session
                .logout(l -> l.logoutSuccessHandler(logout));
        return http.build();
    }
}
