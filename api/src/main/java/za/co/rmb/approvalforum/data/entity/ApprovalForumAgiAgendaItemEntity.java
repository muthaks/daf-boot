package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "AGI_AgendaItem")
public class ApprovalForumAgiAgendaItemEntity {

    private int agiId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private String inkombaId;
    private String counterpartyName;
    private String businessUnit;
    private String purposeOfSubmission;
    private String purposeDetails;
    private Double dcpZar;
    private String ratingBand;
    private Integer numberOfPages;
    private boolean withinPageGuidance;
    private Integer estimatedMinutes;
    private String primaryAnalyst;
    private String primaryAnalystId;
    private String reviewAnalyst;
    private String reviewAnalystId;
    private String creditTeam;
    private String businessTeam;
    private String creditSponsor;
    private String creditSponsorId;
    private String buHead;
    private String buHeadId;
    private boolean lateSubmission;
    private String lateApprovedBy;
    private Timestamp lateApprovedOn;
    private String outOfGuidanceApprovedBy;
    private Timestamp outOfGuidanceApprovedOn;
    private String itemStatus;
    private String agendaNumber;

    private String startTime;
    private String endTime;
    private boolean roundRobin;
    private String chair;
    private String attendeeEmails;
    private String lecRep;
    private String eqMemberFrmExec;
    private String businessRep1;
    private String seniorBusinessRep;
    private String portfolioCce1;
    private String portfolioCce2;
    private String independentMember;
    private String scribe;
    private String mezzOverlayAttendees;
    private String esgOverlayAttendees;
    private String additionalTeamAttendees;
    private String countryOfRisk;
    private String industryClassification;
    private boolean lecCounterparty;
    private String distributionSensitivities;
    private String applicantRelationship;
    private String applicationType;
    private boolean proposalReceived;
    private boolean proposalDistributedEmail;
    private boolean proposalOnDiligent;
    private String comments;
    private String lateSubmissionReason;
    private String submittedBy;
    private Timestamp submittedOn;
    private String quorumOverrideReason;

    private ApprovalForumAgmAgendaMeetingEntity agmAgendaMeetingByAgmId;

    @Id
    @Column(name = "AGI_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgiId() {
        return agiId;
    }

