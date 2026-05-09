package ui;

import db.DatabaseManager;
import model.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.Font;
import java.awt.Image;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Renders a professional Ethiopian Kebele ID card with front and back sides.
 * Includes photo loading, QR code generation, and PDF export functionality.
 */
public class IDCardPanel extends JPanel {

    // Professional Ethiopian ID Card Colors
    private static final Color CARD_BG = new Color(15, 40, 80);           // Dark blue background
    private static final Color CARD_GOLD = new Color(245, 166, 35);       // Ethiopian gold
    private static final Color FIELD_LABEL = new Color(180, 200, 220);    // Light blue gray
    private static final Color FIELD_VALUE = Color.WHITE;                 // White text
    private static final Color ETH_GREEN = new Color(0, 122, 51);         // Ethiopian flag green
    private static final Color ETH_YELLOW = new Color(252, 209, 22);      // Ethiopian flag yellow
    private static final Color ETH_RED = new Color(218, 41, 28);          // Ethiopian flag red

    // Resident data - comprehensive fields
    private final String firstName;
    private final String lastName;
    private final String fatherName;
    private final String motherName;
    private final String kebeleId;
    private final String gender;
    private final String birthDate;
    private final String address;
    private final String addressDetail;
    private final String phoneNumber;
    private final String bloodGroup;
    private final String status;
    private final String photoPath;
    private final String issuedDate;
    private final String kebeleNumber;
    private final String nationality = "Ethiopian";
    private final int residentId;
    
    // Current logged-in user for issuing officer
    private static User currentUser;

    // Card dimensions - larger for more information
    private static final int CW = 650;
    private static final int CH = 400;

