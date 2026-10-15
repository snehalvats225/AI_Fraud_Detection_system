package fraud.detection.ui;

import fraud.detection.dao.ModelVersionDAO;
import fraud.detection.dao.ReportDAO;
import fraud.detection.dao.TransactionDAO;
import fraud.detection.engine.FraudDetector;
import fraud.detection.engine.MachineLearningDetector;
import fraud.detection.engine.RuleBasedDetector;
import fraud.detection.engine.StatisticalDetector;
import fraud.detection.ml.Features;
import fraud.detection.ml.ModelMetrics;
import fraud.detection.ml.ModelRegistry;
import fraud.detection.ml.ModelVersion;
import fraud.detection.model.Admin;
import fraud.detection.model.DetectionConfig;
import fraud.detection.model.FeedbackSummary;
import fraud.detection.service.ConfigManager;
import fraud.detection.service.ModelTrainingService;
import fraud.detection.service.TrainingOutcome;
import fraud.detection.ui.chart.CategoryData;
import fraud.detection.ui.chart.ChartSeries;
import fraud.detection.ui.chart.LineChartPanel;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.DateUtil;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.TableModelBuilder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * ALGORITHM MANAGEMENT: see how well the model performs, retrain it with the
 * latest feedback, compare versions and roll back.
 * Input: performance data and feedback. Output: an updated detection algorithm.
 */
public class AlgorithmManagementFrame extends BaseFrame {

    private static final String[] COLUMNS = {"Ver", "Trained", "By", "Samples", "Real", "Acc", "Prec", "Recall",
        "F1", "Active"};

    private final Admin admin;
    private final Runnable onChange;
    private final JLabel modelTitle, modelInfo, accValue, precValue, recValue, f1Value, matrix, feedbackLine, paramsLine, trainStatus;
    private final JLabel[] detectorLines = new JLabel[3];
    private final JProgressBar trainProgress = new JProgressBar(0, 100);
    private final JButton retrain;
    private final LineChartPanel chart = new LineChartPanel("Model quality across versions (test data)", 0.5);
    private final JTable table = new JTable();
    private List<ModelVersion> versions = new ArrayList<>();

