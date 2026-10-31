package com.mycompany.watch_store.demo;

import java.math.BigDecimal;
import java.util.List;

import com.mycompany.watch_store.entity.Cart;
import com.mycompany.watch_store.entity.CartItem;
import com.mycompany.watch_store.entity.Category;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.OrderItem;
import com.mycompany.watch_store.entity.Payment;
import com.mycompany.watch_store.entity.User;

public final class EntityRelationshipDemo {
    private EntityRelationshipDemo() {
    }

    public static void main(String[] args) {
        // A non-secret hash fixture only; no real credentials or hashing logic.
        User user = new User("Nguyen An", "an@example.com", "demo-hash-not-for-authentication", null);
        System.out.println("User: " + user.getFullName() + ", CUSTOMER: " + user.hasRole("CUSTOMER"));
        System.out.println("New user ID: " + user.getId());
        // Simulate IDs of existing records; this demo never connects to a database.
        user.setId(1);
        Category category = new Category("Everyday watches", "In-memory category example");
        System.out.println("Category: " + category.getName());

        Cart cart = new Cart(user.getId());
        CartItem firstItem = new CartItem(0, 101, 2);
        cart.addItem(firstItem);
        cart.addItem(new CartItem(0, 102, 1));
        System.out.println("Cart item count: " + cart.getItems().size());
        reject("Duplicate cart product", () -> cart.addItem(new CartItem(0, 101, 1)));
        firstItem.updateQuantity(9);
        List<CartItem> snapshot = cart.getItems();
        snapshot.get(0).updateQuantity(8);
        System.out.println("Cart quantity protected from outside changes: " + cart.getItems().get(0).getQuantity());
        reject("Read-only cart list", () -> snapshot.clear());
        reject("Zero cart quantity", () -> new CartItem(0, 103, 0));
        reject("Wrong cart owner", () -> cart.addItem(new CartItem(99, 103, 1)));

        Order order = new Order(user.getId(), user.getFullName(), "0901234567", "123 Example Street");
        order.addItem(new OrderItem(0, 101, 2, new BigDecimal("6500000")));
        order.addItem(new OrderItem(0, 102, 1, new BigDecimal("4500000")));
        for (OrderItem item : order.getItems()) {
            System.out.println("Order line: " + item.getUnitPrice() + " x " + item.getQuantity()
                    + " = " + item.getSubtotal());
        }
        System.out.println("Order total: " + order.calculateTotal());
        System.out.println("Stored total: " + order.getTotalAmount());
        reject("Duplicate order product", () -> order.addItem(new OrderItem(0, 101, 1, BigDecimal.ZERO)));
        reject("Read-only order list", () -> order.getItems().clear());
        reject("Negative price", () -> new OrderItem(0, 103, 1, new BigDecimal("-1")));
        reject("Fractional price", () -> new OrderItem(0, 103, 1, new BigDecimal("1.5")));
        reject("Order total overflow", () -> order.addItem(
                new OrderItem(0, 103, 2, new BigDecimal("999999999999"))));
        System.out.println("Total after rejected additions: " + order.getTotalAmount());

        System.out.println("Initial order status: " + order.getStatus());
        reject("Skip order status", () -> order.changeStatus("DELIVERED"));
        order.changeStatus("CONFIRMED");
        System.out.println("Order status: " + order.getStatus());
        reject("Repeated order status", () -> order.changeStatus("CONFIRMED"));
        order.changeStatus("SHIPPING");
        System.out.println("Order status: " + order.getStatus());
        reject("Cancel shipping order", () -> order.changeStatus("CANCELLED"));
        order.changeStatus("DELIVERED");
        System.out.println("Order status: " + order.getStatus());
        reject("Leave final status", () -> order.changeStatus("CONFIRMED"));

        Order cancelled = new Order(user.getId(), user.getFullName(), "0901234567", "123 Example Street");
        cancelled.changeStatus("CANCELLED");
        System.out.println("Pending cancellation: " + cancelled.getStatus());
        reject("Leave cancelled status", () -> cancelled.changeStatus("CONFIRMED"));
        Order confirmedCancellation = new Order(user.getId(), user.getFullName(), "0901234567", "123 Example Street");
        confirmedCancellation.changeStatus("CONFIRMED");
        confirmedCancellation.changeStatus("CANCELLED");
        System.out.println("Confirmed cancellation: " + confirmedCancellation.getStatus());

        Payment payment = new Payment(order.getId(), order.getTotalAmount());
        System.out.println("Payment: " + payment.getMethod() + ", " + payment.getStatus()
                + ", parent ID: " + payment.getOrderId() + ", paidAt: " + payment.getPaidAt());
        payment.markAsPaid();
        System.out.println("Payment confirmed: " + payment.isPaid() + ", paidAt present: " + (payment.getPaidAt() != null));
        reject("Repeated payment", payment::markAsPaid);
        reject("Negative payment", () -> new Payment(0, new BigDecimal("-1")));
        reject("Unknown role", () -> user.setRole("MANAGER"));
        cart.removeItem(101);
        System.out.println("Cart items after removal: " + cart.getItems().size());
        cart.clearItems();
        System.out.println("Cart items after clear: " + cart.getItems().size());
        System.out.println("Entity relationship demo finished normally.");
    }

    private static void reject(String operation, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException exception) {
            System.out.println("Expected rejection [" + operation + "]: " + exception.getMessage());
            return;
        }
        throw new IllegalStateException("Expected validation did not reject: " + operation);
    }
}
