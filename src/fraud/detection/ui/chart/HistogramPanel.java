package fraud.detection.ui.chart;

import fraud.detection.ui.UITheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Distribution of risk scores in 10 buckets (0-10%, 10-20% ...).
 * It EXTENDS BarChartPanel and only OVERRIDES two hooks: bars are coloured by
 * risk level, and dashed lines mark the alert and block thresholds.
 */
public class HistogramPanel extends BarChartPanel {

    private double alertThreshold = 0.45;
    private double blockThreshold = 0.75;

    public HistogramPanel(String title) {
        super(title);
    }

    public void setThresholds(double alert, double block) {
        this.alertThreshold = alert;
        this.blockThreshold = block;
        repaint();
    }

    @Override
    protected Color barColor(int seriesIndex, int bucket) {
        double mid = (bucket + 0.5) / 10.0;
        return UITheme.riskColor(mid, alertThreshold, blockThreshold);
    }

    @Override
    protected void drawOverlay(Graphics2D g2, int left, int top, int width, int bottom, double slot) {
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] {5, 4}, 0));
        g2.setFont(UITheme.font(Font.BOLD, 10));
        line(g2, left, top, width, bottom, alertThreshold, UITheme.WARNING, "alert");
        line(g2, left, top, width, bottom, blockThreshold, UITheme.DANGER, "block");
        g2.setStroke(new BasicStroke(1));
    }

    private static void line(Graphics2D g2, int left, int top, int width, int bottom, double t, Color c, String text) {
        int x = left + (int) (width * t);
        g2.setColor(c);
        g2.drawLine(x, top, x, bottom);
        g2.drawString(text, x + 3, top + 10);
    }
}
