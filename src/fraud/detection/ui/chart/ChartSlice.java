package fraud.detection.ui.chart;

import java.awt.Color;

/** One part of a donut chart. */
public final class ChartSlice {

    private final String label;
    private final double value;
    private final Color color;

    public ChartSlice(String label, double value, Color color) {
        this.label = label;
        this.value = value;
        this.color = color;
    }

    public String getLabel() { return label; }
    public double getValue() { return value; }
    public Color getColor()  { return color; }
}
