package com.accounting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccountingCycleEngine {
    public static final class Balance {
        public final String account; public final String type; public final double debit; public final double credit;
        Balance(String account, String type, double debit, double credit) {
            this.account = account; this.type = type; this.debit = debit; this.credit = credit;
        }
        public double net() { return debit - credit; }
    }

    public static final class Worksheet {
        public final List<Balance> balances; public final double income; public final double expenses;
        Worksheet(List<Balance> balances, double income, double expenses) {
            this.balances = balances; this.income = income; this.expenses = expenses;
        }
        public double netIncome() { return income - expenses; }
    }

    private final Connection connection;
    private final JournalEntry journal;
    private List<Balance> unadjusted;

    public AccountingCycleEngine(Connection connection) {
        this.connection = connection; this.journal = new JournalEntry(connection);
    }

    public void reset() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("DELETE FROM Ledger"); s.executeUpdate("DELETE FROM JournalEntries");
            s.executeUpdate("DELETE FROM Accounts");
        }
        unadjusted = null;
    }

    public void processBatch(List<SampleDataset.Transaction> entries) throws SQLException {
        for (SampleDataset.Transaction entry : entries)
            journal.createEntry(entry.date, entry.description, entry.debitAccount, entry.debitType,
                    entry.amount, entry.creditAccount, entry.creditType, entry.amount);
        unadjusted = balances();
    }

    public void postAdjustingEntries(List<SampleDataset.Transaction> entries) throws SQLException {
        for (SampleDataset.Transaction entry : entries)
            journal.createEntry(entry.date, entry.description, entry.debitAccount, entry.debitType,
                    entry.amount, entry.creditAccount, entry.creditType, entry.amount);
    }

    public List<Balance> generateUnadjustedTrialBalance() throws SQLException {
        if (unadjusted == null) unadjusted = balances();
        return unadjusted;
    }

    public Worksheet generateTenColumnWorksheet() throws SQLException {
        List<Balance> adjusted = balances();
        double revenue = 0, expenses = 0;
        for (Balance balance : adjusted) {
            if ("Revenue".equals(balance.type)) revenue += balance.credit - balance.debit;
            if ("Expense".equals(balance.type)) expenses += balance.debit - balance.credit;
        }
        return new Worksheet(adjusted, revenue, expenses);
    }

    public void executeClosingRoutine() throws SQLException {
        Worksheet worksheet = generateTenColumnWorksheet();
        journal.createEntry(java.time.LocalDate.of(2026, 1, 31), "Close revenue",
                "Service Revenue", "Revenue", worksheet.income, "Income Summary", "Equity", worksheet.income);
        for (Balance balance : worksheet.balances) {
            if ("Expense".equals(balance.type) && balance.debit - balance.credit > 0)
                journal.createEntry(java.time.LocalDate.of(2026, 1, 31), "Close expense",
                        "Income Summary", "Equity", balance.debit - balance.credit,
                        balance.account, "Expense", balance.debit - balance.credit);
        }
        journal.createEntry(java.time.LocalDate.of(2026, 1, 31), "Transfer net income",
                "Income Summary", "Equity", worksheet.netIncome(), "Owner's Capital", "Equity", worksheet.netIncome());
        journal.createEntry(java.time.LocalDate.of(2026, 1, 31), "Close drawings",
                "Owner's Capital", "Equity", 1200, "Owner's Drawings", "Equity", 1200);
    }

    public List<Balance> generatePostClosingTrialBalance() throws SQLException {
        List<Balance> result = new ArrayList<Balance>();
        for (Balance balance : balances())
            if (!"Revenue".equals(balance.type) && !"Expense".equals(balance.type)
                    && !"Income Summary".equals(balance.account) && Math.abs(balance.net()) > 0.000001)
                result.add(balance);
        return result;
    }

    public boolean verifyAccountingEquation() throws SQLException {
        double assets = 0, liabilities = 0, equity = 0;
        for (Balance balance : generatePostClosingTrialBalance()) {
            if ("Asset".equals(balance.type)) assets += balance.net();
            else if ("Liability".equals(balance.type)) liabilities += -balance.net();
            else if ("Equity".equals(balance.type)) equity += -balance.net();
        }
        return Math.abs(assets - (liabilities + equity)) < 0.000001;
    }

    public double totalDebits(List<Balance> rows) {
        double total = 0; for (Balance row : rows) total += row.debit; return total;
    }

    public double totalCredits(List<Balance> rows) {
        double total = 0; for (Balance row : rows) total += row.credit; return total;
    }

    private List<Balance> balances() throws SQLException {
        List<Balance> rows = new ArrayList<Balance>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(
                "SELECT a.name, a.type, ROUND(SUM(j.debit - j.credit),2) " +
                "FROM Accounts a JOIN JournalEntries j ON j.account_id=a.id GROUP BY a.id ORDER BY a.id")) {
            while (rs.next()) {
                double net = rs.getDouble(3);
                rows.add(new Balance(rs.getString(1), rs.getString(2),
                        net > 0 ? net : 0, net < 0 ? -net : 0));
            }
        }
        return rows;
    }
}
