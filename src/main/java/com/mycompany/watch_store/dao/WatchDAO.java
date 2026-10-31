package com.mycompany.watch_store.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.factory.WatchFactory;

public final class WatchDAO {
    private static final String COLUMNS = "id, category_id, name, brand, model_number, price, "
            + "stock_quantity, watch_type, movement_type, battery_life_hours, description, "
            + "image_url, available, created_at";

    private final Connection transactionConnection;

    /** Each operation opens and closes its own connection. */
    public WatchDAO() {
        transactionConnection = null;
    }

    /** Caller owns this connection, including commit, rollback and close. Not thread-safe. */
    public WatchDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    /** Returns all watches, including unavailable watches, ordered by ID. */
    public List<Watch> findAll() throws SQLException {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM app.watches ORDER BY id");
                 ResultSet result = statement.executeQuery()) {
                List<Watch> watches = new ArrayList<>();
                while (result.next()) {
                    watches.add(mapRow(result));
                }
                return watches;
            }
        });
    }

    /** Returns null if the positive ID does not exist; includes unavailable watches. */
    public Watch findById(int id) throws SQLException {
        requirePositiveId(id);
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM app.watches WHERE id = ?")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? mapRow(result) : null;
                }
            }
        });
    }

    /** Locks a watch until the caller's transaction ends; returns null for a missing ID. */
    public Watch findByIdForUpdate(int id) throws SQLException {
        requirePositiveId(id);
        if (transactionConnection == null || transactionConnection.getAutoCommit()) {
            throw new SQLException("Stock locking requires a borrowed connection with autoCommit=false.");
        }
        try (PreparedStatement statement = transactionConnection.prepareStatement(
                "SELECT " + COLUMNS + " FROM app.watches WHERE id = ? FOR UPDATE")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapRow(result) : null;
            }
        }
    }

    /** Atomic database-side guard; never replaces stock with a stale Java field. */
    public void deductStock(int id, int quantity) throws SQLException {
        requirePositiveId(id);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock deduction quantity must be greater than 0.");
        }
        withConnection(connection -> {
            String sql = """
                    UPDATE app.watches SET stock_quantity = stock_quantity - ?
                    WHERE id = ? AND available = TRUE AND stock_quantity >= ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, quantity);
                statement.setInt(2, id);
                statement.setInt(3, quantity);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Watch stock could not be deducted safely.", "02000");
                }
            }
            return null;
        });
    }

    /** Inserts only a new entity and assigns the database-generated ID and creation time. */
    public int insert(Watch watch) throws SQLException {
        validateForWrite(watch);
        if (watch.getId() != 0) {
            throw new IllegalArgumentException("Only a new watch with ID 0 can be inserted.");
        }
        return withConnection(connection -> {
            String sql = """
                    INSERT INTO app.watches
                        (category_id, name, brand, model_number, price, stock_quantity, watch_type,
                         movement_type, battery_life_hours, description, image_url, available)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id, created_at
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindWatch(statement, watch);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Watch insert returned no generated ID.");
                    }
                    int id = result.getInt("id");
                    watch.setId(id);
                    watch.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
                    return id;
                }
            }
        });
    }

    /** Updates one persisted watch without changing created_at. */
    public void update(Watch watch) throws SQLException {
        validateForWrite(watch);
        requirePositiveId(watch.getId());
        withConnection(connection -> {
            String sql = """
                    UPDATE app.watches
                    SET category_id = ?, name = ?, brand = ?, model_number = ?, price = ?,
                        stock_quantity = ?, watch_type = ?, movement_type = ?, battery_life_hours = ?,
                        description = ?, image_url = ?, available = ?
                    WHERE id = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindWatch(statement, watch);
                statement.setInt(13, watch.getId());
                requireOneRow(statement.executeUpdate());
            }
            return null;
        });
    }

    /** Soft delete: mark unavailable, preserving foreign-key references and order history. */
    public void delete(int id) throws SQLException {
        requirePositiveId(id);
        withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE app.watches SET available = FALSE WHERE id = ?")) {
                statement.setInt(1, id);
                requireOneRow(statement.executeUpdate());
            }
            return null;
        });
    }

    private static Watch mapRow(ResultSet result) throws SQLException {
        return WatchFactory.createWatch(result.getInt("id"), result.getInt("category_id"),
                result.getString("name"), result.getString("brand"), result.getString("model_number"),
                result.getBigDecimal("price"), result.getInt("stock_quantity"), result.getString("watch_type"),
                result.getString("movement_type"), result.getObject("battery_life_hours", Integer.class),
                result.getString("description"), result.getString("image_url"), result.getBoolean("available"),
                result.getObject("created_at", LocalDateTime.class));
    }

    private static void bindWatch(PreparedStatement statement, Watch watch) throws SQLException {
        statement.setInt(1, watch.getCategoryId());
        statement.setString(2, watch.getName());
        statement.setString(3, watch.getBrand());
        statement.setString(4, watch.getModelNumber());
        statement.setBigDecimal(5, watch.getPrice());
        statement.setInt(6, watch.getStockQuantity());
        statement.setString(7, watch.getWatchType());
        if (watch instanceof AutomaticWatch automatic) {
            statement.setString(8, automatic.getMovementType());
            statement.setNull(9, Types.INTEGER);
        } else if (watch instanceof SmartWatch smart) {
            statement.setNull(8, Types.VARCHAR);
            statement.setInt(9, smart.getBatteryLifeHours());
        } else {
            throw new IllegalArgumentException("Unsupported watch subclass.");
        }
        statement.setString(10, watch.getDescription());
        statement.setString(11, watch.getImageUrl());
        statement.setBoolean(12, watch.isAvailable());
    }

    private static void validateForWrite(Watch watch) {
        if (watch == null) {
            throw new IllegalArgumentException("Watch must not be null.");
        }
        requireLength(watch.getName(), 150);
        requireLength(watch.getBrand(), 100);
        requireLength(watch.getModelNumber(), 100);
        if (watch instanceof AutomaticWatch automatic) {
            requireLength(automatic.getMovementType(), 100);
        } else if (!(watch instanceof SmartWatch)) {
            throw new IllegalArgumentException("Unsupported watch subclass.");
        }
    }

    private static void requireLength(String value, int maxLength) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException("Required watch text exceeds its schema limit or is blank.");
        }
    }

    private static void requirePositiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Watch ID must be greater than 0.");
        }
    }

    private static void requireOneRow(int count) throws SQLException {
        if (count != 1) {
            throw new SQLException("Watch ID does not exist.", "02000");
        }
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
