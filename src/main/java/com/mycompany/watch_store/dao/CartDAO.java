package com.mycompany.watch_store.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;

public final class CartDAO {
    private final Connection transactionConnection;

    public CartDAO() {
        transactionConnection = null;
    }

    /** Caller owns commit/rollback/close and must disable autoCommit. */
    public CartDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    /** Returns null when this user has no cart; keeps persisted metadata unchanged. */
    public Cart findByUserId(int userId) throws SQLException {
        positiveId(userId);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            String sql = "SELECT id, created_at, updated_at FROM app.carts WHERE user_id = ? FOR SHARE";
            int cartId;
            LocalDateTime createdAt;
            LocalDateTime updatedAt;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    cartId = result.getInt("id");
                    createdAt = result.getObject("created_at", LocalDateTime.class);
                    updatedAt = result.getObject("updated_at", LocalDateTime.class);
                }
            }
            return new Cart(cartId, userId, createdAt, updatedAt, readItems(connection, cartId));
        });
    }

    /** Idempotent get-or-create, protected by UNIQUE(user_id). Missing user is a foreign-key error. */
    public int createCart(int userId) throws SQLException {
        positiveId(userId);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app.carts (user_id) VALUES (?) ON CONFLICT (user_id) DO NOTHING RETURNING id")) {
                statement.setInt(1, userId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getInt(1);
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM app.carts WHERE user_id = ?")) {
                statement.setInt(1, userId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Cart could not be created or found.");
                    }
                    return result.getInt(1);
                }
            }
        });
    }

    public List<CartItem> findItemsByCartId(int cartId) throws SQLException {
        positiveId(cartId);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            lockCart(connection, cartId, false);
            return readItems(connection, cartId);
        });
    }

    /** Atomically increments an existing item instead of creating a duplicate row. */
    public void addItem(int cartId, int watchId, int quantity) throws SQLException {
        positiveId(cartId);
        positiveId(watchId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            lockCart(connection, cartId, true);
            String sql = """
                    INSERT INTO app.cart_items (cart_id, watch_id, quantity) VALUES (?, ?, ?)
                    ON CONFLICT (cart_id, watch_id)
                    DO UPDATE SET quantity = app.cart_items.quantity + EXCLUDED.quantity
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, cartId);
                statement.setInt(2, watchId);
                statement.setInt(3, quantity);
                requireOneRow(statement.executeUpdate(), "Cart item was not added.");
            }
            touchCart(connection, cartId);
            return null;
        });
    }

    public void updateItemQuantity(int cartId, int watchId, int quantity) throws SQLException {
        positiveId(cartId);
        positiveId(watchId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            lockCart(connection, cartId, true);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE app.cart_items SET quantity = ? WHERE cart_id = ? AND watch_id = ?")) {
                statement.setInt(1, quantity);
                statement.setInt(2, cartId);
                statement.setInt(3, watchId);
                requireOneRow(statement.executeUpdate(), "Cart item does not exist.");
            }
            touchCart(connection, cartId);
            return null;
        });
    }

    public void removeItem(int cartId, int watchId) throws SQLException {
        positiveId(cartId);
        positiveId(watchId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            lockCart(connection, cartId, true);
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM app.cart_items WHERE cart_id = ? AND watch_id = ?")) {
                statement.setInt(1, cartId);
                statement.setInt(2, watchId);
                requireOneRow(statement.executeUpdate(), "Cart item does not exist.");
            }
            touchCart(connection, cartId);
            return null;
        });
    }

    /** Empty cart is a successful no-op; a missing cart is an error. */
    public void clearCart(int cartId) throws SQLException {
        positiveId(cartId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            lockCart(connection, cartId, true);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app.cart_items WHERE cart_id = ?")) {
                statement.setInt(1, cartId);
                if (statement.executeUpdate() > 0) {
                    touchCart(connection, cartId);
                }
            }
            return null;
        });
    }

    /** Serializes a user's cart workflows, including the initial cart creation race. */
    public boolean lockUser(int userId) throws SQLException {
        positiveId(userId);
        return lockReferencedRow("SELECT id FROM app.users WHERE id = ? FOR UPDATE", userId);
    }

    /** Keeps the stock/availability snapshot stable until this transaction finishes. */
    public boolean lockWatch(int watchId) throws SQLException {
        positiveId(watchId);
        return lockReferencedRow("SELECT id FROM app.watches WHERE id = ? FOR SHARE", watchId);
    }

    private boolean lockReferencedRow(String sql, int id) throws SQLException {
        if (transactionConnection == null || transactionConnection.getAutoCommit()) {
            throw new SQLException("Row locks require a caller-owned connection with autoCommit=false.");
        }
        try (PreparedStatement statement = transactionConnection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static List<CartItem> readItems(Connection connection, int cartId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, watch_id, quantity, created_at FROM app.cart_items WHERE cart_id = ? ORDER BY watch_id")) {
            statement.setInt(1, cartId);
            try (ResultSet result = statement.executeQuery()) {
                List<CartItem> items = new ArrayList<>();
                while (result.next()) {
                    CartItem item = new CartItem(cartId, result.getInt("watch_id"), result.getInt("quantity"));
                    item.setId(result.getInt("id"));
                    item.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
                    items.add(item);
                }
                return items;
            }
        }
    }

    private static void lockCart(Connection connection, int cartId, boolean exclusive) throws SQLException {
        String sql = exclusive ? "SELECT id FROM app.carts WHERE id = ? FOR UPDATE"
                : "SELECT id FROM app.carts WHERE id = ? FOR SHARE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, cartId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Cart does not exist.", "02000");
                }
            }
        }
    }

    private static void touchCart(Connection connection, int cartId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE app.carts SET updated_at = clock_timestamp() WHERE id = ?")) {
            statement.setInt(1, cartId);
            requireOneRow(statement.executeUpdate(), "Cart does not exist.");
        }
    }

    private static void requireOneRow(int count, String message) throws SQLException {
        if (count != 1) {
            throw new SQLException(message, "02000");
        }
    }

    private static void positiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be greater than 0.");
        }
    }
}
