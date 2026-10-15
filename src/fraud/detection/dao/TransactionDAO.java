package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.engine.Assessment;
import fraud.detection.engine.UserProfile;
import fraud.detection.exception.DatabaseException;
import fraud.detection.ml.Features;
import fraud.detection.ml.LabeledSample;
import fraud.detection.model.Decision;
import fraud.detection.model.DeviceType;
import fraud.detection.model.MerchantCategory;
import fraud.detection.model.Transaction;
import fraud.detection.model.TransactionRequest;
import fraud.detection.model.TransactionType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Transactions table: every payment with its fraud scores and decision.
 * update() is NOT overridden - financial records are never edited
 * (the default update() from GenericDAO refuses).
 */
public class TransactionDAO implements GenericDAO<Transaction, Long> {

    /** Real feedback counts more than one synthetic example when retraining. */
    public static final double REAL_SAMPLE_WEIGHT = 3.0;

    static final String COLUMNS = "t.txn_id, t.user_id, u.full_name, t.txn_type, t.payee, t.amount, t.category, t.city, "
            + "t.device, t.txn_time, t.risk_score, t.rule_score, t.stat_score, t.ml_score, t.decision, t.balance_after, "
            + "t.model_version";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM transactions t JOIN users u ON u.user_id = t.user_id";
    private static final String ORDER = " ORDER BY t.txn_time DESC, t.txn_id DESC";
    private static final String INSERT =
            "INSERT INTO transactions (user_id, txn_type, payee, amount, category, city, device, txn_time, risk_score, "
            + "rule_score, stat_score, ml_score, decision, balance_after, features, model_version) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String PROFILE_STATS =
            "SELECT COUNT(*), AVG(amount), STDDEV_POP(amount) FROM transactions WHERE user_id = ? AND decision = 'APPROVED' "
            + "AND txn_type <> 'REFUND' AND (label IS NULL OR label = 0) AND txn_time < ?";
    private static final String KNOWN_CITIES =
            "SELECT DISTINCT city FROM transactions WHERE user_id = ? AND decision = 'APPROVED' AND txn_type <> 'REFUND' "
            + "AND (label IS NULL OR label = 0) AND txn_time < ?";
    private static final String KNOWN_DEVICES =
            "SELECT DISTINCT device FROM transactions WHERE user_id = ? AND decision = 'APPROVED' AND txn_type <> 'REFUND' "
            + "AND (label IS NULL OR label = 0) AND txn_time < ?";
    private static final String LAST_HOUR =
            "SELECT COUNT(*) FROM transactions WHERE user_id = ? AND txn_type <> 'REFUND' AND txn_time > ? AND txn_time <= ?";
    private static final String SET_LABEL = "UPDATE transactions SET label = ? WHERE txn_id = ?";
    private static final String LABELLED =
            "SELECT t.features, COALESCE(t.label, CASE a.user_response WHEN 'REPORTED_FRAUD' THEN 1 "
            + "WHEN 'CONFIRMED_GENUINE' THEN 0 END) AS lbl "
            + "FROM transactions t LEFT JOIN alerts a ON a.txn_id = t.txn_id AND a.severity <> 'INFO' "
            + "WHERE t.features IS NOT NULL AND (t.label IS NOT NULL OR a.user_response IN ('REPORTED_FRAUD', 'CONFIRMED_GENUINE'))";

    private final DBConnection db = DBConnection.getInstance();

