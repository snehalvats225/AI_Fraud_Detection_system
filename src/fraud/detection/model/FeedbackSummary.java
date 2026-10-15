package fraud.detection.model;

/** How much real labelled feedback is available for retraining. */
public final class FeedbackSummary {

    private final int adminConfirmedFraud;
    private final int adminFalsePositive;
    private final int userReportedFraud;
    private final int userConfirmedGenuine;
    private final int labelledTotal;

    public FeedbackSummary(int adminConfirmedFraud, int adminFalsePositive, int userReportedFraud,
                           int userConfirmedGenuine, int labelledTotal) {
        this.adminConfirmedFraud = adminConfirmedFraud;
        this.adminFalsePositive = adminFalsePositive;
        this.userReportedFraud = userReportedFraud;
        this.userConfirmedGenuine = userConfirmedGenuine;
        this.labelledTotal = labelledTotal;
    }

    public int getAdminConfirmedFraud()  { return adminConfirmedFraud; }
    public int getAdminFalsePositive()   { return adminFalsePositive; }
    public int getUserReportedFraud()    { return userReportedFraud; }
    public int getUserConfirmedGenuine() { return userConfirmedGenuine; }
    public int getLabelledTotal()        { return labelledTotal; }
}
