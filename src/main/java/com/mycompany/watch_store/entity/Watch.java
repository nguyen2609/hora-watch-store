package com.mycompany.watch_store.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Watch extends BaseEntity {
    private int categoryId;
    private String name;
    private String brand;
    private String modelNumber;
    private BigDecimal price;
    private int stockQuantity;
    private final String watchType;
    private String description;
    private String imageUrl;
    private boolean available;

    protected Watch(int categoryId, String name, String brand, String modelNumber,
                    BigDecimal price, int stockQuantity, String watchType,
                    String description, String imageUrl, boolean available) {
        super();
        if (!"AUTOMATIC".equals(watchType) && !"SMART".equals(watchType)) {
            throw new IllegalArgumentException("Watch type must be AUTOMATIC or SMART.");
        }
        this.watchType = watchType;
        setCategoryId(categoryId);
        setName(name);
        setBrand(brand);
        setModelNumber(modelNumber);
        setPrice(price);
        setStockQuantity(stockQuantity);
        setDescription(description);
        setImageUrl(imageUrl);
        setAvailable(available);
    }

    public int getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getModelNumber() { return modelNumber; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public String getWatchType() { return watchType; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }

    public final void setCategoryId(int categoryId) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be greater than 0.");
        }
        this.categoryId = categoryId;
    }

    public final void setName(String name) {
        this.name = requireText(name, "Name");
    }

    public final void setBrand(String brand) {
        this.brand = requireText(brand, "Brand");
    }

    public final void setModelNumber(String modelNumber) {
        this.modelNumber = requireText(modelNumber, "Model number");
    }

    public final void setPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must not be null or negative.");
        }
        BigDecimal wholePrice;
        try {
            wholePrice = price.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Price must be a whole monetary amount.", exception);
        }
        if (wholePrice.compareTo(new BigDecimal("999999999999")) > 0) {
            throw new IllegalArgumentException("Price must fit NUMERIC(12,0).");
        }
        this.price = wholePrice;
    }

    public final void setStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity must not be negative.");
        }
        this.stockQuantity = stockQuantity;
    }

    // Description and image URL are optional at the Java model level.
    public final void setDescription(String description) { this.description = description; }
    public final void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public final void setAvailable(boolean available) { this.available = available; }

    public String getWarrantyInfo() {
        return "Warranty depends on watch type";
    }

    public final void reduceStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reduction quantity must be greater than 0.");
        }
        if (quantity > stockQuantity) {
            throw new IllegalArgumentException("Insufficient stock.");
        }
        stockQuantity -= quantity;
    }

    public final void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Increase quantity must be greater than 0.");
        }
        if (quantity > Integer.MAX_VALUE - stockQuantity) {
            throw new IllegalArgumentException("Stock quantity would exceed the integer limit.");
        }
        stockQuantity += quantity;
    }

    public final boolean isInStock() {
        return stockQuantity > 0;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank.");
        }
        return value.strip();
    }
}
