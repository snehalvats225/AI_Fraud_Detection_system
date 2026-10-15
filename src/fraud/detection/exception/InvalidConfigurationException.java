package fraud.detection.exception;

/** The admin entered detection settings that do not make sense (e.g. alert threshold above block threshold). */
public class InvalidConfigurationException extends FraudSystemException {

    public InvalidConfigurationException(String message) {
        super(message);
    }
}
