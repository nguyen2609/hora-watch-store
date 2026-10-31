package com.mycompany.watch_store.demo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.CartDAO;
import com.mycompany.watch_store.dao.OrderDAO;
import com.mycompany.watch_store.dao.PaymentDAO;
import com.mycompany.watch_store.dao.UserDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.Payment;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.service.AuthService;
import com.mycompany.watch_store.service.CartService;
import com.mycompany.watch_store.service.CheckoutService;
import com.mycompany.watch_store.service.CODPaymentService;
import com.mycompany.watch_store.service.OrderService;
import com.mycompany.watch_store.service.PaymentService;

public final class OrderManagementDemo {
    private OrderManagementDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== ORDER MANAGEMENT DEMO ===");
        try (Connection connection = DatabaseConnection.getConnection()) {
            demonstrateLifecycle(connection);
        } catch (SQLException | RuntimeException exception) {
            System.err.println("Order management demonstration failed.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void demonstrateLifecycle(Connection connection) throws SQLException {
        String marker = "m10-" + UUID.randomUUID();
        Fixture fixture;
        connection.setAutoCommit(false);
        try {
            fixture = createFixture(connection, marker, 10);
            OrderService orders = new OrderService(connection);
            PaymentService payments = new CODPaymentService(connection);
            WatchDAO watches = new WatchDAO(connection);
            Order first = placeOrder(connection, fixture, fixture.ownerId(), 2, 1);
            Order second = placeOrder(connection, fixture, fixture.ownerId(), 1, 1);
            require(orders.getOrderHistory(fixture.ownerId()).size() == 2
                    && orders.getOrderHistory(fixture.otherId()).isEmpty(), "User-scoped history failed.");
            Order details = orders.getOrderDetails(fixture.ownerId(), first.getId());
            require(details.getItems().size() == 2 && details.getTotalAmount().compareTo(new BigDecimal("17500000")) == 0
                    && new OrderDAO(connection).findItemsByOrderId(first.getId()).size() == 2, "Order detail mapping failed.");
            System.out.println("Persisted history: owner=2 orders, other user=0; details contain 2 actual items.");
            List<String> before = snapshot(connection, fixture);
            expect("Another user's order", NoSuchElementException.class,
                    () -> orders.getOrderDetails(fixture.otherId(), first.getId()));
            expect("Skip PENDING to SHIPPING", IllegalStateException.class,
                    () -> orders.updateOrderStatus(first.getId(), "SHIPPING"));
            expect("Repeated PENDING", IllegalStateException.class,
                    () -> orders.updateOrderStatus(first.getId(), "PENDING"));
            expect("Unknown status", IllegalArgumentException.class,
                    () -> orders.updateOrderStatus(first.getId(), "UNKNOWN"));
            require(before.equals(snapshot(connection, fixture)), "Invalid reads/transitions changed persisted state.");

            Watch automatic = watches.findById(fixture.automaticId());
            automatic.setPrice(new BigDecimal("7000000"));
            automatic.setAvailable(false);
            watches.update(automatic);
            require(orders.getOrderDetails(fixture.ownerId(), first.getId()).getTotalAmount()
                    .compareTo(new BigDecimal("17500000")) == 0, "History changed after product repricing.");
            orders.updateOrderStatus(first.getId(), "CANCELLED");
            require(stock(connection, fixture.automaticId()) == 9 && stock(connection, fixture.smartId()) == 9
                    && !watches.findById(fixture.automaticId()).isAvailable()
                    && new CartDAO(connection).findByUserId(fixture.ownerId()).getItems().isEmpty(),
                    "Pending cancellation did not restore stock correctly.");
            System.out.println("PENDING cancellation restored stock to 9/9, including the discontinued watch; cart stays empty.");
            before = snapshot(connection, fixture);
            expect("Repeated cancellation", IllegalStateException.class,
                    () -> orders.updateOrderStatus(first.getId(), "CANCELLED"));
            expect("Payment for cancelled order", IllegalStateException.class, () -> payments.confirmPayment(first.getId()));
            require(before.equals(snapshot(connection, fixture)), "Repeated cancellation restored stock again.");

            automatic = watches.findById(fixture.automaticId());
            automatic.setAvailable(true);
            automatic.setPrice(new BigDecimal("6500000"));
            watches.update(automatic);
            orders.updateOrderStatus(second.getId(), "CONFIRMED");
            orders.updateOrderStatus(second.getId(), "CANCELLED");
            require(stock(connection, fixture.automaticId()) == 10 && stock(connection, fixture.smartId()) == 10,
                    "Confirmed cancellation failed.");
            System.out.println("CONFIRMED cancellation restored stock to 10/10; no double restoration.");

            Order delivery = placeOrder(connection, fixture, fixture.ownerId(), 1, 1);
            before = snapshot(connection, fixture);
            expect("COD before delivery", IllegalStateException.class, () -> payments.confirmPayment(delivery.getId()));
            require(before.equals(snapshot(connection, fixture)), "Early payment confirmation changed state.");
            orders.updateOrderStatus(delivery.getId(), "CONFIRMED");
            before = snapshot(connection, fixture);
            expect("Repeated CONFIRMED", IllegalStateException.class,
                    () -> orders.updateOrderStatus(delivery.getId(), "CONFIRMED"));
            expect("Backward transition", IllegalStateException.class,
                    () -> orders.updateOrderStatus(delivery.getId(), "PENDING"));
            require(before.equals(snapshot(connection, fixture)), "Invalid confirmed transition changed state.");
            orders.updateOrderStatus(delivery.getId(), "SHIPPING");
            before = snapshot(connection, fixture);
            expect("Cancel SHIPPING order", IllegalStateException.class,
                    () -> orders.updateOrderStatus(delivery.getId(), "CANCELLED"));
            require(before.equals(snapshot(connection, fixture)), "Shipping cancellation changed state.");
            orders.updateOrderStatus(delivery.getId(), "DELIVERED");
            Payment unpaid = new PaymentDAO(connection).findByOrderId(delivery.getId());
            require(!unpaid.isPaid() && unpaid.getPaidAt() == null, "Delivery automatically confirmed payment.");
            System.out.println("Lifecycle: PENDING -> CONFIRMED -> SHIPPING -> DELIVERED; payment still UNPAID.");
            before = snapshot(connection, fixture);
            expect("Leave DELIVERED", IllegalStateException.class,
                    () -> orders.updateOrderStatus(delivery.getId(), "CONFIRMED"));
            expect("Repeated DELIVERED", IllegalStateException.class,
                    () -> orders.updateOrderStatus(delivery.getId(), "DELIVERED"));
            require(before.equals(snapshot(connection, fixture)), "A final order was changed.");

            setPaymentAmount(connection, delivery.getId(), delivery.getTotalAmount().add(BigDecimal.ONE));
            before = snapshot(connection, fixture);
            expect("Payment amount mismatch", IllegalStateException.class, () -> payments.confirmPayment(delivery.getId()));
            require(before.equals(snapshot(connection, fixture)), "Amount mismatch changed payment state.");
            setPaymentAmount(connection, delivery.getId(), delivery.getTotalAmount());
            Savepoint missingPayment = connection.setSavepoint();
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app.payments WHERE order_id=?")) {
                statement.setInt(1, delivery.getId());
                require(statement.executeUpdate() == 1, "Temporary payment was not removed for the missing-row demo.");
            }
            expect("Missing payment", NoSuchElementException.class, () -> payments.confirmPayment(delivery.getId()));
            connection.rollback(missingPayment);
            connection.releaseSavepoint(missingPayment);

            before = snapshot(connection, fixture);
            boolean[] paymentWriteObserved = {false};
            Connection failingPayment = failAfterPaymentWrite(connection, delivery.getId(), paymentWriteObserved);
            expect("SQL failure after PAID update", SQLException.class,
                    () -> new CODPaymentService(failingPayment).confirmPayment(delivery.getId()));
            require(paymentWriteObserved[0] && before.equals(snapshot(connection, fixture)),
                    "Payment confirmation failure did not roll back.");
            System.out.println("Injected payment SQL failure rolled PAID/paid_at back to UNPAID/NULL.");
            payments.confirmPayment(delivery.getId());
            Payment paid = new PaymentDAO(connection).findByOrderId(delivery.getId());
            require(paid.isPaid() && paid.getPaidAt() != null && paid.getId() == unpaid.getId()
                    && "DELIVERED".equals(orders.getOrderDetails(fixture.ownerId(), delivery.getId()).getStatus()),
                    "COD confirmation failed or changed the order.");
            before = snapshot(connection, fixture);
            expect("Duplicate COD confirmation", IllegalStateException.class, () -> payments.confirmPayment(delivery.getId()));
            require(before.equals(snapshot(connection, fixture)), "Duplicate confirmation changed paid_at.");
            System.out.println("COD confirmed: PAID with paid_at; order remains DELIVERED; duplicate rejected.");

            Order cancellation = placeOrder(connection, fixture, fixture.ownerId(), 2, 1);
            before = snapshot(connection, fixture);
            boolean[] stockWriteObserved = {false};
            Connection failingStock = failSecondRestoration(connection, fixture, cancellation.getId(), stockWriteObserved);
            expect("SQL failure during cancellation", SQLException.class,
                    () -> new OrderService(failingStock).updateOrderStatus(cancellation.getId(), "CANCELLED"));
            require(stockWriteObserved[0] && before.equals(snapshot(connection, fixture)),
                    "Failed cancellation left partial restoration or a changed status.");
            System.out.println("Injected cancellation failure restored original status and both stock quantities.");
            orders.updateOrderStatus(cancellation.getId(), "CANCELLED");
            require(stock(connection, fixture.automaticId()) == 9 && stock(connection, fixture.smartId()) == 9,
                    "Cancellation retry restored stock incorrectly.");
            require(new OrderDAO().findById(delivery.getId()) == null, "A borrowed transaction was committed unexpectedly.");
            System.out.println("Cancellation retry restored stock once; borrowed transaction still uncommitted.");
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
            System.out.println("Order management outer transaction rolled back.");
        }
        verifyFixtureGone(fixture, marker);
        System.out.println("Order management demo finished normally.");
    }

