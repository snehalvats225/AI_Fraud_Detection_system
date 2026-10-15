package fraud.detection.ui;

import fraud.detection.dao.UserDAO;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.exception.InvalidInputException;
import fraud.detection.model.Cities;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.MoneyUtil;
import fraud.detection.util.Validator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Account opening form for new customers. */
public class RegisterFrame extends BaseFrame implements ActionListener {

    private static final BigDecimal MIN_DEPOSIT = new BigDecimal("1000");

    private final JTextField fullName, email, phone, deposit, username;
    private final JComboBox<String> homeCity = new JComboBox<>(Cities.DOMESTIC.toArray(new String[0]));
    private final JPasswordField password, confirm;
    private final JButton submit, back;

    public RegisterFrame() {
        super("Open a Customer Account", "Your transactions will be protected by AI fraud monitoring", 640, 650);
        int lx = 45, fx = 230, fw = 340, y = 22, step = 44;
        addLabel("Full Name", lx, y, 170, 30);           fullName = addTextField(fx, y, fw, 30); y += step;
        addLabel("Email", lx, y, 170, 30);               email = addTextField(fx, y, fw, 30); y += step;
        addLabel("Mobile Number", lx, y, 170, 30);       phone = addTextField(fx, y, fw, 30); y += step;
        addLabel("Home City", lx, y, 170, 30);           place(homeCity, fx, y, fw, 30); y += 32;
        addHint("payments from other cities count as 'new location' for fraud checks", fx, y, fw + 30);
        y += 22;
        addLabel("Opening Deposit (Rs.)", lx, y, 175, 30); deposit = addTextField(fx, y, fw, 30); y += step;
        addLabel("Choose Username", lx, y, 170, 30);     username = addTextField(fx, y, fw, 30); y += step;
        addLabel("Password", lx, y, 170, 30);            password = addPasswordField(fx, y, fw, 30); y += step;
        addLabel("Confirm Password", lx, y, 170, 30);    confirm = addPasswordField(fx, y, fw, 30); y += 34;
        addHint("min 6 characters, with a letter and a digit", fx, y, fw);
        y += 32;
        submit = addButton(UITheme.accentButton("Open Account"), fx, y, 165, 38);
        back = addButton(UITheme.greyButton("Back to Login"), fx + 175, y, 165, 38);
        submit.addActionListener(this);
        back.addActionListener(this);
        getRootPane().setDefaultButton(submit);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == back) {
            dispose();
            LoginFrame.open();
            return;
        }
        try {
            String name = Validator.personName(fullName.getText());
            String mail = Validator.email(email.getText());
            String mob = Validator.phone(phone.getText());
            String city = (String) homeCity.getSelectedItem();
            BigDecimal amount = Validator.amount(deposit.getText());
            if (amount.compareTo(MIN_DEPOSIT) < 0) {
                throw new InvalidInputException("The opening deposit must be at least " + MoneyUtil.format(MIN_DEPOSIT) + ".");
            }
            String user = Validator.username(username.getText());
            String pass = Validator.password(new String(password.getPassword()));
            if (!pass.equals(new String(confirm.getPassword()))) {
                throw new InvalidInputException("Password and Confirm Password do not match.");
            }
            submit.setEnabled(false);
            BackgroundTask.run(this,
                    () -> new UserDAO().registerCustomer(name, mail, mob, city, user, pass, amount),
                    id -> {
                        MessageUtil.info(this, "Account opened successfully!\n\nCustomer ID : " + id
                                + "\nUsername    : " + user + "\nBalance     : " + MoneyUtil.format(amount)
                                + "\n\nYou can log in now (Login As: Customer).");
                        dispose();
                        LoginFrame.open();
                    }, () -> submit.setEnabled(true));
        } catch (FraudSystemException ex) {
            MessageUtil.error(this, ex.getMessage());
        }
    }
}
