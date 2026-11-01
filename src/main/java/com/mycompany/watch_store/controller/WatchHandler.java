package com.mycompany.watch_store.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mycompany.watch_store.dto.DtoMapper;
import com.mycompany.watch_store.dto.WatchInputMapper;
import com.mycompany.watch_store.dto.WatchRequest;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.http.ApiRouter;
import com.mycompany.watch_store.http.HttpJson;
import com.mycompany.watch_store.service.WatchService;
import com.mycompany.watch_store.service.AdminQueryService;

import static com.mycompany.watch_store.http.ApiRouter.Access.ADMIN;
import static com.mycompany.watch_store.http.ApiRouter.Access.PUBLIC;

public final class WatchHandler {
    public WatchHandler(ApiRouter router) {
        WatchService watches = new WatchService();
        AdminQueryService management = new AdminQueryService();
        router.add("GET", "/api/admin/watches", ADMIN, (exchange, path, session) ->
                HttpJson.send(exchange, 200, management.watches()));
        router.add("GET", "/api/admin/watches/([^/]+)", ADMIN, (exchange, path, session) ->
                HttpJson.send(exchange, 200, management.watch(ApiRouter.id(path.group(1)))));
        router.add("GET", "/api/admin/categories", ADMIN, (exchange, path, session) ->
                HttpJson.send(exchange, 200, management.categories()));
        router.add("GET", "/api/watches", PUBLIC, (exchange, path, session) -> {
            Map<String, String> query = ApiRouter.query(exchange, Set.of("keyword", "maxPrice"));
            List<Watch> result;
            if (query.containsKey("maxPrice")) {
                String number = query.get("maxPrice");
                if (number.length() > 64 || number.isBlank()) {
                    throw HttpJson.badRequest();
                }
                BigDecimal maximum = new BigDecimal(number);
                if (maximum.precision() > 24 || Math.abs((long) maximum.scale()) > 12) {
                    throw HttpJson.badRequest();
                }
                result = watches.searchWatches(query.get("keyword"), maximum);
            } else {
                result = watches.searchWatches(query.get("keyword"));
            }
            HttpJson.send(exchange, 200, result.stream().map(DtoMapper::watch).toList());
        });
        router.add("GET", "/api/watches/([^/]+)", PUBLIC, (exchange, path, session) ->
                HttpJson.send(exchange, 200, DtoMapper.watch(watches.getWatchById(ApiRouter.id(path.group(1))))));
        router.add("POST", "/api/admin/watches", ADMIN, (exchange, path, session) -> {
            WatchRequest request = HttpJson.read(exchange, WatchRequest.class,
                    Set.of("categoryId", "name", "brand", "modelNumber", "price", "stockQuantity", "watchType")).value();
            Watch watch = WatchInputMapper.create(request);
            watches.createWatch(watch);
            exchange.getResponseHeaders().set("Location", "/api/watches/" + watch.getId());
            HttpJson.send(exchange, 201, DtoMapper.watch(watch));
        });
        router.add("PATCH", "/api/admin/watches/([^/]+)", ADMIN, (exchange, path, session) -> {
            var body = HttpJson.read(exchange, WatchRequest.class, Set.of());
            Watch updated = watches.patchWatch(ApiRouter.id(path.group(1)),
                    watch -> WatchInputMapper.patch(watch, body.value(), body.fields()));
            HttpJson.send(exchange, 200, DtoMapper.watch(updated));
        });
        router.add("DELETE", "/api/admin/watches/([^/]+)", ADMIN, (exchange, path, session) -> {
            HttpJson.emptyBody(exchange);
            watches.discontinueWatch(ApiRouter.id(path.group(1)));
            HttpJson.send(exchange, 204, null);
        });
    }
}
