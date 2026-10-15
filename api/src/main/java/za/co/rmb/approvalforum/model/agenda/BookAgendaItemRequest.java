package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

public class BookAgendaItemRequest {

    private int agcId;
    private Timestamp meetingDate;
    private String heldVia;
    private AgendaItemDTO item;

    public int getAgcId() { return agcId; }
    public void setAgcId(int agcId) { this.agcId = agcId; }

    public Timestamp getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Timestamp meetingDate) { this.meetingDate = meetingDate; }

    public String getHeldVia() { return heldVia; }
    public void setHeldVia(String heldVia) { this.heldVia = heldVia; }

    public AgendaItemDTO getItem() { return item; }
    public void setItem(AgendaItemDTO item) { this.item = item; }

}
