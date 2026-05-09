package ui;

import model.User;
import db.DatabaseManager;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Enhanced resident registration form with comprehensive fields.
 * Includes all fields from the new database schema.
 */
public class EnhancedResidentForm extends JPanel {
    
    // Colors
    private static final Color PRIMARY   = new Color(0x0D3B66);
    private static final Color SECONDARY = new Color(0x1B6CA8);
    private static final Color BG        = new Color(0xF2F5F9);
    private static final Color CARD      = Color.WHITE;
    private static final Color SUCCESS   = new Color(0x2D9E6B);
    private static final Color DANGER    = new Color(0xD62839);
    private static final Color WARN      = new Color(0xE07B39);
    private static final Color TXT       = new Color(0x1A1A2E);
    private static final Color TXT_MUTED = new Color(0x7F8C9A);
    
    // Fonts
    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD,  16);
    private static final Font F_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_BTN   = new Font("Segoe UI", Font.BOLD,  12);
    
    // Form fields - Personal Information
    private JTextField fKebeleId, fFirst, fLast, fFather, fMother, fBirth;
    private JComboBox<String> cGender;
    
    // Form fields - Contact & Address
    private JTextField fPhone, fAddress, fAddressDetail, fBirthPlace, fKebeleNumber;
    
    // Form fields - Additional Information
    private JTextField fOccupation, fIssuedDate;
    private JComboBox<String> cMaritalStatus, cBloodGroup, cStatus;
    
    // Photo
    private JLabel lblPhoto;
    private String photoPath = "";
    
    // Buttons
    private JButton btnSave, btnUpdate, btnDelete, btnClear, btnPhoto;
    
    // State
    private User currentUser;
    private int selectedId = -1;
    private Runnable onDataChanged;
    
    public EnhancedResidentForm(User user, Runnable onDataChanged) {
        this.currentUser = user;
        this.onDataChanged = onDataChanged;
        
        setLayout(new BorderLayout());
        setBackground(BG);
        
        // Create scrollable form
        JPanel formContent = buildFormContent();
        JScrollPane scroll = new JScrollPane(formContent);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scroll, BorderLayout.CENTER);
    }
    
    private JPanel buildFormContent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Add sections
        panel.add(buildPhotoSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildPersonalInfoSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildContactSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildAdditionalInfoSection());
        panel.add(Box.createVerticalStrut(10));
        panel.add(buildButtonSection());
        
        return panel;
    }
    
    private JPanel buildPhotoSection() {
        JPanel section = createSection("Photo");
        
        lblPhoto = new JLabel("No Photo", SwingConstants.CENTER);
        lblPhoto.setPreferredSize(new Dimension(100, 100));
        lblPhoto.setMaximumSize(new Dimension(100, 100));
        lblPhoto.setOpaque(true);
        lblPhoto.setBackground(new Color(0xEDF2F7));
        lblPhoto.setBorder(new LineBorder(new Color(0xCBD5E0), 1, true));
        lblPhoto.setFont(F_SMALL);
        lblPhoto.setForeground(TXT_MUTED);
        
        btnPhoto = createButton("Select Photo", SECONDARY, Color.WHITE);
        btnPhoto.addActionListener(e -> selectPhoto());
        
        JPanel photoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        photoPanel.setOpaque(false);
        photoPanel.add(lblPhoto);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        btnPanel.add(btnPhoto);
        
        section.add(photoPanel);
        section.add(btnPanel);
        
        return section;
    }
    
    private JPanel buildPersonalInfoSection() {
        JPanel section = createSection("Personal Information");
        
        // Initialize fields
        fKebeleId = createField("KBL-2026-001");
        fFirst    = createField("First Name");
        fLast     = createField("Last Name");
        fFather   = createField("Father's Name");
        fMother   = createField("Mother's Name (Optional)");
        fBirth    = createField("YYYY-MM-DD");
        
        cGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        styleComboBox(cGender);
        
        // Kebele ID is read-only
        fKebeleId.setEditable(false);
        fKebeleId.setBackground(new Color(0xF0F4F8));
        fKebeleId.setForeground(TXT_MUTED);
        fKebeleId.setToolTipText("System-assigned, read-only");
        
        // Admin can edit Kebele ID
        if (currentUser.canDeleteResident()) {
            fKebeleId.setEditable(true);
            fKebeleId.setBackground(new Color(0xFAFBFC));
            fKebeleId.setForeground(TXT);
        }
        
        // Add fields to section
        addFormRow(section, "Kebele ID (read-only):", fKebeleId);
        addFormRow(section, "First Name *:", fFirst);
        addFormRow(section, "Last Name *:", fLast);
        addFormRow(section, "Father's Name *:", fFather);
        addFormRow(section, "Mother's Name:", fMother);
        addFormRow(section, "Date of Birth * (YYYY-MM-DD):", fBirth);
        addFormRow(section, "Gender *:", cGender);
        
        return section;
    }
    
    private JPanel buildContactSection() {
        JPanel section = createSection("Contact & Address");
        
        fPhone         = createField("+251 9XX XXX XXX");
        fAddress       = createField("City / Sub-city");
        fAddressDetail = createField("Woreda / House No. / Details");
        fBirthPlace    = createField("Birth Place");
        fKebeleNumber  = createField("Kebele Number");
        
        addFormRow(section, "Phone Number:", fPhone);
        addFormRow(section, "Address:", fAddress);
        addFormRow(section, "Address Detail:", fAddressDetail);
        addFormRow(section, "Birth Place:", fBirthPlace);
        addFormRow(section, "Kebele Number:", fKebeleNumber);
        
        return section;
    }
    
    private JPanel buildAdditionalInfoSection() {
        JPanel section = createSection("Additional Information");
        
        fOccupation = createField("Occupation");
        fIssuedDate = createField("YYYY-MM-DD");
        
        cMaritalStatus = new JComboBox<>(new String[]{
            "Single", "Married", "Divorced", "Widowed", "Other"
        });
        styleComboBox(cMaritalStatus);
        
        cBloodGroup = new JComboBox<>(new String[]{
            "Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
        });
        styleComboBox(cBloodGroup);
        
        cStatus = new JComboBox<>(new String[]{
            "Active", "Inactive", "Deceased", "Relocated"
        });
        styleComboBox(cStatus);
        
        addFormRow(section, "Marital Status:", cMaritalStatus);
        addFormRow(section, "Blood Group:", cBloodGroup);
        addFormRow(section, "Occupation:", fOccupation);
        addFormRow(section, "ID Status:", cStatus);
        addFormRow(section, "Issued Date (YYYY-MM-DD):", fIssuedDate);
        
        return section;
    }
    
    private JPanel buildButtonSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(CARD);
        section.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xD0DBE8), 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        btnSave   = createButton("Save New Resident", SUCCESS, Color.WHITE);
        btnUpdate = createButton("Update Resident", WARN, Color.WHITE);
        btnDelete = createButton("Delete Resident", DANGER, Color.WHITE);
        btnClear  = createButton("Clear Form", new Color(0x78909C), Color.WHITE);
        
        btnSave.addActionListener(e -> saveResident());
        btnUpdate.addActionListener(e -> updateResident());
        btnDelete.addActionListener(e -> deleteResident());
        btnClear.addActionListener(e -> clearForm());
        
        // Button layout
        JPanel btnRow1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnRow1.setOpaque(false);
        btnRow1.add(btnSave);
        btnRow1.add(btnUpdate);
        
        JPanel btnRow2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnRow2.setOpaque(false);
        btnRow2.add(btnDelete);
        btnRow2.add(btnClear);
        
        section.add(btnRow1);
        section.add(btnRow2);
        
        applyRolePermissions();
        
        return section;
    }
    
    private void applyRolePermissions() {
        btnSave.setEnabled(currentUser.canAddResident());
        btnUpdate.setEnabled(currentUser.canEditResident());
        btnDelete.setEnabled(currentUser.canDeleteResident());
    }
    
    private JPanel createSection(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(CARD);
        section.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xD0DBE8), 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(F_TITLE);
        titleLabel.setForeground(PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(10));
        
        return section;
    }
    
    private void addFormRow(JPanel parent, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(F_LABEL);
        lbl.setForeground(TXT);
        lbl.setPreferredSize(new Dimension(200, 30));
        
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        
        parent.add(row);
        parent.add(Box.createVerticalStrut(8));
    }
    
    private JTextField createField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(F_LABEL);
        field.setForeground(TXT_MUTED);
        field.setText(placeholder);
        field.setBorder(new CompoundBorder(
            new LineBorder(new Color(0xCBD5E0), 1, true),
            new EmptyBorder(7, 10, 7, 10)
        ));
        field.setBackground(new Color(0xFAFBFC));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TXT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TXT_MUTED);
                }
            }
        });
        
        return field;
    }
    
    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(F_LABEL);
        combo.setBackground(CARD);
        combo.setForeground(TXT);
        combo.setBorder(new LineBorder(new Color(0xCBD5E0), 1, true));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    }
    
    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(F_BTN);
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bg.darker());
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        
        return btn;
    }
    
    private void selectPhoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            photoPath = fc.getSelectedFile().getAbsolutePath();
            try {
                Image img = new ImageIcon(photoPath).getImage()
                        .getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                lblPhoto.setIcon(new ImageIcon(img));
                lblPhoto.setText("");
            } catch (Exception ex) {
                lblPhoto.setText("Bad Image");
            }
        }
    }
    
    private String getFieldValue(JTextField field, String placeholder) {
        String val = field.getText().trim();
        return val.equals(placeholder) ? "" : val;
    }
    
    private void saveResident() {
        if (!currentUser.canAddResident()) {
            showWarning("No permission to add residents.");
            return;
        }
        
        // Get required fields
        String kebeleId = getFieldValue(fKebeleId, "KBL-2026-001");
        String firstName = getFieldValue(fFirst, "First Name");
        String lastName = getFieldValue(fLast, "Last Name");
        String fatherName = getFieldValue(fFather, "Father's Name");
        String birthDate = getFieldValue(fBirth, "YYYY-MM-DD");
        
        // Validate required fields
        if (kebeleId.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || 
            fatherName.isEmpty() || birthDate.isEmpty()) {
            showWarning("Please fill all required fields (*).");
            return;
        }
        
        // Validate date format
        if (!birthDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showWarning("Birth date must be in YYYY-MM-DD format.");
            return;
        }
        
        try {
            String sql = "INSERT INTO residents (kebele_id, first_name, last_name, " +
                        "father_name, mother_name, date_of_birth, gender, marital_status, " +
                        "blood_group, phone_number, address, address_detail, birth_place, " +
                        "occupation, kebele_number, id_status, issued_date, photo_path, created_by) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, kebeleId);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setString(4, fatherName);
                ps.setString(5, getFieldValue(fMother, "Mother's Name (Optional)"));
                ps.setString(6, birthDate);
                ps.setString(7, (String) cGender.getSelectedItem());
                ps.setString(8, (String) cMaritalStatus.getSelectedItem());
                ps.setString(9, (String) cBloodGroup.getSelectedItem());
                ps.setString(10, getFieldValue(fPhone, "+251 9XX XXX XXX"));
                ps.setString(11, getFieldValue(fAddress, "City / Sub-city"));
                ps.setString(12, getFieldValue(fAddressDetail, "Woreda / House No. / Details"));
                ps.setString(13, getFieldValue(fBirthPlace, "Birth Place"));
                ps.setString(14, getFieldValue(fOccupation, "Occupation"));
                ps.setString(15, getFieldValue(fKebeleNumber, "Kebele Number"));
                ps.setString(16, (String) cStatus.getSelectedItem());
                ps.setString(17, getFieldValue(fIssuedDate, "YYYY-MM-DD"));
                ps.setString(18, photoPath);
                ps.setString(19, currentUser.getUsername());
                
                ps.executeUpdate();
                
                DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                        "ADD_RESIDENT", "Added: " + firstName + " " + lastName);
                
                showSuccess("Resident saved successfully!");
                clearForm();
                if (onDataChanged != null) onDataChanged.run();
            }
        } catch (SQLException ex) {
            showError("Save failed: " + ex.getMessage());
        }
    }
    
    private void updateResident() {
        showWarning("Update functionality - to be implemented with table selection");
    }
    
    private void deleteResident() {
        showWarning("Delete functionality - to be implemented with table selection");
    }
    
    public void clearForm() {
        // Clear all text fields
        setFieldPlaceholder(fKebeleId, "KBL-2026-001");
        setFieldPlaceholder(fFirst, "First Name");
        setFieldPlaceholder(fLast, "Last Name");
        setFieldPlaceholder(fFather, "Father's Name");
        setFieldPlaceholder(fMother, "Mother's Name (Optional)");
        setFieldPlaceholder(fBirth, "YYYY-MM-DD");
        setFieldPlaceholder(fPhone, "+251 9XX XXX XXX");
        setFieldPlaceholder(fAddress, "City / Sub-city");
        setFieldPlaceholder(fAddressDetail, "Woreda / House No. / Details");
        setFieldPlaceholder(fBirthPlace, "Birth Place");
        setFieldPlaceholder(fOccupation, "Occupation");
        setFieldPlaceholder(fKebeleNumber, "Kebele Number");
        setFieldPlaceholder(fIssuedDate, "YYYY-MM-DD");
        
        // Reset combo boxes
        cGender.setSelectedIndex(0);
        cMaritalStatus.setSelectedIndex(0);
        cBloodGroup.setSelectedIndex(0);
        cStatus.setSelectedIndex(0);
        
        // Clear photo
        lblPhoto.setIcon(null);
        lblPhoto.setText("No Photo");
        photoPath = "";
        
        selectedId = -1;
    }
    
    private void setFieldPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(TXT_MUTED);
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
