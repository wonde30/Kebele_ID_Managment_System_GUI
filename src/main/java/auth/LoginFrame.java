package auth;

import db.DatabaseManager;
import model.User;
import utils.PasswordUtils;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.sql.*;

public class LoginFrame extends JFrame {

    // ── Professional Color Palette (Standardized) ────────────
    private static final Color PRIMARY    = new Color(0x1B6CA8); // Professional blue
    private static final Color ACCENT     = new Color(0xF5A623); // Ethiopian gold
    private static final Color BG_DARK    = new Color(0x1A2035); // Background dark panel
    private static final Color SUCCESS    = new Color(0x27AE60); // Success green
    private static final Color ERROR      = new Color(0xE74C3C); // Error red
    private static final Color WHITE      = Color.WHITE;
    private static final Color W80        = new Color(255,255,255,200);
    private static final Color W40        = new Color(255,255,255,100);
    private static final Color W20        = new Color(255,255,255,50);

    // ── Standardized Typography ──────────────────────────────
    private static final Font F_HERO   = new Font("Segoe UI", Font.BOLD,   42);
    private static final Font F_TITLE  = new Font("Segoe UI", Font.BOLD,   28); // 28px
    private static final Font F_SUB    = new Font("Segoe UI", Font.PLAIN,  14); // 14px
    private static final Font F_LABEL  = new Font("Segoe UI", Font.PLAIN,  13); // 13px medium
    private static final Font F_INPUT  = new Font("Segoe UI", Font.PLAIN,  14); // 14px
    private static final Font F_SMALL  = new Font("Segoe UI", Font.PLAIN,  11);
    private static final Font F_BTN    = new Font("Segoe UI", Font.BOLD,   14);

    private JTextField     fUsername;
    private JPasswordField fPassword;
    private JCheckBox      chkShow;
    private JLabel         lblError;
    private JButton        btnLogin;
    private int            attempts = 0;
    private static final int MAX = 5;
    private long           lockoutUntil = 0;
    private static final long LOCKOUT_DURATION = 300_000; // 5 minutes in milliseconds

    // ── Animated background state ────────────────────────────
    private float animOffset = 0f;
    private Timer animTimer;

    public LoginFrame() {
        setTitle("Kebele ID Management System — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(false);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // full screen
        setMinimumSize(new Dimension(1100, 700));
        buildUI();
    }

    private void buildUI() {
        // ── Animated gradient background panel ──────────────
        JPanel bg = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Animated diagonal gradient with professional colors
                GradientPaint gp = new GradientPaint(
                        (float)(w * 0.3 + Math.sin(animOffset) * 80), 0, BG_DARK,
                        w, h, PRIMARY);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Decorative circles (depth effect)
                g2.setColor(W20);
                g2.fillOval((int)(w*0.05 + Math.cos(animOffset*0.7)*30),
                        (int)(h*0.1 + Math.sin(animOffset*0.5)*20),
                        260, 260);
                g2.fillOval((int)(w*0.75 + Math.sin(animOffset*0.4)*25),
                        (int)(h*0.6 + Math.cos(animOffset*0.6)*30),
                        200, 200);
                g2.fillOval((int)(w*0.5  + Math.cos(animOffset*0.3)*40),
                        (int)(h*0.05 + Math.sin(animOffset*0.8)*15),
                        120, 120);
                g2.setColor(new Color(255,255,255,25));
                g2.fillOval((int)(w*0.85 + Math.sin(animOffset*0.5)*20),
                        (int)(h*0.05),
                        350, 350);

                // Subtle grid lines
                g2.setColor(new Color(255,255,255,12));
                g2.setStroke(new BasicStroke(1));
                for (int x = 0; x < w; x += 60)
                    g2.drawLine(x, 0, x, h);
                for (int y = 0; y < h; y += 60)
                    g2.drawLine(0, y, w, y);
            }
        };
        bg.setOpaque(true);

        // Start animation timer
        animTimer = new Timer(50, e -> {
            animOffset += 0.03f;
            bg.repaint();
        });
        animTimer.start();

        // ── Split layout: LEFT info panel | RIGHT login card ─
        bg.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;

        // LEFT — service declaration panel
        gc.gridx = 0; gc.weightx = 0.55;
        bg.add(buildLeftPanel(), gc);

