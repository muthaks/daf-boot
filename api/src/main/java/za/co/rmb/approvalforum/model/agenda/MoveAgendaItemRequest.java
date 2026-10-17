package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

/** Moves a booked item to another meeting date of its committee, or another start time on the same date. */
public class MoveAgendaItemRequest {
    private Timestamp meetingDate;
    private String startTime;
    private String lateSubmissionReason;
    private String reason;
    public Timestamp getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Timestamp value) { meetingDate = value; }
    /** HH:mm; the item's current start time when empty. The end time follows from the item's duration. */
    public String getStartTime() { return startTime; }
    public void setStartTime(String value) { startTime = value; }
    /** Required when the new meeting's paper cut-off has passed (unless the item already has one). */
    public String getLateSubmissionReason() { return lateSubmissionReason; }
    public void setLateSubmissionReason(String value) { lateSubmissionReason = value; }
    /** Why the date changed, for the audit trail and the reviewer's mail. Optional. */
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
}
