package utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utility class for input validation across the application.
 * Provides centralized validation logic to ensure data integrity.
 */
public class ValidationUtils {

    // Regex patterns for validation
    private static final Pattern KEBELE_ID_PATTERN = Pattern.compile("^KBL-\\d{4}-\\d{3}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+251[79]\\d{8}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s'-]{2,50}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Validates if a string is not null, not empty, and not a placeholder.
     */
    public static boolean isNotEmpty(String value, String placeholder) {
        return value != null 
            && !value.trim().isEmpty() 
            && !value.equals(placeholder);
    }

    /**
     * Validates Kebele ID format (KBL-YYYY-NNN).
     */
    public static boolean isValidKebeleId(String kebeleId) {
        return kebeleId != null && KEBELE_ID_PATTERN.matcher(kebeleId).matches();
    }

    /**
     * Validates Ethiopian phone number format (+251XXXXXXXXX).
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Phone is optional
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validates name (letters, spaces, hyphens, apostrophes only).
     */
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Validates username (alphanumeric and underscore, 3-20 chars).
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Validates password strength.
     * Requirements: 8+ chars, uppercase, lowercase, digit, special char.
     */
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Gets password strength requirements message.
     */
    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters long and contain:\n" +
               "• At least one uppercase letter (A-Z)\n" +
               "• At least one lowercase letter (a-z)\n" +
               "• At least one digit (0-9)\n" +
               "• At least one special character (@$!%*?&)";
    }

    /**
     * Validates date format (YYYY-MM-DD) and checks if it's a valid date.
     */
    public static boolean isValidDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates if date is not in the future.
     */
    public static boolean isNotFutureDate(String date) {
        if (!isValidDate(date)) {
            return false;
        }
        LocalDate parsedDate = LocalDate.parse(date, DATE_FORMATTER);
        return !parsedDate.isAfter(LocalDate.now());
    }

    /**
     * Validates if person is at least 18 years old.
     */
    public static boolean isAdult(String birthDate) {
        if (!isValidDate(birthDate)) {
            return false;
        }
        LocalDate birth = LocalDate.parse(birthDate, DATE_FORMATTER);
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        return !birth.isAfter(eighteenYearsAgo);
    }

    /**
     * Validates if age is reasonable (between 0 and 150 years).
     */
    public static boolean isReasonableAge(String birthDate) {
        if (!isValidDate(birthDate)) {
            return false;
        }
        LocalDate birth = LocalDate.parse(birthDate, DATE_FORMATTER);
        LocalDate now = LocalDate.now();
        LocalDate maxAge = now.minusYears(150);
        
        return !birth.isBefore(maxAge) && !birth.isAfter(now);
    }

    /**
     * Sanitizes input by removing leading/trailing whitespace and 
     * replacing multiple spaces with single space.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Validates if a string contains only allowed characters (no SQL injection).
     */
    public static boolean isSafeInput(String input) {
        if (input == null) {
            return true;
        }
        // Check for common SQL injection patterns
        String lower = input.toLowerCase();
        return !lower.contains("--") 
            && !lower.contains("/*") 
            && !lower.contains("*/")
            && !lower.contains("xp_")
            && !lower.contains("sp_")
            && !lower.contains("exec")
            && !lower.contains("execute")
            && !lower.contains("drop ")
            && !lower.contains("delete ")
            && !lower.contains("insert ")
            && !lower.contains("update ")
            && !lower.contains("union ");
    }

    /**
     * Validates email format (basic validation).
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email.trim()).matches();
    }

    /**
     * Validates if a number is within a range.
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validates if a string length is within a range.
     */
    public static boolean isLengthInRange(String value, int min, int max) {
        if (value == null) {
            return false;
        }
        int length = value.trim().length();
        return length >= min && length <= max;
    }
}
