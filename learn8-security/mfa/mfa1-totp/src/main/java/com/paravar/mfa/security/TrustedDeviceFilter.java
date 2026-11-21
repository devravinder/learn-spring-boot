package com.paravar.mfa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// a valid device cookie adds FACTOR_TOTP after the password step, so no code is asked
public class TrustedDeviceFilter extends OncePerRequestFilter {

    private final TrustedDevices devices;
    private final HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public TrustedDeviceFilter(TrustedDevices devices) {
        this.devices = devices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && has(auth, FactorGrantedAuthority.PASSWORD_AUTHORITY)
                && !has(auth, TotpAuthenticationProvider.TOTP_AUTHORITY) && devices.isTrusted(auth.getName(), request)) {
            List<GrantedAuthority> authorities = new ArrayList<>(auth.getAuthorities());
            authorities.add(FactorGrantedAuthority.fromAuthority(TotpAuthenticationProvider.TOTP_AUTHORITY));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(), null, authorities));
            SecurityContextHolder.setContext(context);
            repository.saveContext(context, request, response);
        }
        chain.doFilter(request, response);
    }

    private static boolean has(Authentication auth, String authority) {
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
