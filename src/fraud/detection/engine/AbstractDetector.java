package fraud.detection.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * ABSTRACT base for the detectors (TEMPLATE METHOD pattern).
 * evaluate() does the common work - collect reasons, keep the score between 0 and 1,
 * build the result - and calls computeScore(), which each child class implements
 * in its own way.
 */
public abstract class AbstractDetector implements FraudDetector {

    @Override
    public final DetectionResult evaluate(TransactionContext context) {
        List<String> reasons = new ArrayList<>();
        double score = computeScore(context, reasons);
        return new DetectionResult(getName(), clamp(score), reasons);
    }

    /** Child classes calculate the raw score and add human-readable reasons to the list. */
    protected abstract double computeScore(TransactionContext context, List<String> reasons);

    protected static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    protected static String rupees(double amount) {
        return "Rs. " + String.format("%,.0f", amount);
    }
}
