package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;
import java.util.List;

public class AgendaMeetingDTO {

    private int agmId;
    private int agcId;
    private String committeeName;
    private String mandateLevel;
    private int quorumSize;
    private String chair;
    private Timestamp meetingDate;
    private String heldVia;
    private String status;
    private Timestamp cutOffOn;
    private String lockedBy;
    private Timestamp lockedOn;
    private List<AgendaItemDTO> items;

    public int getAgmId() { return agmId; }
    public void setAgmId(int agmId) { this.agmId = agmId; }

    public int getAgcId() { return agcId; }
    public void setAgcId(int agcId) { this.agcId = agcId; }

    public String getCommitteeName() { return committeeName; }
    public void setCommitteeName(String committeeName) { this.committeeName = committeeName; }

    public String getMandateLevel() { return mandateLevel; }
    public void setMandateLevel(String mandateLevel) { this.mandateLevel = mandateLevel; }

    public int getQuorumSize() { return quorumSize; }
    public void setQuorumSize(int quorumSize) { this.quorumSize = quorumSize; }

    public String getChair() { return chair; }
    public void setChair(String chair) { this.chair = chair; }

    public Timestamp getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Timestamp meetingDate) { this.meetingDate = meetingDate; }

    public String getHeldVia() { return heldVia; }
    public void setHeldVia(String heldVia) { this.heldVia = heldVia; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCutOffOn() { return cutOffOn; }
    public void setCutOffOn(Timestamp cutOffOn) { this.cutOffOn = cutOffOn; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public Timestamp getLockedOn() { return lockedOn; }
    public void setLockedOn(Timestamp lockedOn) { this.lockedOn = lockedOn; }

    public List<AgendaItemDTO> getItems() { return items; }
    public void setItems(List<AgendaItemDTO> items) { this.items = items; }

    // ---- BRS alignment (V13) ----
    private String startTime;
    private String endTime;
    private String venue;
    private String teamsJoinUrl;
    private String graphEventId;
    private Timestamp invitationSentOn;
    private String invitationSentBy;
    private boolean invitationNeedsUpdate;
    private String actualStartTime;
    private String actualEndTime;
    private Boolean quorumAchieved;
    private String cancelledBy;
    private Timestamp cancelledOn;
    private String cancelReason;
    private String closedBy;
    private Timestamp closedOn;
    private List<AgendaMeetingRecordDTO> records;
    private List<AgendaWorkflowActionDTO> actions;

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getTeamsJoinUrl() { return teamsJoinUrl; }
    public void setTeamsJoinUrl(String teamsJoinUrl) { this.teamsJoinUrl = teamsJoinUrl; }

    public String getGraphEventId() { return graphEventId; }
    public void setGraphEventId(String graphEventId) { this.graphEventId = graphEventId; }

    public Timestamp getInvitationSentOn() { return invitationSentOn; }
    public void setInvitationSentOn(Timestamp value) { invitationSentOn = value; }
    public String getInvitationSentBy() { return invitationSentBy; }
    public void setInvitationSentBy(String value) { invitationSentBy = value; }
    public boolean isInvitationNeedsUpdate() { return invitationNeedsUpdate; }
    public void setInvitationNeedsUpdate(boolean value) { invitationNeedsUpdate = value; }

    public String getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(String actualStartTime) { this.actualStartTime = actualStartTime; }

    public String getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(String actualEndTime) { this.actualEndTime = actualEndTime; }

    public Boolean getQuorumAchieved() { return quorumAchieved; }
    public void setQuorumAchieved(Boolean quorumAchieved) { this.quorumAchieved = quorumAchieved; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public Timestamp getCancelledOn() { return cancelledOn; }
    public void setCancelledOn(Timestamp cancelledOn) { this.cancelledOn = cancelledOn; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public Timestamp getClosedOn() { return closedOn; }
    public void setClosedOn(Timestamp closedOn) { this.closedOn = closedOn; }

    public List<AgendaMeetingRecordDTO> getRecords() { return records; }
    public void setRecords(List<AgendaMeetingRecordDTO> records) { this.records = records; }

    public List<AgendaWorkflowActionDTO> getActions() { return actions; }
    public void setActions(List<AgendaWorkflowActionDTO> actions) { this.actions = actions; }

}
