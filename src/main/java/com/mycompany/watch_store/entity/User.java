package com.mycompany.watch_store.entity;

public final class User extends BaseEntity {
    private String fullName;
    private String email;
    private String passwordHash;
    private String phone;
    private String role;

    public User(String fullName, String email, String passwordHash, String phone) {
        this(fullName, email, passwordHash, phone, "CUSTOMER");
    }

    public User(String fullName, String email, String passwordHash, String phone, String role) {
        super();
        setFullName(fullName);
        setEmail(email);
        setPasswordHash(passwordHash);
        setPhone(phone);
        setRole(role);
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }

    public void setFullName(String fullName) {
        this.fullName = EntityValidation.requiredText(fullName, "Full name", 100);
    }

    public void setEmail(String email) {
        this.email = EntityValidation.requiredText(email, "Email", 150);
    }

    /** Accepts an already computed hash, never a plaintext password. */
    public void setPasswordHash(String passwordHash) {
        EntityValidation.requiredText(passwordHash, "Password hash", 255);
        this.passwordHash = EntityValidation.optionalText(passwordHash, "Password hash", 255);
    }

    public void setPhone(String phone) {
        this.phone = EntityValidation.optionalText(phone, "Phone", 20);
    }

    public void setRole(String role) {
        if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Role must be CUSTOMER or ADMIN.");
        }
        this.role = role;
    }

    public boolean hasRole(String role) {
        return this.role.equals(role);
    }
}
