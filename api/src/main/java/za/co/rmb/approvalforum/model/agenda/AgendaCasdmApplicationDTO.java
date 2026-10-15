package za.co.rmb.approvalforum.model.agenda;

/**
 * Result of the direct CASDM REST check for the latest credit application of a
 * counterparty (GET /rest/credit-application/latest-for-inkomba). Drives the
 * agenda booking flag: found + status tell the analyst whether a WIP / sub-approval
 * application exists on CASDM.
 */
public class AgendaCasdmApplicationDTO {

    private boolean configured = true;
    private boolean found;
    private Integer applicationId;
    private String status;
    private boolean inSubReview;
    private boolean inSubApproval;
    private boolean approved;

    // Prepopulation data lifted from the CASDM application (same source DAF capture uses),
    // so the agenda booking can prefill instead of re-capturing.
    private String creditAnalyst;
    private String creditAnalystEmail;
    private String reviewAnalyst;
    private String reviewAnalystEmail;
    private String proposedRating;
    private String countryOfRisk;
    private String purposeOfSubmission;
    private String meetingName;

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }

    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isInSubReview() { return inSubReview; }
    public void setInSubReview(boolean inSubReview) { this.inSubReview = inSubReview; }

    public boolean isInSubApproval() { return inSubApproval; }
    public void setInSubApproval(boolean inSubApproval) { this.inSubApproval = inSubApproval; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public String getCreditAnalyst() { return creditAnalyst; }
    public void setCreditAnalyst(String creditAnalyst) { this.creditAnalyst = creditAnalyst; }

    public String getCreditAnalystEmail() { return creditAnalystEmail; }
    public void setCreditAnalystEmail(String creditAnalystEmail) { this.creditAnalystEmail = creditAnalystEmail; }

    public String getReviewAnalyst() { return reviewAnalyst; }
    public void setReviewAnalyst(String reviewAnalyst) { this.reviewAnalyst = reviewAnalyst; }

    public String getReviewAnalystEmail() { return reviewAnalystEmail; }
    public void setReviewAnalystEmail(String reviewAnalystEmail) { this.reviewAnalystEmail = reviewAnalystEmail; }

    public String getProposedRating() { return proposedRating; }
    public void setProposedRating(String proposedRating) { this.proposedRating = proposedRating; }

    public String getCountryOfRisk() { return countryOfRisk; }
    public void setCountryOfRisk(String countryOfRisk) { this.countryOfRisk = countryOfRisk; }

    public String getPurposeOfSubmission() { return purposeOfSubmission; }
    public void setPurposeOfSubmission(String purposeOfSubmission) { this.purposeOfSubmission = purposeOfSubmission; }

    public String getMeetingName() { return meetingName; }
    public void setMeetingName(String meetingName) { this.meetingName = meetingName; }

}
