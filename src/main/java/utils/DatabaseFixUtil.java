package utils;

import db.DatabaseManager;
import java.sql.*;

/**
 * One-time utility to fix existing database issues
 * Run this once to update admin username and approval status
 */
public class DatabaseFixUtil {
    
    public static void fixAdminAccount() {
        try {
            Connection conn = DatabaseManager.getConnection();
            
            // Check if admin or wonde exists
            String checkSql = "SELECT id, username, approval_status FROM users WHERE username IN ('admin', 'wonde')";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                
                if (rs.next()) {
                    int adminId = rs.getInt("id");
                    String currentUsername = rs.getString("username");
                    String currentStatus = rs.getString("approval_status");
                    
                    System.out.println("Found admin account (ID: " + adminId + ", Username: " + currentUsername + ", Status: " + currentStatus + ")");
                    
                    // Update username to wonde, password to wonde21, photo, and approve
                    String updateSql = "UPDATE users SET " +
                            "username = 'wonde', " +
                            "password = ?, " +
                            "photo_path = 'resource:selfie.jpg', " +
                            "approval_status = 'Approved', " +
                            "approved_by = 'System', " +
                            "approved_at = datetime('now') " +
                            "WHERE id = ?";
                    
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, PasswordUtils.hashPassword("wonde21"));
                        ps.setInt(2, adminId);
                        int updated = ps.executeUpdate();
                        
                        if (updated > 0) {
                            System.out.println("✓ Admin username set to 'wonde'");
                            System.out.println("✓ Admin password set to 'wonde21'");
                            System.out.println("✓ Admin photo set to embedded selfie.jpg");
                            System.out.println("✓ Admin account approved");
                            System.out.println("\nYou can now login with:");
                            System.out.println("  Username: wonde");
                            System.out.println("  Password: wonde21");
                        }
                    }
                } else {
                    // No admin exists, create new one with correct username
                    System.out.println("No admin account found. Creating new admin...");
                    createNewAdmin(conn);
                }
            }
            
            // Also approve any other default users that might be pending
            String approveDefaultsSql = "UPDATE users SET " +
                    "approval_status = 'Approved', " +
                    "approved_by = 'System', " +
                    "approved_at = datetime('now') " +
                    "WHERE username IN ('supervisor', 'staff', 'decoder', 'viewer') " +
                    "AND approval_status = 'Pending'";
            
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate(approveDefaultsSql);
                if (updated > 0) {
                    System.out.println("✓ Approved " + updated + " default user(s)");
                }
            }
            
            System.out.println("\n=== Database Fix Complete ===\n");
            
        } catch (Exception e) {
            System.err.println("Error fixing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createNewAdmin(Connection conn) throws Exception {
        String sql = "INSERT INTO users " +
                "(full_name, username, password, role, kebele_id, status, approval_status, approved_by, approved_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "System Administrator");
            ps.setString(2, "wonde");
            ps.setString(3, PasswordUtils.hashPassword("wonde21"));
            ps.setString(4, "Admin");
            ps.setString(5, "KBL-01");
            ps.setString(6, "Active");
            ps.setString(7, "Approved");
            ps.setString(8, "System");
            ps.executeUpdate();
            
            System.out.println("✓ Created new admin account: wonde");
        }
    }
}
