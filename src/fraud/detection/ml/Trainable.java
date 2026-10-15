package fraud.detection.ml;

import fraud.detection.exception.ModelTrainingException;
import java.util.List;

/**
 * GENERIC INTERFACE for anything that learns from examples.
 * S is the sample type - LogisticRegressionModel implements Trainable<LabeledSample>.
 */
public interface Trainable<S> {

    void train(List<S> samples, TrainingParams params, TrainingListener listener) throws ModelTrainingException;
}
