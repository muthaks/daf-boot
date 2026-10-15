package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * A condition, action or follow-up raised against an agenda item at the committee
 * (BRS section 6.8) - owner, due date, status and closure evidence.
 */
@Entity
@Audited
@Table(name = "AGCA_AgendaCondition")
public class ApprovalForumAgcaAgendaConditionEntity {

    private int agcaId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private String conditionType;
    private String description;
    private String ownerId;
    private String ownerName;
    private Timestamp dueDate;
    private String status;
    private String evidenceRef;
    private String closureComments;
    private String closedBy;
    private Timestamp closedOn;

    private ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId;

    @Id
    @Column(name = "AGCA_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgcaId() {
        return agcaId;
    }

    public void setAgcaId(int agcaId) {
        this.agcaId = agcaId;
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
    @Column(name = "ConditionType", nullable = false, length = 50)
    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    @Basic
    @Column(name = "Description", nullable = false)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "OwnerId")
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Basic
    @Column(name = "OwnerName")
    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    @Basic
    @Column(name = "DueDate")
    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    @Basic
    @Column(name = "Status", nullable = false, length = 50)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Basic
    @Column(name = "EvidenceRef", length = 1000)
    public String getEvidenceRef() {
        return evidenceRef;
    }

    public void setEvidenceRef(String evidenceRef) {
        this.evidenceRef = evidenceRef;
    }

    @Basic
    @Column(name = "ClosureComments")
    public String getClosureComments() {
        return closureComments;
    }

    public void setClosureComments(String closureComments) {
        this.closureComments = closureComments;
    }

    @Basic
    @Column(name = "ClosedBy")
    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    @Basic
    @Column(name = "ClosedOn")
    public Timestamp getClosedOn() {
        return closedOn;
    }

    public void setClosedOn(Timestamp closedOn) {
        this.closedOn = closedOn;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false)
    public ApprovalForumAgiAgendaItemEntity getAgiAgendaItemByAgiId() {
        return agiAgendaItemByAgiId;
    }

    public void setAgiAgendaItemByAgiId(ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId) {
        this.agiAgendaItemByAgiId = agiAgendaItemByAgiId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgcaAgendaConditionEntity that = (ApprovalForumAgcaAgendaConditionEntity) o;
        return agcaId == that.agcaId && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agcaId, description);
    }

}
