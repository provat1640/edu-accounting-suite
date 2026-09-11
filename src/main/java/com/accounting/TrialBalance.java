package com.accounting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TrialBalance {
    private final Connection connection;
    public TrialBalance(Connection connection) { this.connection = connection; }
    public void generate() throws SQLException {
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT a.name, ROUND(SUM(j.debit),2) debit, ROUND(SUM(j.credit),2) credit " +
                "FROM Accounts a JOIN JournalEntries j ON j.account_id=a.id GROUP BY a.id ORDER BY a.name")) {
            System.out.println("Trial balance");
            while (rs.next()) System.out.printf("%-20s %10.2f %10.2f%n", rs.getString(1), rs.getDouble(2), rs.getDouble(3));
        }
    }
}
