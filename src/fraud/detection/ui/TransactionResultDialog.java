package fraud.detection.ui;

import fraud.detection.engine.Assessment;
import fraud.detection.model.Decision;
import fraud.detection.model.DetectionConfig;
import fraud.detection.model.TransactionRequest;
import fraud.detection.service.ConfigManager;
import fraud.detection.service.TransactionResult;
import fraud.detection.util.MoneyUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Shows the fraud verdict: decision banner, risk gauge, each algorithm's score and the reasons. */
public class TransactionResultDialog extends JDialog {

    public TransactionResultDialog(Frame owner, TransactionRequest req, TransactionResult result) {
        super(owner, "Transaction Result | " + UITheme.APP_NAME, true);
        Assessment a = result.getAssessment();
        DetectionConfig cfg = ConfigManager.get();
        setSize(660, 600);
        setResizable(false);
        setLocationRelativeTo(owner);
        JPanel p = new JPanel(null);
        p.setBackground(UITheme.BACKGROUND);
        setContentPane(p);

        Decision d = a.getDecision();
        Color color = d == Decision.APPROVED ? UITheme.SUCCESS : d == Decision.FLAGGED ? UITheme.WARNING : UITheme.DANGER;
        JPanel banner = new JPanel(null);
        banner.setBackground(color);
        banner.setBounds(0, 0, 660, 74);
        p.add(banner);
        String title = d == Decision.APPROVED ? "PAYMENT APPROVED"
                : d == Decision.FLAGGED ? "PAYMENT COMPLETED - FLAGGED AS SUSPICIOUS" : "PAYMENT BLOCKED FOR YOUR SAFETY";
        label(banner, title, 20, 10, 620, 28, Font.BOLD, 19, Color.WHITE);
        label(banner, MoneyUtil.format(req.getAmount()) + " to " + req.getPayee() + "   |   Transaction #" + result.getTxnId()
                + "   |   Balance: " + MoneyUtil.format(result.getBalanceAfter()), 20, 40, 620, 22, Font.PLAIN, 13, Color.WHITE);

        RiskMeter meter = new RiskMeter(a.getRiskScore(), cfg.getAlertThreshold(), cfg.getBlockThreshold());
        meter.setBounds(10, 82, 280, 206);
        p.add(meter);

        int y = 96;
        label(p, "Score from each algorithm", 310, y, 320, 20, Font.BOLD, 13, UITheme.TEXT);
        y += 28;
        y = scoreBar(p, "Rule Engine", a.getRuleScore(), cfg.getRuleWeight(), y, cfg);
        y = scoreBar(p, "Statistical Anomaly", a.getStatScore(), cfg.getStatWeight(), y, cfg);
        scoreBar(p, "ML Model" + (a.getModelVersion() == null ? "" : " (v" + a.getModelVersion() + ")"), a.getMlScore(),
                cfg.getMlWeight(), y, cfg);

        label(p, "Why?", 20, 292, 300, 20, Font.BOLD, 13, UITheme.TEXT);
        JTextArea reasons = new JTextArea();
        reasons.setEditable(false);
        reasons.setLineWrap(true);
        reasons.setWrapStyleWord(true);
        reasons.setFont(UITheme.font(Font.PLAIN, 13));
        StringBuilder sb = new StringBuilder();
        for (String r : a.getResult().getReasons()) {
            sb.append("- ").append(r).append('\n');
        }
        reasons.setText(sb.length() == 0 ? "- Nothing unusual - this matches your normal behaviour." : sb.toString());
        reasons.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(reasons);
        scroll.setBounds(20, 314, 610, 158);
        p.add(scroll);

        String footer = d == Decision.APPROVED ? "No action needed."
                : d == Decision.FLAGGED ? "An alert was added to your dashboard. If this was not you, choose 'Not me - Report Fraud'."
                : "No money was debited. If this was you, choose 'It was me' on your dashboard so the bank can review it.";
        label(p, footer, 20, 478, 620, 22, Font.ITALIC, 12, UITheme.MUTED);
        JButton ok = UITheme.button("OK", color);
        ok.setBounds(510, 508, 120, 38);
        ok.addActionListener(e -> dispose());
        p.add(ok);
        getRootPane().setDefaultButton(ok);
    }

    private static int scoreBar(JPanel p, String name, double score, double weight, int y, DetectionConfig cfg) {
        label(p, name, 310, y, 220, 18, Font.PLAIN, 12, UITheme.TEXT);
        label(p, String.format("weight %.2f", weight), 540, y, 90, 18, Font.PLAIN, 11, UITheme.MUTED);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(score * 100));
        bar.setStringPainted(true);
        bar.setString(String.format("%.0f%%", score * 100));
        bar.setForeground(UITheme.riskColor(score, cfg.getAlertThreshold(), cfg.getBlockThreshold()));
        bar.setBounds(310, y + 20, 320, 18);
        p.add(bar);
        return y + 48;
    }

    private static JLabel label(JPanel parent, String text, int x, int y, int w, int h, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setBounds(x, y, w, h);
        l.setFont(UITheme.font(style, size));
        l.setForeground(color);
        parent.add(l);
        return l;
    }
}
