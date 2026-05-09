package ui;

import db.DatabaseManager;
import model.User;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class KebeleIDSystem extends JFrame {

    private final User currentUser;

    // ── Professional Color Scheme ────────────────────────────────────────
    // Using ColorScheme class for consistent professional colors

    // ── Standardized Typography (Professional) ──────────────────
    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD,  28); // 28px
    private static final Font F_LABEL = new Font("Segoe UI", Font.PLAIN, 13); // 13px
    private static final Font F_BOLD  = new Font("Segoe UI", Font.BOLD,  13); // 13px
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11); // 11px
    private static final Font F_BTN   = new Font("Segoe UI", Font.BOLD,  14); // 14px
    private static final Font F_INPUT = new Font("Segoe UI", Font.PLAIN, 14); // 14px

    // ── Resident form fields ─────────────────────────────────
    private JTextField fKebeleId, fFirst, fLast, fFather, fMother,
            fBirth, fPhone, fAddress, fAddressDetail, fBirthPlace,
            fOccupation, fKebeleNumber, fIssuedDate, fSearch;
    private JComboBox<String> cGender, cMaritalStatus, cBloodGroup, cStatus;
    private JLabel    lblPhoto;
    private String    photoPath = "";

    // ── Tables ───────────────────────────────────────────────
    private JTable             table,     userTable;
    private DefaultTableModel  model,     userModel;

    // ── Stat labels (header cards) ───────────────────────────
    private JLabel sTotal, sActive, sMale, sFemale;

    // ── Toolbar / CRUD buttons ───────────────────────────────
    private JButton btnSave, btnUpdate, btnDelete,
            btnClear, btnSearch, btnRefresh,
            btnPrint, btnPhoto,  btnIDCard;

    private int selectedId = -1;

    private ReportPanel reportPanel;
    
    // ── Profile dropdown (for refreshing photo) ──────────────
    private ProfileDropdown profileDropdown;

    // ╔══════════════════════════════════════════════════════════╗
    // ║  IMPROVED MARQUEE  (professional scrolling ticker)      ║
    // ║                                                          ║
    // ║  Features:                                               ║
    // ║  • Smooth gradient background                            ║
    // ║  • Icons and emojis for visual appeal                    ║
    // ║  • Text shadow for depth                                 ║
    // ║  • Seamless looping animation                            ║
    // ║  • Configurable speed and colors                         ║
    // ║  • Auto-updates every 30 seconds                         ║
    // ╚══════════════════════════════════════════════════════════╝
    private MarqueePanel marqueePanel;

    /** Updates the marquee with current statistics. */
    private void refreshMarqueeText() {
        int total    = count("1=1");
        int active   = count("id_status='Active'");
        int inactive = count("id_status='Inactive'");
        int male     = count("gender='Male'");
        int female   = count("gender='Female'");

        if (marqueePanel != null) {
            marqueePanel.setText(
                String.valueOf(total),
                String.valueOf(active),
                String.valueOf(inactive),
                String.valueOf(male),
                String.valueOf(female),
                currentUser.getFullName(),
                currentUser.getRole(),
                currentUser.getKebeleId()
            );
        }
    }

    /** Starts the marquee animation. */
    private void startMarquee() {
        if (marqueePanel != null) {
            marqueePanel.start();
        }
    }

    // ════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════
    public KebeleIDSystem(User user) {
        this.currentUser = user;

        setTitle("Kebele ID System | "
                + user.getFullName() + " [" + user.getRole() + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1220, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setBackground(ColorScheme.BG_MAIN);

        // stop marquee + log logout when window is closed
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (marqueePanel != null) marqueePanel.stop();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "LOGOUT",
                        "User closed the application");
                DatabaseManager.closeConnection();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ColorScheme.BG_MAIN);
        root.add(buildHeader(),     BorderLayout.NORTH);
        root.add(buildTabbedPane(), BorderLayout.CENTER);
        root.add(buildStatusBar(),  BorderLayout.SOUTH);
        setContentPane(root);

        applyRolePermissions();

        if (isViewer()) loadViewerOwnRecord();
        else            { refreshTable(); refreshStats(); }

        // ── start marquee AFTER data is loaded ──────────────
        refreshMarqueeText();
        startMarquee();
        // refresh marquee stats every 30 seconds
        new Timer(30_000, e -> refreshMarqueeText()).start();
    }

    private boolean isViewer() {
        return "Viewer".equals(currentUser.getRole());
    }

    // ════════════════════════════════════════════════════════
    //  TABBED PANE
    // ════════════════════════════════════════════════════════
    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(F_BOLD);
        tabs.setBackground(ColorScheme.BG_MAIN);

        // Tab 1 — Residents
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildFormPanel(), buildTablePanel());
        split.setDividerLocation(395);
        split.setDividerSize(6);
        split.setBackground(ColorScheme.BG_MAIN);
        split.setBorder(new EmptyBorder(10, 10, 10, 10));
        tabs.addTab("  Residents  ", split);

        // Tab 2 — Reports (non-viewers only)
        if (!isViewer()) {
            reportPanel = new ReportPanel();
            tabs.addTab("  Reports & Charts  ", reportPanel);
            tabs.addChangeListener(e -> {
                if (tabs.getSelectedIndex() == 1 && reportPanel != null)
                    reportPanel.refresh();
            });
        }

        // Tab 3 — User Management (admin / supervisor)
        if (currentUser.canManageUsers())
            tabs.addTab("  User Management  ", buildUserManagementPanel());

        // Tab 4 — Approval Management (admin only)
        if (currentUser.getRole().equals("Admin"))
            tabs.addTab("  📋 Pending Approvals  ", buildApprovalManagementPanel());

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 4 — Feedback tab (JTextArea word-wrap) ║
        // ╚══════════════════════════════════════════════════╝
        tabs.addTab("  Feedback  ", buildFeedbackPanel());

        return tabs;
    }

    // ════════════════════════════════════════════════════════
    //  FEEDBACK PANEL - Role-based functionality
    //
    //  Admin/Supervisor: View all feedback in table + submit new
    //  Other roles: Submit feedback only
    // ════════════════════════════════════════════════════════
    private JPanel buildFeedbackPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ColorScheme.BG_MAIN);
        panel.setBorder(new EmptyBorder(24, 34, 24, 34));

        // Check if user can manage feedback (Admin/Supervisor)
        boolean canManageFeedback = currentUser.getRole().equals("Admin") || 
                                   currentUser.getRole().equals("Supervisor");

        if (canManageFeedback) {
            // Create tabbed interface for Admin/Supervisor
            JTabbedPane feedbackTabs = new JTabbedPane();
            feedbackTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
            feedbackTabs.setBackground(ColorScheme.BG_MAIN);
            
            // Tab 1: View All Feedback
            feedbackTabs.addTab("  📋 Manage Feedback  ", buildFeedbackManagementPanel());
            
            // Tab 2: Submit New Feedback
            feedbackTabs.addTab("  ✍️ Submit Feedback  ", buildFeedbackSubmissionPanel());
            
            panel.add(feedbackTabs, BorderLayout.CENTER);
        } else {
            // Regular users only see submission form
            panel.add(buildFeedbackSubmissionPanel(), BorderLayout.CENTER);
        }

        return panel;
    }

    // ════════════════════════════════════════════════════════
    //  FEEDBACK MANAGEMENT PANEL (Admin/Supervisor only)
    //  Enhanced with Phase 1 Features:
    //  - Priority Level (Low, Medium, High, Critical)
    //  - Status Tracking (New, In Progress, Resolved, Closed)
    //  - Search & Filter
    //  - Statistics Dashboard
    // ════════════════════════════════════════════════════════
    private JPanel buildFeedbackManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ColorScheme.BG_MAIN);

        // ── Statistics Dashboard ──────────────────────────────────
        JPanel statsPanel = buildFeedbackStatsPanel();
        panel.add(statsPanel, BorderLayout.NORTH);

        // ── Search and Filter Panel ──────────────────────────────
        JPanel searchFilterPanel = buildSearchFilterPanel();
        panel.add(searchFilterPanel, BorderLayout.CENTER);

        return panel;
    }

    // ════════════════════════════════════════════════════════
    //  FEEDBACK STATISTICS DASHBOARD
    // ════════════════════════════════════════════════════════
    private JPanel buildFeedbackStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Title
        JLabel title = new JLabel("📊 Feedback Statistics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(ColorScheme.PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        // Stats cards
        JPanel cardsPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        cardsPanel.setOpaque(false);

        feedbackStatTotal = createStatCard("Total", "0", ColorScheme.INFO);
        feedbackStatNew = createStatCard("New", "0", ColorScheme.WARNING);
        feedbackStatInProgress = createStatCard("In Progress", "0", ColorScheme.ROLE_DATA_ENCODER);
        feedbackStatResolved = createStatCard("Resolved", "0", ColorScheme.SUCCESS);
        feedbackStatCritical = createStatCard("Critical", "0", ColorScheme.ERROR);

        cardsPanel.add(feedbackStatTotal);
        cardsPanel.add(feedbackStatNew);
        cardsPanel.add(feedbackStatInProgress);
        cardsPanel.add(feedbackStatResolved);
        cardsPanel.add(feedbackStatCritical);

        panel.add(cardsPanel, BorderLayout.CENTER);

        return panel;
    }

    // Feedback stat labels
    private JLabel feedbackStatTotal, feedbackStatNew, feedbackStatInProgress, 
                   feedbackStatResolved, feedbackStatCritical;

    // ════════════════════════════════════════════════════════
    //  SEARCH AND FILTER PANEL
    // ════════════════════════════════════════════════════════
    private JPanel buildSearchFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // ── Filter controls ──────────────────────────────────
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setFont(F_BOLD);
        searchLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        feedbackSearchField = new JTextField(20);
        feedbackSearchField.setFont(F_LABEL);
        feedbackSearchField.setPreferredSize(new Dimension(200, 32));
        feedbackSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterFeedbackTable(); }
            public void removeUpdate(DocumentEvent e) { filterFeedbackTable(); }
            public void changedUpdate(DocumentEvent e) { filterFeedbackTable(); }
        });

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(F_BOLD);
        statusLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        feedbackStatusFilter = new JComboBox<>(new String[]{
            "All Status", "New", "In Progress", "Resolved", "Closed"
        });
        feedbackStatusFilter.setFont(F_LABEL);
        feedbackStatusFilter.addActionListener(e -> filterFeedbackTable());

        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(F_BOLD);
        priorityLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        feedbackPriorityFilter = new JComboBox<>(new String[]{
            "All Priority", "Low", "Medium", "High", "Critical"
        });
        feedbackPriorityFilter.setFont(F_LABEL);
        feedbackPriorityFilter.addActionListener(e -> filterFeedbackTable());

        JButton btnRefreshFeedback = btn("🔄 Refresh", ColorScheme.BTN_SECONDARY, Color.WHITE);
        btnRefreshFeedback.addActionListener(e -> refreshFeedbackTable());

        filterPanel.add(searchLabel);
        filterPanel.add(feedbackSearchField);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(statusLabel);
        filterPanel.add(feedbackStatusFilter);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(priorityLabel);
        filterPanel.add(feedbackPriorityFilter);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(btnRefreshFeedback);

        panel.add(filterPanel, BorderLayout.NORTH);

        // ── Feedback table with Priority and Status ──────────
        String[] columns = {"ID", "User", "Role", "Category", "Priority", "Status", "Feedback", "Date", "Actions"};
        feedbackModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        feedbackTable = new JTable(feedbackModel);
        feedbackTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        feedbackTable.setRowHeight(40);
        feedbackTable.setGridColor(ColorScheme.BORDER_LIGHT);
        feedbackTable.setSelectionBackground(ColorScheme.SELECTION_BG);
        feedbackTable.setSelectionForeground(ColorScheme.TEXT_PRIMARY);
        feedbackTable.setBackground(ColorScheme.BG_CARD);
        feedbackTable.setForeground(ColorScheme.TEXT_PRIMARY);

        // Set column widths
        feedbackTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        feedbackTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // User
        feedbackTable.getColumnModel().getColumn(2).setPreferredWidth(80);   // Role
        feedbackTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Category
        feedbackTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Priority
        feedbackTable.getColumnModel().getColumn(5).setPreferredWidth(90);   // Status
        feedbackTable.getColumnModel().getColumn(6).setPreferredWidth(250);  // Feedback
        feedbackTable.getColumnModel().getColumn(7).setPreferredWidth(120);  // Date
        feedbackTable.getColumnModel().getColumn(8).setPreferredWidth(150);  // Actions

        // Custom renderer for Priority column (color-coded badges)
        feedbackTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "Medium");
                label.setOpaque(true);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));

                String priority = value != null ? value.toString() : "Medium";
                switch (priority) {
                    case "Critical":
                        label.setBackground(ColorScheme.ERROR);
                        label.setForeground(Color.WHITE);
                        break;
                    case "High":
                        label.setBackground(ColorScheme.WARNING);
                        label.setForeground(Color.WHITE);
                        break;
                    case "Medium":
                        label.setBackground(ColorScheme.INFO);
                        label.setForeground(Color.WHITE);
                        break;
                    case "Low":
                        label.setBackground(ColorScheme.TEXT_MUTED);
                        label.setForeground(Color.WHITE);
                        break;
                }

                if (isSelected) {
                    label.setBackground(ColorScheme.darker(label.getBackground(), 0.2f));
                }

                return label;
            }
        });

        // Custom renderer for Status column (color-coded badges)
        feedbackTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "New");
                label.setOpaque(true);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));

                String status = value != null ? value.toString() : "New";
                switch (status) {
                    case "New":
                        label.setBackground(ColorScheme.WARNING_LIGHT);
                        label.setForeground(ColorScheme.WARNING);
                        break;
                    case "In Progress":
                        label.setBackground(ColorScheme.INFO_LIGHT);
                        label.setForeground(ColorScheme.INFO);
                        break;
                    case "Resolved":
                        label.setBackground(ColorScheme.SUCCESS_LIGHT);
                        label.setForeground(ColorScheme.SUCCESS);
                        break;
                    case "Closed":
                        label.setBackground(ColorScheme.BG_SIDEBAR);
                        label.setForeground(ColorScheme.TEXT_MUTED);
                        break;
                }

                if (isSelected) {
                    label.setBackground(ColorScheme.SELECTION_BG);
                }

                return label;
            }
        });

        // Custom renderer for feedback column (wrap text)
        feedbackTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String text = value.toString();
                    if (text.length() > 40) {
                        setText("<html>" + text.substring(0, 37) + "...</html>");
                        setToolTipText("<html>" + text.replaceAll("(.{60})", "$1<br>") + "</html>");
                    } else {
                        setText(text);
                        setToolTipText(text);
                    }
                }

                if (!isSelected) {
                    setBackground(row % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR);
                }

                return comp;
            }
        });

        // Table header styling
        JTableHeader header = feedbackTable.getTableHeader();
        header.setBackground(ColorScheme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);

        // Add mouse listener for row actions
        feedbackTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = feedbackTable.getSelectedRow();
                    if (row >= 0) {
                        viewFeedbackDetails(row);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(feedbackTable);
        scrollPane.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1));
        scrollPane.getViewport().setBackground(ColorScheme.BG_CARD);

        // Action buttons panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionPanel.setOpaque(false);

        JButton btnView = btn("👁️ View Details", ColorScheme.BTN_PRIMARY, Color.WHITE);
        btnView.addActionListener(e -> {
            int row = feedbackTable.getSelectedRow();
            if (row >= 0) viewFeedbackDetails(row);
            else warn("Please select a feedback entry to view.");
        });

        JButton btnUpdateStatus = btn("📝 Update Status", ColorScheme.BTN_SUCCESS, Color.WHITE);
        btnUpdateStatus.addActionListener(e -> {
            int row = feedbackTable.getSelectedRow();
            if (row >= 0) updateFeedbackStatus(row);
            else warn("Please select a feedback entry to update.");
        });

        JButton btnDelete = btn("🗑️ Delete", ColorScheme.BTN_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> {
            int row = feedbackTable.getSelectedRow();
            if (row >= 0) deleteFeedback(row);
            else warn("Please select a feedback entry to delete.");
        });

        actionPanel.add(btnView);
        actionPanel.add(btnUpdateStatus);
        actionPanel.add(btnDelete);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setOpaque(false);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(actionPanel, BorderLayout.SOUTH);

        panel.add(tablePanel, BorderLayout.CENTER);

        // Load initial data
        refreshFeedbackTable();

        return panel;
    }

    // Filter components
    private JTextField feedbackSearchField;
    private JComboBox<String> feedbackStatusFilter, feedbackPriorityFilter;

    // Table references for feedback management
    private JTable feedbackTable;
    private DefaultTableModel feedbackModel;

    // ════════════════════════════════════════════════════════
    //  FEEDBACK SUBMISSION PANEL (All users)
    //  Enhanced with Priority selection
    // ════════════════════════════════════════════════════════
    private JPanel buildFeedbackSubmissionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ColorScheme.BG_MAIN);

        // ── Title block ──────────────────────────────────────
        JLabel title = new JLabel("Submit Feedback");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ColorScheme.PRIMARY);

        JLabel sub = new JLabel(
                "Share your comments, suggestions, or issues about the system.");
        sub.setFont(F_SMALL);
        sub.setForeground(ColorScheme.TEXT_MUTED);

        JPanel titleArea = new JPanel(new GridLayout(2, 1, 0, 4));
        titleArea.setOpaque(false);
        titleArea.add(title);
        titleArea.add(sub);
        panel.add(titleArea, BorderLayout.NORTH);

        // ── Card ─────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(ColorScheme.BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(24, 28, 24, 28)));

        // ── Your Name — NON-EDITABLE ────────────
        JLabel nameLabel = new JLabel(
                "Your Name  (auto-filled — read only):");
        nameLabel.setFont(F_BOLD);
        nameLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        JTextField nameField = new JTextField(
                currentUser.getFullName()
                        + "   [" + currentUser.getRole() + "]");
        nameField.setEditable(false);
        nameField.setFont(F_LABEL);
        nameField.setForeground(ColorScheme.TEXT_MUTED);
        nameField.setBackground(ColorScheme.BG_SIDEBAR);
        nameField.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        nameField.setToolTipText(
                "Read-only — auto-filled from your login session");

        // ── Category combo ───────────────────────────────────
        JLabel catLabel = new JLabel("Category:");
        catLabel.setFont(F_BOLD);
        catLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        JComboBox<String> catCombo = new JComboBox<>(new String[]{
                "General Comment", "Bug Report",
                "Feature Request", "Performance Issue",
                "UI / Design",     "Other"});
        catCombo.setFont(F_LABEL);
        catCombo.setBackground(ColorScheme.BG_CARD);
        catCombo.setPreferredSize(new Dimension(0, 38));

        // ── Priority combo (NEW) ─────────────────────────────
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(F_BOLD);
        priorityLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{
                "Low", "Medium", "High", "Critical"});
        priorityCombo.setFont(F_LABEL);
        priorityCombo.setBackground(ColorScheme.BG_CARD);
        priorityCombo.setPreferredSize(new Dimension(0, 38));
        priorityCombo.setSelectedIndex(1); // Default to Medium

        // ── Feedback JTextArea ───────────────────────────────
        JLabel fbLabel = new JLabel(
                "Your Feedback  (wraps automatically at word boundaries):");
        fbLabel.setFont(F_BOLD);
        fbLabel.setForeground(ColorScheme.TEXT_PRIMARY);

        JTextArea feedbackArea = new JTextArea(8, 40);
        feedbackArea.setFont(F_LABEL);
        feedbackArea.setForeground(ColorScheme.TEXT_PRIMARY);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setBackground(ColorScheme.BG_SIDEBAR);
        feedbackArea.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));

        JScrollPane feedbackScroll = new JScrollPane(feedbackArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        feedbackScroll.setBorder(BorderFactory.createEmptyBorder());

        // ── Character counter ────────────────────────────────
        JLabel charCount = new JLabel("0 / 500 characters");
        charCount.setFont(F_SMALL);
        charCount.setForeground(ColorScheme.TEXT_MUTED);
        feedbackArea.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                int len = feedbackArea.getText().length();
                charCount.setText(len + " / 500 characters");
                charCount.setForeground(len > 500 ? ColorScheme.ERROR : ColorScheme.TEXT_MUTED);
            }
            public void insertUpdate(DocumentEvent e)  { update(); }
            public void removeUpdate(DocumentEvent e)  { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        // ── Buttons ──────────────────────────────────────────
        JButton btnSubmit = btn("Submit Feedback", ColorScheme.BTN_SUCCESS, Color.WHITE);
        JButton btnClearF = btn("Clear", ColorScheme.BTN_SECONDARY, Color.WHITE);

        addHoverEffect(btnSubmit, ColorScheme.BTN_SUCCESS, ColorScheme.BTN_SUCCESS_HOVER);
        addHoverEffect(btnClearF, ColorScheme.BTN_SECONDARY, ColorScheme.BTN_SECONDARY_HOVER);

        btnSubmit.addActionListener(e -> {
            String text     = feedbackArea.getText().trim();
            String category = (String) catCombo.getSelectedItem();
            String priority = (String) priorityCombo.getSelectedItem();
            
            if (text.isEmpty()) {
                warn("Please write your feedback before submitting.");
                return;
            }
            if (text.length() > 500) {
                warn("Feedback must be 500 characters or less.");
                return;
            }
            
            // Save to new feedback table
            saveFeedback(category, priority, text);
            
            JOptionPane.showMessageDialog(this,
                    "<html><b>Thank you for your feedback!</b><br><br>"
                            + "Category: " + category + "<br>"
                            + "Priority: " + priority + "<br>"
                            + "Your feedback has been recorded.</html>",
                    "Submitted", JOptionPane.INFORMATION_MESSAGE);
            feedbackArea.setText("");
            catCombo.setSelectedIndex(0);
            priorityCombo.setSelectedIndex(1);
        });

        btnClearF.addActionListener(e -> {
            feedbackArea.setText("");
            catCombo.setSelectedIndex(0);
            priorityCombo.setSelectedIndex(1);
        });

        // ── Button row ───────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnSubmit);
        btnRow.add(btnClearF);
        btnRow.add(Box.createHorizontalStrut(14));
        btnRow.add(charCount);

        // ── Stack all widgets in the card ────────────────────
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        stack.add(nameLabel);
        stack.add(Box.createVerticalStrut(4));
        stack.add(nameField);
        stack.add(Box.createVerticalStrut(12));
        stack.add(catLabel);
        stack.add(Box.createVerticalStrut(4));
        stack.add(catCombo);
        stack.add(Box.createVerticalStrut(12));
        stack.add(priorityLabel);
        stack.add(Box.createVerticalStrut(4));
        stack.add(priorityCombo);
        stack.add(Box.createVerticalStrut(12));
        stack.add(fbLabel);
        stack.add(Box.createVerticalStrut(4));
        stack.add(feedbackScroll);
        stack.add(Box.createVerticalStrut(10));
        stack.add(btnRow);
        card.add(stack, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // ════════════════════════════════════════════════════════
    //  HEADER  (contains marquee bar — Feature 1)
    // ════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(ColorScheme.PRIMARY);

        // ── Top row: profile dropdown + title + stat cards + user panel ─────────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(ColorScheme.PRIMARY);
        topRow.setBorder(new EmptyBorder(12, 20, 8, 24));

        // Left section with profile dropdown and titles
        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftSection.setOpaque(false);
        
        // Add profile dropdown
        profileDropdown = new ProfileDropdown(currentUser, this);
        leftSection.add(profileDropdown);
        
        // Add titles
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        JLabel t1 = new JLabel("Kebele ID Management System");
        t1.setFont(F_TITLE); t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Kebele: " + currentUser.getKebeleId()
                + "  |  SQLite Embedded  |  v2.0");
        t2.setFont(F_SMALL); t2.setForeground(ColorScheme.withAlpha(ColorScheme.TEXT_ON_DARK, 180));
        titles.add(t1); titles.add(t2);
        leftSection.add(titles);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        sTotal  = statCard("Total",  "0", ColorScheme.INFO);
        sActive = statCard("Active", "0", ColorScheme.SUCCESS);
        sMale   = statCard("Male",   "0", ColorScheme.ROLE_DATA_ENCODER);
        sFemale = statCard("Female", "0", ColorScheme.WARNING);
        right.add(sTotal);  right.add(sActive);
        right.add(sMale);   right.add(sFemale);
        right.add(buildUserPanel());

        topRow.add(leftSection, BorderLayout.WEST);
        topRow.add(right,  BorderLayout.EAST);

        // ╔══════════════════════════════════════════════════╗
        // ║  IMPROVED MARQUEE BAR                           ║
        // ║  Professional scrolling ticker with gradient    ║
        // ║  background, icons, and smooth animation.       ║
        // ╚══════════════════════════════════════════════════╝
        marqueePanel = new MarqueePanel();
        marqueePanel.setText("0", "0", "0", "0", "0", 
                           currentUser.getFullName(), 
                           currentUser.getRole(), 
                           currentUser.getKebeleId());

        h.add(topRow,      BorderLayout.NORTH);
        h.add(marqueePanel, BorderLayout.SOUTH);
        return h;
    }

    // ── User panel (top-right corner) ───────────────────────
    private JPanel buildUserPanel() {
        JPanel p = new JPanel(new GridLayout(4, 1, 0, 2));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel name = new JLabel(currentUser.getFullName());
        name.setFont(F_BOLD); name.setForeground(Color.WHITE);

        Color roleColor = ColorScheme.getRoleColor(currentUser.getRole());
        JLabel role = new JLabel(
                currentUser.getRole() + " | " + currentUser.getKebeleId());
        role.setFont(F_SMALL); role.setForeground(roleColor);

        // Backup button (Admin only)
        JButton backup = new JButton("💾 Backup");
        backup.setFont(F_SMALL); backup.setForeground(Color.WHITE);
        backup.setBackground(ColorScheme.ROLE_SUPERVISOR);
        backup.setBorderPainted(false); backup.setFocusPainted(false);
        backup.setContentAreaFilled(true);
        backup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backup.setVisible(currentUser.isAdmin()); // Only visible to admins
        addHoverEffect(backup, ColorScheme.ROLE_SUPERVISOR, ColorScheme.darker(ColorScheme.ROLE_SUPERVISOR, 0.2f));
        backup.addActionListener(e -> utils.BackupUtils.showBackupDialog(this));

        JButton logout = new JButton("Logout");
        logout.setFont(F_SMALL); logout.setForeground(Color.WHITE);
        logout.setBackground(ColorScheme.BTN_DANGER);
        logout.setBorderPainted(false); logout.setFocusPainted(false);
        logout.setContentAreaFilled(true);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 2 — MouseListener on Logout button     ║
        // ╚══════════════════════════════════════════════════╝
        addHoverEffect(logout, ColorScheme.BTN_DANGER, ColorScheme.BTN_DANGER_HOVER);

        logout.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(
                    this, "Logout?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                if (marqueePanel != null) marqueePanel.stop();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "LOGOUT", "Logged out");
                
                // Get icon images from this frame
                java.util.List<java.awt.Image> icons = getIconImages();
                
                dispose();
                SwingUtilities.invokeLater(() -> {
                    try {
                        DatabaseManager.initDatabase();
                        auth.LoginFrame loginFrame = new auth.LoginFrame();
                        loginFrame.setIconImages(icons);
                        loginFrame.setVisible(true);
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
            }
        });

        p.add(name); p.add(role); p.add(backup); p.add(logout);
        return p;
    }

    // ════════════════════════════════════════════════════════
    //  FORM PANEL  (contains non-editable Kebele ID — Feature 3)
    // ════════════════════════════════════════════════════════
    private JPanel buildFormPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ColorScheme.BG_MAIN);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(ColorScheme.BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(20, 20, 16, 20)));

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.insets  = new Insets(5, 4, 5, 4);
        g.weightx = 1;
        int row = 0;

        // ── Section header ───────────────────────────────────
        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        String roleDisplay = isViewer()
                ? "Viewer — Your Record" : currentUser.getRole();
        JLabel sec = new JLabel(
                "  Resident Registration  [" + roleDisplay + "]");
        sec.setFont(F_BOLD); sec.setForeground(ColorScheme.PRIMARY);
        sec.setOpaque(true); sec.setBackground(ColorScheme.INFO_LIGHT);
        sec.setBorder(new EmptyBorder(8, 10, 8, 10));
        card.add(sec, g); row++;

        // ── Viewer notice ────────────────────────────────────
        if (isViewer()) {
            g.gridy = row++;
            JPanel notice = new JPanel(new BorderLayout());
            notice.setBackground(ColorScheme.WARNING_LIGHT);
            notice.setBorder(new CompoundBorder(
                    new LineBorder(ColorScheme.WARNING, 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
            JLabel nLbl = new JLabel(
                    "<html><b>Viewer:</b> You can view and update "
                            + "your own record only.</html>");
            nLbl.setFont(F_SMALL); notice.add(nLbl);
            card.add(notice, g);
        }

        g.gridy = row++; card.add(separator(), g);

        // ── Photo preview ────────────────────────────────────
        g.gridy = row++;
        lblPhoto = new JLabel("No Photo", SwingConstants.CENTER);
        lblPhoto.setPreferredSize(new Dimension(80, 80));
        lblPhoto.setOpaque(true);
        lblPhoto.setBackground(ColorScheme.BG_SIDEBAR);
        lblPhoto.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1, true));
        lblPhoto.setFont(F_SMALL); lblPhoto.setForeground(ColorScheme.TEXT_MUTED);
        JPanel pp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        pp.setOpaque(false); pp.add(lblPhoto);
        card.add(pp, g);

        // ── Build text fields ────────────────────────────────
        fKebeleId      = field("KBL-2026-001");
        fFirst         = field("First Name");
        fLast          = field("Last Name");
        fFather        = field("Father's Name");
        fMother        = field("Mother's Name (Optional)");
        fBirth         = field("YYYY-MM-DD");
        fPhone         = field("+251 9XX XXX XXX");
        fAddress       = field("City / Sub-city");
        fAddressDetail = field("Woreda / House No. / Details");
        fBirthPlace    = field("Birth Place");
        fOccupation    = field("Occupation");
        fKebeleNumber  = field("Kebele Number");
        fIssuedDate    = field("YYYY-MM-DD");

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 3 — NON-EDITABLE JTextField            ║
        // ║  Kebele ID is system-assigned; regular users    ║
        // ║  must NOT be able to type in it.                ║
        // ║  setEditable(false) makes it display-only.      ║
        // ║  Grey background + tooltip reinforce the cue.   ║
        // ║  Only Admin/Supervisor override may edit it.    ║
        // ╚══════════════════════════════════════════════════╝
        fKebeleId.setEditable(false);                    // ← NOT EDITABLE
        fKebeleId.setBackground(ColorScheme.BG_SIDEBAR);   // grey = read-only
        fKebeleId.setForeground(ColorScheme.TEXT_MUTED);
        fKebeleId.setToolTipText(
                "Kebele ID is system-assigned — not editable");

        // Admins / Supervisors may override
        if (currentUser.canDeleteResident()) {
            fKebeleId.setEditable(true);
            fKebeleId.setBackground(ColorScheme.BG_CARD);
            fKebeleId.setForeground(ColorScheme.TEXT_PRIMARY);
            fKebeleId.setToolTipText("Admin: Kebele ID is editable");
        }

        // ── Add label + field pairs ──────────────────────────
        String[]     labels = {
                "Kebele ID  (read-only)", "First Name *", "Last Name *",
                "Father's Name *", "Mother's Name", "Birth Date *",
                "Phone", "Address", "Address Detail",
                "Birth Place", "Occupation", "Kebele Number", "Issued Date"
        };
        JTextField[] fields = {
                fKebeleId, fFirst, fLast, fFather, fMother, fBirth,
                fPhone, fAddress, fAddressDetail, fBirthPlace,
                fOccupation, fKebeleNumber, fIssuedDate
        };

        g.gridwidth = 1;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = row; g.weightx = 0.38;
            card.add(lbl(labels[i]), g);
            g.gridx = 1; g.weightx = 0.62;
            card.add(fields[i], g);
            row++;
        }

        // Gender
        g.gridx = 0; g.gridy = row; g.weightx = 0.38;
        card.add(lbl("Gender *"), g);
        g.gridx = 1; g.weightx = 0.62;
        cGender = combo("Male", "Female", "Other");
        card.add(cGender, g); row++;

        // Marital Status
        g.gridx = 0; g.gridy = row; g.weightx = 0.38;
        card.add(lbl("Marital Status"), g);
        g.gridx = 1; g.weightx = 0.62;
        cMaritalStatus = combo("Single", "Married", "Divorced", "Widowed", "Other");
        card.add(cMaritalStatus, g); row++;

        // Blood Group
        g.gridx = 0; g.gridy = row; g.weightx = 0.38;
        card.add(lbl("Blood Group"), g);
        g.gridx = 1; g.weightx = 0.62;
        cBloodGroup = combo("Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
        card.add(cBloodGroup, g); row++;

        // ID Status
        g.gridx = 0; g.gridy = row; g.weightx = 0.38;
        card.add(lbl("ID Status"), g);
        g.gridx = 1; g.weightx = 0.62;
        cStatus = combo("Active", "Inactive", "Deceased", "Relocated");
        if (isViewer()) cStatus.setEnabled(false);
        card.add(cStatus, g); row++;

        // Photo select button
        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        btnPhoto = btn("Select Photo", ColorScheme.BTN_SECONDARY, Color.WHITE);

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 2 — MouseListener on Photo button      ║
        // ╚══════════════════════════════════════════════════╝
        addHoverEffect(btnPhoto, ColorScheme.BTN_SECONDARY, ColorScheme.BTN_SECONDARY_HOVER);
        btnPhoto.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter(
                    "Image Files", "jpg", "jpeg", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                photoPath = fc.getSelectedFile().getAbsolutePath();
                try {
                    Image img = new ImageIcon(photoPath).getImage()
                            .getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    lblPhoto.setIcon(new ImageIcon(img));
                    lblPhoto.setText("");
                } catch (Exception ex) { lblPhoto.setText("Bad Image"); }
            }
        });
        card.add(btnPhoto, g); row++;

        g.gridy = row++; card.add(separator(), g);

        // ── CRUD button grid ─────────────────────────────────
        g.gridy = row;
        
        btnSave   = btn("Save",   ColorScheme.BTN_SUCCESS,             Color.WHITE);
        btnUpdate = btn("Update", ColorScheme.BTN_DANGER,                Color.WHITE);
        btnDelete = btn("Delete", ColorScheme.BTN_DANGER,              Color.WHITE);
        btnClear  = btn("Clear",  ColorScheme.BTN_SECONDARY, Color.WHITE);

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 2 — MouseListener on all CRUD buttons  ║
        // ╚══════════════════════════════════════════════════╝
        addHoverEffect(btnSave,   ColorScheme.BTN_SUCCESS,             ColorScheme.BTN_SUCCESS_HOVER);
        addHoverEffect(btnUpdate, ColorScheme.BTN_DANGER,                ColorScheme.BTN_DANGER_HOVER);
        addHoverEffect(btnDelete, ColorScheme.BTN_DANGER,              ColorScheme.BTN_DANGER_HOVER);
        addHoverEffect(btnClear,  ColorScheme.BTN_SECONDARY, ColorScheme.BTN_SECONDARY_HOVER);

        // Different button layout for Viewer vs other roles
        JPanel btns;
        if (isViewer()) {
            // Viewer: Only Update and Clear buttons (1 row, 2 columns)
            btns = new JPanel(new GridLayout(1, 2, 8, 8));
            btns.setOpaque(false);
            btns.add(btnUpdate);
            btns.add(btnClear);
            // Don't add Save and Delete buttons for Viewers
        } else {
            // Other roles: All 4 buttons (2 rows, 2 columns)
            btns = new JPanel(new GridLayout(2, 2, 8, 8));
            btns.setOpaque(false);
            btns.add(btnSave);
            btns.add(btnUpdate);
            btns.add(btnDelete);
            btns.add(btnClear);
        }

        // Save action
        btnSave.addActionListener(e -> {
            if (!currentUser.canAddResident()) {
                warn("No permission to add residents."); return; }
            
            // Get all field values
            String kid = val(fKebeleId, "KBL-2024-001");
            String fn  = val(fFirst,    "First Name");
            String ln  = val(fLast,     "Last Name");
            String father = val(fFather, "Father's Name");
            String mother = val(fMother, "Mother's Name (Optional)");
            String bd  = val(fBirth,    "YYYY-MM-DD");
            String phone = val(fPhone, "+251 9XX XXX XXX");
            String addr = val(fAddress, "City / Sub-city");
            String addrDetail = val(fAddressDetail, "Woreda / House No. / Details");
            String birthPlace = val(fBirthPlace, "Birth Place");
            String occupation = val(fOccupation, "Occupation");
            String kebeleNum = val(fKebeleNumber, "Kebele Number");
            String issuedDate = val(fIssuedDate, "YYYY-MM-DD");
            
            // Validate required fields
            if (kid.isEmpty() || fn.isEmpty() || ln.isEmpty() || father.isEmpty() || bd.isEmpty()) {
                warn("Fill all required fields (*)."); return; }
            if (!bd.matches("\\d{4}-\\d{2}-\\d{2}")) {
                warn("Birth date format must be YYYY-MM-DD."); return; }
            
            // Determine approval status based on user role
            String approvalStatus = "Pending";  // Default for non-admin users
            String approvedBy = null;
            String approvedAt = null;
            
            // Admin can directly approve while creating
            if (currentUser.getRole().equals("Admin")) {
                int approveNow = JOptionPane.showConfirmDialog(this,
                    "Do you want to approve this registration immediately?",
                    "Approval", JOptionPane.YES_NO_OPTION);
                if (approveNow == JOptionPane.YES_OPTION) {
                    approvalStatus = "Approved";
                    approvedBy = currentUser.getUsername();
                    approvedAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .format(new java.util.Date());
                }
            }
            
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement(
                            "INSERT INTO residents (kebele_id, first_name, last_name, father_name, " +
                            "mother_name, date_of_birth, gender, marital_status, blood_group, " +
                            "phone_number, address, address_detail, birth_place, occupation, " +
                            "kebele_number, id_status, issued_date, photo_path, approval_status, " +
                            "approved_by, approved_at, created_by) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                
                ps.setString(1, kid);
                ps.setString(2, fn);
                ps.setString(3, ln);
                ps.setString(4, father);
                ps.setString(5, mother.isEmpty() ? null : mother);
                ps.setString(6, bd);
                ps.setString(7, (String) cGender.getSelectedItem());
                ps.setString(8, (String) cMaritalStatus.getSelectedItem());
                ps.setString(9, (String) cBloodGroup.getSelectedItem());
                ps.setString(10, phone.isEmpty() ? null : phone);
                ps.setString(11, addr.isEmpty() ? null : addr);
                ps.setString(12, addrDetail.isEmpty() ? null : addrDetail);
                ps.setString(13, birthPlace.isEmpty() ? null : birthPlace);
                ps.setString(14, occupation.isEmpty() ? null : occupation);
                ps.setString(15, kebeleNum.isEmpty() ? null : kebeleNum);
                ps.setString(16, (String) cStatus.getSelectedItem());
                ps.setString(17, issuedDate.isEmpty() ? null : issuedDate);
                ps.setString(18, photoPath.isEmpty() ? null : photoPath);
                ps.setString(19, approvalStatus);
                ps.setString(20, approvedBy);
                ps.setString(21, approvedAt);
                ps.setString(22, currentUser.getUsername());
                
                ps.executeUpdate();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "ADD_RESIDENT",
                        "Added: " + fn + " " + ln + " [Status: " + approvalStatus + "]");
                
                // Refresh profile icon if photo was added
                if (profileDropdown != null) {
                    profileDropdown.refreshProfileIcon();
                }
                
                if (approvalStatus.equals("Pending")) {
                    info("Resident registration submitted successfully!\nStatus: Pending Admin Approval");
                } else {
                    info("Resident registered and approved successfully!");
                }
                
                clearForm(); refreshTable(); refreshStats();
                refreshMarqueeText();
            } catch (SQLException ex) {
                error("Save failed: " + ex.getMessage()); }
        });

        // Update action - Only for approved residents
        btnUpdate.addActionListener(e -> {
            if (!currentUser.canEditResident() && !isViewer()) {
                warn("No permission to edit."); return; }
            if (selectedId < 0) { warn("Select a resident first."); return; }
            
            // Check if resident is approved
            try (PreparedStatement chk = DatabaseManager.getConnection()
                    .prepareStatement(
                            "SELECT approval_status, created_by FROM residents WHERE id=?")) {
                chk.setInt(1, selectedId);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) {
                        String approvalStatus = rs.getString("approval_status");
                        
                        // Only allow updates to approved residents (except for Admin)
                        if (!"Approved".equals(approvalStatus) && !currentUser.getRole().equals("Admin")) {
                            warn("Cannot update: This registration is " + approvalStatus + 
                                 ".\nOnly approved residents can be updated.");
                            return;
                        }
                        
                        // Viewer can only update their own record
                        if (isViewer() && !currentUser.getUsername()
                                .equals(rs.getString("created_by"))) {
                            warn("You can only update your own record.");
                            return;
                        }
                    }
                }
            } catch (SQLException ex) {
                error("Check failed: " + ex.getMessage()); return; }
            
            String fn = val(fFirst, "First Name");
            String ln = val(fLast,  "Last Name");
            String bd = val(fBirth, "YYYY-MM-DD");
            if (fn.isEmpty() || ln.isEmpty() || bd.isEmpty()) {
                warn("Required fields cannot be empty."); return; }
            int c = JOptionPane.showConfirmDialog(this,
                    "Update '" + fn + " " + ln + "'?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement(
                            "UPDATE residents SET kebele_id=?, first_name=?, last_name=?, " +
                            "father_name=?, mother_name=?, date_of_birth=?, gender=?, " +
                            "marital_status=?, blood_group=?, phone_number=?, address=?, " +
                            "address_detail=?, birth_place=?, occupation=?, kebele_number=?, " +
                            "id_status=?, issued_date=?, photo_path=? WHERE id=?")) {
                
                ps.setString(1, val(fKebeleId, "KBL-2024-001"));
                ps.setString(2, fn);
                ps.setString(3, ln);
                ps.setString(4, val(fFather, "Father's Name"));
                ps.setString(5, val(fMother, "Mother's Name (Optional)"));
                ps.setString(6, bd);
                ps.setString(7, (String) cGender.getSelectedItem());
                ps.setString(8, (String) cMaritalStatus.getSelectedItem());
                ps.setString(9, (String) cBloodGroup.getSelectedItem());
                ps.setString(10, val(fPhone, "+251 9XX XXX XXX"));
                ps.setString(11, val(fAddress, "City / Sub-city"));
                ps.setString(12, val(fAddressDetail, "Woreda / House No. / Details"));
                ps.setString(13, val(fBirthPlace, "Birth Place"));
                ps.setString(14, val(fOccupation, "Occupation"));
                ps.setString(15, val(fKebeleNumber, "Kebele Number"));
                ps.setString(16, (String) cStatus.getSelectedItem());
                ps.setString(17, val(fIssuedDate, "YYYY-MM-DD"));
                ps.setString(18, photoPath);
                ps.setInt(19, selectedId);
                
                ps.executeUpdate();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "EDIT_RESIDENT",
                        "Updated ID: " + selectedId);
                info("Record updated successfully!");
                
                // Refresh profile icon if photo was updated
                if (profileDropdown != null) {
                    profileDropdown.refreshProfileIcon();
                }
                
                if (isViewer()) loadViewerOwnRecord();
                else { clearForm(); refreshTable();
                    refreshStats(); refreshMarqueeText(); }
            } catch (SQLException ex) {
                error("Update failed: " + ex.getMessage()); }
        });

        // Delete action - Only approved residents can be deleted (except Admin)
        btnDelete.addActionListener(e -> {
            if (!currentUser.canDeleteResident()) {
                warn("No permission to delete."); return; }
            if (selectedId < 0) { warn("Select a resident first."); return; }
            int r = table.getSelectedRow();
            String name = (r >= 0)
                    ? (String) model.getValueAt(r, 2) : "this resident";
            int c = JOptionPane.showConfirmDialog(this,
                    "DELETE '" + name + "'? This cannot be undone!",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement("DELETE FROM residents WHERE id=?")) {
                ps.setInt(1, selectedId); ps.executeUpdate();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "DELETE_RESIDENT",
                        "Deleted ID: " + selectedId);
                info("Resident deleted.");
                clearForm(); refreshTable(); refreshStats();
                refreshMarqueeText();
            } catch (SQLException ex) {
                error("Delete failed: " + ex.getMessage()); }
        });

        // Clear action
        btnClear.addActionListener(e -> {
            clearForm();
            if (isViewer()) loadViewerOwnRecord();
        });

        card.add(btns, g); row++;

        // ID Card button (all users including viewers can generate their own ID)
        g.gridy = row;
        btnIDCard = btn("Generate ID Card",
                ColorScheme.ROLE_SUPERVISOR, ColorScheme.TEXT_ON_DARK);
        addHoverEffect(btnIDCard,
                ColorScheme.ROLE_SUPERVISOR, ColorScheme.darker(ColorScheme.ROLE_SUPERVISOR, 0.2f));
        btnIDCard.addActionListener(e -> {
            if (selectedId < 0) {
                warn("Select a resident first."); return; }
            IDCardPanel.showIDCard(this, selectedId);
        });
        card.add(btnIDCard, g);

        // Wrap in JScrollPane for scrolling when form is too long
        JScrollPane scrollPane = new JScrollPane(card);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        wrap.add(scrollPane, BorderLayout.CENTER);
        return wrap;
    }

    // ════════════════════════════════════════════════════════
    //  TABLE PANEL
    // ════════════════════════════════════════════════════════
    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ColorScheme.BG_MAIN);

        // ── Search bar ───────────────────────────────────────
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(ColorScheme.BG_CARD);
        bar.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 14, 10, 14)));

        fSearch = field("Search by name, Kebele ID, phone...");
        fSearch.setFont(F_LABEL);
        if (isViewer()) fSearch.setEnabled(false);
        fSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                if (isViewer()) return;
                String kw = val(fSearch,
                        "Search by name, Kebele ID, phone...").trim();
                if (kw.isEmpty()) refreshTable(); else searchTable(kw);
            }
        });

        btnSearch  = btn("Search",  ColorScheme.BTN_PRIMARY,             Color.WHITE);
        btnRefresh = btn("Refresh", ColorScheme.BTN_SUCCESS, Color.WHITE);
        btnPrint   = btn("Print",   ColorScheme.ROLE_DATA_ENCODER, Color.WHITE);
        JButton btnExport = btn("Export", ColorScheme.ROLE_SUPERVISOR, Color.WHITE);

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 2 — MouseListener on toolbar buttons   ║
        // ╚══════════════════════════════════════════════════╝
        addHoverEffect(btnSearch,  ColorScheme.BTN_PRIMARY, ColorScheme.BTN_PRIMARY_HOVER);
        addHoverEffect(btnRefresh, ColorScheme.BTN_SUCCESS, ColorScheme.BTN_SUCCESS_HOVER);
        addHoverEffect(btnPrint,   ColorScheme.ROLE_DATA_ENCODER, ColorScheme.darker(ColorScheme.ROLE_DATA_ENCODER, 0.2f));
        addHoverEffect(btnExport,  ColorScheme.ROLE_SUPERVISOR, ColorScheme.darker(ColorScheme.ROLE_SUPERVISOR, 0.2f));

        if (isViewer()) {
            btnSearch.setEnabled(false);
            btnPrint.setEnabled(false);
            btnExport.setEnabled(false);
        }

        btnSearch.addActionListener(e -> {
            if (isViewer()) return;
            String kw = val(fSearch,
                    "Search by name, Kebele ID, phone...").trim();
            if (kw.isEmpty()) refreshTable(); else searchTable(kw);
        });
        btnRefresh.addActionListener(e -> {
            if (isViewer()) loadViewerOwnRecord();
            else { refreshTable(); refreshStats(); refreshMarqueeText(); }
        });
        btnPrint.addActionListener(e -> {
            if (!currentUser.canPrintID()) {
                warn("No permission to print."); return; }
            try {
                table.print(JTable.PrintMode.FIT_WIDTH,
                        new MessageFormat("Kebele ID System"),
                        new MessageFormat("Page {0}"));
            } catch (Exception ex) {
                error("Print error: " + ex.getMessage()); }
        });
        btnExport.addActionListener(e -> {
            if (isViewer()) return;
            String fileName = "KebeleResidents_" + 
                java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            utils.ExportUtils.showExportDialog(this, model, fileName);
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnSearch); btnRow.add(btnRefresh); btnRow.add(btnExport); btnRow.add(btnPrint);
        bar.add(fSearch, BorderLayout.CENTER);
        bar.add(btnRow,  BorderLayout.EAST);

        // ── Table with Photo Column ────────────────────────────────────────────
        String[] cols = {
                "#", "Photo", "Kebele ID", "Full Name", "Gender",
                "Birth Date", "Phone", "Address", "Status", "Registered"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 1 ? ImageIcon.class : String.class; // Photo column returns ImageIcon
            }
        };
        table = new JTable(model);
        
        // Set custom renderer for photo column
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof ImageIcon) {
                    JLabel label = new JLabel((ImageIcon) value);
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setOpaque(true);
                    label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    label.setBorder(new EmptyBorder(2, 2, 2, 2));
                    return label;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        
        // Set photo column width
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setMinWidth(60);
        
        // Set row height to accommodate photos
        table.setRowHeight(50);
        
        styleTable();

        table.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int r = table.getSelectedRow();
            if (r < 0) return;
            selectedId = (int) model.getValueAt(r, 0);
            loadFormFromDB(selectedId);
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1, true));
        scroll.getViewport().setBackground(ColorScheme.BG_CARD);

        // ── Footer tip ───────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        footer.setOpaque(false);
        String note = isViewer()
                ? "Showing your record only — contact admin for full access"
                : "Click any row to load  |  Role: " + currentUser.getRole()
                + "  |  Kebele: " + currentUser.getKebeleId();
        JLabel tip = new JLabel(note);
        tip.setFont(F_SMALL);
        tip.setForeground(isViewer() ? ColorScheme.WARNING : ColorScheme.TEXT_MUTED);
        footer.add(tip);

        panel.add(bar,    BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    // ════════════════════════════════════════════════════════
    //  USER MANAGEMENT PANEL
    // ════════════════════════════════════════════════════════
    private JPanel buildUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ColorScheme.BG_MAIN);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel(
                "User Management — Assign Roles & Status");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(ColorScheme.PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(title, BorderLayout.NORTH);
        
        // Create tabbed interface for Admin users
        if (currentUser.getRole().equals("Admin")) {
            JTabbedPane userTabs = new JTabbedPane();
            userTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
            userTabs.setBackground(ColorScheme.BG_MAIN);
            
            // Tab 1: Manage existing users
            userTabs.addTab("  👥 Manage Users  ", buildManageUsersPanel());
            
            // Tab 2: Pending user approvals
            userTabs.addTab("  ⏳ Pending User Approvals  ", buildPendingUsersPanel());
            
            panel.add(userTabs, BorderLayout.CENTER);
            return panel;
        }
        
        // For non-admin users (Supervisor), show only manage users panel
        panel.add(buildManageUsersPanel(), BorderLayout.CENTER);
        return panel;
    }
    
    // ════════════════════════════════════════════════════════
    //  MANAGE USERS PANEL (existing users)
    // ════════════════════════════════════════════════════════
    private JPanel buildManageUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(ColorScheme.BG_MAIN);
        
        JPanel info = new JPanel(new BorderLayout());
        info.setBackground(ColorScheme.INFO_LIGHT);
        info.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.INFO, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        JLabel infoLbl = new JLabel(
                "<html><b>Roles:</b> Viewer = own record | "
                        + "DataEncoder = add | Staff = add+edit | "
                        + "Supervisor = add+edit+delete | Admin = full</html>");
        infoLbl.setFont(F_SMALL);
        info.add(infoLbl);

        String[] cols = {
                "ID", "Full Name", "Username",
                "Role", "Kebele", "Status", "Registered"
        };
        userModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userModel);
        styleUserTable();
        loadUsers();

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1, true));

        // ── Actions bar ──────────────────────────────────────
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actions.setBackground(ColorScheme.BG_CARD);
        actions.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel lblRole = new JLabel("Change Role:");
        lblRole.setFont(F_BOLD);
        JComboBox<String> roleCombo = new JComboBox<>(
                new String[]{"Viewer","DataEncoder","Staff","Supervisor","Admin"});
        roleCombo.setFont(F_LABEL);
        roleCombo.setPreferredSize(new Dimension(130, 34));

        JLabel lblStatus = new JLabel("Change Status:");
        lblStatus.setFont(F_BOLD);
        JComboBox<String> statusCombo = new JComboBox<>(
                new String[]{"Active", "Inactive"});
        statusCombo.setFont(F_LABEL);
        statusCombo.setPreferredSize(new Dimension(110, 34));

        JButton btnApply        = btn("Apply Changes",  ColorScheme.BTN_SUCCESS,   Color.WHITE);
        JButton btnRefreshUsers = btn("Refresh",        ColorScheme.BTN_SECONDARY, Color.WHITE);
        JButton btnResetPw      = btn("Reset Password", ColorScheme.BTN_DANGER,      Color.WHITE);

        // ╔══════════════════════════════════════════════════╗
        // ║  FEATURE 2 — MouseListener on management btns   ║
        // ╚══════════════════════════════════════════════════╝
        addHoverEffect(btnApply,        ColorScheme.BTN_SUCCESS,   ColorScheme.BTN_SUCCESS_HOVER);
        addHoverEffect(btnRefreshUsers, ColorScheme.BTN_SECONDARY, ColorScheme.BTN_SECONDARY_HOVER);
        addHoverEffect(btnResetPw,      ColorScheme.BTN_DANGER,      ColorScheme.BTN_DANGER_HOVER);

        btnApply.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row < 0) { warn("Select a user first."); return; }
            int    uid      = (int)    userModel.getValueAt(row, 0);
            String uname    = (String) userModel.getValueAt(row, 2);
            if (uid == currentUser.getId()) {
                warn("You cannot change your own role."); return; }
            String newRole   = (String) roleCombo.getSelectedItem();
            String newStatus = (String) statusCombo.getSelectedItem();
            int c = JOptionPane.showConfirmDialog(this,
                    "<html>Apply to <b>" + uname + "</b>?<br>"
                            + "Role: <b>" + newRole + "</b>  Status: <b>"
                            + newStatus + "</b></html>",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement(
                            "UPDATE users SET role=?,status=? WHERE id=?")) {
                ps.setString(1, newRole); ps.setString(2, newStatus);
                ps.setInt(3, uid); ps.executeUpdate();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "ROLE_CHANGE",
                        uname + " -> " + newRole + "/" + newStatus);
                info("Updated " + uname + " -> " + newRole);
                loadUsers();
            } catch (SQLException ex) {
                error("Failed: " + ex.getMessage()); }
        });

        btnResetPw.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row < 0) { warn("Select a user first."); return; }
            int    uid   = (int)    userModel.getValueAt(row, 0);
            String uname = (String) userModel.getValueAt(row, 2);
            if (uid == currentUser.getId()) {
                warn("Change your own password from settings."); return; }
            String newPw = JOptionPane.showInputDialog(this,
                    "New password for '" + uname + "' (min 6 chars):",
                    "Reset Password", JOptionPane.QUESTION_MESSAGE);
            if (newPw == null || newPw.trim().length() < 6) {
                warn("Minimum 6 characters."); return; }
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement(
                            "UPDATE users SET password=? WHERE id=?")) {
                ps.setString(1,
                        utils.PasswordUtils.hashPassword(newPw.trim()));
                ps.setInt(2, uid); ps.executeUpdate();
                DatabaseManager.logActivity(currentUser.getId(),
                        currentUser.getUsername(), "RESET_PW",
                        "Reset password: " + uname);
                info("Password reset for '" + uname + "'.");
            } catch (SQLException ex) {
                error("Failed: " + ex.getMessage()); }
        });

        btnRefreshUsers.addActionListener(e -> loadUsers());

        userTable.getSelectionModel().addListSelectionListener(ev -> {
            if (ev.getValueIsAdjusting()) return;
            int row = userTable.getSelectedRow();
            if (row < 0) return;
            roleCombo.setSelectedItem(userModel.getValueAt(row, 3));
            statusCombo.setSelectedItem(userModel.getValueAt(row, 5));
        });

        actions.add(lblRole);   actions.add(roleCombo);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(lblStatus); actions.add(statusCombo);
        actions.add(Box.createHorizontalStrut(10));
        actions.add(btnApply);  actions.add(btnResetPw);
        actions.add(btnRefreshUsers);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(ColorScheme.BG_MAIN);
        center.add(info,    BorderLayout.NORTH);
        center.add(scroll,  BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }
    
    // ════════════════════════════════════════════════════════
    //  PENDING USERS PANEL (user approval management)
    // ════════════════════════════════════════════════════════
    private JTable pendingUsersTable;
    private DefaultTableModel pendingUsersModel;
    
    private JPanel buildPendingUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ColorScheme.BG_MAIN);
        
        // ── Header ───────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("Pending User Registrations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ColorScheme.PRIMARY);
        
        JLabel subtitle = new JLabel("Review and approve new user account requests");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(ColorScheme.TEXT_MUTED);
        
        JPanel titleArea = new JPanel(new GridLayout(2, 1, 0, 4));
        titleArea.setOpaque(false);
        titleArea.add(title);
        titleArea.add(subtitle);
        
        headerPanel.add(titleArea, BorderLayout.WEST);
        
        // Refresh button
        JButton btnRefreshPendingUsers = btn("🔄 Refresh", ColorScheme.BTN_SECONDARY, Color.WHITE);
        btnRefreshPendingUsers.addActionListener(e -> refreshPendingUsersTable());
        headerPanel.add(btnRefreshPendingUsers, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // ── Pending users table ──────────────────────────────
        String[] columns = {"ID", "Full Name", "Username", "Kebele", "Registered", "Actions"};
        pendingUsersModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5; // Only Actions column
            }
        };
        
        pendingUsersTable = new JTable(pendingUsersModel);
        pendingUsersTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pendingUsersTable.setRowHeight(40);
        pendingUsersTable.setGridColor(ColorScheme.BORDER_LIGHT);
        pendingUsersTable.setSelectionBackground(ColorScheme.SELECTION_BG);
        pendingUsersTable.setSelectionForeground(ColorScheme.TEXT_PRIMARY);
        pendingUsersTable.setBackground(ColorScheme.BG_CARD);
        pendingUsersTable.setForeground(ColorScheme.TEXT_PRIMARY);
        
        // Set column widths
        pendingUsersTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        pendingUsersTable.getColumnModel().getColumn(1).setPreferredWidth(180);  // Full Name
        pendingUsersTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Username
        pendingUsersTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Kebele
        pendingUsersTable.getColumnModel().getColumn(4).setPreferredWidth(140);  // Registered
        pendingUsersTable.getColumnModel().getColumn(5).setPreferredWidth(220);  // Actions (View/Edit + Approve + Reject)
        
        // Custom renderer and editor for actions column with working buttons
        pendingUsersTable.getColumnModel().getColumn(5).setCellRenderer(new UserApprovalButtonRenderer());
        pendingUsersTable.getColumnModel().getColumn(5).setCellEditor(new UserApprovalButtonEditor(pendingUsersTable));
        
        // Table header styling
        JTableHeader header = pendingUsersTable.getTableHeader();
        header.setBackground(ColorScheme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(pendingUsersTable);
        scrollPane.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1));
        scrollPane.getViewport().setBackground(ColorScheme.BG_CARD);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Load initial data
        refreshPendingUsersTable();
        
        return panel;
    }
    
    // ── Refresh pending users table ──────────────────────────
    private void refreshPendingUsersTable() {
        if (pendingUsersModel == null) return;
        
        pendingUsersModel.setRowCount(0);
        try (Statement s = DatabaseManager.getConnection().createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT * FROM users WHERE approval_status='Pending' ORDER BY created_at DESC")) {
            while (rs.next()) {
                pendingUsersModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("kebele_id"),
                        rs.getString("created_at"),
                        "" // Actions column (rendered by custom renderer)
                });
            }
        } catch (SQLException e) {
            error("Failed to load pending users: " + e.getMessage());
        }
    }
    
    // ── View pending user details ────────────────────────────
    private void viewPendingUserDetails(int row) {
        if (pendingUsersModel == null || row >= pendingUsersModel.getRowCount()) return;
        
        int userId = (int) pendingUsersModel.getValueAt(row, 0);
        
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JDialog dialog = new JDialog(this, "User Registration Details", true);
                    dialog.setSize(500, 400);
                    dialog.setLocationRelativeTo(this);
                    
                    JPanel content = new JPanel(new BorderLayout(0, 16));
                    content.setBackground(ColorScheme.BG_MAIN);
                    content.setBorder(new EmptyBorder(20, 24, 20, 24));
                    
                    // Title
                    JLabel titleLbl = new JLabel("User Registration Request");
                    titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    titleLbl.setForeground(ColorScheme.PRIMARY);
                    content.add(titleLbl, BorderLayout.NORTH);
                    
                    // Details panel
                    JPanel detailsPanel = new JPanel();
                    detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
                    detailsPanel.setBackground(ColorScheme.BG_CARD);
                    detailsPanel.setBorder(new CompoundBorder(
                            new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                            new EmptyBorder(16, 20, 16, 20)));
                    
                    addDetailRow(detailsPanel, "User ID:", String.valueOf(rs.getInt("id")));
                    addDetailRow(detailsPanel, "Full Name:", rs.getString("full_name"));
                    addDetailRow(detailsPanel, "Username:", rs.getString("username"));
                    addDetailRow(detailsPanel, "Kebele:", rs.getString("kebele_id"));
                    addDetailRow(detailsPanel, "Default Role:", rs.getString("role"));
                    addDetailRow(detailsPanel, "Status:", rs.getString("status"));
                    addDetailRow(detailsPanel, "Approval Status:", rs.getString("approval_status"));
                    addDetailRow(detailsPanel, "Registered:", rs.getString("created_at"));
                    
                    JScrollPane scrollPane = new JScrollPane(detailsPanel);
                    scrollPane.setBorder(BorderFactory.createEmptyBorder());
                    content.add(scrollPane, BorderLayout.CENTER);
                    
                    // Action buttons
                    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
                    btnPanel.setOpaque(false);
                    
                    JButton approveBtn = btn("✓ Approve", ColorScheme.BTN_SUCCESS, Color.WHITE);
                    approveBtn.addActionListener(e -> {
                        approveUserById(userId);
                        dialog.dispose();
                        refreshPendingUsersTable();
                        loadUsers();
                    });
                    
                    JButton rejectBtn = btn("✗ Reject", ColorScheme.BTN_DANGER, Color.WHITE);
                    rejectBtn.addActionListener(e -> {
                        rejectUserById(userId);
                        dialog.dispose();
                        refreshPendingUsersTable();
                    });
                    
                    JButton closeBtn = btn("Close", ColorScheme.BTN_SECONDARY, Color.WHITE);
                    closeBtn.addActionListener(e -> dialog.dispose());
                    
                    btnPanel.add(approveBtn);
                    btnPanel.add(rejectBtn);
                    btnPanel.add(closeBtn);
                    
                    content.add(btnPanel, BorderLayout.SOUTH);
                    
                    dialog.setContentPane(content);
                    dialog.setVisible(true);
                }
            }
        } catch (SQLException e) {
            error("Failed to load user details: " + e.getMessage());
        }
    }
    
    // ── Approve user from table ──────────────────────────────
    private void approveUser(int row) {
        if (pendingUsersModel == null || row >= pendingUsersModel.getRowCount()) return;
        
        int userId = (int) pendingUsersModel.getValueAt(row, 0);
        String username = (String) pendingUsersModel.getValueAt(row, 2);
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "Approve user registration for '" + username + "'?",
                "Confirm Approval", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            approveUserById(userId);
            refreshPendingUsersTable();
            loadUsers();
        }
    }
    
    // ── Reject user from table ───────────────────────────────
    private void rejectUser(int row) {
        if (pendingUsersModel == null || row >= pendingUsersModel.getRowCount()) return;
        
        int userId = (int) pendingUsersModel.getValueAt(row, 0);
        String username = (String) pendingUsersModel.getValueAt(row, 2);
        
        String reason = JOptionPane.showInputDialog(this,
                "Reject user registration for '" + username + "'?\nEnter reason (optional):",
                "Confirm Rejection", JOptionPane.QUESTION_MESSAGE);
        
        if (reason != null) { // User clicked OK
            rejectUserById(userId);
            refreshPendingUsersTable();
        }
    }
    
    // ── Approve user by ID ───────────────────────────────────
    private void approveUserById(int userId) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date());
            
            String sql = "UPDATE users SET approval_status = 'Approved', " +
                        "approved_by = ?, approved_at = ? WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, currentUser.getUsername());
                ps.setString(2, timestamp);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }
            
            // Get username for logging
            String username = "";
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement("SELECT username, full_name FROM users WHERE id=?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        username = rs.getString("username");
                    }
                }
            }
            
            DatabaseManager.logActivity(currentUser.getId(),
                    currentUser.getUsername(), "APPROVE_USER",
                    "Approved user: " + username + " (ID: " + userId + ")");
            
            info("User approved successfully! They can now log in.");
        } catch (SQLException e) {
            error("Failed to approve user: " + e.getMessage());
        }
    }
    
    // ── Reject user by ID ────────────────────────────────────
    private void rejectUserById(int userId) {
        try {
            String sql = "UPDATE users SET approval_status = 'Rejected' WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            
            // Get username for logging
            String username = "";
            try (PreparedStatement ps = DatabaseManager.getConnection()
                    .prepareStatement("SELECT username FROM users WHERE id=?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        username = rs.getString("username");
                    }
                }
            }
            
            DatabaseManager.logActivity(currentUser.getId(),
                    currentUser.getUsername(), "REJECT_USER",
                    "Rejected user: " + username + " (ID: " + userId + ")");
            
            info("User registration rejected.");
        } catch (SQLException e) {
            error("Failed to reject user: " + e.getMessage());
        }
    }

    // ── Status bar ───────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ColorScheme.PRIMARY);
        bar.setBorder(new EmptyBorder(8, 16, 8, 16));
        
        // Left: Professional footer
        JLabel l = new JLabel(
                "Federal Democratic Republic of Ethiopia | Developer: Wondatir Fetene | v2.0 | © 2026");
        l.setFont(F_SMALL); 
        l.setForeground(ColorScheme.TEXT_ON_DARK);
        
        // Right: User info
        JLabel r = new JLabel("User: " + currentUser.getFullName()
                + " | Role: " + currentUser.getRole()
                + " | Kebele: " + currentUser.getKebeleId());
        r.setFont(F_SMALL); 
        r.setForeground(ColorScheme.TEXT_ON_DARK);
        
        bar.add(l, BorderLayout.WEST);
        bar.add(r, BorderLayout.EAST);
        return bar;
    }

    // ════════════════════════════════════════════════════════
    //  ROLE PERMISSIONS
    // ════════════════════════════════════════════════════════
    private void applyRolePermissions() {
        SwingUtilities.invokeLater(() -> {
            // Save and Delete buttons don't exist for Viewers
            if (btnSave != null)
                btnSave.setEnabled(currentUser.canAddResident());
            if (btnDelete != null)
                btnDelete.setEnabled(currentUser.canDeleteResident());
            
            // Update button exists for all roles
            if (btnUpdate != null)
                btnUpdate.setEnabled(currentUser.canEditResident() || isViewer());
            
            // Print button (table print)
            if (btnPrint != null)
                btnPrint.setEnabled(currentUser.canPrintID());
            
            // ID Card button - all users including Viewers can generate their own ID
            if (btnIDCard != null)
                btnIDCard.setEnabled(true);
        });
    }

    // ════════════════════════════════════════════════════════
    //  VIEWER — load own record only (using linked_resident_id)
    // ════════════════════════════════════════════════════════
    private void loadViewerOwnRecord() {
        model.setRowCount(0);
        
        try {
            // Get the linked resident ID for this user
            int linkedResidentId = -1;
            String sql = "SELECT linked_resident_id FROM users WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        linkedResidentId = rs.getInt("linked_resident_id");
                    }
                }
            }
            
            if (linkedResidentId > 0) {
                // Load the linked resident record
                sql = "SELECT * FROM residents WHERE id = ?";
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                    ps.setInt(1, linkedResidentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            addRow(rs);
                            // Auto-select the row
                            if (table.getRowCount() > 0) {
                                table.setRowSelectionInterval(0, 0);
                            }
                        } else {
                            showNoRecordMessage();
                        }
                    }
                }
            } else {
                showNoRecordMessage();
            }
        } catch (SQLException e) {
            error("Error loading record: " + e.getMessage());
        }
        
        refreshStats();
        refreshMarqueeText();
    }
    
    private void showNoRecordMessage() {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this,
                "<html><b>Welcome " + currentUser.getFullName() + "!</b><br><br>"
                + "Your resident profile is pending approval.<br>"
                + "Once approved by admin, you can view and update your information.<br><br>"
                + "<i>Status: Pending Approval</i></html>",
                "Profile Pending",
                JOptionPane.INFORMATION_MESSAGE));
    }

    // ── Load users for management tab ────────────────────────
    private void loadUsers() {
        if (userModel == null) return;
        userModel.setRowCount(0);
        try (Statement s = DatabaseManager.getConnection().createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT id,full_name,username,role,kebele_id,"
                             + "status,created_at FROM users ORDER BY id ASC")) {
            while (rs.next())
                userModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("kebele_id"),
                        rs.getString("status"),
                        rs.getString("created_at")});
        } catch (SQLException e) {
            error("Load users: " + e.getMessage()); }
    }

    // ════════════════════════════════════════════════════════
    //  DATA METHODS
    // ════════════════════════════════════════════════════════
    private void refreshTable() {
        model.setRowCount(0);
        try (Statement s = DatabaseManager.getConnection().createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT * FROM residents ORDER BY id DESC")) {
            while (rs.next()) addRow(rs);
        } catch (SQLException e) {
            error("Load error: " + e.getMessage()); }
    }

    private void searchTable(String kw) {
        model.setRowCount(0);
        String sql =
                "SELECT * FROM residents WHERE "
                        + "LOWER(kebele_id) LIKE ? OR LOWER(first_name) LIKE ? OR "
                        + "LOWER(last_name) LIKE ? OR LOWER(phone_number) LIKE ? OR "
                        + "LOWER(address) LIKE ? ORDER BY id DESC";
        String p = "%" + kw.toLowerCase() + "%";
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) addRow(rs); }
        } catch (SQLException e) {
            error("Search error: " + e.getMessage()); }
    }

    private void addRow(ResultSet rs) throws SQLException {
        // Load photo and create thumbnail
        String photoPath = rs.getString("photo_path");
        ImageIcon photoIcon = null;
        
        if (photoPath != null && !photoPath.isEmpty()) {
            try {
                Image img = new ImageIcon(photoPath).getImage();
                Image scaledImg = img.getScaledInstance(45, 45, Image.SCALE_SMOOTH);
                photoIcon = new ImageIcon(scaledImg);
            } catch (Exception e) {
                // If photo loading fails, use placeholder
                photoIcon = createPhotoPlaceholder();
            }
        } else {
            photoIcon = createPhotoPlaceholder();
        }
        
        model.addRow(new Object[]{
                rs.getInt("id"),
                photoIcon, // Photo column
                rs.getString("kebele_id"),
                rs.getString("first_name") + " " + rs.getString("last_name"),
                rs.getString("gender"),
                rs.getString("date_of_birth"),
                rs.getString("phone_number"),
                rs.getString("address"),
                rs.getString("id_status"),
                rs.getString("created_at")});
    }
    
    /**
     * Creates a placeholder icon for residents without photos
     */
    private ImageIcon createPhotoPlaceholder() {
        BufferedImage img = new BufferedImage(45, 45, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background
        g2.setColor(ColorScheme.BG_HEADER_START);
        g2.fillRoundRect(0, 0, 45, 45, 8, 8);
        
        // Icon (person silhouette)
        g2.setColor(ColorScheme.TEXT_ON_DARK);
        // Head
        g2.fillOval(15, 8, 15, 15);
        // Body
        g2.fillRoundRect(10, 23, 25, 18, 8, 8);
        
        g2.dispose();
        return new ImageIcon(img);
    }

    private void loadFormFromDB(int id) {
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement("SELECT * FROM residents WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                setField(fKebeleId, rs.getString("kebele_id"),  "KBL-2026-001");
                setField(fFirst,    rs.getString("first_name"), "First Name");
                setField(fLast,     rs.getString("last_name"),  "Last Name");
                setField(fFather,   rs.getString("father_name"), "Father's Name");
                setField(fMother,   rs.getString("mother_name"), "Mother's Name (Optional)");
                setField(fBirth,    rs.getString("date_of_birth"), "YYYY-MM-DD");
                setField(fPhone,    rs.getString("phone_number"),
                        "+251 9XX XXX XXX");
                setField(fAddress,  rs.getString("address"),
                        "City / Sub-city");
                setField(fAddressDetail, rs.getString("address_detail"),
                        "Woreda / House No. / Details");
                setField(fBirthPlace, rs.getString("birth_place"), "Birth Place");
                setField(fOccupation, rs.getString("occupation"), "Occupation");
                setField(fKebeleNumber, rs.getString("kebele_number"), "Kebele Number");
                setField(fIssuedDate, rs.getString("issued_date"), "YYYY-MM-DD");
                cGender.setSelectedItem(rs.getString("gender"));
                cMaritalStatus.setSelectedItem(rs.getString("marital_status"));
                cBloodGroup.setSelectedItem(rs.getString("blood_group"));
                cStatus.setSelectedItem(rs.getString("id_status"));
                photoPath = rs.getString("photo_path") != null
                        ? rs.getString("photo_path") : "";
                if (!photoPath.isEmpty()) {
                    try {
                        Image img = new ImageIcon(photoPath).getImage()
                                .getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                        lblPhoto.setIcon(new ImageIcon(img));
                        lblPhoto.setText("");
                    } catch (Exception ex) { lblPhoto.setText("Photo N/A"); }
                } else { lblPhoto.setIcon(null); lblPhoto.setText("No Photo"); }
            }
        } catch (SQLException e) {
            error("Load error: " + e.getMessage()); }
    }

    private void refreshStats() {
        updateStatCard(sTotal,  "Total",  count("1=1"),
                ColorScheme.INFO);
        updateStatCard(sActive, "Active", count("id_status='Active'"),
                ColorScheme.SUCCESS);
        updateStatCard(sMale,   "Male",   count("gender='Male'"),
                ColorScheme.ROLE_DATA_ENCODER);
        updateStatCard(sFemale, "Female", count("gender='Female'"),
                ColorScheme.WARNING);
    }

    private int count(String where) {
        try (Statement s = DatabaseManager.getConnection().createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COUNT(*) FROM residents WHERE " + where)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ════════════════════════════════════════════════════════
    //  UI HELPER METHODS
    // ════════════════════════════════════════════════════════
    private void clearForm() {
        String[]     phs = {
                "KBL-2026-001", "First Name", "Last Name",
                "YYYY-MM-DD",   "+251 9XX XXX XXX",
                "Sub-city / Woreda / House No."
        };
        JTextField[] fs  = {
                fKebeleId, fFirst, fLast, fBirth, fPhone, fAddress
        };
        for (int i = 0; i < fs.length; i++) setField(fs[i], "", phs[i]);
        cGender.setSelectedIndex(0); cStatus.setSelectedIndex(0);
        lblPhoto.setIcon(null); lblPhoto.setText("No Photo");
        photoPath = ""; selectedId = -1; table.clearSelection();
    }

    private void styleTable() {
        table.setFont(F_LABEL); table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ColorScheme.SELECTION_BG);
        table.setSelectionForeground(ColorScheme.TEXT_PRIMARY);
        table.setBackground(ColorScheme.BG_CARD); table.setForeground(ColorScheme.TEXT_PRIMARY);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JTableHeader hdr = table.getTableHeader();
        hdr.setBackground(ColorScheme.PRIMARY); hdr.setForeground(Color.WHITE);
        hdr.setFont(F_BOLD); hdr.setReorderingAllowed(false);

        int[] widths = {40,110,160,70,95,120,170,80,145};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.setDefaultRenderer(Object.class,
                new DefaultTableCellRenderer() {
                    @Override public Component getTableCellRendererComponent(
                            JTable t, Object v, boolean sel,
                            boolean foc, int r, int c) {
                        Component comp = super.getTableCellRendererComponent(
                                t, v, sel, foc, r, c);
                        if (!sel) comp.setBackground(
                                r % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR);
                        setBorder(new EmptyBorder(0, 10, 0, 10));
                        if (c == 7 && v != null && !sel)
                            comp.setForeground(switch (v.toString()) {
                                case "Active"   -> ColorScheme.SUCCESS;
                                case "Inactive" -> ColorScheme.WARNING;
                                case "Deceased" -> ColorScheme.ERROR;
                                default         -> ColorScheme.TEXT_MUTED;
                            });
                        else if (!sel) comp.setForeground(ColorScheme.TEXT_PRIMARY);
                        return comp;
                    }
                });
    }

    private void styleUserTable() {
        userTable.setFont(F_LABEL); userTable.setRowHeight(34);
        userTable.setShowGrid(false);
        userTable.setIntercellSpacing(new Dimension(0, 0));
        userTable.setSelectionBackground(ColorScheme.SELECTION_BG);
        userTable.setBackground(ColorScheme.BG_CARD); userTable.setForeground(ColorScheme.TEXT_PRIMARY);

        JTableHeader hdr = userTable.getTableHeader();
        hdr.setBackground(ColorScheme.PRIMARY); hdr.setForeground(Color.WHITE);
        hdr.setFont(F_BOLD); hdr.setReorderingAllowed(false);

        int[] widths = {40,160,120,110,80,80,140};
        for (int i = 0; i < widths.length; i++)
            userTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        userTable.setDefaultRenderer(Object.class,
                new DefaultTableCellRenderer() {
                    @Override public Component getTableCellRendererComponent(
                            JTable t, Object v, boolean sel,
                            boolean foc, int r, int c) {
                        Component comp = super.getTableCellRendererComponent(
                                t, v, sel, foc, r, c);
                        if (!sel) comp.setBackground(
                                r % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR);
                        setBorder(new EmptyBorder(0, 10, 0, 10));
                        if (c == 3 && v != null && !sel)
                            comp.setForeground(switch (v.toString()) {
                                case "Admin"       -> ColorScheme.ROLE_ADMIN;
                                case "Supervisor"  -> ColorScheme.ROLE_SUPERVISOR;
                                case "Staff"       -> ColorScheme.ROLE_STAFF;
                                case "DataEncoder" -> ColorScheme.ROLE_DATA_ENCODER;
                                default            -> ColorScheme.TEXT_MUTED;
                            });
                        else if (c == 5 && v != null && !sel)
                            comp.setForeground(
                                    v.toString().equals("Active") ? ColorScheme.SUCCESS : ColorScheme.ERROR);
                        else if (!sel) comp.setForeground(ColorScheme.TEXT_PRIMARY);
                        return comp;
                    }
                });
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  FEATURE 2 — addHoverEffect()                           ║
    // ║                                                          ║
    // ║  Attaches a MouseAdapter to any JButton.                ║
    // ║  mouseEntered → sets background to hoverColor           ║
    // ║  mouseExited  → restores background to normalColor      ║
    // ║                                                          ║
    // ║  Called on EVERY button in this file so all buttons     ║
    // ║  respond visually to mouse hover.                        ║
    // ╚══════════════════════════════════════════════════════════╝
    private void addHoverEffect(JButton btn,
                                Color normalColor,
                                Color hoverColor) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(hoverColor);   // ← ENTERED: highlight
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalColor);      // ← EXITED:  restore
            }
        });
    }

    // ── Placeholder text field ───────────────────────────────
    private JTextField field(String ph) {
        JTextField tf = new JTextField();
        tf.setFont(F_LABEL); tf.setForeground(ColorScheme.TEXT_MUTED); tf.setText(ph);
        tf.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(7, 10, 7, 10)));
        tf.setBackground(ColorScheme.BG_SIDEBAR);
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (tf.getText().equals(ph)) {
                    tf.setText(""); tf.setForeground(ColorScheme.TEXT_PRIMARY); } }
            @Override public void focusLost(FocusEvent e) {
                if (tf.getText().isEmpty()) {
                    tf.setText(ph); tf.setForeground(ColorScheme.TEXT_MUTED); } }
        });
        return tf;
    }

    private void setField(JTextField tf, String val, String ph) {
        if (val == null || val.isEmpty()) {
            tf.setText(ph); tf.setForeground(ColorScheme.TEXT_MUTED);
        } else { tf.setText(val); tf.setForeground(ColorScheme.TEXT_PRIMARY); }
    }

    private String val(JTextField tf, String ph) {
        String v = tf.getText().trim();
        return v.equals(ph) ? "" : v;
    }

    // ── Rounded button factory ───────────────────────────────
    private JButton btn(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()  ? bg.darker()   :
                        getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_BTN); b.setForeground(fg); b.setBackground(bg);
        b.setBorderPainted(false); b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        return b;
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_LABEL); cb.setBackground(ColorScheme.BG_CARD); cb.setForeground(ColorScheme.TEXT_PRIMARY);
        cb.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1, true));
        return cb;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL); l.setForeground(ColorScheme.TEXT_PRIMARY); return l;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(ColorScheme.BORDER_LIGHT); return sep;
    }

    private JLabel statCard(String label, String value, Color color) {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        updateStatCard(l, label, Integer.parseInt(value), color);
        return l;
    }

    private void updateStatCard(JLabel l, String label,
                                int value, Color color) {
        String hex = String.format("#%02X%02X%02X",
                color.getRed(), color.getGreen(), color.getBlue());
        l.setText("<html><div style='text-align:center;"
                + "padding:6px 16px;background:" + hex
                + ";border-radius:8px;'>"
                + "<span style='color:white;font-size:17px;"
                + "font-weight:bold;'>" + value + "</span><br>"
                + "<span style='color:#ecf0f1;font-size:10px;'>"
                + label + "</span></div></html>");
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg,
                "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg,
                "Warning", JOptionPane.WARNING_MESSAGE); }
    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg,
                "Error",   JOptionPane.ERROR_MESSAGE); }

    // ════════════════════════════════════════════════════════
    //  APPROVAL MANAGEMENT PANEL (Admin only)
    // ════════════════════════════════════════════════════════
    
    private JTable approvalTable;
    private DefaultTableModel approvalModel;
    
    /**
     * Builds the approval management panel for Admin users
     */
    private JPanel buildApprovalManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ColorScheme.BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Header ───────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("Pending Registration Approvals");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ColorScheme.PRIMARY);
        
        JLabel subtitle = new JLabel("Review and approve/reject resident registrations");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(ColorScheme.TEXT_MUTED);
        
        JPanel titleArea = new JPanel(new GridLayout(2, 1, 0, 4));
        titleArea.setOpaque(false);
        titleArea.add(title);
        titleArea.add(subtitle);
        
        headerPanel.add(titleArea, BorderLayout.WEST);
        
        // Refresh button
        JButton btnRefreshApprovals = btn("🔄 Refresh", ColorScheme.BTN_SECONDARY, Color.WHITE);
        btnRefreshApprovals.addActionListener(e -> refreshApprovalTable());
        headerPanel.add(btnRefreshApprovals, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // ── Approval table ───────────────────────────────────
        String[] columns = {"ID", "Kebele ID", "Full Name", "Father Name", "Gender", 
                           "Birth Date", "Status", "Submitted By", "Date", "Actions"};
        approvalModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 9; // Only Actions column
            }
        };

        approvalTable = new JTable(approvalModel);
        approvalTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        approvalTable.setRowHeight(40);
        approvalTable.setGridColor(ColorScheme.BORDER_LIGHT);
        approvalTable.setSelectionBackground(ColorScheme.SELECTION_BG);
        approvalTable.setSelectionForeground(ColorScheme.TEXT_PRIMARY);
        approvalTable.setBackground(ColorScheme.BG_CARD);
        approvalTable.setForeground(ColorScheme.TEXT_PRIMARY);

        // Set column widths
        approvalTable.getColumnModel().getColumn(0).setPreferredWidth(40);   // ID
        approvalTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Kebele ID
        approvalTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Full Name
        approvalTable.getColumnModel().getColumn(3).setPreferredWidth(120);  // Father Name
        approvalTable.getColumnModel().getColumn(4).setPreferredWidth(60);   // Gender
        approvalTable.getColumnModel().getColumn(5).setPreferredWidth(90);   // Birth Date
        approvalTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Status
        approvalTable.getColumnModel().getColumn(7).setPreferredWidth(100);  // Submitted By
        approvalTable.getColumnModel().getColumn(8).setPreferredWidth(120);  // Date
        approvalTable.getColumnModel().getColumn(9).setPreferredWidth(220);  // Actions (View/Edit + Approve + Reject)

        // Custom renderer for status column
        approvalTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected && value != null) {
                    String status = value.toString();
                    if ("Pending".equals(status)) {
                        setForeground(ColorScheme.WARNING);
                    } else if ("Approved".equals(status)) {
                        setForeground(ColorScheme.SUCCESS);
                    } else if ("Rejected".equals(status)) {
                        setForeground(ColorScheme.ERROR);
                    }
                }
                
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR);
                }
                
                return comp;
            }
        });

        // Custom renderer and editor for actions column with working buttons
        approvalTable.getColumnModel().getColumn(9).setCellRenderer(new ApprovalButtonRenderer());
        approvalTable.getColumnModel().getColumn(9).setCellEditor(new ApprovalButtonEditor(approvalTable));

        // Table header styling
        JTableHeader header = approvalTable.getTableHeader();
        header.setBackground(ColorScheme.PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(approvalTable);
        scrollPane.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1));
        scrollPane.getViewport().setBackground(ColorScheme.BG_CARD);

        panel.add(scrollPane, BorderLayout.CENTER);

        // Load initial data
        refreshApprovalTable();

        return panel;
    }

    /**
     * Refreshes the approval table with pending registrations
     */
    private void refreshApprovalTable() {
        if (approvalModel == null) return;
        
        approvalModel.setRowCount(0); // Clear existing data
        
        try {
            String sql = "SELECT id, kebele_id, first_name, last_name, father_name, gender, " +
                        "date_of_birth, approval_status, created_by, created_at " +
                        "FROM residents " +
                        "WHERE approval_status = 'Pending' " +
                        "ORDER BY created_at DESC";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String kebeleId = rs.getString("kebele_id");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String fullName = firstName + " " + lastName;
                    String fatherName = rs.getString("father_name");
                    String gender = rs.getString("gender");
                    String birthDate = rs.getString("date_of_birth");
                    String status = rs.getString("approval_status");
                    String createdBy = rs.getString("created_by");
                    String createdAt = rs.getString("created_at");
                    
                    // Format timestamp
                    String formattedDate = createdAt != null ? createdAt.substring(0, 16).replace("T", " ") : "";
                    
                    approvalModel.addRow(new Object[]{
                        id,
                        kebeleId,
                        fullName,
                        fatherName,
                        gender,
                        birthDate,
                        status,
                        createdBy,
                        formattedDate,
                        "Actions" // Placeholder for action buttons
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to load pending approvals: " + e.getMessage());
        }
    }

    /**
     * Views detailed information about a pending registration
     */
    private void viewApprovalDetails(int row) {
        if (approvalModel == null || row >= approvalModel.getRowCount()) return;
        
        int residentId = (Integer) approvalModel.getValueAt(row, 0);
        
        try {
            String sql = "SELECT * FROM residents WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, residentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Create detailed view dialog
                        JDialog dialog = new JDialog(this, "Registration Details", true);
                        dialog.setSize(600, 500);
                        dialog.setLocationRelativeTo(this);
                        
                        JPanel panel = new JPanel(new BorderLayout(0, 16));
                        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
                        panel.setBackground(ColorScheme.BG_CARD);
                        
                        // Title
                        JLabel titleLabel = new JLabel("Resident Registration Details");
                        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                        titleLabel.setForeground(ColorScheme.PRIMARY);
                        panel.add(titleLabel, BorderLayout.NORTH);
                        
                        // Details panel
                        JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 10, 8));
                        detailsPanel.setOpaque(false);
                        
                        // Map to store references to editable field components
                        Map<String, JTextField> editableFields = new HashMap<>();
                        
                        // Render fields with conditional logic for editable vs read-only
                        // Read-only: kebele_id
                        addFieldRow(detailsPanel, "Kebele ID:", createReadOnlyField(rs.getString("kebele_id")));
                        
                        // Editable: first_name
                        JTextField firstNameField = createEditableField(rs.getString("first_name"));
                        editableFields.put("first_name", firstNameField);
                        addFieldRow(detailsPanel, "First Name:", firstNameField);
                        
                        // Editable: last_name
                        JTextField lastNameField = createEditableField(rs.getString("last_name"));
                        editableFields.put("last_name", lastNameField);
                        addFieldRow(detailsPanel, "Last Name:", lastNameField);
                        
                        // Editable: father_name
                        JTextField fatherNameField = createEditableField(rs.getString("father_name"));
                        editableFields.put("father_name", fatherNameField);
                        addFieldRow(detailsPanel, "Father's Name:", fatherNameField);
                        
                        // Editable: mother_name
                        JTextField motherNameField = createEditableField(rs.getString("mother_name"));
                        editableFields.put("mother_name", motherNameField);
                        addFieldRow(detailsPanel, "Mother's Name:", motherNameField);
                        
                        // Read-only: date_of_birth
                        addFieldRow(detailsPanel, "Date of Birth:", createReadOnlyField(rs.getString("date_of_birth")));
                        
                        // Read-only: gender
                        addFieldRow(detailsPanel, "Gender:", createReadOnlyField(rs.getString("gender")));
                        
                        // Read-only: marital_status
                        addFieldRow(detailsPanel, "Marital Status:", createReadOnlyField(rs.getString("marital_status")));
                        
                        // Read-only: blood_group
                        addFieldRow(detailsPanel, "Blood Group:", createReadOnlyField(rs.getString("blood_group")));
                        
                        // Editable: phone_number
                        JTextField phoneNumberField = createEditableField(rs.getString("phone_number"));
                        editableFields.put("phone_number", phoneNumberField);
                        addFieldRow(detailsPanel, "Phone Number:", phoneNumberField);
                        
                        // Editable: address
                        JTextField addressField = createEditableField(rs.getString("address"));
                        editableFields.put("address", addressField);
                        addFieldRow(detailsPanel, "Address:", addressField);
                        
                        // Editable: address_detail
                        JTextField addressDetailField = createEditableField(rs.getString("address_detail"));
                        editableFields.put("address_detail", addressDetailField);
                        addFieldRow(detailsPanel, "Address Detail:", addressDetailField);
                        
                        // Editable: birth_place
                        JTextField birthPlaceField = createEditableField(rs.getString("birth_place"));
                        editableFields.put("birth_place", birthPlaceField);
                        addFieldRow(detailsPanel, "Birth Place:", birthPlaceField);
                        
                        // Editable: occupation
                        JTextField occupationField = createEditableField(rs.getString("occupation"));
                        editableFields.put("occupation", occupationField);
                        addFieldRow(detailsPanel, "Occupation:", occupationField);
                        
                        // Editable: kebele_number
                        JTextField kebeleNumberField = createEditableField(rs.getString("kebele_number"));
                        editableFields.put("kebele_number", kebeleNumberField);
                        addFieldRow(detailsPanel, "Kebele Number:", kebeleNumberField);
                        
                        // Read-only: issued_date
                        addFieldRow(detailsPanel, "Issued Date:", createReadOnlyField(rs.getString("issued_date")));
                        
                        // Read-only: created_by
                        addFieldRow(detailsPanel, "Submitted By:", createReadOnlyField(rs.getString("created_by")));
                        
                        // Read-only: created_at
                        addFieldRow(detailsPanel, "Submission Date:", createReadOnlyField(rs.getString("created_at")));
                        
                        JScrollPane scrollPane = new JScrollPane(detailsPanel);
                        scrollPane.setBorder(BorderFactory.createEmptyBorder());
                        panel.add(scrollPane, BorderLayout.CENTER);
                        
                        // Buttons
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        buttonPanel.setOpaque(false);
                        
                        JButton saveBtn = btn("💾 Save", ColorScheme.BTN_PRIMARY, Color.WHITE);
                        saveBtn.addActionListener(e -> {
                            saveApprovalEdits(residentId, editableFields, dialog);
                        });
                        
                        JButton approveBtn = btn("✓ Approve", ColorScheme.BTN_SUCCESS, Color.WHITE);
                        approveBtn.addActionListener(e -> {
                            approveRegistrationById(residentId);
                            dialog.dispose();
                            refreshApprovalTable();
                            refreshTable();
                        });
                        
                        JButton rejectBtn = btn("✗ Reject", ColorScheme.BTN_DANGER, Color.WHITE);
                        rejectBtn.addActionListener(e -> {
                            rejectRegistrationById(residentId);
                            dialog.dispose();
                            refreshApprovalTable();
                        });
                        
                        JButton closeBtn = btn("Close", ColorScheme.BTN_SECONDARY, Color.WHITE);
                        closeBtn.addActionListener(e -> dialog.dispose());
                        
                        buttonPanel.add(saveBtn);
                        buttonPanel.add(approveBtn);
                        buttonPanel.add(rejectBtn);
                        buttonPanel.add(closeBtn);
                        
                        panel.add(buttonPanel, BorderLayout.SOUTH);
                        
                        dialog.add(panel);
                        
                        // Set up keyboard navigation
                        setupKeyboardNavigation(dialog, editableFields, saveBtn, closeBtn);
                        
                        // Set initial focus to first editable field when dialog opens
                        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowOpened(java.awt.event.WindowEvent e) {
                                if (firstNameField != null) {
                                    firstNameField.requestFocusInWindow();
                                }
                            }
                        });
                        
                        dialog.setVisible(true);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to load registration details: " + e.getMessage());
        }
    }

    /**
     * Helper method to add detail rows to the details panel
     */
    private void addDetailRow(JPanel panel, String label, String value) {
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLabel.setForeground(ColorScheme.TEXT_MUTED);
        
        JLabel lblValue = new JLabel(value != null ? value : "N/A");
        lblValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblValue.setForeground(ColorScheme.TEXT_PRIMARY);
        
        panel.add(lblLabel);
        panel.add(lblValue);
    }

    /**
     * Helper method to add field rows with JComponent values to the details panel.
     * Used for adding both editable (JTextField) and read-only (JLabel) fields.
     * 
     * @param panel The panel to add the row to
     * @param label The label text for the field
     * @param component The component (JTextField or JLabel) to display
     */
    private void addFieldRow(JPanel panel, String label, JComponent component) {
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLabel.setForeground(ColorScheme.TEXT_MUTED);
        
        panel.add(lblLabel);
        panel.add(component);
    }

    /**
     * Creates a styled JTextField for editable fields in the approval dialog.
     * Applies ColorScheme styling with white background, primary text color, and border.
     * 
     * @param initialValue The current value from the database
     * @return Configured JTextField with proper styling
     */
    private JTextField createEditableField(String initialValue) {
        JTextField field = new JTextField(initialValue != null ? initialValue : "");
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setForeground(ColorScheme.TEXT_PRIMARY);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.BORDER_LIGHT, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        return field;
    }

    /**
     * Creates a styled JLabel for read-only fields in the approval dialog.
     * Applies ColorScheme styling with gray background, secondary text color, and padding.
     * 
     * @param value The value to display
     * @return Configured JLabel with proper styling
     */
    private JLabel createReadOnlyField(String value) {
        JLabel label = new JLabel(value != null ? value : "N/A");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(ColorScheme.TEXT_SECONDARY);
        label.setBackground(ColorScheme.BG_SIDEBAR);
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(4, 8, 4, 8));
        return label;
    }

    /**
     * Validates all editable fields in the approval dialog before saving.
     * Checks required fields, format constraints, and length limits.
     * 
     * @param editableFields Map of field names to JTextField components
     * @param errorMessage StringBuilder to accumulate error messages
     * @return true if all validations pass, false otherwise
     */
    private boolean validateEditableFields(java.util.Map<String, JTextField> editableFields, StringBuilder errorMessage) {
        // Validation patterns
        java.util.regex.Pattern namePattern = java.util.regex.Pattern.compile("^[a-zA-Z\\s'-]+$");
        java.util.regex.Pattern phonePattern = java.util.regex.Pattern.compile("^(09\\d{8}|\\+251\\d{9})$");
        java.util.regex.Pattern kebeleNumberPattern = java.util.regex.Pattern.compile("^\\d+$");

        // Validate first_name (required, letters/spaces only, max 50 chars)
        JTextField firstNameField = editableFields.get("first_name");
        if (firstNameField != null) {
            String firstName = firstNameField.getText().trim();
            if (firstName.isEmpty()) {
                errorMessage.append("First name is required.\n");
                return false;
            }
            if (!namePattern.matcher(firstName).matches()) {
                errorMessage.append("First name must contain only letters, spaces, hyphens, and apostrophes.\n");
                return false;
            }
            if (firstName.length() > 50) {
                errorMessage.append("First name must not exceed 50 characters.\n");
                return false;
            }
        }

        // Validate last_name (required, letters/spaces only, max 50 chars)
        JTextField lastNameField = editableFields.get("last_name");
        if (lastNameField != null) {
            String lastName = lastNameField.getText().trim();
            if (lastName.isEmpty()) {
                errorMessage.append("Last name is required.\n");
                return false;
            }
            if (!namePattern.matcher(lastName).matches()) {
                errorMessage.append("Last name must contain only letters, spaces, hyphens, and apostrophes.\n");
                return false;
            }
            if (lastName.length() > 50) {
                errorMessage.append("Last name must not exceed 50 characters.\n");
                return false;
            }
        }

        // Validate father_name (required, letters/spaces only, max 50 chars)
        JTextField fatherNameField = editableFields.get("father_name");
        if (fatherNameField != null) {
            String fatherName = fatherNameField.getText().trim();
            if (fatherName.isEmpty()) {
                errorMessage.append("Father's name is required.\n");
                return false;
            }
            if (!namePattern.matcher(fatherName).matches()) {
                errorMessage.append("Father's name must contain only letters, spaces, hyphens, and apostrophes.\n");
                return false;
            }
            if (fatherName.length() > 50) {
                errorMessage.append("Father's name must not exceed 50 characters.\n");
                return false;
            }
        }

        // Validate mother_name (optional, letters/spaces only, max 50 chars)
        JTextField motherNameField = editableFields.get("mother_name");
        if (motherNameField != null) {
            String motherName = motherNameField.getText().trim();
            if (!motherName.isEmpty()) {
                if (!namePattern.matcher(motherName).matches()) {
                    errorMessage.append("Mother's name must contain only letters, spaces, hyphens, and apostrophes.\n");
                    return false;
                }
                if (motherName.length() > 50) {
                    errorMessage.append("Mother's name must not exceed 50 characters.\n");
                    return false;
                }
            }
        }

        // Validate phone_number (optional, Ethiopian format: 09XXXXXXXX or +251XXXXXXXXX)
        JTextField phoneField = editableFields.get("phone_number");
        if (phoneField != null) {
            String phone = phoneField.getText().trim();
            if (!phone.isEmpty()) {
                if (!phonePattern.matcher(phone).matches()) {
                    errorMessage.append("Phone number must be in format 09XXXXXXXX or +251XXXXXXXXX.\n");
                    return false;
                }
            }
        }

        // Validate kebele_number (optional, digits only, max 10 chars)
        JTextField kebeleNumberField = editableFields.get("kebele_number");
        if (kebeleNumberField != null) {
            String kebeleNumber = kebeleNumberField.getText().trim();
            if (!kebeleNumber.isEmpty()) {
                if (!kebeleNumberPattern.matcher(kebeleNumber).matches()) {
                    errorMessage.append("Kebele number must contain only digits.\n");
                    return false;
                }
                if (kebeleNumber.length() > 10) {
                    errorMessage.append("Kebele number must not exceed 10 characters.\n");
                    return false;
                }
            }
        }

        // Validate address (optional, max 100 chars)
        JTextField addressField = editableFields.get("address");
        if (addressField != null) {
            String address = addressField.getText().trim();
            if (address.length() > 100) {
                errorMessage.append("Address must not exceed 100 characters.\n");
                return false;
            }
        }

        // Validate address_detail (optional, max 200 chars)
        JTextField addressDetailField = editableFields.get("address_detail");
        if (addressDetailField != null) {
            String addressDetail = addressDetailField.getText().trim();
            if (addressDetail.length() > 200) {
                errorMessage.append("Address detail must not exceed 200 characters.\n");
                return false;
            }
        }

        // Validate birth_place (optional, max 100 chars)
        JTextField birthPlaceField = editableFields.get("birth_place");
        if (birthPlaceField != null) {
            String birthPlace = birthPlaceField.getText().trim();
            if (birthPlace.length() > 100) {
                errorMessage.append("Birth place must not exceed 100 characters.\n");
                return false;
            }
        }

        // Validate occupation (optional, max 50 chars)
        JTextField occupationField = editableFields.get("occupation");
        if (occupationField != null) {
            String occupation = occupationField.getText().trim();
            if (occupation.length() > 50) {
                errorMessage.append("Occupation must not exceed 50 characters.\n");
                return false;
            }
        }

        // All validations passed
        return true;
    }

    /**
     * Sets up keyboard navigation for the approval dialog.
     * Implements Tab/Shift+Tab navigation, Enter key for Save, and Escape key for Close.
     * 
     * @param dialog The approval dialog
     * @param editableFields Map of editable field components
     * @param saveBtn The save button to trigger on Enter
     * @param closeBtn The close button to trigger on Escape
     */
    private void setupKeyboardNavigation(JDialog dialog, Map<String, JTextField> editableFields, JButton saveBtn, JButton closeBtn) {
        // Set focus traversal policy for Tab/Shift+Tab navigation
        // Java Swing handles Tab/Shift+Tab automatically for focusable components
        // We just need to ensure all JTextField components are focusable (they are by default)
        
        // Add KeyListener to dialog for Escape key to trigger Close button
        dialog.getRootPane().registerKeyboardAction(
            e -> closeBtn.doClick(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Add KeyListener to each editable field for Enter key to trigger Save button
        for (JTextField field : editableFields.values()) {
            // Ensure visible focus indicators (default Swing behavior provides this)
            // The border will show focus by default
            
            // Add Enter key listener to trigger Save button
            field.addActionListener(e -> saveBtn.doClick());
        }
    }

    /**
     * Approves a registration from the table
     */
    private void approveRegistration(int row) {
        if (approvalModel == null || row >= approvalModel.getRowCount()) return;
        
        int residentId = (Integer) approvalModel.getValueAt(row, 0);
        String fullName = (String) approvalModel.getValueAt(row, 2);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Approve registration for " + fullName + "?",
            "Confirm Approval", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            approveRegistrationById(residentId);
            refreshApprovalTable();
            refreshTable();
            refreshStats();
        }
    }

    /**
     * Rejects a registration from the table
     */
    private void rejectRegistration(int row) {
        if (approvalModel == null || row >= approvalModel.getRowCount()) return;
        
        int residentId = (Integer) approvalModel.getValueAt(row, 0);
        String fullName = (String) approvalModel.getValueAt(row, 2);
        
        String reason = JOptionPane.showInputDialog(this,
            "Reject registration for " + fullName + "?\nEnter reason (optional):",
            "Confirm Rejection", JOptionPane.WARNING_MESSAGE);
            
        if (reason != null) { // User clicked OK (even if empty)
            rejectRegistrationById(residentId);
            refreshApprovalTable();
        }
    }

    /**
     * Approves a registration by ID
     */
    /**
     * Saves edits made to a pending registration in the approval dialog.
     * This method validates all editable field inputs, updates the database,
     * logs the activity, and provides user feedback.
     * 
     * @param residentId The ID of the resident record to update
     * @param editableFields Map of field names to JTextField components containing edited values
     * @param dialog The dialog window (kept open after save)
     */
    private void saveApprovalEdits(int residentId, Map<String, JTextField> editableFields, JDialog dialog) {
        // Step 1: Validate all editable fields
        StringBuilder errorMessage = new StringBuilder();
        if (!validateEditableFields(editableFields, errorMessage)) {
            // Display validation error
            warn(errorMessage.toString());
            
            // Highlight the first invalid field with error border and set focus
            for (Map.Entry<String, JTextField> entry : editableFields.entrySet()) {
                JTextField field = entry.getValue();
                String value = field.getText().trim();
                
                // Check which field failed validation based on error message
                if (errorMessage.toString().contains("First name") && entry.getKey().equals("first_name")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Last name") && entry.getKey().equals("last_name")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Father's name") && entry.getKey().equals("father_name")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Mother's name") && entry.getKey().equals("mother_name")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Phone number") && entry.getKey().equals("phone_number")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Kebele number") && entry.getKey().equals("kebele_number")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Address detail") && entry.getKey().equals("address_detail")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Address") && entry.getKey().equals("address")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Birth place") && entry.getKey().equals("birth_place")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                } else if (errorMessage.toString().contains("Occupation") && entry.getKey().equals("occupation")) {
                    field.setBorder(BorderFactory.createLineBorder(ColorScheme.ERROR, 2));
                    field.requestFocusInWindow();
                    break;
                }
            }
            return;
        }
        
        // Step 2: Build UPDATE SQL statement for editable fields only
        String sql = "UPDATE residents SET " +
                     "first_name = ?, " +
                     "last_name = ?, " +
                     "father_name = ?, " +
                     "mother_name = ?, " +
                     "phone_number = ?, " +
                     "address = ?, " +
                     "address_detail = ?, " +
                     "birth_place = ?, " +
                     "occupation = ?, " +
                     "kebele_number = ? " +
                     "WHERE id = ? AND approval_status = 'Pending'";
        
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            // Step 3: Extract values from JTextField components and set parameters
            ps.setString(1, editableFields.get("first_name").getText().trim());
            ps.setString(2, editableFields.get("last_name").getText().trim());
            ps.setString(3, editableFields.get("father_name").getText().trim());
            ps.setString(4, editableFields.get("mother_name").getText().trim());
            ps.setString(5, editableFields.get("phone_number").getText().trim());
            ps.setString(6, editableFields.get("address").getText().trim());
            ps.setString(7, editableFields.get("address_detail").getText().trim());
            ps.setString(8, editableFields.get("birth_place").getText().trim());
            ps.setString(9, editableFields.get("occupation").getText().trim());
            ps.setString(10, editableFields.get("kebele_number").getText().trim());
            ps.setInt(11, residentId);
            
            // Step 4: Execute update and check return value for concurrent modification detection
            int rowsUpdated = ps.executeUpdate();
            
            if (rowsUpdated == 0) {
                // No rows were updated - either record doesn't exist or status changed
                // Query to determine the cause
                String checkSql = "SELECT approval_status FROM residents WHERE id = ?";
                try (PreparedStatement checkPs = DatabaseManager.getConnection().prepareStatement(checkSql)) {
                    checkPs.setInt(1, residentId);
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next()) {
                            // Record exists but status changed
                            error("Registration has already been processed by another user.");
                        } else {
                            // Record not found
                            error("Registration not found.");
                        }
                    }
                }
                return;
            }
            
            // Step 5: Log activity to activity_log table
            StringBuilder modifiedFields = new StringBuilder();
            for (String fieldName : editableFields.keySet()) {
                if (modifiedFields.length() > 0) {
                    modifiedFields.append(", ");
                }
                modifiedFields.append(fieldName);
            }
            
            String details = "Edited resident ID: " + residentId + ", Fields: " + modifiedFields.toString();
            DatabaseManager.logActivity(
                currentUser.getId(),
                currentUser.getUsername(),
                "EDIT_PENDING_REGISTRATION",
                details
            );
            
            // Step 6: Display success message
            info("Registration details saved successfully.");
            
            // Reset field borders to normal after successful save
            for (JTextField field : editableFields.values()) {
                field.setBorder(BorderFactory.createLineBorder(ColorScheme.BORDER_LIGHT, 1));
            }
            
        } catch (SQLException e) {
            // Display database error message
            error("Failed to save registration details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void approveRegistrationById(int residentId) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date());
            
            String sql = "UPDATE residents SET approval_status = 'Approved', " +
                        "approved_by = ?, approved_at = ? WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, currentUser.getUsername());
                ps.setString(2, timestamp);
                ps.setInt(3, residentId);
                int updated = ps.executeUpdate();
                
                if (updated > 0) {
                    DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                        "APPROVE_REGISTRATION", "Approved resident ID: " + residentId);
                    info("Registration approved successfully!");
                } else {
                    warn("Registration not found or already processed.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to approve registration: " + e.getMessage());
        }
    }

    /**
     * Rejects a registration by ID
     */
    private void rejectRegistrationById(int residentId) {
        try {
            String sql = "UPDATE residents SET approval_status = 'Rejected' WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, residentId);
                int updated = ps.executeUpdate();
                
                if (updated > 0) {
                    DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                        "REJECT_REGISTRATION", "Rejected resident ID: " + residentId);
                    info("Registration rejected.");
                } else {
                    warn("Registration not found or already processed.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to reject registration: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    //  FEEDBACK MANAGEMENT METHODS (Admin/Supervisor)
    // ════════════════════════════════════════════════════════
    
    /**
     * Refreshes the feedback table with all feedback from the database
    // ════════════════════════════════════════════════════════
    //  FEEDBACK DATABASE METHODS (Enhanced with Phase 1 Features)
    // ════════════════════════════════════════════════════════
    
    /**
     * Saves feedback to the new feedback table
     */
    private void saveFeedback(String category, String priority, String feedback) {
        try {
            String sql = "INSERT INTO feedback (user_id, username, full_name, role, category, priority, status, feedback) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'New', ?)";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, currentUser.getId());
                ps.setString(2, currentUser.getUsername());
                ps.setString(3, currentUser.getFullName());
                ps.setString(4, currentUser.getRole());
                ps.setString(5, category);
                ps.setString(6, priority);
                ps.setString(7, feedback);
                ps.executeUpdate();
                
                // Log activity
                DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                    "FEEDBACK_SUBMIT", "Category: " + category + ", Priority: " + priority);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to save feedback: " + e.getMessage());
        }
    }
    
    /**
     * Refreshes the feedback table with all feedback from the database
     */
    private void refreshFeedbackTable() {
        if (feedbackModel == null) return;
        
        feedbackModel.setRowCount(0); // Clear existing data
        
        try {
            String sql = "SELECT id, user_id, username, full_name, role, category, priority, status, feedback, created_at " +
                        "FROM feedback " +
                        "ORDER BY created_at DESC";
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String role = rs.getString("role");
                    String category = rs.getString("category");
                    String priority = rs.getString("priority");
                    String status = rs.getString("status");
                    String feedback = rs.getString("feedback");
                    String createdAt = rs.getString("created_at");
                    
                    // Format timestamp
                    String formattedDate = createdAt != null ? createdAt.substring(0, 16).replace("T", " ") : "";
                    
                    feedbackModel.addRow(new Object[]{
                        id,
                        username,
                        role,
                        category,
                        priority,
                        status,
                        feedback,
                        formattedDate,
                        "Actions"
                    });
                }
            }
            
            // Update statistics
            updateFeedbackStats();
            
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to load feedback: " + e.getMessage());
        }
    }
    
    /**
     * Filters the feedback table based on search and filter criteria
     */
    private void filterFeedbackTable() {
        if (feedbackModel == null) return;
        
        String searchText = feedbackSearchField.getText().toLowerCase().trim();
        String statusFilter = (String) feedbackStatusFilter.getSelectedItem();
        String priorityFilter = (String) feedbackPriorityFilter.getSelectedItem();
        
        feedbackModel.setRowCount(0);
        
        try {
            StringBuilder sql = new StringBuilder(
                "SELECT id, user_id, username, full_name, role, category, priority, status, feedback, created_at " +
                "FROM feedback WHERE 1=1");
            
            // Add status filter
            if (!"All Status".equals(statusFilter)) {
                sql.append(" AND status = '").append(statusFilter).append("'");
            }
            
            // Add priority filter
            if (!"All Priority".equals(priorityFilter)) {
                sql.append(" AND priority = '").append(priorityFilter).append("'");
            }
            
            sql.append(" ORDER BY created_at DESC");
            
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String role = rs.getString("role");
                    String category = rs.getString("category");
                    String priority = rs.getString("priority");
                    String status = rs.getString("status");
                    String feedback = rs.getString("feedback");
                    String createdAt = rs.getString("created_at");
                    
                    // Apply search filter
                    if (!searchText.isEmpty()) {
                        String searchableText = (username + " " + role + " " + category + " " + feedback).toLowerCase();
                        if (!searchableText.contains(searchText)) {
                            continue;
                        }
                    }
                    
                    String formattedDate = createdAt != null ? createdAt.substring(0, 16).replace("T", " ") : "";
                    
                    feedbackModel.addRow(new Object[]{
                        id,
                        username,
                        role,
                        category,
                        priority,
                        status,
                        feedback,
                        formattedDate,
                        "Actions"
                    });
                }
            }
            
            // Update statistics
            updateFeedbackStats();
            
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to filter feedback: " + e.getMessage());
        }
    }
    
    /**
     * Updates feedback statistics dashboard
     */
    private void updateFeedbackStats() {
        try {
            // Total feedback
            String sqlTotal = "SELECT COUNT(*) FROM feedback";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sqlTotal);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && feedbackStatTotal != null) {
                    feedbackStatTotal.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // New feedback
            String sqlNew = "SELECT COUNT(*) FROM feedback WHERE status = 'New'";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sqlNew);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && feedbackStatNew != null) {
                    feedbackStatNew.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // In Progress feedback
            String sqlInProgress = "SELECT COUNT(*) FROM feedback WHERE status = 'In Progress'";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sqlInProgress);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && feedbackStatInProgress != null) {
                    feedbackStatInProgress.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // Resolved feedback
            String sqlResolved = "SELECT COUNT(*) FROM feedback WHERE status = 'Resolved'";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sqlResolved);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && feedbackStatResolved != null) {
                    feedbackStatResolved.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // Critical priority feedback
            String sqlCritical = "SELECT COUNT(*) FROM feedback WHERE priority = 'Critical' AND status != 'Closed'";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sqlCritical);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && feedbackStatCritical != null) {
                    feedbackStatCritical.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Shows detailed view of selected feedback
     */
    private void viewFeedbackDetails(int row) {
        if (feedbackModel == null || row >= feedbackModel.getRowCount()) return;
        
        int feedbackId = (Integer) feedbackModel.getValueAt(row, 0);
        
        try {
            String sql = "SELECT * FROM feedback WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, feedbackId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        showFeedbackDetailsDialog(rs);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to load feedback details: " + e.getMessage());
        }
    }
    
    /**
     * Shows feedback details dialog
     */
    private void showFeedbackDetailsDialog(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        String role = rs.getString("role");
        String category = rs.getString("category");
        String priority = rs.getString("priority");
        String status = rs.getString("status");
        String feedback = rs.getString("feedback");
        String response = rs.getString("response");
        String respondedBy = rs.getString("responded_by");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");
        
        // Create detailed view dialog
        JDialog dialog = new JDialog(this, "Feedback Details #" + id, true);
        dialog.setSize(600, 550);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(ColorScheme.BG_CARD);
        
        // Header with info
        JPanel headerPanel = new JPanel(new GridLayout(6, 2, 10, 8));
        headerPanel.setOpaque(false);
        
        headerPanel.add(createInfoLabel("Feedback ID:", String.valueOf(id)));
        headerPanel.add(createInfoLabel("Submitted by:", fullName + " (" + username + ")"));
        headerPanel.add(createInfoLabel("User Role:", role));
        headerPanel.add(createInfoLabel("Category:", category));
        headerPanel.add(createInfoLabel("Priority:", priority));
        headerPanel.add(createInfoLabel("Status:", status));
        headerPanel.add(createInfoLabel("Created:", createdAt != null ? createdAt.substring(0, 16).replace("T", " ") : ""));
        headerPanel.add(createInfoLabel("Updated:", updatedAt != null ? updatedAt.substring(0, 16).replace("T", " ") : ""));
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Feedback content
        JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
        contentPanel.setOpaque(false);
        
        JLabel feedbackLabel = new JLabel("Feedback Content:");
        feedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        feedbackLabel.setForeground(ColorScheme.TEXT_PRIMARY);
        
        JTextArea feedbackArea = new JTextArea(feedback);
        feedbackArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        feedbackArea.setForeground(ColorScheme.TEXT_PRIMARY);
        feedbackArea.setBackground(ColorScheme.BG_SIDEBAR);
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackScroll.setBorder(new LineBorder(ColorScheme.BORDER_LIGHT, 1));
        feedbackScroll.setPreferredSize(new Dimension(0, 120));
        
        contentPanel.add(feedbackLabel, BorderLayout.NORTH);
        contentPanel.add(feedbackScroll, BorderLayout.CENTER);
        
        // Response section (if exists)
        if (response != null && !response.isEmpty()) {
            JPanel responsePanel = new JPanel(new BorderLayout(0, 8));
            responsePanel.setOpaque(false);
            
            JLabel responseLabel = new JLabel("Admin Response: (by " + respondedBy + ")");
            responseLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            responseLabel.setForeground(ColorScheme.SUCCESS);
            
            JTextArea responseArea = new JTextArea(response);
            responseArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            responseArea.setForeground(ColorScheme.TEXT_PRIMARY);
            responseArea.setBackground(ColorScheme.SUCCESS_LIGHT);
            responseArea.setEditable(false);
            responseArea.setLineWrap(true);
            responseArea.setWrapStyleWord(true);
            responseArea.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JScrollPane responseScroll = new JScrollPane(responseArea);
            responseScroll.setBorder(new LineBorder(ColorScheme.SUCCESS, 1));
            responseScroll.setPreferredSize(new Dimension(0, 80));
            
            responsePanel.add(responseLabel, BorderLayout.NORTH);
            responsePanel.add(responseScroll, BorderLayout.CENTER);
            
            contentPanel.add(responsePanel, BorderLayout.SOUTH);
        }
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton closeBtn = btn("Close", ColorScheme.BTN_SECONDARY, Color.WHITE);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JButton addResponseBtn = btn("Add/Edit Response", ColorScheme.ROLE_SUPERVISOR, Color.WHITE);
        addResponseBtn.setVisible(currentUser.isAdmin() || currentUser.isSupervisor());
        addResponseBtn.addActionListener(e -> {
            dialog.dispose();
            addFeedbackResponse(id);
        });
        
        JButton updateStatusBtn = btn("Update Status", ColorScheme.BTN_SUCCESS, Color.WHITE);
        updateStatusBtn.addActionListener(e -> {
            dialog.dispose();
            updateFeedbackStatusById(id);
        });
        
        JButton deleteBtn = btn("Delete", ColorScheme.BTN_DANGER, Color.WHITE);
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "Are you sure you want to delete this feedback?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                deleteFeedbackById(id);
                dialog.dispose();
                refreshFeedbackTable();
            }
        });
        
        buttonPanel.add(addResponseBtn);
        buttonPanel.add(updateStatusBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(closeBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    /**
     * Updates feedback status
     */
    private void updateFeedbackStatus(int row) {
        if (feedbackModel == null || row >= feedbackModel.getRowCount()) return;
        
        int feedbackId = (Integer) feedbackModel.getValueAt(row, 0);
        updateFeedbackStatusById(feedbackId);
    }
    
    /**
     * Adds or edits admin response to feedback
     */
    private void addFeedbackResponse(int feedbackId) {
        // Get current response if exists
        String currentResponse = "";
        try {
            String sql = "SELECT response FROM feedback WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, feedbackId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentResponse = rs.getString("response");
                        if (currentResponse == null) currentResponse = "";
                    }
                }
            }
        } catch (SQLException e) {
            error("Error loading response: " + e.getMessage());
            return;
        }
        
        // Show dialog to enter/edit response
        JTextArea responseArea = new JTextArea(currentResponse, 8, 40);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        responseArea.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1),
                new EmptyBorder(10, 10, 10, 10)));
        
        JScrollPane scrollPane = new JScrollPane(responseArea);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("Enter your response to this feedback:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(this,
                panel,
                "Add/Edit Response",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String response = responseArea.getText().trim();
            
            if (response.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Response is empty. Do you want to remove the existing response?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            try {
                String sql = "UPDATE feedback SET response = ?, responded_by = ?, " +
                            "responded_at = datetime('now'), updated_at = datetime('now') " +
                            "WHERE id = ?";
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                    ps.setString(1, response.isEmpty() ? null : response);
                    ps.setString(2, currentUser.getFullName());
                    ps.setInt(3, feedbackId);
                    
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                                "FEEDBACK_RESPONSE", "Added/edited response to feedback ID: " + feedbackId);
                        
                        JOptionPane.showMessageDialog(this,
                                "Response saved successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        
                        refreshFeedbackTable();
                    } else {
                        error("Failed to save response");
                    }
                }
            } catch (SQLException e) {
                error("Error saving response: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Updates feedback status by ID
     */
    private void updateFeedbackStatusById(int feedbackId) {
        String[] statuses = {"New", "In Progress", "Resolved", "Closed"};
        String newStatus = (String) JOptionPane.showInputDialog(
            this,
            "Select new status:",
            "Update Feedback Status",
            JOptionPane.QUESTION_MESSAGE,
            null,
            statuses,
            statuses[0]
        );
        
        if (newStatus != null) {
            try {
                String sql = "UPDATE feedback SET status = ?, updated_at = datetime('now') WHERE id = ?";
                try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, feedbackId);
                    int updated = ps.executeUpdate();
                    
                    if (updated > 0) {
                        DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                            "FEEDBACK_STATUS_UPDATE", "Feedback ID: " + feedbackId + " -> " + newStatus);
                        info("Feedback status updated to: " + newStatus);
                        refreshFeedbackTable();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error("Failed to update feedback status: " + e.getMessage());
            }
        }
    }
    
    /**
     * Deletes feedback from the table
     */
    private void deleteFeedback(int row) {
        if (feedbackModel == null || row >= feedbackModel.getRowCount()) return;
        
        int feedbackId = (Integer) feedbackModel.getValueAt(row, 0);
        String username = (String) feedbackModel.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete feedback from " + username + "?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            deleteFeedbackById(feedbackId);
            refreshFeedbackTable();
        }
    }
    
    /**
     * Deletes feedback by ID from database
     */
    private void deleteFeedbackById(int feedbackId) {
        try {
            String sql = "DELETE FROM feedback WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, feedbackId);
                int deleted = ps.executeUpdate();
                
                if (deleted > 0) {
                    DatabaseManager.logActivity(currentUser.getId(), currentUser.getUsername(),
                        "DELETE_FEEDBACK", "Deleted feedback ID: " + feedbackId);
                    info("Feedback deleted successfully.");
                } else {
                    warn("Feedback not found or already deleted.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Failed to delete feedback: " + e.getMessage());
        }
    }
    
    /**
     * Creates a formatted info label for the details dialog
     */
    private JPanel createInfoLabel(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLabel.setForeground(ColorScheme.TEXT_MUTED);
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblValue.setForeground(ColorScheme.TEXT_PRIMARY);
        
        panel.add(lblLabel, BorderLayout.WEST);
        panel.add(lblValue, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates a stat card for feedback statistics
     */
    private JLabel createStatCard(String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(ColorScheme.BG_CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(color, 2, true),
            new EmptyBorder(12, 16, 12, 16)
        ));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(color);
        lblValue.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLabel.setForeground(ColorScheme.TEXT_MUTED);
        lblLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(lblValue, BorderLayout.CENTER);
        card.add(lblLabel, BorderLayout.SOUTH);
        
        // Return the value label so we can update it
        return lblValue;
    }

    // ════════════════════════════════════════════════════════
    //  APPROVAL BUTTON RENDERER - Shows buttons in table cells
    // ════════════════════════════════════════════════════════
    class ApprovalButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton viewEditBtn;
        private JButton approveBtn;
        private JButton rejectBtn;
        
        public ApprovalButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 3, 2));
            setOpaque(true);
            
            viewEditBtn = new JButton("View/Edit");
            viewEditBtn.setFont(F_SMALL);
            viewEditBtn.setPreferredSize(new Dimension(75, 28));
            viewEditBtn.setBackground(ColorScheme.BTN_PRIMARY);
            viewEditBtn.setForeground(Color.WHITE);
            viewEditBtn.setFocusPainted(false);
            viewEditBtn.setBorderPainted(false);
            viewEditBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            approveBtn = new JButton("Approve");
            approveBtn.setFont(F_SMALL);
            approveBtn.setPreferredSize(new Dimension(70, 28));
            approveBtn.setBackground(ColorScheme.BTN_SUCCESS);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.setFocusPainted(false);
            approveBtn.setBorderPainted(false);
            approveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            rejectBtn = new JButton("Reject");
            rejectBtn.setFont(F_SMALL);
            rejectBtn.setPreferredSize(new Dimension(65, 28));
            rejectBtn.setBackground(ColorScheme.BTN_DANGER);
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setFocusPainted(false);
            rejectBtn.setBorderPainted(false);
            rejectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            add(viewEditBtn);
            add(approveBtn);
            add(rejectBtn);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : 
                         (row % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR));
            return this;
        }
    }

    // ════════════════════════════════════════════════════════
    //  APPROVAL BUTTON EDITOR - Makes buttons clickable in table
    // ════════════════════════════════════════════════════════
    class ApprovalButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton viewEditBtn;
        private JButton approveBtn;
        private JButton rejectBtn;
        private JTable table;
        private int currentRow;
        
        public ApprovalButtonEditor(JTable table) {
            this.table = table;
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 2));
            panel.setOpaque(true);
            
            viewEditBtn = new JButton("View/Edit");
            viewEditBtn.setFont(F_SMALL);
            viewEditBtn.setPreferredSize(new Dimension(75, 28));
            viewEditBtn.setBackground(ColorScheme.BTN_PRIMARY);
            viewEditBtn.setForeground(Color.WHITE);
            viewEditBtn.setFocusPainted(false);
            viewEditBtn.setBorderPainted(false);
            viewEditBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            approveBtn = new JButton("Approve");
            approveBtn.setFont(F_SMALL);
            approveBtn.setPreferredSize(new Dimension(70, 28));
            approveBtn.setBackground(ColorScheme.BTN_SUCCESS);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.setFocusPainted(false);
            approveBtn.setBorderPainted(false);
            approveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            rejectBtn = new JButton("Reject");
            rejectBtn.setFont(F_SMALL);
            rejectBtn.setPreferredSize(new Dimension(65, 28));
            rejectBtn.setBackground(ColorScheme.BTN_DANGER);
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setFocusPainted(false);
            rejectBtn.setBorderPainted(false);
            rejectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // View/Edit button action
            viewEditBtn.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> viewApprovalDetails(currentRow));
            });
            
            // Approve button action
            approveBtn.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> approveRegistration(currentRow));
            });
            
            // Reject button action
            rejectBtn.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> rejectRegistration(currentRow));
            });
            
            panel.add(viewEditBtn);
            panel.add(approveBtn);
            panel.add(rejectBtn);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.currentRow = row;
            panel.setBackground(isSelected ? table.getSelectionBackground() : 
                               (row % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR));
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Actions";
        }
    }

    // ════════════════════════════════════════════════════════
    //  USER APPROVAL BUTTON RENDERER - Shows buttons for user approvals
    // ════════════════════════════════════════════════════════
    class UserApprovalButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton viewEditBtn;
        private JButton approveBtn;
        private JButton rejectBtn;
        
        public UserApprovalButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 3, 2));
            setOpaque(true);
            
            viewEditBtn = new JButton("View/Edit");
            viewEditBtn.setFont(F_SMALL);
            viewEditBtn.setPreferredSize(new Dimension(75, 28));
            viewEditBtn.setBackground(ColorScheme.BTN_PRIMARY);
            viewEditBtn.setForeground(Color.WHITE);
            viewEditBtn.setFocusPainted(false);
            viewEditBtn.setBorderPainted(false);
            viewEditBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            approveBtn = new JButton("Approve");
            approveBtn.setFont(F_SMALL);
            approveBtn.setPreferredSize(new Dimension(70, 28));
            approveBtn.setBackground(ColorScheme.BTN_SUCCESS);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.setFocusPainted(false);
            approveBtn.setBorderPainted(false);
            approveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            rejectBtn = new JButton("Reject");
            rejectBtn.setFont(F_SMALL);
            rejectBtn.setPreferredSize(new Dimension(65, 28));
            rejectBtn.setBackground(ColorScheme.BTN_DANGER);
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setFocusPainted(false);
            rejectBtn.setBorderPainted(false);
            rejectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            add(viewEditBtn);
            add(approveBtn);
            add(rejectBtn);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : 
                         (row % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR));
            return this;
        }
    }

    // ════════════════════════════════════════════════════════
    //  USER APPROVAL BUTTON EDITOR - Makes user approval buttons clickable
    // ════════════════════════════════════════════════════════
    class UserApprovalButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton viewEditBtn;
        private JButton approveBtn;
        private JButton rejectBtn;
        private JTable table;
        private int currentRow;
        
        public UserApprovalButtonEditor(JTable table) {
            this.table = table;
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 2));
            panel.setOpaque(true);
            
            viewEditBtn = new JButton("View/Edit");
            viewEditBtn.setFont(F_SMALL);
            viewEditBtn.setPreferredSize(new Dimension(75, 28));
            viewEditBtn.setBackground(ColorScheme.BTN_PRIMARY);
            viewEditBtn.setForeground(Color.WHITE);
            viewEditBtn.setFocusPainted(false);
            viewEditBtn.setBorderPainted(false);
            viewEditBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            approveBtn = new JButton("Approve");
            approveBtn.setFont(F_SMALL);
            approveBtn.setPreferredSize(new Dimension(70, 28));
            approveBtn.setBackground(ColorScheme.BTN_SUCCESS);
            approveBtn.setForeground(Color.WHITE);
            approveBtn.setFocusPainted(false);
            approveBtn.setBorderPainted(false);
            approveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            rejectBtn = new JButton("Reject");
            rejectBtn.setFont(F_SMALL);
            rejectBtn.setPreferredSize(new Dimension(65, 28));
            rejectBtn.setBackground(ColorScheme.BTN_DANGER);
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setFocusPainted(false);
            rejectBtn.setBorderPainted(false);
            rejectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // View/Edit button action
            viewEditBtn.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> viewPendingUserDetails(currentRow));
            });
            
            // Approve button action
            approveBtn.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> approveUser(currentRow));
            });
            
            // Reject button action
            rejectBtn.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> rejectUser(currentRow));
            });
            
            panel.add(viewEditBtn);
            panel.add(approveBtn);
            panel.add(rejectBtn);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.currentRow = row;
            panel.setBackground(isSelected ? table.getSelectionBackground() : 
                               (row % 2 == 0 ? ColorScheme.BG_CARD : ColorScheme.BG_SIDEBAR));
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Actions";
        }
    }
}