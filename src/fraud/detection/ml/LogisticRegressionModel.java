package fraud.detection.ml;

import fraud.detection.exception.ModelTrainingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * LOGISTIC REGRESSION - the machine-learning model, written in plain Java.
 *
 *   z = bias + w1*x1 + w2*x2 + ... + w9*x9        (x = standardised features)
 *   probability of fraud = 1 / (1 + e^-z)           (the "sigmoid" function)
 *
 * TRAINING (gradient descent): start with all weights 0, then repeat for many
 * "epochs": measure how wrong the predictions are (the gradient of the log-loss)
 * and move every weight a small step (the learning rate) in the direction that
 * reduces the error. L2 regularisation keeps weights small to avoid over-fitting.
 *
 * MULTITHREADING: the samples are split into chunks and each chunk's gradient is
 * computed on a different thread (ExecutorService + Callable + Future). The partial
 * gradients are then added together.
 *
 * Fraud is rare, so each class is given a weight that balances fraud vs genuine;
 * otherwise the model would learn "always say genuine" and still look 90% accurate.
 */
public class LogisticRegressionModel implements ProbabilisticClassifier, Trainable<LabeledSample> {

    private double[] weights;
    private double bias;
    private double[] means;
    private double[] stds;
    private double finalLoss = Double.NaN;

    public LogisticRegressionModel(double[] weights, double bias, double[] means, double[] stds) {
        this.weights = weights.clone();
        this.bias = bias;
        this.means = means.clone();
        this.stds = stds.clone();
    }

    /** A fresh model that has not learned anything yet. */
    public static LogisticRegressionModel untrained() {
        double[] ones = new double[Features.COUNT];
        java.util.Arrays.fill(ones, 1.0);
        return new LogisticRegressionModel(new double[Features.COUNT], 0, new double[Features.COUNT], ones);
    }

    // ------------------------------------------------------------------ prediction

    @Override
    public double predictProbability(double[] x) {
        double z = bias;
        for (int i = 0; i < weights.length; i++) {
            z += weights[i] * standardise(x[i], i);
        }
        return sigmoid(z);
    }

    /** How much each feature pushed the score up (+) or down (-). Used to EXPLAIN a decision. */
    public double[] contributions(double[] x) {
        double[] c = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            c[i] = weights[i] * standardise(x[i], i);
        }
        return c;
    }

    private double standardise(double value, int i) {
        return (value - means[i]) / stds[i];
    }

    static double sigmoid(double z) {
        if (z < -35) {
            return 0;
        }
        if (z > 35) {
            return 1;
        }
        return 1.0 / (1.0 + Math.exp(-z));
    }

    // ------------------------------------------------------------------ training

    @Override
    public void train(List<LabeledSample> samples, TrainingParams p, TrainingListener listener)
            throws ModelTrainingException {
        int n = samples.size();
        int positives = 0;
        for (LabeledSample s : samples) {
            positives += s.getLabel();
        }
        if (n < 20 || positives == 0 || positives == n) {
            throw new ModelTrainingException("Training needs at least 20 samples containing both fraud and genuine examples.");
        }

        computeStandardisation(samples);
        int f = Features.COUNT;
        double[][] x = new double[n][f];
        double[] y = new double[n];
        double[] w = new double[n];
        double posWeight = 0.5 * n / positives;              // class balancing
        double negWeight = 0.5 * n / (n - positives);
        double totalWeight = 0;
        for (int r = 0; r < n; r++) {
            LabeledSample s = samples.get(r);
            double[] raw = s.raw();
            for (int i = 0; i < f; i++) {
                x[r][i] = standardise(raw[i], i);
            }
            y[r] = s.getLabel();
            w[r] = s.getWeight() * (s.getLabel() == 1 ? posWeight : negWeight);
            totalWeight += w[r];
        }

        weights = new double[f];
        bias = 0;
        int threads = Math.max(1, Math.min(p.getThreads(), n / 10));
        int chunk = (n + threads - 1) / threads;
        ExecutorService pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "ml-trainer");
            t.setDaemon(true);
            return t;
        });
        try {
            for (int epoch = 1; epoch <= p.getEpochs(); epoch++) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ModelTrainingException("Training was cancelled.");
                }
                final double[] wSnapshot = weights.clone();
                final double bSnapshot = bias;
                List<Callable<double[]>> tasks = new ArrayList<>();
                for (int start = 0; start < n; start += chunk) {
                    final int from = start;
                    final int to = Math.min(n, start + chunk);
                    tasks.add(() -> partialGradient(x, y, w, from, to, wSnapshot, bSnapshot));
                }
                double[] grad = new double[f + 2];               // [weights..., bias, loss]
                for (Future<double[]> fut : pool.invokeAll(tasks)) {
                    double[] part = fut.get();
                    for (int i = 0; i < grad.length; i++) {
                        grad[i] += part[i];
                    }
                }
                for (int i = 0; i < f; i++) {
                    weights[i] -= p.getLearningRate() * (grad[i] / totalWeight + p.getL2Lambda() * weights[i]);
                }
                bias -= p.getLearningRate() * grad[f] / totalWeight;
                finalLoss = grad[f + 1] / totalWeight;
                if (listener != null && (epoch % 10 == 0 || epoch == p.getEpochs())) {
                    listener.onProgress(epoch, p.getEpochs(), finalLoss);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelTrainingException("Training was interrupted.", e);
        } catch (ExecutionException e) {
            throw new ModelTrainingException("Training failed: " + e.getCause(), e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    /** Runs on a worker thread: gradient + loss for rows [from, to). Reads shared arrays, never writes them. */
    private static double[] partialGradient(double[][] x, double[] y, double[] w, int from, int to,
                                            double[] weights, double bias) {
        int f = weights.length;
        double[] g = new double[f + 2];
        for (int r = from; r < to; r++) {
            double z = bias;
            for (int i = 0; i < f; i++) {
                z += weights[i] * x[r][i];
            }
            double p = sigmoid(z);
            double err = (p - y[r]) * w[r];
            for (int i = 0; i < f; i++) {
                g[i] += err * x[r][i];
            }
            g[f] += err;
            double pc = Math.min(1 - 1e-12, Math.max(1e-12, p));
            g[f + 1] -= w[r] * (y[r] * Math.log(pc) + (1 - y[r]) * Math.log(1 - pc));
        }
        return g;
    }

    private void computeStandardisation(List<LabeledSample> samples) {
        int f = Features.COUNT;
        means = new double[f];
        stds = new double[f];
        for (LabeledSample s : samples) {
            double[] raw = s.raw();
            for (int i = 0; i < f; i++) {
                means[i] += raw[i];
            }
        }
        for (int i = 0; i < f; i++) {
            means[i] /= samples.size();
        }
        for (LabeledSample s : samples) {
            double[] raw = s.raw();
            for (int i = 0; i < f; i++) {
                stds[i] += (raw[i] - means[i]) * (raw[i] - means[i]);
            }
        }
        for (int i = 0; i < f; i++) {
            stds[i] = Math.sqrt(stds[i] / samples.size());
            if (stds[i] < 1e-9) {
                stds[i] = 1;          // a constant feature - avoid dividing by zero
            }
        }
    }

    public double[] getWeights() { return weights.clone(); }
    public double getBias()      { return bias; }
    public double[] getMeans()   { return means.clone(); }
    public double[] getStds()    { return stds.clone(); }
    public double getFinalLoss() { return finalLoss; }
}
