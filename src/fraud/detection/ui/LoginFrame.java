package fraud.detection.ui;

import fraud.detection.dao.UserDAO;
import fraud.detection.model.Admin;
import fraud.detection.model.Customer;
import fraud.detection.model.Person;
import fraud.detection.model.Role;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.MessageUtil;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Login for both the admin and customers. */
public class LoginFrame extends BaseFrame implements ActionListener {

    private final JComboBox<Role> role = new JComboBox<>(Role.values());     // generic Swing component
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JButton login, exit, register;

    public LoginFrame() {
        super("Secure Login", "Sign in to the fraud detection system", 580, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel card = UITheme.card();
        card.setBounds(55, 22, 455, 290);
        content.add(card);
        addValue(card, "Login As", 30, 25, 110, 30);
        role.setFont(UITheme.font(Font.PLAIN, 14));
        role.setSelectedItem(Role.CUSTOMER);
        role.setBounds(150, 25, 265, 30);
        card.add(role);
        addValue(card, "Username", 30, 75, 110, 30);
        username.setFont(UITheme.font(Font.PLAIN, 14));
        username.setBounds(150, 75, 265, 30);
        card.add(username);
        addValue(card, "Password", 30, 125, 110, 30);
        password.setFont(UITheme.font(Font.PLAIN, 14));
        password.setBounds(150, 125, 265, 30);
        card.add(password);

        login = UITheme.primaryButton("Login");
        login.setBounds(150, 180, 128, 38);
        card.add(login);
        exit = UITheme.greyButton("Exit");
        exit.setBounds(287, 180, 128, 38);
        card.add(exit);
        JLabel hint1 = addMuted(card, "Admin:  admin / admin123  (Login As: Admin)", 30, 230, 400, 20);
        JLabel hint2 = addMuted(card, "Demo customers:  rahul, priya, amit, sneha, vikram / user123", 30, 252, 420, 20);
        hint1.setFont(UITheme.font(Font.ITALIC, 12));
        hint2.setFont(UITheme.font(Font.ITALIC, 12));

        register = addButton(UITheme.accentButton("Open a New Customer Account"), 55, 325, 455, 40);
        for (JButton b : new JButton[] {login, exit, register}) {
            b.addActionListener(this);
        }
        getRootPane().setDefaultButton(login);       // Enter key = Login
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exit) {
            System.exit(0);
        } else if (e.getSource() == register) {
            dispose();
            new RegisterFrame().setVisible(true);
        } else {
            doLogin();
        }
    }

    private void doLogin() {
        String user = username.getText().trim();
        String pass = new String(password.getPassword());
        Role selected = (Role) role.getSelectedItem();
        if (user.isEmpty() || pass.isEmpty()) {
            MessageUtil.error(this, "Please enter your username and password.");
            return;
        }
        login.setEnabled(false);
        login.setText("Checking...");
        BackgroundTask.run(this, () -> new UserDAO().authenticate(user, pass, selected), (Person person) -> {
            dispose();
            // Same variable type (Person), different real object -> different dashboard
            if (person instanceof Admin) {
                new AdminDashboard((Admin) person).setVisible(true);
            } else {
                new CustomerDashboard((Customer) person).setVisible(true);
            }
        }, () -> {
            login.setEnabled(true);
            login.setText("Login");
            password.setText("");
        });
    }

    public static void open() {
        new LoginFrame().setVisible(true);
    }
}
