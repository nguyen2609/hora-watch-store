package com.mycompany.watch_store.entity;

import java.math.BigDecimal;

public final class OrderItem extends BaseEntity {
    private final int orderId;
    private final int watchId;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderItem(int orderId, int watchId, int quantity, BigDecimal unitPrice) {
        super();
        this.orderId = EntityValidation.parentId(orderId, "Order ID");
        this.watchId = EntityValidation.positiveId(watchId, "Watch ID");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order item quantity must be greater than 0.");
        }
        this.quantity = quantity;
        this.unitPrice = EntityValidation.money(unitPrice, "Unit price");
    }

    public int getOrderId() { return orderId; }
    public int getWatchId() { return watchId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
