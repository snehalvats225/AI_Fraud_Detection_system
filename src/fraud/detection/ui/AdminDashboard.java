package fraud.detection.ui;

import fraud.detection.dao.ReportDAO;
import fraud.detection.engine.PerformanceMonitor;
import fraud.detection.ml.ModelVersion;
import fraud.detection.ml.ModelRegistry;
import fraud.detection.model.Admin;
import fraud.detection.model.CaseStatus;
import fraud.detection.model.DashboardStats;
import fraud.detection.model.DetectionConfig;
import fraud.detection.service.ConfigManager;
import fraud.detection.ui.chart.BarChartPanel;
import fraud.detection.ui.chart.CategoryData;
import fraud.detection.ui.chart.ChartSeries;
import fraud.detection.ui.chart.ChartSlice;
import fraud.detection.ui.chart.DonutChartPanel;
import fraud.detection.ui.chart.HistogramPanel;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.MoneyUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * ADMIN DASHBOARD: live numbers, three charts of detected fraud ("Detection
 * Reports - graphical representation") and links to Configuration, Detection
 * Monitoring and Algorithm Management.
 *
 * MULTITHREADING: a ScheduledExecutorService ("dashboard-refresher" thread)
 * reloads the statistics every 10 seconds in the background (live monitoring).
 */
public class AdminDashboard extends BaseFrame {

    private static final int REFRESH_SECONDS = 10;

    private final Admin admin;
    private final JLabel txnValue, flaggedValue, blockedValue, openValue, confirmedValue, amountValue, perfLine;
    private final BarChartPanel dailyChart = new BarChartPanel("Fraud cases - last 7 days");
    private final DonutChartPanel statusChart = new DonutChartPanel("Case review status");
    private final HistogramPanel riskChart = new HistogramPanel("Risk score distribution");
    private final ClockLabel clock = new ClockLabel();
    private final ScheduledExecutorService refresher;

    public AdminDashboard(Admin admin) {
        super("Admin Dashboard", admin.getWelcomeMessage(), 1100, 680);
        this.admin = admin;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });
        clock.setBounds(770, 4, 300, 20);
        content.add(clock);

        int w = 165, gap = 10, x = 30, y = 26, h = 92;
        txnValue = statCard("Transactions", x, y, w, h, UITheme.PRIMARY_LIGHT);
        flaggedValue = statCard("Flagged", x += w + gap, y, w, h, UITheme.WARNING);
        blockedValue = statCard("Blocked", x += w + gap, y, w, h, UITheme.DANGER);
        openValue = statCard("Open Cases", x += w + gap, y, w, h, UITheme.PURPLE);
        confirmedValue = statCard("Confirmed Fraud", x += w + gap, y, w, h, UITheme.DANGER);
        amountValue = statCard("Money Protected", x + w + gap, y, w, h, UITheme.SUCCESS);
        amountValue.setFont(UITheme.font(Font.BOLD, 17));

        dailyChart.setBounds(30, 128, 400, 250);
        statusChart.setBounds(440, 128, 300, 250);
        riskChart.setBounds(750, 128, 320, 250);
        content.add(dailyChart);
        content.add(statusChart);
        content.add(riskChart);

        JPanel perf = UITheme.card();
        perf.setBounds(30, 388, 1040, 40);
        content.add(perf);
        perfLine = addLabel(perf, "", 14, 9, 1015, 22);
        perfLine.setFont(UITheme.font(Font.PLAIN, 13));

        JButton[] buttons = {
            action("Configuration Settings", UITheme.PRIMARY_LIGHT, () -> new ConfigurationFrame(admin, this::refresh).setVisible(true)),
            action("Detection Monitoring", UITheme.DANGER, () -> new DetectionMonitoringFrame(admin, this::refresh).setVisible(true)),
            action("Algorithm Management", UITheme.PURPLE, () -> new AlgorithmManagementFrame(admin, this::refresh).setVisible(true)),
            action("All Transactions", UITheme.ACCENT, () -> new AllTransactionsFrame().setVisible(true)),
            action("Traffic Simulator", UITheme.WARNING, () -> new TrafficSimulatorFrame(this::refresh).setVisible(true)),
            action("Refresh Now", UITheme.SUCCESS, this::refresh),
            action("Change Password", UITheme.MUTED, () -> new ChangePasswordFrame(admin).setVisible(true)),
            action("Logout", UITheme.DANGER, this::logout)
        };
        for (int i = 0; i < buttons.length; i++) {
            addButton(buttons[i], 30 + (i % 4) * 265, 440 + (i / 4) * 66, 250, 54);
        }

        refresher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-refresher");
            t.setDaemon(true);
            return t;
        });
        refresher.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::refresh),
                REFRESH_SECONDS, REFRESH_SECONDS, TimeUnit.SECONDS);
        refresh();
    }

    private JButton action(String text, Color color, Runnable onClick) {
        JButton b = UITheme.button(text, color);
        b.setFont(UITheme.font(Font.BOLD, 14));
        b.addActionListener(e -> onClick.run());
        return b;
    }

    /** Loads statistics on a background thread, then updates cards and charts on the UI thread. */
    public void refresh() {
        BackgroundTask.run(this, () -> new ReportDAO().getDashboardStats(), this::show);
    }

    private void show(DashboardStats s) {
        txnValue.setText(String.format("%,d", s.getTransactions()));
        flaggedValue.setText(String.format("%,d", s.getFlagged()));
        blockedValue.setText(String.format("%,d", s.getBlocked()));
        openValue.setText(String.format("%,d", s.getOpenCases()));
        confirmedValue.setText(String.format("%,d", s.getConfirmedFraud()));
        amountValue.setText(MoneyUtil.format(s.getAmountBlocked()));

        List<String> days = new ArrayList<>();
        double[] flagged = new double[s.getCasesPerDay().size()];
        double[] blocked = new double[flagged.length];
        int i = 0;
        for (Map.Entry<LocalDate, int[]> e : s.getCasesPerDay().entrySet()) {
            days.add(e.getKey().format(DateTimeFormatter.ofPattern("dd MMM")));
            flagged[i] = e.getValue()[0];
            blocked[i++] = e.getValue()[1];
        }
        dailyChart.setData(new CategoryData(days, List.of(
                new ChartSeries("Flagged", UITheme.WARNING, flagged),
                new ChartSeries("Blocked", UITheme.DANGER, blocked))));

        Map<CaseStatus, Integer> st = s.getCasesByStatus();
        statusChart.setData(List.of(
                new ChartSlice("Open - needs review", st.get(CaseStatus.OPEN), UITheme.PURPLE),
                new ChartSlice("Confirmed fraud", st.get(CaseStatus.CONFIRMED_FRAUD), UITheme.DANGER),
                new ChartSlice("False positive", st.get(CaseStatus.FALSE_POSITIVE), UITheme.SUCCESS)));

        DetectionConfig cfg = ConfigManager.get();
        int[] hist = s.getRiskHistogram();
        double[] values = new double[hist.length];
        List<String> buckets = new ArrayList<>();
        for (int b = 0; b < hist.length; b++) {
            values[b] = hist[b];
            buckets.add(String.valueOf(b * 10));
        }
        riskChart.setThresholds(cfg.getAlertThreshold(), cfg.getBlockThreshold());
        riskChart.setData(new CategoryData(buckets, List.of(new ChartSeries("Transactions", UITheme.PRIMARY_LIGHT, values))));

        ModelVersion model = ModelRegistry.getActive();
        double precision = s.getLivePrecision();
        perfLine.setText(String.format("<html><b>Engine</b> %,d scored, avg %.1f ms, max %.0f ms &nbsp;|&nbsp; "
                + "<b>Model</b> %s (F1 %.2f) &nbsp;|&nbsp; <b>Thresholds</b> %.2f / %.2f &nbsp;|&nbsp; "
                + "<b>Live precision</b> %s &nbsp;|&nbsp; updated %s</html>",
                PerformanceMonitor.scored(), PerformanceMonitor.averageMillis(), PerformanceMonitor.maxMillis(),
                model == null ? "none" : model.getLabel(), model == null ? 0 : model.getMetrics().getF1(),
                cfg.getAlertThreshold(), cfg.getBlockThreshold(),
                precision < 0 ? "no reviews yet" : MoneyUtil.percent(precision),
                LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"))));
        perfLine.setFont(UITheme.font(Font.PLAIN, 12));
    }

    @Override
    public void dispose() {
        refresher.shutdownNow();
        clock.stop();
        super.dispose();
    }

    private void logout() {
        if (MessageUtil.confirm(this, "Do you want to logout?")) {
            for (Window w : Window.getWindows()) {
                if (w != this && w.isDisplayable() && !(w instanceof LoginFrame)) {
                    w.dispose();
                }
            }
            dispose();
            LoginFrame.open();
        }
    }
}
