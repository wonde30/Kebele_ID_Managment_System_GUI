package config;

import java.io.*;
import java.util.Properties;

/**
 * Application configuration manager.
 * Loads and manages configuration from application.properties file.
 */
public class AppConfig {

    private static final String CONFIG_FILE = "application.properties";
    private static final Properties properties = new Properties();
    private static AppConfig instance;

    // Default values
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:kebele_system.db";
    private static final String DEFAULT_WINDOW_WIDTH = "1220";
    private static final String DEFAULT_WINDOW_HEIGHT = "840";
    private static final String DEFAULT_SESSION_TIMEOUT = "1800000"; // 30 minutes
    private static final String DEFAULT_PASSWORD_MIN_LENGTH = "8";
    private static final String DEFAULT_MAX_LOGIN_ATTEMPTS = "5";
    private static final String DEFAULT_MARQUEE_UPDATE_INTERVAL = "30000"; // 30 seconds
    private static final String DEFAULT_BACKUP_ENABLED = "true";
    private static final String DEFAULT_BACKUP_PATH = "./backups";

    private AppConfig() {
        loadConfig();
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /**
     * Loads configuration from file or creates default if not exists.
     */
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try (InputStream input = new FileInputStream(configFile)) {
                properties.load(input);
                System.out.println("Configuration loaded from " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("Error loading config file: " + e.getMessage());
                loadDefaults();
            }
        } else {
            System.out.println("Config file not found, using defaults");
            loadDefaults();
            saveConfig(); // Create default config file
        }
    }

    /**
     * Loads default configuration values.
     */
    private void loadDefaults() {
        properties.setProperty("db.url", DEFAULT_DB_URL);
        properties.setProperty("db.backup.enabled", DEFAULT_BACKUP_ENABLED);
        properties.setProperty("db.backup.path", DEFAULT_BACKUP_PATH);
        
        properties.setProperty("ui.window.width", DEFAULT_WINDOW_WIDTH);
        properties.setProperty("ui.window.height", DEFAULT_WINDOW_HEIGHT);
        properties.setProperty("ui.theme", "light");
        
        properties.setProperty("security.password.minLength", DEFAULT_PASSWORD_MIN_LENGTH);
        properties.setProperty("security.password.requireSpecialChars", "true");
        properties.setProperty("security.password.requireUppercase", "true");
        properties.setProperty("security.password.requireNumbers", "true");
        properties.setProperty("security.session.timeout", DEFAULT_SESSION_TIMEOUT);
        properties.setProperty("security.maxLoginAttempts", DEFAULT_MAX_LOGIN_ATTEMPTS);
        
        properties.setProperty("features.marquee.enabled", "true");
        properties.setProperty("features.marquee.updateInterval", DEFAULT_MARQUEE_UPDATE_INTERVAL);
        properties.setProperty("features.autoBackup.enabled", "true");
        properties.setProperty("features.autoBackup.interval", "86400000"); // 24 hours
    }

    /**
     * Saves current configuration to file.
     */
    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Kebele ID System Configuration");
            System.out.println("Configuration saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Error saving config file: " + e.getMessage());
        }
    }

    // Database Configuration
    public String getDatabaseUrl() {
        return properties.getProperty("db.url", DEFAULT_DB_URL);
    }

    public boolean isBackupEnabled() {
        return Boolean.parseBoolean(properties.getProperty("db.backup.enabled", DEFAULT_BACKUP_ENABLED));
    }

    public String getBackupPath() {
        return properties.getProperty("db.backup.path", DEFAULT_BACKUP_PATH);
    }

    // UI Configuration
    public int getWindowWidth() {
        return Integer.parseInt(properties.getProperty("ui.window.width", DEFAULT_WINDOW_WIDTH));
    }

    public int getWindowHeight() {
        return Integer.parseInt(properties.getProperty("ui.window.height", DEFAULT_WINDOW_HEIGHT));
    }

    public String getTheme() {
        return properties.getProperty("ui.theme", "light");
    }

    // Security Configuration
    public int getPasswordMinLength() {
        return Integer.parseInt(properties.getProperty("security.password.minLength", DEFAULT_PASSWORD_MIN_LENGTH));
    }

    public boolean isPasswordSpecialCharsRequired() {
        return Boolean.parseBoolean(properties.getProperty("security.password.requireSpecialChars", "true"));
    }

    public boolean isPasswordUppercaseRequired() {
        return Boolean.parseBoolean(properties.getProperty("security.password.requireUppercase", "true"));
    }

    public boolean isPasswordNumbersRequired() {
        return Boolean.parseBoolean(properties.getProperty("security.password.requireNumbers", "true"));
    }

    public long getSessionTimeout() {
        return Long.parseLong(properties.getProperty("security.session.timeout", DEFAULT_SESSION_TIMEOUT));
    }

    public int getMaxLoginAttempts() {
        return Integer.parseInt(properties.getProperty("security.maxLoginAttempts", DEFAULT_MAX_LOGIN_ATTEMPTS));
    }

    // Feature Configuration
    public boolean isMarqueeEnabled() {
        return Boolean.parseBoolean(properties.getProperty("features.marquee.enabled", "true"));
    }

    public int getMarqueeUpdateInterval() {
        return Integer.parseInt(properties.getProperty("features.marquee.updateInterval", DEFAULT_MARQUEE_UPDATE_INTERVAL));
    }

    public boolean isAutoBackupEnabled() {
        return Boolean.parseBoolean(properties.getProperty("features.autoBackup.enabled", "true"));
    }

    public long getAutoBackupInterval() {
        return Long.parseLong(properties.getProperty("features.autoBackup.interval", "86400000"));
    }

    // Generic getter
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Generic setter
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
}
