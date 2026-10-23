package com.patron.feed_suggestor_api.event;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class ClientIdResolver {

    private static final String COOKIE_NAME = "fs_client";
    private static final Duration MAX_AGE = Duration.ofDays(730);


    public UUID resolve(HttpServletRequest request, HttpServletResponse response) {
        UUID existing = read(request);
        if (existing != null) return existing;

        UUID fresh = UUID.randomUUID();
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, fresh.toString())
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return fresh;
    }

    private UUID read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                try {
                    return UUID.fromString(c.getValue());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
}