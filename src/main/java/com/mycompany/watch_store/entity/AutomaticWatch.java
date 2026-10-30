package com.mycompany.watch_store.entity;

import java.math.BigDecimal;

public final class AutomaticWatch extends Watch {
    private String movementType;

    public AutomaticWatch(int categoryId, String name, String brand, String modelNumber,
                          BigDecimal price, int stockQuantity, String description,
                          String imageUrl, boolean available, String movementType) {
        super(categoryId, name, brand, modelNumber, price, stockQuantity, "AUTOMATIC",
                description, imageUrl, available);
        setMovementType(movementType);
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        if (movementType == null || movementType.isBlank()) {
            throw new IllegalArgumentException("Movement type must not be null or blank.");
        }
        this.movementType = movementType.strip();
    }

    @Override
    public String getWarrantyInfo() {
        return "24-month mechanical warranty";
    }
}
