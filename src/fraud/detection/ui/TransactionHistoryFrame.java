package fraud.detection.ui;

import fraud.detection.dao.TransactionDAO;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.model.Customer;
import fraud.detection.model.Decision;
import fraud.detection.model.Transaction;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.DateUtil;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.MoneyUtil;
import fraud.detection.util.TableModelBuilder;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * A customer's transactions with their fraud scores.
 * AllTransactionsFrame (admin) EXTENDS this class and only overrides fetch().
 */
public class TransactionHistoryFrame extends BaseFrame {

    private static final String[] COLUMNS = {"Txn #", "Date & Time", "Customer", "Type", "Payee", "Amount (Rs.)",
        "Category", "Location", "Device", "Risk", "Decision"};
    private static final String ALL = "All Decisions";

    private final Customer customer;
    private final JTable table = new JTable();
    private final JComboBox<String> decisionFilter = new JComboBox<>();
    private final JTextField search;
    private final JLabel summary;
    private List<Transaction> loaded = new ArrayList<>();

    public TransactionHistoryFrame(Customer customer) {
        this("Transaction History", "All your payments with their fraud-risk scores", customer);
    }

    protected TransactionHistoryFrame(String title, String subtitle, Customer customer) {
        super(title, subtitle, 1180, 680);
        this.customer = customer;
        addLabel("Decision", 20, 14, 70, 30);
        decisionFilter.addItem(ALL);
        for (Decision d : Decision.values()) {
            decisionFilter.addItem(d.getLabel());
        }
        place(decisionFilter, 90, 14, 160, 30);
        addLabel("Search", 270, 14, 60, 30);
        search = addTextField(330, 14, 260, 30);
        search.setToolTipText("Payee, customer, city or transaction number");
        JButton find = addButton(UITheme.primaryButton("Search"), 600, 14, 95, 30);
        JButton reload = addButton(UITheme.accentButton("Reload"), 705, 14, 95, 30);
        JButton print = addButton(UITheme.greyButton("Print"), 810, 14, 95, 30);
        JButton close = addButton(UITheme.greyButton("Close"), 1045, 14, 110, 30);

        UITheme.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(20, 56, 1135, 470);
        content.add(scroll);
        summary = addLabel("Loading...", 20, 534, 1135, 40);
        summary.setFont(UITheme.font(Font.BOLD, 13));

        decisionFilter.addActionListener(e -> showFiltered());
        find.addActionListener(e -> showFiltered());
        search.addActionListener(e -> showFiltered());
        reload.addActionListener(e -> load());
        print.addActionListener(e -> {
            try {
                table.print();
            } catch (PrinterException ex) {
                MessageUtil.error(this, "Could not print: " + ex.getMessage());
            }
        });
        close.addActionListener(e -> dispose());
        load();
    }

    /** Which transactions to show. Overridden by AllTransactionsFrame. */
    protected List<Transaction> fetch() throws FraudSystemException {
        return new TransactionDAO().findByUser(customer.getUserId());
    }

    protected final void load() {
        summary.setText("Loading...");
        BackgroundTask.run(this, this::fetch, list -> {
            loaded = list;
            showFiltered();
        });
    }

    /** Filters the loaded list with the Stream API (no new database query). */
    private void showFiltered() {
        String decision = (String) decisionFilter.getSelectedItem();
        String text = search.getText().trim().toLowerCase();
        List<Transaction> rows = loaded.stream()
                .filter(t -> ALL.equals(decision) || t.getDecision().getLabel().equals(decision))
                .filter(t -> text.isEmpty() || t.getPayee().toLowerCase().contains(text)
                        || t.getCustomerName().toLowerCase().contains(text) || t.getCity().toLowerCase().contains(text)
                        || String.valueOf(t.getTxnId()).equals(text))
                .collect(Collectors.toList());
        table.setModel(TableModelBuilder.build(COLUMNS, rows, (Transaction t) -> new Object[] {
            t.getTxnId(), DateUtil.formatShort(t.getTime()), t.getCustomerName(), t.getType(), t.getPayee(), t.getAmount(),
            t.getCategory(), t.getCity(), t.getDevice(), new RiskValue(t.getRiskScore()), t.getDecision()
        }));
        UITheme.setColumnWidths(table, 55, 110, 130, 115, 150, 95, 115, 150, 110, 55, 80);

        // EnumMap: totals per decision, kept in enum order
        Map<Decision, BigDecimal> totals = new EnumMap<>(Decision.class);
        for (Transaction t : rows) {
            totals.merge(t.getDecision(), t.getAmount(), BigDecimal::add);
        }
        StringBuilder sb = new StringBuilder("<html>" + rows.size() + " transactions shown");
        for (Map.Entry<Decision, BigDecimal> e : totals.entrySet()) {
            sb.append(" &nbsp;|&nbsp; ").append(e.getKey().getLabel()).append(": ").append(MoneyUtil.format(e.getValue()));
        }
        summary.setText(sb.append("</html>").toString());
    }
}
