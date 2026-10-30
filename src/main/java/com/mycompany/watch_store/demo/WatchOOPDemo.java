package com.mycompany.watch_store.demo;

import java.math.BigDecimal;

import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;

public final class WatchOOPDemo {
    private WatchOOPDemo() {
    }

    public static void main(String[] args) {
        Watch watch1 = new AutomaticWatch(1, "Classic Automatic", "Demo Brand", "AUTO-01",
                new BigDecimal("5000000"), 5, "Mechanical watch", null, true, "Self-winding");
        Watch watch2 = new SmartWatch(2, "Daily Smart", "Demo Brand", "SMART-01",
                new BigDecimal("3000000"), 3, "Electronic watch", null, true, 48);

        System.out.println(watch1.getWatchType() + ": " + watch1.getWarrantyInfo());
        System.out.println(watch2.getWatchType() + ": " + watch2.getWarrantyInfo());
        System.out.println("New watch ID inherited from BaseEntity: " + watch1.getId());
        System.out.println("Initial stock: " + watch1.getStockQuantity());
        watch1.reduceStock(2);
        System.out.println("After selling 2: " + watch1.getStockQuantity());
        try {
            watch1.reduceStock(4);
        } catch (IllegalArgumentException exception) {
            System.out.println("Expected stock rejection: " + exception.getMessage());
        }
        System.out.println("Stock after rejection: " + watch1.getStockQuantity());
        watch1.increaseStock(2);
        System.out.println("After restocking 2: " + watch1.getStockQuantity());
        System.out.println("In stock: " + watch1.isInStock());
        try {
            watch1.increaseStock(Integer.MAX_VALUE);
        } catch (IllegalArgumentException exception) {
            System.out.println("Expected overflow rejection: " + exception.getMessage());
        }
        System.out.println("Demo finished normally.");
    }
}
