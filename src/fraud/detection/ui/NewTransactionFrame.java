package fraud.detection.ui;

import fraud.detection.exception.FraudSystemException;
import fraud.detection.model.Cities;
import fraud.detection.model.Customer;
import fraud.detection.model.DeviceType;
import fraud.detection.model.MerchantCategory;
import fraud.detection.model.TransactionRequest;
import fraud.detection.model.TransactionType;
import fraud.detection.service.TransactionResult;
import fraud.detection.service.TransactionService;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.Validator;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** TRANSACTION MONITORING: the customer makes a payment and it is checked for fraud in real time. */
public class NewTransactionFrame extends BaseFrame implements ActionListener {

    private final Customer customer;
    private final Runnable onDone;
    private final JComboBox<TransactionType> type = new JComboBox<>(TransactionType.customerTypes());
    private final JComboBox<MerchantCategory> category = new JComboBox<>(MerchantCategory.values());
    private final JComboBox<String> city = new JComboBox<>(Cities.ALL.toArray(new String[0]));
    private final JComboBox<DeviceType> device = new JComboBox<>(DeviceType.values());
    private final JTextField payee, amount;
    private final JRadioButton now = new JRadioButton("Now");
    private final JRadioButton simulated = new JRadioButton("Simulate hour (demo):");
    private final JSpinner hour = new JSpinner(new SpinnerNumberModel(2, 0, 23, 1));
    private final JLabel status;
    private final JButton pay, cancel;

    public NewTransactionFrame(Customer customer, Runnable onDone) {
        super("New Transaction", "Every payment is checked by 3 fraud-detection algorithms in real time", 660, 600);
        this.customer = customer;
        this.onDone = onDone;
        int lx = 45, fx = 230, fw = 360, y = 22, step = 46;
        addLabel("Transaction Type", lx, y, 170, 30);   place(type, fx, y, fw, 30); y += step;
        addLabel("Pay To (Payee)", lx, y, 170, 30);     payee = addTextField(fx, y, fw, 30); y += step;
        addLabel("Amount (Rs.)", lx, y, 170, 30);       amount = addTextField(fx, y, fw, 30); y += step;
        amount.setFont(UITheme.font(Font.BOLD, 15));
        addLabel("Merchant Category", lx, y, 170, 30);  place(category, fx, y, fw, 30); y += step;
        addLabel("Location", lx, y, 170, 30);           place(city, fx, y, fw, 30); y += step;
        city.setSelectedItem(customer.getHomeCity());
        addLabel("Device / Channel", lx, y, 170, 30);   place(device, fx, y, fw, 30); y += step;
        device.setSelectedItem(DeviceType.UPI_APP);

        addLabel("Time", lx, y, 170, 30);
        ButtonGroup g = new ButtonGroup();
        g.add(now);
        g.add(simulated);
        now.setSelected(true);
        for (JRadioButton r : new JRadioButton[] {now, simulated}) {
            r.setBackground(UITheme.BACKGROUND);
        }
        place(now, fx, y, 70, 30);
        place(simulated, fx + 75, y, 190, 30);
        place(hour, fx + 270, y, 60, 30);
        hour.setEnabled(false);
        simulated.addActionListener(e -> hour.setEnabled(true));
        now.addActionListener(e -> hour.setEnabled(false));
        y += 32;
        addHint("lets you test night-time rules during the day", fx + 75, y, 290);
        y += 30;

        pay = addButton(UITheme.primaryButton("Pay Now"), fx, y, 175, 42);
        cancel = addButton(UITheme.greyButton("Cancel"), fx + 185, y, 175, 42);
        y += 50;
        status = addLabel("", lx, y, 560, 24);
        status.setFont(UITheme.font(Font.BOLD, 13));
        status.setForeground(UITheme.PURPLE);
        pay.addActionListener(this);
        cancel.addActionListener(this);
        getRootPane().setDefaultButton(pay);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancel) {
            dispose();
            return;
        }
        try {
            String to = Validator.requireText("Payee", payee.getText(), 80);
            BigDecimal value = Validator.amount(amount.getText());
            LocalDateTime time = LocalDateTime.now().withNano(0);
            if (simulated.isSelected()) {
                time = time.withHour((Integer) hour.getValue());
                if (time.isAfter(LocalDateTime.now())) {
                    time = time.minusDays(1);                     // keep it in the past
                }
            }
            TransactionRequest req = new TransactionRequest((TransactionType) type.getSelectedItem(), to, value,
                    (MerchantCategory) category.getSelectedItem(), (String) city.getSelectedItem(),
                    (DeviceType) device.getSelectedItem(), time);
            pay.setEnabled(false);
            status.setText("Analysing transaction with Rule Engine, Statistical model and ML model...");
            BackgroundTask.run(this, () -> new TransactionService().process(customer.getUserId(), req),
                    (TransactionResult result) -> {
                        status.setText("");
                        onDone.run();
                        dispose();
                        new TransactionResultDialog(null, req, result).setVisible(true);
                    }, () -> {
                        pay.setEnabled(true);
                        if (isDisplayable()) {
                            status.setText("");
                        }
                    });
        } catch (FraudSystemException ex) {
            MessageUtil.error(this, ex.getMessage());
        }
    }
}
