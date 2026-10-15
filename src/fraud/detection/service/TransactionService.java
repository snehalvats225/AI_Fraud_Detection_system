package fraud.detection.service;

import fraud.detection.dao.AlertDAO;
import fraud.detection.dao.FraudCaseDAO;
import fraud.detection.dao.TransactionDAO;
import fraud.detection.dao.UserDAO;
import fraud.detection.engine.Assessment;
import fraud.detection.engine.FraudDetectionEngine;
import fraud.detection.engine.UserProfile;
import fraud.detection.exception.FraudSystemException;
import fraud.detection.exception.InsufficientBalanceException;
import fraud.detection.exception.UnauthorizedActionException;
import fraud.detection.model.AlertSeverity;
import fraud.detection.model.Decision;
import fraud.detection.model.TransactionRequest;
import fraud.detection.util.MoneyUtil;
import java.math.BigDecimal;

/**
 * Processes a payment: score it for fraud, then approve, flag or block it.
 *
 * Safety:
 *   - synchronized (AccountLockManager.lockFor(userId))  -> one payment per customer at a time
 *   - SELECT ... FOR UPDATE on the customer's row        -> database-level lock
 *   - one DB transaction: transaction row + balance + case + alert saved together
 */
public class TransactionService {

    private final UserDAO userDAO = new UserDAO();
    private final TransactionDAO txnDAO = new TransactionDAO();
    private final FraudCaseDAO caseDAO = new FraudCaseDAO();
    private final AlertDAO alertDAO = new AlertDAO();

    public TransactionResult process(int userId, TransactionRequest req) throws FraudSystemException {
        return process(userId, req, null);
    }

    /**
     * @param knownLabel only used by the Traffic Simulator: the true answer (1 = fraud, 0 = genuine)
     *                   is stored as feedback so the model can learn from simulated traffic.
     */
    public TransactionResult process(int userId, TransactionRequest req, Integer knownLabel) throws FraudSystemException {
        synchronized (AccountLockManager.lockFor(userId)) {
            return TxRunner.inTransaction(con -> {
                UserDAO.LockedAccount account = userDAO.lockAccount(con, userId)
                        .orElseThrow(() -> new UnauthorizedActionException("Customer account not found."));
                if (!account.active) {
                    throw new UnauthorizedActionException("This account is blocked.");
                }
                if (req.getAmount().compareTo(account.balance) > 0) {
                    throw new InsufficientBalanceException(req.getAmount(), account.balance);
                }

                UserProfile profile = txnDAO.buildProfile(con, userId, account.balance, account.homeCity, req.getTime());
                Assessment a = FraudDetectionEngine.assess(req, profile);
                Decision d = a.getDecision();

                BigDecimal after = d == Decision.BLOCKED ? account.balance : account.balance.subtract(req.getAmount());
                long txnId = txnDAO.insert(con, userId, req, a, after);
                if (d != Decision.BLOCKED) {
                    userDAO.updateBalance(con, userId, after);
                }
                if (d != Decision.APPROVED) {
                    caseDAO.insert(con, txnId, userId, a.getRiskScore(), a.reasonsText());
                    alertDAO.insert(con, userId, txnId, d == Decision.BLOCKED ? AlertSeverity.HIGH : AlertSeverity.MEDIUM,
                            alertMessage(req, a));
                }
                if (knownLabel != null) {
                    txnDAO.setLabel(con, txnId, knownLabel);
                }
                return new TransactionResult(txnId, a, after);
            });
        }
    }

    private static String alertMessage(TransactionRequest req, Assessment a) {
        String amount = MoneyUtil.format(req.getAmount());
        String risk = MoneyUtil.percent(a.getRiskScore());
        if (a.getDecision() == Decision.BLOCKED) {
            return "BLOCKED: " + amount + " to " + req.getPayee() + " was stopped to protect your account (risk " + risk
                    + "). If this was you, tell us 'It was me'.";
        }
        return "SUSPICIOUS: " + amount + " to " + req.getPayee() + " went through but looks unusual (risk " + risk
                + "). If you did not make it, report it now.";
    }
}