    private static Connection failSecondRestoration(Connection real, Fixture fixture, int orderId, boolean[] observed) {
        int[] attempts = {0};
        return (Connection) Proxy.newProxyInstance(OrderManagementDemo.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement") && args != null && args[0] instanceof String sql
                            && sql.startsWith("UPDATE app.watches SET stock_quantity = stock_quantity +") && ++attempts[0] == 2) {
                        observed[0] = "CANCELLED".equals(new OrderDAO(real).findById(orderId).getStatus())
                                && stock(real, fixture.automaticId()) == 9 && stock(real, fixture.smartId()) == 8;
                        throw new SQLException("Simulated cancellation failure.", "P0001");
                    }
                    return invoke(real, method, args);
                });
    }

    private static Connection failAfterPaymentWrite(Connection real, int orderId, boolean[] observed) {
        return (Connection) Proxy.newProxyInstance(OrderManagementDemo.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    Object result = invoke(real, method, args);
                    if (method.getName().equals("prepareStatement") && args != null && args[0] instanceof String sql
                            && sql.startsWith("UPDATE app.payments SET status")) {
                        PreparedStatement actual = (PreparedStatement) result;
                        return Proxy.newProxyInstance(OrderManagementDemo.class.getClassLoader(),
                                new Class<?>[]{PreparedStatement.class}, (statementProxy, statementMethod, statementArgs) -> {
                                    Object updated = invoke(actual, statementMethod, statementArgs);
                                    if (statementMethod.getName().equals("executeUpdate")) {
                                        Payment payment = new PaymentDAO(real).findByOrderId(orderId);
                                        observed[0] = Integer.valueOf(1).equals(updated) && payment.isPaid() && payment.getPaidAt() != null;
                                        throw new SQLException("Simulated payment confirmation failure.", "P0001");
                                    }
                                    return updated;
                                });
                    }
                    return result;
                });
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static List<String> snapshot(Connection connection, Fixture fixture) throws SQLException {
        List<String> state = new ArrayList<>();
        for (Order order : new OrderService(connection).getOrderHistory(fixture.ownerId())) {
            state.add(order.getId() + ":" + order.getStatus() + ":" + order.getTotalAmount() + ":" + order.getCreatedAt());
            order.getItems().forEach(item -> state.add(item.getId() + ":" + item.getWatchId() + ":"
                    + item.getQuantity() + ":" + item.getUnitPrice() + ":" + item.getCreatedAt()));
            Payment payment = new PaymentDAO(connection).findByOrderId(order.getId());
            state.add(payment == null ? "missing-payment" : payment.getId() + ":" + payment.getMethod() + ":"
                    + payment.getAmount() + ":" + payment.getStatus() + ":" + payment.getPaidAt() + ":" + payment.getCreatedAt());
        }
        for (int id : new int[]{fixture.automaticId(), fixture.smartId()}) {
            Watch watch = new WatchDAO(connection).findById(id);
            state.add(id + ":" + watch.getStockQuantity() + ":" + watch.getPrice() + ":" + watch.isAvailable());
        }
        Cart cart = new CartDAO(connection).findByUserId(fixture.ownerId());
        state.add(cart == null ? "missing-cart" : cart.getId() + ":" + cart.getUpdatedAt());
        if (cart != null) {
            cart.getItems().forEach(item -> state.add(item.getWatchId() + ":" + item.getQuantity()));
        }
        return state;
    }

    private static void setPaymentAmount(Connection connection, int orderId, BigDecimal amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE app.payments SET amount=? WHERE order_id=?")) {
            statement.setBigDecimal(1, amount);
            statement.setInt(2, orderId);
            require(statement.executeUpdate() == 1, "Temporary payment amount was not changed.");
        }
    }

    // Package-private fixture helpers are shared only by the two M10 demos.
    static Fixture createFixture(Connection connection, String marker, int initialStock) throws SQLException {
        AuthService auth = new AuthService(new UserDAO(connection));
        int owner = auth.register("M10 Customer A", marker + "-a@example.invalid", UUID.randomUUID().toString(), null).getId();
        int other = auth.register("M10 Customer B", marker + "-b@example.invalid", UUID.randomUUID().toString(), null).getId();
        int categoryId = 0;
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM app.categories ORDER BY id LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                categoryId = result.getInt(1);
            }
        }
        if (categoryId == 0) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO app.categories(name) VALUES (?) RETURNING id")) {
                statement.setString(1, marker);
                try (ResultSet result = statement.executeQuery()) {
                    require(result.next(), "Temporary category was not created.");
                    categoryId = result.getInt(1);
                }
            }
        }
        WatchDAO watches = new WatchDAO(connection);
        int automatic = watches.insert(new AutomaticWatch(categoryId, "M10 automatic", "Demo", marker + "-auto",
                new BigDecimal("6500000"), initialStock, null, null, true, "Self-winding"));
        int smart = watches.insert(new SmartWatch(categoryId, "M10 smart", "Demo", marker + "-smart",
                new BigDecimal("4500000"), initialStock, null, null, true, 48));
        return new Fixture(owner, other, automatic, smart);
    }

    static Order placeOrder(Connection connection, Fixture fixture, int userId, int automaticQuantity, int smartQuantity)
            throws SQLException {
        require(userId == fixture.ownerId() || userId == fixture.otherId(), "Demo must only use fixture customers.");
        CartService carts = new CartService(connection);
        carts.clearCart(userId);
        if (automaticQuantity > 0) {
            carts.addToCart(userId, fixture.automaticId(), automaticQuantity);
        }
        if (smartQuantity > 0) {
            carts.addToCart(userId, fixture.smartId(), smartQuantity);
        }
        return new CheckoutService(connection).checkout(userId, "M10 Recipient", "0901234567", "123 Example Street");
    }

    static void cleanupCommittedFixture(String marker) throws SQLException {
        JdbcTransactions.execute(null, connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SET LOCAL statement_timeout = '10s'")) {
                statement.execute();
            }
            for (String sql : List.of(
                    "DELETE FROM app.order_items WHERE order_id IN (SELECT o.id FROM app.orders o JOIN app.users u ON u.id=o.user_id WHERE u.email IN (?, ?))",
                    "DELETE FROM app.payments WHERE order_id IN (SELECT o.id FROM app.orders o JOIN app.users u ON u.id=o.user_id WHERE u.email IN (?, ?))",
                    "DELETE FROM app.orders WHERE user_id IN (SELECT id FROM app.users WHERE email IN (?, ?))",
                    "DELETE FROM app.cart_items WHERE cart_id IN (SELECT c.id FROM app.carts c JOIN app.users u ON u.id=c.user_id WHERE u.email IN (?, ?))",
                    "DELETE FROM app.carts WHERE user_id IN (SELECT id FROM app.users WHERE email IN (?, ?))",
                    "DELETE FROM app.users WHERE email IN (?, ?)")) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, marker + "-a@example.invalid");
                    statement.setString(2, marker + "-b@example.invalid");
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app.watches WHERE model_number IN (?, ?)")) {
                statement.setString(1, marker + "-auto");
                statement.setString(2, marker + "-smart");
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app.categories WHERE name=?")) {
                statement.setString(1, marker);
                statement.executeUpdate();
            }
            return null;
        });
    }

    static void verifyFixtureGone(Fixture fixture, String marker) throws SQLException {
        require(new UserDAO().findByEmail(marker + "-a@example.invalid") == null
                && new UserDAO().findByEmail(marker + "-b@example.invalid") == null, "Temporary customers remain.");
        if (fixture != null) {
            require(new OrderDAO().findByUserId(fixture.ownerId()).isEmpty()
                    && new OrderDAO().findByUserId(fixture.otherId()).isEmpty()
                    && new CartDAO().findByUserId(fixture.ownerId()) == null
                    && new CartDAO().findByUserId(fixture.otherId()) == null
                    && new WatchDAO().findById(fixture.automaticId()) == null
                    && new WatchDAO().findById(fixture.smartId()) == null, "Temporary order/cart/watch data remains.");
        }
        JdbcTransactions.execute(null, connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM app.categories WHERE name=?")) {
                statement.setString(1, marker);
                try (ResultSet result = statement.executeQuery()) {
                    require(result.next() && result.getLong(1) == 0, "Temporary category remains.");
                }
            }
            return null;
        });
        System.out.println("Cleanup verified: no M10 fixture user/order/payment/cart/watch/category remains.");
    }

    static int stock(Connection connection, int watchId) throws SQLException {
        return new WatchDAO(connection).findById(watchId).getStockQuantity();
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
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

    record Fixture(int ownerId, int otherId, int automaticId, int smartId) {
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
