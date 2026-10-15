package fraud.detection.exception;

/**
 * Parent of every custom exception in this project (a CHECKED exception).
 * The UI can catch every expected problem with one catch (FraudSystemException e).
 */
public class FraudSystemException extends Exception {

    public FraudSystemException(String message) {
        super(message);
    }

    public FraudSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