    public void setAgiId(int agiId) {
        this.agiId = agiId;
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
    @Column(name = "InkombaId")
    public String getInkombaId() {
        return inkombaId;
    }

    public void setInkombaId(String inkombaId) {
        this.inkombaId = inkombaId;
    }

    @Basic
    @Column(name = "CounterpartyName", nullable = false)
    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    @Basic
    @Column(name = "BusinessUnit")
    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    @Basic
    @Column(name = "PurposeOfSubmission", nullable = false)
    public String getPurposeOfSubmission() {
        return purposeOfSubmission;
    }

    public void setPurposeOfSubmission(String purposeOfSubmission) {
        this.purposeOfSubmission = purposeOfSubmission;
    }

    @Basic
    @Column(name = "PurposeDetails")
    public String getPurposeDetails() {
        return purposeDetails;
    }

    public void setPurposeDetails(String purposeDetails) {
        this.purposeDetails = purposeDetails;
    }

    @Basic
    @Column(name = "DcpZar")
    public Double getDcpZar() {
        return dcpZar;
    }

    public void setDcpZar(Double dcpZar) {
        this.dcpZar = dcpZar;
    }

    @Basic
    @Column(name = "RatingBand")
    public String getRatingBand() {
        return ratingBand;
    }

    public void setRatingBand(String ratingBand) {
        this.ratingBand = ratingBand;
    }

    @Basic
    @Column(name = "NumberOfPages")
    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    @Basic
    @Column(name = "WithinPageGuidance", nullable = false)
    public boolean isWithinPageGuidance() {
        return withinPageGuidance;
    }

    public void setWithinPageGuidance(boolean withinPageGuidance) {
        this.withinPageGuidance = withinPageGuidance;
    }

    @Basic
    @Column(name = "EstimatedMinutes")
    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    @Basic
    @Column(name = "PrimaryAnalyst")
    public String getPrimaryAnalyst() {
        return primaryAnalyst;
    }

    public void setPrimaryAnalyst(String primaryAnalyst) {
        this.primaryAnalyst = primaryAnalyst;
    }

    @Basic
    @Column(name = "PrimaryAnalystId")
    public String getPrimaryAnalystId() {
        return primaryAnalystId;
    }

    public void setPrimaryAnalystId(String primaryAnalystId) {
        this.primaryAnalystId = primaryAnalystId;
    }

    @Basic
    @Column(name = "ReviewAnalyst")
    public String getReviewAnalyst() {
        return reviewAnalyst;
    }

    public void setReviewAnalyst(String reviewAnalyst) {
        this.reviewAnalyst = reviewAnalyst;
    }

    @Basic
    @Column(name = "ReviewAnalystId")
    public String getReviewAnalystId() {
        return reviewAnalystId;
    }

    public void setReviewAnalystId(String reviewAnalystId) {
        this.reviewAnalystId = reviewAnalystId;
    }

    @Basic
    @Column(name = "CreditTeam")
    public String getCreditTeam() {
        return creditTeam;
    }

    public void setCreditTeam(String creditTeam) {
        this.creditTeam = creditTeam;
    }

    @Basic
    @Column(name = "BusinessTeam")
    public String getBusinessTeam() {
        return businessTeam;
    }

    public void setBusinessTeam(String businessTeam) {
        this.businessTeam = businessTeam;
    }

    @Basic
    @Column(name = "CreditSponsor")
    public String getCreditSponsor() {
        return creditSponsor;
    }

    public void setCreditSponsor(String creditSponsor) {
        this.creditSponsor = creditSponsor;
    }

    @Basic
    @Column(name = "CreditSponsorId")
    public String getCreditSponsorId() {
        return creditSponsorId;
    }

    public void setCreditSponsorId(String creditSponsorId) {
        this.creditSponsorId = creditSponsorId;
    }

    @Basic
    @Column(name = "BuHead")
    public String getBuHead() {
        return buHead;
    }

    public void setBuHead(String buHead) {
        this.buHead = buHead;
    }

    @Basic
    @Column(name = "BuHeadId")
    public String getBuHeadId() {
        return buHeadId;
    }

    public void setBuHeadId(String buHeadId) {
        this.buHeadId = buHeadId;
    }

    @Basic
    @Column(name = "LateSubmission", nullable = false)
    public boolean isLateSubmission() {
        return lateSubmission;
    }

    public void setLateSubmission(boolean lateSubmission) {
        this.lateSubmission = lateSubmission;
    }

    @Basic
    @Column(name = "LateApprovedBy")
    public String getLateApprovedBy() {
        return lateApprovedBy;
    }

    public void setLateApprovedBy(String lateApprovedBy) {
        this.lateApprovedBy = lateApprovedBy;
    }

    @Basic
    @Column(name = "LateApprovedOn")
    public Timestamp getLateApprovedOn() {
        return lateApprovedOn;
    }

    public void setLateApprovedOn(Timestamp lateApprovedOn) {
        this.lateApprovedOn = lateApprovedOn;
    }

    @Basic
    @Column(name = "OutOfGuidanceApprovedBy")
    public String getOutOfGuidanceApprovedBy() {
        return outOfGuidanceApprovedBy;
    }

    public void setOutOfGuidanceApprovedBy(String outOfGuidanceApprovedBy) {
        this.outOfGuidanceApprovedBy = outOfGuidanceApprovedBy;
    }

    @Basic
    @Column(name = "OutOfGuidanceApprovedOn")
    public Timestamp getOutOfGuidanceApprovedOn() {
        return outOfGuidanceApprovedOn;
    }

    public void setOutOfGuidanceApprovedOn(Timestamp outOfGuidanceApprovedOn) {
        this.outOfGuidanceApprovedOn = outOfGuidanceApprovedOn;
    }

    @Basic
    @Column(name = "ItemStatus", nullable = false)
    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    @Basic
    @Column(name = "AgendaNumber")
    public String getAgendaNumber() {
        return agendaNumber;
    }

    public void setAgendaNumber(String agendaNumber) {
        this.agendaNumber = agendaNumber;
    }

    @Basic
    @Column(name = "StartTime")
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    @Basic
    @Column(name = "EndTime")
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    @Basic
    @Column(name = "RoundRobin", nullable = false)
    public boolean isRoundRobin() { return roundRobin; }
    public void setRoundRobin(boolean roundRobin) { this.roundRobin = roundRobin; }

    @Basic
    @Column(name = "Chair")
    public String getChair() { return chair; }
    public void setChair(String chair) { this.chair = chair; }

    @Basic
    @Column(name = "AttendeeEmails")
    public String getAttendeeEmails() { return attendeeEmails; }
    public void setAttendeeEmails(String attendeeEmails) { this.attendeeEmails = attendeeEmails; }

    @Basic
    @Column(name = "LecRep")
    public String getLecRep() { return lecRep; }
    public void setLecRep(String lecRep) { this.lecRep = lecRep; }

    @Basic
    @Column(name = "EqMemberFrmExec")
    public String getEqMemberFrmExec() { return eqMemberFrmExec; }
    public void setEqMemberFrmExec(String eqMemberFrmExec) { this.eqMemberFrmExec = eqMemberFrmExec; }

    @Basic
    @Column(name = "BusinessRep1")
    public String getBusinessRep1() { return businessRep1; }
    public void setBusinessRep1(String businessRep1) { this.businessRep1 = businessRep1; }

    @Basic
    @Column(name = "SeniorBusinessRep")
    public String getSeniorBusinessRep() { return seniorBusinessRep; }
    public void setSeniorBusinessRep(String seniorBusinessRep) { this.seniorBusinessRep = seniorBusinessRep; }

    @Basic
    @Column(name = "PortfolioCce1")
    public String getPortfolioCce1() { return portfolioCce1; }
    public void setPortfolioCce1(String portfolioCce1) { this.portfolioCce1 = portfolioCce1; }

    @Basic
    @Column(name = "PortfolioCce2")
    public String getPortfolioCce2() { return portfolioCce2; }
    public void setPortfolioCce2(String portfolioCce2) { this.portfolioCce2 = portfolioCce2; }

    @Basic
    @Column(name = "IndependentMember")
    public String getIndependentMember() { return independentMember; }
    public void setIndependentMember(String independentMember) { this.independentMember = independentMember; }

    @Basic
    @Column(name = "Scribe")
    public String getScribe() { return scribe; }
    public void setScribe(String scribe) { this.scribe = scribe; }

    @Basic
    @Column(name = "MezzOverlayAttendees")
    public String getMezzOverlayAttendees() { return mezzOverlayAttendees; }
    public void setMezzOverlayAttendees(String mezzOverlayAttendees) { this.mezzOverlayAttendees = mezzOverlayAttendees; }

    @Basic
    @Column(name = "EsgOverlayAttendees")
    public String getEsgOverlayAttendees() { return esgOverlayAttendees; }
    public void setEsgOverlayAttendees(String esgOverlayAttendees) { this.esgOverlayAttendees = esgOverlayAttendees; }

    @Basic
    @Column(name = "AdditionalTeamAttendees")
    public String getAdditionalTeamAttendees() { return additionalTeamAttendees; }
    public void setAdditionalTeamAttendees(String additionalTeamAttendees) { this.additionalTeamAttendees = additionalTeamAttendees; }

    @Basic
    @Column(name = "CountryOfRisk")
    public String getCountryOfRisk() { return countryOfRisk; }
    public void setCountryOfRisk(String countryOfRisk) { this.countryOfRisk = countryOfRisk; }

    @Basic
    @Column(name = "IndustryClassification")
    public String getIndustryClassification() { return industryClassification; }
    public void setIndustryClassification(String industryClassification) { this.industryClassification = industryClassification; }

    @Basic
    @Column(name = "LecCounterparty", nullable = false)
    public boolean isLecCounterparty() { return lecCounterparty; }
    public void setLecCounterparty(boolean lecCounterparty) { this.lecCounterparty = lecCounterparty; }

    @Basic
    @Column(name = "DistributionSensitivities")
    public String getDistributionSensitivities() { return distributionSensitivities; }
    public void setDistributionSensitivities(String distributionSensitivities) { this.distributionSensitivities = distributionSensitivities; }

    @Basic
    @Column(name = "ApplicantRelationship")
    public String getApplicantRelationship() { return applicantRelationship; }
    public void setApplicantRelationship(String applicantRelationship) { this.applicantRelationship = applicantRelationship; }

    @Basic
    @Column(name = "ApplicationType")
    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }

