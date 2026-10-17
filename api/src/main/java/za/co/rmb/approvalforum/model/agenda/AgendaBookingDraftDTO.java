package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

/**
 * A booking the analyst saved part-way through the booking wizard, to continue later from any PC.
 * `payload` is the wizard's own state (JSON) and is not interpreted here; the other fields are copies
 * of it for the drafts list. A draft reserves no slot and is not an agenda item.
 */
public class AgendaBookingDraftDTO {
    private int agbdId;
    private Integer agcId;
    private String committeeName;
    private Timestamp meetingDate;
    private String counterpartyName;
    private int step;
    private String payload;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    public int getAgbdId() { return agbdId; }
    public void setAgbdId(int value) { agbdId = value; }
    public Integer getAgcId() { return agcId; }
    public void setAgcId(Integer value) { agcId = value; }
    public String getCommitteeName() { return committeeName; }
    public void setCommitteeName(String value) { committeeName = value; }
    public Timestamp getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Timestamp value) { meetingDate = value; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String value) { counterpartyName = value; }
    public int getStep() { return step; }
    public void setStep(int value) { step = value; }
    public String getPayload() { return payload; }
    public void setPayload(String value) { payload = value; }
    public Timestamp getCreatedOn() { return createdOn; }
    public void setCreatedOn(Timestamp value) { createdOn = value; }
    public Timestamp getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(Timestamp value) { updatedOn = value; }
}
