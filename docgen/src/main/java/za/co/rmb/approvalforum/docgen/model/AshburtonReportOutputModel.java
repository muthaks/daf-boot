package za.co.rmb.approvalforum.docgen.model;

import java.util.List;

public class AshburtonReportOutputModel {

    private String currentDate;
    private String proposalTypeAndAction;

    // Application Summary
    private String counterparty;
    private String meetingName;
    private String recommendedMandate;
    private String wholesaleCreditPortfolio;
    private String distressedStatus;
    private String confidentialStatus;
    private String submittedBy;
    private String submittedDate;
    private String workflowId;
    private String casdmCreditAnalyst;
    private String casdmCaSubmittedDate;
    private String casdmReviewAnalyst;
    private String casdmRaSubmittedDate;
    private String kycStatus;

    // Ratings
    private String currentFrRating;
    private String proposedFrRating;

    // Limit Requested
    private String proposedCPYLZar;
    private String approvedConvertedEZar;
    private String proposedConvertedEZar;
    private String approvedGovermentGZar;
    private String proposedGovermentGZar;
    private String approvedJuniorDZar;
    private String proposedJuniorDZar;
    private String approvedLiquidationZar;
    private String proposedLiquidationZar;
    private String approvedMezzanineDZar;
    private String proposedMezzanineDZar;
    private String approvedPreferredSZar;
    private String proposedPreferredSZar;
    private String approvedSeniorSZar;
    private String proposedSeniorSZar;
    private String approvedSeniorUZar;
    private String proposedSeniorUZar;
    private String approvedSubordinatedSZar;
    private String proposedSubordinatedSZar;
    private String approvedSubordinatedUZar;
    private String proposedSubordinatedUZar;

    //Fund Percentages Limits
    private String approvedLDIZar;
    private String proposedLDIZar;
    private String approvedFixedIZar;
    private String proposedFixedIZar;
    private String approvedCreditZar;
    private String proposedCreditZar;
    private String approvedACEFZar;
    private String proposedACEFZar;
    private String approvedMultiMZar;
    private String proposedMultiMZar;

    // Recommendation
    private String recommendation;

    private String approvalQuorumNumber;

    // Specific Approvers
    private String specificApproversCheckbox;
    private String specificApproversReason;

    private String overallRulesEngineResultCreditDecision;
    private String conditionsOfApproval;
    private String comments;

    private List<Approval> approvals;

    private List<ChatHistory> chatHistory;

    public AshburtonReportOutputModel() { }

    public String getProposalTypeAndAction() {
        return this.proposalTypeAndAction;
    } 

