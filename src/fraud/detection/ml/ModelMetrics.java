package fraud.detection.ml;

import java.util.List;

/**
 * How good a model is, measured on TEST data it never saw while training.
 *
 *   TP = fraud correctly caught          FP = genuine wrongly flagged
 *   TN = genuine correctly passed        FN = fraud that slipped through
 *
 *   Precision = TP / (TP + FP)   "when we flag, how often are we right?"
 *   Recall    = TP / (TP + FN)   "how much of the fraud do we catch?"
 *   F1        = harmonic mean of precision and recall
 */
public final class ModelMetrics {

    private final int tp;
    private final int fp;
    private final int tn;
    private final int fn;

    public ModelMetrics(int tp, int fp, int tn, int fn) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
    }

    public static ModelMetrics evaluate(ProbabilisticClassifier model, List<LabeledSample> test, double threshold) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (LabeledSample s : test) {
            boolean predicted = model.predict(s.raw(), threshold);
            boolean actual = s.getLabel() == 1;
            if (predicted && actual) {
                tp++;
            } else if (predicted) {
                fp++;
            } else if (actual) {
                fn++;
            } else {
                tn++;
            }
        }
        return new ModelMetrics(tp, fp, tn, fn);
    }

    public int getTp() { return tp; }
    public int getFp() { return fp; }
    public int getTn() { return tn; }
    public int getFn() { return fn; }

    public double getAccuracy() {
        int total = tp + fp + tn + fn;
        return total == 0 ? 0 : (double) (tp + tn) / total;
    }

    public double getPrecision() {
        return tp + fp == 0 ? 0 : (double) tp / (tp + fp);
    }

    public double getRecall() {
        return tp + fn == 0 ? 0 : (double) tp / (tp + fn);
    }

    public double getF1() {
        double p = getPrecision();
        double r = getRecall();
        return p + r == 0 ? 0 : 2 * p * r / (p + r);
    }

    @Override
    public String toString() {
        return String.format("accuracy=%.3f precision=%.3f recall=%.3f f1=%.3f (TP=%d FP=%d TN=%d FN=%d)",
                getAccuracy(), getPrecision(), getRecall(), getF1(), tp, fp, tn, fn);
    }
}
