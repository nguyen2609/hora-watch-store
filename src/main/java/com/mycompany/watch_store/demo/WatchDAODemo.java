package com.mycompany.watch_store.demo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;

public final class WatchDAODemo {
    private WatchDAODemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== WATCH DAO DEMO ===");
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connection established successfully");
            connection.setReadOnly(true);
            WatchDAO dao = new WatchDAO(connection);
            List<Watch> existing = dao.findAll();
            System.out.println("Existing watches: " + existing.size());
            for (Watch watch : existing) {
                printWatch(watch);
                Watch found = dao.findById(watch.getId());
                require(found != null && found.getClass() == watch.getClass(), "Existing watch lookup failed.");
                System.out.println("findById(" + watch.getId() + "): " + found.getClass().getSimpleName());
            }

            connection.setReadOnly(false);
            demonstrateRollback(connection);
        } catch (SQLException | RuntimeException exception) {
            // Do not print driver exception messages or stack traces containing connection details.
            System.err.println("Watch DAO demonstration failed. Check local database configuration, "
                    + "schema and data compatibility. No demo transaction is committed.");
            System.exit(1);
        }
    }

    private static void demonstrateRollback(Connection connection) throws SQLException {
        String marker = "m06-" + UUID.randomUUID();
        String autoModel = marker + "-auto";
        String smartModel = marker + "-smart";
        int automaticId;
        int smartId;
        WatchDAO dao = new WatchDAO(connection);
        connection.setAutoCommit(false);
        try {
            int categoryId = chooseCategory(connection, marker);
            Watch automatic = new AutomaticWatch(categoryId, "M06 demo 'automatic'", "Demo Brand", autoModel,
                    new BigDecimal("6500000"), 2, null, null, true, "Self-winding");
            Watch smart = new SmartWatch(categoryId, "M06 demo smart", "Demo Brand", smartModel,
                    new BigDecimal("4500000"), 1, null, null, true, 48);

            automaticId = dao.insert(automatic);
            smartId = dao.insert(smart);
            require(automaticId == automatic.getId() && smartId == smart.getId(), "Generated ID assignment failed.");
            System.out.println("Inserted temporary watches: " + automaticId + ", " + smartId);
            AutomaticWatch loadedAuto = (AutomaticWatch) dao.findById(automaticId);
            SmartWatch loadedSmart = (SmartWatch) dao.findById(smartId);
            require(loadedAuto != null && loadedSmart != null, "Temporary watches were not found.");
            require(loadedAuto.getImageUrl() == null && loadedSmart.getDescription() == null,
                    "Nullable fields were not preserved.");
            require("Self-winding".equals(loadedAuto.getMovementType()) && loadedSmart.getBatteryLifeHours() == 48,
                    "Subtype mapping failed.");
            printWatch(loadedAuto);
            printWatch(loadedSmart);

            LocalDateTime originalCreation = loadedAuto.getCreatedAt();
            loadedAuto.setPrice(new BigDecimal("6600000"));
            loadedAuto.setMovementType("Updated movement");
            loadedAuto.setName("M06 demo updated");
            loadedAuto.setDescription("Updated temporary description");
            loadedAuto.increaseStock(1);
            // update() must ignore an accidental change to the Java creation timestamp.
            loadedAuto.setCreatedAt(originalCreation.minusDays(1));
            dao.update(loadedAuto);
            AutomaticWatch updated = (AutomaticWatch) dao.findById(automaticId);
            require(updated.getPrice().compareTo(new BigDecimal("6600000")) == 0
                    && "Updated movement".equals(updated.getMovementType())
                    && "M06 demo updated".equals(updated.getName()) && updated.getStockQuantity() == 3
                    && originalCreation.equals(updated.getCreatedAt()), "Automatic watch update failed.");
            loadedSmart.setBatteryLifeHours(72);
            dao.update(loadedSmart);
            require(((SmartWatch) dao.findById(smartId)).getBatteryLifeHours() == 72, "Smart watch update failed.");
            System.out.println("Updates verified for both subtypes; created_at preserved.");

            dao.delete(automaticId);
            dao.delete(smartId);
            require(!dao.findById(automaticId).isAvailable() && !dao.findById(smartId).isAvailable(),
                    "Soft delete failed.");
            require(dao.findAll().stream().anyMatch(watch -> watch.getId() == automaticId),
                    "Soft delete must preserve the row.");
            System.out.println("Soft delete verified: available=false; rows and history preserved.");
        } finally {
            // Never commit, even if a demo operation fails. Identity sequence values are not rolled back.
            connection.rollback();
            connection.setAutoCommit(true);
            System.out.println("Demo transaction rolled back.");
        }

        // This DAO uses independent connections and sees only committed data.
        WatchDAO independentDao = new WatchDAO();
        require(independentDao.findById(automaticId) == null && independentDao.findById(smartId) == null,
                "Temporary watches remain after rollback.");
        verifyCleanup(connection, marker, autoModel, smartModel);
        System.out.println("Rollback verified: no temporary watches or categories remain.");
        System.out.println("Note: rolled-back inserts may leave gaps in identity sequences.");
        System.out.println("Watch DAO demo finished normally.");
    }

    private static int chooseCategory(Connection connection, String marker) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM app.categories ORDER BY id LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                return result.getInt(1);
            }
        }
        // Only needed for an empty database; this temporary category is rolled back too.
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app.categories (name) VALUES (?) RETURNING id")) {
            statement.setString(1, marker);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Temporary category insert returned no ID.");
                }
                return result.getInt(1);
            }
        }
    }

    private static void verifyCleanup(Connection connection, String marker, String autoModel, String smartModel)
            throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM app.watches WHERE model_number IN (?, ?))
                     + (SELECT COUNT(*) FROM app.categories WHERE name = ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, autoModel);
            statement.setString(2, smartModel);
            statement.setString(3, marker);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getLong(1) == 0, "Temporary demo data remains.");
            }
        }
    }

    private static void printWatch(Watch watch) {
        System.out.println("Watch " + watch.getId() + ": " + watch.getName() + " | " + watch.getPrice()
                + " | " + watch.getClass().getSimpleName() + " | " + watch.getWarrantyInfo());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
