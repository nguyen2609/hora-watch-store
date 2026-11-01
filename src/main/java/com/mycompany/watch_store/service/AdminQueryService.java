package com.mycompany.watch_store.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.CategoryDAO;
import com.mycompany.watch_store.dao.OrderDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.dto.CategoryResponse;
import com.mycompany.watch_store.dto.DtoMapper;
import com.mycompany.watch_store.dto.OrderResponse;
import com.mycompany.watch_store.dto.WatchResponse;
import com.mycompany.watch_store.entity.Order;
import com.mycompany.watch_store.entity.Watch;

/** Read-only management views; each calling route must require ADMIN. */
public final class AdminQueryService {
    public List<WatchResponse> watches() throws SQLException {
        return new WatchDAO().findAll().stream().map(DtoMapper::watch).toList();
    }

    public WatchResponse watch(int id) throws SQLException {
        Watch watch = new WatchDAO().findById(id);
        if (watch == null) {
            throw new NoSuchElementException("Watch does not exist.");
        }
        return DtoMapper.watch(watch);
    }

    public List<CategoryResponse> categories() throws SQLException {
        return new CategoryDAO().findAll().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName())).toList();
    }

    public List<OrderResponse> orders() throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            List<OrderResponse> result = new ArrayList<>();
            for (Order order : new OrderDAO(connection).findAll()) {
                result.add(ApiViewService.orderResponse(connection, order));
            }
            return List.copyOf(result);
        });
    }

    public OrderResponse order(int id) throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            Order order = new OrderDAO(connection).findById(id);
            if (order == null) {
                throw new NoSuchElementException("Order does not exist.");
            }
            return ApiViewService.orderResponse(connection, order);
        });
    }
}
