package com.mycompany.watch_store.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import com.mycompany.watch_store.config.JdbcTransactions;
import com.mycompany.watch_store.dao.WatchDAO;
import com.mycompany.watch_store.entity.AutomaticWatch;
import com.mycompany.watch_store.entity.SmartWatch;
import com.mycompany.watch_store.entity.Watch;

public final class WatchService {
    private final WatchDAO watchDAO;

    public WatchService() {
        this(new WatchDAO());
    }

    public WatchService(WatchDAO watchDAO) {
        if (watchDAO == null) {
            throw new IllegalArgumentException("WatchDAO must not be null.");
        }
        this.watchDAO = watchDAO;
    }

    public List<Watch> getAvailableWatches() throws SQLException {
        return watchDAO.findAll().stream().filter(Watch::isAvailable).toList();
    }

    /** Customer-facing lookup: missing and unavailable watches are both treated as not found. */
    public Watch getWatchById(int id) throws SQLException {
        requirePositiveId(id);
        Watch watch = watchDAO.findById(id);
        if (watch == null || !watch.isAvailable()) {
            throw new NoSuchElementException("Available watch not found.");
        }
        return watch;
    }

    /** Null or blank keyword means all available watches. */
    public List<Watch> searchWatches(String keyword) throws SQLException {
        return search(keyword, null);
    }

    /** maxPrice is an inclusive, nonnegative budget; decimal budgets are allowed. */
    public List<Watch> searchWatches(String keyword, BigDecimal maxPrice) throws SQLException {
        if (maxPrice == null || maxPrice.signum() < 0) {
            throw new IllegalArgumentException("Maximum price must not be null or negative.");
        }
        return search(keyword, maxPrice);
    }

    /** Internal management operation; a future HTTP boundary must authorize the administrator. */
    public int createWatch(Watch watch) throws SQLException {
        validateWatch(watch);
        if (watch.getId() != 0) {
            throw new IllegalArgumentException("A new watch must have ID 0.");
        }
        return watchDAO.insert(watch);
    }

    /** Internal management operation; does not reset createdAt. */
    public void updateWatch(Watch watch) throws SQLException {
        validateWatch(watch);
        requirePositiveId(watch.getId());
        watchDAO.update(watch);
    }

    /** Owns one HTTP PATCH transaction; locks before merging so concurrent stock is preserved. */
    public Watch patchWatch(int id, UnaryOperator<Watch> patch) throws SQLException {
        requirePositiveId(id);
        if (patch == null) {
            throw new IllegalArgumentException("Patch must not be null.");
        }
        return JdbcTransactions.execute(null, connection -> {
            WatchDAO watches = new WatchDAO(connection);
            Watch current = watches.findByIdForUpdate(id);
            if (current == null) {
                throw new NoSuchElementException("Watch does not exist.");
            }
            String originalType = current.getWatchType();
            var originalCreatedAt = current.getCreatedAt();
            Watch updated = patch.apply(current);
            validateWatch(updated);
            if (updated.getId() != id || !updated.getWatchType().equals(originalType)
                    || !updated.getCreatedAt().equals(originalCreatedAt)) {
                throw new IllegalArgumentException("Patch must preserve watch identity and subtype.");
            }
            new WatchService(watches).updateWatch(updated);
            return updated;
        });
    }

    /** Internal management operation; preserves historical records through soft delete. */
    public void discontinueWatch(int id) throws SQLException {
        requirePositiveId(id);
        watchDAO.delete(id);
    }

    private List<Watch> search(String keyword, BigDecimal maxPrice) throws SQLException {
        String normalized = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        return getAvailableWatches().stream()
                .filter(watch -> watch.getName().toLowerCase(Locale.ROOT).contains(normalized)
                        || watch.getBrand().toLowerCase(Locale.ROOT).contains(normalized))
                .filter(watch -> maxPrice == null || watch.getPrice().compareTo(maxPrice) <= 0)
                .toList();
    }

    private static void validateWatch(Watch watch) {
        if (!(watch instanceof AutomaticWatch) && !(watch instanceof SmartWatch)) {
            throw new IllegalArgumentException("A supported, non-null watch is required.");
        }
        // Constructors and controlled setters already enforce price, stock and required text.
        if (watch.getCategoryId() <= 0 || watch.getPrice() == null || watch.getPrice().signum() < 0
                || watch.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Watch data is invalid.");
        }
    }

    private static void requirePositiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Watch ID must be greater than 0.");
        }
    }
}
