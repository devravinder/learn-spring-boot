package com.paravar.mfa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
public class EmailLinkSender implements OneTimeTokenGenerationSuccessHandler {

    // a real app reads the address from the user record
    private static final Map<String, String> EMAILS = Map.of("alice", "alice@example.test", "bob", "bob@example.test");

    private final JavaMailSender mail;
    private final String baseUrl;

    public EmailLinkSender(JavaMailSender mail, @Value("${mfa.base-url}") String baseUrl) {
        this.mail = mail;
        this.baseUrl = baseUrl;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken token) throws IOException {
        String link = UriComponentsBuilder.fromUriString(baseUrl).path("/login/ott").queryParam("token", token.getTokenValue()).toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@example.test");
        message.setTo(EMAILS.getOrDefault(token.getUsername(), token.getUsername() + "@example.test"));
        message.setSubject("Your sign-in link");
        message.setText("Open this link to finish signing in. It works once and expires at " + token.getExpiresAt() + ".\n\n" + link);
        mail.send(message);
        // same page whether or not the user exists, so the response leaks nothing
        response.sendRedirect("/ott/sent");
    }
}
