package com.accounting;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.tess4j.Tesseract;

public class InputValidator {
    private Connection connection;

    public void initialize() throws SQLException, IOException {
        Path applicationData = Paths.get(System.getProperty("user.home"), ".EduAccountingSuite");
        Files.createDirectories(applicationData);
        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + applicationData.resolve("accounting.db").toString());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            InputStream schemaStream = getClass().getResourceAsStream("/schema.sql");
            if (schemaStream == null) throw new IOException("schema.sql was not found on the classpath");
            ByteArrayOutputStream schemaBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = schemaStream.read(buffer)) != -1) schemaBytes.write(buffer, 0, count);
            schemaStream.close();
            String schema = new String(schemaBytes.toByteArray(), StandardCharsets.UTF_8);
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) statement.execute(sql);
            }
        }
    }

    public Connection getConnection() {
        if (connection == null) throw new IllegalStateException("Database has not been initialized");
        return connection;
    }

    public void loadCSV(String path) throws IOException, SQLException {
        InputSanitizer.inputFile(path);
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (header && line.toLowerCase().contains("date")) { header = false; continue; }
                header = false;
                List<String> fields = parseCsvLine(line);
                if (fields.size() != 6) throw new IllegalArgumentException(
                        "CSV rows must contain date, debit account, debit, credit account, credit, description");
                LocalDate date = LocalDate.parse(fields.get(0).trim());
                double debit = parseAmount(fields.get(2), "debit");
                double credit = parseAmount(fields.get(4), "credit");
                if (debit <= 0 || credit <= 0 || Math.abs(debit - credit) > 0.000001)
                    throw new IllegalArgumentException("Each CSV row must have equal positive debit and credit amounts");
                new JournalEntry(connection).createEntry(date, fields.get(5), fields.get(1), "Other",
                        debit, fields.get(3), "Other", credit);
            }
        }
    }

    public void loadMonthlyInput(String date, String debitAccount, double debit,
                                 String creditAccount, double credit, String description)
            throws SQLException {
        new JournalEntry(connection).createEntry(LocalDate.parse(date), description,
                debitAccount, "Other", debit, creditAccount, "Other", credit);
    }

    public void loadOCRScreenshot(String imagePath) throws Exception {
        InputSanitizer.inputFile(imagePath);
        String text = new Tesseract().doOCR(new File(imagePath));
        boolean imported = false;
        for (String line : text.split("\\r?\\n")) {
            if (line.trim().isEmpty()) continue;
            List<String> fields = parseCsvLine(line);
            if (fields.size() == 6 && fields.get(0).matches("\\d{4}-\\d{2}-\\d{2}")) {
                loadMonthlyInput(fields.get(0), fields.get(1), parseAmount(fields.get(2), "debit"),
                        fields.get(3), parseAmount(fields.get(4), "credit"), fields.get(5));
                imported = true;
            }
        }
        if (!imported) throw new IllegalArgumentException(
                "OCR did not contain a recognized row: date,debit account,debit,credit account,credit,description");
    }

    private double parseAmount(String value, String field) {
        try { return Double.parseDouble(value.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid " + field + " amount: " + value, e); }
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) { result.add(field.toString()); field.setLength(0); }
            else field.append(c);
        }
        result.add(field.toString());
        return result;
    }

    public void close() throws SQLException {
        if (connection != null) connection.close();
    }
}
