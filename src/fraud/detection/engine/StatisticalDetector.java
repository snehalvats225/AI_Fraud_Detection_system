package fraud.detection.engine;

import fraud.detection.ml.Features;
import java.util.List;

/**
 * ALGORITHM 2 - statistical anomaly detection.
 * It compares the transaction with the customer's OWN history using the z-score:
 *     z = (amount - average) / standard deviation
 * z = 0 is a typical amount; z = 4 means "4 standard deviations bigger than usual",
 * which is very rare for genuine behaviour.
 */
public class StatisticalDetector extends AbstractDetector {

    public static final String NAME = "Statistical Anomaly";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Z-score of the amount vs the customer's own history, unusual activity bursts";
    }

    @Override
    protected double computeScore(TransactionContext ctx, List<String> reasons) {
        UserProfile profile = ctx.getProfile();
        double z = ctx.feature(Features.AMOUNT_ZSCORE);
        double velocity = ctx.feature(Features.VELOCITY);
        double ratio = ctx.feature(Features.BALANCE_RATIO);

        double amountScore = clamp((z - 1.5) / 4.5);
        double burstScore = clamp((velocity - 1) / (ctx.getConfig().getVelocityLimit() * 1.5));
        double drainScore = clamp((ratio - 0.3) / 0.7);

        if (z >= 2) {
            reasons.add(String.format("Amount is %.1f standard deviations above %s average of %s", z,
                    profile.hasHistory() ? "this customer's" : "the population", rupees(profile.effectiveMean())));
        }
        if (velocity >= 3) {
            reasons.add(String.format("Burst of activity: %.0f transactions within one hour", velocity));
        }
        if (!profile.hasHistory()) {
            reasons.add("Limited history (" + profile.getHistoryCount() + " past transactions) - compared with all customers");
        }
        return 0.6 * amountScore + 0.2 * burstScore + 0.2 * drainScore;
    }
}
