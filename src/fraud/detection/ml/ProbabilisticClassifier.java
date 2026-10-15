package fraud.detection.ml;

/** INTERFACE: anything that can turn feature values into a fraud probability (0..1). */
public interface ProbabilisticClassifier {

    double predictProbability(double[] features);

    /** DEFAULT method: a yes/no answer using a cut-off. */
    default boolean predict(double[] features, double threshold) {
        return predictProbability(features) >= threshold;
    }
}
