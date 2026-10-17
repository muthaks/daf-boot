package za.co.rmb.approvalforum.model.agenda;

/** A targeted, reasoned correction of the reviewer on a pending submission. */
public class ReassignAgendaReviewerRequest {
    private String reviewerId;
    private String reviewerName;
    private String expectedReviewerId;
    private String reason;
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String value) { reviewerId = value; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String value) { reviewerName = value; }
    public String getExpectedReviewerId() { return expectedReviewerId; }
    public void setExpectedReviewerId(String value) { expectedReviewerId = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
}
