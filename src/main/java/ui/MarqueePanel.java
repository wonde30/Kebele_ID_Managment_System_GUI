package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

/**
 * Professional marquee panel with smooth scrolling animation.
 * Features gradient background, icons, and configurable speed.
 */
public class MarqueePanel extends JPanel {
    
    private String text = "";
    private double scrollX = 0;
    private Timer animationTimer;
    private int speed = 2; // pixels per frame
    private int frameDelay = 20; // milliseconds
    
    // Professional Colors using ColorScheme
    private Color backgroundColor = ColorScheme.BG_HEADER_START;
    private Color gradientStart = ColorScheme.MARQUEE_START;
    private Color gradientEnd = ColorScheme.MARQUEE_END;
    private Color textColor = ColorScheme.MARQUEE_TEXT;
    private Color shadowColor = ColorScheme.withAlpha(Color.BLACK, 100);
    
    // Font
    private Font marqueeFont = new Font("Segoe UI", Font.BOLD, 13);
    
    // Icons/Symbols
    private static final String ICON_USERS = "👥";
    private static final String ICON_ACTIVE = "✓";
    private static final String ICON_MALE = "♂";
    private static final String ICON_FEMALE = "♀";
    private static final String ICON_LOCATION = "📍";
    private static final String ICON_USER = "👤";
    private static final String SEPARATOR = "  •  ";
    
    public MarqueePanel() {
        setPreferredSize(new Dimension(0, 35));
        setOpaque(true);
        setBackground(backgroundColor);
        
        // Start animation
        animationTimer = new Timer(frameDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scrollX -= speed;
                
                // Calculate text width
                FontMetrics fm = getFontMetrics(marqueeFont);
                int textWidth = fm.stringWidth(text);
                
                // Reset position when text scrolls off screen
                if (scrollX < -textWidth - 50) {
                    scrollX = getWidth();
                }
                
                repaint();
            }
        });
    }
    
    /**
     * Sets the marquee text with formatted statistics.
     */
    public void setText(String total, String active, String inactive, 
                       String male, String female, String userName, 
                       String userRole, String kebele) {
        
        StringBuilder sb = new StringBuilder();
        
        // Build formatted text with icons
        sb.append(" --- KEBELE ID MANAGEMENT SYSTEM ---  ");
        sb.append(SEPARATOR);
        sb.append(ICON_USERS).append(" Total: ").append(total);
        sb.append(SEPARATOR);
        sb.append(ICON_ACTIVE).append(" Active: ").append(active);
        sb.append(SEPARATOR);
        sb.append("⊗ Inactive: ").append(inactive);
        sb.append(SEPARATOR);
        sb.append(ICON_MALE).append(" Male: ").append(male);
        sb.append(SEPARATOR);
        sb.append(ICON_FEMALE).append(" Female: ").append(female);
        sb.append(SEPARATOR);
        sb.append(ICON_USER).append(" ").append(userName).append(" [").append(userRole).append("]");
        sb.append(SEPARATOR);
        sb.append(ICON_LOCATION).append(" Kebele: ").append(kebele);
        sb.append("  ---  ");
        
        this.text = sb.toString();
        
        // Reset scroll position if text changed
        if (scrollX < 0) {
            scrollX = getWidth();
        }
    }
    
    /**
     * Starts the marquee animation.
     */
    public void start() {
        if (animationTimer != null && !animationTimer.isRunning()) {
            scrollX = getWidth();
            animationTimer.start();
        }
    }
    
    /**
     * Stops the marquee animation.
     */
    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
    
    /**
     * Sets the scroll speed (pixels per frame).
     */
    public void setSpeed(int speed) {
        this.speed = Math.max(1, Math.min(10, speed)); // Clamp between 1-10
    }
    
    /**
     * Sets the marquee colors.
     */
    public void setMarqueeColors(Color background, Color text) {
        this.backgroundColor = background;
        this.textColor = text;
        setBackground(background);
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable anti-aliasing for smooth text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw gradient background
        GradientPaint gradient = new GradientPaint(
            0, 0, gradientStart,
            width, 0, gradientEnd
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
        
        // Draw subtle top border highlight
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawLine(0, 0, width, 0);
        
        // Draw subtle bottom shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.drawLine(0, height - 1, width, height - 1);
        
        // Draw scrolling text
        if (text != null && !text.isEmpty()) {
            g2d.setFont(marqueeFont);
            FontMetrics fm = g2d.getFontMetrics();
            
            int textHeight = fm.getAscent();
            int y = (height + textHeight) / 2 - 2;
            
            // Draw text shadow for depth
            g2d.setColor(shadowColor);
            g2d.drawString(text, (int) scrollX + 2, y + 2);
            
            // Draw main text
            g2d.setColor(textColor);
            g2d.drawString(text, (int) scrollX, y);
            
            // Draw second copy for seamless loop
            int textWidth = fm.stringWidth(text);
            g2d.setColor(shadowColor);
            g2d.drawString(text, (int) scrollX + textWidth + 100 + 2, y + 2);
            g2d.setColor(textColor);
            g2d.drawString(text, (int) scrollX + textWidth + 100, y);
        }
        
        g2d.dispose();
    }
    
    /**
     * Cleanup method to stop timer when panel is removed.
     */
    public void cleanup() {
        stop();
        if (animationTimer != null) {
            animationTimer = null;
        }
    }
}
