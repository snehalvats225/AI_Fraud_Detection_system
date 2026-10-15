package fraud.detection.engine;

import fraud.detection.ml.Features;
import fraud.detection.model.DetectionConfig;
import fraud.detection.model.TransactionRequest;
import java.util.List;

/**
 * ALGORITHM 1 - fixed business rules written by fraud experts.
 * Every rule that fires adds points. The limits (high amount, night hours,
 * velocity) come from the admin's configuration.
 */
public class RuleBasedDetector extends AbstractDetector {

    public static final String NAME = "Rule Engine";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Expert rules: high amount, night time, new city/device, velocity, risky merchants";
    }

    @Override
    protected double computeScore(TransactionContext ctx, List<String> reasons) {
        DetectionConfig cfg = ctx.getConfig();
        TransactionRequest req = ctx.getRequest();
        double amount = req.getAmount().doubleValue();
        double score = 0;

        if (amount > cfg.getHighAmountLimit().doubleValue()) {
            score += 0.35;
            reasons.add("Amount " + rupees(amount) + " is above the high-value limit of "
                    + rupees(cfg.getHighAmountLimit().doubleValue()));
        }
        double ratio = ctx.feature(Features.BALANCE_RATIO);
        if (ratio >= 0.8) {
            score += 0.20;
            reasons.add(String.format("Uses %.0f%% of the available balance", Math.min(100, ratio * 100)));
        }
        if (ctx.feature(Features.NIGHT_TIME) == 1) {
            score += 0.15;
            reasons.add(String.format("Made at %02d:%02d, inside the night window (%02d:00-%02d:00)",
                    req.getTime().getHour(), req.getTime().getMinute(), cfg.getNightStart(), cfg.getNightEnd()));
        }
        if (ctx.feature(Features.INTERNATIONAL) == 1) {
            score += 0.25;
            reasons.add("International location: " + req.getCity());
        } else if (ctx.feature(Features.NEW_CITY) == 1) {
            score += 0.15;
            reasons.add("First transaction from " + req.getCity() + " (home city: " + ctx.getProfile().getHomeCity() + ")");
        }
        if (ctx.feature(Features.NEW_DEVICE) == 1) {
            score += 0.10;
            reasons.add("Device never used before by this customer: " + req.getDevice());
        }
        int velocity = (int) ctx.feature(Features.VELOCITY);
        if (velocity >= cfg.getVelocityLimit()) {
            score += 0.25;
            reasons.add(velocity + " transactions in the last hour (limit " + cfg.getVelocityLimit() + ")");
        }
        if (req.getCategory().isHighRisk()) {
            score += 0.20;
            reasons.add("High-risk merchant category: " + req.getCategory());
        }
        return score;
    }
}
