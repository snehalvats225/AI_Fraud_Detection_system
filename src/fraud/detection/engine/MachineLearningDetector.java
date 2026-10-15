package fraud.detection.engine;

import fraud.detection.ml.Features;
import fraud.detection.ml.ModelVersion;
import java.util.ArrayList;
import java.util.List;

/**
 * ALGORITHM 3 - the trained machine-learning model (logistic regression).
 * The score is the model's fraud probability. To EXPLAIN it, the features that
 * pushed the probability up the most are listed as reasons.
 */
public class MachineLearningDetector extends AbstractDetector {

    public static final String NAME = "ML Model";

    private final ModelVersion version;

    public MachineLearningDetector(ModelVersion version) {
        this.version = version;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Logistic regression trained on labelled fraud / genuine transactions";
    }

    @Override
    protected double computeScore(TransactionContext ctx, List<String> reasons) {
        if (version == null) {
            reasons.add("ML model not loaded - score skipped");
            return 0;
        }
        double[] x = ctx.getFeatures();
        double p = version.getModel().predictProbability(x);
        double[] contrib = version.getModel().contributions(x);

        // pick the 3 strongest positive signals
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < contrib.length; i++) {
            order.add(i);
        }
        order.sort((a, b) -> Double.compare(contrib[b], contrib[a]));
        StringBuilder top = new StringBuilder();
        for (int k = 0; k < 3; k++) {
            int i = order.get(k);
            if (contrib[i] < 0.3) {
                break;
            }
            if (top.length() > 0) {
                top.append(", ");
            }
            top.append(Features.NAMES.get(i)).append(String.format(" (+%.1f)", contrib[i]));
        }
        if (p >= 0.3) {
            reasons.add(String.format("ML model %s: fraud probability %.0f%%%s", version.getLabel(), p * 100,
                    top.length() > 0 ? " - strongest signals: " + top : ""));
        }
        return p;
    }

    public ModelVersion getVersion() {
        return version;
    }
}
