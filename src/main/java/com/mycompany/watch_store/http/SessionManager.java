package com.mycompany.watch_store.http;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mycompany.watch_store.entity.User;
import com.sun.net.httpserver.HttpExchange;

/** In-memory, absolute-expiry sessions. No credentials or Entity objects are retained. */
public final class SessionManager implements AutoCloseable {
    private static final String COOKIE = "WATCH_SESSION";
    private static final int MAX_SESSIONS = 10000;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration lifetime;
    private final boolean secure;

    public record Session(int userId, String role, String csrfToken, Instant expiresAt) {
    }

    public SessionManager(Duration lifetime, boolean secure) {
        if (lifetime == null || lifetime.compareTo(Duration.ofSeconds(1)) < 0
                || lifetime.compareTo(Duration.ofDays(1)) > 0) {
            throw new IllegalArgumentException("Session lifetime must be between one second and one day.");
        }
        this.lifetime = lifetime;
        this.secure = secure;
    }

    public synchronized void login(HttpExchange exchange, User user) {
        purgeExpired();
        invalidate(exchange);
        if (sessions.size() >= MAX_SESSIONS) {
            throw new ApiException(503, "SESSION_CAPACITY", "Please try signing in later.");
        }
        String token = randomToken();
        Session session = new Session(user.getId(), user.getRole(), randomToken(), Instant.now().plus(lifetime));
        sessions.put(token, session);
        exchange.getResponseHeaders().add("Set-Cookie", cookie(token, lifetime.toSeconds()));
        exchange.getResponseHeaders().set("X-CSRF-Token", session.csrfToken());
    }

    public Session require(HttpExchange exchange) {
        String token = token(exchange);
        Session session = token == null ? null : sessions.get(token);
        if (session == null || !session.expiresAt().isAfter(Instant.now())) {
            if (token != null) {
                sessions.remove(token);
            }
            clearCookie(exchange);
            throw new ApiException(401, "UNAUTHORIZED", "Authentication is required.");
        }
        return session;
    }

    public void verifyCsrf(HttpExchange exchange, Session session) {
        String supplied = exchange.getRequestHeaders().getFirst("X-CSRF-Token");
        if (supplied == null || supplied.length() != 43 || !MessageDigest.isEqual(
                session.csrfToken().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                supplied.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            throw new ApiException(403, "CSRF_REJECTED", "A valid X-CSRF-Token header is required.");
        }
    }

    public void logout(HttpExchange exchange) {
        invalidate(exchange);
        clearCookie(exchange);
    }

    private void invalidate(HttpExchange exchange) {
        String token = token(exchange);
        if (token != null) {
            sessions.remove(token);
        }
    }

    private String token(HttpExchange exchange) {
        String value = null;
        for (String header : exchange.getRequestHeaders().getOrDefault("Cookie", java.util.List.of())) {
            for (String part : header.split(";")) {
                String[] pair = part.strip().split("=", 2);
                if (pair.length == 2 && pair[0].equals(COOKIE)) {
                    if (value != null || !pair[1].matches("[A-Za-z0-9_-]{43}")) {
                        return null;
                    }
                    value = pair[1];
                }
            }
        }
        return value;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String cookie(String value, long maxAge) {
        return COOKIE + "=" + value + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + maxAge
                + (secure ? "; Secure" : "");
    }

    private void clearCookie(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Set-Cookie", cookie("", 0) + "; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
    }

    public void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    @Override
    public void close() {
        sessions.clear();
    }
}
