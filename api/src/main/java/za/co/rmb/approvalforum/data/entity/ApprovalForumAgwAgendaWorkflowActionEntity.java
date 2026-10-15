package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * One row per workflow action on an agenda item or meeting (submit, return, approve, reject,
 * reopen, lock, cancel, close) - the BRS section 6.6 workflow record and RULE-014 audit of
 * actor, timestamp and decision.
 */
@Entity
@Audited
@Table(name = "AGW_AgendaWorkflowAction")
public class ApprovalForumAgwAgendaWorkflowActionEntity {

    private int agwId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private Integer agiId;
    private Integer agmId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String comments;
    private boolean overrideFlag;
    private String overrideReason;

    @Id
    @Column(name = "AGW_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgwId() {
        return agwId;
    }

    public void setAgwId(int agwId) {
        this.agwId = agwId;
    }

    @Basic
    @Column(name = "CreatedBy", nullable = false)
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
    @Column(name = "AGI_ID")
    public Integer getAgiId() {
        return agiId;
    }

    public void setAgiId(Integer agiId) {
        this.agiId = agiId;
    }

    @Basic
    @Column(name = "AGM_ID")
    public Integer getAgmId() {
        return agmId;
    }

    public void setAgmId(Integer agmId) {
        this.agmId = agmId;
    }

    @Basic
    @Column(name = "Action", nullable = false)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Basic
    @Column(name = "FromStatus")
    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    @Basic
    @Column(name = "ToStatus")
    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    @Basic
    @Column(name = "Comments")
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Basic
    @Column(name = "OverrideFlag", nullable = false)
    public boolean isOverrideFlag() {
        return overrideFlag;
    }

    public void setOverrideFlag(boolean overrideFlag) {
        this.overrideFlag = overrideFlag;
    }

    @Basic
    @Column(name = "OverrideReason", length = 1000)
    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgwAgendaWorkflowActionEntity that = (ApprovalForumAgwAgendaWorkflowActionEntity) o;
        return agwId == that.agwId && Objects.equals(action, that.action) && Objects.equals(createdOn, that.createdOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agwId, action, createdOn);
    }

}
