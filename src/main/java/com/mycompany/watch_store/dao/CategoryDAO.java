package com.mycompany.watch_store.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.entity.Category;

/** Read-only category lookup for the management product form. */
public final class CategoryDAO {
    public List<Category> findAll() throws SQLException {
        return JdbcTransactions.execute(null, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, description, created_at FROM app.categories ORDER BY name, id");
                 ResultSet result = statement.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (result.next()) {
                    Category category = new Category(result.getString("name"), result.getString("description"));
                    category.setId(result.getInt("id"));
                    category.setCreatedAt(result.getObject("created_at", LocalDateTime.class));
                    categories.add(category);
                }
                return List.copyOf(categories);
            }
        });
    }
}
