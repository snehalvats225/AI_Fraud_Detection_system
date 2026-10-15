package fraud.detection.model;

/** The two kinds of users described in the project brief. */
public enum Role {
    ADMIN("Admin"),
    CUSTOMER("Customer");

    private final String label;

    Role(String label) {
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
