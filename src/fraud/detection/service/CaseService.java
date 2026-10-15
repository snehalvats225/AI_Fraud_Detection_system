package fraud.detection.service;

import fraud.detection.dao.AlertDAO;
import fraud.detection.dao.FraudCaseDAO;
import fraud.detection.dao.TransactionDAO;
import fraud.detection.dao.UserDAO;
import fraud.detection.exception.CaseAlreadyReviewedException;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.exception.InvalidInputException;
import fraud.detection.model.Admin;
import fraud.detection.model.AlertSeverity;
import fraud.detection.model.CaseStatus;
import fraud.detection.model.Decision;
import fraud.detection.model.FraudCase;
import fraud.detection.model.Transaction;
import fraud.detection.util.MoneyUtil;
import java.math.BigDecimal;

/**
 * Admin review of fraud cases. The decision:
 *   - becomes the transaction's training LABEL (feedback for the ML model),
 *   - refunds the customer if a FLAGGED (already paid) transaction is confirmed as fraud,
 *   - notifies the customer with an INFO alert.
 * All of it happens in ONE database transaction.
 */
public class CaseService {

    private final FraudCaseDAO caseDAO = new FraudCaseDAO();
    private final TransactionDAO txnDAO = new TransactionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AlertDAO alertDAO = new AlertDAO();

    /** @return a summary message for the admin */
    public String review(int caseId, CaseStatus newStatus, String notes, Admin admin) throws FraudSystemException {
        if (newStatus == CaseStatus.OPEN) {
            throw new InvalidInputException("Choose Confirm Fraud or False Positive.");
        }
        FraudCase preview = caseDAO.findById(caseId)
                .orElseThrow(() -> new InvalidInputException("Case #" + caseId + " does not exist."));
        int userId = preview.getTransaction().getUserId();

        synchronized (AccountLockManager.lockFor(userId)) {          // a refund changes the balance
            return TxRunner.inTransaction(con -> {
                CaseStatus current = caseDAO.lockStatus(con, caseId)
                        .orElseThrow(() -> new InvalidInputException("Case #" + caseId + " does not exist."));
                if (current != CaseStatus.OPEN) {
                    throw new CaseAlreadyReviewedException("Case #" + caseId + " was already reviewed (" + current + ").");
                }
                Transaction t = caseDAO.findById(con, caseId).orElseThrow().getTransaction();
                caseDAO.review(con, caseId, newStatus, notes, admin.getUserId());
                txnDAO.setLabel(con, t.getTxnId(), newStatus == CaseStatus.CONFIRMED_FRAUD ? 1 : 0);

                String amount = MoneyUtil.format(t.getAmount());
                String customerMsg;
                String adminMsg;
                if (newStatus == CaseStatus.CONFIRMED_FRAUD && t.getDecision() == Decision.FLAGGED) {
                    UserDAO.LockedAccount acc = userDAO.lockAccount(con, userId).orElseThrow();
                    BigDecimal newBalance = acc.balance.add(t.getAmount());
                    userDAO.updateBalance(con, userId, newBalance);
                    txnDAO.insertRefund(con, userId, t.getAmount(), "Refund for fraud case #" + caseId, newBalance, acc.homeCity);
                    customerMsg = "Case #" + caseId + ": the payment of " + amount + " to " + t.getPayee()
                            + " was confirmed as FRAUD. " + amount + " has been refunded to your account.";
                    adminMsg = "Case #" + caseId + " confirmed as fraud. " + amount + " refunded to " + t.getCustomerName() + ".";
                } else if (newStatus == CaseStatus.CONFIRMED_FRAUD) {
                    customerMsg = "Case #" + caseId + ": the blocked payment of " + amount + " was confirmed as FRAUD. "
                            + "Your money was never debited. Please change your password.";
                    adminMsg = "Case #" + caseId + " confirmed as fraud. The payment had been blocked, so no money was lost.";
                } else if (t.getDecision() == Decision.BLOCKED) {
                    customerMsg = "Case #" + caseId + ": the blocked payment of " + amount + " to " + t.getPayee()
                            + " was reviewed and found GENUINE. Sorry for the trouble - you can try it again.";
                    adminMsg = "Case #" + caseId + " marked as false positive. The customer has been told to retry.";
                } else {
                    customerMsg = "Case #" + caseId + ": the flagged payment of " + amount + " to " + t.getPayee()
                            + " was reviewed and found GENUINE. No action needed.";
                    adminMsg = "Case #" + caseId + " marked as false positive.";
                }
                alertDAO.insert(con, userId, t.getTxnId(), AlertSeverity.INFO, customerMsg);
                return adminMsg + "\nThe decision is saved as a training label for the next model update.";
            });
        }
    }
}
