package fraud.detection.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

/** Colours, fonts and ready-made styled components, so every screen looks the same. */
public final class UITheme {

    public static final String APP_NAME = "FraudShield AI";

    public static final Color PRIMARY = new Color(15, 23, 42);
    public static final Color PRIMARY_LIGHT = new Color(37, 99, 235);
    public static final Color ACCENT = new Color(13, 148, 136);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color PURPLE = new Color(124, 58, 237);
    public static final Color BACKGROUND = new Color(241, 245, 249);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(214, 220, 229);
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color GRID = new Color(226, 232, 240);

    private UITheme() {
    }

    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static Font mono(int size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    public static JButton button(String text, Color background) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 13));
        b.setBackground(background);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton primaryButton(String text) { return button(text, PRIMARY_LIGHT); }
    public static JButton accentButton(String text)  { return button(text, ACCENT); }
    public static JButton dangerButton(String text)  { return button(text, DANGER); }
    public static JButton greyButton(String text)    { return button(text, MUTED); }

    public static JPanel card() {
        JPanel p = new JPanel(null);
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createLineBorder(BORDER));
        return p;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFont(font(Font.PLAIN, 13));
        table.setGridColor(GRID);
        table.setSelectionBackground(new Color(191, 219, 254));
        table.setSelectionForeground(TEXT);
        table.setAutoCreateRowSorter(true);          // click a column header to sort
        table.setFillsViewportHeight(true);
        table.setBackground(CARD);
        StatusCellRenderer renderer = new StatusCellRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Number.class, renderer);
        JTableHeader header = table.getTableHeader();
        header.setFont(font(Font.BOLD, 13));
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
    }

    public static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Green below the alert threshold, orange up to the block threshold, red above. */
    public static Color riskColor(double risk, double alert, double block) {
        return risk >= block ? DANGER : risk >= alert ? WARNING : SUCCESS;
    }
}