    public IDCardPanel(ResultSet rs) throws SQLException {
        firstName = rs.getString("first_name");
        lastName = rs.getString("last_name");
        fatherName = rs.getString("father_name");
        motherName = rs.getString("mother_name");
        kebeleId = rs.getString("kebele_id");
        gender = rs.getString("gender");
        birthDate = rs.getString("date_of_birth");
        address = rs.getString("address") != null ? rs.getString("address") : "";
        addressDetail = rs.getString("address_detail") != null ? rs.getString("address_detail") : "";
        phoneNumber = rs.getString("phone_number") != null ? rs.getString("phone_number") : "";
        bloodGroup = rs.getString("blood_group") != null ? rs.getString("blood_group") : "N/A";
        status = rs.getString("id_status");
        photoPath = rs.getString("photo_path");
        issuedDate = rs.getString("issued_date") != null ? rs.getString("issued_date") : 
                     (rs.getString("created_at") != null ? rs.getString("created_at").substring(0, 10) : "");
        kebeleNumber = rs.getString("kebele_number") != null ? rs.getString("kebele_number") : "";
        residentId = rs.getInt("id");

        setPreferredSize(new Dimension(CW, CH));
        setOpaque(false);
    }
    
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawCardFront(g2, 0, 0, CW, CH);
    }

    public void drawCardFront(Graphics2D g2, int ox, int oy, int w, int h) {
        // ── Card Background with Gold Border ──────────────────
        g2.setColor(CARD_BG);
        g2.fillRoundRect(ox, oy, w, h, 20, 20);
        
        // Gold border (2px)
        g2.setColor(CARD_GOLD);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(ox + 1, oy + 1, w - 2, h - 2, 20, 20);

        // ── Header Section ──────────────────────────────────────
        // Gold header background
        g2.setColor(CARD_GOLD);
        g2.fillRoundRect(ox, oy, w, 60, 20, 20);
        g2.fillRect(ox, oy + 40, w, 20);

        // Ethiopian Coat of Arms placeholder (left side of header)
        g2.setColor(CARD_BG);
        g2.fillOval(ox + 15, oy + 10, 40, 40);
        g2.setFont(new Font("Serif", Font.BOLD, 20));
        g2.drawString("🦁", ox + 22, oy + 38);

        // Main header text
        g2.setFont(new Font("Serif", Font.BOLD, 14));
        String header = "FEDERAL DEMOCRATIC REPUBLIC OF ETHIOPIA";
        int headerWidth = g2.getFontMetrics().stringWidth(header);
        g2.drawString(header, ox + (w - headerWidth) / 2, oy + 25);

        // Sub-header
        g2.setFont(new Font("Serif", Font.BOLD, 12));
        String subHeader = "KEBELE RESIDENT IDENTIFICATION CARD";
        int subHeaderWidth = g2.getFontMetrics().stringWidth(subHeader);
        g2.drawString(subHeader, ox + (w - subHeaderWidth) / 2, oy + 48);

        // ── Photo Section (Left Side) ──────────────────────────
        int photoX = ox + 20;
        int photoY = oy + 75;
        int photoW = 100;
        int photoH = 120;
        
        // Photo border
        g2.setColor(CARD_GOLD);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(photoX - 2, photoY - 2, photoW + 4, photoH + 4);
        
        // Load and draw photo
        Image photo = loadPhotoFromPath(photoPath);
        if (photo != null) {
            g2.drawImage(photo, photoX, photoY, photoW, photoH, null);
        } else {
            // Default silhouette placeholder
            drawSilhouettePlaceholder(g2, photoX, photoY, photoW, photoH);
        }

        // ── Information Fields (Right Side) ────────────────────
        int fieldX = ox + 140;
        int fieldY = oy + 85;
        int lineHeight = 22;

        drawInfoField(g2, "Full Name:", firstName + " " + lastName, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Father Name:", fatherName, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Kebele ID:", kebeleId, fieldX, fieldY, true); // Bold
        fieldY += lineHeight;
        
        drawInfoField(g2, "Gender:", gender, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Date of Birth:", birthDate, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Nationality:", nationality, fieldX, fieldY);
        fieldY += lineHeight;
        
        String kebeleInfo = "Kebele " + kebeleNumber;
        drawInfoField(g2, "Kebele:", kebeleInfo, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Issue Date:", issuedDate, fieldX, fieldY);
        fieldY += lineHeight;
        
        // Calculate expiry date (issue date + 5 years)
        String expiryDate = calculateExpiryDate(issuedDate);
        drawInfoField(g2, "Expiry Date:", expiryDate, fieldX, fieldY);

        // ── Status Badge ────────────────────────────────────────
        int badgeX = fieldX;
        int badgeY = oy + h - 100;
        Color badgeColor = "Active".equalsIgnoreCase(status) ? 
                          new Color(0, 150, 0) : new Color(200, 0, 0);
        
        g2.setColor(badgeColor);
        g2.fillRoundRect(badgeX, badgeY, 90, 28, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        String statusText = status != null ? status.toUpperCase() : "UNKNOWN";
        int statusWidth = g2.getFontMetrics().stringWidth(statusText);
        g2.drawString(statusText, badgeX + (90 - statusWidth) / 2, badgeY + 19);

        // ── Barcode (Right Side) ────────────────────────────────
        int barcodeX = ox + w - 130;
        int barcodeY = oy + 75;
        drawBarcode(g2, kebeleId != null ? kebeleId : "UNKNOWN", barcodeX, barcodeY, 110, 70);
        
        // Barcode label
        g2.setColor(FIELD_VALUE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
        String barcodeText = kebeleId != null ? kebeleId : "";
        int barcodeTextWidth = g2.getFontMetrics().stringWidth(barcodeText);
        g2.drawString(barcodeText, barcodeX + (110 - barcodeTextWidth) / 2, barcodeY + 85);

        // ── Ethiopian Flag Strip (Bottom) ───────────────────────
        int flagY = oy + h - 30;
        int flagHeight = 20;
        int stripeWidth = w / 3;
        
        g2.setColor(ETH_GREEN);
        g2.fillRect(ox, flagY, stripeWidth, flagHeight);
        
        g2.setColor(ETH_YELLOW);
        g2.fillRect(ox + stripeWidth, flagY, stripeWidth, flagHeight);
        
        g2.setColor(ETH_RED);
        g2.fillRect(ox + stripeWidth * 2, flagY, w - (stripeWidth * 2), flagHeight);
    }

    public void drawCardBack(Graphics2D g2, int ox, int oy, int w, int h) {
        // ── Card Background with Gold Border ──────────────────
        g2.setColor(CARD_BG);
        g2.fillRoundRect(ox, oy, w, h, 20, 20);
        
        // Gold border (2px)
        g2.setColor(CARD_GOLD);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(ox + 1, oy + 1, w - 2, h - 2, 20, 20);

        // ── Header ──────────────────────────────────────────────
        g2.setColor(CARD_GOLD);
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        String header = "ADDITIONAL INFORMATION";
        int headerWidth = g2.getFontMetrics().stringWidth(header);
        g2.drawString(header, ox + (w - headerWidth) / 2, oy + 35);

        // ── Information Fields ──────────────────────────────────
        int fieldX = ox + 30;
        int fieldY = oy + 70;
        int lineHeight = 28;

        drawInfoField(g2, "Full Address:", getFullAddress(), fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Phone Number:", phoneNumber.isEmpty() ? "N/A" : phoneNumber, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Blood Type:", bloodGroup, fieldX, fieldY);
        fieldY += lineHeight;
        
        drawInfoField(g2, "Emergency Contact:", "N/A", fieldX, fieldY);
        fieldY += lineHeight;
        
        String issuingOfficer = currentUser != null ? currentUser.getUsername() : "System";
        drawInfoField(g2, "Issuing Officer:", issuingOfficer, fieldX, fieldY);

        // ── QR Code (Right Side) ────────────────────────────────
        int qrSize = 120;
        int qrX = ox + w - qrSize - 40;
        int qrY = oy + 60;
        
        BufferedImage qrCode = generateQRCodeWithFullInfo(qrSize);
        if (qrCode != null) {
            g2.drawImage(qrCode, qrX, qrY, qrSize, qrSize, null);
        }
        
        // QR Code label
        g2.setColor(FIELD_LABEL);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        String qrLabel = "Scan for verification";
        int qrLabelWidth = g2.getFontMetrics().stringWidth(qrLabel);
        g2.drawString(qrLabel, qrX + (qrSize - qrLabelWidth) / 2, qrY + qrSize + 15);

        // ── Kebele Office Stamp Placeholder ─────────────────────
        int stampX = ox + w - 140;
        int stampY = oy + h - 140;
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillOval(stampX, stampY, 100, 100);
        g2.setColor(CARD_GOLD);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(stampX, stampY, 100, 100);
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.drawString("OFFICIAL", stampX + 25, stampY + 45);
        g2.drawString("STAMP", stampX + 30, stampY + 60);

        // ── Signature Line ──────────────────────────────────────
        int sigX = ox + 30;
        int sigY = oy + h - 80;
        g2.setColor(FIELD_LABEL);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(sigX, sigY, sigX + 200, sigY);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.drawString("Cardholder Signature", sigX + 40, sigY + 15);

        // ── Footer Notice ───────────────────────────────────────
        g2.setColor(FIELD_LABEL);
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        String notice = "This card is property of the Federal Democratic Republic of Ethiopia.";
        int noticeWidth = g2.getFontMetrics().stringWidth(notice);
        g2.drawString(notice, ox + (w - noticeWidth) / 2, oy + h - 35);
        
        String notice2 = "If found please return to the nearest Kebele office.";
        int notice2Width = g2.getFontMetrics().stringWidth(notice2);
        g2.drawString(notice2, ox + (w - notice2Width) / 2, oy + h - 20);
    }

    private void drawInfoField(Graphics2D g2, String label, String value, int x, int y) {
        drawInfoField(g2, label, value, x, y, false);
    }

    private void drawInfoField(Graphics2D g2, String label, String value, int x, int y, boolean boldValue) {
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(FIELD_LABEL);
        g2.drawString(label, x, y);

        g2.setFont(new Font("Arial", boldValue ? Font.BOLD : Font.PLAIN, 11));
        g2.setColor(FIELD_VALUE);
        String displayValue = value != null && !value.isEmpty() ? value : "-";
        g2.drawString(displayValue, x + 110, y);
    }

    private Image loadPhotoFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        try {
            // Check if it's an embedded resource
            if (path.startsWith("resource:")) {
                String resourceName = path.substring(9);
                InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    return img.getScaledInstance(100, 120, Image.SCALE_SMOOTH);
                }
            } else {
                // Load from file system
                File file = new File(path);
                if (file.exists()) {
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        return img.getScaledInstance(100, 120, Image.SCALE_SMOOTH);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading photo: " + e.getMessage());
        }
        return null;
    }

    private void drawSilhouettePlaceholder(Graphics2D g2, int x, int y, int w, int h) {
        // Draw a simple silhouette placeholder
        g2.setColor(new Color(100, 100, 100));
        g2.fillRect(x, y, w, h);
        
        g2.setColor(new Color(150, 150, 150));
        // Head
        int headSize = w / 3;
        g2.fillOval(x + w/2 - headSize/2, y + h/4 - headSize/2, headSize, headSize);
        
        // Body
        int bodyWidth = w * 2 / 3;
        int bodyHeight = h / 2;
        g2.fillRoundRect(x + w/2 - bodyWidth/2, y + h/2, bodyWidth, bodyHeight, 20, 20);
        
        g2.setColor(FIELD_LABEL);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.drawString("No Photo", x + w/2 - 25, y + h - 10);
    }

    private String getFullAddress() {
        StringBuilder fullAddr = new StringBuilder();
        if (!address.isEmpty()) {
            fullAddr.append(address);
        }
        if (!addressDetail.isEmpty()) {
            if (fullAddr.length() > 0) fullAddr.append(", ");
            fullAddr.append(addressDetail);
        }
        if (fullAddr.length() == 0) {
            return "N/A";
        }
        // Shorten if too long
        String addr = fullAddr.toString();
        return addr.length() > 50 ? addr.substring(0, 47) + "..." : addr;
    }

    private String calculateExpiryDate(String issueDate) {
        if (issueDate == null || issueDate.isEmpty()) {
            return "N/A";
        }
        try {
            LocalDate issue = LocalDate.parse(issueDate);
            LocalDate expiry = issue.plusYears(5);
            return expiry.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private BufferedImage generateQRCode(String data, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a QR code containing comprehensive resident information in JSON format.
     * When scanned by a phone, it will display all resident details.
     */
    private BufferedImage generateQRCodeWithFullInfo(int size) {
        try {
            // Build JSON-like structured data with all resident information
            StringBuilder data = new StringBuilder();
            data.append("{\n");
            data.append("  \"kebeleId\": \"").append(kebeleId != null ? kebeleId : "").append("\",\n");
            data.append("  \"fullName\": \"").append(firstName).append(" ").append(lastName).append("\",\n");
            data.append("  \"firstName\": \"").append(firstName != null ? firstName : "").append("\",\n");
            data.append("  \"lastName\": \"").append(lastName != null ? lastName : "").append("\",\n");
            data.append("  \"fatherName\": \"").append(fatherName != null ? fatherName : "").append("\",\n");
            data.append("  \"motherName\": \"").append(motherName != null ? motherName : "").append("\",\n");
            data.append("  \"gender\": \"").append(gender != null ? gender : "").append("\",\n");
            data.append("  \"dateOfBirth\": \"").append(birthDate != null ? birthDate : "").append("\",\n");
            data.append("  \"nationality\": \"").append(nationality).append("\",\n");
            data.append("  \"kebeleNumber\": \"").append(kebeleNumber != null ? kebeleNumber : "").append("\",\n");
            data.append("  \"address\": \"").append(getFullAddress()).append("\",\n");
            data.append("  \"phoneNumber\": \"").append(phoneNumber != null ? phoneNumber : "N/A").append("\",\n");
            data.append("  \"bloodGroup\": \"").append(bloodGroup != null ? bloodGroup : "N/A").append("\",\n");
            data.append("  \"status\": \"").append(status != null ? status : "").append("\",\n");
            data.append("  \"issuedDate\": \"").append(issuedDate != null ? issuedDate : "").append("\",\n");
            data.append("  \"expiryDate\": \"").append(calculateExpiryDate(issuedDate)).append("\",\n");
            data.append("  \"issuingOfficer\": \"").append(currentUser != null ? currentUser.getUsername() : "System").append("\",\n");
            data.append("  \"country\": \"Ethiopia\",\n");
            data.append("  \"documentType\": \"Kebele Resident ID Card\"\n");
            data.append("}");
            
            String jsonData = data.toString();
            
            // Debug output to console
            System.out.println("=== QR CODE GENERATION ===");
            System.out.println("Generating QR with FULL INFO for: " + firstName + " " + lastName);
            System.out.println("Data length: " + jsonData.length() + " characters");
            System.out.println("First 100 chars: " + jsonData.substring(0, Math.min(100, jsonData.length())));
            System.out.println("==========================");
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(jsonData, BarcodeFormat.QR_CODE, size, size);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (WriterException e) {
            System.err.println("Error generating QR code with full info: " + e.getMessage());
            e.printStackTrace();
            // Fallback to simple kebele ID if JSON encoding fails
            return generateQRCode(kebeleId, size);
        }
    }

    /**
     * Draws a barcode pattern derived from the kebele ID string.
     */
    private void drawBarcode(Graphics2D g2, String data, int x, int y, int w, int h) {
        // Background
        g2.setColor(Color.WHITE);
        g2.fillRect(x, y, w, h);

        // Generate bar widths from character codes
        byte[] bytes = data.getBytes();
        java.util.List<Integer> bars = new java.util.ArrayList<>();
        // Start guard
        bars.add(2);
        bars.add(1);
        bars.add(2);
        bars.add(1);
        bars.add(2);
        for (byte b : bytes) {
            int code = b & 0xFF;
            for (int bit = 7; bit >= 0; bit--)
                bars.add(((code >> bit) & 1) == 1 ? 2 : 1);
            bars.add(1); // inter-char gap
        }
        // End guard
        bars.add(2);
        bars.add(1);
        bars.add(2);
        bars.add(1);
        bars.add(2);

        int totalUnits = bars.stream().mapToInt(Integer::intValue).sum();
        double unitW = (double) w / totalUnits;
        double cx = x;
        boolean black = true;

        for (int bw : bars) {
            double barW = bw * unitW;
            if (black) {
                g2.setColor(Color.BLACK);
                g2.fill(new Rectangle2D.Double(cx, y + 2, barW - 0.5, h - 4));
            }
            cx += barW;
            black = !black;
        }
    }

    // ── Static factory: show dialog with ID card (front and back) ─────────────
    public static void showIDCard(Component parent, int residentId) {
        try (PreparedStatement ps = DatabaseManager.getConnection()
                .prepareStatement("SELECT * FROM residents WHERE id=?")) {
            ps.setInt(1, residentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(parent,
                            "Resident not found.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                IDCardPanel cardFront = new IDCardPanel(rs);
                IDCardPanel cardBack = new IDCardPanel(rs); // Same data, different rendering

                JDialog dlg = new JDialog(
                        SwingUtilities.getWindowAncestor(parent),
                        "ID Card — " + rs.getString("first_name") + " " + rs.getString("last_name"),
                        java.awt.Dialog.ModalityType.APPLICATION_MODAL);
                dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                // Main panel with card toggle
                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBackground(new Color(0xF2F5F9));
                mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

                // Card display panel (will switch between front and back)
                JPanel cardDisplayPanel = new JPanel(new BorderLayout());
                cardDisplayPanel.setOpaque(false);
                
                // Create custom panels for front and back
                JPanel frontPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        cardFront.drawCardFront(g2, 0, 0, CW, CH);
                    }
                };
                frontPanel.setPreferredSize(new Dimension(CW, CH));
                frontPanel.setOpaque(false);
                
                JPanel backPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        cardFront.drawCardBack(g2, 0, 0, CW, CH);
                    }
                };
                backPanel.setPreferredSize(new Dimension(CW, CH));
                backPanel.setOpaque(false);

                // Start with front side
                cardDisplayPanel.add(frontPanel, BorderLayout.CENTER);

                mainPanel.add(cardDisplayPanel, BorderLayout.CENTER);

                // Buttons panel
                JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
                btnRow.setOpaque(false);

                // Flip card button
                JButton btnFlip = new JButton("🔄 Flip Card");
                btnFlip.setBackground(new Color(0x7B1FA2));
                btnFlip.setForeground(Color.WHITE);
                btnFlip.setBorderPainted(false);
                btnFlip.setFocusPainted(false);
                btnFlip.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnFlip.setBorder(new EmptyBorder(10, 20, 10, 20));
                
                final boolean[] showingFront = {true};
                btnFlip.addActionListener(e -> {
                    cardDisplayPanel.removeAll();
                    if (showingFront[0]) {
                        cardDisplayPanel.add(backPanel, BorderLayout.CENTER);
                        btnFlip.setText("🔄 Show Front");
                    } else {
                        cardDisplayPanel.add(frontPanel, BorderLayout.CENTER);
                        btnFlip.setText("🔄 Show Back");
                    }
                    showingFront[0] = !showingFront[0];
                    cardDisplayPanel.revalidate();
                    cardDisplayPanel.repaint();
                });

                // Save as PDF button
                JButton btnPDF = new JButton("📄 Save as PDF");
                btnPDF.setBackground(new Color(0xD32F2F));
                btnPDF.setForeground(Color.WHITE);
                btnPDF.setBorderPainted(false);
                btnPDF.setFocusPainted(false);
                btnPDF.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnPDF.setBorder(new EmptyBorder(10, 20, 10, 20));
                btnPDF.addActionListener(e -> saveCardAsPDF(cardFront, dlg));

                // Save as Image button
                JButton btnImage = new JButton("🖼️ Save as Image");
                btnImage.setBackground(new Color(0x2D9E6B));
                btnImage.setForeground(Color.WHITE);
                btnImage.setBorderPainted(false);
                btnImage.setFocusPainted(false);
                btnImage.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnImage.setBorder(new EmptyBorder(10, 20, 10, 20));
                btnImage.addActionListener(e -> saveCardAsImage(cardFront, dlg));

                // Print button
                JButton btnPrint = new JButton("🖨️ Print ID Card");
                btnPrint.setBackground(new Color(0x0D3B66));
                btnPrint.setForeground(Color.WHITE);
                btnPrint.setBorderPainted(false);
                btnPrint.setFocusPainted(false);
                btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnPrint.setBorder(new EmptyBorder(10, 20, 10, 20));
                btnPrint.addActionListener(e -> printCard(cardFront, dlg));

                // Close button
                JButton btnClose = new JButton("Close");
                btnClose.setBackground(new Color(0x78909C));
                btnClose.setForeground(Color.WHITE);
                btnClose.setBorderPainted(false);
                btnClose.setFocusPainted(false);
                btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnClose.setBorder(new EmptyBorder(10, 20, 10, 20));
                btnClose.addActionListener(e -> dlg.dispose());

                btnRow.add(btnFlip);
                btnRow.add(btnPDF);
                btnRow.add(btnImage);
                btnRow.add(btnPrint);
                btnRow.add(btnClose);
                mainPanel.add(btnRow, BorderLayout.SOUTH);

                dlg.setContentPane(mainPanel);
                dlg.pack();
                dlg.setLocationRelativeTo(parent);
                dlg.setVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent,
                    "Error loading resident: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveCardAsPDF(IDCardPanel card, JDialog parent) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("KebeleID_" + card.kebeleId + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Document", "pdf"));

        if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".pdf"))
                f = new File(f.getAbsolutePath() + ".pdf");

            try {
                // Create PDF document (A4 size)
                Document document = new Document(PageSize.A4);
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(f));
                document.open();

                // Add title
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                Paragraph title = new Paragraph("Ethiopian Kebele ID Card", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Render front side to image
                BufferedImage frontImg = new BufferedImage(CW, CH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2Front = frontImg.createGraphics();
                g2Front.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2Front.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2Front.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2Front.setColor(Color.WHITE);
                g2Front.fillRect(0, 0, CW, CH);
                card.drawCardFront(g2Front, 0, 0, CW, CH);
                g2Front.dispose();

                // Add front side to PDF
                com.itextpdf.text.Image frontImage = com.itextpdf.text.Image.getInstance(frontImg, null);
                frontImage.scaleToFit(500, 400);
                frontImage.setAlignment(Element.ALIGN_CENTER);
                document.add(frontImage);

                document.add(new Paragraph(" ")); // Spacer

                // Add "FRONT SIDE" label
                com.itextpdf.text.Font labelFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
                Paragraph frontLabel = new Paragraph("FRONT SIDE", labelFont);
                frontLabel.setAlignment(Element.ALIGN_CENTER);
                frontLabel.setSpacingAfter(30);
                document.add(frontLabel);

                // Render back side to image
                BufferedImage backImg = new BufferedImage(CW, CH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2Back = backImg.createGraphics();
                g2Back.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2Back.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2Back.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2Back.setColor(Color.WHITE);
                g2Back.fillRect(0, 0, CW, CH);
                card.drawCardBack(g2Back, 0, 0, CW, CH);
                g2Back.dispose();

                // Add back side to PDF
                com.itextpdf.text.Image backImage = com.itextpdf.text.Image.getInstance(backImg, null);
                backImage.scaleToFit(500, 400);
                backImage.setAlignment(Element.ALIGN_CENTER);
                document.add(backImage);

                // Add "BACK SIDE" label
                Paragraph backLabel = new Paragraph("BACK SIDE", labelFont);
                backLabel.setAlignment(Element.ALIGN_CENTER);
                document.add(backLabel);

                document.close();
                writer.close();

                JOptionPane.showMessageDialog(parent,
                        "ID card PDF saved to:\n" + f.getAbsolutePath(),
                        "Saved", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,
                        "PDF save failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private static void printCard(IDCardPanel card, JDialog parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Kebele ID Card");
        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable((g, pageFormat, pageIndex) -> {
            if (pageIndex > 1)
                return Printable.NO_SUCH_PAGE;
                
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            
            double scale = Math.min(
                    pageFormat.getImageableWidth() / (double) CW,
                    pageFormat.getImageableHeight() / (double) (CH * 2 + 40));
            g2.scale(scale, scale);
            
            if (pageIndex == 0) {
                // Print front side
                card.drawCardFront(g2, 0, 0, CW, CH);
                
                // Print back side below
                card.drawCardBack(g2, 0, CH + 40, CW, CH);
            }
            
            return Printable.PAGE_EXISTS;
        }, pf);

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(parent,
                        "ID card sent to printer successfully!",
                        "Print", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,
                        "Print failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveCardAsImage(IDCardPanel card, JDialog parent) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("KebeleID_" + card.kebeleId + ".png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image", "png"));

        if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".png"))
                f = new File(f.getAbsolutePath() + ".png");

            // Create image with both front and back side by side
            int totalWidth = CW * 2 + 40; // Both cards plus gap
            int totalHeight = CH + 40; // Height plus margins
            
            BufferedImage img = new BufferedImage(totalWidth * 2, totalHeight * 2,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // White background
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, totalWidth * 2, totalHeight * 2);
            
            g2.scale(2, 2);
            
            // Draw front side
            card.drawCardFront(g2, 20, 20, CW, CH);
            
            // Draw back side
            card.drawCardBack(g2, CW + 40, 20, CW, CH);
            
            g2.dispose();

            try {
                ImageIO.write(img, "PNG", f);
                JOptionPane.showMessageDialog(parent,
                        "ID card image saved to:\n" + f.getAbsolutePath(),
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,
                        "Save failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}