package fraud.detection.model;

public enum DeviceType {
    MOBILE_APP("Mobile App"),
    UPI_APP("UPI App"),
    NET_BANKING("Net Banking"),
    POS_TERMINAL("Card Machine (POS)"),
    ATM("ATM");

    private final String label;

    DeviceType(String label) {
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
