package utils;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.*;

/**
 * Utility class for database backup and restore operations.
 */
public class BackupUtils {

    private static final String DB_FILE = "kebele_system.db";
    private static final String BACKUP_DIR = "backups";
    
    /**
     * Creates a backup of the database with timestamp
     */
    public static String createBackup() throws IOException {
        // Create backups directory if it doesn't exist
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Generate backup filename with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = "kebele_system_backup_" + timestamp + ".db";
        String backupPath = BACKUP_DIR + File.separator + backupFileName;
        
        // Copy database file
        File sourceFile = new File(DB_FILE);
        if (!sourceFile.exists()) {
            throw new IOException("Database file not found: " + DB_FILE);
        }
        
        Files.copy(sourceFile.toPath(), 
                  Paths.get(backupPath), 
                  StandardCopyOption.REPLACE_EXISTING);
        
        return backupPath;
    }
    
    /**
     * Creates a compressed backup (ZIP format)
     */
    public static String createCompressedBackup() throws IOException {
        // Create backups directory if it doesn't exist
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Generate backup filename with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String zipFileName = "kebele_system_backup_" + timestamp + ".zip";
        String zipPath = BACKUP_DIR + File.separator + zipFileName;
        
        // Create ZIP file
        File sourceFile = new File(DB_FILE);
        if (!sourceFile.exists()) {
            throw new IOException("Database file not found: " + DB_FILE);
        }
        
        try (FileOutputStream fos = new FileOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFile)) {
            
            ZipEntry zipEntry = new ZipEntry(DB_FILE);
            zos.putNextEntry(zipEntry);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            
            zos.closeEntry();
        }
        
