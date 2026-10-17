package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * A booking saved part-way through the Agenda Builder booking wizard (AGBD_AgendaBookingDraft). It
 * belongs to the analyst who saved it, reserves no slot and is not an agenda item: `Payload` is the
 * wizard's state as JSON. Deleting a draft, or booking from it, sets Active to false.
 */
@Entity
@Table(name = "AGBD_AgendaBookingDraft")
public class ApprovalForumAgbdAgendaBookingDraftEntity {

    private int agbdId;
    private String ownerId;
    private Integer agcId;
    private Timestamp meetingDate;
    private String counterpartyName;
    private int step;
    private String payload;
    private String createdBy;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    private boolean active;

    @Id
    @Column(name = "AGBD_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgbdId() { return agbdId; }
    public void setAgbdId(int agbdId) { this.agbdId = agbdId; }

    @Basic
    @Column(name = "OwnerId", nullable = false)
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    @Basic
    @Column(name = "AGC_ID")
    public Integer getAgcId() { return agcId; }
    public void setAgcId(Integer agcId) { this.agcId = agcId; }

    @Basic
    @Column(name = "MeetingDate")
    public Timestamp getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Timestamp meetingDate) { this.meetingDate = meetingDate; }

    @Basic
    @Column(name = "CounterpartyName")
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }

    @Basic
    @Column(name = "Step", nullable = false)
    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    @Basic
    @Column(name = "Payload", nullable = false)
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    @Basic
    @Column(name = "CreatedBy", nullable = false)
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Basic
    @Column(name = "CreatedOn", nullable = false)
    public Timestamp getCreatedOn() { return createdOn; }
    public void setCreatedOn(Timestamp createdOn) { this.createdOn = createdOn; }

    @Basic
    @Column(name = "UpdatedOn")
    public Timestamp getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(Timestamp updatedOn) { this.updatedOn = updatedOn; }

    @Basic
    @Column(name = "Active", nullable = false)
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
