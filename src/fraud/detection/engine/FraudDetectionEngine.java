package fraud.detection.engine;

import fraud.detection.ml.ModelRegistry;
import fraud.detection.ml.ModelVersion;
import fraud.detection.model.Decision;
import fraud.detection.model.DetectionConfig;
import fraud.detection.model.TransactionRequest;
import fraud.detection.service.ConfigManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scores a transaction with all three algorithms and decides APPROVED / FLAGGED / BLOCKED.
 * It reads the CURRENT configuration and the CURRENT model on every call, so admin
 * changes and newly trained models take effect immediately.
 */
public final class FraudDetectionEngine {

    /** Shared pool for running detectors in parallel (daemon threads do not keep the app alive). */
    private static final ExecutorService DETECTOR_POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "detector-pool");
        t.setDaemon(true);
        return t;
    });

    private FraudDetectionEngine() {
    }

    public static Assessment assess(TransactionRequest request, UserProfile profile) {
        long start = System.nanoTime();
        DetectionConfig config = ConfigManager.get();
        ModelVersion model = ModelRegistry.getActive();

        double[] features = FeatureExtractor.extract(request, profile, config);
        TransactionContext ctx = new TransactionContext(request, profile, config, features);

        Map<FraudDetector, Double> detectors = new LinkedHashMap<>();
        detectors.put(new RuleBasedDetector(), config.getRuleWeight());
        detectors.put(new StatisticalDetector(), config.getStatWeight());
        detectors.put(new MachineLearningDetector(model), config.getMlWeight());
        EnsembleResult result = new EnsembleDetector(detectors, DETECTOR_POOL).evaluate(ctx);

        Decision decision = decide(result.getScore(), config);
        PerformanceMonitor.record(decision, System.nanoTime() - start);
        return new Assessment(features, result, decision, model == null ? null : model.getVersionId());
    }

    public static Decision decide(double risk, DetectionConfig config) {
        if (risk >= config.getBlockThreshold()) {
            return Decision.BLOCKED;
        }
        if (risk >= config.getAlertThreshold()) {
            return Decision.FLAGGED;
        }
        return Decision.APPROVED;
    }
}
