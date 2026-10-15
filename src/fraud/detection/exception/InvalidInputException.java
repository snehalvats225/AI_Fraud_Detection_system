package fraud.detection.exception;

/** The user typed something invalid in a form. */
public class InvalidInputException extends FraudSystemException {

    public InvalidInputException(String message) {
        super(message);
    }
}
