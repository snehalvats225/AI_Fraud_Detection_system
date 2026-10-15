package fraud.detection.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Gradient banner at the top of every window, drawn by OVERRIDING paintComponent(). */
public class HeaderPanel extends JPanel {

    private final String title;
    private final String subtitle;

    public HeaderPanel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
        setPreferredSize(new Dimension(100, 70));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, UITheme.PRIMARY, getWidth(), 0, new Color(30, 64, 175)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        Logo.draw(g2, 20, 12, 40);
        g2.setColor(Color.WHITE);
        g2.setFont(UITheme.font(Font.BOLD, 20));
        g2.drawString(title, 76, 34);
        if (subtitle != null && !subtitle.isEmpty()) {
            g2.setColor(new Color(191, 219, 254));
            g2.setFont(UITheme.font(Font.PLAIN, 13));
            g2.drawString(subtitle, 77, 54);
        }
        g2.setFont(UITheme.font(Font.BOLD, 13));
        g2.setColor(new Color(191, 219, 254));
        String app = UITheme.APP_NAME + "  |  Fraud Detection System";
        g2.drawString(app, getWidth() - g2.getFontMetrics().stringWidth(app) - 18, 40);
        g2.dispose();
    }
}
