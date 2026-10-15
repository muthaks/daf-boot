package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WF_Workflow")
@SqlResultSetMapping(name = "ApprovalForumWfWorkflowEntity",
        entities = {@EntityResult(entityClass = ApprovalForumWfWorkflowEntity.class)})
public class ApprovalForumWfWorkflowEntity {
    private int wfId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private boolean confidential;
    private String status;
    private boolean specific;
    private String specificReason;
    private String submittedBy;
    private Timestamp submittedOn;
    private Timestamp completedOn;
    private boolean firstEscalationSent;
    private boolean secondEscalationSent;
    private String assignedTo;
    private Timestamp assignedOn;
    private Integer referencedWorkfowId;
    private int caLastSeenChatId;
    private int raLastSeenChatId;
    private boolean chairpersonApprovalRequired;
    private String workflowSystemName;

    private ApprovalForumRtRequestTypeEntity rtRequestTypeByRtId;
    private ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataByWfdaId;
    private ApprovalForumWiprrWipRulesResponseEntity wiprrWipRulesResponseByWiprrId;
    private ApprovalForumWipurWipUsersResponseEntity wipurWipUsersResponseByWipurId;
    private Collection<ApprovalForumWfaWorkflowApprovalsEntity> wfaWorkflowApprovalsByWfId;
    private Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> wfcdWorkflowCounterpartyDetailByWfId;

    @Id
    @Column(name = "WF_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfId() {
        return wfId;
    }

    public void setWfId(int wfId) {
        this.wfId = wfId;
    }

    @Basic
    @Column(name = "CreatedBy", nullable = false, length = 255)
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Basic
    @Column(name = "CreatedOn", nullable = false)
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Basic
    @Column(name = "Active", nullable = false)
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Basic
    @Column(name = "Confidential", nullable = false)
    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    @Basic
    @Column(name = "Status", nullable = false, length = 255)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Basic
    @Column(name = "Specific", nullable = false)
    public boolean isSpecific() {
        return specific;
    }

    public void setSpecific(boolean specific) {
        this.specific = specific;
    }

    @Basic
    @Column(name = "SpecificReason", length = 2147483647)
    public String getSpecificReason() {
        return specificReason;
    }

    public void setSpecificReason(String specificReason) {
        this.specificReason = specificReason;
    }

    @Basic
    @Column(name = "SubmittedBy")
    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    @Basic
    @Column(name = "SubmittedOn")
    public Timestamp getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(Timestamp submittedOn) {
        this.submittedOn = submittedOn;
    }

    @Basic
    @Column(name = "CompletedOn")
    public Timestamp getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(Timestamp completedOn) {
        this.completedOn = completedOn;
    }

    @Basic
    @Column(name = "FirstEscalationSent", nullable = false)
    public boolean isFirstEscalationSent() {
        return firstEscalationSent;
    }

    public void setFirstEscalationSent(boolean firstEscalationSent) {
        this.firstEscalationSent = firstEscalationSent;
    }

    @Basic
    @Column(name = "SecondEscalationSent", nullable = false)
    public boolean isSecondEscalationSent() {
        return secondEscalationSent;
    }

    public void setSecondEscalationSent(boolean secondEscalationSent) {
        this.secondEscalationSent = secondEscalationSent;
    }

    @Basic
    @Column(name = "Assignedto")
    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Basic
    @Column(name = "AssignedOn")
    public Timestamp getAssignedOn() {
        return assignedOn;
    }

    public void setAssignedOn(Timestamp assignedOn) {
        this.assignedOn = assignedOn;
    }

    @Basic
    @Column(name = "ReferencedWorkflowId")
    public Integer getReferencedWorkfowId() {
        return referencedWorkfowId;
    }

    public void setReferencedWorkfowId(Integer referencedWorkfowId) {
        this.referencedWorkfowId = referencedWorkfowId;
    }

    @Basic
    @Column(name = "CaLastSeenChatId", nullable = false)
    public int getCaLastSeenChatId() {
        return caLastSeenChatId;
    }

    public void setCaLastSeenChatId(int caLastSeenChatId) {
        this.caLastSeenChatId = caLastSeenChatId;
    }

    @Basic
    @Column(name = "RaLastSeenChatId", nullable = false)
    public int getRaLastSeenChatId() {
        return raLastSeenChatId;
    }

    public void setRaLastSeenChatId(int raLastSeenChatId) {
        this.raLastSeenChatId = raLastSeenChatId;
    }

    @Basic
    @Column(name = "WorkFlowSystemName", nullable = false, columnDefinition = "varchar(10) default 'CASDM'")
    public String getWorkflowSystemName() {
        return workflowSystemName;
    }

    public void setWorkflowSystemName(String workflowSystemName) {
        this.workflowSystemName = workflowSystemName;
    }


