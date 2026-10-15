package fraud.detection.ml;

import java.time.LocalDateTime;

/** One saved version of the ML model (a row of the model_versions table). */
public final class ModelVersion {

    private final int versionId;
    private final LogisticRegressionModel model;
    private final ModelMetrics metrics;
    private final int trainingSamples;
    private final int realSamples;
    private final double learningRate;
    private final int epochs;
    private final double l2Lambda;
    private final boolean active;
    private final String notes;
    private final String trainedBy;
    private final LocalDateTime trainedAt;

    public ModelVersion(int versionId, LogisticRegressionModel model, ModelMetrics metrics, int trainingSamples,
                        int realSamples, double learningRate, int epochs, double l2Lambda, boolean active,
                        String notes, String trainedBy, LocalDateTime trainedAt) {
        this.versionId = versionId;
        this.model = model;
        this.metrics = metrics;
        this.trainingSamples = trainingSamples;
        this.realSamples = realSamples;
        this.learningRate = learningRate;
        this.epochs = epochs;
        this.l2Lambda = l2Lambda;
        this.active = active;
        this.notes = notes;
        this.trainedBy = trainedBy;
        this.trainedAt = trainedAt;
    }

    public int getVersionId()                { return versionId; }
    public LogisticRegressionModel getModel() { return model; }
    public ModelMetrics getMetrics()         { return metrics; }
    public int getTrainingSamples()          { return trainingSamples; }
    public int getRealSamples()              { return realSamples; }
    public double getLearningRate()          { return learningRate; }
    public int getEpochs()                   { return epochs; }
    public double getL2Lambda()              { return l2Lambda; }
    public boolean isActive()                { return active; }
    public String getNotes()                 { return notes; }
    public String getTrainedBy()             { return trainedBy; }
    public LocalDateTime getTrainedAt()      { return trainedAt; }

    public String getLabel() {
        return "v" + versionId;
    }
}
