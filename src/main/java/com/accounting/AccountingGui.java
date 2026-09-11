package com.accounting;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class AccountingGui extends JFrame {
    private final InputValidator validator;
    private final Connection connection;
    private final DefaultTableModel trialModel = new DefaultTableModel(
            new Object[]{"Account", "Type", "Debit", "Credit", "Balance"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable trialTable = new JTable(trialModel);
    private final JComboBox<String> debitAccount = new JComboBox<String>();
    private final JComboBox<String> creditAccount = new JComboBox<String>();
    private final JTextArea status = new JTextArea(3, 50);

    public AccountingGui(InputValidator validator) {
        super("Accounting System");
        this.validator = validator;
        this.connection = validator.getConnection();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent event) {
                try { AccountingGui.this.validator.close(); }
                catch (SQLException e) { showError(e); }
            }
        });
        buildUi();
        refreshData();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dashboard", buildDashboard());
        tabs.addTab("Journal Entry", buildJournalEntry());
        tabs.addTab("Import", buildImportPanel());
        tabs.addTab("Reports", buildReportsPanel());
        root.add(tabs, BorderLayout.CENTER);
        status.setEditable(false);
        status.setLineWrap(true);
        root.add(new JScrollPane(status), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildDashboard() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        trialTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JLabel("Trial Balance", SwingConstants.LEFT), BorderLayout.NORTH);
        panel.add(new JScrollPane(trialTable), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshData());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refresh);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildJournalEntry() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        JTextField date = new JTextField(LocalDate.now().toString(), 18);
        JTextField description = new JTextField(24);
        JTextField debit = new JTextField(12);
        JTextField credit = new JTextField(12);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 3;
        panel.add(new JLabel("Guidance: increases in assets/expenses are usually debits; decreases are credits."), c);
        addField(panel, c, 1, "Date (YYYY-MM-DD)", date);
        addField(panel, c, 2, "Description", description);
        addField(panel, c, 3, "Debit account", debitAccount);
        addField(panel, c, 4, "Debit amount", debit);
        addField(panel, c, 5, "Credit account", creditAccount);
        addField(panel, c, 6, "Credit amount", credit);
        JButton save = new JButton("Post journal entry");
        save.addActionListener(e -> {
            try {
                new JournalEntry(connection).createEntry(LocalDate.parse(date.getText().trim()),
                        description.getText().trim(), (String) debitAccount.getSelectedItem(), "Other",
                        Double.parseDouble(debit.getText().trim()), (String) creditAccount.getSelectedItem(),
                        "Other", Double.parseDouble(credit.getText().trim()));
                new Ledger(connection).postEntries();
                status.setText("Journal entry posted successfully.");
                refreshData();
                description.setText(""); debit.setText(""); credit.setText("");
            } catch (Exception ex) { showError(ex); }
        });
        c.gridx = 1; c.gridy = 7; c.gridwidth = 2; panel.add(save, c);
        return panel;
    }

    private JPanel buildImportPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        JButton csv = new JButton("Import CSV");
        csv.addActionListener(e -> chooseImport(false));
        JButton ocr = new JButton("Import OCR screenshot");
        ocr.addActionListener(e -> chooseImport(true));
        panel.add(csv); panel.add(ocr);
        return panel;
    }

    private JPanel buildReportsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        JButton excel = new JButton("Export Excel");
        JButton pdf = new JButton("Export PDF");
        JButton word = new JButton("Export Word");
        excel.addActionListener(e -> export("Excel"));
        pdf.addActionListener(e -> export("PDF"));
        word.addActionListener(e -> export("Word"));
        panel.add(excel); panel.add(pdf); panel.add(word);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; panel.add(new JLabel(label), c);
        c.gridx = 1; c.gridwidth = 2; panel.add(field, c);
    }

    private void chooseImport(boolean ocr) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            if (ocr) validator.loadOCRScreenshot(chooser.getSelectedFile().getAbsolutePath());
            else validator.loadCSV(chooser.getSelectedFile().getAbsolutePath());
            new Ledger(connection).postEntries();
            status.setText("Imported " + chooser.getSelectedFile().getName() + " successfully.");
            refreshData();
        } catch (Exception ex) { showError(ex); }
    }

    private void export(String type) {
        JFileChooser chooser = new JFileChooser();
        String extension = "Excel".equals(type) ? "xlsx" : type.toLowerCase();
        chooser.setSelectedFile(new File("report." + extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            ExportManager manager = new ExportManager(connection);
            String path = chooser.getSelectedFile().getAbsolutePath();
            if ("Excel".equals(type)) manager.toExcel(path);
            else if ("PDF".equals(type)) manager.toPDF(path);
            else manager.toWord(path);
            status.setText(type + " report exported to " + path);
        } catch (Exception ex) { showError(ex); }
    }

    private void refreshData() {
        try {
            trialModel.setRowCount(0);
            List<WorksheetGenerator.Row> rows = new WorksheetGenerator(connection).buildWorksheet();
            for (WorksheetGenerator.Row row : rows)
                trialModel.addRow(new Object[]{row.account, row.type,
                        String.format("%.2f", row.debit), String.format("%.2f", row.credit),
                        String.format("%.2f", row.debit - row.credit)});
            loadAccounts();
        } catch (SQLException e) { showError(e); }
    }

    private void loadAccounts() throws SQLException {
        debitAccount.removeAllItems(); creditAccount.removeAllItems();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT name FROM Accounts ORDER BY name")) {
            while (results.next()) {
                debitAccount.addItem(results.getString(1));
                creditAccount.addItem(results.getString(1));
            }
        }
    }

    private void showError(Exception error) {
        status.setText("Error: " + error.getMessage());
        Toolkit.getDefaultToolkit().beep();
        if (isDisplayable()) JOptionPane.showMessageDialog(this, error.getMessage(), "Accounting error",
                JOptionPane.ERROR_MESSAGE);
    }
}
