package com.paravar.mfa.security;

import com.paravar.mfa.totp.TotpService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;

import java.util.List;

public class TotpAuthenticationProvider implements AuthenticationProvider {

    public static final String TOTP_AUTHORITY = "FACTOR_TOTP";

    private final TotpService totp;

    public TotpAuthenticationProvider(TotpService totp) {
        this.totp = totp;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String user = (String) authentication.getPrincipal();
        String code = String.valueOf(authentication.getCredentials()).trim();
        return switch (totp.verify(user, code)) {
            // only the new factor is granted here; the filter merges it with the password factor
            case OK -> TotpAuthenticationToken.authenticated(user, List.of(FactorGrantedAuthority.fromAuthority(TOTP_AUTHORITY)));
            case LOCKED -> throw new LockedException("Too many attempts. Try again in a few minutes.");
            case REPLAYED -> throw new BadCredentialsException("That code was already used. Wait for the next one.");
            case NOT_ENROLLED -> throw new BadCredentialsException("Authenticator app is not set up.");
            case INVALID -> throw new BadCredentialsException("Invalid code.");
        };
    }

    @Override
    public boolean supports(Class<?> type) {
        return TotpAuthenticationToken.class.isAssignableFrom(type);
    }
}
