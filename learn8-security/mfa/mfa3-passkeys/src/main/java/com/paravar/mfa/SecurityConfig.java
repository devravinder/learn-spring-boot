package com.paravar.mfa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.authorization.MultiFactorCondition;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.webauthn.management.MapPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.MapUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

// opt-in MFA: users who registered a passkey need password + passkey, others only the password
@Configuration
@EnableMultiFactorAuthentication(
        authorities = {FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.WEBAUTHN_AUTHORITY},
        when = MultiFactorCondition.WEBAUTHN_REGISTERED)
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, @Value("${mfa.rp-id}") String rpId, @Value("${mfa.origin}") String origin) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                // the browser binds each passkey to rpId and checks the origin, which is what stops phishing
                .webAuthn(webAuthn -> webAuthn
                        .rpName("learn-spring-boot")
                        .rpId(rpId)
                        .allowedOrigins(origin));
        return http.build();
    }

    // shared beans: the WebAuthn filters store passkeys here, the MFA condition reads them
    @Bean
    PublicKeyCredentialUserEntityRepository passkeyUsers() {
        return new MapPublicKeyCredentialUserEntityRepository();
    }

    @Bean
    UserCredentialRepository passkeyCredentials() {
        return new MapUserCredentialRepository();
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password("{noop}password").roles("USER").build(),
                User.withUsername("bob").password("{noop}password").roles("USER").build());
    }
}
