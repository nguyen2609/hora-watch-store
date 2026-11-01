package com.mycompany.watch_store.controller;

import java.util.Set;

import com.mycompany.watch_store.dto.CartItemRequest;
import com.mycompany.watch_store.http.ApiRouter;
import com.mycompany.watch_store.http.HttpJson;
import com.mycompany.watch_store.service.ApiViewService;

import static com.mycompany.watch_store.http.ApiRouter.Access.CUSTOMER;

public final class CartHandler {
    public CartHandler(ApiRouter router, ApiViewService views) {
        router.add("GET", "/api/cart", CUSTOMER, (exchange, path, session) ->
                HttpJson.send(exchange, 200, views.cart(session.userId())));
        router.add("POST", "/api/cart/items", CUSTOMER, (exchange, path, session) -> {
            CartItemRequest request = HttpJson.read(exchange, CartItemRequest.class, Set.of("watchId", "quantity")).value();
            HttpJson.send(exchange, 200, views.changeCart(session.userId(),
                    carts -> carts.addToCart(session.userId(), request.watchId(), request.quantity())));
        });
        router.add("PATCH", "/api/cart/items/([^/]+)", CUSTOMER, (exchange, path, session) -> {
            var body = HttpJson.read(exchange, CartItemRequest.class, Set.of("quantity"));
            if (body.fields().contains("watchId")) {
                throw HttpJson.badRequest();
            }
            int watchId = ApiRouter.id(path.group(1));
            HttpJson.send(exchange, 200, views.changeCart(session.userId(),
                    carts -> carts.updateQuantity(session.userId(), watchId, body.value().quantity())));
        });
        router.add("DELETE", "/api/cart/items/([^/]+)", CUSTOMER, (exchange, path, session) -> {
            HttpJson.emptyBody(exchange);
            int watchId = ApiRouter.id(path.group(1));
            HttpJson.send(exchange, 200, views.changeCart(session.userId(),
                    carts -> carts.removeFromCart(session.userId(), watchId)));
        });
        router.add("DELETE", "/api/cart/items", CUSTOMER, (exchange, path, session) -> {
            HttpJson.emptyBody(exchange);
            HttpJson.send(exchange, 200, views.changeCart(session.userId(), carts -> carts.clearCart(session.userId())));
        });
    }
}
