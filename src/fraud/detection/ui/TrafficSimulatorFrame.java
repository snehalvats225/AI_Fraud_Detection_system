package fraud.detection.ui;

import fraud.detection.service.SimulationProgress;
import fraud.detection.service.SimulationService;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.MessageUtil;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

/**
 * Generates realistic traffic from many customers at the same time (multithreading
 * demo) and measures how much of the injected fraud the system catches.
 * A Swing Timer reads the thread-safe counters 6 times per second.
 */
public class TrafficSimulatorFrame extends BaseFrame {

    private final Runnable onDone;
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(300, 10, 3000, 50));
    private final JSpinner fraudPct = new JSpinner(new SpinnerNumberModel(15, 0, 60, 5));
    private final JSpinner threads = new JSpinner(new SpinnerNumberModel(6, 1, 16, 1));
    private final JCheckBox groundTruth = new JCheckBox("Store the simulator's true answers as analyst feedback (for retraining)");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel processed, approved, flagged, blocked, caught, falseAlarms;
    private final JTextArea result = new JTextArea();
    private final JButton start, cancel;
    private SimulationProgress current;
    private Timer timer;
    private int total;

    public TrafficSimulatorFrame(Runnable onDone) {
        super("Traffic Simulator", "Many customers paying at once - through the real detection pipeline", 820, 640);
        this.onDone = onDone;

        JTextArea info = new JTextArea("Creates random payments for the existing customers, spread over the last 7 days.\n"
                + "Some are made to look like fraud (big amounts, night time, foreign city, new device, crypto / gift cards).\n"
                + "Each payment goes through the real scoring, balance update, case and alert logic - in parallel threads.");
        info.setEditable(false);
        info.setFont(UITheme.font(Font.PLAIN, 13));
        info.setBackground(UITheme.CARD);
        info.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.BORDER));
        place(info, 20, 12, 765, 62).setFont(UITheme.font(Font.PLAIN, 13));

        addLabel("Number of transactions", 20, 88, 190, 28);
        place(count, 210, 88, 90, 28);
        addLabel("Fraud share (%)", 330, 88, 130, 28);
        place(fraudPct, 460, 88, 70, 28);
        addLabel("Threads", 560, 88, 70, 28);
        place(threads, 630, 88, 70, 28);
        groundTruth.setSelected(true);
        groundTruth.setBackground(UITheme.BACKGROUND);
        place(groundTruth, 18, 124, 600, 26).setFont(UITheme.font(Font.PLAIN, 13));

        start = addButton(UITheme.button("Start Simulation", UITheme.WARNING), 20, 160, 190, 40);
        cancel = addButton(UITheme.dangerButton("Cancel"), 220, 160, 120, 40);
        JButton close = addButton(UITheme.greyButton("Close"), 665, 160, 120, 40);
        cancel.setEnabled(false);
        progress.setStringPainted(true);
        place(progress, 20, 212, 765, 24);

        JPanel stats = UITheme.card();
        stats.setBounds(20, 246, 765, 92);
        content.add(stats);
        processed = stat(stats, "Processed", 14, 10);
        approved = stat(stats, "Approved", 200, 10);
        flagged = stat(stats, "Flagged", 386, 10);
        blocked = stat(stats, "Blocked", 572, 10);
        caught = stat(stats, "Fraud caught", 14, 50);
        falseAlarms = stat(stats, "False alarms", 386, 50);

        result.setEditable(false);
        result.setFont(UITheme.mono(12));
        place(new javax.swing.JScrollPane(result), 20, 348, 765, 190);
        result.setFont(UITheme.mono(12));

        start.addActionListener(e -> begin());
        cancel.addActionListener(e -> {
            if (current != null) {
                current.cancel();
            }
        });
        close.addActionListener(e -> dispose());
    }

    private JLabel stat(JPanel p, String name, int x, int y) {
        addMuted(p, name, x, y, 100, 28);
        JLabel v = addValue(p, "0", x + 100, y, 80, 28);
        v.setFont(UITheme.font(Font.BOLD, 16));
        return v;
    }

    private void begin() {
        total = (Integer) count.getValue();
        double ratio = (Integer) fraudPct.getValue() / 100.0;
        int t = (Integer) threads.getValue();
        boolean labels = groundTruth.isSelected();
        current = new SimulationProgress();
        SimulationProgress p = current;
        start.setEnabled(false);
        cancel.setEnabled(true);
        result.setText(" Running " + total + " transactions on " + t + " threads...\n");
        timer = new Timer(160, e -> update(p));
        timer.start();
        BackgroundTask.run(this, () -> new SimulationService().run(total, ratio, t, labels, p), ms -> {
            update(p);
            double tps = p.done.get() * 1000.0 / Math.max(1, ms);
            result.setText(String.format(" SIMULATION %s%n%n", p.isCancelled() ? "CANCELLED" : "COMPLETE")
                    + String.format(" Processed %,d transactions in %,d ms on %d threads  ->  %.0f transactions/second%n%n",
                            p.done.get(), ms, t, tps)
                    + String.format(" Approved %d   |   Flagged %d   |   Blocked %d   |   Skipped (not enough balance) %d%n%n",
                            p.approved.get(), p.flagged.get(), p.blocked.get(), p.skipped.get())
                    + String.format(" Fraud injected : %d  ->  caught %d  (detection rate / recall %.0f%%)%n",
                            p.injectedFraud.get(), p.caughtFraud.get(), pct(p.caughtFraud.get(), p.injectedFraud.get()))
                    + String.format(" Genuine        : %d  ->  wrongly stopped %d  (false-alarm rate %.1f%%)%n%n",
                            p.genuine.get(), p.falseAlarms.get(), pct(p.falseAlarms.get(), p.genuine.get()))
                    + (labels ? " True answers were saved as feedback - open Algorithm Management and Retrain.\n" : "")
                    + " The Admin Dashboard charts and Detection Monitoring now include these transactions.");
            onDone.run();
        }, () -> {
            timer.stop();
            start.setEnabled(true);
            cancel.setEnabled(false);
        });
    }

    private void update(SimulationProgress p) {
        int done = p.done.get();
        progress.setValue(total == 0 ? 0 : 100 * done / total);
        progress.setString(done + " / " + total);
        processed.setText(String.valueOf(done));
        approved.setText(String.valueOf(p.approved.get()));
        flagged.setText(String.valueOf(p.flagged.get()));
        blocked.setText(String.valueOf(p.blocked.get()));
        caught.setText(p.caughtFraud.get() + " / " + p.injectedFraud.get());
        falseAlarms.setText(p.falseAlarms.get() + " / " + p.genuine.get());
    }

    private static double pct(int part, int whole) {
        return whole == 0 ? 0 : 100.0 * part / whole;
    }

    @Override
    public void dispose() {
        if (current != null) {
            current.cancel();
        }
        if (timer != null) {
            timer.stop();
        }
        super.dispose();
    }

    static {
        // keep MessageUtil referenced for consistent error dialogs from BackgroundTask
        MessageUtil.class.getName();
    }
}
