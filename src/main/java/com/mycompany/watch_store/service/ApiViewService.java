package com.mycompany.watch_store.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.OrderDAO;
import com.mycompany.watch_store.dao.PaymentDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.dto.CartResponse;
import com.mycompany.watch_store.dto.CheckoutRequest;
import com.mycompany.watch_store.dto.DtoMapper;
import com.mycompany.watch_store.dto.OrderResponse;
import com.mycompany.watch_store.dto.PaymentResponse;
import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;
import com.mycompany.watch_store.entity.Payment;
import com.mycompany.watch_store.entity.Watch;

/** Builds safe API snapshots inside the same transaction as the delegated business operation. */
public final class ApiViewService {
    public CartResponse cart(int userId) throws SQLException {
        return changeCart(userId, carts -> { });
    }

    public CartResponse changeCart(int userId, CartAction action) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            CartService carts = new CartService(connection);
            action.run(carts);
            Cart cart = carts.getCart(userId);
            List<CartResponse.Item> items = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            // One statement supplies a catalog price snapshot. Do not take additional watch locks
            // after CartService may already have locked a product with a higher ID.
            Map<Integer, Watch> watches = new WatchDAO(connection).findAll().stream()
                    .collect(Collectors.toMap(Watch::getId, Function.identity()));
            for (CartItem item : cart.getItems().stream().sorted(Comparator.comparingInt(CartItem::getWatchId)).toList()) {
                Watch watch = watches.get(item.getWatchId());
                if (watch == null) {
                    throw new SQLException("A referenced cart product is missing.");
                }
                BigDecimal subtotal = watch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(subtotal);
                items.add(new CartResponse.Item(watch.getId(), watch.getName(), item.getQuantity(),
                        watch.getPrice(), subtotal, watch.isAvailable()));
            }
            return new CartResponse(cart.getId(), items, total, cart.getUpdatedAt().toString());
        });
    }

    public OrderResponse checkout(int userId, CheckoutRequest request) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            Order order = new CheckoutService(connection).checkout(userId, request.recipientName(),
                    request.recipientPhone(), request.shippingAddress());
            return orderResponse(connection, order);
        });
    }

    public List<OrderResponse> history(int userId) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            List<OrderResponse> result = new ArrayList<>();
            for (Order order : new OrderService(connection).getOrderHistory(userId)) {
                result.add(orderResponse(connection, order));
            }
            return List.copyOf(result);
        });
    }

    public OrderResponse details(int userId, int orderId) throws SQLException {
        return JdbcTransactions.execute(null, connection ->
                orderResponse(connection, new OrderService(connection).getOrderDetails(userId, orderId)));
    }

    /** Handlers authorize ADMIN before invoking these internal management operations. */
    public OrderResponse updateStatus(int orderId, String status) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            new OrderService(connection).updateOrderStatus(orderId, status);
            return orderResponse(connection, new OrderDAO(connection).findById(orderId));
        });
    }

    public PaymentResponse confirmPayment(int orderId) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            PaymentService payments = new CODPaymentService(connection);
            payments.confirmPayment(orderId);
            return DtoMapper.payment(new PaymentDAO(connection).findByOrderId(orderId));
        });
    }

    static OrderResponse orderResponse(Connection connection, Order order) throws SQLException {
        List<OrderResponse.Item> items = new ArrayList<>();
        WatchDAO watches = new WatchDAO(connection);
        for (OrderItem item : order.getItems()) {
            Watch watch = watches.findById(item.getWatchId());
            if (watch == null) {
                throw new SQLException("A referenced order product is missing.");
            }
            items.add(new OrderResponse.Item(item.getWatchId(), watch.getName(), item.getQuantity(),
                    item.getUnitPrice(), item.getSubtotal()));
        }
        Payment payment = new PaymentDAO(connection).findByOrderId(order.getId());
        if (payment == null) {
            throw new SQLException("A referenced order payment is missing.");
        }
        return new OrderResponse(order.getId(), order.getRecipientName(), order.getRecipientPhone(),
                order.getShippingAddress(), order.getTotalAmount(), order.getStatus(), order.getPaymentMethod(),
                items, DtoMapper.payment(payment), order.getCreatedAt().toString());
    }

    @FunctionalInterface
    public interface CartAction {
        void run(CartService carts) throws SQLException;
    }
}
