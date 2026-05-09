import utils.IconGenerator;
import utils.DatabaseFixUtil;
import utils.DatabaseMigration;
import java.awt.Image;
import java.util.List;

public class Main { 
    public static void main(String[] args) { 
        try {
            // Set application icon for all windows
            List<Image> icons = IconGenerator.generateMultiSizeIcons();
            
            db.DatabaseManager.initDatabase();
            
            // Run database migrations (fixes old database schemas)
            DatabaseMigration.runMigrations();
            
            // Fix admin account and approval status (one-time fix)
            DatabaseFixUtil.fixAdminAccount();
            
            javax.swing.SwingUtilities.invokeLater(() -> { 
                auth.LoginFrame loginFrame = new auth.LoginFrame();
                loginFrame.setIconImages(icons);
                loginFrame.setVisible(true); 
            });
        } catch (Exception e) { 
            e.printStackTrace();
        } 
    } 
} 
