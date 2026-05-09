package ui;

import db.DatabaseManager;
import model.User;
import utils.PasswordUtils;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile panel showing user information, notifications, and settings.
 */
public class ProfilePanel extends JPanel {
    
    // Colors
    private static final Color PRIMARY   = new Color(0x77CFA0);
    private static final Color SECONDARY = new Color(0x1B6CA8);
    private static final Color BG        = new Color(0x56B5A5);
    private static final Color CARD      = Color.WHITE;
    private static final Color SUCCESS   = new Color(0x2D9E6B);
    private static final Color DANGER    = new Color(0x808541);
    private static final Color WARN      = new Color(0x9A755E);
    private static final Color INFO      = new Color(0x8AC56C);
    private static final Color TXT       = new Color(0x1A1A2E);
    private static final Color TXT_MUTED = new Color(0x7F8C9A);
    
    // Fonts
    private static final Font F_TITLE  = new Font("Segoe UI", Font.BOLD,  20);
    private static final Font F_HEADER = new Font("Segoe UI", Font.BOLD,  16);
    private static final Font F_LABEL  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font F_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_BTN    = new Font("Segoe UI", Font.BOLD,  12);
    
    private User currentUser;
    private JFrame parentFrame;
    private JLabel lblNotificationCount;
    private List<String> notifications;
    
    public ProfilePanel(User user, JFrame parent) {
        this.currentUser = user;
        this.parentFrame = parent;
        this.notifications = new ArrayList<>();
        
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create scrollable content
        JPanel content = buildContent();
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scroll, BorderLayout.CENTER);
        
