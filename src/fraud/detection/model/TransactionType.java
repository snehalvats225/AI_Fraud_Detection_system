package fraud.detection.model;

/** Kinds of financial transactions a customer can make (REFUND is created by the system). */
public enum TransactionType {
    UPI_PAYMENT("UPI Payment"),
    CARD_PAYMENT("Card Payment"),
    ONLINE_PURCHASE("Online Purchase"),
    ATM_WITHDRAWAL("ATM Withdrawal"),
    BANK_TRANSFER("Bank Transfer"),
    REFUND("Fraud Refund");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Types a customer may choose in the form (REFUND is system-only). */
    public static TransactionType[] customerTypes() {
        return new TransactionType[] {UPI_PAYMENT, CARD_PAYMENT, ONLINE_PURCHASE, ATM_WITHDRAWAL, BANK_TRANSFER};
    }

    @Override
    public String toString() {
        return label;
    }
}
