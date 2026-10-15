package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "AGPM_AgendaPostMeeting")
public class ApprovalForumAgpmAgendaPostMeetingEntity {

    private int agpmId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
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
    private String decisionType;
    private Timestamp decisionDate;

    private ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId;

    @Id
    @Column(name = "AGPM_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgpmId() {
        return agpmId;
    }

    public void setAgpmId(int agpmId) {
        this.agpmId = agpmId;
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
    @Column(name = "ActualPages")
    public Integer getActualPages() {
        return actualPages;
    }

    public void setActualPages(Integer actualPages) {
        this.actualPages = actualPages;
    }

    @Basic
    @Column(name = "ActualMinutes")
    public Integer getActualMinutes() {
        return actualMinutes;
    }

    public void setActualMinutes(Integer actualMinutes) {
        this.actualMinutes = actualMinutes;
    }

    @Basic
    @Column(name = "Decision")
    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    @Basic
    @Column(name = "CreditConditions")
    public String getCreditConditions() {
        return creditConditions;
    }

    public void setCreditConditions(String creditConditions) {
        this.creditConditions = creditConditions;
    }

    @Basic
    @Column(name = "AttendeesPresent")
    public String getAttendeesPresent() {
        return attendeesPresent;
    }

    public void setAttendeesPresent(String attendeesPresent) {
        this.attendeesPresent = attendeesPresent;
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
    @Column(name = "Scribe")
    public String getScribe() {
        return scribe;
    }

    public void setScribe(String scribe) {
        this.scribe = scribe;
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
    @Column(name = "AnalystSignOffBy")
    public String getAnalystSignOffBy() {
        return analystSignOffBy;
    }

    public void setAnalystSignOffBy(String analystSignOffBy) {
        this.analystSignOffBy = analystSignOffBy;
    }

    @Basic
    @Column(name = "AnalystSignOffOn")
    public Timestamp getAnalystSignOffOn() {
        return analystSignOffOn;
    }

    public void setAnalystSignOffOn(Timestamp analystSignOffOn) {
        this.analystSignOffOn = analystSignOffOn;
    }

    @Basic
    @Column(name = "ChairSignOffBy")
    public String getChairSignOffBy() {
        return chairSignOffBy;
    }

    public void setChairSignOffBy(String chairSignOffBy) {
        this.chairSignOffBy = chairSignOffBy;
    }

    @Basic
    @Column(name = "ChairSignOffOn")
    public Timestamp getChairSignOffOn() {
        return chairSignOffOn;
    }

    public void setChairSignOffOn(Timestamp chairSignOffOn) {
        this.chairSignOffOn = chairSignOffOn;
    }

    @Basic
    @Column(name = "DocStashOutputId")
    public String getDocStashOutputId() {
        return docStashOutputId;
    }

    public void setDocStashOutputId(String docStashOutputId) {
        this.docStashOutputId = docStashOutputId;
    }

    @Basic
    @Column(name = "ActualRating")
    public String getActualRating() { return actualRating; }
    public void setActualRating(String actualRating) { this.actualRating = actualRating; }

    @Basic
    @Column(name = "ActualPurpose")
    public String getActualPurpose() { return actualPurpose; }
    public void setActualPurpose(String actualPurpose) { this.actualPurpose = actualPurpose; }

    @Basic
    @Column(name = "ActualDcp")
    public Double getActualDcp() { return actualDcp; }
    public void setActualDcp(Double actualDcp) { this.actualDcp = actualDcp; }

    @Basic
    @Column(name = "DateOfCc")
    public Timestamp getDateOfCc() { return dateOfCc; }
    public void setDateOfCc(Timestamp dateOfCc) { this.dateOfCc = dateOfCc; }

    @Basic
    @Column(name = "Locked")
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    @Basic
    @Column(name = "LockedBy")
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    @Basic
    @Column(name = "LockedOn")
    public Timestamp getLockedOn() { return lockedOn; }
    public void setLockedOn(Timestamp lockedOn) { this.lockedOn = lockedOn; }

    // ---- BRS alignment (V13): controlled decision (section 6.7) ----

    @Basic
    @Column(name = "DecisionType", length = 50)
    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }

    @Basic
    @Column(name = "DecisionDate")
    public Timestamp getDecisionDate() { return decisionDate; }
    public void setDecisionDate(Timestamp decisionDate) { this.decisionDate = decisionDate; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false)
    public ApprovalForumAgiAgendaItemEntity getAgiAgendaItemByAgiId() {
        return agiAgendaItemByAgiId;
    }

    public void setAgiAgendaItemByAgiId(ApprovalForumAgiAgendaItemEntity agiAgendaItemByAgiId) {
        this.agiAgendaItemByAgiId = agiAgendaItemByAgiId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgpmAgendaPostMeetingEntity that = (ApprovalForumAgpmAgendaPostMeetingEntity) o;
        return agpmId == that.agpmId &&
                Objects.equals(decision, that.decision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agpmId, decision);
    }

}
