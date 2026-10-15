package fraud.detection.model;

/** Review status of a fraud case. The admin's decision becomes a training label. */
public enum CaseStatus {
    OPEN("Open"),
    CONFIRMED_FRAUD("Confirmed Fraud"),
    FALSE_POSITIVE("False Positive");

    private final String label;

    CaseStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
