package fraud.detection.service;

import fraud.detection.dao.ModelVersionDAO;
import fraud.detection.dao.TransactionDAO;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.exception.ModelTrainingException;
import fraud.detection.ml.LabeledSample;
import fraud.detection.ml.LogisticRegressionModel;
import fraud.detection.ml.ModelMetrics;
import fraud.detection.ml.ModelRegistry;
import fraud.detection.ml.ModelVersion;
import fraud.detection.ml.SyntheticDataGenerator;
import fraud.detection.ml.TrainingListener;
import fraud.detection.ml.TrainingParams;
import fraud.detection.model.DetectionConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ALGORITHM UPDATES: retrains the ML model using performance data and feedback.
 *
 * Training data = synthetic baseline + REAL labelled transactions (admin case
 * reviews and customers' "it was me" / "not me" answers, weighted x3).
 * 20% of the data is kept aside as a TEST set to measure the new model honestly.
 *
 * SYNCHRONIZATION: a ReentrantLock with tryLock() makes sure only ONE training
 * runs at a time; a second request gets a clear error instead of waiting.
 */
public class ModelTrainingService {

    public static final int SYNTHETIC_SAMPLES = 3000;
    public static final double SYNTHETIC_FRAUD_RATIO = 0.12;
    public static final double TEST_SHARE = 0.2;
    private static final int THREADS = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
    private static final ReentrantLock TRAINING_LOCK = new ReentrantLock();

    private final ModelVersionDAO versionDAO = new ModelVersionDAO();
    private final TransactionDAO txnDAO = new TransactionDAO();

    /** Called at start-up: load the active model, or train version 1 if none exists yet. */
    public ModelVersion loadOrBootstrap() throws FraudSystemException {
        ModelVersion active = versionDAO.findActive().orElse(null);
        if (active == null) {
            active = train("system", "Initial model trained on synthetic data", null).getNewVersion();
        }
        ModelRegistry.setActive(active);
        return active;
    }

    public TrainingOutcome retrain(String trainedBy, TrainingListener listener) throws FraudSystemException {
        return train(trainedBy, null, listener);
    }

    private TrainingOutcome train(String trainedBy, String notes, TrainingListener listener) throws FraudSystemException {
        if (!TRAINING_LOCK.tryLock()) {
            throw new ModelTrainingException("A training run is already in progress. Please wait for it to finish.");
        }
        try {
            long t0 = System.currentTimeMillis();
            DetectionConfig cfg = ConfigManager.get();
            List<LabeledSample> synthetic = SyntheticDataGenerator.generate(
                    SYNTHETIC_SAMPLES, SYNTHETIC_FRAUD_RATIO, SyntheticDataGenerator.DEFAULT_SEED);
            List<LabeledSample> real = txnDAO.loadLabelledSamples();

            List<LabeledSample> trainSet = new ArrayList<>();
            List<LabeledSample> testSet = new ArrayList<>();
            split(synthetic, trainSet, testSet, new Random(7));
            split(real, trainSet, testSet, new Random(11));

            LogisticRegressionModel model = LogisticRegressionModel.untrained();
            model.train(trainSet, new TrainingParams(cfg.getLearningRate(), cfg.getEpochs(), cfg.getL2Lambda(), THREADS),
                    listener);
            ModelMetrics metrics = ModelMetrics.evaluate(model, testSet, 0.5);

            ModelVersion previous = ModelRegistry.getActive();
            String text = notes != null ? notes : String.format("Retrained with %d real feedback samples, final loss %.4f",
                    real.size(), model.getFinalLoss());
            int id = versionDAO.insertAndActivate(model, metrics, trainSet.size() + testSet.size(), real.size(),
                    cfg.getLearningRate(), cfg.getEpochs(), cfg.getL2Lambda(), text, trainedBy);
            ModelVersion saved = versionDAO.findById(id).orElseThrow();
            ModelRegistry.setActive(saved);                    // atomic switch - next transaction uses it
            return new TrainingOutcome(saved, previous == null ? null : previous.getMetrics(), synthetic.size(),
                    real.size(), System.currentTimeMillis() - t0);
        } finally {
            TRAINING_LOCK.unlock();
        }
    }

    /** Rollback / switch to an older version. */
    public ModelVersion activate(int versionId) throws FraudSystemException {
        ModelVersion v = versionDAO.findById(versionId)
                .orElseThrow(() -> new ModelTrainingException("Model version " + versionId + " not found."));
        versionDAO.activate(versionId);
        ModelVersion fresh = versionDAO.findById(versionId).orElse(v);
        ModelRegistry.setActive(fresh);
        return fresh;
    }

    /**
     * Stratified split: fraud and genuine samples are split separately, so the test
     * set has the same fraud share as the training set.
     */
    private static void split(List<LabeledSample> data, List<LabeledSample> train, List<LabeledSample> test, Random rnd) {
        List<LabeledSample> fraud = new ArrayList<>();
        List<LabeledSample> genuine = new ArrayList<>();
        for (LabeledSample s : data) {
            (s.getLabel() == 1 ? fraud : genuine).add(s);
        }
        for (List<LabeledSample> group : List.of(fraud, genuine)) {
            Collections.shuffle(group, rnd);
            int testCount = (int) Math.round(group.size() * TEST_SHARE);
            test.addAll(group.subList(0, testCount));
            train.addAll(group.subList(testCount, group.size()));
        }
    }
}
