package com.mycompany.watch_store.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.entity.User;

public final class UserDAO {
    private static final String COLUMNS = "id, full_name, email, password_hash, phone, role, created_at";
    private final Connection transactionConnection;

    /** Each operation owns and closes an independent connection. */
    public UserDAO() {
        transactionConnection = null;
    }

    /** Caller owns commit, rollback and close. A supplied connection is not thread-safe. */
    public UserDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    /** Returns null for a missing positive ID. */
    public User findById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("User ID must be greater than 0.");
        }
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM app.users WHERE id = ?")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? mapRow(result) : null;
                }
            }
        });
    }

    /** Case-insensitive lookup, also matching legacy emails with surrounding spaces. */
    public User findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank.");
        }
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM app.users WHERE LOWER(BTRIM(email)) = LOWER(BTRIM(?))")) {
                statement.setString(1, email);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    User user = mapRow(result);
                    if (result.next()) {
                        throw new SQLException("Multiple accounts match the normalized email.");
                    }
                    return user;
                }
            }
        });
    }

    /** Accepts an entity containing an already computed hash; does not verify passwords. */
    public int insert(User user) throws SQLException {
        if (user == null || user.getId() != 0) {
            throw new IllegalArgumentException("Insert requires a new user with ID 0.");
        }
        return withConnection(connection -> {
            String sql = """
                    INSERT INTO app.users (full_name, email, password_hash, phone, role)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (email) DO NOTHING
                    RETURNING id, created_at
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getFullName());
                statement.setString(2, user.getEmail());
                statement.setString(3, user.getPasswordHash());
                statement.setString(4, user.getPhone());
                statement.setString(5, user.getRole());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        // ON CONFLICT keeps a caller-owned transaction usable on duplicate email.
                        throw new SQLIntegrityConstraintViolationException("Email is already registered.", "23505");
                    }
                    int id = result.getInt("id");
                    user.setId(id);
                    user.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
                    return id;
                }
            }
        });
    }

    private static User mapRow(ResultSet result) throws SQLException {
        User user = new User(result.getString("full_name"), result.getString("email"),
                result.getString("password_hash"), result.getString("phone"), result.getString("role"));
        user.setId(result.getInt("id"));
        user.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
        return user;
    }

    private <T> T withConnection(SqlOperation<T> operation) throws SQLException {
        if (transactionConnection != null) {
            return operation.execute(transactionConnection);
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            return operation.execute(connection);
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
}
