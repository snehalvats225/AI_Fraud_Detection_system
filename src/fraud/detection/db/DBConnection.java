package fraud.detection.db;

import fraud.detection.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Hands out JDBC connections.
 * SINGLETON: private constructor + static getInstance(), so only ONE object exists.
 * getInstance() is SYNCHRONIZED so two threads can never create two objects.
 */
public final class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/fraud_detection_db?useSSL=false&allowPublicKeyRetrieval=true";

    private static DBConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DBConnection() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not read db.properties, using defaults: " + e.getMessage());
        }
        url = props.getProperty("db.url", DEFAULT_URL);
        user = props.getProperty("db.user", "frauduser");
        password = props.getProperty("db.password", "fraud123");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Connector/J jar is not on the classpath.");
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Opens a new connection. Always close it (use try-with-resources). */
    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to the database.\n"
                    + "Check that MySQL is running and db.properties is correct.\n\n" + e.getMessage(), e);
        }
    }

    public static void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
                // nothing more we can do
            }
        }
    }

    public static void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ignored) {
                // nothing more we can do
            }
        }
    }
}
