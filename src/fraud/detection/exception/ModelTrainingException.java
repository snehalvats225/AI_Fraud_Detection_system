package fraud.detection.exception;

/** Training the ML model failed (too little data, interrupted, already running ...). */
public class ModelTrainingException extends FraudSystemException {

    public ModelTrainingException(String message) {
        super(message);
    }

    public ModelTrainingException(String message, Throwable cause) {
        super(message, cause);
    }
}
