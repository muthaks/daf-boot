package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "AGM_AgendaMeeting")
public class ApprovalForumAgmAgendaMeetingEntity {

    private int agmId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private Timestamp meetingDate;
    private String heldVia;
    private String status;
    private Timestamp cutOffOn;
    private String lockedBy;
    private Timestamp lockedOn;
    private String startTime;
    private String endTime;
    private String venue;
    private String teamsJoinUrl;
    private String graphEventId;
    private String invitationSnapshot;
    private Timestamp invitationSentOn;
    private String invitationSentBy;
    private boolean invitationReopened;
    private String actualStartTime;
    private String actualEndTime;
    private Boolean quorumAchieved;
    private String cancelledBy;
    private Timestamp cancelledOn;
    private String cancelReason;
    private String closedBy;
    private Timestamp closedOn;

    private ApprovalForumAgcAgendaCommitteeEntity agcAgendaCommitteeByAgcId;

    @Id
    @Column(name = "AGM_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgmId() {
        return agmId;
    }

    public void setAgmId(int agmId) {
        this.agmId = agmId;
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
    @Column(name = "MeetingDate", nullable = false)
    public Timestamp getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(Timestamp meetingDate) {
        this.meetingDate = meetingDate;
    }

    @Basic
    @Column(name = "HeldVia")
    public String getHeldVia() {
        return heldVia;
    }

    public void setHeldVia(String heldVia) {
        this.heldVia = heldVia;
    }

    @Basic
    @Column(name = "Status", nullable = false)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Basic
    @Column(name = "CutOffOn")
    public Timestamp getCutOffOn() {
        return cutOffOn;
    }

    public void setCutOffOn(Timestamp cutOffOn) {
        this.cutOffOn = cutOffOn;
    }

    @Basic
    @Column(name = "LockedBy")
    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    @Basic
    @Column(name = "LockedOn")
    public Timestamp getLockedOn() {
        return lockedOn;
    }

    public void setLockedOn(Timestamp lockedOn) {
        this.lockedOn = lockedOn;
    }

    // ---- BRS alignment (V13): calendar detail, Teams/Outlook event, execution, cancel/close ----

    @Basic
    @Column(name = "StartTime", length = 5)
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    @Basic
    @Column(name = "EndTime", length = 5)
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @Basic
    @Column(name = "Venue")
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    @Basic
    @Column(name = "TeamsJoinUrl", length = 1000)
    public String getTeamsJoinUrl() { return teamsJoinUrl; }
    public void setTeamsJoinUrl(String teamsJoinUrl) { this.teamsJoinUrl = teamsJoinUrl; }

    @Basic
    @Column(name = "GraphEventId")
    public String getGraphEventId() { return graphEventId; }
    public void setGraphEventId(String graphEventId) { this.graphEventId = graphEventId; }

    @Basic
    @Column(name = "InvitationSnapshot", length = 64)
    public String getInvitationSnapshot() { return invitationSnapshot; }
    public void setInvitationSnapshot(String value) { invitationSnapshot = value; }

    @Basic
    @Column(name = "InvitationSentOn")
    public Timestamp getInvitationSentOn() { return invitationSentOn; }
    public void setInvitationSentOn(Timestamp value) { invitationSentOn = value; }

    @Basic
    @Column(name = "InvitationSentBy")
    public String getInvitationSentBy() { return invitationSentBy; }
    public void setInvitationSentBy(String value) { invitationSentBy = value; }

    @Basic
    @Column(name = "InvitationReopened", nullable = false)
    public boolean isInvitationReopened() { return invitationReopened; }
    public void setInvitationReopened(boolean value) { invitationReopened = value; }

    @Basic
    @Column(name = "ActualStartTime", length = 5)
    public String getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(String actualStartTime) { this.actualStartTime = actualStartTime; }

    @Basic
    @Column(name = "ActualEndTime", length = 5)
    public String getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(String actualEndTime) { this.actualEndTime = actualEndTime; }

    @Basic
    @Column(name = "QuorumAchieved")
    public Boolean getQuorumAchieved() { return quorumAchieved; }
    public void setQuorumAchieved(Boolean quorumAchieved) { this.quorumAchieved = quorumAchieved; }

    @Basic
    @Column(name = "CancelledBy")
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    @Basic
    @Column(name = "CancelledOn")
    public Timestamp getCancelledOn() { return cancelledOn; }
    public void setCancelledOn(Timestamp cancelledOn) { this.cancelledOn = cancelledOn; }

    @Basic
    @Column(name = "CancelReason", length = 1000)
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    @Basic
    @Column(name = "ClosedBy")
    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    @Basic
    @Column(name = "ClosedOn")
    public Timestamp getClosedOn() { return closedOn; }
    public void setClosedOn(Timestamp closedOn) { this.closedOn = closedOn; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGC_ID", referencedColumnName = "AGC_ID", nullable = false)
    public ApprovalForumAgcAgendaCommitteeEntity getAgcAgendaCommitteeByAgcId() {
        return agcAgendaCommitteeByAgcId;
    }

    public void setAgcAgendaCommitteeByAgcId(ApprovalForumAgcAgendaCommitteeEntity agcAgendaCommitteeByAgcId) {
        this.agcAgendaCommitteeByAgcId = agcAgendaCommitteeByAgcId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgmAgendaMeetingEntity that = (ApprovalForumAgmAgendaMeetingEntity) o;
        return agmId == that.agmId &&
                Objects.equals(meetingDate, that.meetingDate) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agmId, meetingDate, status);
    }

}
