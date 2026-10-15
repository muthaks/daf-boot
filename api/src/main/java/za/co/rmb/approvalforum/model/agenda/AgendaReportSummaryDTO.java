package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Operational reporting aggregates for a date range (BRS section 10). */
public class AgendaReportSummaryDTO {

    private Timestamp from;
    private Timestamp to;
    private int meetings;
    private int items;
    private Map<String, Integer> itemsByStatus = new LinkedHashMap<>();
    private Map<String, Integer> itemsByCommittee = new LinkedHashMap<>();
    private Map<String, Integer> itemsByBusinessUnit = new LinkedHashMap<>();
    private Map<String, Integer> itemsByPurpose = new LinkedHashMap<>();
    private Map<String, Integer> decisions = new LinkedHashMap<>();
    private Map<String, Integer> meetingsByStatus = new LinkedHashMap<>();
    private int lateItems;
    private int outstandingMandatoryPapers;
    private int plannedMinutes;
    private int actualMinutes;
    private int invited;
    private int plannedAttending;
    private int actuallyAttended;
    private int quorumMet;
    private int quorumNotMet;
    private int openConditions;
    private int overdueConditions;
    private List<AgendaItemDTO> lateItemDetails = new ArrayList<>();

    public Timestamp getFrom() { return from; }
    public void setFrom(Timestamp from) { this.from = from; }

    public Timestamp getTo() { return to; }
    public void setTo(Timestamp to) { this.to = to; }

    public int getMeetings() { return meetings; }
    public void setMeetings(int meetings) { this.meetings = meetings; }

    public int getItems() { return items; }
    public void setItems(int items) { this.items = items; }

    public Map<String, Integer> getItemsByStatus() { return itemsByStatus; }
    public void setItemsByStatus(Map<String, Integer> itemsByStatus) { this.itemsByStatus = itemsByStatus; }

    public Map<String, Integer> getItemsByCommittee() { return itemsByCommittee; }
    public void setItemsByCommittee(Map<String, Integer> itemsByCommittee) { this.itemsByCommittee = itemsByCommittee; }

    public Map<String, Integer> getItemsByBusinessUnit() { return itemsByBusinessUnit; }
    public void setItemsByBusinessUnit(Map<String, Integer> itemsByBusinessUnit) { this.itemsByBusinessUnit = itemsByBusinessUnit; }

    public Map<String, Integer> getItemsByPurpose() { return itemsByPurpose; }
    public void setItemsByPurpose(Map<String, Integer> itemsByPurpose) { this.itemsByPurpose = itemsByPurpose; }

    public Map<String, Integer> getDecisions() { return decisions; }
    public void setDecisions(Map<String, Integer> decisions) { this.decisions = decisions; }

    public Map<String, Integer> getMeetingsByStatus() { return meetingsByStatus; }
    public void setMeetingsByStatus(Map<String, Integer> meetingsByStatus) { this.meetingsByStatus = meetingsByStatus; }

    public int getLateItems() { return lateItems; }
    public void setLateItems(int lateItems) { this.lateItems = lateItems; }

    public int getOutstandingMandatoryPapers() { return outstandingMandatoryPapers; }
    public void setOutstandingMandatoryPapers(int outstandingMandatoryPapers) { this.outstandingMandatoryPapers = outstandingMandatoryPapers; }

    public int getPlannedMinutes() { return plannedMinutes; }
    public void setPlannedMinutes(int plannedMinutes) { this.plannedMinutes = plannedMinutes; }

    public int getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(int actualMinutes) { this.actualMinutes = actualMinutes; }

    public int getInvited() { return invited; }
    public void setInvited(int invited) { this.invited = invited; }

    public int getPlannedAttending() { return plannedAttending; }
    public void setPlannedAttending(int plannedAttending) { this.plannedAttending = plannedAttending; }

    public int getActuallyAttended() { return actuallyAttended; }
    public void setActuallyAttended(int actuallyAttended) { this.actuallyAttended = actuallyAttended; }

    public int getQuorumMet() { return quorumMet; }
    public void setQuorumMet(int quorumMet) { this.quorumMet = quorumMet; }

    public int getQuorumNotMet() { return quorumNotMet; }
    public void setQuorumNotMet(int quorumNotMet) { this.quorumNotMet = quorumNotMet; }

    public int getOpenConditions() { return openConditions; }
    public void setOpenConditions(int openConditions) { this.openConditions = openConditions; }

    public int getOverdueConditions() { return overdueConditions; }
    public void setOverdueConditions(int overdueConditions) { this.overdueConditions = overdueConditions; }

    public List<AgendaItemDTO> getLateItemDetails() { return lateItemDetails; }
    public void setLateItemDetails(List<AgendaItemDTO> lateItemDetails) { this.lateItemDetails = lateItemDetails; }

}
