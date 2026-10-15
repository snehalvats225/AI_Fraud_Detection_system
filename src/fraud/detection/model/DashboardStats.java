package fraud.detection.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Numbers and chart data for the admin dashboard. */
public final class DashboardStats {

    private final int customers;
    private final int transactions;
    private final int flagged;
    private final int blocked;
    private final int openCases;
    private final int confirmedFraud;
    private final int falsePositives;
    private final BigDecimal amountBlocked;
    private final Map<LocalDate, int[]> casesPerDay;          // date -> {flagged, blocked} (TreeMap, sorted)
    private final Map<CaseStatus, Integer> casesByStatus;     // EnumMap
    private final int[] riskHistogram;                        // 10 buckets: 0.0-0.1, 0.1-0.2 ...
    private final Map<MerchantCategory, Integer> casesByCategory;

    public DashboardStats(int customers, int transactions, int flagged, int blocked, int openCases,
                          int confirmedFraud, int falsePositives, BigDecimal amountBlocked,
                          Map<LocalDate, int[]> casesPerDay, Map<CaseStatus, Integer> casesByStatus,
                          int[] riskHistogram, Map<MerchantCategory, Integer> casesByCategory) {
        this.customers = customers;
        this.transactions = transactions;
        this.flagged = flagged;
        this.blocked = blocked;
        this.openCases = openCases;
        this.confirmedFraud = confirmedFraud;
        this.falsePositives = falsePositives;
        this.amountBlocked = amountBlocked;
        this.casesPerDay = casesPerDay;
        this.casesByStatus = casesByStatus;
        this.riskHistogram = riskHistogram;
        this.casesByCategory = casesByCategory;
    }

    public int getCustomers()            { return customers; }
    public int getTransactions()         { return transactions; }
    public int getFlagged()              { return flagged; }
    public int getBlocked()              { return blocked; }
    public int getOpenCases()            { return openCases; }
    public int getConfirmedFraud()       { return confirmedFraud; }
    public int getFalsePositives()       { return falsePositives; }
    public BigDecimal getAmountBlocked() { return amountBlocked; }
    public Map<LocalDate, int[]> getCasesPerDay()              { return casesPerDay; }
    public Map<CaseStatus, Integer> getCasesByStatus()         { return casesByStatus; }
    public int[] getRiskHistogram()                            { return riskHistogram.clone(); }
    public Map<MerchantCategory, Integer> getCasesByCategory() { return casesByCategory; }

    /** Of the cases the admin has reviewed, how many were real fraud (live precision). -1 = no reviews yet. */
    public double getLivePrecision() {
        int reviewed = confirmedFraud + falsePositives;
        return reviewed == 0 ? -1 : (double) confirmedFraud / reviewed;
    }
}
