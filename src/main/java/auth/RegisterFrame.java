package auth;

import db.DatabaseManager;
import ui.ColorScheme;
import utils.PasswordUtils;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

public class RegisterFrame extends JFrame {

    // ── Professional Color Palette & Typography (Standardized) ────
    private static final Font F_HERO  = new Font("Segoe UI", Font.BOLD,  36);
    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD,  28); // 28px
    private static final Font F_LABEL = new Font("Segoe UI", Font.PLAIN, 13); // 13px medium
    private static final Font F_INPUT = new Font("Segoe UI", Font.PLAIN, 14); // 14px
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_BTN   = new Font("Segoe UI", Font.BOLD,  14);

    private JTextField     fFullName, fUsername, fFirstName, fLastName, fFatherName, fMotherName;
    private JTextField     fBirthDate, fPhone, fAddress, fAddressDetail, fBirthPlace, fOccupation;
    private JPasswordField fPassword, fConfirm;
    private JComboBox<String> cKebele, cGender, cMaritalStatus, cBloodGroup;
    private JLabel         lblMsg, lblPhoto;
    private JFrame         parent;
    private String         photoPath = "";
    
    // Password strength meter components
    private JPanel passwordStrengthBar;
    private JLabel passwordStrengthLabel;

    // Animated background
    private float animOffset = 0f;
    private Timer animTimer;

    public RegisterFrame(JFrame parent) {
        this.parent = parent;
        setTitle("Kebele ID System — Create Account");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // full screen
        setMinimumSize(new Dimension(1100, 700));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (animTimer != null) animTimer.stop();
                parent.setVisible(true);
            }
        });
        buildUI();
    }

    private void buildUI() {
        JPanel bg = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Animated diagonal gradient using professional colors
                GradientPaint gp = new GradientPaint(
                        (float)(w * 0.3 + Math.sin(animOffset) * 80), 0, ColorScheme.BG_DARK_PANEL,
                        w, h, ColorScheme.PRIMARY);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Decorative circles with professional transparency
                g2.setColor(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 50));
                g2.fillOval((int)(w*0.05 + Math.cos(animOffset*0.7)*30),
                        (int)(h*0.1 + Math.sin(animOffset*0.5)*20),
                        260, 260);
                g2.fillOval((int)(w*0.75 + Math.sin(animOffset*0.4)*25),
                        (int)(h*0.6 + Math.cos(animOffset*0.6)*30),
                        200, 200);
                g2.fillOval((int)(w*0.5  + Math.cos(animOffset*0.3)*40),
                        (int)(h*0.05 + Math.sin(animOffset*0.8)*15),
                        120, 120);
                g2.setColor(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 25));
                g2.fillOval((int)(w*0.85 + Math.sin(animOffset*0.5)*20),
                        (int)(h*0.05),
                        350, 350);

                // Subtle grid lines
                g2.setColor(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 12));
                g2.setStroke(new BasicStroke(1));
                for (int x = 0; x < w; x += 60)
                    g2.drawLine(x, 0, x, h);
                for (int y = 0; y < h; y += 60)
                    g2.drawLine(0, y, w, y);
            }
        };
        bg.setOpaque(true);

        animTimer = new Timer(50, e -> { animOffset += 0.03f; bg.repaint(); });
        animTimer.start();

        bg.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;

        gc.gridx = 0; gc.weightx = 0.48;
        bg.add(buildLeftPanel(), gc);

        gc.gridx = 1; gc.weightx = 0.52;
        bg.add(buildRightPanel(), gc);

        setContentPane(bg);
    }

    // ── LEFT PANEL ───────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0,0,0,70));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());
        p.setBorder(new EmptyBorder(60, 60, 60, 40));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // Logo Badge (KIS - consistent with login screen, NO "NEW" badge)
        JLabel badge = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Yellow circle background
                g2.setColor(ColorScheme.ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Dark text "KIS"
                g2.setColor(ColorScheme.BG_DARK_PANEL);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("KIS",
                        (getWidth()  - fm.stringWidth("KIS"))/2,
                        (getHeight() + fm.getAscent())/2 - 4);
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(72, 72));
        badge.setMaximumSize(new Dimension(72, 72));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hero = new JLabel(
                "<html>Join Kebele<br>ID System</html>");
        hero.setFont(F_HERO);
        hero.setForeground(ColorScheme.TEXT_ON_DARK);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Professional divider
        JPanel div = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(
                        0,0,ColorScheme.ACCENT,getWidth(),0,ColorScheme.withAlpha(ColorScheme.ACCENT, 0)));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        div.setOpaque(false);
        div.setPreferredSize(new Dimension(280,3));
        div.setMaximumSize(new Dimension(380,3));
        div.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel(
                "Register to access your digital identity");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        tagline.setForeground(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 200));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Info cards
        JPanel infoGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        infoGrid.setOpaque(false);
        infoGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoGrid.setMaximumSize(new Dimension(500, 200));

        String[][] items = {
                {"Auto Role Assignment",
                        "All new accounts start as Viewer"},
                {"Admin Upgrades",
                        "Admin assigns your role & permissions"},
                {"Secure Password",
                        "Passwords are encrypted with BCrypt"},
                {"Kebele Linked",
                        "Your account is tied to your Kebele"}
        };
        for (String[] it : items) infoGrid.add(infoCard(it[0], it[1]));

        // Role explanation panel
        JPanel roleBox = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorScheme.withAlpha(ColorScheme.ACCENT, 30));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(ColorScheme.withAlpha(ColorScheme.ACCENT, 80));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roleBox.setOpaque(false);
        roleBox.setBorder(new EmptyBorder(12,16,12,16));
        roleBox.setMaximumSize(new Dimension(500,100));
        roleBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel roleTitle = new JLabel("Registration Process:");
        roleTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleTitle.setForeground(ColorScheme.ACCENT);

        JLabel roleDesc = new JLabel(
                "<html><span style='color:white;font-size:11px;'>"
                        +"Viewer &rarr; DataEncoder &rarr; Staff "
                        +"&rarr; Supervisor &rarr; Admin<br>"
                        +"Contact your administrator to upgrade your role "
                        +"after registration.</span></html>");

        roleBox.add(roleTitle, BorderLayout.NORTH);
        roleBox.add(roleDesc,  BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "<html>Federal Democratic Republic of Ethiopia<br>" +
                "Developer: Wondatir Fetene | v2.0 | © 2026</html>");
        footer.setFont(F_SMALL);
        footer.setForeground(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 100));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        inner.add(badge);
        inner.add(Box.createVerticalStrut(18));
        inner.add(hero);
        inner.add(Box.createVerticalStrut(16));
        inner.add(div);
        inner.add(Box.createVerticalStrut(12));
        inner.add(tagline);
        inner.add(Box.createVerticalStrut(26));
        inner.add(infoGrid);
        inner.add(Box.createVerticalStrut(20));
        inner.add(roleBox);
        inner.add(Box.createVerticalGlue());
        inner.add(footer);

        p.add(inner);
        return p;
    }

    private JPanel infoCard(String title, String desc) {
        JPanel p = new JPanel() {
            private boolean hovered = false;
            
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Hover effect - subtle lift
                int offset = hovered ? -2 : 0;
                g2.translate(0, offset);
                
                g2.setColor(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, hovered ? 35 : 25));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, hovered ? 70 : 55));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BorderLayout(0,4));
        p.setBorder(new EmptyBorder(12,14,12,14)); // Uniform padding

        // Add hover listener
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                try {
                    java.lang.reflect.Field field = p.getClass().getDeclaredField("hovered");
                    field.setAccessible(true);
                    field.set(p, true);
                    p.repaint();
                } catch (Exception ex) {}
            }
            @Override
            public void mouseExited(MouseEvent e) {
                try {
                    java.lang.reflect.Field field = p.getClass().getDeclaredField("hovered");
                    field.setAccessible(true);
                    field.set(p, false);
                    p.repaint();
                } catch (Exception ex) {}
            }
        });

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI",Font.BOLD,12)); // Consistent font size
        t.setForeground(ColorScheme.ACCENT);

        JLabel d = new JLabel(
                "<html><body style='width:100px;'>"+desc+"</body></html>");
        d.setFont(F_SMALL);
        d.setForeground(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 200));

        p.add(t, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);
        return p;
    }

    // ── RIGHT PANEL — registration form ─────────────────────
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(30, 20, 30, 60));

        // Professional frosted glass card
        JPanel card = new JPanel(new BorderLayout(0,0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorScheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(ColorScheme.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(460, 700));
        card.setBorder(new EmptyBorder(32, 40, 28, 40));

        // Header
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Create Account");
        title.setFont(F_TITLE);
        title.setForeground(ColorScheme.PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Register to access the system");
        sub.setFont(F_SMALL);
        sub.setForeground(ColorScheme.TEXT_SECONDARY);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setPaint(new GradientPaint(0,0,ColorScheme.PRIMARY,getWidth(),0,ColorScheme.ACCENT));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),4,4);
            }
        };
        accent.setOpaque(false);
        accent.setPreferredSize(new Dimension(60,4));
        accent.setMaximumSize(new Dimension(60,4));
        accent.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        header.add(Box.createVerticalStrut(12));
        header.add(accent);

        // Scrollable form with ALL resident fields
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(Box.createVerticalStrut(20));
        
        // === PERSONAL INFORMATION ===
        form.add(sectionHeader("👤 Personal Information"));
        
        form.add(formLabel("First Name *"));
        form.add(Box.createVerticalStrut(5));
        fFirstName = formField("Enter first name");
        form.add(fFirstName);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Last Name *"));
        form.add(Box.createVerticalStrut(5));
        fLastName = formField("Enter last name");
        form.add(fLastName);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Father's Name *"));
        form.add(Box.createVerticalStrut(5));
        fFatherName = formField("Enter father's full name");
        form.add(fFatherName);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Mother's Name"));
        form.add(Box.createVerticalStrut(5));
        fMotherName = formField("Enter mother's full name (optional)");
        form.add(fMotherName);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Birth Date * (YYYY-MM-DD)"));
        form.add(Box.createVerticalStrut(5));
        fBirthDate = formField("1990-01-15");
        form.add(fBirthDate);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Gender *"));
        form.add(Box.createVerticalStrut(5));
        cGender = styledCombo("Male", "Female");
        form.add(cGender);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Marital Status"));
        form.add(Box.createVerticalStrut(5));
        cMaritalStatus = styledCombo("Single", "Married", "Divorced", "Widowed");
        form.add(cMaritalStatus);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Blood Group"));
        form.add(Box.createVerticalStrut(5));
        cBloodGroup = styledCombo("Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        form.add(cBloodGroup);

        // === CONTACT INFORMATION ===
        form.add(Box.createVerticalStrut(20));
        form.add(sectionHeader("📞 Contact Information"));

        form.add(formLabel("Phone Number *"));
        form.add(Box.createVerticalStrut(5));
        fPhone = formField("+251 9XX XXX XXX");
        form.add(fPhone);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Address *"));
        form.add(Box.createVerticalStrut(5));
        fAddress = formField("City / Sub-City");
        form.add(fAddress);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Address Detail"));
        form.add(Box.createVerticalStrut(5));
        fAddressDetail = formField("Woreda / House No. / Details (optional)");
        form.add(fAddressDetail);

        // === ADDITIONAL INFORMATION ===
        form.add(Box.createVerticalStrut(20));
        form.add(sectionHeader("📋 Additional Information"));

        form.add(formLabel("Birth Place"));
        form.add(Box.createVerticalStrut(5));
        fBirthPlace = formField("City/Town of birth (optional)");
        form.add(fBirthPlace);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Occupation"));
        form.add(Box.createVerticalStrut(5));
        fOccupation = formField("Your occupation (optional)");
        form.add(fOccupation);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Kebele *"));
        form.add(Box.createVerticalStrut(5));
        cKebele = styledCombo("KBL-01","KBL-02","KBL-03","KBL-04","KBL-05",
                "KBL-06","KBL-07","KBL-08","KBL-09","KBL-10");
        form.add(cKebele);

        // === ACCOUNT CREDENTIALS ===
        form.add(Box.createVerticalStrut(20));
        form.add(sectionHeader("🔐 Account Credentials"));

        form.add(formLabel("Username *"));
        form.add(Box.createVerticalStrut(5));
        fUsername = formField("Choose a username (letters, numbers, _)");
        form.add(fUsername);

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Password * (min 8 characters)"));
        form.add(Box.createVerticalStrut(5));
        fPassword = formPasswordField("Create a strong password");
        form.add(fPassword);
        
        // Password Strength Meter
        form.add(Box.createVerticalStrut(6));
        JPanel strengthMeter = createPasswordStrengthMeter();
        form.add(strengthMeter);
        
        // Add document listener to password field for real-time strength checking
        fPassword.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePasswordStrength(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePasswordStrength(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePasswordStrength(); }
        });

        form.add(Box.createVerticalStrut(14));
        form.add(formLabel("Confirm Password *"));
        form.add(Box.createVerticalStrut(5));
        fConfirm = formPasswordField("Re-enter your password");
        form.add(fConfirm);

        // Role notice box
        form.add(Box.createVerticalStrut(16));
        JPanel roleNotice = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorScheme.WARNING_LIGHT);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(ColorScheme.WARNING);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roleNotice.setOpaque(false);
        roleNotice.setBorder(new EmptyBorder(10,14,10,14));
        roleNotice.setMaximumSize(new Dimension(9999,70));
        JLabel roleInfo = new JLabel(
                "<html><b style='color:" + String.format("#%06X", ColorScheme.WARNING.getRGB() & 0xFFFFFF) + "'>⏳ Approval Required</b><br>"
                        +"<span style='color:" + String.format("#%06X", ColorScheme.TEXT_PRIMARY.getRGB() & 0xFFFFFF) + "'>"
                        +"Your registration will be reviewed by an administrator.<br>"
                        +"You can log in after approval is granted.</span></html>");
        roleInfo.setFont(F_SMALL);
        roleNotice.add(roleInfo);
        form.add(roleNotice);

        // Message label
        form.add(Box.createVerticalStrut(10));
        lblMsg = new JLabel(" ");
        lblMsg.setFont(F_SMALL);
        lblMsg.setForeground(ColorScheme.ERROR);
        lblMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblMsg);

        // Register button
        form.add(Box.createVerticalStrut(4));
        JButton btnReg = actionBtn("Submit Registration", ColorScheme.BTN_SUCCESS, ColorScheme.TEXT_ON_DARK);
        btnReg.addActionListener(e -> doRegister());
        form.add(btnReg);

        // Back button
        form.add(Box.createVerticalStrut(12));
        JButton btnBack = outlineBtn("Back to Login", ColorScheme.PRIMARY);
        btnBack.addActionListener(e -> {
            dispose();
            parent.setVisible(true);
        });
        form.add(btnBack);

        form.add(Box.createVerticalStrut(20));

        JScrollPane scroll = new JScrollPane(form,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll,  BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "<html><center>Federal Democratic Republic of Ethiopia<br>" +
                "Developer: Wondatir Fetene | v2.0 | © 2026</center></html>",
                SwingConstants.CENTER);
        footer.setFont(F_SMALL);
        footer.setForeground(ColorScheme.TEXT_MUTED);
        card.add(footer, BorderLayout.SOUTH);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1; gc.weighty = 1;
        outer.add(card, gc);
        return outer;
    }

    /**
     * Creates a section header label
     */
    private JLabel sectionHeader(String text) {
        JLabel header = new JLabel(text);
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setForeground(ColorScheme.PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        return header;
    }

    // ── Register logic with enhanced security ───────────────────────────────────────
    private void doRegister() {
        // Get all field values
        String firstName = fFirstName.getText().trim();
        String lastName = fLastName.getText().trim();
        String fatherName = fFatherName.getText().trim();
        String motherName = fMotherName.getText().trim();
        String birthDate = fBirthDate.getText().trim();
        String gender = (String) cGender.getSelectedItem();
        String maritalStatus = (String) cMaritalStatus.getSelectedItem();
        String bloodGroup = (String) cBloodGroup.getSelectedItem();
        String phone = fPhone.getText().trim();
        String address = fAddress.getText().trim();
        String addressDetail = fAddressDetail.getText().trim();
        String birthPlace = fBirthPlace.getText().trim();
        String occupation = fOccupation.getText().trim();
        String kebele = (String) cKebele.getSelectedItem();
        String username = fUsername.getText().trim();
        String password = new String(fPassword.getPassword());
        String confirm = new String(fConfirm.getPassword());

        // ═══════════════════════════════════════════════════════
        // ENHANCED INPUT VALIDATION & SQL INJECTION PREVENTION
        // ═══════════════════════════════════════════════════════
        
        // Validate required fields
        if (firstName.isEmpty() || lastName.isEmpty() || fatherName.isEmpty()) {
            showMsg("First Name, Last Name, and Father's Name are required!", ColorScheme.ERROR);
            return;
        }
        
        // Validate names (letters, spaces, hyphens, apostrophes only)
        if (!utils.ValidationUtils.isValidName(firstName)) {
            showMsg("First Name contains invalid characters!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isValidName(lastName)) {
            showMsg("Last Name contains invalid characters!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isValidName(fatherName)) {
            showMsg("Father's Name contains invalid characters!", ColorScheme.ERROR);
            return;
        }
        if (!motherName.isEmpty() && !utils.ValidationUtils.isValidName(motherName)) {
            showMsg("Mother's Name contains invalid characters!", ColorScheme.ERROR);
            return;
        }
        
        // Validate birth date
        if (birthDate.isEmpty() || !birthDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showMsg("Please enter birth date in format: YYYY-MM-DD", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isValidDate(birthDate)) {
            showMsg("Invalid birth date!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isNotFutureDate(birthDate)) {
            showMsg("Birth date cannot be in the future!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isReasonableAge(birthDate)) {
            showMsg("Birth date is not reasonable (must be 0-150 years old)!", ColorScheme.ERROR);
            return;
        }
        
        // Validate phone
        if (phone.isEmpty()) {
            showMsg("Phone number is required!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isValidPhone(phone)) {
            showMsg("Invalid phone format! Use: +251 9XX XXX XXX", ColorScheme.ERROR);
            return;
        }
        
        // Validate address
        if (address.isEmpty()) {
            showMsg("Address is required!", ColorScheme.ERROR);
            return;
        }
        
        // Validate username
        if (username.isEmpty() || username.length() < 3) {
            showMsg("Username must be at least 3 characters!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isValidUsername(username)) {
            showMsg("Username can only contain letters, numbers, and underscore (3-20 chars)!", ColorScheme.ERROR);
            return;
        }
        
        // Validate password strength
        if (password.length() < 8) {
            showMsg("Password must be at least 8 characters!", ColorScheme.ERROR);
            return;
        }
        if (!utils.ValidationUtils.isValidPassword(password)) {
            showMsg("Password must contain uppercase, lowercase, digit, and special character (@$!%*?&)!", ColorScheme.ERROR);
            return;
        }
        if (!password.equals(confirm)) {
            showMsg("Passwords do not match!", ColorScheme.ERROR);
            fConfirm.setText("");
            return;
        }
        
        // SQL Injection Prevention - Check all text inputs
        String[] inputs = {firstName, lastName, fatherName, motherName, phone, 
                          address, addressDetail, birthPlace, occupation, username};
        for (String input : inputs) {
            if (!input.isEmpty() && !utils.ValidationUtils.isSafeInput(input)) {
                showMsg("Invalid characters detected in input!", ColorScheme.ERROR);
                DatabaseManager.logActivity(0, username, "REGISTER_FAILED", 
                    "Potential SQL injection attempt");
                return;
            }
        }

        try {
            // Check if username exists
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement("SELECT id FROM users WHERE username=?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        showMsg("Username already taken! Try another.", ColorScheme.ERROR);
                        return;
                    }
                }
            }

            // Step 1: Create resident record with ALL information
            int residentId = -1;
            String generatedKebeleId = generateKebeleId(kebele);
            
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement(
                            "INSERT INTO residents (kebele_id, first_name, last_name, father_name, mother_name, " +
                            "date_of_birth, gender, marital_status, blood_group, phone_number, address, " +
                            "address_detail, birth_place, occupation, kebele_number, id_status, " +
                            "approval_status, created_by, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Active', 'Pending', ?, datetime('now'))",
                            Statement.RETURN_GENERATED_KEYS)) {
                
                ps.setString(1, generatedKebeleId);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setString(4, fatherName);
                ps.setString(5, motherName.isEmpty() ? null : motherName);
                ps.setString(6, birthDate);
                ps.setString(7, gender);
                ps.setString(8, maritalStatus);
                ps.setString(9, bloodGroup);
                ps.setString(10, phone);
                ps.setString(11, address);
                ps.setString(12, addressDetail.isEmpty() ? null : addressDetail);
                ps.setString(13, birthPlace.isEmpty() ? null : birthPlace);
                ps.setString(14, occupation.isEmpty() ? null : occupation);
                ps.setString(15, kebele);
                ps.setString(16, username); // Created by
                
                ps.executeUpdate();
                
                // Get generated resident ID
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        residentId = rs.getInt(1);
                    }
                }
            }

            // Step 2: Create user account linked to resident (with BCrypt hashed password)
            String fullName = firstName + " " + lastName;
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement(
                            "INSERT INTO users (full_name, username, password, role, kebele_id, " +
                            "status, approval_status, linked_resident_id) " +
                            "VALUES (?, ?, ?, 'Viewer', ?, 'Active', 'Pending', ?)")) {
                ps.setString(1, fullName);
                ps.setString(2, username);
                ps.setString(3, PasswordUtils.hashPassword(password)); // BCrypt hashing
                ps.setString(4, kebele);
                ps.setInt(5, residentId);
                ps.executeUpdate();
            }

            DatabaseManager.logActivity(0, username, "REGISTER",
                    "New resident registration: " + fullName + " (Kebele ID: " + generatedKebeleId + ")");

            showMsg("Registration submitted successfully!", ColorScheme.SUCCESS);

            Timer t = new Timer(1500, e -> {
                JOptionPane.showMessageDialog(this,
                        "<html><b>Registration Submitted Successfully!</b><br><br>"
                                +"<b>Personal Information:</b><br>"
                                +"Name: " + fullName + "<br>"
                                +"Father: " + fatherName + "<br>"
                                +"Birth Date: " + birthDate + "<br>"
                                +"Gender: " + gender + "<br><br>"
                                +"<b>Account Information:</b><br>"
                                +"Username: <b>" + username + "</b><br>"
                                +"Kebele ID: <b>" + generatedKebeleId + "</b><br><br>"
                                +"<b style='color:#D97706'>⏳ Status: Pending Admin Approval</b><br>"
                                +"You will be able to log in after an administrator approves your account."
                                +"</html>",
                        "Registration Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
                parent.setVisible(true);
            });
            t.setRepeats(false);
            t.start();

        } catch (Exception ex) {
            showMsg("Registration failed: " + ex.getMessage(), ColorScheme.ERROR);
            DatabaseManager.logActivity(0, username, "REGISTER_ERROR", 
                "Registration error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showMsg(String msg, Color color) {
        lblMsg.setText(msg);
        lblMsg.setForeground(color);
    }

    /**
     * Generates a unique Kebele ID for new resident
     */
    private String generateKebeleId(String kebeleCode) {
        try {
            // Get the last ID number for this kebele
            String sql = "SELECT kebele_id FROM residents WHERE kebele_id LIKE ? ORDER BY id DESC LIMIT 1";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, kebeleCode + "-%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String lastId = rs.getString("kebele_id");
                        // Extract number and increment
                        String[] parts = lastId.split("-");
                        if (parts.length == 2) {
                            int num = Integer.parseInt(parts[1]) + 1;
                            return kebeleCode + "-" + String.format("%06d", num);
                        }
                    }
                }
            }
            // First ID for this kebele
            return kebeleCode + "-000001";
        } catch (Exception e) {
            // Fallback to timestamp-based ID
            return kebeleCode + "-" + System.currentTimeMillis() % 1000000;
        }
    }

    // ── Form helpers ─────────────────────────────────────────
    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(ColorScheme.TEXT_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField formField(String ph) {
        JTextField tf = new JTextField();
        tf.setText(ph);
        tf.setForeground(new Color(0x9CA3AF));
        tf.setFont(F_INPUT);
        tf.setOpaque(true);
        tf.setBackground(new Color(0xF9FAFB));
        tf.setBorder(new CompoundBorder(
                new LoginFrame.RoundBorder(new Color(0xD1D5DB), 1, 10),
                new EmptyBorder(12, 14, 12, 14)));
        tf.setPreferredSize(new Dimension(0, 48));
        tf.setMaximumSize(new Dimension(9999, 48));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(ph)) {
                    tf.setText(""); tf.setForeground(new Color(0x111827)); }
                tf.setBorder(new CompoundBorder(
                        new LoginFrame.RoundBorder(ColorScheme.PRIMARY, 2, 10),
                        new EmptyBorder(12, 14, 12, 14)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) {
                    tf.setText(ph); tf.setForeground(new Color(0x9CA3AF)); }
                tf.setBorder(new CompoundBorder(
                        new LoginFrame.RoundBorder(new Color(0xD1D5DB),1,10),
                        new EmptyBorder(12,14,12,14)));
            }
        });
        return tf;
    }

    private JPasswordField formPasswordField(String ph) {
        JPasswordField pf = new JPasswordField();
        pf.setEchoChar((char)0);
        pf.setText(ph);
        pf.setForeground(new Color(0x9CA3AF));
        pf.setFont(F_INPUT);
        pf.setOpaque(true);
        pf.setBackground(new Color(0xF9FAFB));
        pf.setBorder(new CompoundBorder(
                new LoginFrame.RoundBorder(new Color(0xD1D5DB), 1, 10),
                new EmptyBorder(12, 14, 12, 14)));
        pf.setPreferredSize(new Dimension(0, 48));
        pf.setMaximumSize(new Dimension(9999, 48));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        pf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (new String(pf.getPassword()).equals(ph)) {
                    pf.setText(""); pf.setForeground(new Color(0x111827));
                    pf.setEchoChar('\u25CF');
                }
                pf.setBorder(new CompoundBorder(
                        new LoginFrame.RoundBorder(ColorScheme.PRIMARY, 2, 10),
                        new EmptyBorder(12, 14, 12, 14)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (new String(pf.getPassword()).isEmpty()) {
                    pf.setText(ph); pf.setForeground(new Color(0x9CA3AF));
                    pf.setEchoChar((char)0);
                }
                pf.setBorder(new CompoundBorder(
                        new LoginFrame.RoundBorder(new Color(0xD1D5DB),1,10),
                        new EmptyBorder(12,14,12,14)));
            }
        });
        return pf;
    }

    private JComboBox<String> styledCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_INPUT);
        cb.setBackground(new Color(0xF9FAFB));
        cb.setForeground(new Color(0x111827));
        cb.setBorder(new CompoundBorder(
                new LoginFrame.RoundBorder(new Color(0xD1D5DB), 1, 10),
                new EmptyBorder(8, 10, 8, 10)));
        cb.setPreferredSize(new Dimension(0, 48));
        cb.setMaximumSize(new Dimension(9999, 48));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        return cb;
    }

    private JButton actionBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Hover effect - slight darken
                Color c = getModel().isPressed()  ? darker(bg, 0.2f)  :
                        getModel().isRollover()  ? darker(bg, 0.1f): bg;
                g2.setPaint(new GradientPaint(0,0,c,0,getHeight(),darker(c, 0.05f)));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(F_BTN); b.setForeground(fg); b.setBackground(bg);
        b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(0, 50));
        b.setMaximumSize(new Dimension(9999, 50));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }
    
    // Helper method for darkening colors
    private Color darker(Color c, float factor) {
        return new Color(
            Math.max(0, (int)(c.getRed() * (1 - factor))),
            Math.max(0, (int)(c.getGreen() * (1 - factor))),
            Math.max(0, (int)(c.getBlue() * (1 - factor)))
        );
    }

    private JButton outlineBtn(String text, Color color) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0xEFF6FF));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                }
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,12,12);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setFont(F_BTN); b.setForeground(color);
        b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(0,50));
        b.setMaximumSize(new Dimension(9999,50));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }
    
    /**
     * Creates password strength meter component
     */
    private JPanel createPasswordStrengthMeter() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setMaximumSize(new Dimension(9999, 30));
        
        // Strength bar
        passwordStrengthBar = new JPanel() {
            private float strength = 0f;
            
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2.setColor(new Color(0xE5E7EB));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                
                // Strength bar
                int barWidth = (int)(getWidth() * strength);
                if (barWidth > 0) {
                    Color barColor;
                    if (strength < 0.33f) {
                        barColor = ColorScheme.ERROR; // Weak - Red
                    } else if (strength < 0.67f) {
                        barColor = ColorScheme.WARNING; // Medium - Yellow/Gold
                    } else {
                        barColor = ColorScheme.SUCCESS; // Strong - Green
                    }
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, barWidth, getHeight(), 6, 6);
                }
            }
            
            public void setStrength(float s) {
                this.strength = Math.max(0f, Math.min(1f, s));
                repaint();
            }
        };
        passwordStrengthBar.setPreferredSize(new Dimension(0, 8));
        passwordStrengthBar.setMaximumSize(new Dimension(9999, 8));
        passwordStrengthBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Strength label
        passwordStrengthLabel = new JLabel(" ");
        passwordStrengthLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        passwordStrengthLabel.setForeground(ColorScheme.TEXT_SECONDARY);
        passwordStrengthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        container.add(passwordStrengthBar);
        container.add(Box.createVerticalStrut(4));
        container.add(passwordStrengthLabel);
        
        return container;
    }
    
    /**
     * Updates password strength meter based on current password
     */
    private void updatePasswordStrength() {
        String password = new String(fPassword.getPassword());
        
        // Don't show strength for placeholder text
        if (password.equals("Create a strong password") || password.isEmpty()) {
            try {
                java.lang.reflect.Method method = passwordStrengthBar.getClass().getDeclaredMethod("setStrength", float.class);
                method.invoke(passwordStrengthBar, 0f);
            } catch (Exception e) {}
            passwordStrengthLabel.setText(" ");
            return;
        }
        
        // Calculate strength (0.0 to 1.0)
        float strength = calculatePasswordStrength(password);
        
        // Update bar
        try {
            java.lang.reflect.Method method = passwordStrengthBar.getClass().getDeclaredMethod("setStrength", float.class);
            method.invoke(passwordStrengthBar, strength);
        } catch (Exception e) {}
        
        // Update label
        if (strength < 0.33f) {
            passwordStrengthLabel.setText("Weak password");
            passwordStrengthLabel.setForeground(ColorScheme.ERROR);
        } else if (strength < 0.67f) {
            passwordStrengthLabel.setText("Medium password");
            passwordStrengthLabel.setForeground(ColorScheme.WARNING);
        } else {
            passwordStrengthLabel.setText("Strong password");
            passwordStrengthLabel.setForeground(ColorScheme.SUCCESS);
        }
    }
    
    /**
     * Calculates password strength (0.0 = weak, 1.0 = strong)
     */
    private float calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0f;
        
        float strength = 0f;
        
        // Length (max 0.3)
        if (password.length() >= 8) strength += 0.15f;
        if (password.length() >= 12) strength += 0.15f;
        
        // Has lowercase (0.15)
        if (password.matches(".*[a-z].*")) strength += 0.15f;
        
        // Has uppercase (0.15)
        if (password.matches(".*[A-Z].*")) strength += 0.15f;
        
        // Has digit (0.15)
        if (password.matches(".*\\d.*")) strength += 0.15f;
        
        // Has special character (0.15)
        if (password.matches(".*[@$!%*?&].*")) strength += 0.15f;
        
        // Variety bonus (0.1)
        int variety = 0;
        if (password.matches(".*[a-z].*")) variety++;
        if (password.matches(".*[A-Z].*")) variety++;
        if (password.matches(".*\\d.*")) variety++;
        if (password.matches(".*[@$!%*?&].*")) variety++;
        if (variety >= 3) strength += 0.1f;
        
        return Math.min(1.0f, strength);
    }
}