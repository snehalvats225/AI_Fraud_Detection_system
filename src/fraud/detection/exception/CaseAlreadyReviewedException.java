package fraud.detection.exception;

/** Somebody already reviewed this fraud case. */
public class CaseAlreadyReviewedException extends FraudSystemException {

    public CaseAlreadyReviewedException(String message) {
        super(message);
    }
}