        // Load notifications
        loadNotifications();
    }
    
    private JPanel buildContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        
        // Title
        JLabel title = new JLabel("My Profile");
        title.setFont(F_TITLE);
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        
        // Profile card
        panel.add(buildProfileCard());
        panel.add(Box.createVerticalStrut(15));
        
        // Notifications card
        panel.add(buildNotificationsCard());
        panel.add(Box.createVerticalStrut(15));
        
        // Actions card
        panel.add(buildActionsCard());
        panel.add(Box.createVerticalStrut(15));
        
        // System info card
        panel.add(buildSystemInfoCard());
        
        return panel;
    }
    
    private JPanel buildProfileCard() {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(40, 0));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(50, 50, 50, 50));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));
        
        // Left side - PROFESSIONAL LARGE circular photo (300x300)
        JPanel photoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                
                int size = 300; // PROFESSIONAL LARGE SIZE
                int x = (getWidth() - size) / 2;
                int y = 10;
                
                // Load user photo
                Image userPhoto = loadUserPhoto();
                
                if (userPhoto != null) {
                    // Draw soft shadow
                    g2d.setColor(new Color(0, 0, 0, 15));
                    for (int i = 0; i < 12; i++) {
                        int alpha = (int)(15 * (1 - i / 12.0));
                        g2d.setColor(new Color(0, 0, 0, alpha));
                        g2d.fillOval(x - i, y + i, size + (i * 2), size + (i * 2));
                    }
                    
                    // Draw circular photo with high quality scaling
                    g2d.setClip(new java.awt.geom.Ellipse2D.Float(x, y, size, size));
                    g2d.drawImage(userPhoto, x, y, size, size, null);
                    g2d.setClip(null);
                    
                    // Draw elegant white border
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(8));
                    g2d.drawOval(x, y, size, size);
                    
                    // Draw subtle outer accent
                    g2d.setColor(new Color(0xD1D5DB));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval(x - 4, y - 4, size + 8, size + 8);
                } else {
                    // Fallback: modern gradient circle
                    // Soft shadow
                    g2d.setColor(new Color(0, 0, 0, 12));
                    for (int i = 0; i < 10; i++) {
                        int alpha = (int)(12 * (1 - i / 10.0));
                        g2d.setColor(new Color(0, 0, 0, alpha));
                        g2d.fillOval(x - i, y + i, size + (i * 2), size + (i * 2));
                    }
                    
                    // Elegant gradient background
                    GradientPaint gradient = new GradientPaint(
                        x, y, getRoleColor(),
                        x + size, y + size, getRoleColor().darker().darker()
                    );
                    g2d.setPaint(gradient);
                    g2d.fillOval(x, y, size, size);
                    
                    // White border
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(8));
                    g2d.drawOval(x, y, size, size);
                    
                    // Draw large initial
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 120));
                    FontMetrics fm = g2d.getFontMetrics();
                    String initial = currentUser.getFullName().substring(0, 1).toUpperCase();
                    int textX = x + (size - fm.stringWidth(initial)) / 2;
                    int textY = y + (size + fm.getAscent()) / 2 - 20;
                    g2d.drawString(initial, textX, textY);
                }
                
                g2d.dispose();
            }
        };
        photoPanel.setOpaque(false);
        photoPanel.setPreferredSize(new Dimension(350, 330));
        
        // Right side - User information with modern styling
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // Name with extra large font
        JLabel nameLabel = new JLabel(currentUser.getFullName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        nameLabel.setForeground(new Color(0x1F2937));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Role badge with modern styling
        JPanel roleBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        roleBadge.setOpaque(false);
        roleBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel roleLabel = new JLabel("  " + currentUser.getRole() + "  ");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        roleLabel.setForeground(Color.WHITE);
        roleLabel.setOpaque(true);
        roleLabel.setBackground(getRoleColor());
        roleLabel.setBorder(new CompoundBorder(
            new LineBorder(getRoleColor().darker(), 1, true),
            new EmptyBorder(6, 16, 6, 16)
        ));
        
        roleBadge.add(roleLabel);
        
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(roleBadge);
        infoPanel.add(Box.createVerticalStrut(25));
        
        // Info rows with larger icons and text
        addModernInfoRow(infoPanel, "👤", "Username", currentUser.getUsername());
        addModernInfoRow(infoPanel, "🏢", "Kebele", currentUser.getKebeleId());
        addModernInfoRow(infoPanel, "📊", "Status", currentUser.getStatus(), 
                        currentUser.getStatus().equals("Active") ? SUCCESS : DANGER);
        addModernInfoRow(infoPanel, "🆔", "User ID", "#" + currentUser.getId());
        
        infoPanel.add(Box.createVerticalStrut(25));
        
        // Action buttons with modern styling
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton btnEdit = createModernButton("✏️ Edit Profile", SECONDARY);
        btnEdit.addActionListener(e -> showEditProfileDialog());
        
        JButton btnPassword = createModernButton("🔑 Change Password", WARN);
        btnPassword.addActionListener(e -> showChangePasswordDialog());
        
        btnPanel.add(btnEdit);
        btnPanel.add(btnPassword);
        
        infoPanel.add(btnPanel);
        
        // Assemble card
        card.add(photoPanel, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void addModernInfoRow(JPanel parent, String icon, String label, String value) {
        addModernInfoRow(parent, icon, label, value, new Color(0x1F2937));
    }
    
    private void addModernInfoRow(JPanel parent, String icon, String label, String value, Color valueColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        
        JLabel lblLabel = new JLabel(label + ":");
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblLabel.setForeground(new Color(0x6B7280));
        lblLabel.setPreferredSize(new Dimension(100, 30));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblValue.setForeground(valueColor);
        
        row.add(iconLabel);
        row.add(lblLabel);
        row.add(lblValue);
        
        parent.add(row);
    }
    
    private JButton createModernButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
            new LineBorder(bg.darker(), 1, true),
            new EmptyBorder(12, 24, 12, 24)
        ));
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            Color originalBg = bg;
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bg.brighter());
                    btn.setBorder(new CompoundBorder(
                        new LineBorder(bg, 1, true),
                        new EmptyBorder(12, 24, 12, 24)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(originalBg);
                btn.setBorder(new CompoundBorder(
                    new LineBorder(originalBg.darker(), 1, true),
                    new EmptyBorder(12, 24, 12, 24)
                ));
            }
        });
        
        return btn;
    }
    
    private Image loadUserPhoto() {
        try {
            // First, check if user has a linked resident record with photo
            String sql = "SELECT r.photo_path FROM users u " +
                        "LEFT JOIN residents r ON u.linked_resident_id = r.id " +
                        "WHERE u.id = ?";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String photoPath = rs.getString("photo_path");
                        if (photoPath != null && !photoPath.isEmpty()) {
                            Image img = loadImageFromPath(photoPath);
                            if (img != null) return img;
                        }
                    }
                }
            }
            
            // Fallback: check user's own photo_path
            sql = "SELECT photo_path FROM users WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String photoPath = rs.getString("photo_path");
                        if (photoPath != null && !photoPath.isEmpty()) {
                            return loadImageFromPath(photoPath);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Image loadImageFromPath(String photoPath) {
        try {
            // Check if it's an embedded resource
            if (photoPath.startsWith("resource:")) {
                String resourceName = photoPath.substring(9);
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                if (is != null) {
                    return new ImageIcon(is.readAllBytes()).getImage();
                }
            } else {
                // Load from file system
                java.io.File file = new java.io.File(photoPath);
                if (file.exists()) {
                    return new ImageIcon(photoPath).getImage();
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return null;
    }
    
    private JPanel buildNotificationsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Header with notification count
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel headerLabel = new JLabel("🔔 Recent Activity");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(PRIMARY);
        
        lblNotificationCount = new JLabel(String.valueOf(notifications.size()));
        lblNotificationCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblNotificationCount.setForeground(Color.WHITE);
        lblNotificationCount.setOpaque(true);
        lblNotificationCount.setBackground(DANGER);
        lblNotificationCount.setBorder(new EmptyBorder(4, 10, 4, 10));
        
        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(lblNotificationCount, BorderLayout.EAST);
        
        card.add(headerPanel);
        card.add(Box.createVerticalStrut(15));
        
        // Notifications list
        if (notifications.isEmpty()) {
            JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            emptyPanel.setOpaque(false);
            emptyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            
            JLabel noNotif = new JLabel("No recent activity");
            noNotif.setFont(F_LABEL);
            noNotif.setForeground(TXT_MUTED);
            emptyPanel.add(noNotif);
            
            card.add(emptyPanel);
        } else {
            for (int i = 0; i < Math.min(notifications.size(), 5); i++) {
                card.add(createModernNotificationItem(notifications.get(i)));
                card.add(Box.createVerticalStrut(10));
            }
        }
        
        card.add(Box.createVerticalStrut(10));
        
        // View all button
        JButton btnViewAll = createModernButton("📋 View All Activity", INFO);
        btnViewAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnViewAll.addActionListener(e -> showAllActivity());
        
        card.add(btnViewAll);
        
        return card;
    }
    
    private JPanel createModernNotificationItem(String text) {
        JPanel item = new JPanel(new BorderLayout(12, 0));
        item.setOpaque(true);
        item.setBackground(new Color(0xF8FAFC));
        item.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xE2E8F0), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel icon = new JLabel("•");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 24));
        icon.setForeground(PRIMARY);
        
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(F_LABEL);
        label.setForeground(TXT);
        
        item.add(icon, BorderLayout.WEST);
        item.add(label, BorderLayout.CENTER);
        
        return item;
    }
    
    private JPanel buildActionsCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JLabel title = new JLabel("⚡ Quick Actions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(15));
        
        // Action buttons in grid
        JPanel actionsGrid = new JPanel(new GridLayout(0, 1, 0, 12));
        actionsGrid.setOpaque(false);
        actionsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Change password
        JButton btnPassword = createModernButton("🔑 Change Password", WARN);
        btnPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPassword.addActionListener(e -> showChangePasswordDialog());
        actionsGrid.add(btnPassword);
        
        // View my resident record (if viewer)
        if ("Viewer".equals(currentUser.getRole())) {
            JButton btnMyRecord = createModernButton("📄 View My ID Record", INFO);
            btnMyRecord.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnMyRecord.addActionListener(e -> viewMyResidentRecord());
            actionsGrid.add(btnMyRecord);
        }
        
        // Logout
        JButton btnLogout = createModernButton("🚪 Logout", DANGER);
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogout.addActionListener(e -> logout());
        actionsGrid.add(btnLogout);
        
        card.add(actionsGrid);
        
        return card;
    }
    
    private JPanel buildSystemInfoCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(30, 30, 30, 30));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JLabel title = new JLabel("ℹ️ System Information");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(15));
        
        addModernInfoRow(card, "💻", "Application", "Kebele ID Management System");
        addModernInfoRow(card, "🔢", "Version", "v2.0");
        addModernInfoRow(card, "💾", "Database", "SQLite (Embedded)");
        addModernInfoRow(card, "🔒", "Security", "BCrypt Password Hashing");
        
        return card;
    }
    
    private Color getRoleColor() {
        return switch (currentUser.getRole()) {
            case "Admin" -> DANGER;
            case "Supervisor" -> WARN;
            case "Staff" -> SUCCESS;
            case "DataEncoder" -> new Color(0x7B1FA2);
            case "Viewer" -> TXT_MUTED;
            default -> SECONDARY;
        };
    }
    
    private void loadNotifications() {
        notifications.clear();
        
        try {
            String sql = "SELECT action, details, timestamp FROM activity_log " +
                        "WHERE user_id = ? ORDER BY timestamp DESC LIMIT 5";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String action = rs.getString("action");
                        String details = rs.getString("details");
                        String timestamp = rs.getString("timestamp");
                        
                        String notif = "<b>" + action + "</b> - " + details + 
                                      "<br><small>" + formatTimestamp(timestamp) + "</small>";
                        notifications.add(notif);
                    }
                }
            }
            
            if (lblNotificationCount != null) {
                lblNotificationCount.setText(String.valueOf(notifications.size()));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
            return outputFormat.format(inputFormat.parse(timestamp));
        } catch (Exception e) {
            return timestamp;
        }
    }
    
    private void showEditProfileDialog() {
        JDialog dialog = new JDialog(parentFrame, "Edit Profile", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(parentFrame);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(CARD);
        
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 8, 8, 8);
        g.weightx = 1;
        
        // Full Name
        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("Full Name:"), g);
        g.gridx = 1;
        JTextField fName = new JTextField(currentUser.getFullName());
        panel.add(fName, g);
        
        // Username (read-only)
        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("Username:"), g);
        g.gridx = 1;
        JTextField fUsername = new JTextField(currentUser.getUsername());
        fUsername.setEditable(false);
        fUsername.setBackground(new Color(0xF0F4F8));
        panel.add(fUsername, g);
        
        // Kebele (read-only)
        g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("Kebele:"), g);
        g.gridx = 1;
        JTextField fKebele = new JTextField(currentUser.getKebeleId());
        fKebele.setEditable(false);
        fKebele.setBackground(new Color(0xF0F4F8));
        panel.add(fKebele, g);
        
        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        
        JButton btnSave = new JButton("Save Changes");
        btnSave.setBackground(SUCCESS);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setBackground(TXT_MUTED);
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        
        btnSave.addActionListener(e -> {
            String newName = fName.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty!", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                String sql = "UPDATE users SET full_name = ? WHERE id = ?";
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                    ps.setString(1, newName);
                    ps.setInt(2, currentUser.getId());
                    ps.executeUpdate();
                }
                
                DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                                           "PROFILE_UPDATE", "Updated profile information");
                
                JOptionPane.showMessageDialog(dialog, "Profile updated successfully!", 
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                
                // Refresh the panel
                loadNotifications();
                revalidate();
                repaint();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to update profile: " + ex.getMessage(),
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        panel.add(btnPanel, g);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
    
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog(parentFrame, "Change Password", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(450, 280);
        dialog.setLocationRelativeTo(parentFrame);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(CARD);
        
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 8, 8, 8);
        g.weightx = 1;
        
        // Current Password
        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("Current Password:"), g);
        g.gridx = 1;
        JPasswordField fCurrent = new JPasswordField();
        panel.add(fCurrent, g);
        
        // New Password
        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("New Password:"), g);
        g.gridx = 1;
        JPasswordField fNew = new JPasswordField();
        panel.add(fNew, g);
        
        // Confirm Password
        g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("Confirm Password:"), g);
        g.gridx = 1;
        JPasswordField fConfirm = new JPasswordField();
        panel.add(fConfirm, g);
        
        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        
        JButton btnChange = new JButton("Change Password");
        btnChange.setBackground(SUCCESS);
        btnChange.setForeground(Color.WHITE);
        btnChange.setFocusPainted(false);
        
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setBackground(TXT_MUTED);
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        
        btnChange.addActionListener(e -> {
            String current = new String(fCurrent.getPassword());
            String newPass = new String(fNew.getPassword());
            String confirm = new String(fConfirm.getPassword());
            
            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required!", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPass.equals(confirm)) {
                JOptionPane.showMessageDialog(dialog, "New passwords do not match!", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPass.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 6 characters!", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Verify current password
                String sql = "SELECT password FROM users WHERE id = ?";
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                    ps.setInt(1, currentUser.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String storedHash = rs.getString("password");
                            if (!PasswordUtils.verifyPassword(current, storedHash)) {
                                JOptionPane.showMessageDialog(dialog, "Current password is incorrect!", 
                                                            "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                    }
                }
                
                // Update password
                String updateSql = "UPDATE users SET password = ? WHERE id = ?";
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(updateSql)) {
                    ps.setString(1, PasswordUtils.hashPassword(newPass));
                    ps.setInt(2, currentUser.getId());
                    ps.executeUpdate();
                }
                
                DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                                           "PASSWORD_CHANGE", "Changed password");
                
                JOptionPane.showMessageDialog(dialog, "Password changed successfully!", 
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to change password: " + ex.getMessage(),
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnChange);
        btnPanel.add(btnCancel);
        
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        panel.add(btnPanel, g);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
    
    private void viewMyResidentRecord() {
        JOptionPane.showMessageDialog(this, 
            "This will show your resident ID record.\nNavigate to the Residents tab to view and edit your information.",
            "My ID Record", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAllActivity() {
        JDialog dialog = new JDialog(parentFrame, "Activity Log", true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(parentFrame);
        
        String[] columns = {"Action", "Details", "Timestamp"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        
        try {
            String sql = "SELECT action, details, timestamp FROM activity_log " +
                        "WHERE user_id = ? ORDER BY timestamp DESC LIMIT 50";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("action"),
                            rs.getString("details"),
                            formatTimestamp(rs.getString("timestamp"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        JTable table = new JTable(model);
        table.setFont(F_LABEL);
        table.setRowHeight(30);
        
        JScrollPane scroll = new JScrollPane(table);
        dialog.add(scroll, BorderLayout.CENTER);
        
        dialog.setVisible(true);
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                                       "LOGOUT", "Logged out from profile");
            
            parentFrame.dispose();
            
            SwingUtilities.invokeLater(() -> {
                try {
                    DatabaseManager.initDatabase();
                    auth.LoginFrame loginFrame = new auth.LoginFrame();
                    loginFrame.setIconImages(parentFrame.getIconImages());
                    loginFrame.setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }
}
