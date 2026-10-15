package fraud.detection.ui;

import fraud.detection.ml.ModelVersion;
import fraud.detection.service.ConfigManager;
import fraud.detection.service.ModelTrainingService;
import fraud.detection.util.MessageUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * Loading screen that does REAL work on its own thread (implements Runnable):
 * it loads the detection settings and the active ML model - and on the very
 * first run it TRAINS model v1. Screen updates go back to the UI thread with
 * SwingUtilities.invokeLater().
 */
public class SplashWindow extends JWindow implements Runnable {

    private final JProgressBar progress = new JProgressBar(0, 100);
    private volatile Throwable startupError;

    public SplashWindow() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UITheme.PRIMARY, getWidth(), getHeight(), new Color(30, 64, 175)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                Logo.draw(g2, getWidth() / 2 - 38, 34, 76);
                g2.setColor(Color.WHITE);
                g2.setFont(UITheme.font(Font.BOLD, 30));
                center(g2, UITheme.APP_NAME, 166);
                g2.setFont(UITheme.font(Font.PLAIN, 16));
                g2.setColor(new Color(191, 219, 254));
                center(g2, "AI-Powered Fraud Detection System", 196);
                g2.setFont(UITheme.font(Font.ITALIC, 13));
                center(g2, "Detect  |  Alert  |  Learn", 223);
                g2.dispose();
            }

            private void center(Graphics2D g2, String s, int y) {
                g2.drawString(s, (getWidth() - g2.getFontMetrics().stringWidth(s)) / 2, y);
            }
        };
        progress.setStringPainted(true);
        progress.setFont(UITheme.font(Font.PLAIN, 12));
        progress.setForeground(UITheme.ACCENT);
        progress.setBorder(BorderFactory.createEmptyBorder(0, 40, 25, 40));
        progress.setOpaque(false);
        panel.add(progress, BorderLayout.SOUTH);
        setContentPane(panel);
        setSize(560, 320);
        setLocationRelativeTo(null);
    }

    public void start() {
        setVisible(true);
        new Thread(this, "splash-loader").start();
    }

    @Override
    public void run() {
        try {
            step(15, "Connecting to the database...", 250);
            ConfigManager.load();
            step(40, "Detection settings loaded", 250);
            step(55, "Loading the ML model (training v1 on first run)...", 0);
            ModelVersion model = new ModelTrainingService().loadOrBootstrap();
            step(90, "ML model " + model.getLabel() + " ready", 350);
            step(100, "Ready", 250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            startupError = e;                      // shown after the login window opens
        }
        SwingUtilities.invokeLater(() -> {
            dispose();
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
            if (startupError != null) {
                MessageUtil.error(login, startupError);
            }
        });
    }

    private void step(int value, String text, long sleepMs) throws InterruptedException {
        SwingUtilities.invokeLater(() -> {
            progress.setValue(value);
            progress.setString(text + "  " + value + "%");
        });
        Thread.sleep(sleepMs);
    }
}
