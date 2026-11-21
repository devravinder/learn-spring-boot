package com.paravar.mfa.security;

import com.paravar.mfa.totp.TotpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.time.Clock;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, TotpService totp, TrustedDevices devices) throws Exception {
        // both factors are needed for the app; the MFA pages need only the password
        AuthorizationManagerFactory<Object> mfa = AuthorizationManagerFactories.multiFactor()
                .requireFactors(FactorGrantedAuthority.PASSWORD_AUTHORITY, TotpAuthenticationProvider.TOTP_AUTHORITY)
                .build();

        TotpAuthenticationFilter totpFilter = new TotpAuthenticationFilter(
                new ProviderManager(new TotpAuthenticationProvider(totp)), devices);

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error").permitAll()
                        .requestMatchers("/mfa/**").authenticated()
                        .anyRequest().access(mfa.authenticated()))
                .formLogin(Customizer.withDefaults())
                // missing FACTOR_TOTP sends the user to the code page instead of a 403
                .exceptionHandling(e -> e.defaultDeniedHandlerForMissingAuthority(
                        new LoginUrlAuthenticationEntryPoint("/mfa/totp"), TotpAuthenticationProvider.TOTP_AUTHORITY))
                .addFilterAfter(totpFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new TrustedDeviceFilter(devices), AuthorizationFilter.class);
        return http.build();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("alice").password(encoder.encode("password")).roles("USER").build(),
                User.withUsername("bob").password(encoder.encode("password")).roles("USER").build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
