package com.mycompany.watch_store.demo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.dao.CartDAO;
import com.mycompany.watch_store.dao.OrderDAO;
import com.mycompany.watch_store.dao.UserDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.service.AuthService;
import com.mycompany.watch_store.service.CartService;
import com.mycompany.watch_store.service.CheckoutService;

public final class CheckoutDemo {
    private CheckoutDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== CHECKOUT DEMO ===");
        try (Connection connection = DatabaseConnection.getConnection()) {
            demonstrateCheckout(connection);
        } catch (SQLException | RuntimeException exception) {
            System.err.println("Checkout demonstration failed; check configuration and schema compatibility.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void demonstrateCheckout(Connection connection) throws SQLException {
        String marker = "m09-" + UUID.randomUUID();
        String email = marker + "@example.invalid";
        int userId;
        int automaticId;
        int smartId;
        int cartId;
        List<Integer> orderIds = new ArrayList<>();
        connection.setAutoCommit(false);
        try {
            userId = new AuthService(new UserDAO(connection)).register("M09 Temporary Customer", email,
                    UUID.randomUUID().toString().substring(0, 15), null).getId();
            int categoryId = chooseCategory(connection, marker);
            WatchDAO watches = new WatchDAO(connection);
            automaticId = watches.insert(new AutomaticWatch(categoryId, "M09 automatic", "Demo", marker + "-auto",
                    new BigDecimal("6500000"), 10, null, null, true, "Self-winding"));
            smartId = watches.insert(new SmartWatch(categoryId, "M09 smart", "Demo", marker + "-smart",
                    new BigDecimal("4500000"), 8, null, null, true, 48));
            CartDAO cartDAO = new CartDAO(connection);
            CartService carts = new CartService(connection);
            CheckoutService checkout = new CheckoutService(connection);
            OrderDAO orders = new OrderDAO(connection);

            expect("Missing cart", IllegalStateException.class, () -> buy(checkout, userId));
            require(cartDAO.findByUserId(userId) == null, "Rejected checkout created a cart.");
            cartId = carts.getCart(userId).getId();
            expect("Empty cart", IllegalStateException.class, () -> buy(checkout, userId));

            carts.addToCart(userId, automaticId, 1);
            Order single = buy(checkout, userId);
            orderIds.add(single.getId());
            verifyOrder(connection, single, new BigDecimal("6500000"), 1);
            require(watches.findById(automaticId).getStockQuantity() == 9
                    && carts.getCart(userId).getItems().isEmpty(), "Single-watch checkout failed.");
            System.out.println("Single-watch checkout: total=6500000, stock=9, cart empty.");

            carts.addToCart(userId, smartId, 1);
            carts.addToCart(userId, automaticId, 2);
            Watch repriced = watches.findById(automaticId);
            repriced.setPrice(new BigDecimal("6600000"));
            watches.update(repriced);
            Order multiple = buy(checkout, userId);
            orderIds.add(multiple.getId());
            verifyOrder(connection, multiple, new BigDecimal("17700000"), 2);
            require(watches.findById(automaticId).getStockQuantity() == 7
                    && watches.findById(smartId).getStockQuantity() == 7
                    && carts.getCart(userId).getItems().isEmpty(), "Multi-watch checkout failed.");
            System.out.println("Multi-watch checkout: 6600000 x 2 + 4500000 = 17700000; stock=7/7.");
            System.out.println("Orders PENDING/COD; payments UNPAID with paid_at NULL.");

            repriced = watches.findById(automaticId);
            repriced.setPrice(new BigDecimal("6700000"));
            watches.update(repriced);
            Order historical = orders.findById(multiple.getId());
            require(historical.getItems().stream().filter(item -> item.getWatchId() == automaticId).findFirst()
                    .orElseThrow().getUnitPrice().compareTo(new BigDecimal("6600000")) == 0,
                    "Historical purchase price was changed.");
            System.out.println("Historical unit prices remain unchanged after repricing.");

            carts.addToCart(userId, automaticId, 2);
            Watch changedStock = watches.findById(automaticId);
            changedStock.setStockQuantity(1);
            watches.update(changedStock);
            List<String> before = snapshot(connection, userId, automaticId, smartId);
            expect("Stock fell after adding to cart", IllegalArgumentException.class, () -> buy(checkout, userId));
            require(before.equals(snapshot(connection, userId, automaticId, smartId)), "Stock rejection changed state.");

            changedStock.setStockQuantity(7);
            changedStock.setAvailable(false);
            watches.update(changedStock);
            before = snapshot(connection, userId, automaticId, smartId);
            expect("Watch discontinued after adding to cart", NoSuchElementException.class, () -> buy(checkout, userId));
            require(before.equals(snapshot(connection, userId, automaticId, smartId)), "Availability rejection changed state.");
            changedStock.setAvailable(true);
            watches.update(changedStock);
            carts.addToCart(userId, smartId, 1);

            before = snapshot(connection, userId, automaticId, smartId);
            long previousOrders = countOrders(connection, userId);
            boolean[] observedPartialWork = {false};
            Connection failureConnection = failAfterCartDeletion(connection, userId, automaticId, smartId,
                    previousOrders, observedPartialWork);
            expect("Simulated SQL failure after stock deduction and cart deletion", SQLException.class,
                    () -> buy(new CheckoutService(failureConnection), userId));
            require(observedPartialWork[0], "Failure injection did not observe the intended database writes.");
            require(before.equals(snapshot(connection, userId, automaticId, smartId)), "Checkout did not fully roll back.");
            System.out.println("Full rollback verified: orders/items/payment/stock/cart and timestamps restored.");

            Watch overflow = watches.findById(automaticId);
            overflow.setPrice(new BigDecimal("999999999999"));
            watches.update(overflow);
            before = snapshot(connection, userId, automaticId, smartId);
            expect("Order total exceeds NUMERIC(12,0)", IllegalArgumentException.class, () -> buy(checkout, userId));
            require(before.equals(snapshot(connection, userId, automaticId, smartId)), "Total overflow changed state.");
            require(new OrderDAO().findById(single.getId()) == null,
                    "Borrowed checkout unexpectedly committed its caller's transaction.");
            System.out.println("Caller-owned connection remains open and uncommitted.");
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
            System.out.println("Outer demo transaction rolled back.");
        }
        require(new UserDAO().findByEmail(email) == null && new CartDAO().findByUserId(userId) == null,
                "Temporary customer or cart remains.");
        WatchDAO independentWatches = new WatchDAO();
        require(independentWatches.findById(automaticId) == null && independentWatches.findById(smartId) == null,
                "Temporary watches remain.");
        for (int id : orderIds) {
            require(new OrderDAO().findById(id) == null, "Temporary order remains.");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT (SELECT COUNT(*) FROM app.order_items WHERE watch_id IN (?, ?))
                     + (SELECT COUNT(*) FROM app.payments WHERE order_id IN (?, ?))
                     + (SELECT COUNT(*) FROM app.cart_items WHERE cart_id = ?)
                     + (SELECT COUNT(*) FROM app.categories WHERE name = ?)
                """)) {
            statement.setInt(1, automaticId);
            statement.setInt(2, smartId);
            statement.setInt(3, orderIds.get(0));
            statement.setInt(4, orderIds.get(1));
            statement.setInt(5, cartId);
            statement.setString(6, marker);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getLong(1) == 0, "Temporary dependent rows remain.");
            }
        }
        System.out.println("Cleanup verified: no checkout fixture data remains. Identity gaps are expected.");
        System.out.println("Checkout demo finished normally.");
    }

    private static Connection failAfterCartDeletion(Connection real, int userId, int automaticId, int smartId,
                                                     long previousOrders, boolean[] observed) {
        return (Connection) Proxy.newProxyInstance(CheckoutDemo.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement") && args != null && args.length > 0
                            && args[0] instanceof String sql && sql.startsWith("UPDATE app.carts SET updated_at")) {
                        observed[0] = countOrders(real, userId) == previousOrders + 1
                                && new CartDAO(real).findByUserId(userId).getItems().isEmpty()
                                && new WatchDAO(real).findById(automaticId).getStockQuantity() == 5
                                && new WatchDAO(real).findById(smartId).getStockQuantity() == 6;
                        throw new SQLException("Simulated failure after cart deletion.", "P0001");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
    }

    private static List<String> snapshot(Connection connection, int userId, int automaticId, int smartId) throws SQLException {
        List<String> state = new ArrayList<>();
        state.add(Long.toString(countOrders(connection, userId)));
        for (String sql : List.of(
                "SELECT COUNT(*) FROM app.order_items i JOIN app.orders o ON o.id=i.order_id WHERE o.user_id=?",
                "SELECT COUNT(*) FROM app.payments p JOIN app.orders o ON o.id=p.order_id WHERE o.user_id=?")) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    state.add(Long.toString(result.getLong(1)));
                }
            }
        }
        Cart cart = new CartDAO(connection).findByUserId(userId);
        state.add(cart.getId() + ":" + cart.getCreatedAt() + ":" + cart.getUpdatedAt());
        cart.getItems().forEach(item -> state.add(item.getId() + ":" + item.getWatchId() + ":"
                + item.getQuantity() + ":" + item.getCreatedAt()));
        for (int watchId : new int[]{automaticId, smartId}) {
            Watch watch = new WatchDAO(connection).findById(watchId);
            state.add(watchId + ":" + watch.getStockQuantity() + ":" + watch.getPrice() + ":" + watch.isAvailable());
        }
        return state;
    }

    private static void verifyOrder(Connection connection, Order order, BigDecimal total, int lineCount) throws SQLException {
        Order stored = new OrderDAO(connection).findById(order.getId());
        require(stored != null && stored.getTotalAmount().compareTo(total) == 0 && stored.getItems().size() == lineCount
                && "PENDING".equals(stored.getStatus()) && "COD".equals(stored.getPaymentMethod()), "Order persistence failed.");
        for (OrderItem item : stored.getItems()) {
            require(item.getId() > 0 && item.getOrderId() == stored.getId() && item.getCreatedAt() != null,
                    "Order item generated metadata was not mapped.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT amount, method, status, paid_at FROM app.payments WHERE order_id=?")) {
            statement.setInt(1, stored.getId());
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && total.compareTo(result.getBigDecimal("amount")) == 0
                        && "COD".equals(result.getString("method")) && "UNPAID".equals(result.getString("status"))
                        && result.getObject("paid_at") == null && !result.next(), "Initial COD payment state is incorrect.");
            }
        }
    }

    private static long countOrders(Connection connection, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM app.orders WHERE user_id=?")) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private static int chooseCategory(Connection connection, String marker) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM app.categories ORDER BY id LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                return result.getInt(1);
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO app.categories(name) VALUES (?) RETURNING id")) {
            statement.setString(1, marker);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "Temporary category could not be created.");
                return result.getInt(1);
            }
        }
    }

    private static Order buy(CheckoutService checkout, int userId) throws SQLException {
        return checkout.checkout(userId, "M09 Recipient", "0901234567", "123 Example Street");
    }

    private static void expect(String label, Class<? extends Exception> type, SqlAction action) throws SQLException {
        try {
            action.run();
        } catch (SQLException | RuntimeException exception) {
            if (!type.isInstance(exception)) {
                throw exception;
            }
            System.out.println("Expected rejection [" + label + "]: " + exception.getClass().getSimpleName());
            return;
        }
        throw new IllegalStateException("Expected rejection did not occur: " + label);
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
