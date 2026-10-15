package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFA_WorkflowApprovals")
public class ApprovalForumWfaWorkflowApprovalsEntity {

    private int wfaId;
    private String userId;
    private String name;
    private String surname;
    private String email;
    private String title;
    private String domain;
    private Timestamp dateAssigned;
    private Timestamp dateCompleted;
    private String decision;
    private boolean onlyForDistribution;
    private boolean specificApprover;
    private boolean additionalApprover;
    private boolean chairpersonApprover;
    private String approvalConditions;
    private String recommendedPolicyApprovalMandate;
    private int lastSeenChatId;

    private ApprovalForumAflmmApprovalForumListManagementMappingEntity aflmmApprovalForumListManagementMappingByAflmmId;
    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;

    @Id
    @Column(name = "WFA_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfaId() {
        return wfaId;
    }

    public void setWfaId(int wfaId) {
        this.wfaId = wfaId;
    }

    @Basic
    @Column(name = "UserId", nullable = false, length = 255)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "Name", nullable = true, length = 255)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "Surname", nullable = true, length = 255)
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Basic
    @Column(name = "Email", nullable = true, length = 255)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Basic
    @Column(name = "Title", nullable = true, length = 100)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    @Column(name = "Domain", nullable = true, length = 100)
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Basic
    @Column(name = "DateAssigned", nullable = true)
    public Timestamp getDateAssigned() {
        return dateAssigned;
    }

    public void setDateAssigned(Timestamp dateAssigned) {
        this.dateAssigned = dateAssigned;
    }

    @Basic
    @Column(name = "DateCompleted", nullable = true)
    public Timestamp getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Timestamp dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    @Basic
    @Column(name = "Decision", nullable = true, length = 100)
    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    @Basic
    @Column(name = "OnlyForDistribution", nullable = false)
    public boolean isOnlyForDistribution() {
        return onlyForDistribution;
    }

    public void setOnlyForDistribution(boolean onlyForDistribution) {
        this.onlyForDistribution = onlyForDistribution;
    }

    @Basic
    @Column(name = "SpecificApprover", nullable = false)
    public boolean isSpecificApprover() {
        return specificApprover;
    }

    public void setSpecificApprover(boolean specificApprover) {
        this.specificApprover = specificApprover;
    }

    @Basic
    @Column(name = "AdditionalApprover", nullable = false)
    public boolean isAdditionalApprover() {
        return additionalApprover;
    }

    public void setAdditionalApprover(boolean additionalApprover) {
        this.additionalApprover = additionalApprover;
    }

    @Basic
    @Column(name = "ApprovalConditions", nullable = true, length = 2147483647)
    public String getApprovalConditions() {
        return approvalConditions;
    }

    public void setApprovalConditions(String approvalConditions) {
        this.approvalConditions = approvalConditions;
    }

    @Basic
    @Column(name = "RecommendedPolicyApprovalMandate", nullable = true, length = 255)
    public String getRecommendedPolicyApprovalMandate() {
        return recommendedPolicyApprovalMandate;
    }

    public void setRecommendedPolicyApprovalMandate(String recommendedPolicyApprovalMandate) {
        this.recommendedPolicyApprovalMandate = recommendedPolicyApprovalMandate;
    }

    @Basic
    @Column(name = "LastSeenChatId", nullable = false)
    public int getLastSeenChatId() {
        return lastSeenChatId;
    }

    public void setLastSeenChatId(int lastSeenChatId) {
        this.lastSeenChatId = lastSeenChatId;
    }

    @Basic
    @Column(name = "ChairpersonApprover", nullable = false)
    public boolean isChairpersonApprover() {
        return chairpersonApprover;
    }

    public void setChairpersonApprover(boolean chairpersonApprover) {
        this.chairpersonApprover = chairpersonApprover;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfaWorkflowApprovalsEntity that = (ApprovalForumWfaWorkflowApprovalsEntity) o;
        return wfaId == that.wfaId &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(surname, that.surname) &&
                Objects.equals(email, that.email) &&
                Objects.equals(title, that.title) &&
                Objects.equals(domain, that.domain) &&
                Objects.equals(dateAssigned, that.dateAssigned) &&
                Objects.equals(dateCompleted, that.dateCompleted) &&
                Objects.equals(decision, that.decision) &&
                onlyForDistribution == that.onlyForDistribution &&
                specificApprover == that.specificApprover &&
                additionalApprover == that.additionalApprover &&
                Objects.equals(approvalConditions, that.approvalConditions) &&
                Objects.equals(recommendedPolicyApprovalMandate, that.recommendedPolicyApprovalMandate) &&
                lastSeenChatId == that.lastSeenChatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfaId, userId, name, surname, email, title, domain, dateAssigned, dateCompleted, decision, onlyForDistribution,
                specificApprover, additionalApprover, approvalConditions, recommendedPolicyApprovalMandate, lastSeenChatId);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AFLMM_ID", referencedColumnName = "AFLMM_ID")
    public ApprovalForumAflmmApprovalForumListManagementMappingEntity getAflmmApprovalForumListManagementMappingByAflmmId() {
        return aflmmApprovalForumListManagementMappingByAflmmId;
    }

    public void setAflmmApprovalForumListManagementMappingByAflmmId(ApprovalForumAflmmApprovalForumListManagementMappingEntity aflmmApprovalForumListManagementMappingByAflmmId) {
        this.aflmmApprovalForumListManagementMappingByAflmmId = aflmmApprovalForumListManagementMappingByAflmmId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WF_ID", referencedColumnName = "WF_ID", nullable = false)
    public ApprovalForumWfWorkflowEntity getWfApprovalForumWorkflowByWfId() {
        return wfApprovalForumWorkflowByWfId;
    }

    public void setWfApprovalForumWorkflowByWfId(ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId) {
        this.wfApprovalForumWorkflowByWfId = wfApprovalForumWorkflowByWfId;
    }

}
