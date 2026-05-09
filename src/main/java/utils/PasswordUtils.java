package utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    private static final int WORK_FACTOR = 12;

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(WORK_FACTOR));
        } catch (Exception e) {
            System.err.println("Error hashing password: " + e.getMessage());
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        
        try {
            if (hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$")) {
                return BCrypt.checkpw(plainPassword, hashedPassword);
            } else if (hashedPassword.contains(":")) {
                return verifyLegacyPassword(plainPassword, hashedPassword);
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    private static boolean verifyLegacyPassword(String plainPassword, String storedHash) {
        try {
            if (!storedHash.contains(":")) {
                return false;
            }
            
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            byte[] salt = java.util.Base64.getDecoder().decode(parts[0]);
            
            java.security.MessageDigest md = 
                java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashed = md.digest(plainPassword.getBytes("UTF-8"));
            
            String inputHash = java.util.Base64.getEncoder().encodeToString(hashed);
            return inputHash.equals(parts[1]);
        } catch (Exception e) {
            System.err.println("Error verifying legacy password: " + e.getMessage());
            return false;
        }
    }

    public static boolean needsUpgrade(String hashedPassword) {
        if (hashedPassword == null) {
            return false;
        }
        return !hashedPassword.startsWith("$2a$") && !hashedPassword.startsWith("$2b$");
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("@$!%*?&".indexOf(c) >= 0) hasSpecial = true;
        }
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters and include:\n" +
               "• Uppercase letter (A-Z)\n" +
               "• Lowercase letter (a-z)\n" +
               "• Number (0-9)\n" +
               "• Special character (@$!%*?&)";
    }
}
