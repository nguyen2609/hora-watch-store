package com.mycompany.watch_store.entity;

public final class Category extends BaseEntity {
    private String name;
    private String description;

    public Category(String name, String description) {
        super();
        setName(name);
        setDescription(description);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    public void setName(String name) {
        this.name = EntityValidation.requiredText(name, "Category name", 100);
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
