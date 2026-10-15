package fraud.detection.model;

/** Fraud analyst / system administrator. */
public class Admin extends Person {

    public Admin(int userId, String username, String fullName) {
        super(userId, username, fullName);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getWelcomeMessage() {
        return "Fraud Control Center - Welcome, " + getFullName();
    }
}
