package com.mycompany.watch_store.service;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Locale;
import java.util.UUID;

import com.mycompany.watch_store.dao.UserDAO;
import com.mycompany.watch_store.entity.User;

public final class AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid email or password.";
    // Perform a password check even when a validly formatted email is unknown.
    private static final String DUMMY_HASH = PasswordHasher.hash(UUID.randomUUID().toString().substring(0, 15));
    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO must not be null.");
        }
        this.userDAO = userDAO;
    }

    /** Returns an internal entity, never a public HTTP response or a login session. */
    public User register(String fullName, String email, String password, String phone) throws SQLException {
        String name = requiredText(fullName, "Full name", 100);
        String normalizedEmail = normalizeEmail(email);
        if (phone != null && phone.codePointCount(0, phone.length()) > 20) {
            throw new IllegalArgumentException("Phone must not exceed 20 characters.");
        }
        if (userDAO.findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        String hash = PasswordHasher.hash(password);
        User user = new User(name, normalizedEmail, hash, phone, "CUSTOMER");
        try {
            userDAO.insert(user);
        } catch (SQLIntegrityConstraintViolationException exception) {
            if (!"23505".equals(exception.getSQLState())) {
                throw exception;
            }
            // Covers a concurrent registration after the earlier availability check.
            throw new IllegalArgumentException("Email is already registered.");
        }
        return user;
    }

    /** Credential verification only. The returned entity must stay inside trusted application code. */
    public User login(String email, String password) throws SQLException {
        String normalizedEmail;
        try {
            normalizedEmail = normalizeEmail(email);
        } catch (IllegalArgumentException exception) {
            throw new SecurityException(INVALID_CREDENTIALS);
        }
        User user = userDAO.findByEmail(normalizedEmail);
        boolean verified = PasswordHasher.verify(password, user == null ? DUMMY_HASH : user.getPasswordHash());
        if (user == null || !verified) {
            throw new SecurityException(INVALID_CREDENTIALS);
        }
        return user;
    }

    private static String normalizeEmail(String email) {
        String normalized = requiredText(email, "Email", 150).toLowerCase(Locale.ROOT);
        if (normalized.codePointCount(0, normalized.length()) > 150
                || !normalized.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
        return normalized;
    }

    private static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank.");
        }
        String stripped = value.strip();
        if (stripped.codePointCount(0, stripped.length()) > maxLength) {
            throw new IllegalArgumentException(field + " exceeds its maximum length.");
        }
        return stripped;
    }
}
