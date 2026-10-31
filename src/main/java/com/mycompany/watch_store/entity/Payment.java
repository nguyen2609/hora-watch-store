package com.mycompany.watch_store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class Payment extends BaseEntity {
    private final int orderId;
    private final BigDecimal amount;
    private final String method = "COD";
    private String status = "UNPAID";
    private LocalDateTime paidAt;

    public Payment(int orderId, BigDecimal amount) {
        super();
        this.orderId = EntityValidation.parentId(orderId, "Order ID");
        this.amount = EntityValidation.money(amount, "Payment amount");
    }

    /** Restores stored payment state without generating a new confirmation timestamp. */
    public Payment(int id, int orderId, BigDecimal amount, String method, String status,
                   LocalDateTime paidAt, LocalDateTime createdAt) {
        this(orderId, amount);
        EntityValidation.positiveId(orderId, "Order ID");
        if (!"COD".equals(method)) {
            throw new IllegalArgumentException("Stored payment method must be COD.");
        }
        if (!("UNPAID".equals(status) && paidAt == null) && !("PAID".equals(status) && paidAt != null)) {
            throw new IllegalArgumentException("Payment status and paidAt are inconsistent.");
        }
        setId(id);
        setCreatedAt(createdAt);
        this.status = status;
        this.paidAt = paidAt;
    }

    public int getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public LocalDateTime getPaidAt() { return paidAt; }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public void markAsPaid() {
        if (isPaid()) {
            throw new IllegalStateException("Payment has already been confirmed.");
        }
        paidAt = LocalDateTime.now();
        status = "PAID";
    }
}
