package com.mycompany.watch_store.dto;

/** PATCH uses only quantity; the product identity comes from the route. */
public record CartItemRequest(Integer watchId, Integer quantity) {
}
