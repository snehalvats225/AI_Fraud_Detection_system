package fraud.detection.ui.chart;

import fraud.detection.ui.UITheme;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * STACKED bar chart (e.g. flagged + blocked cases per day).
 * barColor() and drawOverlay() are "hooks" that a child class may override -
 * HistogramPanel does exactly that.
 */
public class BarChartPanel extends ChartPanel<CategoryData> {

    public BarChartPanel(String title) {
        super(title);
    }

    @Override
    protected boolean isEmpty() {
        for (ChartSeries s : data.getSeries()) {
            for (int i = 0; i < s.size(); i++) {
                if (s.value(i) > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void drawChart(Graphics2D g2, Rectangle a) {
        int legendH = data.getSeries().size() > 1 ? 18 : 0;
        int left = a.x + 30;
        int bottom = a.y + a.height - 20 - legendH;
        int top = a.y + 6;
        int width = a.x + a.width - left;
        int n = data.getLabels().size();

        double max = 1;
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (ChartSeries s : data.getSeries()) {
                sum += s.value(i);
            }
            max = Math.max(max, sum);
        }
        max = niceMax(max);

        // grid lines + y-axis numbers
        g2.setFont(UITheme.font(Font.PLAIN, 10));
        for (int k = 0; k <= 4; k++) {
            int y = bottom - (int) ((bottom - top) * k / 4.0);
            g2.setColor(UITheme.GRID);
            g2.drawLine(left, y, left + width, y);
            g2.setColor(UITheme.MUTED);
            String txt = compact(max * k / 4);
            g2.drawString(txt, left - 6 - g2.getFontMetrics().stringWidth(txt), y + 4);
        }

        double slot = (double) width / n;
        int barW = (int) Math.max(4, slot * 0.62);
        for (int i = 0; i < n; i++) {
            int x = left + (int) (slot * i + (slot - barW) / 2);
            int yCursor = bottom;
            for (int s = 0; s < data.getSeries().size(); s++) {
                double v = data.getSeries().get(s).value(i);
                int h = (int) Math.round((bottom - top) * v / max);
                if (h > 0) {
                    g2.setColor(barColor(s, i));
                    g2.fillRoundRect(x, yCursor - h, barW, h, 4, 4);
                    yCursor -= h;
                }
            }
            g2.setColor(UITheme.MUTED);
            g2.setFont(UITheme.font(Font.PLAIN, 10));
            String label = data.getLabels().get(i);
            g2.drawString(label, x + (barW - g2.getFontMetrics().stringWidth(label)) / 2, bottom + 14);
        }
        drawOverlay(g2, left, top, width, bottom, slot);

        if (legendH > 0) {
            int lx = left;
            for (ChartSeries s : data.getSeries()) {
                lx = legend(g2, lx, a.y + a.height - 2, s.getColor(), s.getName());
            }
        }
    }

    /** Colour of one bar segment. Overridden by HistogramPanel. */
    protected Color barColor(int seriesIndex, int categoryIndex) {
        return data.getSeries().get(seriesIndex).getColor();
    }

    /** Extra drawing on top of the bars. Empty here, overridden by HistogramPanel. */
    protected void drawOverlay(Graphics2D g2, int left, int top, int width, int bottom, double slot) {
    }

    private static double niceMax(double v) {
        double[] steps = {4, 8, 12, 20, 40, 60, 100, 200, 400, 600, 1000, 2000, 4000, 10000};
        for (double s : steps) {
            if (v <= s) {
                return s;
            }
        }
        return Math.ceil(v / 1000) * 1000;
    }
}
