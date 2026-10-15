package za.co.rmb.approvalforum.docgen.model;

public class Approval {

    private String approverName;
    private String approverDecision;
    private String decisionDate;
    private String category;
    private String conditionsOfApproval;
    private String approvalMandate;

    public Approval() { }

    public String getApproverName() {
        return this.approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproverDecision() {
        return this.approverDecision;
    }

    public void setApproverDecision(String approverDecision) {
        this.approverDecision = approverDecision;
    }

    public String getDecisionDate() {
        return this.decisionDate;
    }

    public void setDecisionDate(String decisionDate) {
        this.decisionDate = decisionDate;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getConditionsOfApproval() {
        return this.conditionsOfApproval;
    }

    public void setConditionsOfApproval(String conditionsOfApproval) {
        this.conditionsOfApproval = conditionsOfApproval;
    }

    public String getApprovalMandate() {
        return approvalMandate;
    }

    public void setApprovalMandate(String approvalMandate) {
        this.approvalMandate = approvalMandate;
    }

}