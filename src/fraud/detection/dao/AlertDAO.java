package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.DatabaseException;
import fraud.detection.exception.UnauthorizedActionException;
import fraud.detection.model.Alert;
import fraud.detection.model.AlertSeverity;
import fraud.detection.model.Decision;
import fraud.detection.model.UserResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Customer notifications about suspicious transactions and case outcomes. */
public class AlertDAO {

    private static final String INSERT = "INSERT INTO alerts (user_id, txn_id, severity, message) VALUES (?, ?, ?, ?)";
    private static final String BY_USER =
            "SELECT a.*, t.amount, t.payee, t.decision FROM alerts a JOIN transactions t ON t.txn_id = a.txn_id "
            + "WHERE a.user_id = ? ORDER BY a.alert_id DESC";
    private static final String UNREAD = "SELECT COUNT(*) FROM alerts WHERE user_id = ? AND is_read = 0";
    private static final String MARK_READ = "UPDATE alerts SET is_read = 1 WHERE user_id = ? AND is_read = 0";
    private static final String LOCK = "SELECT user_id, severity, user_response FROM alerts WHERE alert_id = ? FOR UPDATE";
    private static final String RESPOND =
            "UPDATE alerts SET user_response = ?, responded_at = CURRENT_TIMESTAMP, is_read = 1 WHERE alert_id = ?";

    private final DBConnection db = DBConnection.getInstance();

    public void insert(Connection con, int userId, long txnId, AlertSeverity severity, String message) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setInt(1, userId);
            ps.setLong(2, txnId);
            ps.setString(3, severity.name());
            ps.setString(4, message.length() > 400 ? message.substring(0, 400) : message);
            ps.executeUpdate();
        }
    }

    public List<Alert> findByUser(int userId) throws DatabaseException {
        List<Alert> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp created = rs.getTimestamp("created_at");
                    list.add(new Alert(rs.getInt("alert_id"), rs.getLong("txn_id"),
                            AlertSeverity.valueOf(rs.getString("severity")), rs.getString("message"),
                            rs.getBoolean("is_read"), UserResponse.valueOf(rs.getString("user_response")),
                            created == null ? null : created.toLocalDateTime(), rs.getBigDecimal("amount"),
                            rs.getString("payee"), Decision.valueOf(rs.getString("decision"))));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load alerts: " + e.getMessage(), e);
        }
        return list;
    }

    public int unreadCount(int userId) throws DatabaseException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(UNREAD)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not count alerts: " + e.getMessage(), e);
        }
    }

    public void markAllRead(int userId) throws DatabaseException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(MARK_READ)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not update alerts: " + e.getMessage(), e);
        }
    }

    /** Saves the customer's answer. Only the owner may answer, and only once. */
    public void respond(int alertId, int userId, UserResponse response)
            throws DatabaseException, UnauthorizedActionException {
        Connection con = null;
        try {
            con = db.getConnection();
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(LOCK)) {
                ps.setInt(1, alertId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("user_id") != userId) {
                        throw new UnauthorizedActionException("This alert does not belong to you.");
                    }
                    if ("INFO".equals(rs.getString("severity"))) {
                        throw new UnauthorizedActionException("This is an information message - no answer is needed.");
                    }
                    if (!"NONE".equals(rs.getString("user_response"))) {
                        throw new UnauthorizedActionException("You have already answered this alert.");
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(RESPOND)) {
                ps.setString(1, response.name());
                ps.setInt(2, alertId);
                ps.executeUpdate();
            }
            con.commit();
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);
            throw new DatabaseException("Could not save your answer: " + e.getMessage(), e);
        } catch (UnauthorizedActionException e) {
            DBConnection.rollbackQuietly(con);
            throw e;
        } finally {
            DBConnection.closeQuietly(con);
        }
    }
}
