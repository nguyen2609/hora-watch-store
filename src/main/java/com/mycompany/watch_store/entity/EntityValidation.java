package com.mycompany.watch_store.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared validation for the M04 entities; does not access the database. */
final class EntityValidation {
    private EntityValidation() {
    }

    static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be null or blank.");
        }
        return optionalText(value.strip(), field, maxLength);
    }

    static String optionalText(String value, String field, int maxLength) {
        if (value != null && value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters.");
        }
        return value;
    }

    static int positiveId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than 0.");
        }
        return id;
    }

    static int parentId(int id, String field) {
        if (id < 0) {
            throw new IllegalArgumentException(field + " must be 0 (unsaved parent) or positive.");
        }
        return id;
    }

    static BigDecimal money(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be null or negative.");
        }
        BigDecimal wholeValue;
        try {
            wholeValue = value.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(field + " must be a whole monetary amount.", exception);
        }
        if (wholeValue.compareTo(new BigDecimal("999999999999")) > 0) {
            throw new IllegalArgumentException(field + " must fit NUMERIC(12,0).");
        }
        return wholeValue;
    }
}
