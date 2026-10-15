package fraud.detection.ui;

import fraud.detection.dao.AlertDAO;
import fraud.detection.dao.TransactionDAO;
import fraud.detection.dao.UserDAO;
import fraud.detection.model.Alert;
import fraud.detection.model.Customer;
import fraud.detection.model.Decision;
import fraud.detection.model.Transaction;
import fraud.detection.model.UserResponse;
import fraud.detection.util.BackgroundTask;
import fraud.detection.util.DateUtil;
import fraud.detection.util.MessageUtil;
import fraud.detection.util.MoneyUtil;
import fraud.detection.util.TableModelBuilder;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * USER DASHBOARD: balance, buttons and the list of TRANSACTION ALERTS.
 *
 * MULTITHREADING: a ScheduledExecutorService ("alert-poller" thread) checks for
 * new alerts every 10 seconds, so an alert raised while the customer is on this
 * screen (e.g. after an admin review) appears without pressing anything.
 */
public class CustomerDashboard extends BaseFrame {

    private static final String[] COLUMNS = {"Date", "Severity", "Amount (Rs.)", "Payee", "Decision", "Message", "Your Answer"};
    private static final int POLL_SECONDS = 10;

    private final Customer customer;
    private final JLabel balanceValue, unreadValue, suspiciousValue, totalValue, newAlertBanner, selectedMessage;
    private final JTable table = new JTable();
    private final ClockLabel clock = new ClockLabel();
    private final ScheduledExecutorService poller;
    private List<Alert> alerts = new ArrayList<>();
    private volatile int lastUnread = -1;

