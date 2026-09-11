package com.accounting;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class SampleDataset {
    public static final class Transaction {
        public final LocalDate date;
        public final String debitAccount;
        public final String debitType;
        public final String creditAccount;
        public final String creditType;
        public final double amount;
        public final String description;

        public Transaction(String date, String debitAccount, String debitType, String creditAccount,
                           String creditType, double amount, String description) {
            this.date = LocalDate.parse(date); this.debitAccount = debitAccount; this.debitType = debitType;
            this.creditAccount = creditAccount; this.creditType = creditType; this.amount = amount;
            this.description = description;
        }
    }

    private SampleDataset() { }

    public static List<Transaction> januaryTransactions() {
        return Arrays.asList(
            t("2026-01-01", "Cash", "Asset", "Owner's Capital", "Equity", 25000, "Initial capital investment"),
            t("2026-01-02", "Prepaid Insurance", "Asset", "Cash", "Asset", 2400, "Paid 12-month insurance policy"),
            t("2026-01-03", "Rent Expense", "Expense", "Cash", "Asset", 1800, "Paid January office rent"),
            t("2026-01-05", "Computer Equipment", "Asset", "Accounts Payable", "Liability", 6000, "Purchased equipment on credit"),
            t("2026-01-08", "Office Supplies", "Asset", "Accounts Payable", "Liability", 950, "Purchased office consumables on credit"),
            t("2026-01-12", "Cash", "Asset", "Unearned Service Revenue", "Liability", 3600, "Received project advance"),
            t("2026-01-15", "Accounts Receivable", "Asset", "Service Revenue", "Revenue", 5200, "Billed client for deployment"),
            t("2026-01-18", "Cash", "Asset", "Service Revenue", "Revenue", 4100, "Consulting delivered for cash"),
            t("2026-01-20", "Accounts Payable", "Liability", "Cash", "Asset", 3000, "Partial equipment settlement"),
            t("2026-01-22", "Cash", "Asset", "Accounts Receivable", "Asset", 3200, "Collected client payment"),
            t("2026-01-25", "Salaries Expense", "Expense", "Cash", "Asset", 2800, "Paid staff payroll"),
            t("2026-01-28", "Utilities Expense", "Expense", "Cash", "Asset", 450, "Paid utilities"),
            t("2026-01-30", "Owner's Drawings", "Equity", "Cash", "Asset", 1200, "Owner withdrawal")
        );
    }

    public static List<Transaction> januaryAdjustments() {
        return Arrays.asList(
            t("2026-01-31", "Insurance Expense", "Expense", "Prepaid Insurance", "Asset", 200, "One month insurance expired"),
            t("2026-01-31", "Supplies Expense", "Expense", "Office Supplies", "Asset", 600, "Supplies consumed"),
            t("2026-01-31", "Depreciation Expense", "Expense", "Accumulated Depreciation - Equip.", "Asset", 100, "Monthly depreciation"),
            t("2026-01-31", "Unearned Service Revenue", "Liability", "Service Revenue", "Revenue", 1800, "Advance revenue earned"),
            t("2026-01-31", "Salaries Expense", "Expense", "Salaries Payable", "Liability", 750, "Accrued salaries")
        );
    }

    private static Transaction t(String date, String debit, String debitType, String credit,
                                 String creditType, double amount, String description) {
        return new Transaction(date, debit, debitType, credit, creditType, amount, description);
    }
}
