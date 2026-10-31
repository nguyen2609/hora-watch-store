package com.mycompany.watch_store.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class Cart extends BaseEntity {
    private final int userId;
    private final List<CartItem> items = new ArrayList<>();
    private LocalDateTime updatedAt;

    public Cart(int userId) {
        super();
        this.userId = EntityValidation.positiveId(userId, "User ID");
        updatedAt = getCreatedAt();
    }

    public int getUserId() { return userId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void addItem(CartItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item must not be null.");
        }
        if (item.getCartId() != 0 && item.getCartId() != getId()) {
            throw new IllegalArgumentException("Cart item belongs to a different cart.");
        }
        if (items.stream().anyMatch(existing -> existing.getWatchId() == item.getWatchId())) {
            throw new IllegalArgumentException("Cart already contains watch ID " + item.getWatchId() + ".");
        }
        items.add(copyItem(item));
        updatedAt = LocalDateTime.now();
    }

    public void removeItem(int watchId) {
        EntityValidation.positiveId(watchId, "Watch ID");
        if (items.removeIf(item -> item.getWatchId() == watchId)) {
            updatedAt = LocalDateTime.now();
        }
    }

    public List<CartItem> getItems() {
        List<CartItem> copies = new ArrayList<>();
        for (CartItem item : items) {
            copies.add(copyItem(item));
        }
        return List.copyOf(copies);
    }

    public void clearItems() {
        if (!items.isEmpty()) {
            items.clear();
            updatedAt = LocalDateTime.now();
        }
    }

    private static CartItem copyItem(CartItem source) {
        CartItem copy = new CartItem(source.getCartId(), source.getWatchId(), source.getQuantity());
        if (source.getId() > 0) {
            copy.setId(source.getId());
        }
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }
}
