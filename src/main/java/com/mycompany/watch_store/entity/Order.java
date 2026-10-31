package com.mycompany.watch_store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class Order extends BaseEntity {
    private final int userId;
    private final String recipientName;
    private final String recipientPhone;
    private final String shippingAddress;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String status = "PENDING";
    private final String paymentMethod = "COD";
    private final List<OrderItem> items = new ArrayList<>();

    public Order(int userId, String recipientName, String recipientPhone, String shippingAddress) {
        super();
        this.userId = EntityValidation.positiveId(userId, "User ID");
        this.recipientName = EntityValidation.requiredText(recipientName, "Recipient name", 100);
        this.recipientPhone = EntityValidation.requiredText(recipientPhone, "Recipient phone", 20);
        this.shippingAddress = EntityValidation.requiredText(shippingAddress, "Shipping address", Integer.MAX_VALUE);
    }

    /** Rehydrates stored state without replaying business transitions or exposing a status setter. */
    public Order(int id, int userId, String recipientName, String recipientPhone, String shippingAddress,
                 String status, LocalDateTime createdAt, List<OrderItem> items) {
        this(userId, recipientName, recipientPhone, shippingAddress);
        setId(id);
        setCreatedAt(createdAt);
        if (status == null || !List.of("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Unknown stored order status.");
        }
        if (items == null) {
            throw new IllegalArgumentException("Order items must not be null.");
        }
        for (OrderItem item : items) {
            addItem(item);
        }
        this.status = status;
    }

    public int getUserId() { return userId; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }

    public void addItem(OrderItem item) {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("Items can only be added to a PENDING order.");
        }
        if (item == null) {
            throw new IllegalArgumentException("Order item must not be null.");
        }
        if (item.getOrderId() != 0 && item.getOrderId() != getId()) {
            throw new IllegalArgumentException("Order item belongs to a different order.");
        }
        if (items.stream().anyMatch(existing -> existing.getWatchId() == item.getWatchId())) {
            throw new IllegalArgumentException("Order already contains watch ID " + item.getWatchId() + ".");
        }
        BigDecimal newTotal = EntityValidation.money(totalAmount.add(item.getSubtotal()), "Order total");
        items.add(item);
        totalAmount = newTotal;
    }

    public BigDecimal calculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : items) {
            sum = sum.add(item.getSubtotal());
        }
        totalAmount = EntityValidation.money(sum, "Order total");
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public void changeStatus(String newStatus) {
        if (newStatus == null || !List.of("PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED")
                .contains(newStatus)) {
            throw new IllegalArgumentException("Unknown order status: " + newStatus);
        }
        boolean allowed = switch (status) {
            case "PENDING" -> "CONFIRMED".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "CONFIRMED" -> "SHIPPING".equals(newStatus) || "CANCELLED".equals(newStatus);
            case "SHIPPING" -> "DELIVERED".equals(newStatus);
            default -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Cannot change order status from " + status + " to " + newStatus + ".");
        }
        status = newStatus;
    }
}
