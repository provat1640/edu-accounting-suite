package com.accounting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WorksheetGenerator {
    public static class Row {
        public final String account; public final String type; public final double debit; public final double credit;
        Row(String account, String type, double debit, double credit) {
            this.account = account; this.type = type; this.debit = debit; this.credit = credit;
        }
    }
    private final Connection connection;
    public WorksheetGenerator(Connection connection) { this.connection = connection; }
    public List<Row> buildWorksheet() throws SQLException {
        List<Row> rows = new ArrayList<Row>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT a.name, a.type, ROUND(SUM(j.debit),2), ROUND(SUM(j.credit),2) " +
                "FROM Accounts a JOIN JournalEntries j ON j.account_id=a.id GROUP BY a.id ORDER BY a.name")) {
            while (rs.next()) rows.add(new Row(rs.getString(1), rs.getString(2), rs.getDouble(3), rs.getDouble(4)));
        }
        return rows;
    }
}
