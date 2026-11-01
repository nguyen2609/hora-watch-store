package com.mycompany.watch_store.dto;

import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.Payment;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.User;
import com.mycompany.watch_store.entity.Watch;

/** Explicit allowlists prevent Entity internals from becoming public JSON fields. */
public final class DtoMapper {
    private DtoMapper() {
    }

    public static UserResponse user(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    public static WatchResponse watch(Watch watch) {
        return new WatchResponse(watch.getId(), watch.getCategoryId(), watch.getName(), watch.getBrand(),
                watch.getModelNumber(), watch.getPrice(), watch.getStockQuantity(), watch.getWatchType(),
                watch instanceof AutomaticWatch automatic ? automatic.getMovementType() : null,
                watch instanceof SmartWatch smart ? smart.getBatteryLifeHours() : null,
                watch.getDescription(), watch.getImageUrl(), watch.isAvailable(), watch.getWarrantyInfo());
    }

    public static PaymentResponse payment(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getMethod(),
                payment.getStatus(), payment.getPaidAt() == null ? null : payment.getPaidAt().toString());
    }
}
