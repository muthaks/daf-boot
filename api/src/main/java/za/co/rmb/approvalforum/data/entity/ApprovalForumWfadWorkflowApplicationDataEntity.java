package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFAD_WorkflowApplicationData")
public class ApprovalForumWfadWorkflowApplicationDataEntity {

    private int wfadId;
    private String lastApprovedFrRating;
    private String proposedFrRating;
    private Double lastApprovedDcp;
    private Double lastApprovedSettlementDcp;
    private Double proposedDcp;
    private Double proposedSettlementDcp;
    private String distressStatus;
    private String wholesaleCreditPortfolio;
    private String meetingName;
    private String proposalType;
    private String proposalAction;
    private String reviewAnalyst;
    private String reviewAnalystId;
    private String reviewAnalystEmail;
    private String creditAnalyst;
    private String creditAnalystId;
    private String creditAnalystEmail;
    private int applicationId;
    private Timestamp reviewAnalystSignatureDate;
    private Timestamp creditAnalystSignatureDate;
    private Boolean lumos;

    //private Integer tenantId;

    private ApprovalForumWfcdWorkflowCounterpartyDetailEntity wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId;

    @Id
    @Column(name = "WFAD_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfadId() {
        return wfadId;
    }

    public void setWfadId(int wfadId) {
        this.wfadId = wfadId;
    }

    @Basic
    @Column(name = "LastApprovedFrRating", nullable = true, length = 100)
    public String getLastApprovedFrRating() {
        return lastApprovedFrRating;
    }

    public void setLastApprovedFrRating(String lastApprovedFrRating) {
        this.lastApprovedFrRating = lastApprovedFrRating;
    }

    @Basic
    @Column(name = "ProposedFrRating", nullable = true, length = 100)
    public String getProposedFrRating() {
        return proposedFrRating;
    }

    public void setProposedFrRating(String proposedFrRating) {
        this.proposedFrRating = proposedFrRating;
    }

    @Basic
    @Column(name = "LastApprovedDcp", nullable = true, precision = 0)
    public Double getLastApprovedDcp() {
        return lastApprovedDcp;
    }

    public void setLastApprovedDcp(Double lastApprovedDcp) {
        this.lastApprovedDcp = lastApprovedDcp;
    }

    @Basic
    @Column(name = "LastApprovedSettlementDcp", nullable = true, precision = 0)
    public Double getLastApprovedSettlementDcp() {
        return lastApprovedSettlementDcp;
    }

    public void setLastApprovedSettlementDcp(Double lastApprovedSettlementDcp) {
        this.lastApprovedSettlementDcp = lastApprovedSettlementDcp;
    }

    @Basic
    @Column(name = "ProposedDcp", nullable = true, precision = 0)
    public Double getProposedDcp() {
        return proposedDcp;
    }

    public void setProposedDcp(Double proposedDcp) {
        this.proposedDcp = proposedDcp;
    }

    @Basic
    @Column(name = "ProposedSettlementDcp", nullable = true, precision = 0)
    public Double getProposedSettlementDcp() {
        return proposedSettlementDcp;
    }

    public void setProposedSettlementDcp(Double proposedSettlementDcp) {
        this.proposedSettlementDcp = proposedSettlementDcp;
    }

    @Basic
    @Column(name = "DistressStatus", nullable = true, length = 255)
    public String getDistressStatus() {
        return distressStatus;
    }

    public void setDistressStatus(String distressStatus) {
        this.distressStatus = distressStatus;
    }

    @Basic
    @Column(name = "WholesaleCreditPortfolio", nullable = true, length = 255)
    public String getWholesaleCreditPortfolio() {
        return wholesaleCreditPortfolio;
    }

    public void setWholesaleCreditPortfolio(String wholesaleCreditPortfolio) {
        this.wholesaleCreditPortfolio = wholesaleCreditPortfolio;
    }

