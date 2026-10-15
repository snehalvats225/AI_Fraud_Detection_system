package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.DatabaseException;
import fraud.detection.model.CaseStatus;
import fraud.detection.model.DashboardStats;
import fraud.detection.model.FeedbackSummary;
import fraud.detection.model.MerchantCategory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/** COUNT / SUM / GROUP BY queries for the admin dashboard and charts. */
public class ReportDAO {

    public static final int CHART_DAYS = 7;

    private static final String COUNTS =
            "SELECT (SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') AS customers, "
            + "(SELECT COUNT(*) FROM transactions WHERE txn_type <> 'REFUND') AS txns, "
            + "(SELECT COUNT(*) FROM transactions WHERE decision = 'FLAGGED') AS flagged, "
            + "(SELECT COUNT(*) FROM transactions WHERE decision = 'BLOCKED') AS blocked, "
            + "(SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE decision = 'BLOCKED') AS blocked_amount";
    private static final String BY_STATUS = "SELECT status, COUNT(*) FROM fraud_cases GROUP BY status";
    private static final String PER_DAY =
            "SELECT DATE(t.txn_time), t.decision, COUNT(*) FROM fraud_cases c JOIN transactions t ON t.txn_id = c.txn_id "
            + "WHERE t.txn_time >= ? GROUP BY DATE(t.txn_time), t.decision";
    private static final String HISTOGRAM =
            "SELECT LEAST(FLOOR(risk_score * 10), 9) AS bucket, COUNT(*) FROM transactions WHERE txn_type <> 'REFUND' "
            + "GROUP BY LEAST(FLOOR(risk_score * 10), 9)";
    private static final String BY_CATEGORY =
            "SELECT t.category, COUNT(*) FROM fraud_cases c JOIN transactions t ON t.txn_id = c.txn_id GROUP BY t.category";
    private static final String FEEDBACK =
            "SELECT (SELECT COUNT(*) FROM fraud_cases WHERE status = 'CONFIRMED_FRAUD'), "
            + "(SELECT COUNT(*) FROM fraud_cases WHERE status = 'FALSE_POSITIVE'), "
            + "(SELECT COUNT(*) FROM alerts WHERE user_response = 'REPORTED_FRAUD'), "
            + "(SELECT COUNT(*) FROM alerts WHERE user_response = 'CONFIRMED_GENUINE')";

    private final DBConnection db = DBConnection.getInstance();

    public DashboardStats getDashboardStats() throws DatabaseException {
        try (Connection con = db.getConnection()) {
            Map<CaseStatus, Integer> byStatus = new EnumMap<>(CaseStatus.class);
            for (CaseStatus s : CaseStatus.values()) {
                byStatus.put(s, 0);
            }
            try (PreparedStatement ps = con.prepareStatement(BY_STATUS);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byStatus.put(CaseStatus.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }

            // TreeMap keeps the dates sorted; every one of the last 7 days gets an entry
            Map<LocalDate, int[]> perDay = new TreeMap<>();
            LocalDate first = LocalDate.now().minusDays(CHART_DAYS - 1);
            for (int i = 0; i < CHART_DAYS; i++) {
                perDay.put(first.plusDays(i), new int[2]);
            }
            try (PreparedStatement ps = con.prepareStatement(PER_DAY)) {
                ps.setDate(1, Date.valueOf(first));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int[] counts = perDay.get(rs.getDate(1).toLocalDate());
                        if (counts != null) {
                            counts["FLAGGED".equals(rs.getString(2)) ? 0 : 1] += rs.getInt(3);
                        }
                    }
                }
            }

            int[] histogram = new int[10];
            try (PreparedStatement ps = con.prepareStatement(HISTOGRAM);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int bucket = Math.max(0, Math.min(9, rs.getInt(1)));
                    histogram[bucket] += rs.getInt(2);
                }
            }

            Map<MerchantCategory, Integer> byCategory = new EnumMap<>(MerchantCategory.class);
            try (PreparedStatement ps = con.prepareStatement(BY_CATEGORY);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byCategory.put(MerchantCategory.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }

            try (PreparedStatement ps = con.prepareStatement(COUNTS);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal blockedAmount = rs.getBigDecimal("blocked_amount");
                return new DashboardStats(rs.getInt("customers"), rs.getInt("txns"), rs.getInt("flagged"),
                        rs.getInt("blocked"), byStatus.get(CaseStatus.OPEN), byStatus.get(CaseStatus.CONFIRMED_FRAUD),
                        byStatus.get(CaseStatus.FALSE_POSITIVE), blockedAmount == null ? BigDecimal.ZERO : blockedAmount,
                        perDay, byStatus, histogram, byCategory);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load dashboard: " + e.getMessage(), e);
        }
    }

    public FeedbackSummary getFeedbackSummary(int labelledTotal) throws DatabaseException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(FEEDBACK);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new FeedbackSummary(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), labelledTotal);
        } catch (SQLException e) {
            throw new DatabaseException("Could not load feedback summary: " + e.getMessage(), e);
        }
    }
}
