package fraud.detection.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * All admin-controlled settings of the detection system (IMMUTABLE).
 * To change a setting, a new object is created with the BUILDER pattern:
 *   DetectionConfig c = current.toBuilder().alertThreshold(0.4).build();
 * Because the object never changes, many threads can read it safely at the same time.
 */
public final class DetectionConfig {

    private final double alertThreshold;      // risk >= this  -> FLAGGED
    private final double blockThreshold;      // risk >= this  -> BLOCKED
    private final BigDecimal highAmountLimit; // rule: "high-value transaction"
    private final int nightStart;             // rule: night hours [start, end)
    private final int nightEnd;
    private final int velocityLimit;          // rule: max transactions in one hour
    private final double ruleWeight;          // ensemble weights
    private final double statWeight;
    private final double mlWeight;
    private final double learningRate;        // ML training parameters
    private final int epochs;
    private final double l2Lambda;
    private final String updatedBy;
    private final LocalDateTime updatedAt;

    private DetectionConfig(Builder b) {
        alertThreshold = b.alertThreshold;
        blockThreshold = b.blockThreshold;
        highAmountLimit = b.highAmountLimit;
        nightStart = b.nightStart;
        nightEnd = b.nightEnd;
        velocityLimit = b.velocityLimit;
        ruleWeight = b.ruleWeight;
        statWeight = b.statWeight;
        mlWeight = b.mlWeight;
        learningRate = b.learningRate;
        epochs = b.epochs;
        l2Lambda = b.l2Lambda;
        updatedBy = b.updatedBy;
        updatedAt = b.updatedAt;
    }

    public static DetectionConfig defaults() {
        return new Builder().build();
    }

    public Builder toBuilder() {
        return new Builder()
                .alertThreshold(alertThreshold).blockThreshold(blockThreshold).highAmountLimit(highAmountLimit)
                .nightStart(nightStart).nightEnd(nightEnd).velocityLimit(velocityLimit)
                .ruleWeight(ruleWeight).statWeight(statWeight).mlWeight(mlWeight)
                .learningRate(learningRate).epochs(epochs).l2Lambda(l2Lambda)
                .updatedBy(updatedBy).updatedAt(updatedAt);
    }

    /** True if the hour falls inside the configured night window (the window may cross midnight). */
    public boolean isNightHour(int hour) {
        if (nightStart == nightEnd) {
            return false;
        }
        return nightStart < nightEnd
                ? hour >= nightStart && hour < nightEnd
                : hour >= nightStart || hour < nightEnd;
    }

    public double getAlertThreshold()      { return alertThreshold; }
    public double getBlockThreshold()      { return blockThreshold; }
    public BigDecimal getHighAmountLimit() { return highAmountLimit; }
    public int getNightStart()             { return nightStart; }
    public int getNightEnd()               { return nightEnd; }
    public int getVelocityLimit()          { return velocityLimit; }
    public double getRuleWeight()          { return ruleWeight; }
    public double getStatWeight()          { return statWeight; }
    public double getMlWeight()            { return mlWeight; }
    public double getLearningRate()        { return learningRate; }
    public int getEpochs()                 { return epochs; }
    public double getL2Lambda()            { return l2Lambda; }
    public String getUpdatedBy()           { return updatedBy; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }

    /** BUILDER: set only the values you want; the rest keep their defaults. */
    public static final class Builder {
        private double alertThreshold = 0.45;
        private double blockThreshold = 0.75;
        private BigDecimal highAmountLimit = new BigDecimal("50000.00");
        private int nightStart = 0;
        private int nightEnd = 5;
        private int velocityLimit = 5;
        private double ruleWeight = 0.30;
        private double statWeight = 0.25;
        private double mlWeight = 0.45;
        private double learningRate = 0.30;
        private int epochs = 400;
        private double l2Lambda = 0.001;
        private String updatedBy = "system";
        private LocalDateTime updatedAt;

        public Builder alertThreshold(double v)      { alertThreshold = v; return this; }
        public Builder blockThreshold(double v)      { blockThreshold = v; return this; }
        public Builder highAmountLimit(BigDecimal v) { highAmountLimit = v; return this; }
        public Builder nightStart(int v)             { nightStart = v; return this; }
        public Builder nightEnd(int v)               { nightEnd = v; return this; }
        public Builder velocityLimit(int v)          { velocityLimit = v; return this; }
        public Builder ruleWeight(double v)          { ruleWeight = v; return this; }
        public Builder statWeight(double v)          { statWeight = v; return this; }
        public Builder mlWeight(double v)            { mlWeight = v; return this; }
        public Builder learningRate(double v)        { learningRate = v; return this; }
        public Builder epochs(int v)                 { epochs = v; return this; }
        public Builder l2Lambda(double v)            { l2Lambda = v; return this; }
        public Builder updatedBy(String v)           { updatedBy = v; return this; }
        public Builder updatedAt(LocalDateTime v)    { updatedAt = v; return this; }

        public DetectionConfig build() {
            return new DetectionConfig(this);
        }
    }
}
