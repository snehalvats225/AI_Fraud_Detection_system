package fraud.detection.dao;

import fraud.detection.exception.DatabaseException;
import java.util.List;
import java.util.Optional;

/**
 * GENERIC INTERFACE shared by the DAO classes (DAO = Data Access Object).
 *   T = object type (Transaction, FraudCase, Customer, ModelVersion)
 *   K = primary-key type (Long, Integer)
 *
 * count() and update() are DEFAULT methods. TransactionDAO keeps the default
 * update(), which refuses: a financial transaction must never be edited.
 */
public interface GenericDAO<T, K> {

    Optional<T> findById(K id) throws DatabaseException;

    List<T> findAll() throws DatabaseException;

    default int count() throws DatabaseException {
        return findAll().size();
    }

    default boolean update(T entity) throws DatabaseException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not allow updates.");
    }
}
