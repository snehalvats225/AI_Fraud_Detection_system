package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.DatabaseException;
import fraud.detection.model.CaseStatus;
import fraud.detection.model.FraudCase;
import fraud.detection.model.UserResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Fraud cases for the admin to review (4-table JOIN: cases, transactions, users, alerts). */
public class FraudCaseDAO implements GenericDAO<FraudCase, Integer> {

    private static final String SELECT =
            "SELECT c.case_id, c.reasons, c.status AS case_status, c.admin_notes, c.reviewed_at, "
            + "c.created_at AS case_created, rv.full_name AS reviewer, COALESCE(a.user_response, 'NONE') AS user_response, "
            + TransactionDAO.COLUMNS
            + " FROM fraud_cases c JOIN transactions t ON t.txn_id = c.txn_id JOIN users u ON u.user_id = t.user_id"
            + " LEFT JOIN users rv ON rv.user_id = c.reviewed_by"
            + " LEFT JOIN alerts a ON a.txn_id = c.txn_id AND a.severity <> 'INFO'";
    private static final String INSERT = "INSERT INTO fraud_cases (txn_id, user_id, risk_score, reasons) VALUES (?, ?, ?, ?)";
    private static final String LOCK = "SELECT status FROM fraud_cases WHERE case_id = ? FOR UPDATE";
    private static final String REVIEW =
            "UPDATE fraud_cases SET status = ?, admin_notes = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP WHERE case_id = ?";

    private final DBConnection db = DBConnection.getInstance();

    public void insert(Connection con, long txnId, int userId, double risk, String reasons) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setLong(1, txnId);
            ps.setInt(2, userId);
            ps.setDouble(3, Math.round(risk * 10000) / 10000.0);
            ps.setString(4, reasons.length() > 1000 ? reasons.substring(0, 1000) : reasons);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<FraudCase> findById(Integer id) throws DatabaseException {
        try (Connection con = db.getConnection()) {
            return findById(con, id);
        } catch (SQLException e) {
            throw new DatabaseException("Could not load case: " + e.getMessage(), e);
        }
    }

    public Optional<FraudCase> findById(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE c.case_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<FraudCase> findAll() throws DatabaseException {
        List<FraudCase> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY c.case_id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load fraud cases: " + e.getMessage(), e);
        }
        return list;
    }

    /** Locks the case row and returns its current status (empty if it does not exist). */
    public Optional<CaseStatus> lockStatus(Connection con, int caseId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(LOCK)) {
            ps.setInt(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(CaseStatus.valueOf(rs.getString(1))) : Optional.empty();
            }
        }
    }

    public void review(Connection con, int caseId, CaseStatus status, String notes, int adminId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(REVIEW)) {
            ps.setString(1, status.name());
            ps.setString(2, notes);
            ps.setInt(3, adminId);
            ps.setInt(4, caseId);
            ps.executeUpdate();
        }
    }

    private FraudCase mapRow(ResultSet rs) throws SQLException {
        Timestamp reviewed = rs.getTimestamp("reviewed_at");
        Timestamp created = rs.getTimestamp("case_created");
        return new FraudCase(rs.getInt("case_id"), TransactionDAO.mapRow(rs), rs.getString("reasons"),
                CaseStatus.valueOf(rs.getString("case_status")), rs.getString("admin_notes"), rs.getString("reviewer"),
                reviewed == null ? null : reviewed.toLocalDateTime(), created == null ? null : created.toLocalDateTime(),
                UserResponse.valueOf(rs.getString("user_response")));
    }
}
