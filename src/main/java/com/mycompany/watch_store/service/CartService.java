package com.mycompany.watch_store.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.CartDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;
import com.mycompany.watch_store.entity.Watch;

/** userId must come from trusted internal code; HTTP identity binding is a later milestone. */
public final class CartService {
    private final Connection transactionConnection;

    public CartService() {
        transactionConnection = null;
    }

    /** Caller owns the surrounding transaction and must disable autoCommit. */
    public CartService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    public Cart getCart(int userId) throws SQLException {
        positiveId(userId);
        return JdbcTransactions.execute(transactionConnection,
                connection -> getOrCreateCart(new CartDAO(connection), userId));
    }

    public void addToCart(int userId, int watchId, int quantity) throws SQLException {
        validateInput(userId, watchId, quantity);
        JdbcTransactions.execute(transactionConnection, connection -> {
            CartDAO carts = new CartDAO(connection);
            Cart cart = getOrCreateCart(carts, userId);
            Watch watch = availableWatch(connection, carts, watchId);
            long currentQuantity = cart.getItems().stream().filter(item -> item.getWatchId() == watchId)
                    .mapToLong(CartItem::getQuantity).findFirst().orElse(0);
            checkStock(currentQuantity + quantity, watch);
            carts.addItem(cart.getId(), watchId, quantity);
            return null;
        });
    }

    public void updateQuantity(int userId, int watchId, int quantity) throws SQLException {
        validateInput(userId, watchId, quantity);
        JdbcTransactions.execute(transactionConnection, connection -> {
            CartDAO carts = new CartDAO(connection);
            Cart cart = getOrCreateCart(carts, userId);
            requireItem(cart, watchId);
            Watch watch = availableWatch(connection, carts, watchId);
            checkStock(quantity, watch);
            carts.updateItemQuantity(cart.getId(), watchId, quantity);
            return null;
        });
    }

    public void removeFromCart(int userId, int watchId) throws SQLException {
        positiveId(userId);
        positiveId(watchId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            CartDAO carts = new CartDAO(connection);
            Cart cart = getOrCreateCart(carts, userId);
            requireItem(cart, watchId);
            carts.removeItem(cart.getId(), watchId);
            return null;
        });
    }

    public void clearCart(int userId) throws SQLException {
        positiveId(userId);
        JdbcTransactions.execute(transactionConnection, connection -> {
            CartDAO carts = new CartDAO(connection);
            Cart cart = getOrCreateCart(carts, userId);
            carts.clearCart(cart.getId());
            return null;
        });
    }

    /** Uses current database prices, even for items discontinued after being added. */
    public BigDecimal calculateCartTotal(int userId) throws SQLException {
        positiveId(userId);
        return JdbcTransactions.execute(transactionConnection, connection -> {
            Cart cart = getOrCreateCart(new CartDAO(connection), userId);
            WatchDAO watches = new WatchDAO(connection);
            BigDecimal total = BigDecimal.ZERO;
            for (CartItem item : cart.getItems()) {
                Watch watch = watches.findById(item.getWatchId());
                if (watch == null) {
                    throw new NoSuchElementException("A cart watch no longer exists.");
                }
                total = total.add(watch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            return total;
        });
    }

    private static Cart getOrCreateCart(CartDAO carts, int userId) throws SQLException {
        if (!carts.lockUser(userId)) {
            throw new NoSuchElementException("User does not exist.");
        }
        Cart cart = carts.findByUserId(userId);
        if (cart == null) {
            carts.createCart(userId);
            cart = carts.findByUserId(userId);
        }
        if (cart == null || cart.getUserId() != userId) {
            throw new SQLException("The user's cart could not be loaded.");
        }
        return cart;
    }

    private static Watch availableWatch(Connection connection, CartDAO carts, int watchId) throws SQLException {
        if (!carts.lockWatch(watchId)) {
            throw new NoSuchElementException("Available watch not found.");
        }
        Watch watch = new WatchDAO(connection).findById(watchId);
        if (watch == null || !watch.isAvailable()) {
            throw new NoSuchElementException("Available watch not found.");
        }
        return watch;
    }

    private static void requireItem(Cart cart, int watchId) {
        if (cart.getItems().stream().noneMatch(item -> item.getWatchId() == watchId)) {
            throw new NoSuchElementException("Cart item does not exist.");
        }
    }

    private static void checkStock(long quantity, Watch watch) {
        if (quantity > watch.getStockQuantity()) {
            throw new IllegalArgumentException("Requested cart quantity exceeds current stock.");
        }
    }

    private static void validateInput(int userId, int watchId, int quantity) {
        positiveId(userId);
        positiveId(watchId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
    }

    private static void positiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be greater than 0.");
        }
    }
}
