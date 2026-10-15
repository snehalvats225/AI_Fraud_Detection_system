package fraud.detection.exception;

import java.math.BigDecimal;

/** The account does not have enough money. Stores the numbers so the UI can show them. */
public class InsufficientBalanceException extends FraudSystemException {

    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientBalanceException(BigDecimal requested, BigDecimal available) {
        super("Insufficient balance. You tried to pay Rs. " + requested.toPlainString()
                + " but only Rs. " + available.toPlainString() + " is available.");
        this.requested = requested;
        this.available = available;
    }

    public BigDecimal getRequested() { return requested; }
    public BigDecimal getAvailable() { return available; }
}
