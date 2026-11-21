package com.paravar.mfa;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;
import java.util.TreeMap;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser user, Authentication auth, Model model) {
        model.addAttribute("user", user.getPreferredUsername());
        // acr is the IdP's statement of how strong this login was
        model.addAttribute("acr", user.getIdToken().getClaimAsString("acr"));
        Map<String, Object> claims = new TreeMap<>(user.getIdToken().getClaims());
        model.addAttribute("claims", claims);
        model.addAttribute("authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        return "home";
    }
}
