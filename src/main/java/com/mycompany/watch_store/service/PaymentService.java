package com.mycompany.watch_store.service;

import java.sql.SQLException;

public interface PaymentService {
    void confirmPayment(int orderId) throws SQLException;
}
