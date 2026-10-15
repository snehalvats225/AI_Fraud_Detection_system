package fraud.detection.ui;

import fraud.detection.dao.FraudCaseDAO;
import fraud.detection.exception.InvalidInputException;
import fraud.detection.model.Admin;
import fraud.detection.model.CaseStatus;
import fraud.detection.model.Decision;
import fraud.detection.model.DetectionConfig;
import fraud.detection.model.FraudCase;
import fraud.detection.model.Transaction;
import fraud.detection.service.CaseService;
import fraud.detection.service.ConfigManager;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.DateUtil;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.MoneyUtil;
import fraud.detection.util.TableModelBuilder;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * DETECTION MONITORING: review detected fraud cases.
 * Input: detected cases. Output: detailed report per case.
 * The admin's decision becomes feedback (a training label) for the ML model.
 */
public class DetectionMonitoringFrame extends BaseFrame {

    private static final String[] COLUMNS = {"Case #", "Date", "Customer", "Amount (Rs.)", "Payee", "Category",
        "Location", "Risk", "Decision", "Status", "Customer Says"};
    private static final String ALL = "All";

    private final Admin admin;
    private final Runnable onChange;
    private final JTable table = new JTable();
    private final JComboBox<String> statusFilter = new JComboBox<>();
    private final JComboBox<String> decisionFilter = new JComboBox<>();
    private final JTextField search, notes;
    private final JLabel count;
    private final JTextArea report = new JTextArea();
    private final JProgressBar ruleBar = bar(), statBar = bar(), mlBar = bar(), totalBar = bar();
    private final JButton confirm, falsePositive;
    private List<FraudCase> loaded = new ArrayList<>();
    private List<FraudCase> shown = new ArrayList<>();

    public DetectionMonitoringFrame(Admin admin, Runnable onChange) {
        super("Detection Monitoring", "Review detected fraud cases and take action", 1200, 720);
        this.admin = admin;
        this.onChange = onChange;

        addLabel("Status", 20, 12, 50, 30);
        statusFilter.addItem(ALL);
        for (CaseStatus s : CaseStatus.values()) {
            statusFilter.addItem(s.getLabel());
        }
        statusFilter.setSelectedItem(CaseStatus.OPEN.getLabel());
        place(statusFilter, 72, 12, 190, 30);
        addLabel("Decision", 275, 12, 65, 30);
        decisionFilter.addItem(ALL);
        decisionFilter.addItem(Decision.FLAGGED.getLabel());
        decisionFilter.addItem(Decision.BLOCKED.getLabel());
        place(decisionFilter, 340, 12, 110, 30);
        addLabel("Search", 465, 12, 55, 30);
        search = addTextField(520, 12, 220, 30);
        JButton find = addButton(UITheme.primaryButton("Search"), 750, 12, 90, 30);
        JButton reload = addButton(UITheme.accentButton("Reload"), 850, 12, 90, 30);
        count = addMuted(content, "", 955, 16, 230, 22);

        UITheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(20, 52, 1155, 285);
        content.add(scroll);

        addSection("Case Report", 20, 344, 300);
        report.setEditable(false);
        report.setFont(UITheme.mono(12));
        report.setText("\n  Select a case in the table to see its detailed report.");
        JScrollPane rs = new JScrollPane(report);
        rs.setBounds(20, 370, 720, 232);
        content.add(rs);

        JPanel side = UITheme.card();
        side.setBounds(755, 370, 420, 232);
        content.add(side);
        addValue(side, "Algorithm scores", 14, 8, 300, 20);
        scoreRow(side, "Rule Engine", ruleBar, 34);
        scoreRow(side, "Statistical Anomaly", statBar, 60);
        scoreRow(side, "ML Model", mlBar, 86);
        scoreRow(side, "Combined risk", totalBar, 112);
        addLabel(side, "Analyst notes", 14, 146, 120, 24);
        notes = new JTextField();
        notes.setFont(UITheme.font(Font.PLAIN, 13));
        notes.setBounds(130, 146, 274, 26);
        side.add(notes);
        confirm = UITheme.dangerButton("Confirm Fraud");
        confirm.setBounds(14, 184, 190, 36);
        side.add(confirm);
        falsePositive = UITheme.button("False Positive", UITheme.SUCCESS);
        falsePositive.setBounds(214, 184, 190, 36);
        side.add(falsePositive);

        JButton print = addButton(UITheme.primaryButton("Print Report"), 20, 610, 160, 36);
        JButton summary = addButton(UITheme.button("Summary Report", UITheme.PURPLE), 190, 610, 170, 36);
        JButton close = addButton(UITheme.greyButton("Close"), 1055, 610, 120, 36);

        statusFilter.addActionListener(e -> showFiltered());
        decisionFilter.addActionListener(e -> showFiltered());
        find.addActionListener(e -> showFiltered());
        search.addActionListener(e -> showFiltered());
        reload.addActionListener(e -> load());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        confirm.addActionListener(e -> review(CaseStatus.CONFIRMED_FRAUD));
        falsePositive.addActionListener(e -> review(CaseStatus.FALSE_POSITIVE));
        print.addActionListener(e -> {
            try {
                report.print();
            } catch (PrinterException ex) {
                MessageUtil.error(this, "Could not print: " + ex.getMessage());
            }
        });
        summary.addActionListener(e -> showSummary());
        close.addActionListener(e -> dispose());
        setReviewEnabled(false);
        load();
    }

