package fraud.detection.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * COMPOSITE detector: runs several detectors and combines their scores with a
 * weighted average (weights come from the admin's configuration).
 * It implements the same FraudDetector interface, so to the rest of the program
 * it looks like just another detector (POLYMORPHISM).
 *
 * MULTITHREADING: the detectors run IN PARALLEL with CompletableFuture on a
 * thread pool; join() waits for all of them.
 */
public class EnsembleDetector implements FraudDetector {

    public static final String NAME = "Weighted Ensemble";

    private final Map<FraudDetector, Double> weighted;     // LinkedHashMap keeps the order
    private final ExecutorService pool;

    public EnsembleDetector(Map<FraudDetector, Double> weighted, ExecutorService pool) {
        this.weighted = new LinkedHashMap<>(weighted);
        this.pool = pool;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Weighted average of all detectors";
    }

    @Override
    public EnsembleResult evaluate(TransactionContext ctx) {
        Map<FraudDetector, CompletableFuture<DetectionResult>> futures = new LinkedHashMap<>();
        for (FraudDetector d : weighted.keySet()) {
            futures.put(d, CompletableFuture.supplyAsync(() -> d.evaluate(ctx), pool));
        }
        double weightedSum = 0;
        double weightTotal = 0;
        List<String> reasons = new ArrayList<>();
        Map<String, DetectionResult> components = new LinkedHashMap<>();
        for (Map.Entry<FraudDetector, CompletableFuture<DetectionResult>> e : futures.entrySet()) {
            DetectionResult r = e.getValue().join();
            double w = weighted.get(e.getKey());
            components.put(r.getDetectorName(), r);
            if (w > 0) {
                weightedSum += w * r.getScore();
                weightTotal += w;
                reasons.addAll(r.getReasons());
            }
        }
        double score = weightTotal == 0 ? 0 : weightedSum / weightTotal;
        return new EnsembleResult(NAME, score, reasons, components);
    }
}
