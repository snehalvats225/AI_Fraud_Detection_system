package fraud.detection.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/** Draws the shield logo at any size with Java2D (no image files needed). */
public final class Logo {

    private Logo() {
    }

    public static void draw(Graphics2D g2, int x, int y, int size) {
        double w = size;
        double h = size * 1.1;
        Path2D shield = new Path2D.Double();
        shield.moveTo(x + w / 2, y);
        shield.lineTo(x + w, y + h * 0.18);
        shield.curveTo(x + w, y + h * 0.65, x + w * 0.75, y + h * 0.88, x + w / 2, y + h);
        shield.curveTo(x + w * 0.25, y + h * 0.88, x, y + h * 0.65, x, y + h * 0.18);
        shield.closePath();
        g2.setColor(Color.WHITE);
        g2.fill(shield);
        g2.setColor(UITheme.ACCENT);
        g2.setStroke(new BasicStroke(Math.max(2f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D check = new Path2D.Double();
        check.moveTo(x + w * 0.28, y + h * 0.52);
        check.lineTo(x + w * 0.45, y + h * 0.68);
        check.lineTo(x + w * 0.74, y + h * 0.36);
        g2.draw(check);
    }
}
