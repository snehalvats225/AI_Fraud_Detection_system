package fraud.detection.ui;

import fraud.detection.dao.TransactionDAO;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.model.Transaction;
import java.util.List;

/**
 * Admin view of every customer's transactions. It INHERITS the whole screen from
 * TransactionHistoryFrame and only OVERRIDES fetch() - reuse through inheritance.
 */
public class AllTransactionsFrame extends TransactionHistoryFrame {

    public AllTransactionsFrame() {
        super("All Transactions", "Every transaction processed by the detection engine (latest 5,000)", null);
    }

    @Override
    protected List<Transaction> fetch() throws FraudSystemException {
        return new TransactionDAO().findAll();
    }
}
