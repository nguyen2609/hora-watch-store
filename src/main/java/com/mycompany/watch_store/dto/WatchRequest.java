package com.mycompany.watch_store.dto;

import java.math.BigDecimal;

/** Boxed values distinguish absent PATCH fields from zero and false. */
public record WatchRequest(Integer categoryId, String name, String brand, String modelNumber,
                           BigDecimal price, Integer stockQuantity, String watchType,
                           String movementType, Integer batteryLifeHours, String description,
                           String imageUrl, Boolean available) {
}
