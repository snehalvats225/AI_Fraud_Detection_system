package fraud.detection.service;

import fraud.detection.exception.FraudSystemException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * GENERIC FUNCTIONAL INTERFACE: "some database work that returns a T".
 * Like Callable<T>, but it receives the Connection and may throw our checked exceptions.
 */
@FunctionalInterface
public interface TransactionWork<T> {

    T execute(Connection con) throws FraudSystemException, SQLException;
}
