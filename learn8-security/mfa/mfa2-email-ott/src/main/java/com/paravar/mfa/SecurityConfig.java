package com.paravar.mfa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ott.GenerateOneTimeTokenRequest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenRequestResolver;

import java.time.Duration;

// every authenticated request now needs BOTH factors; Spring routes the user to the missing one
@Configuration
@EnableMultiFactorAuthentication(authorities = {FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.OTT_AUTHORITY})
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, EmailLinkSender sender, GenerateOneTimeTokenRequestResolver resolver) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ott/sent", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .oneTimeTokenLogin(ott -> ott
                        .tokenGenerationSuccessHandler(sender)
                        .generateRequestResolver(resolver));
        return http.build();
    }

    // after the password step, ignore the form's username so nobody can mail a link to someone else
    @Bean
    GenerateOneTimeTokenRequestResolver signedInUserOnly(@Value("${mfa.ott-ttl:5m}") Duration ttl) {
        return request -> {
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            String user = current != null && current.isAuthenticated() ? current.getName() : request.getParameter("username");
            return user == null ? null : new GenerateOneTimeTokenRequest(user, ttl);
        };
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password("{noop}password").roles("USER").build(),
                User.withUsername("bob").password("{noop}password").roles("USER").build());
    }
}
