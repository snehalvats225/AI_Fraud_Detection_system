package fraud.detection.ml;

import java.util.List;

/**
 * The 9 numbers ("features") that describe every transaction for the detectors.
 * FeatureExtractor fills them from a real transaction; SyntheticDataGenerator
 * creates realistic examples for the first training run.
 */
public final class Features {

    public static final int AMOUNT_ZSCORE = 0;     // how unusual the amount is for THIS user
    public static final int LOG_AMOUNT = 1;        // ln(1 + amount)
    public static final int BALANCE_RATIO = 2;     // amount / balance (capped at 2)
    public static final int NIGHT_TIME = 3;        // 1 if in the night window
    public static final int NEW_CITY = 4;          // 1 if the user never used this city before
    public static final int NEW_DEVICE = 5;        // 1 if the user never used this device before
    public static final int VELOCITY = 6;          // transactions by this user in the last hour
    public static final int MERCHANT_RISK = 7;     // 0 (grocery) ... 0.9 (crypto)
    public static final int INTERNATIONAL = 8;     // 1 for a foreign location

    public static final int COUNT = 9;

    public static final List<String> NAMES = List.of(
            "Amount vs. user's average", "Transaction size", "Share of balance used", "Night-time transaction",
            "New city for this user", "New device for this user", "Transactions in last hour",
            "Merchant category risk", "International location");

    private Features() {
    }

    /** Compact text form stored in the database, e.g. "1.2,7.8,0.03,0,...". */
    public static String toCsv(double[] x) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Math.round(x[i] * 10000) / 10000.0);
        }
        return sb.toString();
    }

    public static double[] fromCsv(String csv) {
        String[] parts = csv.split(",");
        double[] x = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            x[i] = Double.parseDouble(parts[i].trim());
        }
        return x;
    }
}
