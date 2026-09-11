package com.accounting;

import java.nio.file.Paths;
import java.time.LocalDate;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) throws Exception {
        InputValidator validator = new InputValidator();
        validator.initialize();
        if (args.length > 0 && "--training".equals(args[0])) {
            runTraining(validator);
            return;
        }
        if (args.length == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    new AccountingGui(validator).setVisible(true);
                }
            });
            return;
        }

        runCli(validator, args[0]);
    }

    private static void runCli(InputValidator validator, String csvPath) throws Exception {
        validator.loadCSV(csvPath);
        JournalEntry journal = new JournalEntry(validator.getConnection());
        journal.createEntry(LocalDate.now(), "Supplies purchase",
                "Supplies", "Asset", 500, "Cash", "Asset", 500);

        Ledger ledger = new Ledger(validator.getConnection());
        ledger.postEntries();
        TrialBalance trialBalance = new TrialBalance(validator.getConnection());
        trialBalance.generate();
        WorksheetGenerator worksheet = new WorksheetGenerator(validator.getConnection());
        worksheet.buildWorksheet();

        ExportManager export = new ExportManager(validator.getConnection());
        export.toExcel(Paths.get("report.xlsx").toString());
        export.toPDF(Paths.get("report.pdf").toString());
        export.toWord(Paths.get("report.docx").toString());
        System.out.println("Accounting reports generated: report.xlsx, report.pdf, report.docx");
        validator.close();
    }

    private static void runTraining(InputValidator validator) throws Exception {
        AccountingCycleEngine engine = new AccountingCycleEngine(validator.getConnection());
        engine.reset();
        engine.processBatch(SampleDataset.januaryTransactions());
        System.out.printf("Unadjusted TB: %.2f / %.2f%n",
                engine.totalDebits(engine.generateUnadjustedTrialBalance()),
                engine.totalCredits(engine.generateUnadjustedTrialBalance()));
        engine.postAdjustingEntries(SampleDataset.januaryAdjustments());
        AccountingCycleEngine.Worksheet worksheet = engine.generateTenColumnWorksheet();
        System.out.printf("Worksheet net income: %.2f%n", worksheet.netIncome());
        engine.executeClosingRoutine();
        java.util.List<AccountingCycleEngine.Balance> postClosing = engine.generatePostClosingTrialBalance();
        System.out.printf("Post-closing TB: %.2f / %.2f; equation valid: %s%n",
                engine.totalDebits(postClosing), engine.totalCredits(postClosing),
                engine.verifyAccountingEquation());
        validator.close();
    }
}
