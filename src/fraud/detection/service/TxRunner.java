package fraud.detection.service;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.DatabaseException;
import fraud.detection.exception.FraudSystemException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs work inside ONE database transaction (GENERIC METHOD, <T> = result type).
 * Everything is saved with commit() - or, if anything fails, undone with rollback().
 */
public final class TxRunner {

    private TxRunner() {
    }

    public static <T> T inTransaction(TransactionWork<T> work) throws FraudSystemException {
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            con.setAutoCommit(false);
            T result = work.execute(con);
            con.commit();
            return result;
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);
            throw new DatabaseException("The operation failed and was cancelled: " + e.getMessage(), e);
        } catch (FraudSystemException e) {
            DBConnection.rollbackQuietly(con);
            throw e;
        } finally {
            DBConnection.closeQuietly(con);
        }
    }
}
