package com.paravar.mfa.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class TotpAuthenticationToken extends AbstractAuthenticationToken {

    private final String username;
    private final String code;

    private TotpAuthenticationToken(String username, String code, Collection<? extends GrantedAuthority> authorities, boolean authenticated) {
        super(authorities);
        this.username = username;
        this.code = code;
        setAuthenticated(authenticated);
    }

    public static TotpAuthenticationToken unauthenticated(String username, String code) {
        return new TotpAuthenticationToken(username, code, List.of(), false);
    }

    public static TotpAuthenticationToken authenticated(String username, Collection<? extends GrantedAuthority> authorities) {
        return new TotpAuthenticationToken(username, null, authorities, true);
    }

    @Override
    public Object getCredentials() {
        return code;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }
}
