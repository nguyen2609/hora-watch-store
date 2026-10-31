package com.mycompany.watch_store.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.CartDAO;
import com.mycompany.watch_store.dao.OrderDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;
import com.mycompany.watch_store.entity.Watch;

/** User-scoped reads and trusted internal management operations; no HTTP principal exists yet. */
public final class OrderService {
    private final Connection transactionConnection;

    public OrderService() {
        transactionConnection = null;
    }

    public OrderService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    public List<Order> getOrderHistory(int userId) throws SQLException {
        positiveId(userId);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            requireUser(connection, userId);
            return new OrderDAO(connection).findByUserId(userId);
        });
    }

    /** Both missing and foreign-owned orders produce the same not-found result. */
    public Order getOrderDetails(int userId, int orderId) throws SQLException {
        positiveId(userId);
        positiveId(orderId);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            requireUser(connection, userId);
            Order order = new OrderDAO(connection).findByIdForUser(userId, orderId);
            if (order == null) {
                throw new NoSuchElementException("Order not found for this user.");
            }
            return order;
        });
    }

    /** Internal management API: a future server boundary must authorize the administrator. */
    public void updateOrderStatus(int orderId, String newStatus) throws SQLException {
        positiveId(orderId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            Order order = lockOrder(connection, orderId);
            String previousStatus = order.getStatus();
            order.changeStatus(newStatus);
            List<OrderItem> items = order.getItems().stream()
                    .sorted(Comparator.comparingInt(OrderItem::getWatchId)).toList();
            WatchDAO watches = new WatchDAO(connection);
            if ("CANCELLED".equals(newStatus)) {
                // Acquire every watch lock before any database write, using the M09 ID order.
                for (OrderItem item : items) {
                    Watch watch = watches.findByIdForUpdate(item.getWatchId());
                    if (watch == null) {
                        throw new NoSuchElementException("An ordered watch no longer exists.");
                    }
                    watch.increaseStock(item.getQuantity());
                }
            }
            new OrderDAO(connection).updateStatus(orderId, previousStatus, order.getStatus());
            if ("CANCELLED".equals(newStatus)) {
                for (OrderItem item : items) {
                    watches.restoreStock(item.getWatchId(), item.getQuantity());
                }
            }
            return null;
        });
    }

    /** Shared M10 protocol: discover owner without locks, then lock user -> order. */
    static Order lockOrder(Connection connection, int orderId) throws SQLException {
        OrderDAO orders = new OrderDAO(connection);
        Integer ownerId = orders.findOwnerId(orderId);
        if (ownerId == null) {
            throw new NoSuchElementException("Order does not exist.");
        }
        requireUser(connection, ownerId);
        Order order = orders.findByIdForUpdate(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order does not exist.");
        }
        if (order.getUserId() != ownerId) {
            throw new SQLException("Order ownership changed during lock acquisition.");
        }
        return order;
    }

    private static void requireUser(Connection connection, int userId) throws SQLException {
        if (!new CartDAO(connection).lockUser(userId)) {
            throw new NoSuchElementException("User does not exist.");
        }
    }

    private static void positiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be greater than 0.");
        }
    }
}
