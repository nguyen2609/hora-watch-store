package com.mycompany.watch_store.dto;

import java.math.BigDecimal;

public record PaymentResponse(int id, int orderId, BigDecimal amount, String method, String status, String paidAt) {
}
