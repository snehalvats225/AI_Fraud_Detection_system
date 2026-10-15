package fraud.detection.model;

import java.time.LocalDateTime;

/** A suspicious transaction waiting for (or already given) an admin review. */
public final class FraudCase {

    private final int caseId;
    private final Transaction transaction;
    private final String reasons;
    private final CaseStatus status;
    private final String adminNotes;
    private final String reviewedBy;
    private final LocalDateTime reviewedAt;
    private final LocalDateTime createdAt;
    private final UserResponse userResponse;

    public FraudCase(int caseId, Transaction transaction, String reasons, CaseStatus status, String adminNotes,
                     String reviewedBy, LocalDateTime reviewedAt, LocalDateTime createdAt, UserResponse userResponse) {
        this.caseId = caseId;
        this.transaction = transaction;
        this.reasons = reasons;
        this.status = status;
        this.adminNotes = adminNotes;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
        this.userResponse = userResponse;
    }

    public int getCaseId()                 { return caseId; }
    public Transaction getTransaction()    { return transaction; }
    public String getReasons()             { return reasons; }
    public CaseStatus getStatus()          { return status; }
    public String getAdminNotes()          { return adminNotes; }
    public String getReviewedBy()          { return reviewedBy; }
    public LocalDateTime getReviewedAt()   { return reviewedAt; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public UserResponse getUserResponse()  { return userResponse; }
}
