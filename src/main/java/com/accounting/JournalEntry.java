package com.accounting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class JournalEntry {
    private final Connection connection;
    public JournalEntry(Connection connection) { this.connection = connection; }

    public void createEntry(String debitAccount, double debit, String creditAccount, double credit) throws SQLException {
        createEntry(LocalDate.now(), "", debitAccount, "Other", debit, creditAccount, "Other", credit);
    }

    public void createEntry(LocalDate date, String description, String debitAccount, String debitType,
                            double debit, String creditAccount, String creditType, double credit) throws SQLException {
        new ValidationService().validateBalanced(debit, credit);
        String safeDebitAccount = InputSanitizer.accountName(debitAccount);
        String safeCreditAccount = InputSanitizer.accountName(creditAccount);
        String safeDescription = InputSanitizer.description(description);
        new ValidationService().validateAccountType(debitType);
        new ValidationService().validateAccountType(creditType);
        connection.setAutoCommit(false);
        try {
            int debitId = accountId(safeDebitAccount, debitType);
            int creditId = accountId(safeCreditAccount, creditType);
            String sql = "INSERT INTO JournalEntries(date, account_id, description, debit, credit) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, date.toString()); ps.setInt(2, debitId); ps.setString(3, safeDescription);
                ps.setDouble(4, debit); ps.setDouble(5, 0); ps.executeUpdate();
                ps.setInt(2, creditId); ps.setDouble(4, 0); ps.setDouble(5, credit); ps.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback(); throw e;
        } finally { connection.setAutoCommit(true); }
    }

    private int accountId(String name, String type) throws SQLException {
        String sql = "INSERT INTO Accounts(name, type) VALUES (?, ?) ON CONFLICT(name) DO UPDATE SET type=excluded.type";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name.trim()); ps.setString(2, type); ps.executeUpdate();
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM Accounts WHERE name = ?")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        }
        throw new SQLException("Unable to create account: " + name);
    }
}
