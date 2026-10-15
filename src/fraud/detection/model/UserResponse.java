package fraud.detection.model;

/** How the customer answered an alert. Used as feedback for the ML model. */
public enum UserResponse {
    NONE("No response yet"),
    CONFIRMED_GENUINE("It was me"),
    REPORTED_FRAUD("Reported as fraud");

    private final String label;

    UserResponse(String label) {
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
