package com.paravar.mfa.web;

import com.paravar.mfa.totp.QrCode;
import com.paravar.mfa.totp.Totp;
import com.paravar.mfa.totp.TotpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class MfaController {

    private static final String PENDING = "pendingTotpSecret";

    private final TotpService totp;

    public MfaController(TotpService totp) {
        this.totp = totp;
    }

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        model.addAttribute("user", auth.getName());
        model.addAttribute("factors", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        model.addAttribute("recoveryLeft", totp.recoveryCodesLeft(auth.getName()));
        return "home";
    }

    @GetMapping("/mfa/totp")
    public String codePage(Authentication auth) {
        return totp.isEnrolled(auth.getName()) ? "totp" : "redirect:/mfa/enroll";
    }

    // the secret lives in the session until the first valid code confirms the app has it
    @GetMapping("/mfa/enroll")
    public String enrollPage(Authentication auth, HttpSession session, Model model) {
        if (totp.isEnrolled(auth.getName())) {
            return "redirect:/mfa/totp";
        }
        String secret = (String) session.getAttribute(PENDING);
        if (secret == null) {
            secret = Totp.newSecret();
            session.setAttribute(PENDING, secret);
        }
        model.addAttribute("secret", secret);
        model.addAttribute("qr", QrCode.dataUri(Totp.otpauthUri("learn-spring-boot", auth.getName(), secret)));
        return "enroll";
    }

    @PostMapping("/mfa/enroll")
    public String enroll(Authentication auth, HttpSession session, @RequestParam String code, Model model) {
        String secret = (String) session.getAttribute(PENDING);
        if (secret == null) {
            return "redirect:/mfa/enroll";
        }
        return totp.enroll(auth.getName(), secret, code.trim())
                .map(codes -> {
                    session.removeAttribute(PENDING);
                    model.addAttribute("codes", codes);
                    return "recovery-codes";
                })
                .orElseGet(() -> "redirect:/mfa/enroll?error");
    }

    @PostMapping("/account/mfa/disable")
    public String disable(Authentication auth, HttpSession session) {
        totp.disable(auth.getName());
        session.invalidate();
        return "redirect:/login?mfaDisabled";
    }
}
