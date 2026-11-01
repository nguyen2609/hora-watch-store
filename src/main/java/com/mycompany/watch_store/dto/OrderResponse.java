package com.mycompany.watch_store.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(int id, String recipientName, String recipientPhone, String shippingAddress,
                            BigDecimal totalAmount, String status, String paymentMethod,
                            List<Item> items, PaymentResponse payment, String createdAt) {
    public OrderResponse {
        items = List.copyOf(items);
    }

    /** Name is current catalog text; unitPrice is the immutable purchase-time snapshot. */
    public record Item(int watchId, String name, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
