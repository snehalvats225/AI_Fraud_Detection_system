package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.DatabaseException;
import fraud.detection.ml.LogisticRegressionModel;
import fraud.detection.ml.ModelMetrics;
import fraud.detection.ml.ModelVersion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Saved versions of the ML model, so the admin can compare them and roll back. */
public class ModelVersionDAO implements GenericDAO<ModelVersion, Integer> {

    private static final String SELECT = "SELECT * FROM model_versions";
    private static final String INSERT =
            "INSERT INTO model_versions (algorithm, weights, bias, feature_means, feature_stds, training_samples, "
            + "real_samples, tp, fp, tn, fn, learning_rate, epochs, l2_lambda, notes, trained_by, is_active) "
            + "VALUES ('LOGISTIC_REGRESSION', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
    private static final String ACTIVATE = "UPDATE model_versions SET is_active = (version_id = ?)";

    private final DBConnection db = DBConnection.getInstance();

    /** Saves a newly trained model and makes it the active one - both in ONE transaction. */
    public int insertAndActivate(LogisticRegressionModel m, ModelMetrics metrics, int samples, int realSamples,
                                 double lr, int epochs, double l2, String notes, String trainedBy) throws DatabaseException {
        Connection con = null;
        try {
            con = db.getConnection();
            con.setAutoCommit(false);
            int id;
            try (PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, csv(m.getWeights()));
                ps.setDouble(2, m.getBias());
                ps.setString(3, csv(m.getMeans()));
                ps.setString(4, csv(m.getStds()));
                ps.setInt(5, samples);
                ps.setInt(6, realSamples);
                ps.setInt(7, metrics.getTp());
                ps.setInt(8, metrics.getFp());
                ps.setInt(9, metrics.getTn());
                ps.setInt(10, metrics.getFn());
                ps.setDouble(11, lr);
                ps.setInt(12, epochs);
                ps.setDouble(13, l2);
                ps.setString(14, notes);
                ps.setString(15, trainedBy);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getInt(1);
                }
            }
            activate(con, id);
            con.commit();
            return id;
        } catch (SQLException e) {
            DBConnection.rollbackQuietly(con);
            throw new DatabaseException("Could not save the model: " + e.getMessage(), e);
        } finally {
            DBConnection.closeQuietly(con);
        }
    }

    public void activate(int versionId) throws DatabaseException {
        try (Connection con = db.getConnection()) {
            activate(con, versionId);
        } catch (SQLException e) {
            throw new DatabaseException("Could not activate the model: " + e.getMessage(), e);
        }
    }

    private void activate(Connection con, int versionId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(ACTIVATE)) {
            ps.setInt(1, versionId);
            ps.executeUpdate();
        }
    }

    public Optional<ModelVersion> findActive() throws DatabaseException {
        List<ModelVersion> list = query(SELECT + " WHERE is_active = 1", null);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<ModelVersion> findById(Integer id) throws DatabaseException {
        List<ModelVersion> list = query(SELECT + " WHERE version_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<ModelVersion> findAll() throws DatabaseException {
        return query(SELECT + " ORDER BY version_id DESC", null);
    }

    private List<ModelVersion> query(String sql, Integer param) throws DatabaseException {
        List<ModelVersion> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load model versions: " + e.getMessage(), e);
        }
        return list;
    }

    private ModelVersion mapRow(ResultSet rs) throws SQLException {
        LogisticRegressionModel model = new LogisticRegressionModel(parse(rs.getString("weights")), rs.getDouble("bias"),
                parse(rs.getString("feature_means")), parse(rs.getString("feature_stds")));
        ModelMetrics metrics = new ModelMetrics(rs.getInt("tp"), rs.getInt("fp"), rs.getInt("tn"), rs.getInt("fn"));
        Timestamp t = rs.getTimestamp("trained_at");
        return new ModelVersion(rs.getInt("version_id"), model, metrics, rs.getInt("training_samples"),
                rs.getInt("real_samples"), rs.getDouble("learning_rate"), rs.getInt("epochs"), rs.getDouble("l2_lambda"),
                rs.getBoolean("is_active"), rs.getString("notes"), rs.getString("trained_by"),
                t == null ? null : t.toLocalDateTime());
    }

    private static String csv(double[] values) {
        return Arrays.stream(values).mapToObj(Double::toString).collect(Collectors.joining(","));
    }

    private static double[] parse(String csv) {
        return Arrays.stream(csv.split(",")).mapToDouble(Double::parseDouble).toArray();
    }
}
