package com.paravar.mfa.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// POST /mfa/totp turns "password done" into "password + TOTP done"
public class TotpAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public TotpAuthenticationFilter(AuthenticationManager manager, TrustedDevices devices) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/mfa/totp"), manager);
        setSecurityContextRepository(new HttpSessionSecurityContextRepository());
        // a new session id after each factor blocks session fixation
        setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
        SavedRequestAwareAuthenticationSuccessHandler success = new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
                    throws IOException, jakarta.servlet.ServletException {
                if (request.getParameter("trust") != null) {
                    devices.remember(auth.getName(), response);
                }
                super.onAuthenticationSuccess(request, response, auth);
            }
        };
        setAuthenticationSuccessHandler(success);
        setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/mfa/totp?error"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !current.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Sign in with your password first.");
        }
        String code = request.getParameter("code");
        Authentication totp = getAuthenticationManager().authenticate(TotpAuthenticationToken.unauthenticated(current.getName(), code == null ? "" : code));
        // keep FACTOR_PASSWORD (and roles) from the first step and add FACTOR_TOTP
        List<GrantedAuthority> authorities = new ArrayList<>(current.getAuthorities());
        authorities.addAll(totp.getAuthorities());
        return UsernamePasswordAuthenticationToken.authenticated(current.getPrincipal(), null, authorities);
    }
}
