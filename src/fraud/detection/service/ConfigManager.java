package fraud.detection.service;

import fraud.detection.dao.ConfigDAO;
import fraud.detection.exception.DatabaseException;
import fraud.detection.exception.InvalidConfigurationException;
import fraud.detection.model.Admin;
import fraud.detection.model.DetectionConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keeps the CURRENT detection configuration in memory.
 *
 * SYNCHRONIZATION with a ReentrantReadWriteLock:
 *   - READ lock: every scored transaction reads the config; many threads may
 *     hold the read lock at the same time, so scoring is never slowed down.
 *   - WRITE lock: an admin saving new settings gets exclusive access. Saving to the
 *     database and swapping the in-memory copy happen together, so memory and
 *     database can never disagree, even if two admins save at the same moment.
 */
public final class ConfigManager {

    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static DetectionConfig current = DetectionConfig.defaults();

    private ConfigManager() {
    }

    public static DetectionConfig get() {
        LOCK.readLock().lock();
        try {
            return current;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    /** Loads the saved settings at start-up. */
    public static void load() throws DatabaseException {
        DetectionConfig loaded = new ConfigDAO().load();
        LOCK.writeLock().lock();
        try {
            current = loaded;
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    /** Validates, saves and activates new settings. Returns the saved configuration. */
    public static DetectionConfig update(DetectionConfig requested, Admin admin)
            throws InvalidConfigurationException, DatabaseException {
        validate(requested);
        DetectionConfig stamped = requested.toBuilder()
                .updatedBy(admin.getFullName())
                .updatedAt(LocalDateTime.now().withNano(0))
                .build();
        LOCK.writeLock().lock();
        try {
            new ConfigDAO().save(stamped);
            current = stamped;
            return stamped;
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void validate(DetectionConfig c) throws InvalidConfigurationException {
        if (c.getAlertThreshold() < 0.05 || c.getAlertThreshold() > 0.95) {
            throw new InvalidConfigurationException("Alert threshold must be between 0.05 and 0.95.");
        }
        if (c.getBlockThreshold() <= c.getAlertThreshold() || c.getBlockThreshold() > 1.0) {
            throw new InvalidConfigurationException("Block threshold must be greater than the alert threshold and at most 1.0.");
        }
        if (c.getHighAmountLimit().compareTo(new BigDecimal("1000")) < 0) {
            throw new InvalidConfigurationException("High-amount limit must be at least Rs. 1,000.");
        }
        if (c.getNightStart() < 0 || c.getNightStart() > 23 || c.getNightEnd() < 0 || c.getNightEnd() > 23) {
            throw new InvalidConfigurationException("Night hours must be between 0 and 23.");
        }
        if (c.getVelocityLimit() < 1 || c.getVelocityLimit() > 50) {
            throw new InvalidConfigurationException("Velocity limit must be between 1 and 50 transactions per hour.");
        }
        if (c.getRuleWeight() < 0 || c.getStatWeight() < 0 || c.getMlWeight() < 0
                || c.getRuleWeight() + c.getStatWeight() + c.getMlWeight() <= 0) {
            throw new InvalidConfigurationException("Algorithm weights cannot be negative and at least one must be above 0.");
        }
        if (c.getLearningRate() < 0.001 || c.getLearningRate() > 2) {
            throw new InvalidConfigurationException("Learning rate must be between 0.001 and 2.");
        }
        if (c.getEpochs() < 10 || c.getEpochs() > 5000) {
            throw new InvalidConfigurationException("Epochs must be between 10 and 5000.");
        }
        if (c.getL2Lambda() < 0 || c.getL2Lambda() > 1) {
            throw new InvalidConfigurationException("L2 regularisation must be between 0 and 1.");
        }
    }
}
