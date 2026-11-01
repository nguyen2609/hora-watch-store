package com.mycompany.watch_store.dto;

import java.math.BigDecimal;

public record WatchResponse(int id, int categoryId, String name, String brand, String modelNumber,
                            BigDecimal price, int stockQuantity, String watchType, String movementType,
                            Integer batteryLifeHours, String description, String imageUrl,
                            boolean available, String warrantyInfo) {
}