        return zipPath;
    }
    
    /**
     * Restores database from a backup file
     */
    public static void restoreBackup(String backupPath) throws IOException {
        File backupFile = new File(backupPath);
        if (!backupFile.exists()) {
            throw new IOException("Backup file not found: " + backupPath);
        }
        
        // Create a backup of current database before restoring
        String currentBackup = createBackup();
        System.out.println("Current database backed up to: " + currentBackup);
        
        // Restore from backup
        File targetFile = new File(DB_FILE);
        
        if (backupPath.endsWith(".zip")) {
            // Extract from ZIP
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                ZipEntry entry = zis.getNextEntry();
                if (entry != null) {
                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        } else {
            // Direct copy
            Files.copy(backupFile.toPath(), 
                      targetFile.toPath(), 
                      StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    /**
     * Lists all available backups
     */
    public static File[] listBackups() {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            return new File[0];
        }
        
        File[] backups = backupDir.listFiles((dir, name) -> 
            name.startsWith("kebele_system_backup_") && 
            (name.endsWith(".db") || name.endsWith(".zip")));
        
        if (backups == null) {
            return new File[0];
        }
        
        // Sort by last modified (newest first)
        java.util.Arrays.sort(backups, (a, b) -> 
            Long.compare(b.lastModified(), a.lastModified()));
        
        return backups;
    }
    
    /**
     * Deletes old backups, keeping only the specified number of recent backups
     */
    public static int cleanOldBackups(int keepCount) {
        File[] backups = listBackups();
        int deleted = 0;
        
        if (backups.length > keepCount) {
            for (int i = keepCount; i < backups.length; i++) {
                if (backups[i].delete()) {
                    deleted++;
                }
            }
        }
        
        return deleted;
    }
    
    /**
     * Gets the size of a file in human-readable format
     */
    public static String getFileSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Shows backup dialog with options
     */
    public static void showBackupDialog(JFrame parent) {
        String[] options = {"Create Backup", "Create Compressed Backup", "Restore Backup", 
                           "Manage Backups", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent,
                "Choose backup operation:",
                "Database Backup & Restore",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        
        try {
            switch (choice) {
                case 0: // Create Backup
                    String backupPath = createBackup();
                    JOptionPane.showMessageDialog(parent,
                            "Backup created successfully!\n\nLocation: " + backupPath,
                            "Backup Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                    
                case 1: // Create Compressed Backup
                    String zipPath = createCompressedBackup();
                    File zipFile = new File(zipPath);
                    JOptionPane.showMessageDialog(parent,
                            "Compressed backup created successfully!\n\n" +
                            "Location: " + zipPath + "\n" +
                            "Size: " + getFileSize(zipFile),
                            "Backup Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    break;
                    
                case 2: // Restore Backup
                    showRestoreDialog(parent);
                    break;
                    
                case 3: // Manage Backups
                    showManageBackupsDialog(parent);
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Operation failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Shows restore dialog with list of available backups
     */
    private static void showRestoreDialog(JFrame parent) {
        File[] backups = listBackups();
        
        if (backups.length == 0) {
            JOptionPane.showMessageDialog(parent,
                    "No backups found in the backups directory.",
                    "No Backups",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Create list of backup descriptions
        String[] backupDescriptions = new String[backups.length];
        for (int i = 0; i < backups.length; i++) {
            String name = backups[i].getName();
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(backups[i].lastModified()));
            String size = getFileSize(backups[i]);
            backupDescriptions[i] = name + " (" + date + ", " + size + ")";
        }
        
        String selected = (String) JOptionPane.showInputDialog(parent,
                "Select a backup to restore:\n\n" +
                "⚠️ WARNING: This will replace your current database!\n" +
                "A backup of the current database will be created first.",
                "Restore Backup",
                JOptionPane.WARNING_MESSAGE,
                null,
                backupDescriptions,
                backupDescriptions[0]);
        
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "Are you sure you want to restore this backup?\n\n" +
                    selected + "\n\n" +
                    "Your current database will be backed up first.",
                    "Confirm Restore",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    int index = java.util.Arrays.asList(backupDescriptions).indexOf(selected);
                    restoreBackup(backups[index].getAbsolutePath());
                    
                    JOptionPane.showMessageDialog(parent,
                            "Database restored successfully!\n\n" +
                            "⚠️ Please restart the application for changes to take effect.",
                            "Restore Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    
                    // Suggest restart
                    int restart = JOptionPane.showConfirmDialog(parent,
                            "Would you like to restart the application now?",
                            "Restart Application",
                            JOptionPane.YES_NO_OPTION);
                    
                    if (restart == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parent,
                            "Restore failed: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Shows dialog to manage existing backups
     */
    private static void showManageBackupsDialog(JFrame parent) {
        File[] backups = listBackups();
        
        if (backups.length == 0) {
            JOptionPane.showMessageDialog(parent,
                    "No backups found in the backups directory.",
                    "No Backups",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Calculate total size
        long totalSize = 0;
        for (File backup : backups) {
            totalSize += backup.length();
        }
        
        final long finalTotalSize = totalSize; // Make effectively final
        
        String message = "Total Backups: " + backups.length + "\n" +
                        "Total Size: " + getFileSize(new File("") {
                            @Override public long length() { return finalTotalSize; }
                        }) + "\n\n" +
                        "What would you like to do?";
        
        String[] options = {"Delete Old Backups", "View All Backups", "Close"};
        int choice = JOptionPane.showOptionDialog(parent,
                message,
                "Manage Backups",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);
        
        if (choice == 0) {
            // Delete old backups
            String input = JOptionPane.showInputDialog(parent,
                    "How many recent backups would you like to keep?",
                    "5");
            
            if (input != null) {
                try {
                    int keepCount = Integer.parseInt(input);
                    int deleted = cleanOldBackups(keepCount);
                    JOptionPane.showMessageDialog(parent,
                            "Deleted " + deleted + " old backup(s).\n" +
                            "Kept " + keepCount + " most recent backup(s).",
                            "Cleanup Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(parent,
                            "Invalid number entered.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (choice == 1) {
            // View all backups
            StringBuilder sb = new StringBuilder("Available Backups:\n\n");
            for (int i = 0; i < backups.length; i++) {
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date(backups[i].lastModified()));
                sb.append((i + 1)).append(". ").append(backups[i].getName())
                  .append("\n   Date: ").append(date)
                  .append("\n   Size: ").append(getFileSize(backups[i]))
                  .append("\n\n");
            }
            
            JTextArea textArea = new JTextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new java.awt.Dimension(500, 400));
            
            JOptionPane.showMessageDialog(parent,
                    scrollPane,
                    "All Backups",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
