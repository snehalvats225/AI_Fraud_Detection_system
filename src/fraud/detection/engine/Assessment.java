package fraud.detection.engine;

import fraud.detection.model.Decision;

/** The final verdict on one transaction. */
public final class Assessment {

    private final double[] features;
    private final EnsembleResult result;
    private final Decision decision;
    private final Integer modelVersion;

    public Assessment(double[] features, EnsembleResult result, Decision decision, Integer modelVersion) {
        this.features = features.clone();
        this.result = result;
        this.decision = decision;
        this.modelVersion = modelVersion;
    }

    public double[] getFeatures()     { return features.clone(); }
    public EnsembleResult getResult() { return result; }
    public Decision getDecision()     { return decision; }
    public Integer getModelVersion()  { return modelVersion; }

    public double getRiskScore()  { return result.getScore(); }
    public double getRuleScore()  { return result.componentScore(RuleBasedDetector.NAME); }
    public double getStatScore()  { return result.componentScore(StatisticalDetector.NAME); }
    public double getMlScore()    { return result.componentScore(MachineLearningDetector.NAME); }

    /** All reasons on separate lines (stored with the fraud case). */
    public String reasonsText() {
        return result.getReasons().isEmpty() ? "No specific risk indicators" : String.join("\n", result.getReasons());
    }
}
