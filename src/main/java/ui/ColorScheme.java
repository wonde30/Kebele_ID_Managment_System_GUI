package ui;

import java.awt.Color;

/**
 * Professional color scheme for Kebele ID Management System.
 * Based on modern government and enterprise application standards.
 */
public class ColorScheme {
    
    // ═══════════════════════════════════════════════════════════════
    //  PRIMARY BRAND COLORS (Ethiopian Government Inspired)
    // ═══════════════════════════════════════════════════════════════
    
    /** Primary brand color - Professional Blue */
    public static final Color PRIMARY = new Color(0x1B6CA8);        // Professional Blue
    
    /** Secondary brand color - Professional Blue (same as primary) */
    public static final Color SECONDARY = new Color(0x1B6CA8);      // Professional Blue
    
    /** Accent color - Ethiopian Gold */
    public static final Color ACCENT = new Color(0xF5A623);         // Ethiopian Gold
    
    // ═══════════════════════════════════════════════════════════════
    //  BACKGROUND COLORS (Modern & Clean)
    // ═══════════════════════════════════════════════════════════════
    
    /** Main background - Light gray */
    public static final Color BG_MAIN = new Color(0xF3F4F6);        // Gray 100
    
    /** Card background - Pure white */
    public static final Color BG_CARD = Color.WHITE;                // White
    
    /** Header background - Gradient start */
    public static final Color BG_HEADER_START = new Color(0x1A2035); // Dark panel
    
    /** Header background - Gradient end */
    public static final Color BG_HEADER_END = new Color(0x0F1623);   // Darker panel
    
    /** Sidebar background */
    public static final Color BG_SIDEBAR = new Color(0xF9FAFB);     // Gray 50
    
    /** Dark panel background */
    public static final Color BG_DARK_PANEL = new Color(0x1A2035);  // Background dark panel
    
    // ═══════════════════════════════════════════════════════════════
    //  STATUS COLORS (Professional & Accessible)
    // ═══════════════════════════════════════════════════════════════
    
    /** Success color - Professional green */
    public static final Color SUCCESS = new Color(0x27AE60);        // Success green
    
    /** Success light - For backgrounds */
    public static final Color SUCCESS_LIGHT = new Color(0xD5F4E6);  // Light green
    
    /** Warning color - Professional amber */
    public static final Color WARNING = new Color(0xF5A623);        // Ethiopian Gold (same as accent)
    
    /** Warning light - For backgrounds */
    public static final Color WARNING_LIGHT = new Color(0xFEF3C7);  // Amber 100
    
    /** Error color - Professional red */
    public static final Color ERROR = new Color(0xE74C3C);          // Error red
    
    /** Error light - For backgrounds */
    public static final Color ERROR_LIGHT = new Color(0xFEE2E2);    // Red 100
    
    /** Info color - Teal accent */
    public static final Color INFO = new Color(0x0891B2);           // Cyan 600
    
    /** Info light - For backgrounds */
    public static final Color INFO_LIGHT = new Color(0xCFFAFE);     // Cyan 100
    
    // ═══════════════════════════════════════════════════════════════
    //  TEXT COLORS (High Contrast & Readable)
    // ═══════════════════════════════════════════════════════════════
    
    /** Primary text - Dark gray */
    public static final Color TEXT_PRIMARY = new Color(0x111827);   // Gray 900
    
    /** Secondary text - Medium gray */
    public static final Color TEXT_SECONDARY = new Color(0x6B7280); // Gray 500
    
    /** Muted text - Light gray */
    public static final Color TEXT_MUTED = new Color(0x9CA3AF);     // Gray 400
    
    /** Disabled text - Very light gray */
    public static final Color TEXT_DISABLED = new Color(0xD1D5DB);  // Gray 300
    
    /** Text on dark backgrounds */
    public static final Color TEXT_ON_DARK = Color.WHITE;
    
    // ═══════════════════════════════════════════════════════════════
    //  BORDER COLORS (Subtle & Professional)
    // ═══════════════════════════════════════════════════════════════
    
    /** Light border - For cards and inputs */
    public static final Color BORDER_LIGHT = new Color(0xE5E7EB);   // Gray 200
    
    /** Medium border - For emphasis */
    public static final Color BORDER_MEDIUM = new Color(0xD1D5DB);  // Gray 300
    
