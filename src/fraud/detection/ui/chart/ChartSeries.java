package fraud.detection.ui.chart;

import java.awt.Color;

/** One named, coloured row of values (e.g. "Blocked" per day). */
public final class ChartSeries {

    private final String name;
    private final Color color;
    private final double[] values;

    public ChartSeries(String name, Color color, double[] values) {
        this.name = name;
        this.color = color;
        this.values = values.clone();
    }

    public String getName()   { return name; }
    public Color getColor()   { return color; }
    public double value(int i) { return values[i]; }
    public int size()         { return values.length; }
}