    public AlgorithmManagementFrame(Admin admin, Runnable onChange) {
        super("Algorithm Management", "Monitor, retrain and roll back the fraud detection model", 1200, 720);
        this.admin = admin;
        this.onChange = onChange;

        // ---------- active model ----------
        JPanel card = UITheme.card();
        card.setBounds(20, 10, 560, 182);
        content.add(card);
        modelTitle = addValue(card, "Active model", 16, 10, 520, 24);
        modelTitle.setFont(UITheme.font(Font.BOLD, 16));
        modelInfo = addMuted(card, "", 16, 34, 530, 20);
        accValue = metricBox(card, "Accuracy", 16);
        precValue = metricBox(card, "Precision", 148);
        recValue = metricBox(card, "Recall", 280);
        f1Value = metricBox(card, "F1 score", 412);
        matrix = addLabel(card, "", 16, 132, 530, 40);
        matrix.setFont(UITheme.font(Font.PLAIN, 12));

        // ---------- detectors (polymorphism: each object describes itself) ----------
        JPanel det = UITheme.card();
        det.setBounds(20, 202, 560, 150);
        content.add(det);
        addValue(det, "Detection algorithms in the ensemble", 16, 8, 500, 22);
        for (int i = 0; i < 3; i++) {
            detectorLines[i] = addLabel(det, "", 16, 32 + i * 38, 535, 36);
            detectorLines[i].setFont(UITheme.font(Font.PLAIN, 12));
            detectorLines[i].setVerticalAlignment(JLabel.TOP);
        }

        // ---------- feedback ----------
        JPanel fb = UITheme.card();
        fb.setBounds(20, 362, 560, 74);
        content.add(fb);
        addValue(fb, "Feedback available for training", 16, 8, 500, 22);
        feedbackLine = addLabel(fb, "Loading...", 16, 30, 535, 40);
        feedbackLine.setFont(UITheme.font(Font.PLAIN, 12));
        feedbackLine.setVerticalAlignment(JLabel.TOP);

        // ---------- retrain ----------
        JPanel tr = UITheme.card();
        tr.setBounds(20, 446, 560, 160);
        content.add(tr);
        addValue(tr, "Retrain the ML model", 16, 8, 500, 22);
        paramsLine = addMuted(tr, "", 16, 32, 535, 20);
        retrain = UITheme.button("Retrain with Latest Feedback", UITheme.PURPLE);
        retrain.setBounds(16, 56, 260, 38);
        tr.add(retrain);
        JButton config = UITheme.greyButton("Change Parameters");
        config.setBounds(286, 56, 180, 38);
        tr.add(config);
        trainProgress.setStringPainted(true);
        trainProgress.setString("Idle");
        trainProgress.setForeground(UITheme.PURPLE);
        trainProgress.setBounds(16, 102, 528, 22);
        tr.add(trainProgress);
        trainStatus = addMuted(tr, "Training runs on background threads; the new model goes live immediately.", 16, 130, 535, 22);
        trainStatus.setFont(UITheme.font(Font.ITALIC, 12));

        // ---------- right side: chart + versions ----------
        chart.setBounds(595, 10, 580, 250);
        content.add(chart);
        UITheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(595, 270, 580, 286);
        content.add(scroll);
        JButton activate = addButton(UITheme.primaryButton("Activate Selected (Rollback)"), 595, 566, 240, 40);
        JButton weights = addButton(UITheme.accentButton("View Model Weights"), 845, 566, 190, 40);
        JButton close = addButton(UITheme.greyButton("Close"), 1045, 566, 130, 40);

        retrain.addActionListener(e -> doRetrain());
        config.addActionListener(e -> new ConfigurationFrame(admin, this::reload).setVisible(true));
        activate.addActionListener(e -> doActivate());
        weights.addActionListener(e -> showWeights());
        close.addActionListener(e -> dispose());
        reload();
    }

    private JLabel metricBox(JPanel card, String title, int x) {
        JPanel box = new JPanel(null);
        box.setBackground(UITheme.BACKGROUND);
        box.setBounds(x, 60, 122, 64);
        card.add(box);
        addMuted(box, title, 10, 6, 110, 18).setFont(UITheme.font(Font.PLAIN, 12));
        JLabel v = addValue(box, "-", 10, 24, 110, 32);
        v.setFont(UITheme.font(Font.BOLD, 24));
        v.setForeground(UITheme.PURPLE);
        return v;
    }

    /** Reloads everything on a background thread. */
    private void reload() {
        BackgroundTask.run(this, () -> {
            List<ModelVersion> list = new ModelVersionDAO().findAll();
            int labelled = new TransactionDAO().loadLabelledSamples().size();
            FeedbackSummary fb = new ReportDAO().getFeedbackSummary(labelled);
            return new Object[] {list, fb};
        }, r -> {
            @SuppressWarnings("unchecked")
            List<ModelVersion> list = (List<ModelVersion>) r[0];
            versions = list;
            showActive();
            showFeedback((FeedbackSummary) r[1]);
            showVersions();
        });
    }

