package com.paravar.mfa.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

// "trust this device": a signed cookie that stands in for the TOTP factor for 30 days
@Component
public class TrustedDevices {

    public static final String COOKIE = "mfa_device";
    static final Duration TTL = Duration.ofDays(30);

    private final byte[] key;
    private final Clock clock;

    public TrustedDevices(@Value("${mfa.device-cookie-key}") String key, Clock clock) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public void remember(String user, HttpServletResponse response) {
        long expires = clock.instant().plus(TTL).getEpochSecond();
        String payload = user + "|" + expires;
        Cookie cookie = new Cookie(COOKIE, Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + "|" + sign(payload)).getBytes(StandardCharsets.UTF_8)));
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) TTL.toSeconds());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public boolean isTrusted(String user, HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        return Arrays.stream(request.getCookies()).filter(c -> c.getName().equals(COOKIE)).anyMatch(c -> valid(user, c.getValue()));
    }

    private boolean valid(String user, String value) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|");
            String payload = parts[0] + "|" + parts[1];
            boolean signed = MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8));
            return signed && parts[0].equals(user) && Long.parseLong(parts[1]) > clock.instant().getEpochSecond();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
