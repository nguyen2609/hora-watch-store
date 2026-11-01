package com.mycompany.watch_store.controller;

import java.util.Set;

import com.mycompany.watch_store.dto.CheckoutRequest;
import com.mycompany.watch_store.dto.OrderResponse;
import com.mycompany.watch_store.http.ApiRouter;
import com.mycompany.watch_store.http.HttpJson;
import com.mycompany.watch_store.service.ApiViewService;

import static com.mycompany.watch_store.http.ApiRouter.Access.CUSTOMER;

public final class OrderHandler {
    public OrderHandler(ApiRouter router, ApiViewService views) {
        router.add("POST", "/api/orders/checkout", CUSTOMER, (exchange, path, session) -> {
            CheckoutRequest request = HttpJson.read(exchange, CheckoutRequest.class,
                    Set.of("recipientName", "recipientPhone", "shippingAddress")).value();
            OrderResponse order = views.checkout(session.userId(), request);
            exchange.getResponseHeaders().set("Location", "/api/orders/" + order.id());
            HttpJson.send(exchange, 201, order);
        });
        router.add("GET", "/api/orders/my", CUSTOMER, (exchange, path, session) ->
                HttpJson.send(exchange, 200, views.history(session.userId())));
        router.add("GET", "/api/orders/([^/]+)", CUSTOMER, (exchange, path, session) ->
                HttpJson.send(exchange, 200, views.details(session.userId(), ApiRouter.id(path.group(1)))));
    }
}