        // RIGHT — login form card
        gc.gridx = 1; gc.weightx = 0.45;
        bg.add(buildRightPanel(), gc);

        setContentPane(bg);
    }

    // ── LEFT PANEL — service declaration ────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                // semi-transparent dark overlay
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());
        p.setBorder(new EmptyBorder(60, 60, 60, 40));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // ── Logo Badge (KIS) ────────────────────────────────────
        JLabel badge = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Yellow circle background
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Dark text "KIS"
                g2.setColor(BG_DARK);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("KIS",
                        (getWidth()  - fm.stringWidth("KIS")) / 2,
                        (getHeight() + fm.getAscent()) / 2 - 4);
                g2.dispose();
            }
        };
        badge.setPreferredSize(new Dimension(72, 72));
        badge.setMaximumSize(new Dimension(72, 72));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── System name ──────────────────────────────────────
        JLabel hero = new JLabel(
                "<html>Kebele ID<br>Management<br>System</html>");
        hero.setFont(F_HERO);
        hero.setForeground(WHITE);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Gold divider ─────────────────────────────────────
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                        0, 0, ACCENT, getWidth(), 0,
                        new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 0));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(300, 3));
        divider.setMaximumSize(new Dimension(400, 3));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Tagline ──────────────────────────────────────────
        JLabel tagline = new JLabel(
                "Empowering Local Governance Through Digital Identity");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 15));
        tagline.setForeground(W80);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Service cards ────────────────────────────────────
        JPanel services = new JPanel(new GridLayout(3, 2, 12, 12));
        services.setOpaque(false);
        services.setAlignmentX(Component.LEFT_ALIGNMENT);
        services.setMaximumSize(new Dimension(560, 200));

        String[][] svcs = {
                {"ID Registration",  "Fast & secure resident ID issuance"},
                {"Record Management","Add, update & search resident data"},
                {"Role-Based Access","Granular permissions for every role"},
                {"Analytics Reports","Charts & insights across kebeles"},
                {"ID Card Export",   "Print or save professional ID cards"},
                {"Audit Logging",    "Every action tracked & logged"}
        };
        for (String[] s : svcs)
            services.add(serviceCard(s[0], s[1]));

        // ── Footer note ──────────────────────────────────────
        JLabel footer = new JLabel(
                "<html>Federal Democratic Republic of Ethiopia<br>" +
                "Developer: Wondatir Fetene | v2.0 | © 2026</html>");
        footer.setFont(F_SMALL);
        footer.setForeground(W40);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        inner.add(badge);
        inner.add(Box.createVerticalStrut(20));
        inner.add(hero);
        inner.add(Box.createVerticalStrut(18));
        inner.add(divider);
        inner.add(Box.createVerticalStrut(14));
        inner.add(tagline);
        inner.add(Box.createVerticalStrut(30));
        inner.add(services);
        inner.add(Box.createVerticalGlue());
        inner.add(footer);

        p.add(inner);
        return p;
    }

    private JPanel serviceCard(String title, String desc) {
        JPanel p = new JPanel() {
            private boolean hovered = false;
            
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Hover effect - subtle lift
                int offset = hovered ? -2 : 0;
                g2.translate(0, offset);
                
                g2.setColor(new Color(255,255,255,hovered ? 35 : 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255,255,255,hovered ? 70 : 60));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setLayout(new BorderLayout(0, 4));
        p.setBorder(new EmptyBorder(12, 14, 12, 14)); // Uniform padding

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
        t.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Consistent font size
        t.setForeground(ACCENT);

        JLabel d = new JLabel(
                "<html><body style='width:120px;'>" + desc + "</body></html>");
        d.setFont(F_SMALL);
        d.setForeground(W80);

        p.add(t, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);
        return p;
    }

    // ── RIGHT PANEL — login form card ────────────────────────
    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(40, 20, 40, 60));

        // Frosted glass card
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // White frosted background
                g2.setColor(new Color(255,255,255,230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                // Subtle top border highlight
                g2.setColor(new Color(255,255,255,180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(420, 600));
        card.setBorder(new EmptyBorder(40, 40, 36, 40));

        // ── Card header ──────────────────────────────────────
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(F_TITLE); // 28px bold
        welcome.setForeground(BG_DARK);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to your account");
        sub.setFont(F_SUB); // 14px gray
        sub.setForeground(new Color(0x6B7280));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Colored accent line
        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                        0, 0, PRIMARY, getWidth(), 0, ACCENT);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
        };
        accent.setOpaque(false);
        accent.setPreferredSize(new Dimension(60, 4));
        accent.setMaximumSize(new Dimension(60, 4));
        accent.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(welcome);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        header.add(Box.createVerticalStrut(12));
        header.add(accent);

        // ── Form ─────────────────────────────────────────────
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // Username
        form.add(Box.createVerticalStrut(28));
        form.add(formLabel("Username"));
        form.add(Box.createVerticalStrut(6));
        fUsername = formField("Enter your username");
        form.add(fUsername);

        // Password
        form.add(Box.createVerticalStrut(18));
        form.add(formLabel("Password"));
        form.add(Box.createVerticalStrut(6));
        fPassword = formPasswordField("Enter your password");
        form.add(fPassword);

        // Show password
        form.add(Box.createVerticalStrut(8));
        chkShow = new JCheckBox("Show password");
        chkShow.setFont(F_SMALL);
        chkShow.setForeground(new Color(0x6B7280));
        chkShow.setOpaque(false);
        chkShow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chkShow.addActionListener(e ->
                fPassword.setEchoChar(
                        chkShow.isSelected() ? (char)0 : '\u25CF'));
        form.add(chkShow);

        // Error label
        form.add(Box.createVerticalStrut(10));
        lblError = new JLabel(" ");
        lblError.setFont(F_SMALL);
        lblError.setForeground(ERROR); // Use standardized error color
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);

        // Login button with loading state
        form.add(Box.createVerticalStrut(6));
        btnLogin = actionBtn("Sign In", PRIMARY, WHITE);
        btnLogin.addActionListener(e -> doLogin());
        form.add(btnLogin);

        // Forgot Password link
        form.add(Box.createVerticalStrut(10));
        JLabel forgotPassword = new JLabel("<html><u>Forgot Password?</u></html>");
        forgotPassword.setFont(F_SMALL);
        forgotPassword.setForeground(PRIMARY);
        forgotPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        forgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (animTimer != null) animTimer.stop();
                setVisible(false);
                auth.PasswordResetDialog.showDialog(LoginFrame.this);
                setVisible(true);
                if (animTimer != null) animTimer.start();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                forgotPassword.setForeground(PRIMARY.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                forgotPassword.setForeground(PRIMARY);
            }
        });
        form.add(forgotPassword);

        // Divider
        form.add(Box.createVerticalStrut(10));
        JPanel divRow = new JPanel(new BorderLayout(10, 0));
        divRow.setOpaque(false);
        divRow.setMaximumSize(new Dimension(9999, 20));
        JSeparator s1 = new JSeparator();
        s1.setForeground(new Color(0xE5E7EB));
        JSeparator s2 = new JSeparator();
        s2.setForeground(new Color(0xE5E7EB));
        JLabel orLbl = new JLabel("OR", SwingConstants.CENTER);
        orLbl.setFont(F_SMALL);
        orLbl.setForeground(new Color(0x9CA3AF));
        divRow.add(s1, BorderLayout.WEST);
        divRow.add(orLbl, BorderLayout.CENTER);
        divRow.add(s2, BorderLayout.EAST);
        form.add(divRow);

        // Register button
        form.add(Box.createVerticalStrut(14));
        JButton btnReg = outlineBtn("Create New Account");
        btnReg.addActionListener(e -> {
            if (animTimer != null) animTimer.stop();
            setVisible(false);
            new RegisterFrame(this).setVisible(true);
        });
        form.add(btnReg);

        // REMOVED: Security notice box with default credentials (security vulnerability)
        // Contact administrator for credentials instead

        card.add(header, BorderLayout.NORTH);
        card.add(form,   BorderLayout.CENTER);

        // Footer with complete information
        JLabel footer = new JLabel(
                "<html><center>Federal Democratic Republic of Ethiopia<br>" +
                "Developer: Wondatir Fetene | v2.0 | © 2026</center></html>",
                SwingConstants.CENTER);
        footer.setFont(F_SMALL);
        footer.setForeground(new Color(0x9CA3AF));
        card.add(footer, BorderLayout.SOUTH);

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1; gc.weighty = 1;
        outer.add(card, gc);
        return outer;
    }

    // ── Login logic with enhanced security ──────────────────────────────────────
    private void doLogin() {
        // Check if account is locked out
        if (lockoutUntil > System.currentTimeMillis()) {
            long remainingSeconds = (lockoutUntil - System.currentTimeMillis()) / 1000;
            lblError.setText("Account locked. Try again in " + remainingSeconds + " seconds.");
            shake(btnLogin);
            return;
        }
        
        if (attempts >= MAX) {
            lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION;
            lblError.setText("Too many attempts. Locked for 5 minutes.");
            btnLogin.setEnabled(false);
            // Re-enable button after lockout
            Timer unlockTimer = new Timer((int)LOCKOUT_DURATION, e -> {
                btnLogin.setEnabled(true);
                attempts = 0;
                lblError.setText(" ");
            });
            unlockTimer.setRepeats(false);
            unlockTimer.start();
            return;
        }
        
        String username = fUsername.getText().trim();
        String password = new String(fPassword.getPassword());

        // Input validation
        if (username.isEmpty() || password.isEmpty()
                || "Enter your username".equals(username)
                || "Enter your password".equals(password)) {
            lblError.setText("Please enter both username and password.");
            shake(btnLogin); return;
        }

        // Validate username format (prevent SQL injection)
        if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
            lblError.setText("Invalid username format.");
            shake(btnLogin);
            attempts++;
            return;
        }

        // Check for SQL injection patterns
        if (!utils.ValidationUtils.isSafeInput(username) || !utils.ValidationUtils.isSafeInput(password)) {
            lblError.setText("Invalid characters detected.");
            shake(btnLogin);
            attempts++;
            DatabaseManager.logActivity(0, username, "LOGIN_FAILED", 
                "Potential SQL injection attempt from username: " + username);
            return;
        }

        try {
            String sql = "SELECT * FROM users WHERE username=? AND status='Active'";
            try (PreparedStatement ps =
                         DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        
                        // Verify password using BCrypt
                        if (PasswordUtils.verifyPassword(password, storedPassword)) {
                            // Check if user is approved
                            String approvalStatus = rs.getString("approval_status");
                            if ("Pending".equals(approvalStatus)) {
                                lblError.setText("Account pending approval. Contact admin.");
                                shake(btnLogin);
                                return;
                            } else if ("Rejected".equals(approvalStatus)) {
                                lblError.setText("Account rejected. Contact admin.");
                                shake(btnLogin);
                                return;
                            }
                            
                            User user = new User();
                            user.setId(rs.getInt("id"));
                            user.setFullName(rs.getString("full_name"));
                            user.setUsername(rs.getString("username"));
                            user.setRole(rs.getString("role"));
                            user.setKebeleId(rs.getString("kebele_id"));
                            user.setStatus(rs.getString("status"));
                            
                            // Check if password needs upgrade from legacy SHA-256
                            if (PasswordUtils.needsUpgrade(storedPassword)) {
                                String newHash = PasswordUtils.hashPassword(password);
                                try (PreparedStatement updatePs = DatabaseManager.getConnection()
                                        .prepareStatement("UPDATE users SET password=? WHERE id=?")) {
                                    updatePs.setString(1, newHash);
                                    updatePs.setInt(2, user.getId());
                                    updatePs.executeUpdate();
                                }
                                DatabaseManager.logActivity(user.getId(), user.getUsername(),
                                        "PASSWORD_UPGRADE", "Password upgraded from SHA-256 to BCrypt");
                            }
                            
                            DatabaseManager.logActivity(
                                    user.getId(), user.getUsername(),
                                    "LOGIN", "Login successful");
                            if (animTimer != null) animTimer.stop();
                            dispose();
                            SwingUtilities.invokeLater(() ->
                                    new ui.KebeleIDSystem(user).setVisible(true));
                        } else {
                            attempts++;
                            lblError.setText("Wrong password. "
                                    + (MAX - attempts) + " attempt(s) left.");
                            shake(btnLogin);
                            fPassword.setText("");
                            DatabaseManager.logActivity(0, username, "LOGIN_FAILED", 
                                "Failed password attempt");
                        }
                    } else {
                        attempts++;
                        lblError.setText("User not found or account inactive.");
                        shake(btnLogin);
                        DatabaseManager.logActivity(0, username, "LOGIN_FAILED", 
                            "User not found or inactive");
                    }
                }
            }
        } catch (SQLException e) {
            lblError.setText("Database error. Please try again.");
            DatabaseManager.logActivity(0, username, "LOGIN_ERROR", 
                "Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void shake(Component c) {
        Point o = c.getLocation();
        Timer t = new Timer(30, null);
        int[] n = {0}; int[] d = {1};
        t.addActionListener(e -> {
            c.setLocation(o.x + 6 * d[0], o.y);
            d[0] *= -1;
            if (++n[0] >= 10) { c.setLocation(o); t.stop(); }
        });
        t.start();
    }

    // ── Form helpers ─────────────────────────────────────────
    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(new Color(0x374151));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField formField(String ph) {
        JTextField tf = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleFormField(tf, ph);
        return tf;
    }

    private JPasswordField formPasswordField(String ph) {
        JPasswordField pf = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pf.setEchoChar((char)0);
        pf.setText(ph);
        pf.setForeground(new Color(0x9CA3AF));
        pf.setFont(F_INPUT);
        pf.setOpaque(false);
        pf.setBackground(new Color(0xF9FAFB));
        pf.setBorder(new CompoundBorder(
                new RoundBorder(new Color(0xD1D5DB), 1, 10),
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
                // Blue border glow on focus
                pf.setBorder(new CompoundBorder(
                        new RoundBorder(PRIMARY, 2, 10),
                        new EmptyBorder(12, 14, 12, 14)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (new String(pf.getPassword()).isEmpty()) {
                    pf.setText(ph); pf.setForeground(new Color(0x9CA3AF));
                    pf.setEchoChar((char)0);
                }
                pf.setBorder(new CompoundBorder(
                        new RoundBorder(new Color(0xD1D5DB), 1, 10),
                        new EmptyBorder(12, 14, 12, 14)));
            }
        });
        return pf;
    }

    private void styleFormField(JTextField tf, String ph) {
        tf.setText(ph);
        tf.setForeground(new Color(0x9CA3AF));
        tf.setFont(F_INPUT);
        tf.setOpaque(false);
        tf.setBackground(new Color(0xF9FAFB));
        tf.setBorder(new CompoundBorder(
                new RoundBorder(new Color(0xD1D5DB), 1, 10),
                new EmptyBorder(12, 14, 12, 14)));
        tf.setPreferredSize(new Dimension(0, 48));
        tf.setMaximumSize(new Dimension(9999, 48));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(ph)) {
                    tf.setText(""); tf.setForeground(new Color(0x111827)); }
                // Blue border glow on focus
                tf.setBorder(new CompoundBorder(
                        new RoundBorder(PRIMARY, 2, 10),
                        new EmptyBorder(12, 14, 12, 14)));
            }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) {
                    tf.setText(ph); tf.setForeground(new Color(0x9CA3AF)); }
                tf.setBorder(new CompoundBorder(
                        new RoundBorder(new Color(0xD1D5DB), 1, 10),
                        new EmptyBorder(12, 14, 12, 14)));
            }
        });
    }

    private JButton actionBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Hover effect - slight darken
                Color c = getModel().isPressed()  ? darker(bg, 0.2f)   :
                        getModel().isRollover()  ? darker(bg, 0.1f) : bg;
                GradientPaint gp = new GradientPaint(
                        0, 0, c, 0, getHeight(), darker(c, 0.05f));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
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

    private JButton outlineBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Hover effect
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0xEFF6FF));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_BTN); b.setForeground(PRIMARY); b.setBackground(WHITE);
        b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(0, 50));
        b.setMaximumSize(new Dimension(9999, 50));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    // ── Custom round border helper ────────────────────────────
    static class RoundBorder extends AbstractBorder {
        private final Color color; private final int thickness, radius;
        RoundBorder(Color c, int t, int r) { color=c; thickness=t; radius=r; }
        @Override public void paintBorder(Component c, Graphics g,
                                          int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
            g2.dispose();
        }
    }
}