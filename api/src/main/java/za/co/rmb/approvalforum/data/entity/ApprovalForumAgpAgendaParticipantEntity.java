package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * A person assigned to a committee role on an agenda item (BRS section 6.4). Planned attendance,
 * invitation status and actual attendance are kept apart (RULE-006) and quorum is evaluated
 * over the role/eligibility flags, not a headcount (RULE-005).
 */
@Entity
@Audited
@Table(name = "AGP_AgendaParticipant")
public class ApprovalForumAgpAgendaParticipantEntity {

    private int agpId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private String personId;
    private String personName;
    private String email;
    private String committeeRole;
    private boolean attendanceRequired;
    private boolean quorumEligible;
    private boolean votingEligible;
    private boolean independentFlag;
    private String plannedAttendance;
    private String invitationStatus;
    private Boolean actualAttendance;
    private String overrideReason;

    private ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId;

    @Id
    @Column(name = "AGP_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgpId() {
        return agpId;
    }

    public void setAgpId(int agpId) {
        this.agpId = agpId;
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
    @Column(name = "PersonId")
    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    @Basic
    @Column(name = "PersonName", nullable = false)
    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    @Basic
    @Column(name = "Email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Basic
    @Column(name = "CommitteeRole", nullable = false, length = 100)
    public String getCommitteeRole() {
        return committeeRole;
    }

    public void setCommitteeRole(String committeeRole) {
        this.committeeRole = committeeRole;
    }

    @Basic
    @Column(name = "AttendanceRequired", nullable = false)
    public boolean isAttendanceRequired() {
        return attendanceRequired;
    }

    public void setAttendanceRequired(boolean attendanceRequired) {
        this.attendanceRequired = attendanceRequired;
    }

    @Basic
    @Column(name = "QuorumEligible", nullable = false)
    public boolean isQuorumEligible() {
        return quorumEligible;
    }

    public void setQuorumEligible(boolean quorumEligible) {
        this.quorumEligible = quorumEligible;
    }

    @Basic
    @Column(name = "VotingEligible", nullable = false)
    public boolean isVotingEligible() {
        return votingEligible;
    }

    public void setVotingEligible(boolean votingEligible) {
        this.votingEligible = votingEligible;
    }

    @Basic
    @Column(name = "IndependentFlag", nullable = false)
    public boolean isIndependentFlag() {
        return independentFlag;
    }

    public void setIndependentFlag(boolean independentFlag) {
        this.independentFlag = independentFlag;
    }

    @Basic
    @Column(name = "PlannedAttendance", length = 20)
    public String getPlannedAttendance() {
        return plannedAttendance;
    }

    public void setPlannedAttendance(String plannedAttendance) {
        this.plannedAttendance = plannedAttendance;
    }

    @Basic
    @Column(name = "InvitationStatus", length = 20)
    public String getInvitationStatus() {
        return invitationStatus;
    }

    public void setInvitationStatus(String invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    @Basic
    @Column(name = "ActualAttendance")
    public Boolean getActualAttendance() {
        return actualAttendance;
    }

    public void setActualAttendance(Boolean actualAttendance) {
        this.actualAttendance = actualAttendance;
    }

    @Basic
    @Column(name = "OverrideReason", length = 1000)
    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
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
        ApprovalForumAgpAgendaParticipantEntity that = (ApprovalForumAgpAgendaParticipantEntity) o;
        return agpId == that.agpId && Objects.equals(personName, that.personName) && Objects.equals(committeeRole, that.committeeRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agpId, personName, committeeRole);
    }

}