    @Basic
    @Column(name = "MeetingName", nullable = true, length = 255)
    public String getMeetingName() {
        return meetingName;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    @Basic
    @Column(name = "ProposalType", nullable = true, length = 255)
    public String getProposalType() {
        return proposalType;
    }

    public void setProposalType(String proposalType) {
        this.proposalType = proposalType;
    }

    @Basic
    @Column(name = "ProposalAction", nullable = true, length = 255)
    public String getProposalAction() {
        return proposalAction;
    }

    public void setProposalAction(String proposalAction) {
        this.proposalAction = proposalAction;
    }

    @Basic
    @Column(name = "ReviewAnalyst", nullable = true, length = 255)
    public String getReviewAnalyst() {
        return reviewAnalyst;
    }

    public void setReviewAnalyst(String reviewAnalyst) {
        this.reviewAnalyst = reviewAnalyst;
    }

    @Basic
    @Column(name = "ReviewAnalystId", nullable = true, length = 255)
    public String getReviewAnalystId() {
        return reviewAnalystId;
    }

    public void setReviewAnalystId(String reviewAnalystId) {
        this.reviewAnalystId = reviewAnalystId;
    }

    @Basic
    @Column(name = "ReviewAnalystEmail", nullable = true, length = 255)
    public String getReviewAnalystEmail() {
        return reviewAnalystEmail;
    }

    public void setReviewAnalystEmail(String reviewAnalystEmail) {
        this.reviewAnalystEmail = reviewAnalystEmail;
    }

    @Basic
    @Column(name = "CreditAnalyst", nullable = true, length = 255)
    public String getCreditAnalyst() {
        return creditAnalyst;
    }

    public void setCreditAnalyst(String creditAnalyst) {
        this.creditAnalyst = creditAnalyst;
    }

    @Basic
    @Column(name = "CreditAnalystId", nullable = true, length = 255)
    public String getCreditAnalystId() {
        return creditAnalystId;
    }

    public void setCreditAnalystId(String creditAnalystId) {
        this.creditAnalystId = creditAnalystId;
    }

    @Basic
    @Column(name = "CreditAnalystEmail", nullable = true, length = 255)
    public String getCreditAnalystEmail() {
        return creditAnalystEmail;
    }

    public void setCreditAnalystEmail(String creditAnalystEmail) {
        this.creditAnalystEmail = creditAnalystEmail;
    }

    @Basic
    @Column(name = "ApplicationId", nullable = false)
    public int getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    @Basic
    @Column(name = "ReviewAnalystSignatureDate", nullable = true)
    public Timestamp getReviewAnalystSignatureDate() {
        return reviewAnalystSignatureDate;
    }

    public void setReviewAnalystSignatureDate(Timestamp reviewAnalystSignatureDate) {
        this.reviewAnalystSignatureDate = reviewAnalystSignatureDate;
    }

    @Basic
    @Column(name = "CreditAnalystSignatureDate", nullable = true)
    public Timestamp getCreditAnalystSignatureDate() {
        return creditAnalystSignatureDate;
    }

    public void setCreditAnalystSignatureDate(Timestamp creditAnalystSignatureDate) {
        this.creditAnalystSignatureDate = creditAnalystSignatureDate;
    }

    @Basic
    @Column(name = "IsLumos", nullable = false)
    public Boolean getLumos() {
        return lumos;
    }

    public void setLumos(Boolean lumos) {
        this.lumos = lumos;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfadWorkflowApplicationDataEntity that = (ApprovalForumWfadWorkflowApplicationDataEntity) o;
        return wfadId == that.wfadId &&
                Objects.equals(lastApprovedFrRating, that.lastApprovedFrRating) &&
                Objects.equals(proposedFrRating, that.proposedFrRating) &&
                Objects.equals(lastApprovedDcp, that.lastApprovedDcp) &&
                Objects.equals(lastApprovedSettlementDcp, that.lastApprovedSettlementDcp) &&
                Objects.equals(proposedDcp, that.proposedDcp) &&
                Objects.equals(proposedSettlementDcp, that.proposedSettlementDcp) &&
                Objects.equals(distressStatus, that.distressStatus) &&
                Objects.equals(wholesaleCreditPortfolio, that.wholesaleCreditPortfolio) &&
                Objects.equals(meetingName, that.meetingName) &&
                Objects.equals(proposalType, that.proposalType) &&
                Objects.equals(proposalAction, that.proposalAction) &&
                Objects.equals(reviewAnalyst, that.reviewAnalyst) &&
                Objects.equals(reviewAnalystId, that.reviewAnalystId) &&
                Objects.equals(reviewAnalystEmail, that.reviewAnalystEmail) &&
                Objects.equals(creditAnalyst, that.creditAnalyst) &&
                Objects.equals(creditAnalystId, that.creditAnalystId) &&
                Objects.equals(creditAnalystEmail, that.creditAnalystEmail) &&
                applicationId == that.applicationId &&
                Objects.equals(reviewAnalystSignatureDate, that.reviewAnalystSignatureDate) &&
                Objects.equals(creditAnalystSignatureDate, that.creditAnalystSignatureDate) &&
                lumos == that.lumos ;
                //&& Objects.equals(tenantId, that.tenantId);

    }

    @Override
    public int hashCode() {
        return Objects.hash(wfadId, lastApprovedFrRating, proposedFrRating, lastApprovedDcp, lastApprovedSettlementDcp, proposedDcp,
                proposedSettlementDcp, distressStatus, wholesaleCreditPortfolio, meetingName, proposalType, proposalAction, reviewAnalyst,
                reviewAnalystId, reviewAnalystEmail, creditAnalyst,
                creditAnalystId, creditAnalystEmail, applicationId, reviewAnalystSignatureDate, creditAnalystSignatureDate, lumos);
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WFCD_ID", referencedColumnName = "WFCD_ID", nullable = false)
    public ApprovalForumWfcdWorkflowCounterpartyDetailEntity getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId() {
        return wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId;
    }

    public void setWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId(ApprovalForumWfcdWorkflowCounterpartyDetailEntity wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId) {
        this.wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId = wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId;
    }

}
