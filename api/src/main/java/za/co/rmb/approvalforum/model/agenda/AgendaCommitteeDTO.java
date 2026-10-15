package za.co.rmb.approvalforum.model.agenda;

public class AgendaCommitteeDTO {

    private int agcId;
    private String committeeName;
    private String mandateLevel;
    private String levelDescription;
    private int quorumSize;
    private String meetingDay;
    private String chair;
    private String chairId;
    private String altChair;
    private String altChairId;
    private String meetingSecretary;
    private String meetingSecretaryId;
    private String minuteTaker;
    private String minuteTakerId;
    private String administrator;
    private String administratorId;
    private String approverComposition;
    private Integer pageGuidance;
    private String listName;

    public int getAgcId() { return agcId; }
    public void setAgcId(int agcId) { this.agcId = agcId; }

    public String getCommitteeName() { return committeeName; }
    public void setCommitteeName(String committeeName) { this.committeeName = committeeName; }

    public String getMandateLevel() { return mandateLevel; }
    public void setMandateLevel(String mandateLevel) { this.mandateLevel = mandateLevel; }

    public String getLevelDescription() { return levelDescription; }
    public void setLevelDescription(String levelDescription) { this.levelDescription = levelDescription; }

    public int getQuorumSize() { return quorumSize; }
    public void setQuorumSize(int quorumSize) { this.quorumSize = quorumSize; }

    public String getMeetingDay() { return meetingDay; }
    public void setMeetingDay(String meetingDay) { this.meetingDay = meetingDay; }

    public String getChair() { return chair; }
    public void setChair(String chair) { this.chair = chair; }

    public String getChairId() { return chairId; }
    public void setChairId(String chairId) { this.chairId = chairId; }

    public String getAltChair() { return altChair; }
    public void setAltChair(String altChair) { this.altChair = altChair; }

    public String getAltChairId() { return altChairId; }
    public void setAltChairId(String altChairId) { this.altChairId = altChairId; }

    public String getMeetingSecretary() { return meetingSecretary; }
    public void setMeetingSecretary(String meetingSecretary) { this.meetingSecretary = meetingSecretary; }

    public String getMeetingSecretaryId() { return meetingSecretaryId; }
    public void setMeetingSecretaryId(String meetingSecretaryId) { this.meetingSecretaryId = meetingSecretaryId; }

    public String getMinuteTaker() { return minuteTaker; }
    public void setMinuteTaker(String minuteTaker) { this.minuteTaker = minuteTaker; }

    public String getMinuteTakerId() { return minuteTakerId; }
    public void setMinuteTakerId(String minuteTakerId) { this.minuteTakerId = minuteTakerId; }

    public String getAdministrator() { return administrator; }
    public void setAdministrator(String administrator) { this.administrator = administrator; }

    public String getAdministratorId() { return administratorId; }
    public void setAdministratorId(String administratorId) { this.administratorId = administratorId; }

    public String getApproverComposition() { return approverComposition; }
    public void setApproverComposition(String approverComposition) { this.approverComposition = approverComposition; }

    public Integer getPageGuidance() { return pageGuidance; }
    public void setPageGuidance(Integer pageGuidance) { this.pageGuidance = pageGuidance; }

    public String getListName() { return listName; }
    public void setListName(String listName) { this.listName = listName; }

    // ---- BRS alignment (V13) ----
    private boolean active = true;
    private java.sql.Timestamp effectiveFrom;
    private java.sql.Timestamp effectiveTo;
    private String defaultHeldVia;
    private Integer defaultDurationMinutes;
    private String cutOffDayOfWeek;
    private String cutOffTime;
    private String requiredDocTypes;
    private String quorumRoles;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public java.sql.Timestamp getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(java.sql.Timestamp effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public java.sql.Timestamp getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(java.sql.Timestamp effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getDefaultHeldVia() { return defaultHeldVia; }
    public void setDefaultHeldVia(String defaultHeldVia) { this.defaultHeldVia = defaultHeldVia; }

    public Integer getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) { this.defaultDurationMinutes = defaultDurationMinutes; }

    public String getCutOffDayOfWeek() { return cutOffDayOfWeek; }
    public void setCutOffDayOfWeek(String cutOffDayOfWeek) { this.cutOffDayOfWeek = cutOffDayOfWeek; }

    public String getCutOffTime() { return cutOffTime; }
    public void setCutOffTime(String cutOffTime) { this.cutOffTime = cutOffTime; }

    public String getRequiredDocTypes() { return requiredDocTypes; }
    public void setRequiredDocTypes(String requiredDocTypes) { this.requiredDocTypes = requiredDocTypes; }

    public String getQuorumRoles() { return quorumRoles; }
    public void setQuorumRoles(String quorumRoles) { this.quorumRoles = quorumRoles; }

}
