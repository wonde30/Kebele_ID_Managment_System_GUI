package utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for exporting data to PDF and Excel formats.
 */
public class ExportUtils {

    /**
     * Exports JTable data to Excel (.xlsx) file
     */
    public static void exportToExcel(TableModel model, String filePath, String sheetName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Create header row
        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < model.getColumnCount(); col++) {
            Cell cell = headerRow.createCell(col);
            String columnName = model.getColumnName(col);
            // Skip Photo column
            if ("Photo".equals(columnName)) continue;
            cell.setCellValue(columnName);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int excelRow = 1;
        for (int row = 0; row < model.getRowCount(); row++) {
            Row dataRow = sheet.createRow(excelRow++);
            int excelCol = 0;
            for (int col = 0; col < model.getColumnCount(); col++) {
                // Skip Photo column
                if ("Photo".equals(model.getColumnName(col))) continue;
                
                Cell cell = dataRow.createCell(excelCol++);
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
                cell.setCellStyle(dataStyle);
            }
        }

        // Auto-size columns
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (!"Photo".equals(model.getColumnName(col))) {
                sheet.autoSizeColumn(col);
            }
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    /**
     * Exports JTable data to PDF file
     */
    public static void exportToPDF(TableModel model, String filePath, String title) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate()); // Landscape
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Add title
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(20);
        document.add(titlePara);

        // Add export date
        com.itextpdf.text.Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        String dateStr = "Exported: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Paragraph datePara = new Paragraph(dateStr, dateFont);
        datePara.setAlignment(Element.ALIGN_RIGHT);
        datePara.setSpacingAfter(10);
        document.add(datePara);

        // Create table (skip Photo column)
        int columnCount = model.getColumnCount();
        int photoColumnIndex = -1;
        for (int i = 0; i < columnCount; i++) {
            if ("Photo".equals(model.getColumnName(i))) {
                photoColumnIndex = i;
                break;
            }
        }
        
        int pdfColumns = photoColumnIndex >= 0 ? columnCount - 1 : columnCount;
        PdfPTable pdfTable = new PdfPTable(pdfColumns);
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(10f);
        pdfTable.setSpacingAfter(10f);

        // Add header cells
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        for (int col = 0; col < columnCount; col++) {
            if (col == photoColumnIndex) continue; // Skip Photo column
            
            PdfPCell headerCell = new PdfPCell(new Phrase(model.getColumnName(col), headerFont));
            headerCell.setBackgroundColor(new BaseColor(13, 59, 102)); // CARD_BG color
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(8);
            pdfTable.addCell(headerCell);
        }

        // Add data cells
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < columnCount; col++) {
                if (col == photoColumnIndex) continue; // Skip Photo column
                
                Object value = model.getValueAt(row, col);
                PdfPCell dataCell = new PdfPCell(new Phrase(value != null ? value.toString() : "", dataFont));
                dataCell.setPadding(5);
                dataCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                pdfTable.addCell(dataCell);
            }
        }

        document.add(pdfTable);

        // Add footer
        com.itextpdf.text.Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY);
        Paragraph footer = new Paragraph("Kebele ID Management System - Federal Democratic Republic of Ethiopia", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);

        document.close();
    }

    /**
     * Exports residents data directly from database to Excel
     */
    public static void exportResidentsToExcel(Connection conn, String filePath) throws SQLException, IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Residents");

        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Kebele ID", "First Name", "Last Name", "Father Name", 
                           "Gender", "Birth Date", "Phone", "Address", "Status", "Created"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        String sql = "SELECT id, kebele_id, first_name, last_name, father_name, gender, " +
                    "date_of_birth, phone_number, address, id_status, created_at " +
                    "FROM residents ORDER BY id DESC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rs.getInt("id"));
                row.createCell(1).setCellValue(rs.getString("kebele_id"));
                row.createCell(2).setCellValue(rs.getString("first_name"));
                row.createCell(3).setCellValue(rs.getString("last_name"));
                row.createCell(4).setCellValue(rs.getString("father_name"));
                row.createCell(5).setCellValue(rs.getString("gender"));
                row.createCell(6).setCellValue(rs.getString("date_of_birth"));
                row.createCell(7).setCellValue(rs.getString("phone_number"));
                row.createCell(8).setCellValue(rs.getString("address"));
                row.createCell(9).setCellValue(rs.getString("id_status"));
                row.createCell(10).setCellValue(rs.getString("created_at"));
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    /**
     * Shows export dialog and exports table data
     */
    public static void showExportDialog(JFrame parent, TableModel model, String defaultFileName) {
        String[] options = {"Export to Excel (.xlsx)", "Export to PDF (.pdf)", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent,
                "Choose export format:",
                "Export Data",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Export File");
        
        if (choice == 0) {
            // Excel export
            fileChooser.setSelectedFile(new java.io.File(defaultFileName + ".xlsx"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
            
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".xlsx")) {
                    file = new java.io.File(file.getAbsolutePath() + ".xlsx");
                }
                
                try {
                    exportToExcel(model, file.getAbsolutePath(), "Residents");
                    JOptionPane.showMessageDialog(parent,
                            "Data exported successfully to:\n" + file.getAbsolutePath(),
                            "Export Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parent,
                            "Export failed: " + e.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        } else if (choice == 1) {
            // PDF export
            fileChooser.setSelectedFile(new java.io.File(defaultFileName + ".pdf"));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
            
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".pdf")) {
                    file = new java.io.File(file.getAbsolutePath() + ".pdf");
                }
                
                try {
                    exportToPDF(model, file.getAbsolutePath(), "Kebele ID System - Residents Report");
                    JOptionPane.showMessageDialog(parent,
                            "Data exported successfully to:\n" + file.getAbsolutePath(),
                            "Export Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(parent,
                            "Export failed: " + e.getMessage(),
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }
    }
}