    private static JProgressBar bar() {
        JProgressBar b = new JProgressBar(0, 100);
        b.setStringPainted(true);
        b.setString("-");
        return b;
    }

    private void scoreRow(JPanel side, String name, JProgressBar b, int y) {
        addLabel(side, name, 14, y, 150, 20).setFont(UITheme.font(Font.PLAIN, 13));
        b.setBounds(165, y + 2, 239, 18);
        side.add(b);
    }

    private void load() {
        count.setText("Loading...");
        BackgroundTask.run(this, () -> new FraudCaseDAO().findAll(), list -> {
            loaded = list;
            showFiltered();
        });
    }

    private void showFiltered() {
        String status = (String) statusFilter.getSelectedItem();
        String decision = (String) decisionFilter.getSelectedItem();
        String text = search.getText().trim().toLowerCase();
        shown = loaded.stream()
                .filter(c -> ALL.equals(status) || c.getStatus().getLabel().equals(status))
                .filter(c -> ALL.equals(decision) || c.getTransaction().getDecision().getLabel().equals(decision))
                .filter(c -> text.isEmpty() || c.getTransaction().getCustomerName().toLowerCase().contains(text)
                        || c.getTransaction().getPayee().toLowerCase().contains(text)
                        || String.valueOf(c.getCaseId()).equals(text))
                .collect(Collectors.toList());
        table.setModel(TableModelBuilder.build(COLUMNS, shown, (FraudCase c) -> {
            Transaction t = c.getTransaction();
            return new Object[] {c.getCaseId(), DateUtil.formatShort(t.getTime()), t.getCustomerName(), t.getAmount(),
                t.getPayee(), t.getCategory(), t.getCity(), new RiskValue(t.getRiskScore()), t.getDecision(),
                c.getStatus(), c.getUserResponse()};
        }));
        UITheme.setColumnWidths(table, 50, 110, 135, 95, 145, 120, 160, 50, 70, 115, 140);
        long open = loaded.stream().filter(c -> c.getStatus() == CaseStatus.OPEN).count();
        count.setText(shown.size() + " shown  |  " + open + " open");
        report.setText("\n  Select a case in the table to see its detailed report.");
        setReviewEnabled(false);
    }

