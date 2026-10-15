package za.co.rmb.approvalforum.docgen.model;

import java.util.List;

public class ReportOutputModel {

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

    // Limit Request Summary
    private String approvedDcpZar;
    private String approvedSettZar;
    private String approvedUnderwritingZar;
    private String approvedChurnZar;
    private String approvedCreditFundZar;
    private String approvedTradedCreditZar;

    private String proposedDcpZar;
    private String proposedSettZar;
    private String proposedUnderwritingZar;
    private String proposedChurnZar;
    private String proposedCreditFundZar;
    private String proposedTradedCreditZar;

    private String movementDcpZar;
    private String movementSettZar;
    private String movementUnderwritingZar;
    private String movementChurnZar;
    private String movementCreditFundZar;
    private String movementTradedCreditZar;

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

    public ReportOutputModel() { }

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

    public String getApprovedDcpZar() {
        return approvedDcpZar;
    }

    public void setApprovedDcpZar(String approvedDcpZar) {
        this.approvedDcpZar = approvedDcpZar;
    }

    public String getApprovedSettZar() {
        return approvedSettZar;
    }

    public void setApprovedSettZar(String approvedSettZar) {
        this.approvedSettZar = approvedSettZar;
    }

    public String getApprovedUnderwritingZar() {
        return approvedUnderwritingZar;
    }

    public void setApprovedUnderwritingZar(String approvedUnderwritingZar) {
        this.approvedUnderwritingZar = approvedUnderwritingZar;
    }

    public String getApprovedChurnZar() {
        return approvedChurnZar;
    }

    public void setApprovedChurnZar(String approvedChurnZar) {
        this.approvedChurnZar = approvedChurnZar;
    }

    public String getApprovedCreditFundZar() {
        return approvedCreditFundZar;
    }

    public void setApprovedCreditFundZar(String approvedCreditFundZar) {
        this.approvedCreditFundZar = approvedCreditFundZar;
    }

    public String getApprovedTradedCreditZar() {
        return approvedTradedCreditZar;
    }

    public void setApprovedTradedCreditZar(String approvedTradedCreditZar) {
        this.approvedTradedCreditZar = approvedTradedCreditZar;
    }

    public String getProposedDcpZar() {
        return proposedDcpZar;
    }

    public void setProposedDcpZar(String proposedDcpZar) {
        this.proposedDcpZar = proposedDcpZar;
    }

    public String getProposedSettZar() {
        return proposedSettZar;
    }

    public void setProposedSettZar(String proposedSettZar) {
        this.proposedSettZar = proposedSettZar;
    }

    public String getProposedUnderwritingZar() {
        return proposedUnderwritingZar;
    }

    public void setProposedUnderwritingZar(String proposedUnderwritingZar) {
        this.proposedUnderwritingZar = proposedUnderwritingZar;
    }

    public String getProposedChurnZar() {
        return proposedChurnZar;
    }

    public void setProposedChurnZar(String proposedChurnZar) {
        this.proposedChurnZar = proposedChurnZar;
    }

    public String getProposedCreditFundZar() {
        return proposedCreditFundZar;
    }

    public void setProposedCreditFundZar(String proposedCreditFundZar) {
        this.proposedCreditFundZar = proposedCreditFundZar;
    }

    public String getProposedTradedCreditZar() {
        return proposedTradedCreditZar;
    }

    public void setProposedTradedCreditZar(String proposedTradedCreditZar) {
        this.proposedTradedCreditZar = proposedTradedCreditZar;
    }

    public String getMovementDcpZar() {
        return movementDcpZar;
    }

    public void setMovementDcpZar(String movementDcpZar) {
        this.movementDcpZar = movementDcpZar;
    }

    public String getMovementSettZar() {
        return movementSettZar;
    }

    public void setMovementSettZar(String movementSettZar) {
        this.movementSettZar = movementSettZar;
    }

    public String getMovementUnderwritingZar() {
        return movementUnderwritingZar;
    }

    public void setMovementUnderwritingZar(String movementUnderwritingZar) {
        this.movementUnderwritingZar = movementUnderwritingZar;
    }

    public String getMovementChurnZar() {
        return movementChurnZar;
    }

    public void setMovementChurnZar(String movementChurnZar) {
        this.movementChurnZar = movementChurnZar;
    }

    public String getMovementCreditFundZar() {
        return movementCreditFundZar;
    }

    public void setMovementCreditFundZar(String movementCreditFundZar) {
        this.movementCreditFundZar = movementCreditFundZar;
    }

    public String getMovementTradedCreditZar() {
        return movementTradedCreditZar;
    }

    public void setMovementTradedCreditZar(String movementTradedCreditZar) {
        this.movementTradedCreditZar = movementTradedCreditZar;
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