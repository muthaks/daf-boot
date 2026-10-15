package za.co.rmb.approvalforum.model.agenda;

/** A person in a committee role on an agenda item (BRS section 6.4). */
public class AgendaParticipantDTO {

    private int agpId;
    private int agiId;
    private String personId;
    private String personName;
    private String email;
    private String committeeRole;
    private boolean attendanceRequired = true;
    private boolean quorumEligible = true;
    private boolean votingEligible = true;
    private boolean independentFlag;
    private String plannedAttendance;
    private String invitationStatus;
    private Boolean actualAttendance;
    private String overrideReason;

    public int getAgpId() { return agpId; }
    public void setAgpId(int agpId) { this.agpId = agpId; }

    public int getAgiId() { return agiId; }
    public void setAgiId(int agiId) { this.agiId = agiId; }

    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCommitteeRole() { return committeeRole; }
    public void setCommitteeRole(String committeeRole) { this.committeeRole = committeeRole; }

    public boolean isAttendanceRequired() { return attendanceRequired; }
    public void setAttendanceRequired(boolean attendanceRequired) { this.attendanceRequired = attendanceRequired; }

    public boolean isQuorumEligible() { return quorumEligible; }
    public void setQuorumEligible(boolean quorumEligible) { this.quorumEligible = quorumEligible; }

    public boolean isVotingEligible() { return votingEligible; }
    public void setVotingEligible(boolean votingEligible) { this.votingEligible = votingEligible; }

    public boolean isIndependentFlag() { return independentFlag; }
    public void setIndependentFlag(boolean independentFlag) { this.independentFlag = independentFlag; }

    public String getPlannedAttendance() { return plannedAttendance; }
    public void setPlannedAttendance(String plannedAttendance) { this.plannedAttendance = plannedAttendance; }

    public String getInvitationStatus() { return invitationStatus; }
    public void setInvitationStatus(String invitationStatus) { this.invitationStatus = invitationStatus; }

    public Boolean getActualAttendance() { return actualAttendance; }
    public void setActualAttendance(Boolean actualAttendance) { this.actualAttendance = actualAttendance; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }

}
