package fraud.detection.model;

/**
 * ABSTRACT parent of every user - INHERITANCE.
 * getRole() is abstract and getWelcomeMessage() is overridden, so the same call
 * behaves differently for an Admin and a Customer - POLYMORPHISM.
 */
public abstract class Person {

    private final int userId;
    private final String username;
    private final String fullName;

    protected Person(int userId, String username, String fullName) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
    }

    public int getUserId()      { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }

    public abstract Role getRole();

    public String getWelcomeMessage() {
        return "Welcome, " + fullName;
    }

    @Override
    public String toString() {
        return getRole() + ": " + fullName;
    }
}
