package auth;

import db.DatabaseManager;
import ui.ColorScheme;
import utils.PasswordUtils;
import utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

/**
 * Dialog for password reset functionality.
 * Allows users to reset their password by verifying their username and phone number.
 */
public class PasswordResetDialog extends JDialog {

    private JTextField fUsername;
    private JTextField fPhone;
    private JPasswordField fNewPassword;
    private JPasswordField fConfirmPassword;
    private JLabel lblMessage;
    private JButton btnVerify;
    private JButton btnReset;
    private JButton btnCancel;
    
    private int userId = -1;
    private boolean verified = false;

    public PasswordResetDialog(JFrame parent) {
        super(parent, "Password Reset", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(ColorScheme.BG_CARD);
        main.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel title = new JLabel("🔑 Reset Password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(ColorScheme.PRIMARY);
        
        JLabel subtitle = new JLabel("Verify your identity to reset your password");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(ColorScheme.TEXT_SECONDARY);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);
        
        header.add(titlePanel, BorderLayout.CENTER);

        // Form
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // Step 1: Verification
        JLabel step1 = new JLabel("Step 1: Verify Identity");
        step1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        step1.setForeground(ColorScheme.PRIMARY);
        step1.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(step1);
        form.add(Box.createVerticalStrut(10));

        form.add(createLabel("Username"));
        form.add(Box.createVerticalStrut(5));
        fUsername = createTextField("Enter your username");
        form.add(fUsername);
        form.add(Box.createVerticalStrut(15));

        form.add(createLabel("Phone Number"));
        form.add(Box.createVerticalStrut(5));
        fPhone = createTextField("Enter registered phone number");
        form.add(fPhone);
        form.add(Box.createVerticalStrut(15));

        btnVerify = createButton("Verify Identity", ColorScheme.BTN_PRIMARY);
        btnVerify.addActionListener(e -> verifyIdentity());
        form.add(btnVerify);

        form.add(Box.createVerticalStrut(20));

        // Step 2: New Password (initially disabled)
        JLabel step2 = new JLabel("Step 2: Set New Password");
        step2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        step2.setForeground(ColorScheme.TEXT_MUTED);
        step2.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(step2);
        form.add(Box.createVerticalStrut(10));

        form.add(createLabel("New Password"));
        form.add(Box.createVerticalStrut(5));
        fNewPassword = createPasswordField("Enter new password (min 8 chars)");
        fNewPassword.setEnabled(false);
        form.add(fNewPassword);
        form.add(Box.createVerticalStrut(15));

        form.add(createLabel("Confirm Password"));
        form.add(Box.createVerticalStrut(5));
        fConfirmPassword = createPasswordField("Re-enter new password");
        fConfirmPassword.setEnabled(false);
        form.add(fConfirmPassword);
        form.add(Box.createVerticalStrut(15));

        // Message label
        lblMessage = new JLabel(" ");
        lblMessage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblMessage.setForeground(ColorScheme.ERROR);
        lblMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblMessage);
        form.add(Box.createVerticalStrut(10));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnReset = createButton("Reset Password", ColorScheme.BTN_SUCCESS);
        btnReset.setEnabled(false);
        btnReset.addActionListener(e -> resetPassword());

        btnCancel = createButton("Cancel", ColorScheme.TEXT_MUTED);
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnReset);
        form.add(buttonPanel);

        main.add(header, BorderLayout.NORTH);
        main.add(form, BorderLayout.CENTER);

