package fraud.detection.ui.chart;

import fraud.detection.ui.UITheme;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.util.List;

/** Donut chart of parts of a whole (e.g. fraud cases by review status). */
public class DonutChartPanel extends ChartPanel<List<ChartSlice>> {

    public DonutChartPanel(String title) {
        super(title);
    }

    @Override
    protected boolean isEmpty() {
        return total() == 0;
    }

    private double total() {
        double t = 0;
        for (ChartSlice s : data) {
            t += s.getValue();
        }
        return t;
    }

    @Override
    protected void drawChart(Graphics2D g2, Rectangle a) {
        double total = total();
        int size = Math.min(a.height - 4, (int) (a.width * 0.44));
        int x = a.x + 6;
        int y = a.y + (a.height - size) / 2;
        float thick = size / 4.5f;
        g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        double start = 90;
        double inset = thick / 2.0;
        for (ChartSlice s : data) {
            double extent = -360 * s.getValue() / total;
            if (s.getValue() > 0) {
                g2.setColor(s.getColor());
                g2.draw(new Arc2D.Double(x + inset, y + inset, size - 2 * inset, size - 2 * inset, start, extent, Arc2D.OPEN));
            }
            start += extent;
        }
        g2.setStroke(new BasicStroke(1));
        g2.setColor(UITheme.TEXT);
        g2.setFont(UITheme.font(Font.BOLD, 20));
        String t = compact(total);
        g2.drawString(t, x + (size - g2.getFontMetrics().stringWidth(t)) / 2, y + size / 2 + 4);
        g2.setFont(UITheme.font(Font.PLAIN, 10));
        g2.setColor(UITheme.MUTED);
        g2.drawString("cases", x + (size - g2.getFontMetrics().stringWidth("cases")) / 2, y + size / 2 + 18);

        int lx = x + size + 16;
        int ly = a.y + Math.max(14, (a.height - data.size() * 34) / 2);
        for (ChartSlice s : data) {
            g2.setColor(s.getColor());
            g2.fillRoundRect(lx, ly - 10, 12, 12, 3, 3);
            g2.setColor(UITheme.TEXT);
            g2.setFont(UITheme.font(Font.BOLD, 12));
            g2.drawString(compact(s.getValue()) + String.format("  (%.0f%%)", 100 * s.getValue() / total), lx + 18, ly);
            g2.setColor(UITheme.MUTED);
            g2.setFont(UITheme.font(Font.PLAIN, 11));
            g2.drawString(s.getLabel(), lx + 18, ly + 14);
            ly += 34;
        }
    }
}