    /** Dark border - For strong emphasis */
    public static final Color BORDER_DARK = new Color(0x9CA3AF);    // Gray 400
    
    /** Focus border - Blue accent */
    public static final Color BORDER_FOCUS = new Color(0x1B6CA8);   // Professional Blue
    
    // ═══════════════════════════════════════════════════════════════
    //  ROLE-BASED COLORS (Professional Hierarchy)
    // ═══════════════════════════════════════════════════════════════
    
    /** Admin role - Deep red (authority) */
    public static final Color ROLE_ADMIN = new Color(0xDC2626);     // Red 600
    
    /** Supervisor role - Orange (management) */
    public static final Color ROLE_SUPERVISOR = new Color(0xEA580C); // Orange 600
    
    /** Staff role - Green (operational) */
    public static final Color ROLE_STAFF = new Color(0x059669);     // Emerald 600
    
    /** Data Encoder role - Purple (specialist) */
    public static final Color ROLE_DATA_ENCODER = new Color(0x7C3AED); // Violet 600
    
    /** Viewer role - Teal (read-only) */
    public static final Color ROLE_VIEWER = new Color(0x0891B2);    // Cyan 600
    
    // ═══════════════════════════════════════════════════════════════
    //  INTERACTIVE COLORS (Buttons & Links)
    // ═══════════════════════════════════════════════════════════════
    
    /** Primary button background */
    public static final Color BTN_PRIMARY = new Color(0x1B6CA8);    // Professional Blue
    
    /** Primary button hover */
    public static final Color BTN_PRIMARY_HOVER = new Color(0x155A8A); // Darker Blue
    
    /** Secondary button background */
    public static final Color BTN_SECONDARY = new Color(0x6B7280);  // Gray 500
    
    /** Secondary button hover */
    public static final Color BTN_SECONDARY_HOVER = new Color(0x4B5563); // Gray 600
    
    /** Success button background */
    public static final Color BTN_SUCCESS = new Color(0x27AE60);    // Success green
    
    /** Success button hover */
    public static final Color BTN_SUCCESS_HOVER = new Color(0x229954); // Darker green
    
    /** Danger button background */
    public static final Color BTN_DANGER = new Color(0xDC2626);     // Red 600
    
    /** Danger button hover */
    public static final Color BTN_DANGER_HOVER = new Color(0xB91C1C); // Red 700
    
    // ═══════════════════════════════════════════════════════════════
    //  SPECIAL COLORS (Highlights & Accents)
    // ═══════════════════════════════════════════════════════════════
    
    /** Selection background - Light green */
    public static final Color SELECTION_BG = new Color(0xD1FAE5);   // Emerald 100
    
    /** Hover background - Very light gray */
    public static final Color HOVER_BG = new Color(0xF9FAFB);       // Gray 50
    
    /** Active background - Light green */
    public static final Color ACTIVE_BG = new Color(0xA7F3D0);      // Emerald 200
    
    /** Marquee gradient start */
    public static final Color MARQUEE_START = new Color(0x047857);  // Emerald 700
    
    /** Marquee gradient end */
    public static final Color MARQUEE_END = new Color(0x065F46);    // Emerald 800
    
    /** Marquee text color */
    public static final Color MARQUEE_TEXT = new Color(0xFBBF24);   // Amber 400
    
    // ═══════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get role color based on role name
     */
    public static Color getRoleColor(String role) {
        return switch (role) {
            case "Admin" -> ROLE_ADMIN;
            case "Supervisor" -> ROLE_SUPERVISOR;
            case "Staff" -> ROLE_STAFF;
            case "DataEncoder" -> ROLE_DATA_ENCODER;
            case "Viewer" -> ROLE_VIEWER;
            default -> SECONDARY;
        };
    }
    
    /**
     * Get status color based on status
     */
    public static Color getStatusColor(String status) {
        return switch (status) {
            case "Active" -> SUCCESS;
            case "Inactive" -> WARNING;
            case "Deceased" -> ERROR;
            case "Relocated" -> INFO;
            default -> TEXT_SECONDARY;
        };
    }
    
    /**
     * Create a lighter version of a color (for hover effects)
     */
    public static Color lighter(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }
    
    /**
     * Create a darker version of a color (for pressed effects)
     */
    public static Color darker(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b);
    }
    
    /**
     * Create a semi-transparent version of a color
     */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}