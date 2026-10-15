package fraud.detection.ui;

import fraud.detection.exception.FraudSystemException;
import fraud.detection.model.Admin;
import fraud.detection.model.DetectionConfig;
import fraud.detection.service.ConfigManager;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.DateUtil;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.MoneyUtil;
import fraud.detection.util.Validator;
import java.awt.Font;
import java.math.BigDecimal;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * SYSTEM CONFIGURATION: detection thresholds and algorithm parameters.
 * Input: settings. Output: confirmation of a successful update.
 * Changes take effect for the very next transaction (no restart needed).
 */
public class ConfigurationFrame extends BaseFrame {

    private final Admin admin;
    private final Runnable onSaved;
    private final JTextField alert, block, highAmount, nightStart, nightEnd, velocity, ruleW, statW, mlW, lr, epochs, l2;
    private final JLabel lastUpdated;

    public ConfigurationFrame(Admin admin, Runnable onSaved) {
        super("Configuration Settings", "Detection thresholds and algorithm parameters", 1000, 580);
        this.admin = admin;
        this.onSaved = onSaved;
        int left = 30, right = 515;

        addSection("Detection Thresholds", left, 12, 440);
        alert = row("Alert threshold (0-1)", "risk at or above this -> FLAGGED + customer alert", left, 42);
        block = row("Block threshold (0-1)", "risk at or above this -> BLOCKED (payment declined)", left, 92);
        addSection("Rule Engine Parameters", left, 148, 440);
        highAmount = row("High-value limit (Rs.)", "amounts above this trigger the high-value rule", left, 178);
        nightStart = row("Night window starts (hour 0-23)", "e.g. 0 = midnight", left, 228);
        nightEnd = row("Night window ends (hour 0-23)", "e.g. 5 = 5 a.m.", left, 278);
        velocity = row("Velocity limit (txns per hour)", "more payments than this in one hour is suspicious", left, 328);

        addSection("Algorithm Weights (ensemble)", right, 12, 440);
        ruleW = row("Rule Engine weight", "0 switches an algorithm off", right, 42);
        statW = row("Statistical Anomaly weight", "weights are normalised automatically", right, 92);
        mlW = row("ML Model weight", "the trained logistic-regression model", right, 142);
        addSection("ML Training Parameters (used by Retrain)", right, 198, 460);
        lr = row("Learning rate", "gradient-descent step size (0.001-2)", right, 228);
        epochs = row("Epochs", "passes over the training data (10-5000)", right, 278);
        l2 = row("L2 regularisation", "keeps weights small, prevents over-fitting (0-1)", right, 328);

        lastUpdated = addMuted(content, "", left, 392, 600, 20);
        JButton save = addButton(UITheme.accentButton("Save Configuration"), 515, 420, 200, 40);
        JButton reset = addButton(UITheme.button("Load Defaults", UITheme.WARNING), 725, 420, 130, 40);
        JButton close = addButton(UITheme.greyButton("Close"), 865, 420, 100, 40);
        save.addActionListener(e -> save());
        reset.addActionListener(e -> {
            fill(DetectionConfig.defaults());
            MessageUtil.info(this, "Default values loaded into the form.\nClick 'Save Configuration' to apply them.");
        });
        close.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);
        fill(ConfigManager.get());
        showUpdated(ConfigManager.get());
    }

    /** One labelled input with a small hint underneath. */
    private JTextField row(String label, String hint, int x, int y) {
        addLabel(label, x, y, 240, 26);
        JTextField f = addTextField(x + 245, y, 120, 26);
        JLabel h = addMuted(content, hint, x, y + 26, 450, 18);
        h.setFont(UITheme.font(Font.ITALIC, 11));
        return f;
    }

    private void fill(DetectionConfig c) {
        alert.setText(String.valueOf(c.getAlertThreshold()));
        block.setText(String.valueOf(c.getBlockThreshold()));
        highAmount.setText(c.getHighAmountLimit().stripTrailingZeros().toPlainString());
        nightStart.setText(String.valueOf(c.getNightStart()));
        nightEnd.setText(String.valueOf(c.getNightEnd()));
        velocity.setText(String.valueOf(c.getVelocityLimit()));
        ruleW.setText(String.valueOf(c.getRuleWeight()));
        statW.setText(String.valueOf(c.getStatWeight()));
        mlW.setText(String.valueOf(c.getMlWeight()));
        lr.setText(String.valueOf(c.getLearningRate()));
        epochs.setText(String.valueOf(c.getEpochs()));
        l2.setText(String.valueOf(c.getL2Lambda()));
    }

    private void showUpdated(DetectionConfig c) {
        lastUpdated.setText("Last updated by " + c.getUpdatedBy()
                + (c.getUpdatedAt() == null ? "" : " on " + DateUtil.format(c.getUpdatedAt())));
    }

    private void save() {
        try {
            // BUILDER pattern: start from the current settings and change the fields from the form
            DetectionConfig requested = ConfigManager.get().toBuilder()
                    .alertThreshold(Validator.decimal("Alert threshold", alert.getText(), 0, 1))
                    .blockThreshold(Validator.decimal("Block threshold", block.getText(), 0, 1))
                    .highAmountLimit(BigDecimal.valueOf(Validator.decimal("High-value limit", highAmount.getText(), 0, 10_000_000)))
                    .nightStart(Validator.integer("Night start", nightStart.getText(), 0, 23))
                    .nightEnd(Validator.integer("Night end", nightEnd.getText(), 0, 23))
                    .velocityLimit(Validator.integer("Velocity limit", velocity.getText(), 1, 50))
                    .ruleWeight(Validator.decimal("Rule weight", ruleW.getText(), 0, 10))
                    .statWeight(Validator.decimal("Statistical weight", statW.getText(), 0, 10))
                    .mlWeight(Validator.decimal("ML weight", mlW.getText(), 0, 10))
                    .learningRate(Validator.decimal("Learning rate", lr.getText(), 0, 10))
                    .epochs(Validator.integer("Epochs", epochs.getText(), 1, 100000))
                    .l2Lambda(Validator.decimal("L2 regularisation", l2.getText(), 0, 10))
                    .build();
            BackgroundTask.run(this, () -> ConfigManager.update(requested, admin), (DetectionConfig saved) -> {
                showUpdated(saved);
                onSaved.run();
                MessageUtil.info(this, "Configuration updated successfully.\n\n"
                        + String.format("Alert at %.2f, block at %.2f%n", saved.getAlertThreshold(), saved.getBlockThreshold())
                        + "High-value limit: " + MoneyUtil.format(saved.getHighAmountLimit()) + "\n"
                        + String.format("Weights - rules %.2f, statistics %.2f, ML %.2f%n", saved.getRuleWeight(),
                                saved.getStatWeight(), saved.getMlWeight())
                        + "\nThe new settings apply to the next transaction immediately.");
            });
        } catch (FraudSystemException ex) {
            MessageUtil.error(this, ex.getMessage());
        }
    }
}
