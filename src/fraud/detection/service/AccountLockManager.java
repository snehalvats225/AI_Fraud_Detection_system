package fraud.detection.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ONE lock object per customer. Two payments by the SAME customer wait for each
 * other; payments by different customers run in parallel.
 * ConcurrentHashMap is a thread-safe Map; computeIfAbsent() guarantees one lock per customer.
 */
public final class AccountLockManager {

    private static final ConcurrentMap<Integer, Object> LOCKS = new ConcurrentHashMap<>();

    private AccountLockManager() {
    }

    public static Object lockFor(int userId) {
        return LOCKS.computeIfAbsent(userId, id -> new Object());
    }
}
