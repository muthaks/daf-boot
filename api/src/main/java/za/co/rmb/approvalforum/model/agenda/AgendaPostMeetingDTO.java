package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

public class AgendaPostMeetingDTO {

    private int agpmId;
    private int agiId;
    private Integer actualPages;
    private Integer actualMinutes;
    private String decision;
    private String creditConditions;
    private String attendeesPresent;
    private String minuteTaker;
    private String scribe;
    private String administrator;
    private String analystSignOffBy;
    private Timestamp analystSignOffOn;
    private String chairSignOffBy;
    private Timestamp chairSignOffOn;
    private String docStashOutputId;
    private String actualRating;
    private String actualPurpose;
    private Double actualDcp;
    private Timestamp dateOfCc;
    private boolean locked;
    private String lockedBy;
    private Timestamp lockedOn;

    public int getAgpmId() { return agpmId; }
    public void setAgpmId(int agpmId) { this.agpmId = agpmId; }

    public int getAgiId() { return agiId; }
    public void setAgiId(int agiId) { this.agiId = agiId; }

    public Integer getActualPages() { return actualPages; }
    public void setActualPages(Integer actualPages) { this.actualPages = actualPages; }

    public Integer getActualMinutes() { return actualMinutes; }
    public void setActualMinutes(Integer actualMinutes) { this.actualMinutes = actualMinutes; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getCreditConditions() { return creditConditions; }
    public void setCreditConditions(String creditConditions) { this.creditConditions = creditConditions; }

    public String getAttendeesPresent() { return attendeesPresent; }
    public void setAttendeesPresent(String attendeesPresent) { this.attendeesPresent = attendeesPresent; }

    public String getMinuteTaker() { return minuteTaker; }
    public void setMinuteTaker(String minuteTaker) { this.minuteTaker = minuteTaker; }

    public String getScribe() { return scribe; }
    public void setScribe(String scribe) { this.scribe = scribe; }

    public String getAdministrator() { return administrator; }
    public void setAdministrator(String administrator) { this.administrator = administrator; }

    public String getAnalystSignOffBy() { return analystSignOffBy; }
    public void setAnalystSignOffBy(String analystSignOffBy) { this.analystSignOffBy = analystSignOffBy; }

    public Timestamp getAnalystSignOffOn() { return analystSignOffOn; }
    public void setAnalystSignOffOn(Timestamp analystSignOffOn) { this.analystSignOffOn = analystSignOffOn; }

    public String getChairSignOffBy() { return chairSignOffBy; }
    public void setChairSignOffBy(String chairSignOffBy) { this.chairSignOffBy = chairSignOffBy; }

    public Timestamp getChairSignOffOn() { return chairSignOffOn; }
    public void setChairSignOffOn(Timestamp chairSignOffOn) { this.chairSignOffOn = chairSignOffOn; }

    public String getDocStashOutputId() { return docStashOutputId; }
    public void setDocStashOutputId(String docStashOutputId) { this.docStashOutputId = docStashOutputId; }

    public String getActualRating() { return actualRating; }
    public void setActualRating(String actualRating) { this.actualRating = actualRating; }

    public String getActualPurpose() { return actualPurpose; }
    public void setActualPurpose(String actualPurpose) { this.actualPurpose = actualPurpose; }

    public Double getActualDcp() { return actualDcp; }
    public void setActualDcp(Double actualDcp) { this.actualDcp = actualDcp; }

    public Timestamp getDateOfCc() { return dateOfCc; }
    public void setDateOfCc(Timestamp dateOfCc) { this.dateOfCc = dateOfCc; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public Timestamp getLockedOn() { return lockedOn; }
    public void setLockedOn(Timestamp lockedOn) { this.lockedOn = lockedOn; }

    // ---- BRS alignment (V13) ----
    private String decisionType;
    private Timestamp decisionDate;

    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }

    public Timestamp getDecisionDate() { return decisionDate; }
    public void setDecisionDate(Timestamp decisionDate) { this.decisionDate = decisionDate; }

}
