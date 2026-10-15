package za.co.rmb.approvalforum.model.lumos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowDTO {
        private int id;
        private String status;
        private String recordStatus;
        private String workflowDecision;

        private int clientId;
        private String clientName;

        private String submittedBy;
        private String submittedByName;
        private String submittedByEmail;
        private LocalDateTime submittedOn;

        private String reviewedBy;
        private String reviewedByName;
        private String reviewedByEmail;
        private LocalDateTime reviewedOn;

        private String decisionCapturedBy;
        private String decisionCapturedByName;
        private String decisionCapturedByEmail;
        private LocalDateTime decisionCapturedOn;

        private String decisionApprovedBy;
        private String decisionApprovedByName;
        private String decisionApprovedByEmail;
        private LocalDateTime decisionApprovedOn;

        private String conditionsCapturedBy;
        private String conditionsCapturedByName;
        private String conditionsCapturedByEmail;
        private LocalDateTime conditionsCapturedOn;

        private String conditionsReviewedBy;
        private String conditionsReviewedByName;
        private String conditionsReviewedByEmail;
        private LocalDateTime conditionsReviewedOn;

        private String type;

        public int getId() {
                return id;
        }

        public void setId(int id) {
                this.id = id;
        }

        public String getStatus() {
                return status;
        }

        public void setStatus(String status) {
                this.status = status;
        }

        public String getRecordStatus() {
                return recordStatus;
        }

        public void setRecordStatus(String recordStatus) {
                this.recordStatus = recordStatus;
        }

        public String getWorkflowDecision() {
                return workflowDecision;
        }

        public void setWorkflowDecision(String workflowDecision) {
                this.workflowDecision = workflowDecision;
        }

        public int getClientId() {
                return clientId;
        }

        public void setClientId(int clientId) {
                this.clientId = clientId;
        }

        public String getClientName() {
                return clientName;
        }

        public void setClientName(String clientName) {
                this.clientName = clientName;
        }

        public String getSubmittedBy() {
                return submittedBy;
        }

        public void setSubmittedBy(String submittedBy) {
                this.submittedBy = submittedBy;
        }

        public String getSubmittedByName() {
                return submittedByName;
        }

        public void setSubmittedByName(String submittedByName) {
                this.submittedByName = submittedByName;
        }

        public String getSubmittedByEmail() {
                return submittedByEmail;
        }

        public void setSubmittedByEmail(String submittedByEmail) {
                this.submittedByEmail = submittedByEmail;
        }

        public LocalDateTime getSubmittedOn() {
                return submittedOn;
        }

        public void setSubmittedOn(LocalDateTime submittedOn) {
                this.submittedOn = submittedOn;
        }

        public String getReviewedBy() {
                return reviewedBy;
        }

        public void setReviewedBy(String reviewedBy) {
                this.reviewedBy = reviewedBy;
        }

        public String getReviewedByName() {
                return reviewedByName;
        }

        public void setReviewedByName(String reviewedByName) {
                this.reviewedByName = reviewedByName;
        }

        public String getReviewedByEmail() {
                return reviewedByEmail;
        }

        public void setReviewedByEmail(String reviewedByEmail) {
                this.reviewedByEmail = reviewedByEmail;
        }

        public LocalDateTime getReviewedOn() {
                return reviewedOn;
        }

        public void setReviewedOn(LocalDateTime reviewedOn) {
                this.reviewedOn = reviewedOn;
        }

        public String getDecisionCapturedBy() {
                return decisionCapturedBy;
        }

        public void setDecisionCapturedBy(String decisionCapturedBy) {
                this.decisionCapturedBy = decisionCapturedBy;
        }

        public String getDecisionCapturedByName() {
                return decisionCapturedByName;
        }

        public void setDecisionCapturedByName(String decisionCapturedByName) {
                this.decisionCapturedByName = decisionCapturedByName;
        }

        public String getDecisionCapturedByEmail() {
                return decisionCapturedByEmail;
        }

        public void setDecisionCapturedByEmail(String decisionCapturedByEmail) {
                this.decisionCapturedByEmail = decisionCapturedByEmail;
        }

        public LocalDateTime getDecisionCapturedOn() {
                return decisionCapturedOn;
        }

        public void setDecisionCapturedOn(LocalDateTime decisionCapturedOn) {
                this.decisionCapturedOn = decisionCapturedOn;
        }

        public String getDecisionApprovedBy() {
                return decisionApprovedBy;
        }

        public void setDecisionApprovedBy(String decisionApprovedBy) {
                this.decisionApprovedBy = decisionApprovedBy;
        }

        public String getDecisionApprovedByName() {
                return decisionApprovedByName;
        }

        public void setDecisionApprovedByName(String decisionApprovedByName) {
                this.decisionApprovedByName = decisionApprovedByName;
        }

        public String getDecisionApprovedByEmail() {
                return decisionApprovedByEmail;
        }

        public void setDecisionApprovedByEmail(String decisionApprovedByEmail) {
                this.decisionApprovedByEmail = decisionApprovedByEmail;
        }

        public LocalDateTime getDecisionApprovedOn() {
                return decisionApprovedOn;
        }

        public void setDecisionApprovedOn(LocalDateTime decisionApprovedOn) {
                this.decisionApprovedOn = decisionApprovedOn;
        }

        public String getConditionsCapturedBy() {
                return conditionsCapturedBy;
        }

        public void setConditionsCapturedBy(String conditionsCapturedBy) {
                this.conditionsCapturedBy = conditionsCapturedBy;
        }

        public String getConditionsCapturedByName() {
                return conditionsCapturedByName;
        }

        public void setConditionsCapturedByName(String conditionsCapturedByName) {
                this.conditionsCapturedByName = conditionsCapturedByName;
        }

        public String getConditionsCapturedByEmail() {
                return conditionsCapturedByEmail;
        }

        public void setConditionsCapturedByEmail(String conditionsCapturedByEmail) {
                this.conditionsCapturedByEmail = conditionsCapturedByEmail;
        }

        public LocalDateTime getConditionsCapturedOn() {
                return conditionsCapturedOn;
        }

        public void setConditionsCapturedOn(LocalDateTime conditionsCapturedOn) {
                this.conditionsCapturedOn = conditionsCapturedOn;
        }

        public String getConditionsReviewedBy() {
                return conditionsReviewedBy;
        }

        public void setConditionsReviewedBy(String conditionsReviewedBy) {
                this.conditionsReviewedBy = conditionsReviewedBy;
        }

        public String getConditionsReviewedByName() {
                return conditionsReviewedByName;
        }

        public void setConditionsReviewedByName(String conditionsReviewedByName) {
                this.conditionsReviewedByName = conditionsReviewedByName;
        }

        public String getConditionsReviewedByEmail() {
                return conditionsReviewedByEmail;
        }

        public void setConditionsReviewedByEmail(String conditionsReviewedByEmail) {
                this.conditionsReviewedByEmail = conditionsReviewedByEmail;
        }

        public LocalDateTime getConditionsReviewedOn() {
                return conditionsReviewedOn;
        }

        public void setConditionsReviewedOn(LocalDateTime conditionsReviewedOn) {
                this.conditionsReviewedOn = conditionsReviewedOn;
        }

        public String getType() {
                return type;
        }

        public void setType(String type) {
                this.type = type;
        }

// Getters and setters (or use Lombok @Data for brevity)
    }