    private void showActive() {
        ModelVersion v = ModelRegistry.getActive();
        DetectionConfig cfg = ConfigManager.get();
        if (v != null) {
            ModelMetrics m = v.getMetrics();
            modelTitle.setText("Active model " + v.getLabel() + "  -  Logistic Regression (" + Features.COUNT + " features)");
            modelInfo.setText("Trained " + DateUtil.format(v.getTrainedAt()) + " by " + v.getTrainedBy() + "  |  "
                    + v.getTrainingSamples() + " samples (" + v.getRealSamples() + " real feedback)");
            accValue.setText(pct(m.getAccuracy()));
            precValue.setText(pct(m.getPrecision()));
            recValue.setText(pct(m.getRecall()));
            f1Value.setText(String.format("%.3f", m.getF1()));
            matrix.setText(String.format("<html>Confusion matrix on test data:&nbsp; <b>%d</b> fraud caught (TP), "
                    + "<b>%d</b> fraud missed (FN), <b>%d</b> false alarms (FP), <b>%d</b> genuine passed (TN)</html>",
                    m.getTp(), m.getFn(), m.getFp(), m.getTn()));
        }
        List<FraudDetector> detectors = List.of(new RuleBasedDetector(), new StatisticalDetector(), new MachineLearningDetector(v));
        double[] w = {cfg.getRuleWeight(), cfg.getStatWeight(), cfg.getMlWeight()};
        double total = w[0] + w[1] + w[2];
        for (int i = 0; i < detectors.size(); i++) {
            FraudDetector d = detectors.get(i);
            detectorLines[i].setText(String.format("<html><b>%s</b> &nbsp;(weight %.2f = %.0f%%) - %s</html>",
                    d.getName(), w[i], total == 0 ? 0 : 100 * w[i] / total, d.getDescription()));
        }
        paramsLine.setText(String.format("Learning rate %.3f  |  %d epochs  |  L2 %.4f  |  80%% train / 20%% test split",
                cfg.getLearningRate(), cfg.getEpochs(), cfg.getL2Lambda()));
    }

    private void showFeedback(FeedbackSummary f) {
        feedbackLine.setText(String.format("<html><b>%d</b> real labelled transactions (each counts 3x). Admin reviews: "
                + "<b>%d</b> confirmed fraud, <b>%d</b> false positives. Customers: <b>%d</b> reported fraud, "
                + "<b>%d</b> confirmed genuine.</html>", f.getLabelledTotal(), f.getAdminConfirmedFraud(),
                f.getAdminFalsePositive(), f.getUserReportedFraud(), f.getUserConfirmedGenuine()));
    }

    private void showVersions() {
        table.setModel(TableModelBuilder.build(COLUMNS, versions, (ModelVersion v) -> new Object[] {
            v.getLabel(), DateUtil.formatShort(v.getTrainedAt()), v.getTrainedBy(), v.getTrainingSamples(), v.getRealSamples(),
            pct(v.getMetrics().getAccuracy()), pct(v.getMetrics().getPrecision()), pct(v.getMetrics().getRecall()),
            String.format("%.3f", v.getMetrics().getF1()), v.isActive() ? "Yes" : ""
        }));
        UITheme.setColumnWidths(table, 40, 105, 95, 60, 40, 55, 55, 55, 50, 50);

        List<ModelVersion> ordered = new ArrayList<>(versions);
        Collections.reverse(ordered);                     // oldest first for the chart
        if (ordered.size() > 12) {
            ordered = ordered.subList(ordered.size() - 12, ordered.size());
        }
        List<String> labels = new ArrayList<>();
        double[] acc = new double[ordered.size()];
        double[] prec = new double[ordered.size()];
        double[] rec = new double[ordered.size()];
        double[] f1 = new double[ordered.size()];
        for (int i = 0; i < ordered.size(); i++) {
            ModelMetrics m = ordered.get(i).getMetrics();
            labels.add(ordered.get(i).getLabel());
            acc[i] = m.getAccuracy();
            prec[i] = m.getPrecision();
            rec[i] = m.getRecall();
            f1[i] = m.getF1();
        }
        chart.setData(new CategoryData(labels, List.of(
                new ChartSeries("Accuracy", UITheme.PRIMARY_LIGHT, acc),
                new ChartSeries("Precision", UITheme.WARNING, prec),
                new ChartSeries("Recall", UITheme.SUCCESS, rec),
                new ChartSeries("F1", UITheme.PURPLE, f1))));
    }