    @Basic
    @Column(name = "ChairApprovalRequired", nullable = false)
    public boolean isChairpersonApprovalRequired() {
        return chairpersonApprovalRequired;
    }

    public void setChairpersonApprovalRequired(boolean chairpersonApprovalRequired) {
        this.chairpersonApprovalRequired = chairpersonApprovalRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfWorkflowEntity that = (ApprovalForumWfWorkflowEntity) o;
        return wfId == that.wfId &&
                active == that.active &&
                confidential == that.confidential &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(status, that.status) &&
                specific == that.specific &&
                Objects.equals(specificReason, that.specificReason) &&
                Objects.equals(submittedBy, that.submittedBy) &&
                Objects.equals(submittedOn, that.submittedOn) &&
                Objects.equals(completedOn, that.completedOn) &&
                firstEscalationSent == that.firstEscalationSent &&
                secondEscalationSent == that.secondEscalationSent &&
                Objects.equals(assignedTo, that.assignedTo) &&
                Objects.equals(assignedOn, that.assignedOn) &&
                Objects.equals(referencedWorkfowId, that.referencedWorkfowId) &&
                raLastSeenChatId == that.raLastSeenChatId &&
                caLastSeenChatId == that.caLastSeenChatId &&
                chairpersonApprovalRequired == that.chairpersonApprovalRequired &&
                caLastSeenChatId == that.caLastSeenChatId &&
                Objects.equals(workflowSystemName,that.workflowSystemName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfId, createdBy, createdOn, active, confidential, status, specific, specificReason,
                submittedBy, submittedOn, completedOn, firstEscalationSent, secondEscalationSent, assignedTo, assignedOn,
                referencedWorkfowId, raLastSeenChatId, caLastSeenChatId, chairpersonApprovalRequired, workflowSystemName);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RT_ID", referencedColumnName = "RT_ID", nullable = false)
    public ApprovalForumRtRequestTypeEntity getRtRequestTypeByRtId() {
        return rtRequestTypeByRtId;
    }

    public void setRtRequestTypeByRtId(ApprovalForumRtRequestTypeEntity rtRequestTypeByRtId) {
        this.rtRequestTypeByRtId = rtRequestTypeByRtId;
    }

    @OneToMany(mappedBy = "wfApprovalForumWorkflowByWfId")
    public Collection<ApprovalForumWfaWorkflowApprovalsEntity> getWfaWorkflowApprovalsByWfId() {
        return wfaWorkflowApprovalsByWfId;
    }

    public void setWfaWorkflowApprovalsByWfId(Collection<ApprovalForumWfaWorkflowApprovalsEntity> wfaWorkflowApprovalsByWfId) {
        this.wfaWorkflowApprovalsByWfId = wfaWorkflowApprovalsByWfId;
    }

    @OneToMany(mappedBy = "wfApprovalForumWorkflowByWfId")
    public Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getWfcdWorkflowCounterpartyDetailByWfId() {
        return wfcdWorkflowCounterpartyDetailByWfId;
    }

    public void setWfcdWorkflowCounterpartyDetailByWfId(Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> wfcdWorkflowCounterpartyDetailByWfId) {
        this.wfcdWorkflowCounterpartyDetailByWfId = wfcdWorkflowCounterpartyDetailByWfId;
    }

    @OneToOne(mappedBy = "wfApprovalForumWorkflowByWfId", fetch = FetchType.LAZY)
    public ApprovalForumWfdaWorkflowDataEntity getWfdaWorkflowDataByWfdaId() {
        return wfdaWorkflowDataByWfdaId;
    }

    public void setWfdaWorkflowDataByWfdaId(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataByWfdaId) {
        this.wfdaWorkflowDataByWfdaId = wfdaWorkflowDataByWfdaId;
    }

    @OneToOne(mappedBy = "wfApprovalForumWorkflowByWfId", fetch = FetchType.LAZY)
    public ApprovalForumWiprrWipRulesResponseEntity getWiprrWipRulesResponseByWiprrId() {
        return wiprrWipRulesResponseByWiprrId;
    }

    public void setWiprrWipRulesResponseByWiprrId(ApprovalForumWiprrWipRulesResponseEntity wiprrWipRulesResponseByWiprrId) {
        this.wiprrWipRulesResponseByWiprrId = wiprrWipRulesResponseByWiprrId;
    }

    @OneToOne(mappedBy = "wfApprovalForumWorkflowByWfId", fetch = FetchType.LAZY)
    public ApprovalForumWipurWipUsersResponseEntity getWipurWipUsersResponseByWipurId() {
        return wipurWipUsersResponseByWipurId;
    }

    public void setWipurWipUsersResponseByWipurId(ApprovalForumWipurWipUsersResponseEntity wipurWipUsersResponseByWipurId) {
        this.wipurWipUsersResponseByWipurId = wipurWipUsersResponseByWipurId;
    }


}
