package fraud.detection.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One stored transaction together with its fraud scores and decision (immutable). */
public final class Transaction {

    private final long txnId;
    private final int userId;
    private final String customerName;
    private final TransactionType type;
    private final String payee;
    private final BigDecimal amount;
    private final MerchantCategory category;
    private final String city;
    private final DeviceType device;
    private final LocalDateTime time;
    private final double riskScore;
    private final double ruleScore;
    private final double statScore;
    private final double mlScore;
    private final Decision decision;
    private final BigDecimal balanceAfter;
    private final Integer modelVersion;

    public Transaction(long txnId, int userId, String customerName, TransactionType type, String payee,
                       BigDecimal amount, MerchantCategory category, String city, DeviceType device,
                       LocalDateTime time, double riskScore, double ruleScore, double statScore, double mlScore,
                       Decision decision, BigDecimal balanceAfter, Integer modelVersion) {
        this.txnId = txnId;
        this.userId = userId;
        this.customerName = customerName;
        this.type = type;
        this.payee = payee;
        this.amount = amount;
        this.category = category;
        this.city = city;
        this.device = device;
        this.time = time;
        this.riskScore = riskScore;
        this.ruleScore = ruleScore;
        this.statScore = statScore;
        this.mlScore = mlScore;
        this.decision = decision;
        this.balanceAfter = balanceAfter;
        this.modelVersion = modelVersion;
    }

    public long getTxnId()                 { return txnId; }
    public int getUserId()                 { return userId; }
    public String getCustomerName()        { return customerName; }
    public TransactionType getType()       { return type; }
    public String getPayee()               { return payee; }
    public BigDecimal getAmount()          { return amount; }
    public MerchantCategory getCategory()  { return category; }
    public String getCity()                { return city; }
    public DeviceType getDevice()          { return device; }
    public LocalDateTime getTime()         { return time; }
    public double getRiskScore()           { return riskScore; }
    public double getRuleScore()           { return ruleScore; }
    public double getStatScore()           { return statScore; }
    public double getMlScore()             { return mlScore; }
    public Decision getDecision()          { return decision; }
    public BigDecimal getBalanceAfter()    { return balanceAfter; }
    public Integer getModelVersion()       { return modelVersion; }
}
