package fraud.detection.ml;

/** Settings for one training run. */
public final class TrainingParams {

    private final double learningRate;
    private final int epochs;
    private final double l2Lambda;
    private final int threads;

    public TrainingParams(double learningRate, int epochs, double l2Lambda, int threads) {
        this.learningRate = learningRate;
        this.epochs = epochs;
        this.l2Lambda = l2Lambda;
        this.threads = threads;
    }

    public double getLearningRate() { return learningRate; }
    public int getEpochs()          { return epochs; }
    public double getL2Lambda()     { return l2Lambda; }
    public int getThreads()         { return threads; }
}