    /** Saves a scored transaction inside the caller's DB transaction. Returns the new txn_id. */
    public long insert(Connection con, int userId, TransactionRequest r, Assessment a, BigDecimal balanceAfter)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, r.getType().name());
            ps.setString(3, r.getPayee());
            ps.setBigDecimal(4, r.getAmount());
            ps.setString(5, r.getCategory().name());
            ps.setString(6, r.getCity());
            ps.setString(7, r.getDevice().name());
            ps.setTimestamp(8, Timestamp.valueOf(r.getTime()));
            ps.setDouble(9, round(a.getRiskScore()));
            ps.setDouble(10, round(a.getRuleScore()));
            ps.setDouble(11, round(a.getStatScore()));
            ps.setDouble(12, round(a.getMlScore()));
            ps.setString(13, a.getDecision().name());
            ps.setBigDecimal(14, balanceAfter);
            ps.setString(15, Features.toCsv(a.getFeatures()));
            if (a.getModelVersion() == null) {
                ps.setNull(16, Types.INTEGER);
            } else {
                ps.setInt(16, a.getModelVersion());
            }
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    /** Money given back after a confirmed fraud (no fraud scores, never used for training). */
    public long insertRefund(Connection con, int userId, BigDecimal amount, String note, BigDecimal balanceAfter,
                             String city) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, TransactionType.REFUND.name());
            ps.setString(3, note);
            ps.setBigDecimal(4, amount);
            ps.setString(5, MerchantCategory.MONEY_TRANSFER.name());
            ps.setString(6, city);
            ps.setString(7, DeviceType.NET_BANKING.name());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            for (int i = 9; i <= 12; i++) {
                ps.setDouble(i, 0);
            }
            ps.setString(13, Decision.APPROVED.name());
            ps.setBigDecimal(14, balanceAfter);
            ps.setNull(15, Types.VARCHAR);
            ps.setNull(16, Types.INTEGER);
            ps.executeUpdate();
            return generatedKey(ps);
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) throws DatabaseException {
        List<Transaction> list = query(SELECT + " WHERE t.txn_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<Transaction> findById(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE t.txn_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        return query(SELECT + ORDER + " LIMIT 5000", null);
    }

    public List<Transaction> findByUser(int userId) throws DatabaseException {
        return query(SELECT + " WHERE t.user_id = ?" + ORDER, userId);
    }

    /**
     * Builds the customer's behaviour profile from transactions BEFORE the given time.
     * Only approved, non-fraud history counts as "normal".
     */
    public UserProfile buildProfile(Connection con, int userId, BigDecimal balance, String homeCity, LocalDateTime at)
            throws SQLException {
        Timestamp t = Timestamp.valueOf(at);
        int count = 0;
        double mean = 0;
        double std = 0;
        try (PreparedStatement ps = con.prepareStatement(PROFILE_STATS)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, t);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                    mean = rs.getDouble(2);
                    std = rs.getDouble(3);
                }
            }
        }
        Set<String> cities = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(KNOWN_CITIES)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, t);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cities.add(rs.getString(1));
                }
            }
        }
        Set<DeviceType> devices = EnumSet.noneOf(DeviceType.class);
        try (PreparedStatement ps = con.prepareStatement(KNOWN_DEVICES)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, t);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    devices.add(DeviceType.valueOf(rs.getString(1)));
                }
            }
        }
        int lastHour;
        try (PreparedStatement ps = con.prepareStatement(LAST_HOUR)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(at.minusHours(1)));
            ps.setTimestamp(3, t);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                lastHour = rs.getInt(1);
            }
        }
        return new UserProfile(count, mean, std, cities, devices, lastHour, balance, homeCity);
    }

    public void setLabel(Connection con, long txnId, int label) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SET_LABEL)) {
            ps.setInt(1, label);
            ps.setLong(2, txnId);
            ps.executeUpdate();
        }
    }

    /**
     * Real labelled examples for retraining. The admin's review wins; if the case was
     * not reviewed yet, the customer's own answer ("it was me" / "not me") is used.
     */
    public List<LabeledSample> loadLabelledSamples() throws DatabaseException {
        List<LabeledSample> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(LABELLED);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double[] x = Features.fromCsv(rs.getString(1));
                if (x.length == Features.COUNT) {
                    list.add(new LabeledSample(x, rs.getInt(2), REAL_SAMPLE_WEIGHT, true));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load feedback data: " + e.getMessage(), e);
        }
        return list;
    }

    private List<Transaction> query(String sql, Object param) throws DatabaseException {
        List<Transaction> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setObject(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load transactions: " + e.getMessage(), e);
        }
        return list;
    }

    /** Shared with FraudCaseDAO, whose query uses the same transaction columns. */
    static Transaction mapRow(ResultSet rs) throws SQLException {
        int mv = rs.getInt("model_version");
        Integer modelVersion = rs.wasNull() ? null : mv;
        Timestamp time = rs.getTimestamp("txn_time");
        return new Transaction(rs.getLong("txn_id"), rs.getInt("user_id"), rs.getString("full_name"),
                TransactionType.valueOf(rs.getString("txn_type")), rs.getString("payee"), rs.getBigDecimal("amount"),
                MerchantCategory.valueOf(rs.getString("category")), rs.getString("city"),
                DeviceType.valueOf(rs.getString("device")), time == null ? null : time.toLocalDateTime(),
                rs.getDouble("risk_score"), rs.getDouble("rule_score"), rs.getDouble("stat_score"),
                rs.getDouble("ml_score"), Decision.valueOf(rs.getString("decision")),
                rs.getBigDecimal("balance_after"), modelVersion);
    }

    private static long generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("No id was generated.");
        }
    }

    private static double round(double v) {
        return Math.round(v * 10000) / 10000.0;
    }
}
