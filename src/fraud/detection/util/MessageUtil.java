package fraud.detection.util;

import fraud.detection.exception.DatabaseException;
import fraud.detection.exception.FraudSystemException;
import java.awt.Component;
import javax.swing.JOptionPane;

/** Pop-up messages, so errors are visible to the user (not only in the console). */
public final class MessageUtil {

    private MessageUtil() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void error(Component parent, Throwable error) {
        if (!(error instanceof FraudSystemException) || error instanceof DatabaseException) {
            error.printStackTrace();          // unexpected problems also go to the NetBeans Output panel
        }
        error(parent, error.getMessage() != null ? error.getMessage() : error.toString());
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
