package fraud.detection.model;

import java.math.BigDecimal;

/** A bank customer whose transactions are monitored for fraud. */
public class Customer extends Person {

    private final String email;
    private final String phone;
    private final String homeCity;
    private final BigDecimal balance;

    public Customer(int userId, String username, String fullName, String email, String phone,
                    String homeCity, BigDecimal balance) {
        super(userId, username, fullName);
        this.email = email;
        this.phone = phone;
        this.homeCity = homeCity;
        this.balance = balance;
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }

    @Override
    public String getWelcomeMessage() {
        return "Secure Banking - Welcome, " + getFullName();
    }

    public String getEmail()      { return email; }
    public String getPhone()      { return phone; }
    public String getHomeCity()   { return homeCity; }
    public BigDecimal getBalance() { return balance; }
}
