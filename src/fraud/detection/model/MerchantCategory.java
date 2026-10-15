package fraud.detection.model;

/**
 * Merchant categories. Each one carries a RISK value (0 = safe, 1 = very risky)
 * that is used as a feature by the detectors. Gift cards, gambling and crypto are
 * where stolen money is most often spent, so they carry the highest risk.
 */
public enum MerchantCategory {
    GROCERY("Grocery", 0.05),
    UTILITIES("Utilities & Bills", 0.05),
    FOOD_DINING("Food & Dining", 0.10),
    FUEL("Fuel", 0.10),
    SHOPPING("Shopping", 0.20),
    TRAVEL("Travel", 0.30),
    ELECTRONICS("Electronics", 0.40),
    MONEY_TRANSFER("Money Transfer", 0.50),
    GIFT_CARDS("Gift Cards", 0.80),
    GAMBLING("Gambling", 0.90),
    CRYPTO("Crypto Exchange", 0.90);

    private final String label;
    private final double risk;

    MerchantCategory(String label, double risk) {
        this.label = label;
        this.risk = risk;
    }

    public String getLabel() {
        return label;
    }

    public double getRisk() {
        return risk;
    }

    public boolean isHighRisk() {
        return risk >= 0.5;
    }

    @Override
    public String toString() {
        return label;
    }
}
