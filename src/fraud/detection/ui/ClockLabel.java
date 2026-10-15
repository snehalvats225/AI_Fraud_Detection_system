package fraud.detection.ui;

import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Live date-and-time label on its own DAEMON thread (does not keep the program alive).
 * "running" is VOLATILE, so when stop() changes it on the UI thread the clock
 * thread is guaranteed to see it and exit.
 */
public class ClockLabel extends JLabel implements Runnable {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  hh:mm:ss a");

    private volatile boolean running = true;

    public ClockLabel() {
        setFont(UITheme.font(Font.PLAIN, 13));
        setForeground(UITheme.MUTED);
        setHorizontalAlignment(SwingConstants.RIGHT);
        Thread t = new Thread(this, "clock-thread");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        while (running) {
            String now = LocalDateTime.now().format(FORMAT);
            SwingUtilities.invokeLater(() -> setText(now));      // UI changes only on the UI thread
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void stop() {
        running = false;
    }
}
