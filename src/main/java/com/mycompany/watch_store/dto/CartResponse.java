package com.mycompany.watch_store.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(int id, List<Item> items, BigDecimal totalAmount, String updatedAt) {
    public CartResponse {
        items = List.copyOf(items);
    }

    public record Item(int watchId, String name, int quantity, BigDecimal unitPrice,
                       BigDecimal subtotal, boolean available) {
    }
}
