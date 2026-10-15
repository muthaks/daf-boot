package za.co.rmb.approvalforum.model.lumos;

import lombok.Data;

/**
 * @author khg
 */
@Data
public class WorkflowResponse {
    private int id;
    private String status;
    private String recordStatus;
    private String  workflowDecision;
    private int clientId;
    private String clientName;
    private String submittedBy;
    private String submittedByName;
    private String submittedByEmail;
    private String submittedOn;
    private String reviewedBy;
    private String reviewedByName;
    private String reviewedByEmail;
    private String reviewedOn;
    private String decisionCapturedBy;
    private String decisionCapturedByName;
    private String decisionCapturedByEmail;
    private String decisionCapturedOn;
    private String decisionApprovedBy;
    private String decisionApprovedByName;
    private String decisionApprovedByEmail;
    private String decisionApprovedOn;
    private String conditionsCapturedBy;
    private String conditionsCapturedByName;
    private String conditionsCapturedByEmail;
    private String conditionsCapturedOn;
    private String conditionsReviewedBy;
    private String conditionsReviewedByName;
    private String conditionsReviewedByEmail;
    private String conditionsReviewedOn;
}
