package db;

import utils.PasswordUtils;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL =
            "jdbc:sqlite:kebele_system.db";

    private static Connection conn;

    public static Connection getConnection()
            throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(DB_URL);
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
            }
        }
        return conn;
    }

    public static void initDatabase() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection c = getConnection();
        Statement s  = c.createStatement();

        // Users table with approval workflow
        s.execute(
                "CREATE TABLE IF NOT EXISTS users ("
                        + "id              INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "full_name       TEXT NOT NULL,"
                        + "username        TEXT UNIQUE NOT NULL,"
                        + "password        TEXT NOT NULL,"
                        + "role            TEXT NOT NULL DEFAULT 'Viewer',"
                        + "kebele_id       TEXT NOT NULL DEFAULT 'KBL-01',"
                        + "status          TEXT NOT NULL DEFAULT 'Active',"
                        + "approval_status TEXT DEFAULT 'Pending',"  // Pending, Approved, Rejected
                        + "approved_by     TEXT,"                     // Admin who approved
                        + "approved_at     TEXT,"                     // Approval timestamp
                        + "linked_resident_id INTEGER,"               // NEW: Links to residents table
                        + "created_at      TEXT DEFAULT (datetime('now')),"
                        + "FOREIGN KEY (linked_resident_id) REFERENCES residents(id)"
                        + ")"
        );

        // Add approval columns to users table if they don't exist (for existing databases)
        try {
            s.execute("ALTER TABLE users ADD COLUMN approval_status TEXT DEFAULT 'Pending'");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE users ADD COLUMN approved_by TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE users ADD COLUMN approved_at TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE users ADD COLUMN linked_resident_id INTEGER");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE users ADD COLUMN photo_path TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }

        // Residents table - Enhanced schema with comprehensive fields and approval workflow
        s.execute(
                "CREATE TABLE IF NOT EXISTS residents ("
                        + "id              INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "kebele_id       TEXT UNIQUE NOT NULL,"
                        + "first_name      TEXT NOT NULL,"
                        + "last_name       TEXT NOT NULL,"
                        + "father_name     TEXT NOT NULL,"
                        + "mother_name     TEXT,"
                        + "date_of_birth   TEXT NOT NULL,"
                        + "gender          TEXT NOT NULL,"
                        + "marital_status  TEXT,"
                        + "blood_group     TEXT,"
                        + "phone_number    TEXT,"
                        + "address         TEXT,"
                        + "address_detail  TEXT,"
                        + "birth_place     TEXT,"
                        + "occupation      TEXT,"
                        + "kebele_number   TEXT,"
                        + "id_status       TEXT DEFAULT 'Active',"
                        + "issued_date     TEXT,"
                        + "photo_path      TEXT,"
                        + "approval_status TEXT DEFAULT 'Pending',"  // New: Pending, Approved, Rejected
                        + "approved_by     TEXT,"                     // New: Admin who approved
                        + "approved_at     TEXT,"                     // New: Approval timestamp
                        + "created_by      TEXT,"
                        + "created_at      TEXT DEFAULT (datetime('now'))"
                        + ")"
        );

        // Add approval_status column if it doesn't exist (for existing databases)
        try {
            s.execute("ALTER TABLE residents ADD COLUMN approval_status TEXT DEFAULT 'Pending'");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE residents ADD COLUMN approved_by TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE residents ADD COLUMN approved_at TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }

        // Activity log table
        s.execute(
                "CREATE TABLE IF NOT EXISTS activity_log ("
                        + "id        INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "user_id   INTEGER,"
                        + "username  TEXT,"
                        + "action    TEXT,"
                        + "details   TEXT,"
                        + "timestamp TEXT DEFAULT (datetime('now'))"
                        + ")"
        );

        // Feedback table with priority and status tracking
        s.execute(
                "CREATE TABLE IF NOT EXISTS feedback ("
                        + "id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "user_id     INTEGER NOT NULL,"
                        + "username    TEXT NOT NULL,"
                        + "full_name   TEXT NOT NULL,"
                        + "role        TEXT NOT NULL,"
                        + "category    TEXT NOT NULL,"
                        + "priority    TEXT DEFAULT 'Medium',"  // Low, Medium, High, Critical
                        + "status      TEXT DEFAULT 'New',"     // New, In Progress, Resolved, Closed
                        + "feedback    TEXT NOT NULL,"
                        + "response    TEXT,"                    // Admin response
                        + "responded_by TEXT,"                   // Admin who responded
                        + "responded_at TEXT,"                   // Response timestamp
                        + "created_at  TEXT DEFAULT (datetime('now')),"
                        + "updated_at  TEXT DEFAULT (datetime('now')),"
                        + "FOREIGN KEY (user_id) REFERENCES users(id)"
                        + ")"
        );

        // Add new columns to existing feedback table if they don't exist
        try {
            s.execute("ALTER TABLE feedback ADD COLUMN priority TEXT DEFAULT 'Medium'");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE feedback ADD COLUMN status TEXT DEFAULT 'New'");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE feedback ADD COLUMN response TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE feedback ADD COLUMN responded_by TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE feedback ADD COLUMN responded_at TEXT");
        } catch (SQLException e) {
            // Column already exists, ignore
        }
        try {
            s.execute("ALTER TABLE feedback ADD COLUMN updated_at TEXT DEFAULT (datetime('now'))");
        } catch (SQLException e) {
            // Column already exists, ignore
        }

        s.close();
        createDefaultUsers();
        System.out.println("Database initialized!");
    }

    private static void createDefaultUsers()
            throws Exception {
        Connection c = getConnection();
        
        // Check if users already exist
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        String sql =
                "INSERT INTO users "
                        + "(full_name,username,password,role,"
                        + "kebele_id,status,approval_status,approved_by,approved_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)";

        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date());

        // Create 5 default users with different roles
        String[][] defaultUsers = {
            {"System Administrator", "wonde", "wonde21", "Admin", "KBL-01"},
            {"Data Encoder User", "decoder", "decoder123", "DataEncoder", "KBL-01"},
            {"Supervisor User", "supervisor", "supervisor123", "Supervisor", "KBL-01"},
            {"Staff User", "staff", "staff123", "Staff", "KBL-01"},
            {"Viewer User", "viewer", "viewer123", "Viewer", "KBL-01"}
        };

        for (String[] user : defaultUsers) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, user[0]);  // full_name
                ps.setString(2, user[1]);  // username
                ps.setString(3, PasswordUtils.hashPassword(user[2]));  // password (hashed)
                ps.setString(4, user[3]);  // role
                ps.setString(5, user[4]);  // kebele_id
                ps.setString(6, "Active");  // status
                ps.setString(7, "Approved");  // approval_status (pre-approved)
                ps.setString(8, "System");  // approved_by
                ps.setString(9, timestamp);  // approved_at
                ps.executeUpdate();
                
                System.out.println("Created default user: " + user[1] + " (Role: " + user[3] + ")");
            }
        }
        
        System.out.println("\n=== Default User Credentials ===");
        System.out.println("Admin:       username: wonde  password: wonde21");
        System.out.println("DataEncoder: username: decoder       password: decoder123");
        System.out.println("Supervisor:  username: supervisor    password: supervisor123");
        System.out.println("Staff:       username: staff         password: staff123");
        System.out.println("Viewer:      username: viewer        password: viewer123");
        System.out.println("================================\n");
    }

    public static void logActivity(
            int userId, String username,
            String action, String details) {
        try {
            String sql =
                    "INSERT INTO activity_log "
                            + "(user_id,username,action,details) "
                            + "VALUES (?,?,?,?)";
            try (PreparedStatement ps =
                         getConnection().prepareStatement(sql)) {
                ps.setInt(1,    userId);
                ps.setString(2, username);
                ps.setString(3, action);
                ps.setString(4, details);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}