    private void doRetrain() {
        if (!MessageUtil.confirm(this, "Retrain the ML model with the latest feedback?\n"
                + "The new version becomes active immediately (you can roll back later).")) {
            return;
        }
        retrain.setEnabled(false);
        trainProgress.setValue(0);
        trainProgress.setString("Preparing data...");
        // The listener is called on a TRAINING thread, so the UI update goes through invokeLater
        BackgroundTask.run(this, () -> new ModelTrainingService().retrain(admin.getFullName(),
                (epoch, total, loss) -> SwingUtilities.invokeLater(() -> {
                    trainProgress.setValue(100 * epoch / total);
                    trainProgress.setString(String.format("Epoch %d / %d   -   loss %.4f", epoch, total, loss));
                })), (TrainingOutcome out) -> {
                    ModelMetrics m = out.getNewVersion().getMetrics();
                    ModelMetrics p = out.getPreviousMetrics();
                    trainProgress.setValue(100);
                    trainProgress.setString("Done - " + out.getNewVersion().getLabel() + " is now active");
                    trainStatus.setText(String.format("Trained in %d ms on %d synthetic + %d real samples.",
                            out.getMillis(), out.getSyntheticSamples(), out.getRealSamples()));
                    MessageUtil.info(this, "Detection algorithm updated!\n\n"
                            + "New model: " + out.getNewVersion().getLabel() + "\n"
                            + compare("Accuracy", p == null ? -1 : p.getAccuracy(), m.getAccuracy())
                            + compare("Precision", p == null ? -1 : p.getPrecision(), m.getPrecision())
                            + compare("Recall", p == null ? -1 : p.getRecall(), m.getRecall())
                            + compare("F1 score", p == null ? -1 : p.getF1(), m.getF1())
                            + "\nIf the new version is worse, select an older one and click 'Activate Selected'.");
                    onChange.run();
                    reload();
                }, () -> retrain.setEnabled(true));
    }

    private static String compare(String name, double before, double after) {
        return before < 0 ? String.format("%-10s %.3f%n", name, after)
                : String.format("%-10s %.3f  ->  %.3f  (%+.3f)%n", name, before, after, after - before);
    }

    private ModelVersion selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : versions.get(table.convertRowIndexToModel(row));
    }

    private void doActivate() {
        ModelVersion v = selected();
        if (v == null) {
            MessageUtil.error(this, "Please select a model version first.");
            return;
        }
        if (v.isActive()) {
            MessageUtil.info(this, v.getLabel() + " is already the active model.");
            return;
        }
        if (!MessageUtil.confirm(this, "Make " + v.getLabel() + " the active model?\nNew transactions will be scored with it.")) {
            return;
        }
        BackgroundTask.run(this, () -> new ModelTrainingService().activate(v.getVersionId()), active -> {
            MessageUtil.info(this, active.getLabel() + " is now the active model.");
            onChange.run();
            reload();
        });
    }

    /** Shows what the model learned: one weight per feature (bigger = stronger fraud signal). */
    private void showWeights() {
        ModelVersion v = selected() != null ? selected() : ModelRegistry.getActive();
        if (v == null) {
            return;
        }
        double[] w = v.getModel().getWeights();
        double max = 0.01;
        for (double d : w) {
            max = Math.max(max, Math.abs(d));
        }
        StringBuilder sb = new StringBuilder(" What model " + v.getLabel() + " learned\n");
        sb.append(" (weight > 0 raises the fraud probability; features are standardised)\n\n");
        for (int i = 0; i < w.length; i++) {
            int len = (int) Math.round(24 * Math.abs(w[i]) / max);
            sb.append(String.format(" %-28s %+6.2f  %s%n", Features.NAMES.get(i), w[i], (w[i] >= 0 ? "#" : "-").repeat(len)));
        }
        sb.append(String.format("%n %-28s %+6.2f%n", "Bias (intercept)", v.getModel().getBias()));
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(UITheme.mono(13));
        area.setBackground(new Color(248, 250, 252));
        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(640, 330));
        JOptionPane.showMessageDialog(this, pane, "Model weights - " + v.getLabel(), JOptionPane.PLAIN_MESSAGE);
    }

    private static String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }
}
