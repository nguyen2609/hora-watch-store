package com.mycompany.watch_store.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.entity.Payment;

public final class PaymentDAO {
    private final Connection transactionConnection;

    public PaymentDAO() {
        transactionConnection = null;
    }

    /** Caller owns transaction completion and close; autoCommit must be false. */
    public PaymentDAO(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        }
        transactionConnection = connection;
    }

    /** Returns null for a missing payment. This snapshot does not retain a payment lock. */
    public Payment findByOrderId(int orderId) throws SQLException {
        positiveId(orderId);
        return JdbcTransactions.execute(transactionConnection, connection -> read(connection, orderId, false));
    }

    /** Call only after locking the owner and order in the same borrowed transaction. */
    public Payment findByOrderIdForUpdate(int orderId) throws SQLException {
        positiveId(orderId);
        if (transactionConnection == null || transactionConnection.getAutoCommit()) {
            throw new SQLException("Payment locking requires a borrowed connection with autoCommit=false.");
        }
        return read(transactionConnection, orderId, true);
    }

    /** Only persists a previously UNPAID COD payment after the entity was marked as paid. */
    public void update(Payment payment) throws SQLException {
        if (payment == null || payment.getId() <= 0 || payment.getOrderId() <= 0
                || !payment.isPaid() || payment.getPaidAt() == null || !"COD".equals(payment.getMethod())) {
            throw new IllegalArgumentException("A persisted COD payment marked PAID is required.");
        }
        JdbcTransactions.execute(transactionConnection, connection -> {
            String sql = """
                    UPDATE app.payments SET status = ?, paid_at = ?
                    WHERE id = ? AND order_id = ? AND status = 'UNPAID' AND paid_at IS NULL
                      AND method = 'COD' AND amount = ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, payment.getStatus());
                statement.setObject(2, payment.getPaidAt());
                statement.setInt(3, payment.getId());
                statement.setInt(4, payment.getOrderId());
                statement.setBigDecimal(5, payment.getAmount());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Payment is missing or changed unexpectedly.", "02000");
                }
            }
            return null;
        });
    }

    private static Payment read(Connection connection, int orderId, boolean exclusive) throws SQLException {
        String sql = "SELECT id, order_id, amount, method, status, paid_at, created_at FROM app.payments WHERE order_id = ?"
                + (exclusive ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Payment(result.getInt("id"), result.getInt("order_id"), result.getBigDecimal("amount"),
                        result.getString("method"), result.getString("status"),
                        result.getObject("paid_at", LocalDateTime.class), result.getObject("created_at", LocalDateTime.class));
            }
        }
    }

    private static void positiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Order ID must be greater than 0.");
        }
    }
}
