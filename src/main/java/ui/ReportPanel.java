package ui;

import db.DatabaseManager;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class ReportPanel extends JPanel {

    // ── Professional Color Scheme (using ColorScheme class) ──────────────
    // Updated to match standardized color palette
    private static final Color PRIMARY = ColorScheme.PRIMARY;           // #1B6CA8
    private static final Color BG = ColorScheme.BG_MAIN;                // #F3F4F6
    private static final Color CARD = ColorScheme.BG_CARD;              // White
    private static final Color SUCCESS = ColorScheme.SUCCESS;           // #27AE60
    private static final Color DANGER = ColorScheme.ERROR;              // #E74C3C
    private static final Color WARN = ColorScheme.WARNING;              // #F5A623
    private static final Color TXT = ColorScheme.TEXT_PRIMARY;          // #111827
    private static final Color TXT_MUTED = ColorScheme.TEXT_MUTED;      // #9CA3AF
    
    // ── Standardized Typography (Professional) ──────────────────────────
    private static final Font F_BOLD = new Font("Segoe UI", Font.BOLD, 13);   // 13px
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 11); // 11px
    private static final Font F_TITLE = new Font("Segoe UI", Font.BOLD, 18);  // 18px (increased for consistency)

    // Chart colors - Professional palette matching ColorScheme
    private static final Color[] PALETTE = {
            ColorScheme.PRIMARY,              // Professional Blue
            ColorScheme.SUCCESS,              // Success Green
            ColorScheme.WARNING,              // Ethiopian Gold
            ColorScheme.ERROR,                // Error Red
            ColorScheme.ROLE_DATA_ENCODER,    // Purple
            ColorScheme.INFO,                 // Teal
            ColorScheme.ROLE_SUPERVISOR,      // Orange
            ColorScheme.ROLE_STAFF,           // Emerald
            ColorScheme.ROLE_ADMIN,           // Deep Red
            ColorScheme.ROLE_VIEWER           // Cyan
    };

    public ReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));
        refresh();
    }

    public void refresh() {
        removeAll();

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Reports & Analytics Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28)); // 28px for consistency
        title.setForeground(PRIMARY);

        JButton btnRefresh = smallBtn("🔄 Refresh", ColorScheme.BTN_SUCCESS);
        btnRefresh.addActionListener(e -> refresh());
        addHoverEffect(btnRefresh, ColorScheme.BTN_SUCCESS, ColorScheme.BTN_SUCCESS_HOVER);
        
        JButton btnPrint = smallBtn("🖨️ Print Report", ColorScheme.ROLE_DATA_ENCODER);
        btnPrint.addActionListener(e -> printReport());
        addHoverEffect(btnPrint, ColorScheme.ROLE_DATA_ENCODER, ColorScheme.darker(ColorScheme.ROLE_DATA_ENCODER, 0.2f));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnRefresh);
        btnRow.add(btnPrint);

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(btnRow, BorderLayout.EAST);
        add(titleRow, BorderLayout.NORTH);

        // Summary cards row
        JPanel summaryRow = new JPanel(new GridLayout(1, 4, 12, 0));
        summaryRow.setOpaque(false);
        summaryRow.setBorder(new EmptyBorder(10, 0, 10, 0));

        int total = count("1=1");
        int active = count("id_status='Active'");
        int male = count("gender='Male'");
        int female = count("gender='Female'");

        summaryRow.add(summaryCard("Total Residents", total, ColorScheme.INFO));
        summaryRow.add(summaryCard("Active", active, SUCCESS));
        summaryRow.add(summaryCard("Male", male, ColorScheme.ROLE_DATA_ENCODER));
        summaryRow.add(summaryCard("Female", female, WARN));

        // Charts grid — 2x2
        JPanel charts = new JPanel(new GridLayout(2, 2, 14, 14));
        charts.setOpaque(false);

        charts.add(wrapChart("Residents by Kebele", buildBarChart(getKebeleData())));
        charts.add(wrapChart("Residents by Gender", buildPieChart(getGenderData())));
        charts.add(wrapChart("Residents by Status", buildPieChart(getStatusData())));
        charts.add(wrapChart("Registrations Over Time", buildLineChart(getMonthlyData())));

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(summaryRow, BorderLayout.NORTH);
        center.add(charts, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ── SUMMARY CARD ─────────────────────────────────────────
    private JPanel summaryCard(String label, int value, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setBackground(color);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel valLbl = new JLabel(String.valueOf(value), SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valLbl.setForeground(Color.WHITE);

        JLabel lblLbl = new JLabel(label, SwingConstants.CENTER);
        lblLbl.setFont(F_SMALL);
        lblLbl.setForeground(new Color(255, 255, 255, 200));

        p.add(valLbl);
        p.add(lblLbl);
        return p;
    }

    // ── CHART WRAPPER ────────────────────────────────────────
    private JPanel wrapChart(String title, JPanel chart) {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setBackground(CARD);
        wrap.setBorder(new CompoundBorder(
                new LineBorder(ColorScheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(14, 14, 14, 14)));

        JLabel t = new JLabel(title);
        t.setFont(F_TITLE);
        t.setForeground(PRIMARY);
        t.setBorder(new EmptyBorder(0, 0, 8, 0));

        wrap.add(t, BorderLayout.NORTH);
        wrap.add(chart, BorderLayout.CENTER);
        return wrap;
    }

    // ── BAR CHART ────────────────────────────────────────────
    private JPanel buildBarChart(Map<String, Integer> data) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (data.isEmpty()) {
                    drawEmpty(g);
                    return;
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int padL = 50, padB = 40, padT = 10, padR = 10;
                int chartW = w - padL - padR;
                int chartH = h - padB - padT;

                int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

                // Grid lines
                g2.setColor(new Color(0xEEEEEE));
                for (int i = 0; i <= 4; i++) {
                    int y = padT + chartH - (i * chartH / 4);
                    g2.drawLine(padL, y, w - padR, y);
                    g2.setColor(TXT_MUTED);
                    g2.setFont(F_SMALL);
                    g2.drawString(String.valueOf(max * i / 4), 4, y + 4);
                    g2.setColor(new Color(0xEEEEEE));
                }

                // Bars
                List<String> keys = new ArrayList<>(data.keySet());
                int barW = Math.max(10, chartW / keys.size() - 8);
                int gap = chartW / keys.size();

                for (int i = 0; i < keys.size(); i++) {
                    int val = data.get(keys.get(i));
                    int barH = max == 0 ? 0 : (int) ((double) val / max * chartH);
                    int x = padL + i * gap + (gap - barW) / 2;
                    int y = padT + chartH - barH;
                    Color col = PALETTE[i % PALETTE.length];

                    // Shadow
                    g2.setColor(new Color(0, 0, 0, 20));
                    g2.fillRoundRect(x + 3, y + 3, barW, barH, 6, 6);

                    // Bar
                    g2.setColor(col);
                    g2.fillRoundRect(x, y, barW, barH, 6, 6);

                    // Value on top
                    g2.setColor(TXT);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    String sv = String.valueOf(val);
                    int sw = g2.getFontMetrics().stringWidth(sv);
                    g2.drawString(sv, x + (barW - sw) / 2, y - 3);

                    // Label below
                    g2.setColor(TXT_MUTED);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    String lbl = keys.get(i);
                    if (lbl.length() > 7)
                        lbl = lbl.substring(0, 6) + ".";
                    int lw = g2.getFontMetrics().stringWidth(lbl);
                    g2.drawString(lbl, x + (barW - lw) / 2,
                            padT + chartH + 14);
                }

                // Axes
                g2.setColor(new Color(0xCCCCCC));
                g2.drawLine(padL, padT, padL, padT + chartH);
                g2.drawLine(padL, padT + chartH, w - padR, padT + chartH);
            }
        };
    }

    // ── PIE CHART ────────────────────────────────────────────
    private JPanel buildPieChart(Map<String, Integer> data) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (data.isEmpty()) {
                    drawEmpty(g);
                    return;
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int legendW = 120;
                int size = Math.min(w - legendW - 20, h - 20);
                int x = 10, y = (h - size) / 2;

                int total = data.values().stream().mapToInt(Integer::intValue).sum();
                if (total == 0) {
                    drawEmpty(g);
                    return;
                }

                double start = -90;
                int i = 0;
                List<String> keys = new ArrayList<>(data.keySet());

                for (String key : keys) {
                    int val = data.get(key);
                    double arc = 360.0 * val / total;
                    Color col = PALETTE[i % PALETTE.length];

                    // Shadow
                    g2.setColor(new Color(0, 0, 0, 18));
                    g2.fill(new Arc2D.Double(x + 3, y + 3, size, size,
                            start, arc, Arc2D.PIE));

                    // Slice
                    g2.setColor(col);
                    g2.fill(new Arc2D.Double(x, y, size, size,
                            start, arc, Arc2D.PIE));

                    // White border
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2));
                    g2.draw(new Arc2D.Double(x, y, size, size,
                            start, arc, Arc2D.PIE));

                    // Percentage label on slice
                    double mid = Math.toRadians(start + arc / 2);
                    int cx = (int) (x + size / 2 + Math.cos(mid) * size * 0.32);
                    int cy = (int) (y + size / 2 - Math.sin(mid) * size * 0.32);
                    int pct = (int) Math.round(100.0 * val / total);
                    if (pct >= 5) {
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        String ps = pct + "%";
                        int sw = g2.getFontMetrics().stringWidth(ps);
                        g2.drawString(ps, cx - sw / 2, cy + 4);
                    }

                    start += arc;
                    i++;
                }

                // Legend
                int lx = x + size + 16, ly = y + 10;
                for (int j = 0; j < keys.size(); j++) {
                    g2.setColor(PALETTE[j % PALETTE.length]);
                    g2.fillRoundRect(lx, ly + j * 22, 14, 14, 4, 4);
                    g2.setColor(TXT);
                    g2.setFont(F_SMALL);
                    g2.drawString(keys.get(j) + " (" + data.get(keys.get(j)) + ")",
                            lx + 18, ly + j * 22 + 11);
                }
            }
        };
    }

    // ── LINE CHART ───────────────────────────────────────────
    private JPanel buildLineChart(Map<String, Integer> data) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (data.isEmpty()) {
                    drawEmpty(g);
                    return;
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int padL = 50, padB = 40, padT = 20, padR = 20;
                int chartW = w - padL - padR;
                int chartH = h - padB - padT;

                List<String> keys = new ArrayList<>(data.keySet());
                if (keys.isEmpty()) {
                    drawEmpty(g);
                    return;
                }

                int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

                // Grid
                g2.setColor(new Color(0xEEEEEE));
                for (int i = 0; i <= 4; i++) {
                    int y2 = padT + chartH - (i * chartH / 4);
                    g2.drawLine(padL, y2, w - padR, y2);
                    g2.setColor(TXT_MUTED);
                    g2.setFont(F_SMALL);
                    g2.drawString(String.valueOf(max * i / 4), 4, y2 + 4);
                    g2.setColor(new Color(0xEEEEEE));
                }

                // Fill under line
                int step = keys.size() > 1 ? chartW / (keys.size() - 1) : chartW;
                int[] xs = new int[keys.size()];
                int[] ys = new int[keys.size()];

                for (int i = 0; i < keys.size(); i++) {
                    xs[i] = padL + i * step;
                    int val = data.get(keys.get(i));
                    ys[i] = padT + chartH - (max == 0 ? 0 : (int) ((double) val / max * chartH));
                }

                // Area fill
                int[] fillX = new int[keys.size() + 2];
                int[] fillY = new int[keys.size() + 2];
                fillX[0] = xs[0];
                fillY[0] = padT + chartH;
                for (int i = 0; i < keys.size(); i++) {
                    fillX[i + 1] = xs[i];
                    fillY[i + 1] = ys[i];
                }
                fillX[keys.size() + 1] = xs[keys.size() - 1];
                fillY[keys.size() + 1] = padT + chartH;

                g2.setColor(ColorScheme.withAlpha(ColorScheme.PRIMARY, 255));
                GradientPaint gp = new GradientPaint(0, padT,
                        ColorScheme.withAlpha(ColorScheme.PRIMARY, 80), 0, padT + chartH,
                        ColorScheme.withAlpha(ColorScheme.PRIMARY, 10));
                g2.setPaint(gp);
                g2.fillPolygon(fillX, fillY, fillX.length);

                // Line
                g2.setColor(ColorScheme.PRIMARY);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                for (int i = 0; i < keys.size() - 1; i++)
                    g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);

                // Dots + labels
                for (int i = 0; i < keys.size(); i++) {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
                    g2.setColor(ColorScheme.PRIMARY);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawOval(xs[i] - 5, ys[i] - 5, 10, 10);

                    // Value
                    g2.setColor(TXT);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    String sv = String.valueOf(data.get(keys.get(i)));
                    g2.drawString(sv, xs[i] - 4, ys[i] - 8);

                    // X label
                    g2.setColor(TXT_MUTED);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    String lbl = keys.get(i);
                    if (lbl.length() > 7)
                        lbl = lbl.substring(lbl.length() - 5);
                    int lw = g2.getFontMetrics().stringWidth(lbl);
                    g2.drawString(lbl, xs[i] - lw / 2, padT + chartH + 14);
                }

                // Axes
                g2.setColor(new Color(0xCCCCCC));
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(padL, padT, padL, padT + chartH);
                g2.drawLine(padL, padT + chartH, w - padR, padT + chartH);
            }
        };
    }

    private void drawEmpty(Graphics g) {
        g.setColor(TXT_MUTED);
        g.setFont(F_SMALL);
        g.drawString("No data available", 20, getHeight() / 2);
    }

    // ── DATA QUERIES ─────────────────────────────────────────
    private Map<String, Integer> getKebeleData() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Statement s = DatabaseManager.getConnection().createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT kebele_id, COUNT(*) as cnt FROM residents "
                                + "GROUP BY kebele_id ORDER BY cnt DESC")) {
            while (rs.next())
                m.put(rs.getString("kebele_id"), rs.getInt("cnt"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private Map<String, Integer> getGenderData() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Statement s = DatabaseManager.getConnection().createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT gender, COUNT(*) as cnt FROM residents GROUP BY gender")) {
            while (rs.next())
                m.put(rs.getString("gender"), rs.getInt("cnt"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private Map<String, Integer> getStatusData() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Statement s = DatabaseManager.getConnection().createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT id_status, COUNT(*) as cnt FROM residents GROUP BY id_status")) {
            while (rs.next())
                m.put(rs.getString("id_status"), rs.getInt("cnt"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private Map<String, Integer> getMonthlyData() {
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Statement s = DatabaseManager.getConnection().createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT strftime('%Y-%m', created_at) as month, COUNT(*) as cnt "
                                + "FROM residents GROUP BY month ORDER BY month ASC LIMIT 12")) {
            while (rs.next()) {
                String mo = rs.getString("month");
                if (mo != null)
                    m.put(mo, rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    private int count(String where) {
        try (Statement s = DatabaseManager.getConnection().createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT COUNT(*) FROM residents WHERE " + where)) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ── PRINT ────────────────────────────────────────────────
    private void printReport() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Kebele ID System Report");
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0)
                return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scaleX = pageFormat.getImageableWidth() / getWidth();
            double scaleY = pageFormat.getImageableHeight() / getHeight();
            double scale = Math.min(scaleX, scaleY);
            g2.scale(scale, scale);
            paintAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Print failed: " + e.getMessage());
            }
        }
    }

    private JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14)); // 14px for consistency
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 18, 10, 18)); // Slightly larger padding
        return b;
    }
    
    /**
     * Adds hover effect to buttons for professional interaction feedback.
     */
    private void addHoverEffect(JButton btn, Color normalColor, Color hoverColor) {
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(normalColor);
            }
        });
    }
}