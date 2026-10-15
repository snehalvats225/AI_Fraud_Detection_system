package fraud.detection.exception;

/** Wrong username, password or role, or a blocked account. */
public class AuthenticationException extends FraudSystemException {

    public AuthenticationException(String message) {
        super(message);
    }
}
