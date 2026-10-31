package com.mycompany.watch_store.factory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;

public final class WatchFactory {
    private WatchFactory() {
    }

    /** Creates a persisted watch from values already extracted by the DAO. */
    public static Watch createWatch(int id, int categoryId, String name, String brand,
                                    String modelNumber, BigDecimal price, int stockQuantity,
                                    String watchType, String movementType, Integer batteryLifeHours,
                                    String description, String imageUrl, boolean available,
                                    LocalDateTime createdAt) {
        Watch watch;
        if ("AUTOMATIC".equals(watchType)) {
            if (batteryLifeHours != null) {
                throw new IllegalArgumentException("An automatic watch must not have battery life hours.");
            }
            watch = new AutomaticWatch(categoryId, name, brand, modelNumber, price, stockQuantity,
                    description, imageUrl, available, movementType);
        } else if ("SMART".equals(watchType)) {
            if (movementType != null || batteryLifeHours == null) {
                throw new IllegalArgumentException("A smart watch requires battery life hours and no movement type.");
            }
            watch = new SmartWatch(categoryId, name, brand, modelNumber, price, stockQuantity,
                    description, imageUrl, available, batteryLifeHours);
        } else {
            throw new IllegalArgumentException("Unsupported watch type.");
        }
        watch.setId(id);
        watch.setCreatedAt(createdAt);
        return watch;
    }
}
