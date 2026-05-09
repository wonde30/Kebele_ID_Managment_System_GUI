package model;

public class User {

    private int    id;
    private String fullName;
    private String username;
    private String role;
    private String kebeleId;
    private String status;

    public User() {}

    public User(int id, String fullName,
                String username, String role,
                String kebeleId, String status) {
        this.id       = id;
        this.fullName = fullName;
        this.username = username;
        this.role     = role;
        this.kebeleId = kebeleId;
        this.status   = status;
    }

    // Role checks
    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role);
    }
    public boolean isSupervisor() {
        return "Supervisor".equalsIgnoreCase(role)
                || isAdmin();
    }
    public boolean canAddResident() {
        return isAdmin() || isSupervisor()
                || "Staff".equalsIgnoreCase(role)
                || "DataEncoder".equalsIgnoreCase(role);
    }
    public boolean canEditResident() {
        return isAdmin() || isSupervisor()
                || "Staff".equalsIgnoreCase(role)
                || "DataEncoder".equalsIgnoreCase(role);
    }
    public boolean canDeleteResident() {
        return isAdmin() || isSupervisor();
    }
    public boolean canPrintID() {
        return isAdmin() || isSupervisor()
                || "Staff".equalsIgnoreCase(role);
    }
    public boolean canManageUsers() {
        return isAdmin();
    }

    // Getters
    public int    getId()       { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
    public String getKebeleId() { return kebeleId; }
    public String getStatus()   { return status; }

    // Setters
    public void setId(int id)         { this.id       = id; }
    public void setFullName(String n) { this.fullName = n; }
    public void setUsername(String u) { this.username = u; }
    public void setRole(String r)     { this.role     = r; }
    public void setKebeleId(String k) { this.kebeleId = k; }
    public void setStatus(String s)   { this.status   = s; }
}