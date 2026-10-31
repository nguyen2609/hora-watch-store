package com.mycompany.watch_store.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    /** Returns a new connection; the caller must close it. */
    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            url = "jdbc:postgresql://localhost:5433/watch_store";
        }
        String user = System.getenv("DB_USER");
        if (user == null || user.isBlank()) {
            user = "postgres";
        }
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            throw new SQLException("DB_PASSWORD is required. Set it in the environment before connecting.");
        }
        return DriverManager.getConnection(url, user, password);
    }
}
