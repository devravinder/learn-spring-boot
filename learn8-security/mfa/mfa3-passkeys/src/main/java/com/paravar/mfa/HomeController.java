package com.paravar.mfa;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        model.addAttribute("user", auth.getName());
        model.addAttribute("factors", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        return "home";
    }
}
