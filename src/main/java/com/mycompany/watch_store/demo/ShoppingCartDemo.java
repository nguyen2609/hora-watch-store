package com.mycompany.watch_store.demo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.dao.CartDAO;
import com.mycompany.watch_store.dao.UserDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.User;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.service.AuthService;
import com.mycompany.watch_store.service.CartService;

public final class ShoppingCartDemo {
    private ShoppingCartDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== SHOPPING CART DEMO ===");
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connection established successfully");
            demonstrateCart(connection);
        } catch (SQLException | RuntimeException exception) {
            System.err.println("Shopping cart demonstration failed. Check local configuration and schema compatibility.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void demonstrateCart(Connection connection) throws SQLException {
        String marker = "m08-" + UUID.randomUUID();
        String email = marker + "@example.invalid";
        int userId;
        int cartId;
        int automaticId;
        int smartId;
        int unavailableId;
        connection.setAutoCommit(false);
        try {
            User user = new AuthService(new UserDAO(connection)).register("M08 Temporary Customer", email,
                    UUID.randomUUID().toString().substring(0, 15), null);
            userId = user.getId();
            int categoryId = chooseCategory(connection, marker);
            WatchDAO watches = new WatchDAO(connection);
            Watch automatic = new AutomaticWatch(categoryId, "M08 automatic", "Demo Brand", marker + "-auto",
                    new BigDecimal("6500000"), 5, null, null, true, "Self-winding");
            Watch smart = new SmartWatch(categoryId, "M08 smart", "Demo Brand", marker + "-smart",
                    new BigDecimal("4500000"), 3, null, null, true, 48);
            Watch unavailable = new SmartWatch(categoryId, "M08 discontinued", "Demo Brand", marker + "-off",
                    new BigDecimal("1000000"), 7, null, null, false, 24);
            automaticId = watches.insert(automatic);
            smartId = watches.insert(smart);
            unavailableId = watches.insert(unavailable);

            CartService service = new CartService(connection);
            CartDAO carts = new CartDAO(connection);
            Cart empty = service.getCart(userId);
            cartId = empty.getId();
            require(empty.getItems().isEmpty() && service.calculateCartTotal(userId).signum() == 0,
                    "New cart must be empty.");
            require(service.getCart(userId).getId() == cartId && carts.createCart(userId) == cartId,
                    "Cart creation must be idempotent.");
            System.out.println("Created one cart for the temporary user; empty total: 0");

            service.addToCart(userId, automaticId, 1);
            CartItem firstLine = item(service.getCart(userId), automaticId);
            service.addToCart(userId, smartId, 1);
            service.addToCart(userId, automaticId, 1);
            Cart combined = service.getCart(userId);
            CartItem combinedLine = item(combined, automaticId);
            require(combined.getItems().size() == 2 && combinedLine.getQuantity() == 2
                    && combinedLine.getId() == firstLine.getId()
                    && combinedLine.getCreatedAt().equals(firstLine.getCreatedAt()), "Atomic cart addition failed.");
            require(carts.findItemsByCartId(cartId).stream().allMatch(line -> line.getId() > 0),
                    "Cart item IDs were not mapped.");
            System.out.println("Automatic + smart added; repeated automatic addition: 2 rows, automatic quantity=2");

            service.updateQuantity(userId, automaticId, 3);
            require(service.calculateCartTotal(userId).compareTo(new BigDecimal("24000000")) == 0,
                    "Cart total is incorrect.");
            System.out.println("Updated automatic quantity=3; current-price total: " + service.calculateCartTotal(userId));
            automatic.setPrice(new BigDecimal("6600000"));
            watches.update(automatic);
            require(service.calculateCartTotal(userId).compareTo(new BigDecimal("24300000")) == 0,
                    "Total did not use the current database price.");
            System.out.println("After temporary watch price change, current-price total: " + service.calculateCartTotal(userId));

            Cart beforeRejections = service.getCart(userId);
            expect("Combined quantity exceeds stock", IllegalArgumentException.class,
                    () -> service.addToCart(userId, automaticId, 3));
            expect("Quantity addition cannot overflow int", IllegalArgumentException.class,
                    () -> service.addToCart(userId, automaticId, Integer.MAX_VALUE));
            expect("Zero quantity", IllegalArgumentException.class, () -> service.addToCart(userId, smartId, 0));
            expect("Negative quantity", IllegalArgumentException.class, () -> service.updateQuantity(userId, smartId, -1));
            expect("Update exceeds stock", IllegalArgumentException.class, () -> service.updateQuantity(userId, smartId, 4));
            expect("Unavailable watch", java.util.NoSuchElementException.class,
                    () -> service.addToCart(userId, unavailableId, 1));
            expect("Missing cart item", java.util.NoSuchElementException.class,
                    () -> service.updateQuantity(userId, unavailableId, 1));
            expectSqlState("Database quantity constraint", "23514",
                    () -> carts.updateItemQuantity(cartId, smartId, 0));
            expectSqlState("Missing DAO item", "02000", () -> carts.removeItem(cartId, unavailableId));
            Cart afterRejections = service.getCart(userId);
            require(afterRejections.getUpdatedAt().equals(beforeRejections.getUpdatedAt())
                    && item(afterRejections, automaticId).getQuantity() == 3,
                    "Rejected operations changed the cart.");
            System.out.println("Failed operations rolled back to savepoints; previous cart changes preserved.");

            User other = new AuthService(new UserDAO(connection)).register("M08 Other Customer", "other-" + email,
                    UUID.randomUUID().toString().substring(0, 15), null);
            expect("Another user's item removal", java.util.NoSuchElementException.class,
                    () -> service.removeFromCart(other.getId(), automaticId));
            require(carts.findByUserId(other.getId()) == null
                    && item(service.getCart(userId), automaticId).getQuantity() == 3,
                    "An operation for another user affected the original cart.");
            System.out.println("Ownership verified: another user cannot remove this cart's item.");

            verifyMetadata(connection, afterRejections);
            require(afterRejections.getCreatedAt().equals(empty.getCreatedAt())
                    && afterRejections.getUpdatedAt().isAfter(empty.getUpdatedAt()), "Cart timestamp semantics failed.");
            System.out.println("Database IDs/created_at restored; updated_at advances only on successful changes.");
            require(watches.findById(automaticId).getStockQuantity() == 5
                    && watches.findById(smartId).getStockQuantity() == 3, "Adding to cart changed watch stock.");
            System.out.println("Watch stock unchanged: automatic=5, smart=3");

            service.removeFromCart(userId, smartId);
            require(service.getCart(userId).getItems().size() == 1, "Cart item removal failed.");
            expect("Remove missing item", java.util.NoSuchElementException.class,
                    () -> service.removeFromCart(userId, smartId));
            System.out.println("Removed smartwatch; remaining cart rows: 1");
            service.clearCart(userId);
            Cart cleared = service.getCart(userId);
            require(cleared.getId() == cartId && cleared.getItems().isEmpty()
                    && service.calculateCartTotal(userId).equals(BigDecimal.ZERO), "Cart clearing failed.");
            service.clearCart(userId);
            require(service.getCart(userId).getUpdatedAt().equals(cleared.getUpdatedAt()),
                    "Clearing an empty cart should not change its timestamp.");
            System.out.println("Cart cleared: 0 items, total=0; cart itself preserved.");
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
            System.out.println("Shopping cart demo transaction rolled back.");
        }

        require(new UserDAO().findByEmail(email) == null && new UserDAO().findByEmail("other-" + email) == null
                && new CartDAO().findByUserId(userId) == null,
                "Temporary user or cart remains.");
        WatchDAO independent = new WatchDAO();
        require(independent.findById(automaticId) == null && independent.findById(smartId) == null
                && independent.findById(unavailableId) == null, "Temporary watches remain.");
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (SELECT COUNT(*) FROM app.cart_items WHERE cart_id = ?)
                     + (SELECT COUNT(*) FROM app.categories WHERE name = ?)
                """)) {
            statement.setInt(1, cartId);
            statement.setString(2, marker);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getLong(1) == 0, "Temporary items or category remain.");
            }
        }
        System.out.println("Cleanup verified: no temporary user, cart, items, watches or category remain.");
        System.out.println("Identity sequence gaps after rollback are expected.");
        System.out.println("Shopping cart demo finished normally.");
    }

    private static int chooseCategory(Connection connection, String marker) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM app.categories ORDER BY id LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                return result.getInt(1);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app.categories (name) VALUES (?) RETURNING id")) {
            statement.setString(1, marker);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Temporary category was not created.");
                return result.getInt(1);
            }
        }
    }

    private static void verifyMetadata(Connection connection, Cart cart) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT created_at, updated_at FROM app.carts WHERE id = ?")) {
            statement.setInt(1, cart.getId());
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && cart.getCreatedAt().equals(result.getObject(1, LocalDateTime.class))
                        && cart.getUpdatedAt().equals(result.getObject(2, LocalDateTime.class)),
                        "Hydrating a cart changed its stored timestamps.");
            }
        }
    }

    private static CartItem item(Cart cart, int watchId) {
        return cart.getItems().stream().filter(line -> line.getWatchId() == watchId).findFirst().orElseThrow();
    }

    private static void expect(String label, Class<? extends RuntimeException> type, SqlAction action) throws SQLException {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (!type.isInstance(exception)) {
                throw exception;
            }
            System.out.println("Expected rejection [" + label + "]: " + exception.getMessage());
            return;
        }
        throw new IllegalStateException("Expected validation did not reject: " + label);
    }

    private static void expectSqlState(String label, String state, SqlAction action) throws SQLException {
        try {
            action.run();
        } catch (SQLException exception) {
            if (!state.equals(exception.getSQLState())) {
                throw exception;
            }
            System.out.println("Expected SQL rejection [" + label + "]: " + state);
            return;
        }
        throw new IllegalStateException("Expected SQL rejection did not occur: " + label);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
