package com.mycompany.watch_store.entity;

import java.time.LocalDateTime;

public abstract class BaseEntity {
    private int id;
    private LocalDateTime createdAt;

    protected BaseEntity() {
        createdAt = LocalDateTime.now();
    }

    protected BaseEntity(int id, LocalDateTime createdAt) {
        if (id <= 0) {
            throw new IllegalArgumentException("Existing entity ID must be greater than 0.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation time must not be null.");
        }
        this.id = id;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Entity ID must be greater than 0.");
        }
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation time must not be null.");
        }
        this.createdAt = createdAt;
    }
}
