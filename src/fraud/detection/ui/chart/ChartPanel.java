package fraud.detection.ui.chart;

import fraud.detection.ui.UITheme;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * ABSTRACT, GENERIC base of all charts. T is the type of data the chart shows
 * (CategoryData for bar/line charts, List<ChartSlice> for the donut).
 *
 * paintComponent() is a TEMPLATE METHOD: it draws the card, the title and the
 * "no data" message, and calls drawChart() - which each chart type implements.
 * All charts are drawn with Java2D, no chart library is used.
 */
public abstract class ChartPanel<T> extends JPanel {

    private final String title;
    protected T data;

    protected ChartPanel(String title) {
        this.title = title;
        setBackground(UITheme.CARD);
        setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
    }

    public void setData(T data) {
        this.data = data;
        repaint();
    }

    @Override
    protected final void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(UITheme.TEXT);
        g2.setFont(UITheme.font(Font.BOLD, 14));
        g2.drawString(title, 14, 24);
        Rectangle area = new Rectangle(14, 38, getWidth() - 28, getHeight() - 50);
        if (data == null || isEmpty()) {
            g2.setColor(UITheme.MUTED);
            g2.setFont(UITheme.font(Font.ITALIC, 13));
            String msg = data == null ? "Loading..." : "No data yet";
            g2.drawString(msg, area.x + (area.width - g2.getFontMetrics().stringWidth(msg)) / 2, area.y + area.height / 2);
        } else {
            drawChart(g2, area);
        }
        g2.dispose();
    }

    protected abstract boolean isEmpty();

    protected abstract void drawChart(Graphics2D g2, Rectangle area);

    /** Small coloured square + text, used by legends. */
    protected static int legend(Graphics2D g2, int x, int y, Color color, String text) {
        g2.setColor(color);
        g2.fillRect(x, y - 9, 10, 10);
        g2.setColor(UITheme.MUTED);
        g2.setFont(UITheme.font(Font.PLAIN, 11));
        g2.drawString(text, x + 14, y);
        return x + 14 + g2.getFontMetrics().stringWidth(text) + 14;
    }

    protected static String compact(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }
}
