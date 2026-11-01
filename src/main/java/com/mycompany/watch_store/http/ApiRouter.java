package com.mycompany.watch_store.http;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mycompany.watch_store.dto.ErrorResponse;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** Exact route matching and a single authentication/error boundary for every endpoint. */
public final class ApiRouter implements HttpHandler {
    public enum Access { PUBLIC, AUTHENTICATED, CUSTOMER, ADMIN }

    private final List<Route> routes = new ArrayList<>();
    private final SessionManager sessions;
    private final String origin;
    private final String authority;

    public ApiRouter(SessionManager sessions, String origin) {
        this.sessions = sessions;
        URI uri = URI.create(origin);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                || !(uri.getRawPath().isEmpty() || uri.getRawPath().equals("/"))) {
            throw new IllegalArgumentException("API_ORIGIN must be an HTTP(S) origin without a path.");
        }
        this.origin = uri.getScheme() + "://" + uri.getRawAuthority();
        authority = uri.getRawAuthority();
    }

    public void add(String method, String pathRegex, Access access, Action action) {
        routes.add(new Route(method, Pattern.compile("^" + pathRegex + "$"), access, action));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            try {
                checkOrigin(exchange);
                dispatch(exchange);
            } catch (ApiException exception) {
                error(exchange, exception.status(), exception.code(), exception.getMessage());
            } catch (SecurityException exception) {
                error(exchange, 401, "INVALID_CREDENTIALS", "Invalid email or password.");
            } catch (NoSuchElementException exception) {
                error(exchange, 404, "NOT_FOUND", "The requested resource was not found.");
            } catch (IllegalArgumentException exception) {
                error(exchange, 400, "INVALID_INPUT", "One or more field values are invalid.");
            } catch (IllegalStateException exception) {
                error(exchange, 409, "CONFLICT", "The operation conflicts with the current resource state.");
            } catch (SQLException exception) {
                String state = exception.getSQLState();
                if ("23505".equals(state)) {
                    error(exchange, 409, "CONFLICT", "A resource with these unique values already exists.");
                } else if ("23503".equals(state) || "23514".equals(state)
                        || "22001".equals(state) || "22003".equals(state)) {
                    error(exchange, 400, "INVALID_INPUT", "One or more field values are invalid.");
                } else if ("02000".equals(state)) {
                    error(exchange, 404, "NOT_FOUND", "The requested resource was not found.");
                } else {
                    System.err.println("API database failure; SQLState=" + state);
                    error(exchange, 500, "INTERNAL_ERROR", "The request could not be completed.");
                }
            } catch (RuntimeException exception) {
                System.err.println("API failure type: " + exception.getClass().getSimpleName());
                error(exchange, 500, "INTERNAL_ERROR", "The request could not be completed.");
            }
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException, SQLException {
        List<String> allowed = new ArrayList<>();
        String path = exchange.getRequestURI().getRawPath();
        boolean exactRoute = routes.stream().anyMatch(route -> route.path().matcher("").groupCount() == 0
                && route.path().matcher(path).matches());
        for (Route route : routes) {
            if (exactRoute && route.path().matcher("").groupCount() != 0) {
                continue;
            }
            Matcher match = route.path().matcher(path);
            if (!match.matches()) {
                continue;
            }
            allowed.add(route.method());
            if (!route.method().equals(exchange.getRequestMethod())) {
                continue;
            }
            SessionManager.Session session = null;
            if (route.access() != Access.PUBLIC) {
                session = sessions.require(exchange);
                if (route.access() != Access.AUTHENTICATED && !route.access().name().equals(session.role())) {
                    throw new ApiException(403, "FORBIDDEN", "Your account is not permitted to perform this operation.");
                }
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                // Custom headers cannot be sent by cross-origin forms. No credentialed CORS is enabled.
                if (!"WatchStore".equals(exchange.getRequestHeaders().getFirst("X-Requested-With"))) {
                    throw new ApiException(403, "CSRF_REJECTED", "The X-Requested-With: WatchStore header is required.");
                }
                if (session != null) {
                    sessions.verifyCsrf(exchange, session);
                }
            }
            route.action().run(exchange, match, session);
            return;
        }
        if (!allowed.isEmpty()) {
            exchange.getResponseHeaders().set("Allow", String.join(", ", allowed));
            throw new ApiException(405, "METHOD_NOT_ALLOWED", "HTTP method is not supported for this route.");
        }
        throw new ApiException(404, "NOT_FOUND", "API route was not found.");
    }

    private void checkOrigin(HttpExchange exchange) {
        List<String> hosts = exchange.getRequestHeaders().getOrDefault("Host", List.of());
        List<String> origins = exchange.getRequestHeaders().getOrDefault("Origin", List.of());
        String site = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if (hosts.size() != 1 || !authority.equalsIgnoreCase(hosts.get(0)) || origins.size() > 1
                || (!origins.isEmpty() && !origin.equals(origins.get(0)))
                || (site != null && !site.equals("same-origin") && !site.equals("none"))) {
            throw new ApiException(403, "ORIGIN_REJECTED", "Cross-origin requests are not permitted.");
        }
    }

    public static int id(String value) {
        try {
            if (!value.matches("[1-9][0-9]{0,9}")) {
                throw HttpJson.badRequest();
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw HttpJson.badRequest();
        }
    }

    public static Map<String, String> query(HttpExchange exchange, java.util.Set<String> permitted) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        if (raw.length() > 2048) {
            throw HttpJson.badRequest();
        }
        try {
            for (String part : raw.split("&", -1)) {
                String[] pair = part.split("=", 2);
                String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                if (!permitted.contains(name) || result.putIfAbsent(name, value) != null) {
                    throw HttpJson.badRequest();
                }
            }
        } catch (IllegalArgumentException exception) {
            throw HttpJson.badRequest();
        }
        return result;
    }

    private static void error(HttpExchange exchange, int status, String code, String message) throws IOException {
        HttpJson.send(exchange, status, new ErrorResponse(code, message));
    }

    private record Route(String method, Pattern path, Access access, Action action) {
    }

    @FunctionalInterface
    public interface Action {
        void run(HttpExchange exchange, Matcher path, SessionManager.Session session) throws IOException, SQLException;
    }
}
