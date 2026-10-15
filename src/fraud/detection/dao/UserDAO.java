package fraud.detection.dao;

import fraud.detection.db.DBConnection;
import fraud.detection.exception.AuthenticationException;
import fraud.detection.exception.DatabaseException;
import fraud.detection.exception.InvalidInputException;
import fraud.detection.model.Admin;
import fraud.detection.model.Customer;
import fraud.detection.model.Person;
import fraud.detection.model.Role;
import fraud.detection.util.PasswordUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Users table: login, registration, passwords and customer balances. */
public class UserDAO implements GenericDAO<Customer, Integer> {

    private static final String FIND_FOR_LOGIN = "SELECT * FROM users WHERE username = ? AND role = ?";
    private static final String USERNAME_EXISTS = "SELECT COUNT(*) FROM users WHERE username = ?";
    private static final String INSERT =
            "INSERT INTO users (username, password_hash, salt, role, full_name, email, phone, home_city, balance) "
            + "VALUES (?, ?, ?, 'CUSTOMER', ?, ?, ?, ?, ?)";
    private static final String SELECT_CUSTOMERS = "SELECT * FROM users WHERE role = 'CUSTOMER'";
    private static final String LOCK_BALANCE =
            "SELECT balance, home_city, status FROM users WHERE user_id = ? AND role = 'CUSTOMER' FOR UPDATE";
    private static final String UPDATE_BALANCE = "UPDATE users SET balance = ? WHERE user_id = ?";
    private static final String FIND_PASSWORD = "SELECT password_hash, salt FROM users WHERE user_id = ?";
    private static final String UPDATE_PASSWORD = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";

    /** Balance and home city read while the row is locked. */
    public static final class LockedAccount {
        public final BigDecimal balance;
        public final String homeCity;
        public final boolean active;

        LockedAccount(BigDecimal balance, String homeCity, boolean active) {
            this.balance = balance;
            this.homeCity = homeCity;
            this.active = active;
        }
    }

    private final DBConnection db = DBConnection.getInstance();

    /** Returns an Admin or a Customer object, both as the parent type Person (POLYMORPHISM). */
    public Person authenticate(String username, String password, Role role)
            throws DatabaseException, AuthenticationException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_FOR_LOGIN)) {
            ps.setString(1, username);
            ps.setString(2, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordUtil.verify(password, rs.getString("salt"), rs.getString("password_hash"))) {
                    throw new AuthenticationException("Invalid username, password or role.");
                }
                if ("BLOCKED".equals(rs.getString("status"))) {
                    throw new AuthenticationException("This account has been blocked. Please contact the bank.");
                }
                return role == Role.ADMIN
                        ? new Admin(rs.getInt("user_id"), username, rs.getString("full_name"))
                        : mapCustomer(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Login failed: " + e.getMessage(), e);
        }
    }

    public boolean usernameExists(String username) throws DatabaseException {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(USERNAME_EXISTS)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not check username: " + e.getMessage(), e);
        }
    }

    /** Creates a customer account. Returns the new auto-generated user_id. */
    public int registerCustomer(String fullName, String email, String phone, String homeCity, String username,
                                String password, BigDecimal openingBalance) throws DatabaseException, InvalidInputException {
        if (usernameExists(username)) {
            throw new InvalidInputException("Username '" + username + "' is already taken.");
        }
        String salt = PasswordUtil.generateSalt();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password, salt));
            ps.setString(3, salt);
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.setString(6, phone);
            ps.setString(7, homeCity);
            ps.setBigDecimal(8, openingBalance);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new DatabaseException("No user id was generated.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not register: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Customer> findById(Integer id) throws DatabaseException {
        List<Customer> list = queryCustomers(SELECT_CUSTOMERS + " AND user_id = ?", id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Customer> findAll() throws DatabaseException {
        return queryCustomers(SELECT_CUSTOMERS + " ORDER BY full_name", null);
    }

    /**
     * Locks the customer's row until the transaction ends (SELECT ... FOR UPDATE),
     * so two payments can never both read the same old balance.
     */
    public Optional<LockedAccount> lockAccount(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(LOCK_BALANCE)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new LockedAccount(rs.getBigDecimal(1), rs.getString(2), "ACTIVE".equals(rs.getString(3))))
                        : Optional.empty();
            }
        }
    }

    public void updateBalance(Connection con, int userId, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(UPDATE_BALANCE)) {
            ps.setBigDecimal(1, balance);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void changePassword(int userId, String oldPassword, String newPassword)
            throws DatabaseException, AuthenticationException {
        try (Connection con = db.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(FIND_PASSWORD)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !PasswordUtil.verify(oldPassword, rs.getString("salt"), rs.getString("password_hash"))) {
                        throw new AuthenticationException("Current password is incorrect.");
                    }
                }
            }
            String salt = PasswordUtil.generateSalt();
            try (PreparedStatement ps = con.prepareStatement(UPDATE_PASSWORD)) {
                ps.setString(1, PasswordUtil.hash(newPassword, salt));
                ps.setString(2, salt);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not change password: " + e.getMessage(), e);
        }
    }

    private List<Customer> queryCustomers(String sql, Integer param) throws DatabaseException {
        List<Customer> list = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCustomer(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not load customers: " + e.getMessage(), e);
        }
        return list;
    }

    private static Customer mapCustomer(ResultSet rs) throws SQLException {
        return new Customer(rs.getInt("user_id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("phone"), rs.getString("home_city"), rs.getBigDecimal("balance"));
    }
}
