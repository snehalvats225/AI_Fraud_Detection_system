package fraud.detection.ui;

import fraud.detection.dao.UserDAO;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.exception.InvalidInputException;
import fraud.detection.model.Person;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.Validator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPasswordField;

/** Works for EVERY kind of user, because the parameter type is the parent class Person. */
public class ChangePasswordFrame extends BaseFrame implements ActionListener {

    private final Person user;
    private final JPasswordField current, newPass, confirm;
    private final JButton save, cancel;

    public ChangePasswordFrame(Person user) {
        super("Change Password", user.getRole() + " : " + user.getUsername(), 540, 380);
        this.user = user;
        addLabel("Current Password", 40, 30, 160, 30);  current = addPasswordField(215, 30, 270, 30);
        addLabel("New Password", 40, 80, 160, 30);      newPass = addPasswordField(215, 80, 270, 30);
        addLabel("Confirm Password", 40, 130, 160, 30); confirm = addPasswordField(215, 130, 270, 30);
        save = addButton(UITheme.accentButton("Update"), 215, 190, 130, 38);
        cancel = addButton(UITheme.greyButton("Cancel"), 355, 190, 130, 38);
        save.addActionListener(this);
        cancel.addActionListener(this);
        getRootPane().setDefaultButton(save);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cancel) {
            dispose();
            return;
        }
        try {
            String oldPw = new String(current.getPassword());
            String newPw = Validator.password(new String(newPass.getPassword()));
            if (!newPw.equals(new String(confirm.getPassword()))) {
                throw new InvalidInputException("New password and confirmation do not match.");
            }
            if (newPw.equals(oldPw)) {
                throw new InvalidInputException("New password must be different from the current one.");
            }
            save.setEnabled(false);
            BackgroundTask.run(this, () -> {
                new UserDAO().changePassword(user.getUserId(), oldPw, newPw);
                return Boolean.TRUE;
            }, ok -> {
                MessageUtil.info(this, "Password changed successfully.");
                dispose();
            }, () -> save.setEnabled(true));
        } catch (FraudSystemException ex) {
            MessageUtil.error(this, ex.getMessage());
        }
    }
}