    private FraudCase selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : shown.get(table.convertRowIndexToModel(row));
    }

    private void showSelected() {
        FraudCase c = selected();
        if (c == null) {
            return;
        }
        Transaction t = c.getTransaction();
        report.setText(buildReport(c));
        report.setCaretPosition(0);
        setBar(ruleBar, t.getRuleScore());
        setBar(statBar, t.getStatScore());
        setBar(mlBar, t.getMlScore());
        setBar(totalBar, t.getRiskScore());
        notes.setText(c.getAdminNotes() == null ? "" : c.getAdminNotes());
        setReviewEnabled(c.getStatus() == CaseStatus.OPEN);
    }

    private void setBar(JProgressBar b, double score) {
        DetectionConfig cfg = ConfigManager.get();
        b.setValue((int) Math.round(score * 100));
        b.setString(String.format("%.0f%%", score * 100));
        b.setForeground(UITheme.riskColor(score, cfg.getAlertThreshold(), cfg.getBlockThreshold()));
    }

    private void setReviewEnabled(boolean enabled) {
        confirm.setEnabled(enabled);
        falsePositive.setEnabled(enabled);
        notes.setEditable(enabled);
    }

    /** Detailed text report of one case. */
    static String buildReport(FraudCase c) {
        Transaction t = c.getTransaction();
        String line = "-".repeat(78) + "\n";
        StringBuilder sb = new StringBuilder();
        sb.append(" FRAUD CASE REPORT  #").append(c.getCaseId()).append("    Status: ").append(c.getStatus().getLabel()).append('\n');
        sb.append(line);
        sb.append(String.format(" Customer      : %s (ID %d)%n", t.getCustomerName(), t.getUserId()));
        sb.append(String.format(" Transaction   : #%d  %s  on %s%n", t.getTxnId(), t.getType().getLabel(), DateUtil.format(t.getTime())));
        sb.append(String.format(" Amount        : %s  to  %s%n", MoneyUtil.format(t.getAmount()), t.getPayee()));
        sb.append(String.format(" Category      : %s (merchant risk %.0f%%)%n", t.getCategory(), t.getCategory().getRisk() * 100));
        sb.append(String.format(" Location      : %s     Device: %s%n", t.getCity(), t.getDevice()));
        sb.append(String.format(" Decision      : %s%s%n", t.getDecision().getLabel().toUpperCase(),
                t.getDecision() == Decision.BLOCKED ? " (money NOT debited)" : " (money debited)"));
        sb.append(line);
        sb.append(String.format(" Risk scores   : Combined %.0f%%  |  Rules %.0f%%  |  Statistics %.0f%%  |  ML %.0f%%%n",
                t.getRiskScore() * 100, t.getRuleScore() * 100, t.getStatScore() * 100, t.getMlScore() * 100));
        sb.append(" Model version : ").append(t.getModelVersion() == null ? "-" : "v" + t.getModelVersion()).append('\n');
        sb.append(line).append(" REASONS\n");
        for (String r : c.getReasons().split("\n")) {
            sb.append("  * ").append(r).append('\n');
        }
        sb.append(line);
        sb.append(" Customer's answer : ").append(c.getUserResponse().getLabel()).append('\n');
        if (c.getStatus() != fraud.detection.model.CaseStatus.OPEN) {
            sb.append(" Reviewed by       : ").append(c.getReviewedBy()).append(" on ").append(DateUtil.format(c.getReviewedAt())).append('\n');
            sb.append(" Analyst notes     : ").append(c.getAdminNotes() == null || c.getAdminNotes().isEmpty() ? "-" : c.getAdminNotes()).append('\n');
        }
        return sb.toString();
    }

    private void review(CaseStatus status) {
        FraudCase c = selected();
        if (c == null) {
            MessageUtil.error(this, "Please select a case first.");
            return;
        }
        String note = notes.getText().trim();
        if (note.length() > 300) {
            MessageUtil.error(this, new InvalidInputException("Notes can be at most 300 characters."));
            return;
        }
        String extra = status == CaseStatus.CONFIRMED_FRAUD && c.getTransaction().getDecision() == Decision.FLAGGED
                ? "\n\nThe customer will be REFUNDED " + MoneyUtil.format(c.getTransaction().getAmount()) + "." : "";
        if (!MessageUtil.confirm(this, "Mark case #" + c.getCaseId() + " as " + status.getLabel().toUpperCase() + "?" + extra)) {
            return;
        }
        setReviewEnabled(false);
        BackgroundTask.run(this, () -> new CaseService().review(c.getCaseId(), status, note, admin), msg -> {
            MessageUtil.info(this, msg);
            onChange.run();
            load();
        }, () -> {
            FraudCase still = selected();
            setReviewEnabled(still != null && still.getStatus() == CaseStatus.OPEN);
        });
    }

    /** Overall report of all loaded cases. */
    private void showSummary() {
        long open = loaded.stream().filter(c -> c.getStatus() == CaseStatus.OPEN).count();
        long fraud = loaded.stream().filter(c -> c.getStatus() == CaseStatus.CONFIRMED_FRAUD).count();
        long fp = loaded.stream().filter(c -> c.getStatus() == CaseStatus.FALSE_POSITIVE).count();
        double avg = loaded.stream().mapToDouble(c -> c.getTransaction().getRiskScore()).average().orElse(0);
        StringBuilder sb = new StringBuilder();
        sb.append(" DETECTION SUMMARY REPORT  -  ").append(DateUtil.format(java.time.LocalDateTime.now())).append("\n\n");
        sb.append(String.format(" Total cases           : %d%n", loaded.size()));
        sb.append(String.format(" Open (needs review)   : %d%n", open));
        sb.append(String.format(" Confirmed fraud       : %d%n", fraud));
        sb.append(String.format(" False positives       : %d%n", fp));
        sb.append(String.format(" Average risk score    : %.0f%%%n", avg * 100));
        sb.append(String.format(" Precision (reviewed)  : %s%n", fraud + fp == 0 ? "-" : String.format("%.0f%%", 100.0 * fraud / (fraud + fp))));
        sb.append("\n Cases by category:\n");
        loaded.stream().collect(Collectors.groupingBy(c -> c.getTransaction().getCategory(), java.util.TreeMap::new, Collectors.counting()))
                .forEach((cat, n) -> sb.append(String.format("   %-20s %d%n", cat.getLabel(), n)));
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(UITheme.mono(13));
        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(520, 380));
        JOptionPane.showMessageDialog(this, pane, "Summary Report", JOptionPane.PLAIN_MESSAGE);
    }
}
