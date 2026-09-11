package com.accounting;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ExportManager {
    private final WorksheetGenerator worksheet;
    public ExportManager(Connection connection) { worksheet = new WorksheetGenerator(connection); }
    public void toExcel(String path) throws SQLException, IOException {
        List<WorksheetGenerator.Row> rows = worksheet.buildWorksheet();
        try (XSSFWorkbook book = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(path)) {
            Sheet sheet = book.createSheet("Worksheet");
            Row header = sheet.createRow(0); header.createCell(0).setCellValue("Account");
            header.createCell(1).setCellValue("Type"); header.createCell(2).setCellValue("Debit"); header.createCell(3).setCellValue("Credit");
            for (int i = 0; i < rows.size(); i++) {
                WorksheetGenerator.Row item = rows.get(i); Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.account); row.createCell(1).setCellValue(item.type);
                row.createCell(2).setCellValue(item.debit); row.createCell(3).setCellValue(item.credit);
            }
            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);
            book.write(out);
        }
    }
    public void toPDF(String path) throws SQLException, IOException {
        try (PdfWriter writer = new PdfWriter(path); PdfDocument pdf = new PdfDocument(writer); Document document = new Document(pdf)) {
            document.add(new Paragraph("Accounting Worksheet"));
            for (WorksheetGenerator.Row row : worksheet.buildWorksheet())
                document.add(new Paragraph(String.format("%s (%s): Debit %.2f, Credit %.2f", row.account, row.type, row.debit, row.credit)));
        }
    }
    public void toWord(String path) throws SQLException, IOException {
        try (XWPFDocument document = new XWPFDocument(); FileOutputStream out = new FileOutputStream(path)) {
            document.createParagraph().createRun().setText("Accounting Worksheet");
            for (WorksheetGenerator.Row row : worksheet.buildWorksheet())
                document.createParagraph().createRun().setText(String.format("%s (%s): Debit %.2f, Credit %.2f", row.account, row.type, row.debit, row.credit));
            document.write(out);
        }
    }
}
