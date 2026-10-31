package com.mycompany.watch_store.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.PaymentDAO;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.Payment;

/** Trusted internal management API, not a public payment endpoint. */
public final class CODPaymentService implements PaymentService {
    private final Connection transactionConnection;

    public CODPaymentService() {
        transactionConnection = null;
    }

    public CODPaymentService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    @Override
    public void confirmPayment(int orderId) throws SQLException {
        if (orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be greater than 0.");
        }
        JdbcTransactions.execute(transactionConnection, connection -> {
            Order order = OrderService.lockOrder(connection, orderId);
            if (!"DELIVERED".equals(order.getStatus())) {
                throw new IllegalStateException("COD can only be confirmed after delivery.");
            }
            PaymentDAO payments = new PaymentDAO(connection);
            Payment payment = payments.findByOrderIdForUpdate(orderId);
            if (payment == null) {
                throw new NoSuchElementException("Payment does not exist.");
            }
            if (!"COD".equals(order.getPaymentMethod()) || !"COD".equals(payment.getMethod())) {
                throw new IllegalStateException("Only COD payment confirmation is supported.");
            }
            if (!"UNPAID".equals(payment.getStatus())) {
                throw new IllegalStateException("Payment has already been confirmed.");
            }
            if (payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
                throw new IllegalStateException("Payment amount does not match the order total.");
            }
            payment.markAsPaid();
            payments.update(payment);
            return null;
        });
    }
}