    @Basic
    @Column(name = "ProposalReceived", nullable = false)
    public boolean isProposalReceived() { return proposalReceived; }
    public void setProposalReceived(boolean proposalReceived) { this.proposalReceived = proposalReceived; }

    @Basic
    @Column(name = "ProposalDistributedEmail", nullable = false)
    public boolean isProposalDistributedEmail() { return proposalDistributedEmail; }
    public void setProposalDistributedEmail(boolean proposalDistributedEmail) { this.proposalDistributedEmail = proposalDistributedEmail; }

    @Basic
    @Column(name = "ProposalOnDiligent", nullable = false)
    public boolean isProposalOnDiligent() { return proposalOnDiligent; }
    public void setProposalOnDiligent(boolean proposalOnDiligent) { this.proposalOnDiligent = proposalOnDiligent; }

    @Basic
    @Column(name = "Comments")
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    @Basic
    @Column(name = "LateSubmissionReason")
    public String getLateSubmissionReason() { return lateSubmissionReason; }
    public void setLateSubmissionReason(String lateSubmissionReason) { this.lateSubmissionReason = lateSubmissionReason; }

    // ---- BRS alignment (V13): submission audit and quorum override ----

    @Basic
    @Column(name = "SubmittedBy")
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    @Basic
    @Column(name = "SubmittedOn")
    public Timestamp getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(Timestamp submittedOn) { this.submittedOn = submittedOn; }

    @Basic
    @Column(name = "QuorumOverrideReason", length = 1000)
    public String getQuorumOverrideReason() { return quorumOverrideReason; }
    public void setQuorumOverrideReason(String quorumOverrideReason) { this.quorumOverrideReason = quorumOverrideReason; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGM_ID", referencedColumnName = "AGM_ID", nullable = false)
    public ApprovalForumAgmAgendaMeetingEntity getAgmAgendaMeetingByAgmId() {
        return agmAgendaMeetingByAgmId;
    }

    public void setAgmAgendaMeetingByAgmId(ApprovalForumAgmAgendaMeetingEntity agmAgendaMeetingByAgmId) {
        this.agmAgendaMeetingByAgmId = agmAgendaMeetingByAgmId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgiAgendaItemEntity that = (ApprovalForumAgiAgendaItemEntity) o;
        return agiId == that.agiId &&
                Objects.equals(counterpartyName, that.counterpartyName) &&
                Objects.equals(itemStatus, that.itemStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agiId, counterpartyName, itemStatus);
    }

}
