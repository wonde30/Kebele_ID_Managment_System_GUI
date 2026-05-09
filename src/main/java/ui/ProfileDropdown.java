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

/**
 * Profile dropdown component for the header area.
 * Shows a circular profile icon that opens a dropdown menu when clicked.
 */
public class ProfileDropdown extends JPanel {
    
    // Professional Color Scheme
    // Using ColorScheme class for consistent professional colors
    
    // Fonts
    private static final Font F_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_BOLD  = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 10);
    
    private User currentUser;
    private JFrame parentFrame;
    private JPopupMenu dropdownMenu;
    private JLabel profileIcon;
    private int notificationCount = 0;
    
    public ProfileDropdown(User user, JFrame parent) {
        this.currentUser = user;
        this.parentFrame = parent;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        setOpaque(false);
        
        createProfileIcon();
        createDropdownMenu();
        loadNotificationCount();
    }
    
    /**
     * Refreshes the profile icon to show updated photo
     */
    public void refreshProfileIcon() {
        if (profileIcon != null) {
            profileIcon.repaint();
        }
    }
    
    private void createProfileIcon() {
        // Create circular profile icon with LARGER SIZE
        profileIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Try to load user photo
                Image userPhoto = loadUserPhoto();
                
                if (userPhoto != null) {
                    // Draw circular photo with high quality
                    g2d.setClip(new java.awt.geom.Ellipse2D.Float(5, 5, getWidth() - 10, getHeight() - 10));
                    g2d.drawImage(userPhoto, 5, 5, getWidth() - 10, getHeight() - 10, null);
                    g2d.setClip(null);
                    
                    // Draw elegant border
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(6));
                    g2d.drawOval(5, 5, getWidth() - 10, getHeight() - 10);
                } else {
                    // Draw circle background (fallback to initial)
                    g2d.setColor(getRoleColor());
                    g2d.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
                    
                    // Draw border
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(6));
                    g2d.drawOval(5, 5, getWidth() - 10, getHeight() - 10);
                    
                    // Draw user initial
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 48));
                    FontMetrics fm = g2d.getFontMetrics();
                    String initial = currentUser.getFullName().substring(0, 1).toUpperCase();
                    int x = (getWidth() - fm.stringWidth(initial)) / 2;
                    int y = (getHeight() + fm.getAscent()) / 2 - 2;
                    g2d.drawString(initial, x, y);
                }
                
                // Draw notification badge
                if (notificationCount > 0) {
                    g2d.setColor(ColorScheme.ERROR);
                    g2d.fillOval(getWidth() - 28, 0, 26, 26);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(getWidth() - 28, 0, 26, 26);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    String count = notificationCount > 9 ? "9+" : String.valueOf(notificationCount);
                    FontMetrics fmBadge = g2d.getFontMetrics();
                    int badgeX = getWidth() - 28 + (26 - fmBadge.stringWidth(count)) / 2;
                    int badgeY = (26 + fmBadge.getAscent()) / 2 - 1;
                    g2d.drawString(count, badgeX, badgeY);
                }
                
                g2d.dispose();
            }
        };
        
        profileIcon.setPreferredSize(new Dimension(120, 120));
        profileIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileIcon.setToolTipText(currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        
        // Click handler to show dropdown
        profileIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showDropdown();
            }
        });
        
        add(profileIcon);
    }
    
    private Image loadUserPhoto() {
        try {
            // First, try to get photo from linked resident (for Viewers)
            String sql = "SELECT u.photo_path as user_photo, r.photo_path as resident_photo " +
                        "FROM users u " +
                        "LEFT JOIN residents r ON u.linked_resident_id = r.id " +
                        "WHERE u.id = ?";
            
            try (java.sql.PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Try resident photo first (for Viewers with linked residents)
                        String residentPhoto = rs.getString("resident_photo");
                        if (residentPhoto != null && !residentPhoto.isEmpty()) {
                            Image img = loadImageFromPath(residentPhoto);
                            if (img != null) return img;
                        }
                        
                        // Fall back to user photo
                        String userPhoto = rs.getString("user_photo");
                        if (userPhoto != null && !userPhoto.isEmpty()) {
                            return loadImageFromPath(userPhoto);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail and use fallback
        }
        return null;
    }
    
    private Image loadImageFromPath(String photoPath) {
        try {
            // Check if it's an embedded resource
            if (photoPath.startsWith("resource:")) {
                String resourceName = photoPath.substring(9); // Remove "resource:" prefix
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
    
    private void createDropdownMenu() {
        dropdownMenu = new JPopupMenu();
        
        // Professional shadow border
        dropdownMenu.setBorder(new CompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB), 1),
                BorderFactory.createEmptyBorder(0, 0, 4, 4)
            ),
            new EmptyBorder(0, 0, 0, 0)
        ));
        
        dropdownMenu.setBackground(Color.WHITE);
        
        // ========== PROFESSIONAL USER INFO HEADER ==========
        JPanel userInfo = new JPanel(new BorderLayout(12, 0));
        userInfo.setBackground(new Color(0xF9FAFB));
        userInfo.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
            new EmptyBorder(16, 16, 16, 16)
        ));
        userInfo.setPreferredSize(new Dimension(320, 90));
        
        // Mini profile icon (50x50)
        JLabel miniIcon = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                
                int size = 50;
                Image photo = loadUserPhoto();
                
                if (photo != null) {
                    g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
                    g2d.drawImage(photo, 0, 0, size, size, null);
                    g2d.setClip(null);
                    g2d.setColor(new Color(0xE5E7EB));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(0, 0, size, size);
                } else {
                    g2d.setColor(getRoleColor());
                    g2d.fillOval(0, 0, size, size);
                    g2d.setColor(new Color(0xE5E7EB));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(0, 0, size, size);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
                    FontMetrics fm = g2d.getFontMetrics();
                    String initial = currentUser.getFullName().substring(0, 1).toUpperCase();
                    int x = (size - fm.stringWidth(initial)) / 2;
                    int y = (size + fm.getAscent()) / 2 - 2;
                    g2d.drawString(initial, x, y);
                }
                g2d.dispose();
            }
        };
        miniIcon.setPreferredSize(new Dimension(50, 50));
        
        // Info panel
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        
        JLabel name = new JLabel(currentUser.getFullName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(new Color(0x111827));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel role = new JLabel(currentUser.getRole() + " • " + currentUser.getKebeleId());
        role.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        role.setForeground(new Color(0x6B7280));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel status = new JLabel("● " + currentUser.getStatus());
        status.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        status.setForeground(currentUser.getStatus().equals("Active") ? new Color(0x10B981) : new Color(0xEF4444));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        info.add(name);
        info.add(Box.createVerticalStrut(4));
        info.add(role);
        info.add(Box.createVerticalStrut(4));
        info.add(status);
        
        userInfo.add(miniIcon, BorderLayout.WEST);
        userInfo.add(info, BorderLayout.CENTER);
        
        dropdownMenu.add(userInfo);
        
        // ========== PROFESSIONAL MENU ITEMS ==========
        addProfessionalMenuItem("View Profile", "👤", this::showProfileDialog);
        addProfessionalMenuItem("Edit Profile", "✏️", this::showEditProfileDialog);
        addProfessionalMenuItem("Notifications (" + notificationCount + ")", "🔔", this::showNotifications);
        
        addMenuSeparator();
        
        addProfessionalMenuItem("Change Password", "🔑", this::showChangePasswordDialog);
        
        // Viewer-specific option
        if ("Viewer".equals(currentUser.getRole())) {
            addProfessionalMenuItem("My ID Record", "📄", this::viewMyRecord);
        }
        
        addMenuSeparator();
        
        addProfessionalMenuItem("Logout", "🚪", this::logout);
    }
    
    private void addProfessionalMenuItem(String text, String icon, Runnable action) {
        JPanel itemPanel = new JPanel(new BorderLayout(12, 0));
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        itemPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        itemPanel.setPreferredSize(new Dimension(320, 48));
        
        // Icon label
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        iconLabel.setPreferredSize(new Dimension(24, 24));
        
        // Text label
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textLabel.setForeground(new Color(0x374151));
        
        itemPanel.add(iconLabel, BorderLayout.WEST);
        itemPanel.add(textLabel, BorderLayout.CENTER);
        
        // Hover effect
        itemPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                itemPanel.setBackground(new Color(0xF3F4F6));
                textLabel.setForeground(new Color(0x111827));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                itemPanel.setBackground(Color.WHITE);
                textLabel.setForeground(new Color(0x374151));
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                dropdownMenu.setVisible(false);
                action.run();
            }
        });
        
        dropdownMenu.add(itemPanel);
    }
    
    private void addMenuSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(0xE5E7EB));
        separator.setBackground(new Color(0xE5E7EB));
        separator.setPreferredSize(new Dimension(320, 1));
        dropdownMenu.add(separator);
    }
    
    
    private void showDropdown() {
        // Refresh notification count before showing
        loadNotificationCount();
        updateNotificationMenuItem();
        
        // Show dropdown below the profile icon
        dropdownMenu.show(profileIcon, 0, profileIcon.getHeight() + 5);
    }
    
    private void updateNotificationMenuItem() {
        // Update the notification menu item text
        Component[] components = dropdownMenu.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Component[] panelComps = panel.getComponents();
                for (Component pc : panelComps) {
                    if (pc instanceof JLabel) {
                        JLabel label = (JLabel) pc;
                        if (label.getText().contains("Notifications")) {
                            label.setText("Notifications (" + notificationCount + ")");
                            break;
                        }
                    }
                }
            }
        }
    }
    
    private Color getRoleColor() {
        return ColorScheme.getRoleColor(currentUser.getRole());
    }
    
    private String getUserIcon() {
        return switch (currentUser.getRole()) {
            case "Admin" -> "👑";
            case "Supervisor" -> "👨‍💼";
            case "Staff" -> "👨‍💻";
            case "DataEncoder" -> "✍️";
            case "Viewer" -> "👤";
            default -> "👤";
        };
    }
    
    private void loadNotificationCount() {
        try {
            String sql = "SELECT COUNT(*) FROM activity_log WHERE user_id = ? AND timestamp > datetime('now', '-24 hours')";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        notificationCount = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            notificationCount = 0;
        }
        
        if (profileIcon != null) {
            profileIcon.repaint();
        }
    }
    
    private void showProfileDialog() {
        JDialog dialog = new JDialog(parentFrame, "My Profile", true);
        dialog.setSize(950, 750);
        dialog.setLocationRelativeTo(parentFrame);
        
        // Main panel with scroll
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.BG_MAIN);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // ========== PROFILE CARD WITH HUGE PHOTO - PROFESSIONAL DESIGN ==========
        JPanel profileCard = new JPanel(new BorderLayout(40, 0));
        profileCard.setBackground(Color.WHITE);
        profileCard.setBorder(new EmptyBorder(50, 50, 50, 50));
        profileCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));
        
        // LEFT: PROFESSIONAL HUGE PHOTO PANEL (300x300px)
        JPanel photoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                
                int size = 300; // PROFESSIONAL HUGE SIZE
                int x = (getWidth() - size) / 2;
                int y = 10;
                
                // Try to load photo
                Image photo = loadUserPhotoForDialog();
                
                if (photo != null) {
                    // Draw soft shadow
                    g2d.setColor(new Color(0, 0, 0, 15));
                    for (int i = 0; i < 12; i++) {
                        int alpha = (int)(15 * (1 - i / 12.0));
                        g2d.setColor(new Color(0, 0, 0, alpha));
                        g2d.fillOval(x - i, y + i, size + (i * 2), size + (i * 2));
                    }
                    
                    // Draw circular photo with high quality
                    g2d.setClip(new java.awt.geom.Ellipse2D.Float(x, y, size, size));
                    g2d.drawImage(photo, x, y, size, size, null);
                    g2d.setClip(null);
                    
                    // Elegant white border
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(8));
                    g2d.drawOval(x, y, size, size);
                    
                    // Subtle outer accent
                    g2d.setColor(new Color(0xD1D5DB));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawOval(x - 4, y - 4, size + 8, size + 8);
                } else {
                    // Elegant gradient fallback
                    Color roleColor = ColorScheme.getRoleColor(currentUser.getRole());
                    GradientPaint gradient = new GradientPaint(
                        x, y, roleColor,
                        x + size, y + size, roleColor.darker().darker()
                    );
                    g2d.setPaint(gradient);
                    g2d.fillOval(x, y, size, size);
                    
                    // White border
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(8));
                    g2d.drawOval(x, y, size, size);
                    
                    // Draw initial
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
        
        // RIGHT: INFO PANEL
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // Name
        JLabel nameLabel = new JLabel(currentUser.getFullName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        nameLabel.setForeground(new Color(0x1F2937));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Role badge
        JLabel roleLabel = new JLabel("  " + currentUser.getRole() + "  ");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        roleLabel.setForeground(Color.WHITE);
        roleLabel.setOpaque(true);
        roleLabel.setBackground(ColorScheme.getRoleColor(currentUser.getRole()));
        roleLabel.setBorder(new EmptyBorder(6, 16, 6, 16));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(roleLabel);
        infoPanel.add(Box.createVerticalStrut(25));
        
        // Info rows
        addInfoRowToPanel(infoPanel, "👤", "Username", currentUser.getUsername());
        addInfoRowToPanel(infoPanel, "🏢", "Kebele", currentUser.getKebeleId());
        addInfoRowToPanel(infoPanel, "📊", "Status", currentUser.getStatus());
        addInfoRowToPanel(infoPanel, "🆔", "User ID", "#" + currentUser.getId());
        
        profileCard.add(photoPanel, BorderLayout.WEST);
        profileCard.add(infoPanel, BorderLayout.CENTER);
        
        mainPanel.add(profileCard);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // ========== ACTIONS CARD - PROFESSIONAL DESIGN ==========
        JPanel actionsCard = new JPanel();
        actionsCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        actionsCard.setBackground(Color.WHITE);
        actionsCard.setBorder(new EmptyBorder(30, 30, 30, 30));
        actionsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        JButton btnEdit = new JButton("✏️ Edit Profile");
        JButton btnPassword = new JButton("🔑 Change Password");
        JButton btnLogout = new JButton("🚪 Logout");
        
        for (JButton btn : new JButton[]{btnEdit, btnPassword, btnLogout}) {
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btn.setForeground(Color.WHITE);
            btn.setBackground(ColorScheme.PRIMARY);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(12, 24, 12, 24));
            actionsCard.add(btn);
        }
        
        btnEdit.addActionListener(e -> { dialog.dispose(); showEditProfileDialog(); });
        btnPassword.addActionListener(e -> { dialog.dispose(); showChangePasswordDialog(); });
        btnLogout.addActionListener(e -> { dialog.dispose(); logout(); });
        
        mainPanel.add(actionsCard);
        
        // Add scroll
        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        dialog.add(scroll);
        dialog.setVisible(true);
    }
    
    private void addInfoRowToPanel(JPanel parent, String icon, String label, String value) {
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
        lblValue.setForeground(new Color(0x1F2937));
        
        row.add(iconLabel);
        row.add(lblLabel);
        row.add(lblValue);
        
        parent.add(row);
    }
    
    private Image loadUserPhotoForDialog() {
        try {
            // Check linked resident photo
            String sql = "SELECT r.photo_path FROM users u " +
                        "LEFT JOIN residents r ON u.linked_resident_id = r.id " +
                        "WHERE u.id = ?";
            
            try (java.sql.PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String photoPath = rs.getString("photo_path");
                        if (photoPath != null && !photoPath.isEmpty()) {
                            Image img = loadImageFromPathForDialog(photoPath);
                            if (img != null) return img;
                        }
                    }
                }
            }
            
            // Check user photo
            sql = "SELECT photo_path FROM users WHERE id = ?";
            try (java.sql.PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String photoPath = rs.getString("photo_path");
                        if (photoPath != null && !photoPath.isEmpty()) {
                            return loadImageFromPathForDialog(photoPath);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        return null;
    }
    
    private Image loadImageFromPathForDialog(String photoPath) {
        try {
            if (photoPath.startsWith("resource:")) {
                String resourceName = photoPath.substring(9);
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                if (is != null) {
                    return new ImageIcon(is.readAllBytes()).getImage();
                }
            } else {
                java.io.File file = new java.io.File(photoPath);
                if (file.exists()) {
                    return new ImageIcon(photoPath).getImage();
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        return null;
    }
    
    private void showEditProfileDialog() {
        JDialog dialog = new JDialog(parentFrame, "Edit Profile", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(parentFrame);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(ColorScheme.BG_CARD);
        
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
        fUsername.setBackground(ColorScheme.BG_SIDEBAR);
        panel.add(fUsername, g);
        
        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        
        JButton btnSave = new JButton("Save");
        JButton btnCancel = new JButton("Cancel");
        
        btnSave.addActionListener(e -> {
            String newName = fName.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty!");
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
                                           "PROFILE_UPDATE", "Updated profile name");
                
                JOptionPane.showMessageDialog(dialog, "Profile updated successfully!");
                dialog.dispose();
                
                // Refresh the dropdown
                loadNotificationCount();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to update: " + ex.getMessage());
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        panel.add(btnPanel, g);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showChangePasswordDialog() {
        JDialog dialog = new JDialog(parentFrame, "Change Password", true);
        dialog.setSize(400, 280);
        dialog.setLocationRelativeTo(parentFrame);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(ColorScheme.BG_CARD);
        
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
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        
        JButton btnChange = new JButton("Change Password");
        JButton btnCancel = new JButton("Cancel");
        
        btnChange.addActionListener(e -> {
            String current = new String(fCurrent.getPassword());
            String newPass = new String(fNew.getPassword());
            String confirm = new String(fConfirm.getPassword());
            
            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields are required!");
                return;
            }
            
            if (!newPass.equals(confirm)) {
                JOptionPane.showMessageDialog(dialog, "New passwords do not match!");
                return;
            }
            
            if (newPass.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 6 characters!");
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
                                JOptionPane.showMessageDialog(dialog, "Current password is incorrect!");
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
                
                JOptionPane.showMessageDialog(dialog, "Password changed successfully!");
                dialog.dispose();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to change password: " + ex.getMessage());
            }
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnChange);
        btnPanel.add(btnCancel);
        
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        panel.add(btnPanel, g);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void showNotifications() {
        JDialog dialog = new JDialog(parentFrame, "Recent Activity", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parentFrame);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create table for activities
        String[] columns = {"Action", "Details", "Time"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        
        try {
            String sql = "SELECT action, details, timestamp FROM activity_log " +
                        "WHERE user_id = ? ORDER BY timestamp DESC LIMIT 20";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("action"),
                            rs.getString("details"),
                            rs.getString("timestamp")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        JTable table = new JTable(model);
        table.setFont(F_LABEL);
        table.setRowHeight(25);
        
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void viewMyRecord() {
        JOptionPane.showMessageDialog(parentFrame,
            "Navigate to the Residents tab to view and edit your ID record.",
            "My ID Record", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(parentFrame,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                                       "LOGOUT", "Logged out");
            
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