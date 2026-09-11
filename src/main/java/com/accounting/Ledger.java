package com.accounting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class Ledger {
    private final Connection connection;
    public Ledger(Connection connection) { this.connection = connection; }
    public void postEntries() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM Ledger");
            statement.executeUpdate("INSERT INTO Ledger(account_id, balance) " +
                    "SELECT account_id, ROUND(SUM(debit - credit), 2) FROM JournalEntries GROUP BY account_id");
        }
    }
}
