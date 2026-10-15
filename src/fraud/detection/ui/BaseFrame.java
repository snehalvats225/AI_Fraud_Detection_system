package fraud.detection.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * ABSTRACT parent of every window (INHERITANCE).
 * It sets the size, centres the window, adds the header and a content panel and
 * offers protected helper methods, so the child screens stay short.
 */
public abstract class BaseFrame extends JFrame {

    protected final JPanel content;

    protected BaseFrame(String title, String subtitle, int width, int height) {
        super(title + " | " + UITheme.APP_NAME);
        setSize(width, height);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new HeaderPanel(title, subtitle), BorderLayout.NORTH);
        content = new JPanel(null);
        content.setBackground(UITheme.BACKGROUND);
        getContentPane().add(content, BorderLayout.CENTER);
    }

    protected JLabel addLabel(String text, int x, int y, int w, int h) {
        return addLabel(content, text, x, y, w, h);
    }

    protected JLabel addLabel(JComponent parent, String text, int x, int y, int w, int h) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.font(Font.PLAIN, 14));
        l.setForeground(UITheme.TEXT);
        l.setBounds(x, y, w, h);
        parent.add(l);
        return l;
    }

    protected JLabel addValue(JComponent parent, String text, int x, int y, int w, int h) {
        JLabel l = addLabel(parent, text, x, y, w, h);
        l.setFont(UITheme.font(Font.BOLD, 14));
        return l;
    }

    protected JLabel addMuted(JComponent parent, String text, int x, int y, int w, int h) {
        JLabel l = addLabel(parent, text, x, y, w, h);
        l.setForeground(UITheme.MUTED);
        l.setFont(UITheme.font(Font.PLAIN, 13));
        return l;
    }

    protected JLabel addHint(String text, int x, int y, int w) {
        JLabel l = addLabel(text, x, y, w, 16);
        l.setFont(UITheme.font(Font.ITALIC, 11));
        l.setForeground(UITheme.MUTED);
        return l;
    }

    protected JLabel addSection(String text, int x, int y, int w) {
        JLabel l = addLabel(text, x, y, w, 24);
        l.setFont(UITheme.font(Font.BOLD, 14));
        l.setForeground(UITheme.PRIMARY_LIGHT);
        return l;
    }

    protected JTextField addTextField(int x, int y, int w, int h) {
        JTextField f = new JTextField();
        f.setFont(UITheme.font(Font.PLAIN, 14));
        f.setBounds(x, y, w, h);
        content.add(f);
        return f;
    }

    protected JPasswordField addPasswordField(int x, int y, int w, int h) {
        JPasswordField f = new JPasswordField();
        f.setFont(UITheme.font(Font.PLAIN, 14));
        f.setBounds(x, y, w, h);
        content.add(f);
        return f;
    }

    protected JButton addButton(JButton button, int x, int y, int w, int h) {
        button.setBounds(x, y, w, h);
        content.add(button);
        return button;
    }

    /** GENERIC METHOD: places any component type and returns it with its real type. */
    protected <T extends JComponent> T place(T component, int x, int y, int w, int h) {
        component.setBounds(x, y, w, h);
        component.setFont(UITheme.font(Font.PLAIN, 14));
        content.add(component);
        return component;
    }

    /** A white card with a coloured top stripe, a title and a big value label. */
    protected JLabel statCard(String title, int x, int y, int w, int h, Color color) {
        JPanel card = UITheme.card();
        card.setBounds(x, y, w, h);
        content.add(card);
        JPanel stripe = new JPanel();
        stripe.setBackground(color);
        stripe.setBounds(0, 0, w, 5);
        card.add(stripe);
        addMuted(card, title, 14, 12, w - 20, 20);
        JLabel value = addValue(card, "...", 14, 36, w - 20, 40);
        value.setFont(UITheme.font(Font.BOLD, 26));
        value.setForeground(color);
        return value;
    }
}
