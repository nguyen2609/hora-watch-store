package com.mycompany.watch_store.demo;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.OrderDAO;
import com.mycompany.watch_store.dao.PaymentDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.Payment;
import com.mycompany.watch_store.service.CartService;
import com.mycompany.watch_store.service.CheckoutService;
import com.mycompany.watch_store.service.CODPaymentService;
import com.mycompany.watch_store.service.OrderService;
import com.mycompany.watch_store.service.PaymentService;

import static com.mycompany.watch_store.demo.OrderManagementDemo.require;

/** Real independent connections use committed UUID fixtures, then deterministic cleanup. */
public final class OrderManagementConcurrencyDemo {
    private OrderManagementConcurrencyDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== ORDER MANAGEMENT CONCURRENCY DEMO ===");
        try {
            demonstrateConcurrency();
        } catch (Exception exception) {
            System.err.println("Order management concurrency demonstration failed.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void demonstrateConcurrency() throws Exception {
        String marker = "m10-race-" + UUID.randomUUID();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        OrderManagementDemo.Fixture fixture = null;
        try {
            Setup setup = JdbcTransactions.execute(null, connection -> {
                OrderManagementDemo.Fixture data = OrderManagementDemo.createFixture(connection, marker, 3);
                Order order = OrderManagementDemo.placeOrder(connection, data, data.ownerId(), 2, 1);
                CartService carts = new CartService(connection);
                carts.addToCart(data.otherId(), data.smartId(), 1);
                carts.addToCart(data.otherId(), data.automaticId(), 1);
                return new Setup(data, order.getId());
            });
            fixture = setup.fixture();
            final OrderManagementDemo.Fixture data = fixture;

            int cancellations = race(workers,
                    () -> new OrderService().updateOrderStatus(setup.orderId(), "CANCELLED"),
                    () -> new OrderService().updateOrderStatus(setup.orderId(), "CANCELLED"));
            require(cancellations == 1
                    && "CANCELLED".equals(new OrderDAO().findById(setup.orderId()).getStatus())
                    && stock(data.automaticId()) == 3 && stock(data.smartId()) == 3,
                    "Concurrent cancellation restored stock twice or failed.");
            require(new CartService().getCart(data.ownerId()).getItems().isEmpty(), "Cancellation repopulated the cart.");
            System.out.println("Concurrent cancellation: one accepted, one rejected; stock restored once to 3/3.");

            Order nextCancellation = JdbcTransactions.execute(null,
                    connection -> OrderManagementDemo.placeOrder(connection, data, data.ownerId(), 2, 1));
            AtomicReference<Order> purchase = new AtomicReference<>();
            int completed = race(workers,
                    () -> new OrderService().updateOrderStatus(nextCancellation.getId(), "CANCELLED"),
                    () -> purchase.set(new CheckoutService().checkout(data.otherId(), "M10 Recipient", "0901234567",
                            "123 Example Street")));
            require(completed == 2 && purchase.get() != null
                    && stock(data.automaticId()) == 2 && stock(data.smartId()) == 2
                    && "CANCELLED".equals(new OrderDAO().findById(nextCancellation.getId()).getStatus()),
                    "Cancellation/checkout race lost a stock update or deadlocked.");
            require(new OrderService().getOrderHistory(data.ownerId()).size() == 2
                    && new OrderService().getOrderHistory(data.otherId()).size() == 1,
                    "Committed order history is incorrect.");
            System.out.println("Cancellation versus checkout: both completed; deterministic locks, final stock=2/2.");

            int deliveredId = purchase.get().getId();
            OrderService orders = new OrderService();
            for (String status : List.of("CONFIRMED", "SHIPPING", "DELIVERED")) {
                orders.updateOrderStatus(deliveredId, status);
            }
            require(!new PaymentDAO().findByOrderId(deliveredId).isPaid(), "Delivery automatically paid COD.");
            PaymentService firstPayment = new CODPaymentService();
            PaymentService secondPayment = new CODPaymentService();
            int confirmations = race(workers,
                    () -> firstPayment.confirmPayment(deliveredId),
                    () -> secondPayment.confirmPayment(deliveredId));
            Payment paid = new PaymentDAO().findByOrderId(deliveredId);
            require(confirmations == 1 && paid.isPaid() && paid.getPaidAt() != null
                    && "DELIVERED".equals(new OrderDAO().findById(deliveredId).getStatus())
                    && stock(data.automaticId()) == 2 && stock(data.smartId()) == 2,
                    "Concurrent COD confirmation was applied twice or changed other state.");
            require(!new PaymentDAO().findByOrderId(setup.orderId()).isPaid()
                    && !new PaymentDAO().findByOrderId(nextCancellation.getId()).isPaid(),
                    "A cancelled order's payment was marked paid.");
            System.out.println("Concurrent COD confirmation: one accepted, one rejected; PAID/paid_at persisted once.");
            System.out.println("DELIVERED remains unchanged; cancelled orders remain UNPAID.");
        } finally {
            workers.shutdownNow();
            try {
                require(workers.awaitTermination(20, TimeUnit.SECONDS), "Concurrency workers did not finish.");
            } finally {
                OrderManagementDemo.cleanupCommittedFixture(marker);
                OrderManagementDemo.verifyFixtureGone(fixture, marker);
            }
        }
        System.out.println("Committed fixtures cleaned up; identity sequence gaps are expected.");
        System.out.println("Order management concurrency demo finished normally.");
    }

    private static int race(ExecutorService workers, SqlAction first, SqlAction second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Boolean> firstResult = workers.submit(attempt(ready, start, first));
        Future<Boolean> secondResult = workers.submit(attempt(ready, start, second));
        require(ready.await(5, TimeUnit.SECONDS), "Concurrency workers did not start.");
        start.countDown();
        return (firstResult.get(15, TimeUnit.SECONDS) ? 1 : 0)
                + (secondResult.get(15, TimeUnit.SECONDS) ? 1 : 0);
    }

    private static Callable<Boolean> attempt(CountDownLatch ready, CountDownLatch start, SqlAction action) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                action.run();
                return true;
            } catch (IllegalStateException rejected) {
                return false;
            }
        };
    }

    private static int stock(int watchId) throws SQLException {
        return new WatchDAO().findById(watchId).getStockQuantity();
    }

    private record Setup(OrderManagementDemo.Fixture fixture, int orderId) {
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
