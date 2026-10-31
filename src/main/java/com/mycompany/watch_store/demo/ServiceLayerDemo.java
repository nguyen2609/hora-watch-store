package com.mycompany.watch_store.demo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.mycompany.watch_store.config.DatabaseConnection;
import com.mycompany.watch_store.dao.UserDAO;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.User;
import com.mycompany.watch_store.entity.Watch;
import com.mycompany.watch_store.service.AuthService;
import com.mycompany.watch_store.service.PasswordHasher;
import com.mycompany.watch_store.service.WatchService;

public final class ServiceLayerDemo {
    private ServiceLayerDemo() {
    }

    public static void main(String[] args) {
        System.out.println("=== SERVICE LAYER DEMO ===");
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connection established successfully");
            demonstrateServices(connection);
        } catch (SQLException | RuntimeException exception) {
            // No raw SQL errors, User objects, passwords, hashes or connection details are logged.
            System.err.println("Service demonstration failed. Check local configuration and schema compatibility. "
                    + "No demo transaction is committed.");
            System.err.println("Failure type: " + exception.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void demonstrateServices(Connection connection) throws SQLException {
        String marker = "m07-" + UUID.randomUUID();
        String email = marker + "@example.invalid";
        String autoModel = marker + "-auto";
        String smartModel = marker + "-smart";
        // Only transient random demo passwords; no credentials are stored in source code.
        String password = UUID.randomUUID().toString();
        UserDAO userDAO = new UserDAO(connection);
        WatchDAO watchDAO = new WatchDAO(connection);
        AuthService auth = new AuthService(userDAO);
        WatchService watches = new WatchService(watchDAO);
        int automaticId;
        int smartId;
        int userId;
        connection.setAutoCommit(false);
        try {
            List<Watch> existing = watches.getAvailableWatches();
            System.out.println("Existing available watches: " + existing.size());
            for (Watch watch : existing) {
                System.out.println("Available: " + watch.getName() + " | " + watch.getClass().getSimpleName());
            }

            int categoryId = chooseCategory(connection, marker);
            Watch automatic = new AutomaticWatch(categoryId, marker + " automatic", "M07 Demo Brand", autoModel,
                    new BigDecimal("6500000"), 2, null, null, true, "Self-winding");
            Watch smart = new SmartWatch(categoryId, marker + " smart", "M07 Demo Brand", smartModel,
                    new BigDecimal("4500000"), 1, null, null, true, 48);
            automaticId = watches.createWatch(automatic);
            smartId = watches.createWatch(smart);
            List<Watch> keywordResults = watches.searchWatches(marker.toUpperCase(Locale.ROOT));
            require(keywordResults.size() == 2, "Case-insensitive keyword search failed.");
            System.out.println("Keyword search, case-insensitive: " + keywordResults.size());
            require(watches.searchWatches("m07 demo brand").stream().anyMatch(watch -> watch.getId() == automaticId),
                    "Brand search failed.");
            System.out.println("Case-insensitive brand search verified.");
            List<Watch> budgetResults = watches.searchWatches(marker, new BigDecimal("4500000.00"));
            require(budgetResults.size() == 1 && budgetResults.get(0).getId() == smartId, "Budget search failed.");
            System.out.println("Keyword + maxPrice 4500000.00: " + budgetResults.size());
            require(watches.searchWatches(null).size() == watches.searchWatches("   ").size()
                    && watches.searchWatches(null).size() == watches.getAvailableWatches().size(),
                    "Empty keyword policy is inconsistent.");

            LocalDateTime createdAt = automatic.getCreatedAt();
            automatic.setPrice(new BigDecimal("6600000"));
            watches.updateWatch(automatic);
            Watch updated = watches.getWatchById(automaticId);
            require(updated instanceof AutomaticWatch && updated.getPrice().compareTo(new BigDecimal("6600000")) == 0
                    && createdAt.equals(updated.getCreatedAt()), "Service update failed.");
            System.out.println("Watch creation/update verified; subtype and created_at preserved.");
            watches.discontinueWatch(smartId);
            require(!watchDAO.findById(smartId).isAvailable()
                    && watches.getAvailableWatches().stream().noneMatch(watch -> watch.getId() == smartId)
                    && watches.searchWatches(marker).size() == 1
                    && watches.searchWatches(marker, new BigDecimal("4500000.00")).isEmpty(),
                    "Unavailable watch was exposed in customer results.");
            expect("Discontinued watch lookup", NoSuchElementException.class, () -> watches.getWatchById(smartId));
            System.out.println("Unavailable watches excluded from listing and both search overloads.");

            User registered = auth.register("M07 Temporary Customer", "  " + email.toUpperCase(Locale.ROOT) + "  ",
                    password, null);
            userId = registered.getId();
            User stored = userDAO.findById(userId);
            require(stored != null && email.equals(stored.getEmail()) && stored.hasRole("CUSTOMER")
                    && registered.getCreatedAt().equals(stored.getCreatedAt()), "Registration mapping failed.");
            require(!password.equals(stored.getPasswordHash()) && stored.getPasswordHash().startsWith("$argon2id$")
                    && PasswordHasher.verify(password, stored.getPasswordHash()), "Stored password is not a valid hash.");
            System.out.println("Temporary registration: CUSTOMER; normalized email and database ID verified.");
            System.out.println("Database stores an Argon2id hash; no password or hash is printed.");
            String secondHash = PasswordHasher.hash(password);
            require(!secondHash.equals(stored.getPasswordHash()) && PasswordHasher.verify(password, secondHash),
                    "Random salt verification failed.");
            System.out.println("Unique salts verified for the same password.");

            require(auth.login(" " + email.toUpperCase(Locale.ROOT) + " ", password).getId() == userId,
                    "Valid login failed.");
            System.out.println("Correct-password login verified (internal user only; no session created).");
            SecurityException wrong = expect("Incorrect password", SecurityException.class,
                    () -> auth.login(email, UUID.randomUUID().toString()));
            SecurityException unknown = expect("Unknown account", SecurityException.class,
                    () -> auth.login("missing-" + email, password));
            require(wrong.getMessage().equals(unknown.getMessage()), "Login messages disclose account existence.");
            expect("Duplicate registration", IllegalArgumentException.class,
                    () -> auth.register("M07 Duplicate", email.toUpperCase(Locale.ROOT), password, null));

            // Bypass the service pre-check to exercise the database's unique constraint safely.
            try {
                userDAO.insert(new User("M07 Duplicate", email, stored.getPasswordHash(), null));
                throw new IllegalStateException("Database duplicate email guard did not reject the insert.");
            } catch (SQLIntegrityConstraintViolationException exception) {
                require("23505".equals(exception.getSQLState()), "Unexpected duplicate email SQL state.");
                System.out.println("Database duplicate email guard verified: SQLState 23505.");
            }

            expect("Invalid registration email", IllegalArgumentException.class,
                    () -> auth.register("M07 Customer", marker, password, null));
            expect("Short password", IllegalArgumentException.class,
                    () -> auth.register("M07 Customer", "short-" + email, password.substring(0, 8), null));
            expect("Oversized password", IllegalArgumentException.class, () -> PasswordHasher.hash(password.repeat(4)));
            require(!PasswordHasher.verify(password, "$argon2id$invalid"), "Malformed hash was accepted.");
            expect("Negative maximum price", IllegalArgumentException.class,
                    () -> watches.searchWatches(marker, new BigDecimal("-1")));
            expect("Null maximum price", IllegalArgumentException.class, () -> watches.searchWatches(marker, null));
            expect("Invalid watch ID", IllegalArgumentException.class, () -> watches.getWatchById(0));
            expect("Null watch", IllegalArgumentException.class, () -> watches.createWatch(null));
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
            System.out.println("Demo transaction rolled back.");
        }

        require(new UserDAO().findById(userId) == null && new UserDAO().findByEmail(email) == null,
                "Temporary customer remains after rollback.");
        require(new WatchDAO().findById(automaticId) == null && new WatchDAO().findById(smartId) == null,
                "Temporary watches remain after rollback.");
        verifyCleanup(connection, marker, email, autoModel, smartModel);
        System.out.println("Cleanup verified through independent connections: no temporary users or watches remain.");
        System.out.println("No temporary category remains. Identity sequences may have gaps after rollback.");
        System.out.println("Service layer demo finished normally.");
    }

    private static int chooseCategory(Connection connection, String marker) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM app.categories ORDER BY id LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                return result.getInt(1);
            }
        }
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

    private static void verifyCleanup(Connection connection, String marker, String email, String autoModel,
                                      String smartModel) throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM app.users WHERE email IN (?, ?))
                     + (SELECT COUNT(*) FROM app.watches WHERE model_number IN (?, ?))
                     + (SELECT COUNT(*) FROM app.categories WHERE name = ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, "short-" + email);
            statement.setString(3, autoModel);
            statement.setString(4, smartModel);
            statement.setString(5, marker);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next() && result.getLong(1) == 0, "Temporary demo data remains.");
            }
        }
    }

    private static <T extends RuntimeException> T expect(String label, Class<T> type, SqlAction action)
            throws SQLException {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (!type.isInstance(exception)) {
                throw exception;
            }
            System.out.println("Expected rejection [" + label + "]: " + exception.getMessage());
            return type.cast(exception);
        }
        throw new IllegalStateException("Expected validation did not reject: " + label);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
