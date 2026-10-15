package fraud.detection.util;

import fraud.detection.exception.InvalidInputException;
import java.math.BigDecimal;

/** Checks form input and THROWS InvalidInputException with a clear message. */
public final class Validator {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private Validator() {
    }

    public static String requireText(String field, String value, int maxLength) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(field + " cannot be empty.");
        }
        String v = value.trim();
        if (v.length() > maxLength) {
            throw new InvalidInputException(field + " can be at most " + maxLength + " characters.");
        }
        return v;
    }

    public static String personName(String value) throws InvalidInputException {
        String v = requireText("Full Name", value, 60);
        if (!v.matches("[A-Za-z .]{3,60}")) {
            throw new InvalidInputException("Full Name should contain only letters and spaces.");
        }
        return v;
    }

    public static String email(String value) throws InvalidInputException {
        String v = requireText("Email", value, 80);
        if (!v.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new InvalidInputException("Please enter a valid email address.");
        }
        return v;
    }

    public static String phone(String value) throws InvalidInputException {
        String v = requireText("Mobile number", value, 10);
        if (!v.matches("[6-9]\\d{9}")) {
            throw new InvalidInputException("Mobile number must be 10 digits and start with 6, 7, 8 or 9.");
        }
        return v;
    }

    public static String username(String value) throws InvalidInputException {
        String v = requireText("Username", value, 20);
        if (!v.matches("[A-Za-z0-9_]{4,20}")) {
            throw new InvalidInputException("Username must be 4-20 letters, digits or _ (no spaces).");
        }
        return v;
    }

    public static String password(String value) throws InvalidInputException {
        if (value == null || value.length() < 6) {
            throw new InvalidInputException("Password must be at least 6 characters.");
        }
        if (!value.matches(".*\\d.*") || !value.matches(".*[A-Za-z].*")) {
            throw new InvalidInputException("Password must contain at least one letter and one digit.");
        }
        return value;
    }

    /** Positive amount with at most 2 decimals, not above Rs. 10 lakh. */
    public static BigDecimal amount(String value) throws InvalidInputException {
        String v = requireText("Amount", value, 15).replace(",", "");
        BigDecimal amount;
        try {
            amount = new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Amount must be a number, e.g. 2500 or 2500.50.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidInputException("Amount must be greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new InvalidInputException("Amount can have at most 2 decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidInputException("A single transaction cannot exceed Rs. 10,00,000.");
        }
        return amount.setScale(2);
    }

    /** Parses a decimal setting and checks its range. */
    public static double decimal(String field, String value, double min, double max) throws InvalidInputException {
        double d;
        try {
            d = Double.parseDouble(requireText(field, value, 15));
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field + " must be a number.");
        }
        if (d < min || d > max) {
            throw new InvalidInputException(field + " must be between " + min + " and " + max + ".");
        }
        return d;
    }

    public static int integer(String field, String value, int min, int max) throws InvalidInputException {
        int i;
        try {
            i = Integer.parseInt(requireText(field, value, 9));
        } catch (NumberFormatException e) {
            throw new InvalidInputException(field + " must be a whole number.");
        }
        if (i < min || i > max) {
            throw new InvalidInputException(field + " must be between " + min + " and " + max + ".");
        }
        return i;
    }
}
