package fraud.detection.service;

import fraud.detection.ml.ModelMetrics;
import fraud.detection.ml.ModelVersion;

/** Result of a retraining run, including the previous model's metrics for comparison. */
public final class TrainingOutcome {

    private final ModelVersion newVersion;
    private final ModelMetrics previousMetrics;   // null on the very first training
    private final int syntheticSamples;
    private final int realSamples;
    private final long millis;

    public TrainingOutcome(ModelVersion newVersion, ModelMetrics previousMetrics, int syntheticSamples,
                           int realSamples, long millis) {
        this.newVersion = newVersion;
        this.previousMetrics = previousMetrics;
        this.syntheticSamples = syntheticSamples;
        this.realSamples = realSamples;
        this.millis = millis;
    }

    public ModelVersion getNewVersion()      { return newVersion; }
    public ModelMetrics getPreviousMetrics() { return previousMetrics; }
    public int getSyntheticSamples()         { return syntheticSamples; }
    public int getRealSamples()              { return realSamples; }
    public long getMillis()                  { return millis; }
}
