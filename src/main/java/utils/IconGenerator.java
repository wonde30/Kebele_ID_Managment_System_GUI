package utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to generate and load application icons.
 * Creates a professional icon for the Kebele ID System.
 */
public class IconGenerator {

    /**
     * Generates a professional application icon.
     * Features: ID card design with Ethiopian colors.
     * 
     * @param size Icon size (e.g., 16, 32, 64, 128, 256)
     * @return BufferedImage of the icon
     */
    public static BufferedImage generateIcon(int size) {
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        
        // Enable anti-aliasing for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Ethiopian flag colors
        Color green = new Color(0x009639);  // Ethiopian green
        Color yellow = new Color(0xFEDD00); // Ethiopian yellow
        Color red = new Color(0xDA121A);    // Ethiopian red
        Color blue = new Color(0x0D47A1);   // Professional blue
        Color gold = new Color(0xFFD700);   // Gold accent
        
        // Background - Gradient from blue to darker blue
        GradientPaint bgGradient = new GradientPaint(
            0, 0, new Color(0x1565C0),
            size, size, new Color(0x0D47A1)
        );
        g2d.setPaint(bgGradient);
        g2d.fillRoundRect(0, 0, size, size, size/8, size/8);
        
        // Ethiopian flag stripe at top (thin)
        int stripeHeight = size / 12;
        g2d.setColor(green);
        g2d.fillRect(0, 0, size/3, stripeHeight);
        g2d.setColor(yellow);
        g2d.fillRect(size/3, 0, size/3, stripeHeight);
        g2d.setColor(red);
        g2d.fillRect(2*size/3, 0, size/3, stripeHeight);
        
        // ID Card shape in center
        int cardWidth = (int)(size * 0.65);
        int cardHeight = (int)(size * 0.5);
        int cardX = (size - cardWidth) / 2;
        int cardY = (size - cardHeight) / 2 + size/10;
        
        // Card shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRoundRect(cardX + 3, cardY + 3, cardWidth, cardHeight, size/16, size/16);
        
        // Card background - white
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(cardX, cardY, cardWidth, cardHeight, size/16, size/16);
        
        // Card border - gold
        g2d.setColor(gold);
        g2d.setStroke(new BasicStroke(Math.max(1, size/64f)));
        g2d.drawRoundRect(cardX, cardY, cardWidth, cardHeight, size/16, size/16);
        
        // Person icon on card (simplified)
        int personSize = cardHeight / 3;
        int personX = cardX + cardWidth/4 - personSize/2;
        int personY = cardY + cardHeight/2 - personSize/2;
        
        // Head
        g2d.setColor(blue);
        g2d.fillOval(personX + personSize/4, personY, personSize/2, personSize/2);
        
        // Body
        g2d.fillRoundRect(personX, personY + personSize/2, 
                         personSize, personSize/2, personSize/4, personSize/4);
        
        // Text lines on card (representing ID info)
        g2d.setColor(new Color(0x666666));
        int lineX = cardX + cardWidth/2;
        int lineY = cardY + cardHeight/3;
        int lineWidth = cardWidth/3;
        int lineHeight = Math.max(1, size/48);
        int lineSpacing = cardHeight/6;
        
        for (int i = 0; i < 3; i++) {
            g2d.fillRoundRect(lineX, lineY + i * lineSpacing, 
                            lineWidth, lineHeight, lineHeight, lineHeight);
        }
        
        // "KIS" text at bottom (Kebele ID System)
        if (size >= 32) {
            Font font = new Font("Arial", Font.BOLD, Math.max(8, size/8));
            g2d.setFont(font);
            g2d.setColor(gold);
            
            String text = "KIS";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textX = (size - textWidth) / 2;
            int textY = size - size/8;
            
            // Text shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.drawString(text, textX + 1, textY + 1);
            
            // Text
            g2d.setColor(gold);
            g2d.drawString(text, textX, textY);
        }
        
        // Border around entire icon
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(Math.max(1, size/32f)));
        g2d.drawRoundRect(1, 1, size-2, size-2, size/8, size/8);
        
        g2d.dispose();
        return icon;
    }

    /**
     * Generates icons in multiple sizes for different contexts.
     * 
     * @return Array of icons in sizes: 16, 32, 48, 64, 128, 256
     */
    public static java.util.List<Image> generateMultiSizeIcons() {
        java.util.List<Image> icons = new java.util.ArrayList<>();
        int[] sizes = {16, 32, 48, 64, 128, 256};
        
        for (int size : sizes) {
            icons.add(generateIcon(size));
        }
        
        return icons;
    }

    /**
     * Saves the icon to a file (for testing/export).
     * 
     * @param size Icon size
     * @param filename Output filename
     */
    public static void saveIconToFile(int size, String filename) {
        try {
            BufferedImage icon = generateIcon(size);
            File outputFile = new File(filename);
            ImageIO.write(icon, "PNG", outputFile);
            System.out.println("Icon saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving icon: " + e.getMessage());
        }
    }

    /**
     * Loads icon from resources if available, otherwise generates it.
     * 
     * @param size Icon size
     * @return Icon image
     */
    public static Image loadOrGenerateIcon(int size) {
        try {
            // Try to load from resources first
            InputStream is = IconGenerator.class.getResourceAsStream("/icon.png");
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                is.close();
                // Scale to requested size
                return img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            // If loading fails, generate icon
        }
        
        // Generate icon if not found in resources
        return generateIcon(size);
    }

    /**
     * Test method to generate and save icons.
     */
    public static void main(String[] args) {
        // Generate icons in various sizes
        int[] sizes = {16, 32, 48, 64, 128, 256};
        
        for (int size : sizes) {
            saveIconToFile(size, "icon_" + size + ".png");
        }
        
        System.out.println("All icons generated successfully!");
    }
}
