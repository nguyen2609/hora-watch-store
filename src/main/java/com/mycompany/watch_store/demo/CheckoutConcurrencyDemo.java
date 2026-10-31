package com.mycompany.watch_store.demo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.CartDAO;
import com.mycompany.watch_store.dao.UserDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.service.AuthService;
import com.mycompany.watch_store.service.CartService;
import com.mycompany.watch_store.service.CheckoutService;

/** Separate connections need committed, uniquely named fixtures; all are explicitly cleaned up. */
public final class CheckoutConcurrencyDemo {
    private CheckoutConcurrencyDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== CHECKOUT CONCURRENCY DEMO ===");
        try {
            demonstrateConcurrency();
        } catch (Exception exception) {
            System.err.println("Checkout concurrency demonstration failed.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void demonstrateConcurrency() throws Exception {
        String marker = "m09-race-" + UUID.randomUUID();
        String firstEmail = marker + "-a@example.invalid";
        String secondEmail = marker + "-b@example.invalid";
        String autoModel = marker + "-auto";
        String smartModel = marker + "-smart";
        ExecutorService workers = Executors.newFixedThreadPool(2);
        Fixture fixture = null;
        try {
            fixture = createFixture(marker, firstEmail, secondEmail, autoModel, smartModel);
            final Fixture data = fixture;
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<Order> first = workers.submit(competingCheckout(data.firstUserId(), ready, start));
            Future<Order> second = workers.submit(competingCheckout(data.secondUserId(), ready, start));
            require(ready.await(5, TimeUnit.SECONDS), "Checkout workers did not start.");
            start.countDown();
            Order firstResult = first.get(15, TimeUnit.SECONDS);
            Order secondResult = second.get(15, TimeUnit.SECONDS);
            require((firstResult == null) != (secondResult == null), "Exactly one limited-stock checkout must succeed.");
            Order winner = firstResult != null ? firstResult : secondResult;
            int losingUserId = winner.getUserId() == data.firstUserId() ? data.secondUserId() : data.firstUserId();
            require(winner.getItems().size() == 2 && winner.getTotalAmount().compareTo(new BigDecimal("11000000")) == 0,
                    "Winning order is incomplete.");
            require(new WatchDAO().findById(data.automaticId()).getStockQuantity() == 0
                    && new WatchDAO().findById(data.smartId()).getStockQuantity() == 0,
                    "Limited stock was oversold or incorrectly deducted.");
            require(new CartService().getCart(winner.getUserId()).getItems().isEmpty()
                    && new CartService().getCart(losingUserId).getItems().size() == 2,
                    "Losing cart was not preserved.");
            verifyCounts(data, 1, 2);
            System.out.println("Two users, reversed cart insertion order: one checkout succeeded, one rejected.");
            System.out.println("Limited stock: 0/0; one complete order, two items, one UNPAID payment; losing cart preserved.");

            // Restock only the temporary watches for a same-user cart/checkout race.
            JdbcTransactions.execute(null, connection -> {
                WatchDAO watches = new WatchDAO(connection);
                for (int id : new int[]{data.automaticId(), data.smartId()}) {
                    Watch watch = watches.findByIdForUpdate(id);
                    watch.setStockQuantity(3);
                    watches.update(watch);
                }
                return null;
            });
            CountDownLatch ownerReady = new CountDownLatch(2);
            CountDownLatch ownerStart = new CountDownLatch(1);
            Future<Order> purchase = workers.submit(() -> {
                ownerReady.countDown();
                ownerStart.await();
                return buy(losingUserId);
            });
            Future<?> addition = workers.submit(() -> {
                ownerReady.countDown();
                ownerStart.await();
                new CartService().addToCart(losingUserId, data.automaticId(), 1);
                return null;
            });
            require(ownerReady.await(5, TimeUnit.SECONDS), "Same-user workers did not start.");
            ownerStart.countDown();
            Order purchased = purchase.get(15, TimeUnit.SECONDS);
            addition.get(15, TimeUnit.SECONDS);
            Cart remaining = new CartService().getCart(losingUserId);
            int boughtAutomatic = purchased.getItems().stream().filter(item -> item.getWatchId() == data.automaticId())
                    .mapToInt(OrderItem::getQuantity).sum();
            int remainingAutomatic = remaining.getItems().stream().filter(item -> item.getWatchId() == data.automaticId())
                    .mapToInt(CartItem::getQuantity).sum();
            require(boughtAutomatic + remainingAutomatic == 2
                    && remaining.getItems().stream().allMatch(item -> item.getWatchId() == data.automaticId())
                    && purchased.getItems().stream().filter(item -> item.getWatchId() == data.smartId())
                            .mapToInt(OrderItem::getQuantity).sum() == 1
                    && new WatchDAO().findById(data.automaticId()).getStockQuantity() == 3 - boughtAutomatic
                    && new WatchDAO().findById(data.smartId()).getStockQuantity() == 2,
                    "A same-user cart operation was lost or bought twice.");
            verifyCounts(data, 2, 4);
            System.out.println("Same-user checkout/add serialized correctly: purchased + remaining automatic quantity = 2.");
            System.out.println("Owned transactions committed completely; no partial orders or paid-success state.");
        } finally {
            workers.shutdownNow();
            try {
                require(workers.awaitTermination(20, TimeUnit.SECONDS), "Concurrency workers did not finish.");
            } finally {
                cleanup(marker, firstEmail, secondEmail, autoModel, smartModel);
                verifyCleanup(fixture, marker, firstEmail, secondEmail);
            }
        }
        System.out.println("Committed concurrency fixtures cleaned up and verified; identity gaps are expected.");
        System.out.println("Checkout concurrency demo finished normally.");
    }

    private static Callable<Order> competingCheckout(int userId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                return buy(userId);
            } catch (IllegalArgumentException insufficientStock) {
                return null;
            }
        };
    }

    private static Fixture createFixture(String marker, String firstEmail, String secondEmail,
                                          String autoModel, String smartModel) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            AuthService auth = new AuthService(new UserDAO(connection));
            int firstUser = auth.register("M09 Race A", firstEmail, UUID.randomUUID().toString(), null).getId();
            int secondUser = auth.register("M09 Race B", secondEmail, UUID.randomUUID().toString(), null).getId();
            int categoryId = chooseCategory(connection, marker);
            WatchDAO watches = new WatchDAO(connection);
            int automaticId = watches.insert(new AutomaticWatch(categoryId, "M09 limited automatic", "Demo", autoModel,
                    new BigDecimal("6500000"), 1, null, null, true, "Self-winding"));
            int smartId = watches.insert(new SmartWatch(categoryId, "M09 limited smart", "Demo", smartModel,
                    new BigDecimal("4500000"), 1, null, null, true, 48));
            CartService carts = new CartService(connection);
            carts.addToCart(firstUser, automaticId, 1);
            carts.addToCart(firstUser, smartId, 1);
            carts.addToCart(secondUser, smartId, 1);
            carts.addToCart(secondUser, automaticId, 1);
            return new Fixture(firstUser, secondUser, automaticId, smartId);
        });
    }

    private static void verifyCounts(Fixture fixture, int expectedOrders, int expectedItems) throws SQLException {
        JdbcTransactions.execute(null, connection -> {
            String sql = """
                    SELECT (SELECT COUNT(*) FROM app.orders WHERE user_id IN (?, ?)),
                           (SELECT COUNT(*) FROM app.order_items i JOIN app.orders o ON o.id=i.order_id
                            WHERE o.user_id IN (?, ?)),
                           (SELECT COUNT(*) FROM app.payments p JOIN app.orders o ON o.id=p.order_id
                            WHERE o.user_id IN (?, ?) AND p.status='UNPAID' AND p.method='COD' AND p.paid_at IS NULL)
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int position = 1; position <= 6; position += 2) {
                    statement.setInt(position, fixture.firstUserId());
                    statement.setInt(position + 1, fixture.secondUserId());
                }
                try (ResultSet result = statement.executeQuery()) {
                    require(result.next() && result.getLong(1) == expectedOrders && result.getLong(2) == expectedItems
                            && result.getLong(3) == expectedOrders, "Partial order or incorrect COD data remains.");
                }
            }
            return null;
        });
    }

    private static void cleanup(String marker, String firstEmail, String secondEmail, String autoModel, String smartModel)
            throws SQLException {
        JdbcTransactions.execute(null, connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SET LOCAL statement_timeout = '10s'")) {
                statement.execute();
            }
            List<String> deletions = List.of(
                    "DELETE FROM app.order_items WHERE order_id IN (SELECT o.id FROM app.orders o JOIN app.users u ON u.id=o.user_id WHERE u.email IN (?, ?))",
                    "DELETE FROM app.payments WHERE order_id IN (SELECT o.id FROM app.orders o JOIN app.users u ON u.id=o.user_id WHERE u.email IN (?, ?))",
                    "DELETE FROM app.orders WHERE user_id IN (SELECT id FROM app.users WHERE email IN (?, ?))",
                    "DELETE FROM app.cart_items WHERE cart_id IN (SELECT c.id FROM app.carts c JOIN app.users u ON u.id=c.user_id WHERE u.email IN (?, ?))",
                    "DELETE FROM app.carts WHERE user_id IN (SELECT id FROM app.users WHERE email IN (?, ?))",
                    "DELETE FROM app.users WHERE email IN (?, ?)");
            for (String sql : deletions) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, firstEmail);
                    statement.setString(2, secondEmail);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app.watches WHERE model_number IN (?, ?)")) {
                statement.setString(1, autoModel);
                statement.setString(2, smartModel);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM app.categories WHERE name = ?")) {
                statement.setString(1, marker);
                statement.executeUpdate();
            }
            return null;
        });
    }

    private static void verifyCleanup(Fixture fixture, String marker, String firstEmail, String secondEmail) throws SQLException {
        require(new UserDAO().findByEmail(firstEmail) == null && new UserDAO().findByEmail(secondEmail) == null,
                "Temporary concurrency customers remain.");
        if (fixture != null) {
            verifyCounts(fixture, 0, 0);
            require(new CartDAO().findByUserId(fixture.firstUserId()) == null
                    && new CartDAO().findByUserId(fixture.secondUserId()) == null
                    && new WatchDAO().findById(fixture.automaticId()) == null
                    && new WatchDAO().findById(fixture.smartId()) == null, "Temporary concurrency data remains.");
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
                require(result.next(), "Temporary category was not created.");
                return result.getInt(1);
            }
        }
    }

    private static Order buy(int userId) throws SQLException {
        return new CheckoutService().checkout(userId, "M09 Recipient", "0901234567", "123 Example Street");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private record Fixture(int firstUserId, int secondUserId, int automaticId, int smartId) {
    }
}
