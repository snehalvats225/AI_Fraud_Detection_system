package fraud.detection.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import javax.swing.JPanel;

/** Speedometer-style gauge of the risk score with the alert / block thresholds marked. */
public class RiskMeter extends JPanel {

    private final double risk;
    private final double alert;
    private final double block;

    public RiskMeter(double risk, double alert, double block) {
        this.risk = risk;
        this.alert = alert;
        this.block = block;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int size = Math.min(getWidth() - 20, (getHeight() - 56) * 2);
        int x = (getWidth() - size) / 2;
        int y = 10;
        float thick = size / 9f;
        g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        arc(g2, x, y, size, 0, alert, UITheme.SUCCESS);
        arc(g2, x, y, size, alert, block, UITheme.WARNING);
        arc(g2, x, y, size, block, 1, UITheme.DANGER);

        // needle
        double angle = Math.toRadians(180 - 180 * Math.max(0, Math.min(1, risk)));
        int cx = x + size / 2;
        int cy = y + size / 2;
        int len = (int) (size / 2 - thick);
        g2.setColor(UITheme.PRIMARY);
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx, cy, cx + (int) (len * Math.cos(angle)), cy - (int) (len * Math.sin(angle)));
        g2.fillOval(cx - 8, cy - 8, 16, 16);

        g2.setFont(UITheme.font(Font.BOLD, 26));
        g2.setColor(UITheme.riskColor(risk, alert, block));
        String text = String.format("%.0f%%", risk * 100);
        g2.drawString(text, cx - g2.getFontMetrics().stringWidth(text) / 2, cy + 32);
        g2.setFont(UITheme.font(Font.PLAIN, 12));
        g2.setColor(UITheme.MUTED);
        String caption = "Overall risk score";
        g2.drawString(caption, cx - g2.getFontMetrics().stringWidth(caption) / 2, cy + 50);
        g2.setFont(UITheme.font(Font.PLAIN, 11));
        g2.setColor(UITheme.MUTED);
        g2.drawString("0%", x, cy + 16);
        g2.drawString("100%", x + size - g2.getFontMetrics().stringWidth("100%"), cy + 16);
        g2.dispose();
    }

    private static void arc(Graphics2D g2, int x, int y, int size, double from, double to, Color color) {
        double inset = size / 18.0;
        g2.setColor(color);
        g2.draw(new Arc2D.Double(x + inset, y + inset, size - 2 * inset, size - 2 * inset,
                180 - 180 * to, 180 * (to - from), Arc2D.OPEN));
    }
}
