package com.mycompany.watch_store.dto;

public record CheckoutRequest(String recipientName, String recipientPhone, String shippingAddress) {
}
