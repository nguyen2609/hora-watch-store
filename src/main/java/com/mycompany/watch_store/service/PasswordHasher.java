package com.mycompany.watch_store.service;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;

public final class PasswordHasher {
    private static final int MIN_LENGTH = 15;
    private static final int MAX_LENGTH = 128;
    private static final Argon2Function ARGON2 = Argon2Function.getInstance(19456, 2, 1, 32, Argon2.ID);

    private PasswordHasher() {
    }

    /** Passwords are never trimmed or logged. Salt and parameters are encoded in the result. */
    public static String hash(String password) {
        if (!hasValidLength(password)) {
            throw new IllegalArgumentException("Password must contain 15 to 128 characters and must not be blank.");
        }
        return Password.hash(password).addRandomSalt(16).with(ARGON2).getResult();
    }

    /** Invalid passwords and unsupported or malformed stored hashes fail authentication. */
    public static boolean verify(String password, String storedHash) {
        if (!hasValidLength(password) || storedHash == null || storedHash.length() > 255
                || !storedHash.startsWith("$argon2id$")) {
            return false;
        }
        try {
            Argon2Function storedParameters = Argon2Function.getInstanceFromHash(storedHash);
            return Password.check(password, storedHash).with(storedParameters);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean hasValidLength(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        int length = password.codePointCount(0, password.length());
        return length >= MIN_LENGTH && length <= MAX_LENGTH;
    }
}