        setContentPane(main);
    }

    private void verifyIdentity() {
        String username = fUsername.getText().trim();
        String phone = fPhone.getText().trim();

        // Validation
        if (username.isEmpty()) {
            showMessage("Please enter your username", ColorScheme.ERROR);
            return;
        }

        if (phone.isEmpty()) {
            showMessage("Please enter your phone number", ColorScheme.ERROR);
            return;
        }

        // Validate username format
        if (!ValidationUtils.isValidUsername(username)) {
            showMessage("Invalid username format", ColorScheme.ERROR);
            return;
        }

        // Validate phone format
        if (!ValidationUtils.isValidPhone(phone)) {
            showMessage("Invalid phone format. Use: +251 9XX XXX XXX", ColorScheme.ERROR);
            return;
        }

        try {
            // Check if user exists with matching phone
            String sql = "SELECT u.id, u.full_name, r.phone_number " +
                        "FROM users u " +
                        "LEFT JOIN residents r ON u.linked_resident_id = r.id " +
                        "WHERE u.username = ? AND u.status = 'Active'";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String dbPhone = rs.getString("phone_number");
                        
                        if (dbPhone != null && dbPhone.equals(phone)) {
                            // Verification successful
                            userId = rs.getInt("id");
                            verified = true;
                            
                            showMessage("✓ Identity verified! You can now set a new password.", ColorScheme.SUCCESS);
                            
                            // Enable password fields
                            fNewPassword.setEnabled(true);
                            fConfirmPassword.setEnabled(true);
                            btnReset.setEnabled(true);
                            
                            // Disable verification fields
                            fUsername.setEnabled(false);
                            fPhone.setEnabled(false);
                            btnVerify.setEnabled(false);
                            
                            // Update step 2 label color
                            Component[] components = ((JPanel)getContentPane()).getComponents();
                            for (Component comp : components) {
                                if (comp instanceof JPanel) {
                                    for (Component inner : ((JPanel)comp).getComponents()) {
                                        if (inner instanceof JLabel) {
                                            JLabel label = (JLabel) inner;
                                            if (label.getText().equals("Step 2: Set New Password")) {
                                                label.setForeground(ColorScheme.PRIMARY);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            DatabaseManager.logActivity(userId, username, "PASSWORD_RESET_VERIFY", 
                                "Identity verified for password reset");
                        } else {
                            showMessage("Phone number does not match our records", ColorScheme.ERROR);
                            DatabaseManager.logActivity(0, username, "PASSWORD_RESET_FAILED", 
                                "Phone number mismatch");
                        }
                    } else {
                        showMessage("Username not found or account inactive", ColorScheme.ERROR);
                        DatabaseManager.logActivity(0, username, "PASSWORD_RESET_FAILED", 
                            "User not found");
                    }
                }
            }
        } catch (SQLException e) {
            showMessage("Database error: " + e.getMessage(), ColorScheme.ERROR);
            e.printStackTrace();
        }
    }

    private void resetPassword() {
        if (!verified) {
            showMessage("Please verify your identity first", ColorScheme.ERROR);
            return;
        }

        String newPassword = new String(fNewPassword.getPassword());
        String confirmPassword = new String(fConfirmPassword.getPassword());

        // Validation
        if (newPassword.isEmpty()) {
            showMessage("Please enter a new password", ColorScheme.ERROR);
            return;
        }

        if (newPassword.length() < 8) {
            showMessage("Password must be at least 8 characters", ColorScheme.ERROR);
            return;
        }

        if (!ValidationUtils.isValidPassword(newPassword)) {
            showMessage("Password must contain uppercase, lowercase, digit, and special character", ColorScheme.ERROR);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showMessage("Passwords do not match", ColorScheme.ERROR);
            fConfirmPassword.setText("");
            return;
        }

        try {
            // Update password
            String sql = "UPDATE users SET password = ? WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, PasswordUtils.hashPassword(newPassword));
                ps.setInt(2, userId);
                int updated = ps.executeUpdate();
                
                if (updated > 0) {
                    DatabaseManager.logActivity(userId, fUsername.getText(), "PASSWORD_RESET", 
                        "Password reset successful");
                    
                    JOptionPane.showMessageDialog(this,
                            "Password reset successfully!\n\nYou can now login with your new password.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    
                    dispose();
                } else {
                    showMessage("Failed to update password", ColorScheme.ERROR);
                }
            }
        } catch (SQLException e) {
            showMessage("Database error: " + e.getMessage(), ColorScheme.ERROR);
            e.printStackTrace();
        }
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(ColorScheme.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setText(placeholder);
        field.setForeground(ColorScheme.TEXT_MUTED);
        
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(ColorScheme.TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(ColorScheme.TEXT_MUTED);
                }
            }
        });
        
        return field;
    }

    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.setForeground(ColorScheme.TEXT_MUTED);
        
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (new String(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('●');
                    field.setForeground(ColorScheme.TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (new String(field.getPassword()).isEmpty()) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(ColorScheme.TEXT_MUTED);
                }
            }
        });
        
        return field;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    private void showMessage(String message, Color color) {
        lblMessage.setText(message);
        lblMessage.setForeground(color);
    }

    /**
     * Shows the password reset dialog
     */
    public static void showDialog(JFrame parent) {
        PasswordResetDialog dialog = new PasswordResetDialog(parent);
        dialog.setVisible(true);
    }
}
