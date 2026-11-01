package com.mycompany.watch_store.controller;

import java.util.Set;

import com.mycompany.watch_store.dto.OrderStatusRequest;
import com.mycompany.watch_store.http.ApiRouter;
import com.mycompany.watch_store.http.HttpJson;
import com.mycompany.watch_store.service.ApiViewService;
import com.mycompany.watch_store.service.AdminQueryService;

import static com.mycompany.watch_store.http.ApiRouter.Access.ADMIN;

public final class AdminOrderHandler {
    public AdminOrderHandler(ApiRouter router, ApiViewService views) {
        AdminQueryService management = new AdminQueryService();
        router.add("GET", "/api/admin/orders", ADMIN, (exchange, path, session) ->
                HttpJson.send(exchange, 200, management.orders()));
        router.add("GET", "/api/admin/orders/([^/]+)", ADMIN, (exchange, path, session) ->
                HttpJson.send(exchange, 200, management.order(ApiRouter.id(path.group(1)))));
        router.add("PATCH", "/api/admin/orders/([^/]+)/status", ADMIN, (exchange, path, session) -> {
            OrderStatusRequest request = HttpJson.read(exchange, OrderStatusRequest.class, Set.of("status")).value();
            HttpJson.send(exchange, 200, views.updateStatus(ApiRouter.id(path.group(1)), request.status()));
        });
        router.add("PATCH", "/api/admin/orders/([^/]+)/payment", ADMIN, (exchange, path, session) -> {
            HttpJson.emptyBody(exchange);
            HttpJson.send(exchange, 200, views.confirmPayment(ApiRouter.id(path.group(1))));
        });
    }
}
