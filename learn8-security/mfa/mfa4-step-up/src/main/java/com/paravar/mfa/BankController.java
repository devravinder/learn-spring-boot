package com.paravar.mfa;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@Controller
public class BankController {

    @GetMapping("/")
    public String statement(Authentication auth, Model model) {
        model.addAttribute("user", auth.getName());
        model.addAttribute("passwordAt", passwordIssuedAt(auth));
        return "statement";
    }

    @GetMapping("/transfer")
    public String transferForm(Authentication auth, Model model) {
        model.addAttribute("passwordAt", passwordIssuedAt(auth));
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam String to, @RequestParam long amount, Model model) {
        model.addAttribute("message", "Sent " + amount + " to " + to);
        return "done";
    }

    // FactorGrantedAuthority carries issuedAt; that timestamp is what validDuration checks
    static Instant passwordIssuedAt(Authentication auth) {
        return auth.getAuthorities().stream()
                .filter(a -> a instanceof FactorGrantedAuthority f && f.getAuthority().equals(FactorGrantedAuthority.PASSWORD_AUTHORITY))
                .map(a -> ((FactorGrantedAuthority) a).getIssuedAt())
                .findFirst().orElse(null);
    }
}
