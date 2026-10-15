package fraud.detection.service;

import fraud.detection.engine.Assessment;
import java.math.BigDecimal;

/** What the customer sees after submitting a transaction. */
public final class TransactionResult {

    private final long txnId;
    private final Assessment assessment;
    private final BigDecimal balanceAfter;

    public TransactionResult(long txnId, Assessment assessment, BigDecimal balanceAfter) {
        this.txnId = txnId;
        this.assessment = assessment;
        this.balanceAfter = balanceAfter;
    }

    public long getTxnId()             { return txnId; }
    public Assessment getAssessment()  { return assessment; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
}