    public void setProposalTypeAndAction(String proposalTypeAndAction) {
        this.proposalTypeAndAction = proposalTypeAndAction;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public String getRecommendedMandate() {
        return recommendedMandate;
    }

    public void setRecommendedMandate(String recommendedMandate) {
        this.recommendedMandate = recommendedMandate;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getWholesaleCreditPortfolio() {
        return wholesaleCreditPortfolio;
    }

    public void setWholesaleCreditPortfolio(String wholesaleCreditPortfolio) {
        this.wholesaleCreditPortfolio = wholesaleCreditPortfolio;
    }

    public String getDistressedStatus() {
        return distressedStatus;
    }

    public void setDistressedStatus(String distressedStatus) {
        this.distressedStatus = distressedStatus;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getCasdmCreditAnalyst() {
        return casdmCreditAnalyst;
    }

    public void setCasdmCreditAnalyst(String casdmCreditAnalyst) {
        this.casdmCreditAnalyst = casdmCreditAnalyst;
    }

    public String getCasdmCaSubmittedDate() {
        return casdmCaSubmittedDate;
    }

    public void setCasdmCaSubmittedDate(String casdmCaSubmittedDate) {
        this.casdmCaSubmittedDate = casdmCaSubmittedDate;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getCasdmReviewAnalyst() {
        return casdmReviewAnalyst;
    }

    public void setCasdmReviewAnalyst(String casdmReviewAnalyst) {
        this.casdmReviewAnalyst = casdmReviewAnalyst;
    }

    public String getCasdmRaSubmittedDate() {
        return casdmRaSubmittedDate;
    }

    public void setCasdmRaSubmittedDate(String casdmRaSubmittedDate) {
        this.casdmRaSubmittedDate = casdmRaSubmittedDate;
    }

    public String getConfidentialStatus() {
        return confidentialStatus;
    }

    public void setConfidentialStatus(String confidentialStatus) {
        this.confidentialStatus = confidentialStatus;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(String submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getCurrentFrRating() {
        return currentFrRating;
    }

    public void setCurrentFrRating(String currentFrRating) {
        this.currentFrRating = currentFrRating;
    }

    public String getProposedFrRating() {
        return proposedFrRating;
    }

    public void setProposedFrRating(String proposedFrRating) {
        this.proposedFrRating = proposedFrRating;
    }

    public String getProposedCPYLZar() {
        return proposedCPYLZar;
    }

    public void setProposedCPYLZar(String proposedCPYLZar) {
        this.proposedCPYLZar = proposedCPYLZar;
    }

    public String getApprovedConvertedEZar() {
        return approvedConvertedEZar;
    }

    public void setApprovedConvertedEZar(String approvedConvertedEZar) {
        this.approvedConvertedEZar = approvedConvertedEZar;
    }

    public String getProposedConvertedEZar() {
        return proposedConvertedEZar;
    }

    public void setProposedConvertedEZar(String proposedConvertedEZar) {
        this.proposedConvertedEZar = proposedConvertedEZar;
    }

    public String getApprovedGovermentGZar() {
        return approvedGovermentGZar;
    }

    public void setApprovedGovermentGZar(String approvedGovermentGZar) {
        this.approvedGovermentGZar = approvedGovermentGZar;
    }

    public String getProposedGovermentGZar() {
        return proposedGovermentGZar;
    }

    public void setProposedGovermentGZar(String proposedGovermentGZar) {
        this.proposedGovermentGZar = proposedGovermentGZar;
    }

    public String getApprovedJuniorDZar() {
        return approvedJuniorDZar;
    }

    public void setApprovedJuniorDZar(String approvedJuniorDZar) {
        this.approvedJuniorDZar = approvedJuniorDZar;
    }

    public String getProposedJuniorDZar() {
        return proposedJuniorDZar;
    }

    public void setProposedJuniorDZar(String proposedJuniorDZar) {
        this.proposedJuniorDZar = proposedJuniorDZar;
    }

    public String getApprovedLiquidationZar() {
        return approvedLiquidationZar;
    }

    public void setApprovedLiquidationZar(String approvedLiquidationZar) {
        this.approvedLiquidationZar = approvedLiquidationZar;
    }

    public String getProposedLiquidationZar() {
        return proposedLiquidationZar;
    }

    public void setProposedLiquidationZar(String proposedLiquidationZar) {
        this.proposedLiquidationZar = proposedLiquidationZar;
    }

    public String getApprovedMezzanineDZar() {
        return approvedMezzanineDZar;
    }

    public void setApprovedMezzanineDZar(String approvedMezzanineDZar) {
        this.approvedMezzanineDZar = approvedMezzanineDZar;
    }

    public String getProposedMezzanineDZar() {
        return proposedMezzanineDZar;
    }

    public void setProposedMezzanineDZar(String proposedMezzanineDZar) {
        this.proposedMezzanineDZar = proposedMezzanineDZar;
    }

    public String getApprovedPreferredSZar() {
        return approvedPreferredSZar;
    }

    public void setApprovedPreferredSZar(String approvedPreferredSZar) {
        this.approvedPreferredSZar = approvedPreferredSZar;
    }

    public String getProposedPreferredSZar() {
        return proposedPreferredSZar;
    }

    public void setProposedPreferredSZar(String proposedPreferredSZar) {
        this.proposedPreferredSZar = proposedPreferredSZar;
    }

    public String getApprovedSeniorSZar() {
        return approvedSeniorSZar;
    }

    public void setApprovedSeniorSZar(String approvedSeniorSZar) {
        this.approvedSeniorSZar = approvedSeniorSZar;
    }

    public String getProposedSeniorSZar() {
        return proposedSeniorSZar;
    }

    public void setProposedSeniorSZar(String proposedSeniorSZar) {
        this.proposedSeniorSZar = proposedSeniorSZar;
    }

    public String getApprovedSeniorUZar() {
        return approvedSeniorUZar;
    }

    public void setApprovedSeniorUZar(String approvedSeniorUZar) {
        this.approvedSeniorUZar = approvedSeniorUZar;
    }

    public String getProposedSeniorUZar() {
        return proposedSeniorUZar;
    }

    public void setProposedSeniorUZar(String proposedSeniorUZar) {
        this.proposedSeniorUZar = proposedSeniorUZar;
    }

    public String getApprovedSubordinatedSZar() {
        return approvedSubordinatedSZar;
    }

    public void setApprovedSubordinatedSZar(String approvedSubordinatedSZar) {
        this.approvedSubordinatedSZar = approvedSubordinatedSZar;
    }

    public String getProposedSubordinatedSZar() {
        return proposedSubordinatedSZar;
    }

    public void setProposedSubordinatedSZar(String proposedSubordinatedSZar) {
        this.proposedSubordinatedSZar = proposedSubordinatedSZar;
    }

    public String getApprovedSubordinatedUZar() {
        return approvedSubordinatedUZar;
    }

    public void setApprovedSubordinatedUZar(String approvedSubordinatedUZar) {
        this.approvedSubordinatedUZar = approvedSubordinatedUZar;
    }

    public String getProposedSubordinatedUZar() {
        return proposedSubordinatedUZar;
    }

    public void setProposedSubordinatedUZar(String proposedSubordinatedUZar) {
        this.proposedSubordinatedUZar = proposedSubordinatedUZar;
    }

    public String getApprovedLDIZar() {
        return approvedLDIZar;
    }

    public void setApprovedLDIZar(String approvedLDIZar) {
        this.approvedLDIZar = approvedLDIZar;
    }

    public String getProposedLDIZar() {
        return proposedLDIZar;
    }

    public void setProposedLDIZar(String proposedLDIZar) {
        this.proposedLDIZar = proposedLDIZar;
    }

    public String getApprovedFixedIZar() {
        return approvedFixedIZar;
    }

    public void setApprovedFixedIZar(String approvedFixedIZar) {
        this.approvedFixedIZar = approvedFixedIZar;
    }

    public String getProposedFixedIZar() {
        return proposedFixedIZar;
    }

    public void setProposedFixedIZar(String proposedFixedIZar) {
        this.proposedFixedIZar = proposedFixedIZar;
    }

    public String getApprovedCreditZar() {
        return approvedCreditZar;
    }

    public void setApprovedCreditZar(String approvedCreditZar) {
        this.approvedCreditZar = approvedCreditZar;
    }

    public String getProposedCreditZar() {
        return proposedCreditZar;
    }

    public void setProposedCreditZar(String proposedCreditZar) {
        this.proposedCreditZar = proposedCreditZar;
    }

    public String getApprovedACEFZar() {
        return approvedACEFZar;
    }

    public void setApprovedACEFZar(String approvedACEFZar) {
        this.approvedACEFZar = approvedACEFZar;
    }

    public String getProposedACEFZar() {
        return proposedACEFZar;
    }

    public void setProposedACEFZar(String proposedACEFZar) {
        this.proposedACEFZar = proposedACEFZar;
    }

    public String getApprovedMultiMZar() {
        return approvedMultiMZar;
    }

    public void setApprovedMultiMZar(String approvedMultiMZar) {
        this.approvedMultiMZar = approvedMultiMZar;
    }

    public String getProposedMultiMZar() {
        return proposedMultiMZar;
    }

    public void setProposedMultiMZar(String proposedMultiMZar) {
        this.proposedMultiMZar = proposedMultiMZar;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getApprovalQuorumNumber() {
        return approvalQuorumNumber;
    }

    public void setApprovalQuorumNumber(String approvalQuorumNumber) {
        this.approvalQuorumNumber = approvalQuorumNumber;
    }

    public String getSpecificApproversCheckbox() {
        return specificApproversCheckbox;
    }

    public void setSpecificApproversCheckbox(String specificApproversCheckbox) {
        this.specificApproversCheckbox = specificApproversCheckbox;
    }

    public String getSpecificApproversReason() {
        return specificApproversReason;
    }

    public void setSpecificApproversReason(String specificApproversReason) {
        this.specificApproversReason = specificApproversReason;
    }

    public String getOverallRulesEngineResultCreditDecision() {
        return overallRulesEngineResultCreditDecision;
    }

    public void setOverallRulesEngineResultCreditDecision(String overallRulesEngineResultCreditDecision) {
        this.overallRulesEngineResultCreditDecision = overallRulesEngineResultCreditDecision;
    }

    public String getConditionsOfApproval() {
        return conditionsOfApproval;
    }

    public void setConditionsOfApproval(String conditionsOfApproval) {
        this.conditionsOfApproval = conditionsOfApproval;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<Approval> getApprovals() {
        return approvals;
    }

    public void setApprovals(List<Approval> approvals) {
        this.approvals = approvals;
    }   
    
    public List<ChatHistory> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatHistory> chatHistory) {
        this.chatHistory = chatHistory;
    }

}