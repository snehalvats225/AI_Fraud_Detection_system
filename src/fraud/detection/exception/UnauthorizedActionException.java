package fraud.detection.exception;

/** The user tried something their role or ownership does not allow. */
public class UnauthorizedActionException extends FraudSystemException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
