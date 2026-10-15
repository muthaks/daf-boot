package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "AGC_AgendaCommittee")
public class ApprovalForumAgcAgendaCommitteeEntity {

    private int agcId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
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
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private String defaultHeldVia;
    private Integer defaultDurationMinutes;
    private String cutOffDayOfWeek;
    private String cutOffTime;
    private String requiredDocTypes;
    private String quorumRoles;

    @Id
    @Column(name = "AGC_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgcId() {
        return agcId;
    }

    public void setAgcId(int agcId) {
        this.agcId = agcId;
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
    @Column(name = "CommitteeName", nullable = false)
    public String getCommitteeName() {
        return committeeName;
    }

    public void setCommitteeName(String committeeName) {
        this.committeeName = committeeName;
    }

    @Basic
    @Column(name = "MandateLevel", nullable = false)
    public String getMandateLevel() {
        return mandateLevel;
    }

    public void setMandateLevel(String mandateLevel) {
        this.mandateLevel = mandateLevel;
    }

    @Basic
    @Column(name = "LevelDescription")
    public String getLevelDescription() {
        return levelDescription;
    }

    public void setLevelDescription(String levelDescription) {
        this.levelDescription = levelDescription;
    }

    @Basic
    @Column(name = "QuorumSize", nullable = false)
    public int getQuorumSize() {
        return quorumSize;
    }

    public void setQuorumSize(int quorumSize) {
        this.quorumSize = quorumSize;
    }

    @Basic
    @Column(name = "MeetingDay", nullable = false)
    public String getMeetingDay() {
        return meetingDay;
    }

    public void setMeetingDay(String meetingDay) {
        this.meetingDay = meetingDay;
    }

    @Basic
    @Column(name = "Chair")
    public String getChair() {
        return chair;
    }

    public void setChair(String chair) {
        this.chair = chair;
    }

    @Basic
    @Column(name = "ChairId")
    public String getChairId() {
        return chairId;
    }

    public void setChairId(String chairId) {
        this.chairId = chairId;
    }

    @Basic
    @Column(name = "AltChair")
    public String getAltChair() {
        return altChair;
    }

    public void setAltChair(String altChair) {
        this.altChair = altChair;
    }

    @Basic
    @Column(name = "AltChairId")
    public String getAltChairId() {
        return altChairId;
    }

    public void setAltChairId(String altChairId) {
        this.altChairId = altChairId;
    }

    @Basic
    @Column(name = "MeetingSecretary")
    public String getMeetingSecretary() {
        return meetingSecretary;
    }

    public void setMeetingSecretary(String meetingSecretary) {
        this.meetingSecretary = meetingSecretary;
    }

    @Basic
    @Column(name = "MeetingSecretaryId")
    public String getMeetingSecretaryId() {
        return meetingSecretaryId;
    }

    public void setMeetingSecretaryId(String meetingSecretaryId) {
        this.meetingSecretaryId = meetingSecretaryId;
    }

    @Basic
    @Column(name = "MinuteTaker")
    public String getMinuteTaker() {
        return minuteTaker;
    }

    public void setMinuteTaker(String minuteTaker) {
        this.minuteTaker = minuteTaker;
    }

    @Basic
    @Column(name = "MinuteTakerId")
    public String getMinuteTakerId() {
        return minuteTakerId;
    }

    public void setMinuteTakerId(String minuteTakerId) {
        this.minuteTakerId = minuteTakerId;
    }

    @Basic
    @Column(name = "Administrator")
    public String getAdministrator() {
        return administrator;
    }

    public void setAdministrator(String administrator) {
        this.administrator = administrator;
    }

    @Basic
    @Column(name = "AdministratorId")
    public String getAdministratorId() {
        return administratorId;
    }

    public void setAdministratorId(String administratorId) {
        this.administratorId = administratorId;
    }

    @Basic
    @Column(name = "ApproverComposition")
    public String getApproverComposition() {
        return approverComposition;
    }

    public void setApproverComposition(String approverComposition) {
        this.approverComposition = approverComposition;
    }

    @Basic
    @Column(name = "PageGuidance")
    public Integer getPageGuidance() {
        return pageGuidance;
    }

    public void setPageGuidance(Integer pageGuidance) {
        this.pageGuidance = pageGuidance;
    }

    @Basic
    @Column(name = "ListName")
    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    // ---- BRS alignment (V13): effective dating, calendar defaults, cut-off, readiness, quorum ----

    @Basic
    @Column(name = "EffectiveFrom")
    public Timestamp getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Timestamp effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    @Basic
    @Column(name = "EffectiveTo")
    public Timestamp getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Timestamp effectiveTo) { this.effectiveTo = effectiveTo; }

    @Basic
    @Column(name = "DefaultHeldVia", length = 50)
    public String getDefaultHeldVia() { return defaultHeldVia; }
    public void setDefaultHeldVia(String defaultHeldVia) { this.defaultHeldVia = defaultHeldVia; }

    @Basic
    @Column(name = "DefaultDurationMinutes")
    public Integer getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) { this.defaultDurationMinutes = defaultDurationMinutes; }

    @Basic
    @Column(name = "CutOffDayOfWeek", length = 20)
    public String getCutOffDayOfWeek() { return cutOffDayOfWeek; }
    public void setCutOffDayOfWeek(String cutOffDayOfWeek) { this.cutOffDayOfWeek = cutOffDayOfWeek; }

    @Basic
    @Column(name = "CutOffTime", length = 5)
    public String getCutOffTime() { return cutOffTime; }
    public void setCutOffTime(String cutOffTime) { this.cutOffTime = cutOffTime; }

    @Basic
    @Column(name = "RequiredDocTypes", length = 500)
    public String getRequiredDocTypes() { return requiredDocTypes; }
    public void setRequiredDocTypes(String requiredDocTypes) { this.requiredDocTypes = requiredDocTypes; }

    @Basic
    @Column(name = "QuorumRoles", length = 1000)
    public String getQuorumRoles() { return quorumRoles; }
    public void setQuorumRoles(String quorumRoles) { this.quorumRoles = quorumRoles; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgcAgendaCommitteeEntity that = (ApprovalForumAgcAgendaCommitteeEntity) o;
        return agcId == that.agcId &&
                quorumSize == that.quorumSize &&
                Objects.equals(committeeName, that.committeeName) &&
                Objects.equals(mandateLevel, that.mandateLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agcId, committeeName, mandateLevel, quorumSize);
    }

}
