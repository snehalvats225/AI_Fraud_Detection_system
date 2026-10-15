package fraud.detection.ui;

import fraud.detection.service.ConfigManager;

/**
 * A risk score shown in tables as "62%" and coloured by the renderer.
 * It implements Comparable<RiskValue> (a GENERIC interface), so clicking the
 * column header sorts by the real number, not by the text.
 */
public final class RiskValue implements Comparable<RiskValue> {

    private final double value;

    public RiskValue(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(RiskValue other) {
        return Double.compare(value, other.value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RiskValue && ((RiskValue) o).value == value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public String toString() {
        return String.format("%.0f%%", value * 100);
    }

    java.awt.Color color() {
        return UITheme.riskColor(value, ConfigManager.get().getAlertThreshold(), ConfigManager.get().getBlockThreshold());
    }
}
