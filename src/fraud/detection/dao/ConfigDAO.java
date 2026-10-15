package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.DatabaseException;
import fraud.detection.model.DetectionConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** The single-row detection_config table. */
public class ConfigDAO {

    private static final String SELECT = "SELECT * FROM detection_config WHERE id = 1";
    private static final String SAVE =
            "UPDATE detection_config SET alert_threshold = ?, block_threshold = ?, high_amount_limit = ?, night_start = ?, "
            + "night_end = ?, velocity_limit = ?, rule_weight = ?, stat_weight = ?, ml_weight = ?, learning_rate = ?, "
            + "epochs = ?, l2_lambda = ?, updated_by = ?, updated_at = ? WHERE id = 1";

    private final DBConnection db = DBConnection.getInstance();

    public DetectionConfig load() throws DatabaseException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return DetectionConfig.defaults();
            }
            Timestamp t = rs.getTimestamp("updated_at");
            return new DetectionConfig.Builder()
                    .alertThreshold(rs.getDouble("alert_threshold"))
                    .blockThreshold(rs.getDouble("block_threshold"))
                    .highAmountLimit(rs.getBigDecimal("high_amount_limit"))
                    .nightStart(rs.getInt("night_start"))
                    .nightEnd(rs.getInt("night_end"))
                    .velocityLimit(rs.getInt("velocity_limit"))
                    .ruleWeight(rs.getDouble("rule_weight"))
                    .statWeight(rs.getDouble("stat_weight"))
                    .mlWeight(rs.getDouble("ml_weight"))
                    .learningRate(rs.getDouble("learning_rate"))
                    .epochs(rs.getInt("epochs"))
                    .l2Lambda(rs.getDouble("l2_lambda"))
                    .updatedBy(rs.getString("updated_by"))
                    .updatedAt(t == null ? null : t.toLocalDateTime())
                    .build();
        } catch (SQLException e) {
            throw new DatabaseException("Could not load configuration: " + e.getMessage(), e);
        }
    }

    public void save(DetectionConfig c) throws DatabaseException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(SAVE)) {
            ps.setDouble(1, c.getAlertThreshold());
            ps.setDouble(2, c.getBlockThreshold());
            ps.setBigDecimal(3, c.getHighAmountLimit());
            ps.setInt(4, c.getNightStart());
            ps.setInt(5, c.getNightEnd());
            ps.setInt(6, c.getVelocityLimit());
            ps.setDouble(7, c.getRuleWeight());
            ps.setDouble(8, c.getStatWeight());
            ps.setDouble(9, c.getMlWeight());
            ps.setDouble(10, c.getLearningRate());
            ps.setInt(11, c.getEpochs());
            ps.setDouble(12, c.getL2Lambda());
            ps.setString(13, c.getUpdatedBy());
            ps.setTimestamp(14, Timestamp.valueOf(c.getUpdatedAt()));
            if (ps.executeUpdate() == 0) {
                throw new DatabaseException("detection_config row is missing. Run the schema script again.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not save configuration: " + e.getMessage(), e);
        }
    }
}
