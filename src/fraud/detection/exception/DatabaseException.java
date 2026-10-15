package fraud.detection.exception;

/** A database operation failed. The original SQLException is kept as the cause. */
public class DatabaseException extends FraudSystemException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
