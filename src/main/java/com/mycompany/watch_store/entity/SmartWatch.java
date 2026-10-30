package com.mycompany.watch_store.entity;

import java.math.BigDecimal;

public final class SmartWatch extends Watch {
    private int batteryLifeHours;

    public SmartWatch(int categoryId, String name, String brand, String modelNumber,
                      BigDecimal price, int stockQuantity, String description,
                      String imageUrl, boolean available, int batteryLifeHours) {
        super(categoryId, name, brand, modelNumber, price, stockQuantity, "SMART",
                description, imageUrl, available);
        setBatteryLifeHours(batteryLifeHours);
    }

    public int getBatteryLifeHours() {
        return batteryLifeHours;
    }

    public void setBatteryLifeHours(int batteryLifeHours) {
        if (batteryLifeHours <= 0) {
            throw new IllegalArgumentException("Battery life hours must be greater than 0.");
        }
        this.batteryLifeHours = batteryLifeHours;
    }

    @Override
    public String getWarrantyInfo() {
        return "12-month electronic warranty";
    }
}
