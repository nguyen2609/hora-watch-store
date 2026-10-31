package com.mycompany.watch_store.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;

public final class OrderDAO {
    private final Connection transactionConnection;

    public OrderDAO() {
        transactionConnection = null;
    }

    /** Caller owns commit/rollback/close; autoCommit must be false. */
    public OrderDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    /** Persists an order aggregate plus its unpaid COD record, not stock or cart changes. */
    public Order insert(Order draft) throws SQLException {
        if (draft == null || draft.getId() != 0 || draft.getItems().isEmpty()
                || !"PENDING".equals(draft.getStatus()) || !"COD".equals(draft.getPaymentMethod())) {
            throw new IllegalArgumentException("Insert requires a new, nonempty PENDING COD order.");
        }
        return JdbcTransactions.execute(transactionConnection, connection -> {
            int orderId;
            LocalDateTime createdAt;
            String sql = """
                    INSERT INTO app.orders
                        (user_id, recipient_name, recipient_phone, shipping_address, total_amount)
                    VALUES (?, ?, ?, ?, ?) RETURNING id, created_at
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, draft.getUserId());
                statement.setString(2, draft.getRecipientName());
                statement.setString(3, draft.getRecipientPhone());
                statement.setString(4, draft.getShippingAddress());
                statement.setBigDecimal(5, draft.calculateTotal());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Order insert returned no generated ID.");
                    }
                    orderId = result.getInt("id");
                    createdAt = result.getObject("created_at", LocalDateTime.class);
                }
            }
            List<OrderItem> storedItems = new ArrayList<>();
            for (OrderItem item : draft.getItems()) {
                storedItems.add(insertItem(connection, orderId, item));
            }
            // Defaults are COD / UNPAID / paid_at NULL in the inspected schema.
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO app.payments (order_id, amount) VALUES (?, ?)")) {
                statement.setInt(1, orderId);
                statement.setBigDecimal(2, draft.getTotalAmount());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Unpaid COD record was not created.");
                }
            }
            return new Order(orderId, draft.getUserId(), draft.getRecipientName(), draft.getRecipientPhone(),
                    draft.getShippingAddress(), "PENDING", createdAt, storedItems);
        });
    }

    /** Internal persistence lookup; authorization of an order owner is a later API responsibility. */
    public Order findById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Order ID must be greater than 0.");
        }
        return JdbcTransactions.execute(transactionConnection, connection -> {
            // Also prevents a concurrent status update during aggregate hydration.
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT user_id, recipient_name, recipient_phone, shipping_address, total_amount, "
                            + "status, payment_method, created_at FROM app.orders WHERE id = ? FOR SHARE")) {
                statement.setInt(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return null;
                    }
                    if (!"COD".equals(result.getString("payment_method"))) {
                        throw new SQLException("Unsupported stored payment method.");
                    }
                    Order order = new Order(id, result.getInt("user_id"), result.getString("recipient_name"),
                            result.getString("recipient_phone"), result.getString("shipping_address"),
                            result.getString("status"), result.getObject("created_at", LocalDateTime.class),
                            readItems(connection, id));
                    if (order.getTotalAmount().compareTo(result.getBigDecimal("total_amount")) != 0) {
                        throw new SQLException("Stored order total does not match its items.");
                    }
                    return order;
                }
            }
        });
    }

    private static OrderItem insertItem(Connection connection, int orderId, OrderItem item) throws SQLException {
        if (item.getOrderId() != 0 && item.getOrderId() != orderId) {
            throw new IllegalArgumentException("Order item belongs to a different order.");
        }
        String sql = """
                INSERT INTO app.order_items (order_id, watch_id, quantity, unit_price)
                VALUES (?, ?, ?, ?) RETURNING id, created_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            statement.setInt(2, item.getWatchId());
            statement.setInt(3, item.getQuantity());
            statement.setBigDecimal(4, item.getUnitPrice());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Order item insert returned no generated ID.");
                }
                OrderItem stored = new OrderItem(orderId, item.getWatchId(), item.getQuantity(), item.getUnitPrice());
                stored.setId(result.getInt("id"));
                stored.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
                return stored;
            }
        }
    }

    private static List<OrderItem> readItems(Connection connection, int orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, watch_id, quantity, unit_price, created_at FROM app.order_items WHERE order_id = ? ORDER BY watch_id")) {
            statement.setInt(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderItem> items = new ArrayList<>();
                while (result.next()) {
                    OrderItem item = new OrderItem(orderId, result.getInt("watch_id"), result.getInt("quantity"),
                            result.getBigDecimal("unit_price"));
                    item.setId(result.getInt("id"));
                    item.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
                    items.add(item);
                }
                return items;
            }
        }
    }
}
