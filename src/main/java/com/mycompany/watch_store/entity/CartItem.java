package com.mycompany.watch_store.entity;

public final class CartItem extends BaseEntity {
    private final int cartId;
    private final int watchId;
    private int quantity;

    public CartItem(int cartId, int watchId, int quantity) {
        super();
        this.cartId = EntityValidation.parentId(cartId, "Cart ID");
        this.watchId = EntityValidation.positiveId(watchId, "Watch ID");
        updateQuantity(quantity);
    }

    public int getCartId() { return cartId; }
    public int getWatchId() { return watchId; }
    public int getQuantity() { return quantity; }

    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Cart item quantity must be greater than 0.");
        }
        this.quantity = quantity;
    }
}
