package com.mycompany.watch_store.controller;

import java.util.Set;

import com.mycompany.watch_store.dto.DtoMapper;
import com.mycompany.watch_store.dto.LoginRequest;
import com.mycompany.watch_store.dto.RegisterRequest;
import com.mycompany.watch_store.entity.User;
import com.mycompany.watch_store.http.ApiException;
import com.mycompany.watch_store.http.ApiRouter;
import com.mycompany.watch_store.http.HttpJson;
import com.mycompany.watch_store.http.SessionManager;
import com.mycompany.watch_store.service.AuthService;

import static com.mycompany.watch_store.http.ApiRouter.Access.AUTHENTICATED;
import static com.mycompany.watch_store.http.ApiRouter.Access.PUBLIC;

/** Route actions run inside ApiRouter's shared HttpHandler boundary. */
public final class AuthHandler {
    public AuthHandler(ApiRouter router, SessionManager sessions) {
        AuthService auth = new AuthService();
        router.add("POST", "/api/auth/register", PUBLIC, (exchange, path, session) -> {
            RegisterRequest request = HttpJson.read(exchange, RegisterRequest.class,
                    Set.of("fullName", "email", "password")).value();
            User user;
            try {
                user = auth.register(request.fullName(), request.email(), request.password(), request.phone());
            } catch (IllegalArgumentException exception) {
                if ("Email is already registered.".equals(exception.getMessage())) {
                    throw new ApiException(409, "EMAIL_EXISTS", "Email is already registered.");
                }
                throw exception;
            }
            HttpJson.send(exchange, 201, DtoMapper.user(user));
        });
        router.add("POST", "/api/auth/login", PUBLIC, (exchange, path, session) -> {
            LoginRequest request = HttpJson.read(exchange, LoginRequest.class, Set.of("email", "password")).value();
            User user = auth.login(request.email(), request.password());
            sessions.login(exchange, user);
            HttpJson.send(exchange, 200, DtoMapper.user(user));
        });
        router.add("POST", "/api/auth/logout", AUTHENTICATED, (exchange, path, session) -> {
            HttpJson.emptyBody(exchange);
            sessions.logout(exchange);
            HttpJson.send(exchange, 204, null);
        });
        router.add("GET", "/api/auth/me", AUTHENTICATED, (exchange, path, session) -> {
            User user = auth.getUserById(session.userId());
            exchange.getResponseHeaders().set("X-CSRF-Token", session.csrfToken());
            HttpJson.send(exchange, 200, DtoMapper.user(user));
        });
    }
}
