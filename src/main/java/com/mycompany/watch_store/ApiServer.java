package com.mycompany.watch_store;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mycompany.watch_store.controller.AdminOrderHandler;
import com.mycompany.watch_store.controller.AuthHandler;
import com.mycompany.watch_store.controller.CartHandler;
import com.mycompany.watch_store.controller.OrderHandler;
import com.mycompany.watch_store.controller.WatchHandler;
import com.mycompany.watch_store.http.ApiRouter;
import com.mycompany.watch_store.http.SessionManager;
import com.mycompany.watch_store.service.ApiViewService;
import com.sun.net.httpserver.HttpServer;

public final class ApiServer implements AutoCloseable {
    private final HttpServer server;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService maintenance;
    private final SessionManager sessions;
    private boolean closed;

    public ApiServer(String host, int port, String origin, Duration sessionLifetime) throws IOException {
        sessions = new SessionManager(sessionLifetime, origin.startsWith("https://"));
        ApiRouter router = new ApiRouter(sessions, origin);
        ApiViewService views = new ApiViewService();
        new AuthHandler(router, sessions);
        new WatchHandler(router);
        new CartHandler(router, views);
        new OrderHandler(router, views);
        new AdminOrderHandler(router, views);
        server = HttpServer.create(new InetSocketAddress(host, port), 64);
        executor = new ThreadPoolExecutor(4, 8, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(64), new ThreadPoolExecutor.CallerRunsPolicy());
        server.setExecutor(executor);
        server.createContext("/", router);
        maintenance = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "session-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        maintenance.scheduleAtFixedRate(sessions::purgeExpired, 1, 1, TimeUnit.MINUTES);
    }

    public void start() {
        server.start();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        server.stop(2);
        maintenance.shutdownNow();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            sessions.close();
        }
    }

    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(environment("PORT", "8080"));
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port.");
            String origin = environment("API_ORIGIN", "http://localhost:" + port);
            long seconds = Long.parseLong(environment("SESSION_TTL_SECONDS", "1800"));
            ApiServer api = new ApiServer(environment("API_HOST", "127.0.0.1"), port,
                    origin, Duration.ofSeconds(seconds));
            Runtime.getRuntime().addShutdownHook(new Thread(api::close, "api-shutdown"));
            api.start();
            System.out.println("=== ONLINE WATCH STORE API ===");
            System.out.println("Server running at " + origin);
        } catch (IOException | RuntimeException exception) {
            System.err.println("API startup failed. Check the port, origin, and local configuration.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static String environment(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
