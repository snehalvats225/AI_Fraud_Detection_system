package fraud.detection.ui;

import fraud.detection.model.AlertSeverity;
import fraud.detection.model.CaseStatus;
import fraud.detection.model.Decision;
import fraud.detection.model.UserResponse;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Colours table cells by meaning - decisions, case status, alert severity, risk.
 * It OVERRIDES getTableCellRendererComponent(); Swing calls our version for every
 * cell (POLYMORPHISM through inheritance).
 */
public class StatusCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Object shown = value instanceof BigDecimal ? new DecimalFormat("#,##0.00").format(value) : value;
        Component c = super.getTableCellRendererComponent(table, shown, isSelected, hasFocus, row, column);
        c.setFont(table.getFont());
        c.setForeground(UITheme.TEXT);
        setHorizontalAlignment(value instanceof Number || value instanceof RiskValue ? SwingConstants.RIGHT : SwingConstants.LEFT);
        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? UITheme.CARD : UITheme.BACKGROUND);
        }
        Color color = colorFor(value);
        if (color != null) {
            c.setForeground(color);
            c.setFont(table.getFont().deriveFont(Font.BOLD));
        }
        return c;
    }

    static Color colorFor(Object value) {
        if (value instanceof Decision) {
            Decision d = (Decision) value;
            return d == Decision.APPROVED ? UITheme.SUCCESS : d == Decision.FLAGGED ? UITheme.WARNING : UITheme.DANGER;
        }
        if (value instanceof CaseStatus) {
            CaseStatus s = (CaseStatus) value;
            return s == CaseStatus.OPEN ? UITheme.WARNING : s == CaseStatus.CONFIRMED_FRAUD ? UITheme.DANGER : UITheme.SUCCESS;
        }
        if (value instanceof AlertSeverity) {
            AlertSeverity s = (AlertSeverity) value;
            return s == AlertSeverity.HIGH ? UITheme.DANGER : s == AlertSeverity.MEDIUM ? UITheme.WARNING : UITheme.PRIMARY_LIGHT;
        }
        if (value instanceof UserResponse) {
            UserResponse r = (UserResponse) value;
            return r == UserResponse.REPORTED_FRAUD ? UITheme.DANGER : r == UserResponse.CONFIRMED_GENUINE ? UITheme.SUCCESS : UITheme.MUTED;
        }
        if (value instanceof RiskValue) {
            return ((RiskValue) value).color();
        }
        return null;
    }
}
