package fraud.detection.ui.chart;

import fraud.detection.ui.UITheme;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Line chart with values between 0 and 1 (model metrics across versions). */
public class LineChartPanel extends ChartPanel<CategoryData> {

    private final double minY;

    public LineChartPanel(String title, double minY) {
        super(title);
        this.minY = minY;
    }

    @Override
    protected boolean isEmpty() {
        return data.getLabels().isEmpty();
    }

    @Override
    protected void drawChart(Graphics2D g2, Rectangle a) {
        int left = a.x + 34;
        int top = a.y + 6;
        int bottom = a.y + a.height - 38;
        int width = a.x + a.width - left - 10;
        int n = data.getLabels().size();
        g2.setFont(UITheme.font(Font.PLAIN, 10));
        for (int k = 0; k <= 4; k++) {
            double v = minY + (1 - minY) * k / 4;
            int y = y(v, top, bottom);
            g2.setColor(UITheme.GRID);
            g2.drawLine(left, y, left + width, y);
            g2.setColor(UITheme.MUTED);
            String txt = String.format("%.2f", v);
            g2.drawString(txt, left - 6 - g2.getFontMetrics().stringWidth(txt), y + 4);
        }
        for (int i = 0; i < n; i++) {
            String label = data.getLabels().get(i);
            g2.setColor(UITheme.MUTED);
            g2.drawString(label, xFor(i, n, left, width) - g2.getFontMetrics().stringWidth(label) / 2, bottom + 14);
        }
        for (ChartSeries s : data.getSeries()) {
            g2.setColor(s.getColor());
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < n; i++) {
                int x = xFor(i, n, left, width);
                int y = y(s.value(i), top, bottom);
                if (i > 0) {
                    g2.drawLine(xFor(i - 1, n, left, width), y(s.value(i - 1), top, bottom), x, y);
                }
                g2.fillOval(x - 3, y - 3, 7, 7);
            }
        }
        g2.setStroke(new BasicStroke(1));
        int lx = left;
        for (ChartSeries s : data.getSeries()) {
            lx = legend(g2, lx, a.y + a.height - 4, s.getColor(), s.getName());
        }
    }

    private int y(double v, int top, int bottom) {
        double clamped = Math.max(minY, Math.min(1, v));
        return bottom - (int) ((bottom - top) * (clamped - minY) / (1 - minY));
    }

    private static int xFor(int i, int n, int left, int width) {
        return n == 1 ? left + width / 2 : left + 12 + (int) ((width - 24) * i / (double) (n - 1));
    }
}
