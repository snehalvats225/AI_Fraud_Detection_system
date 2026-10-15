package fraud.detection.ml;

/** One training example: feature values + the correct answer (1 = fraud, 0 = genuine). */
public final class LabeledSample {

    private final double[] features;
    private final int label;
    private final double weight;       // real feedback can count more than synthetic data
    private final boolean real;

    public LabeledSample(double[] features, int label, double weight, boolean real) {
        this.features = features.clone();
        this.label = label;
        this.weight = weight;
        this.real = real;
    }

    public double[] getFeatures() { return features.clone(); }
    double[] raw()                { return features; }           // package-private, no copy (fast path)
    public int getLabel()         { return label; }
    public double getWeight()     { return weight; }
    public boolean isReal()       { return real; }
}
