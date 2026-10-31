package com.mycompany.watch_store.demo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mycompany.watch_store.config.DatabaseConnection;

public final class DatabaseConnectionDemo {
    private DatabaseConnectionDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== DATABASE CONNECTION DEMO ===");
        try {
            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setReadOnly(true);
                System.out.println("Connection established successfully");
                String database = readDatabaseName(connection);
                int tableCount = countAppTables(connection);
                System.out.println("Database: " + database);
                System.out.println("Tables in app schema: " + tableCount);
                if (!"watch_store".equals(database) || tableCount != 8) {
                    throw new SQLException("Database verification did not match the expected database and table count.");
                }
            }
            // Reached only after Connection.close() completes successfully.
            System.out.println("Connection closed successfully");
        } catch (SQLException exception) {
            String password = System.getenv("DB_PASSWORD");
            if (password == null || password.isEmpty()) {
                System.err.println("Configuration error: DB_PASSWORD is required in the environment.");
            } else {
                // Driver messages, URLs and stack traces may contain connection details.
                System.err.println("Database verification failed. Check the JDBC driver, environment variables, "
                        + "PostgreSQL availability, database name and app schema table count.");
            }
            System.exit(1);
        }
    }

    private static String readDatabaseName(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT current_database()");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Database name query returned no result.");
            }
            return result.getString(1);
        }
    }

    private static int countAppTables(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "app");
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Table count query returned no result.");
                }
                return result.getInt(1);
            }
        }
    }
}
