package com.mycompany.watch_store.config;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;

/** Shared transaction ownership rules for multi-statement cart operations. */
public final class JdbcTransactions {
    private JdbcTransactions() {
    }

    public static <T> T execute(Connection borrowed, Operation<T> operation) throws SQLException {
        if (borrowed == null) {
            try (Connection owned = DatabaseConnection.getConnection()) {
                owned.setAutoCommit(false);
                try {
                    T result = operation.run(owned);
                    owned.commit();
                    return result;
                } catch (SQLException | RuntimeException | Error exception) {
                    try {
                        owned.rollback();
                    } catch (SQLException rollbackFailure) {
                        exception.addSuppressed(rollbackFailure);
                    }
                    throw exception;
                }
            }
        }
        if (borrowed.getAutoCommit()) {
            throw new SQLException("A borrowed connection must have autoCommit=false for cart operations.");
        }
        Savepoint savepoint = borrowed.setSavepoint();
        try {
            T result = operation.run(borrowed);
            borrowed.releaseSavepoint(savepoint);
            return result;
        } catch (SQLException | RuntimeException | Error exception) {
            try {
                borrowed.rollback(savepoint);
                borrowed.releaseSavepoint(savepoint);
            } catch (SQLException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
    }

    @FunctionalInterface
    public interface Operation<T> {
        T run(Connection connection) throws SQLException;
    }
}
