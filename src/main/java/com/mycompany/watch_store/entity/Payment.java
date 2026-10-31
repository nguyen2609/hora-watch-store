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
