package utils;

import db.DatabaseManager;
import java.sql.*;

/**
 * Database migration utility to handle schema updates
 * Automatically adds missing columns to existing databases
 */
public class DatabaseMigration {
    
    /**
     * Run all database migrations
     * This ensures old databases are updated to the latest schema
     */
    public static void runMigrations() {
        try {
            System.out.println("Running database migrations...");
            
            // Migration 1: Add missing columns to residents table
            migrateResidentsTable();
            
            // Migration 2: Add missing columns to users table
            migrateUsersTable();
            
            // Migration 3: Add missing columns to feedback table
            migrateFeedbackTable();
            
            System.out.println("Database migrations completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during database migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Migrate residents table - add missing columns
     */
    private static void migrateResidentsTable() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        
        // List of columns to add if missing
        String[] migrations = {
            "ALTER TABLE residents ADD COLUMN father_name TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE residents ADD COLUMN mother_name TEXT",
            "ALTER TABLE residents ADD COLUMN marital_status TEXT",
            "ALTER TABLE residents ADD COLUMN blood_group TEXT",
            "ALTER TABLE residents ADD COLUMN address_detail TEXT",
            "ALTER TABLE residents ADD COLUMN birth_place TEXT",
            "ALTER TABLE residents ADD COLUMN occupation TEXT",
            "ALTER TABLE residents ADD COLUMN kebele_number TEXT",
            "ALTER TABLE residents ADD COLUMN issued_date TEXT",
            "ALTER TABLE residents ADD COLUMN approval_status TEXT DEFAULT 'Pending'",
            "ALTER TABLE residents ADD COLUMN approved_by TEXT",
            "ALTER TABLE residents ADD COLUMN approved_at TEXT"
        };
        
        for (String sql : migrations) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("✓ Migration applied: " + sql.substring(0, Math.min(50, sql.length())) + "...");
            } catch (SQLException e) {
                // Column already exists - this is OK, just skip
                if (e.getMessage().contains("duplicate column name")) {
                    // Silently ignore - column already exists
                } else {
                    System.out.println("⚠ Migration skipped (column may already exist): " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Migrate users table - add missing columns
     */
    private static void migrateUsersTable() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        
        String[] migrations = {
            "ALTER TABLE users ADD COLUMN approval_status TEXT DEFAULT 'Pending'",
            "ALTER TABLE users ADD COLUMN approved_by TEXT",
            "ALTER TABLE users ADD COLUMN approved_at TEXT",
            "ALTER TABLE users ADD COLUMN linked_resident_id INTEGER",
            "ALTER TABLE users ADD COLUMN photo_path TEXT"
        };
        
        for (String sql : migrations) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("✓ Migration applied: " + sql.substring(0, Math.min(50, sql.length())) + "...");
            } catch (SQLException e) {
                // Column already exists - this is OK, just skip
                if (e.getMessage().contains("duplicate column name")) {
                    // Silently ignore
                } else {
                    System.out.println("⚠ Migration skipped: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Migrate feedback table - add missing columns
     */
    private static void migrateFeedbackTable() throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        
        String[] migrations = {
            "ALTER TABLE feedback ADD COLUMN priority TEXT DEFAULT 'Medium'",
            "ALTER TABLE feedback ADD COLUMN status TEXT DEFAULT 'New'",
            "ALTER TABLE feedback ADD COLUMN response TEXT",
            "ALTER TABLE feedback ADD COLUMN responded_by TEXT",
            "ALTER TABLE feedback ADD COLUMN responded_at TEXT",
            "ALTER TABLE feedback ADD COLUMN updated_at TEXT DEFAULT (datetime('now'))"
        };
        
        for (String sql : migrations) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                System.out.println("✓ Migration applied: " + sql.substring(0, Math.min(50, sql.length())) + "...");
            } catch (SQLException e) {
                // Column already exists - this is OK, just skip
                if (e.getMessage().contains("duplicate column name")) {
                    // Silently ignore
                } else {
                    System.out.println("⚠ Migration skipped: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Check if a column exists in a table
     */
    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String colName = rs.getString("name");
                if (colName.equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
