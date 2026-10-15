package fraud.detection.model;

/**
 * What the system does with a transaction after scoring it.
 *   APPROVED - risk below the alert threshold, money moves normally
 *   FLAGGED  - suspicious: money moves, but the user is alerted and a case is opened
 *   BLOCKED  - very risky: the transaction is declined, user alerted, case opened
 */
public enum Decision {
    APPROVED("Approved"),
    FLAGGED("Flagged"),
    BLOCKED("Blocked");

    private final String label;

    Decision(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