    public CustomerDashboard(Customer customer) {
        super("My Secure Banking", customer.getWelcomeMessage(), 1040, 690);
        this.customer = customer;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logout();
            }
        });
        clock.setBounds(700, 4, 300, 22);
        content.add(clock);

        JPanel card = UITheme.card();
        card.setBounds(30, 28, 330, 110);
        content.add(card);
        addMuted(card, "Available Balance", 16, 10, 300, 20);
        balanceValue = addValue(card, "...", 16, 32, 310, 40);
        balanceValue.setFont(UITheme.font(Font.BOLD, 28));
        balanceValue.setForeground(UITheme.PRIMARY);
        addMuted(card, customer.getFullName() + "  |  Home city: " + customer.getHomeCity(), 16, 78, 310, 20);

        unreadValue = statCard("Unread Alerts", 375, 28, 200, 110, UITheme.DANGER);
        suspiciousValue = statCard("Flagged / Blocked", 590, 28, 200, 110, UITheme.WARNING);
        totalValue = statCard("My Transactions", 805, 28, 195, 110, UITheme.PRIMARY_LIGHT);

        JButton[] actions = {
            action("New Transaction", UITheme.PRIMARY_LIGHT, () -> new NewTransactionFrame(customer, this::refresh).setVisible(true)),
            action("Transaction History", UITheme.ACCENT, () -> new TransactionHistoryFrame(customer).setVisible(true)),
            action("Change Password", UITheme.MUTED, () -> new ChangePasswordFrame(customer).setVisible(true)),
            action("Logout", UITheme.DANGER, this::logout)
        };
        for (int i = 0; i < actions.length; i++) {
            addButton(actions[i], 30 + i * 245, 152, 230, 48);
        }

        addSection("Transaction Alerts", 30, 212, 300);
        newAlertBanner = addValue(content, "", 330, 212, 670, 24);
        newAlertBanner.setForeground(UITheme.DANGER);
        UITheme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(30, 240, 970, 248);
        content.add(scroll);

        selectedMessage = addLabel("Select an alert to read the full message.", 30, 492, 970, 24);
        selectedMessage.setFont(UITheme.font(Font.PLAIN, 12));
        selectedMessage.setForeground(UITheme.MUTED);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                selectedMessage.setForeground(UITheme.TEXT);
                selectedMessage.setText(alerts.get(table.convertRowIndexToModel(row)).getMessage());
            }
        });

        JButton mine = addButton(UITheme.button("It was me", UITheme.SUCCESS), 30, 522, 170, 40);
        JButton notMe = addButton(UITheme.dangerButton("Not me - Report Fraud"), 210, 522, 210, 40);
        JButton read = addButton(UITheme.greyButton("Mark All as Read"), 430, 522, 170, 40);
        JLabel hint = addMuted(content, "Your answer helps the AI learn. Select an alert, then answer.", 615, 530, 390, 22);
        hint.setFont(UITheme.font(Font.ITALIC, 12));
        mine.addActionListener(e -> respond(UserResponse.CONFIRMED_GENUINE));
        notMe.addActionListener(e -> respond(UserResponse.REPORTED_FRAUD));
        read.addActionListener(e -> BackgroundTask.run(this, () -> {
            new AlertDAO().markAllRead(customer.getUserId());
            return Boolean.TRUE;
        }, ok -> refresh()));

        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "alert-poller");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleAtFixedRate(this::pollUnread, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS);
        refresh();
    }

    private JButton action(String text, Color color, Runnable onClick) {
        JButton b = UITheme.button(text, color);
        b.setFont(UITheme.font(Font.BOLD, 14));
        b.addActionListener(e -> onClick.run());
        return b;
    }

    /** Reloads balance, counters and alerts on a background thread. */
    public void refresh() {
        BackgroundTask.run(this, () -> {
            Customer fresh = new UserDAO().findById(customer.getUserId()).orElse(customer);
            List<Alert> list = new AlertDAO().findByUser(customer.getUserId());
            List<Transaction> txns = new TransactionDAO().findByUser(customer.getUserId());
            return new Object[] {fresh, list, txns};
        }, r -> {
            Customer fresh = (Customer) r[0];
            @SuppressWarnings("unchecked")
            List<Alert> list = (List<Alert>) r[1];
            @SuppressWarnings("unchecked")
            List<Transaction> txns = (List<Transaction>) r[2];
            alerts = list;
            balanceValue.setText(MoneyUtil.format(fresh.getBalance()));
            long unread = list.stream().filter(a -> !a.isRead()).count();
            lastUnread = (int) unread;
            unreadValue.setText(String.valueOf(unread));
            suspiciousValue.setText(String.valueOf(txns.stream().filter(t -> t.getDecision() != Decision.APPROVED).count()));
            totalValue.setText(String.valueOf(txns.size()));
            if (unread == 0) {
                newAlertBanner.setText("");
            }
            table.setModel(TableModelBuilder.build(COLUMNS, list, (Alert a) -> new Object[] {
                DateUtil.formatShort(a.getCreatedAt()), a.getSeverity(), a.getAmount(), a.getPayee(), a.getDecision(),
                (a.isRead() ? "" : "NEW  ") + a.getMessage(), a.getResponse()
            }));
            UITheme.setColumnWidths(table, 105, 70, 95, 130, 75, 395, 120);
        });
    }

    /** Runs on the "alert-poller" thread - it must NOT touch Swing directly. */
    private void pollUnread() {
        try {
            int unread = new AlertDAO().unreadCount(customer.getUserId());
            int before = lastUnread;
            if (unread != before) {
                SwingUtilities.invokeLater(() -> {
                    if (unread > Math.max(0, before)) {
                        newAlertBanner.setText("You have " + (unread - Math.max(0, before)) + " new alert(s)!");
                    }
                    refresh();
                });
            }
        } catch (Exception e) {
            // an exception would cancel the scheduled task, so it is caught and ignored here
            System.err.println("Alert polling failed: " + e.getMessage());
        }
    }

    private void respond(UserResponse response) {
        int row = table.getSelectedRow();
        if (row < 0) {
            MessageUtil.error(this, "Please select an alert first.");
            return;
        }
        Alert a = alerts.get(table.convertRowIndexToModel(row));
        if (!a.canRespond()) {
            MessageUtil.error(this, a.getResponse() != UserResponse.NONE
                    ? "You have already answered this alert." : "This is an information message - no answer is needed.");
            return;
        }
        String question = response == UserResponse.REPORTED_FRAUD
                ? "Report the payment of " + MoneyUtil.format(a.getAmount()) + " to " + a.getPayee() + " as FRAUD?\nThe bank's fraud team will investigate."
                : "Confirm that YOU made the payment of " + MoneyUtil.format(a.getAmount()) + " to " + a.getPayee() + "?";
        if (!MessageUtil.confirm(this, question)) {
            return;
        }
        BackgroundTask.run(this, () -> {
            new AlertDAO().respond(a.getAlertId(), customer.getUserId(), response);
            return Boolean.TRUE;
        }, ok -> {
            MessageUtil.info(this, response == UserResponse.REPORTED_FRAUD
                    ? "Thank you. The case has been reported to the fraud team.\nIf it is confirmed, any debited money will be refunded."
                    : "Thank you for confirming. This helps the system learn your normal behaviour.");
            refresh();
        });
    }

    @Override
    public void dispose() {
        poller.shutdownNow();
        clock.stop();
        super.dispose();
    }

    private void logout() {
        if (MessageUtil.confirm(this, "Do you want to logout?")) {
            for (Window w : Window.getWindows()) {
                if (w != this && w.isDisplayable() && !(w instanceof LoginFrame)) {
                    w.dispose();
                }
            }
            dispose();
            LoginFrame.open();
        }
    }
}
