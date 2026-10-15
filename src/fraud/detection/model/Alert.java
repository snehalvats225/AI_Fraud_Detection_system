package fraud.detection.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A notification shown on the customer's dashboard. */
public final class Alert {

    private final int alertId;
    private final long txnId;
    private final AlertSeverity severity;
    private final String message;
    private final boolean read;
    private final UserResponse response;
    private final LocalDateTime createdAt;
    private final BigDecimal amount;
    private final String payee;
    private final Decision decision;

    public Alert(int alertId, long txnId, AlertSeverity severity, String message, boolean read,
                 UserResponse response, LocalDateTime createdAt, BigDecimal amount, String payee, Decision decision) {
        this.alertId = alertId;
        this.txnId = txnId;
        this.severity = severity;
        this.message = message;
        this.read = read;
        this.response = response;
        this.createdAt = createdAt;
        this.amount = amount;
        this.payee = payee;
        this.decision = decision;
    }

    public int getAlertId()             { return alertId; }
    public long getTxnId()              { return txnId; }
    public AlertSeverity getSeverity()  { return severity; }
    public String getMessage()          { return message; }
    public boolean isRead()             { return read; }
    public UserResponse getResponse()   { return response; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getAmount()       { return amount; }
    public String getPayee()            { return payee; }
    public Decision getDecision()       { return decision; }

    /** Only alerts about a suspicious transaction can be answered (not INFO messages). */
    public boolean canRespond() {
        return severity != AlertSeverity.INFO && response == UserResponse.NONE;
    }
}
