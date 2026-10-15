package fraud.detection.ml;

/** Callback so the UI can show training progress (a functional interface - usable as a lambda). */
@FunctionalInterface
public interface TrainingListener {

    void onProgress(int epoch, int totalEpochs, double loss);
}
