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
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;
import com.mycompany.watch_store.entity.Watch;

/** userId must be supplied by trusted internal code, not an unverified HTTP field. */
public final class CheckoutService {
    private final Connection transactionConnection;

    public CheckoutService() {
        transactionConnection = null;
    }

    /** Caller owns the outer transaction. Successful checkout does not commit it. */
    public CheckoutService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    public Order checkout(int userId, String recipientName, String recipientPhone, String shippingAddress)
            throws SQLException {
        // Validate shipping information before opening a transaction or taking locks.
        Order draft = new Order(userId, recipientName, recipientPhone, shippingAddress);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            CartDAO carts = new CartDAO(connection);
            if (!carts.lockUser(userId)) {
                throw new NoSuchElementException("User does not exist.");
            }
            Cart cart = carts.findByUserIdForUpdate(userId);
            if (cart == null || cart.getItems().isEmpty()) {
                throw new IllegalStateException("Checkout requires an existing, nonempty cart.");
            }
            List<CartItem> items = cart.getItems().stream()
                    .sorted(Comparator.comparingInt(CartItem::getWatchId)).toList();
            WatchDAO watches = new WatchDAO(connection);
            // Lock order: user -> cart -> watches in ascending ID order, before any write.
            for (CartItem item : items) {
                if (item.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Checkout quantities must be greater than 0.");
                }
                Watch watch = watches.findByIdForUpdate(item.getWatchId());
                if (watch == null || !watch.isAvailable()) {
                    throw new NoSuchElementException("A cart watch is missing or unavailable.");
                }
                if (item.getQuantity() > watch.getStockQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for checkout.");
                }
                draft.addItem(new OrderItem(0, watch.getId(), item.getQuantity(), watch.getPrice()));
            }
            Order stored = new OrderDAO(connection).insert(draft);
            for (OrderItem item : stored.getItems()) {
                watches.deductStock(item.getWatchId(), item.getQuantity());
            }
            carts.clearCart(cart.getId());
            return stored;
        });
    }
}
