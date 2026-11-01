package com.mycompany.watch_store.dto;

import java.util.Set;

import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;

/** Converts only authorized administrator input into the existing validated Entity hierarchy. */
public final class WatchInputMapper {
    private WatchInputMapper() {
    }

    public static Watch create(WatchRequest request) {
        if (request.categoryId() == null || request.stockQuantity() == null) {
            throw new IllegalArgumentException("Category and stock are required.");
        }
        boolean available = request.available() == null || request.available();
        if ("AUTOMATIC".equals(request.watchType()) && request.batteryLifeHours() == null) {
            return new AutomaticWatch(request.categoryId(), request.name(), request.brand(), request.modelNumber(),
                    request.price(), request.stockQuantity(), request.description(), request.imageUrl(), available,
                    request.movementType());
        }
        if ("SMART".equals(request.watchType()) && request.movementType() == null && request.batteryLifeHours() != null) {
            return new SmartWatch(request.categoryId(), request.name(), request.brand(), request.modelNumber(),
                    request.price(), request.stockQuantity(), request.description(), request.imageUrl(), available,
                    request.batteryLifeHours());
        }
        throw new IllegalArgumentException("Watch subtype fields are invalid.");
    }

    public static Watch patch(Watch watch, WatchRequest request, Set<String> fields) {
        if (fields.isEmpty() || (fields.contains("watchType") && !watch.getWatchType().equals(request.watchType()))) {
            throw new IllegalArgumentException("Patch must contain fields and preserve the watch subtype.");
        }
        if (fields.contains("categoryId")) watch.setCategoryId(request.categoryId());
        if (fields.contains("name")) watch.setName(request.name());
        if (fields.contains("brand")) watch.setBrand(request.brand());
        if (fields.contains("modelNumber")) watch.setModelNumber(request.modelNumber());
        if (fields.contains("price")) watch.setPrice(request.price());
        if (fields.contains("stockQuantity")) watch.setStockQuantity(request.stockQuantity());
        if (fields.contains("description")) watch.setDescription(request.description());
        if (fields.contains("imageUrl")) watch.setImageUrl(request.imageUrl());
        if (fields.contains("available")) watch.setAvailable(request.available());
        if (watch instanceof AutomaticWatch automatic) {
            if (fields.contains("batteryLifeHours")) {
                throw new IllegalArgumentException("Automatic watches do not have battery life hours.");
            }
            if (fields.contains("movementType")) automatic.setMovementType(request.movementType());
        } else if (watch instanceof SmartWatch smart) {
            if (fields.contains("movementType") || (fields.contains("batteryLifeHours") && request.batteryLifeHours() == null)) {
                throw new IllegalArgumentException("Smart watch subtype fields are invalid.");
            }
            if (fields.contains("batteryLifeHours")) smart.setBatteryLifeHours(request.batteryLifeHours());
        }
        return watch;
    }
}
