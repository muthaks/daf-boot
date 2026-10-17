package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;
import java.util.List;

public class AgendaItemDTO {

    private int agiId;
    private int agmId;
    private String committeeName;
    private String mandateLevel;
    private Timestamp meetingDate;
    private String meetingStatus;
    private String createdBy;
    private Timestamp createdOn;
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
    private Timestamp approvedOn;
    public Timestamp getApprovedOn() { return approvedOn; }
    public void setApprovedOn(Timestamp value) { this.approvedOn = value; }
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

    private List<AgendaItemDocumentDTO> documents;
    private List<String> requiredDocumentTypes;
    public List<String> getRequiredDocumentTypes() { return requiredDocumentTypes; }
    public void setRequiredDocumentTypes(List<String> types) { this.requiredDocumentTypes = types; }
    private AgendaPostMeetingDTO postMeeting;

    public int getAgiId() { return agiId; }
    public void setAgiId(int agiId) { this.agiId = agiId; }

    public int getAgmId() { return agmId; }
    public void setAgmId(int agmId) { this.agmId = agmId; }

    public String getCommitteeName() { return committeeName; }
    public void setCommitteeName(String committeeName) { this.committeeName = committeeName; }

    public String getMandateLevel() { return mandateLevel; }
    public void setMandateLevel(String mandateLevel) { this.mandateLevel = mandateLevel; }

    public Timestamp getMeetingDate() { return meetingDate; }
    public void setMeetingDate(Timestamp meetingDate) { this.meetingDate = meetingDate; }

    public String getMeetingStatus() { return meetingStatus; }
    public void setMeetingStatus(String meetingStatus) { this.meetingStatus = meetingStatus; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedOn() { return createdOn; }
    public void setCreatedOn(Timestamp createdOn) { this.createdOn = createdOn; }

    public String getInkombaId() { return inkombaId; }
    public void setInkombaId(String inkombaId) { this.inkombaId = inkombaId; }

    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }

    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }

    public String getPurposeOfSubmission() { return purposeOfSubmission; }
    public void setPurposeOfSubmission(String purposeOfSubmission) { this.purposeOfSubmission = purposeOfSubmission; }

    public String getPurposeDetails() { return purposeDetails; }
    public void setPurposeDetails(String purposeDetails) { this.purposeDetails = purposeDetails; }

    public Double getDcpZar() { return dcpZar; }
    public void setDcpZar(Double dcpZar) { this.dcpZar = dcpZar; }

    public String getRatingBand() { return ratingBand; }
    public void setRatingBand(String ratingBand) { this.ratingBand = ratingBand; }

    public Integer getNumberOfPages() { return numberOfPages; }
    public void setNumberOfPages(Integer numberOfPages) { this.numberOfPages = numberOfPages; }

    public boolean isWithinPageGuidance() { return withinPageGuidance; }
    public void setWithinPageGuidance(boolean withinPageGuidance) { this.withinPageGuidance = withinPageGuidance; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public String getPrimaryAnalyst() { return primaryAnalyst; }
    public void setPrimaryAnalyst(String primaryAnalyst) { this.primaryAnalyst = primaryAnalyst; }

    public String getPrimaryAnalystId() { return primaryAnalystId; }
    public void setPrimaryAnalystId(String primaryAnalystId) { this.primaryAnalystId = primaryAnalystId; }

    public String getReviewAnalyst() { return reviewAnalyst; }
    public void setReviewAnalyst(String reviewAnalyst) { this.reviewAnalyst = reviewAnalyst; }

    public String getReviewAnalystId() { return reviewAnalystId; }
    public void setReviewAnalystId(String reviewAnalystId) { this.reviewAnalystId = reviewAnalystId; }

    public String getCreditTeam() { return creditTeam; }
    public void setCreditTeam(String creditTeam) { this.creditTeam = creditTeam; }

    public String getBusinessTeam() { return businessTeam; }
    public void setBusinessTeam(String businessTeam) { this.businessTeam = businessTeam; }

    public String getCreditSponsor() { return creditSponsor; }
    public void setCreditSponsor(String creditSponsor) { this.creditSponsor = creditSponsor; }

    public String getCreditSponsorId() { return creditSponsorId; }
    public void setCreditSponsorId(String creditSponsorId) { this.creditSponsorId = creditSponsorId; }

    public String getBuHead() { return buHead; }
    public void setBuHead(String buHead) { this.buHead = buHead; }

    public String getBuHeadId() { return buHeadId; }
    public void setBuHeadId(String buHeadId) { this.buHeadId = buHeadId; }

    public boolean isLateSubmission() { return lateSubmission; }
    public void setLateSubmission(boolean lateSubmission) { this.lateSubmission = lateSubmission; }

    public String getLateApprovedBy() { return lateApprovedBy; }
    public void setLateApprovedBy(String lateApprovedBy) { this.lateApprovedBy = lateApprovedBy; }

    public Timestamp getLateApprovedOn() { return lateApprovedOn; }
    public void setLateApprovedOn(Timestamp lateApprovedOn) { this.lateApprovedOn = lateApprovedOn; }

    public String getOutOfGuidanceApprovedBy() { return outOfGuidanceApprovedBy; }
    public void setOutOfGuidanceApprovedBy(String outOfGuidanceApprovedBy) { this.outOfGuidanceApprovedBy = outOfGuidanceApprovedBy; }

    public Timestamp getOutOfGuidanceApprovedOn() { return outOfGuidanceApprovedOn; }
    public void setOutOfGuidanceApprovedOn(Timestamp outOfGuidanceApprovedOn) { this.outOfGuidanceApprovedOn = outOfGuidanceApprovedOn; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getAgendaNumber() { return agendaNumber; }
    public void setAgendaNumber(String agendaNumber) { this.agendaNumber = agendaNumber; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isRoundRobin() { return roundRobin; }
    public void setRoundRobin(boolean roundRobin) { this.roundRobin = roundRobin; }

    public String getChair() { return chair; }
    public void setChair(String chair) { this.chair = chair; }

    public String getAttendeeEmails() { return attendeeEmails; }
    public void setAttendeeEmails(String attendeeEmails) { this.attendeeEmails = attendeeEmails; }

    public String getLecRep() { return lecRep; }
    public void setLecRep(String lecRep) { this.lecRep = lecRep; }

    public String getEqMemberFrmExec() { return eqMemberFrmExec; }
    public void setEqMemberFrmExec(String eqMemberFrmExec) { this.eqMemberFrmExec = eqMemberFrmExec; }

    public String getBusinessRep1() { return businessRep1; }
    public void setBusinessRep1(String businessRep1) { this.businessRep1 = businessRep1; }

    public String getSeniorBusinessRep() { return seniorBusinessRep; }
    public void setSeniorBusinessRep(String seniorBusinessRep) { this.seniorBusinessRep = seniorBusinessRep; }

    public String getPortfolioCce1() { return portfolioCce1; }
    public void setPortfolioCce1(String portfolioCce1) { this.portfolioCce1 = portfolioCce1; }

    public String getPortfolioCce2() { return portfolioCce2; }
    public void setPortfolioCce2(String portfolioCce2) { this.portfolioCce2 = portfolioCce2; }

    public String getIndependentMember() { return independentMember; }
    public void setIndependentMember(String independentMember) { this.independentMember = independentMember; }

    public String getScribe() { return scribe; }
    public void setScribe(String scribe) { this.scribe = scribe; }

    public String getMezzOverlayAttendees() { return mezzOverlayAttendees; }
    public void setMezzOverlayAttendees(String mezzOverlayAttendees) { this.mezzOverlayAttendees = mezzOverlayAttendees; }

    public String getEsgOverlayAttendees() { return esgOverlayAttendees; }
    public void setEsgOverlayAttendees(String esgOverlayAttendees) { this.esgOverlayAttendees = esgOverlayAttendees; }

    public String getAdditionalTeamAttendees() { return additionalTeamAttendees; }
    public void setAdditionalTeamAttendees(String additionalTeamAttendees) { this.additionalTeamAttendees = additionalTeamAttendees; }

    public String getCountryOfRisk() { return countryOfRisk; }
    public void setCountryOfRisk(String countryOfRisk) { this.countryOfRisk = countryOfRisk; }

    public String getIndustryClassification() { return industryClassification; }
    public void setIndustryClassification(String industryClassification) { this.industryClassification = industryClassification; }

    public boolean isLecCounterparty() { return lecCounterparty; }
    public void setLecCounterparty(boolean lecCounterparty) { this.lecCounterparty = lecCounterparty; }

    public String getDistributionSensitivities() { return distributionSensitivities; }
    public void setDistributionSensitivities(String distributionSensitivities) { this.distributionSensitivities = distributionSensitivities; }

    public String getApplicantRelationship() { return applicantRelationship; }
    public void setApplicantRelationship(String applicantRelationship) { this.applicantRelationship = applicantRelationship; }

    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }

    public boolean isProposalReceived() { return proposalReceived; }
    public void setProposalReceived(boolean proposalReceived) { this.proposalReceived = proposalReceived; }

    public boolean isProposalDistributedEmail() { return proposalDistributedEmail; }
    public void setProposalDistributedEmail(boolean proposalDistributedEmail) { this.proposalDistributedEmail = proposalDistributedEmail; }

    public boolean isProposalOnDiligent() { return proposalOnDiligent; }
    public void setProposalOnDiligent(boolean proposalOnDiligent) { this.proposalOnDiligent = proposalOnDiligent; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getLateSubmissionReason() { return lateSubmissionReason; }
    public void setLateSubmissionReason(String lateSubmissionReason) { this.lateSubmissionReason = lateSubmissionReason; }

    public List<AgendaItemDocumentDTO> getDocuments() { return documents; }
    public void setDocuments(List<AgendaItemDocumentDTO> documents) { this.documents = documents; }

    public AgendaPostMeetingDTO getPostMeeting() { return postMeeting; }
    public void setPostMeeting(AgendaPostMeetingDTO postMeeting) { this.postMeeting = postMeeting; }

    // ---- BRS alignment (V13) ----
    private boolean canReassignReviewer;
    public boolean isCanReassignReviewer() { return canReassignReviewer; }
    public void setCanReassignReviewer(boolean value) { canReassignReviewer = value; }

    /** The caller may move this item to another meeting date and change its quorum roster (see AgendaService). */
    private boolean canChangeSchedule;
    public boolean isCanChangeSchedule() { return canChangeSchedule; }
    public void setCanChangeSchedule(boolean value) { canChangeSchedule = value; }

    private String submittedBy;
    private Timestamp submittedOn;
    private String quorumOverrideReason;
    private Timestamp cutOffOn;
    private List<AgendaParticipantDTO> participants;
    private List<AgendaWorkflowActionDTO> actions;
    private List<AgendaConditionDTO> conditions;
    private AgendaQuorumResultDTO quorum;

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Timestamp getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(Timestamp submittedOn) { this.submittedOn = submittedOn; }

    public String getQuorumOverrideReason() { return quorumOverrideReason; }
    public void setQuorumOverrideReason(String quorumOverrideReason) { this.quorumOverrideReason = quorumOverrideReason; }

    // ---- V16: quorum exception (Requested, Approved or Declined) ----
    private String quorumExceptionStatus;
    private String quorumExceptionReason;
    private String quorumExceptionRequestedBy;
    private Timestamp quorumExceptionRequestedOn;
    private String quorumExceptionDecidedBy;
    private Timestamp quorumExceptionDecidedOn;
    private String quorumExceptionDecisionNote;
    /** The caller may ask for a quorum exception on this item (its analyst, before submission, quorum not met). */
    private boolean canRequestQuorumException;
    /** The caller may approve or decline the pending request (this committee's secretary or chair). */
    private boolean canDecideQuorumException;

    public String getQuorumExceptionStatus() { return quorumExceptionStatus; }
    public void setQuorumExceptionStatus(String value) { quorumExceptionStatus = value; }
    public String getQuorumExceptionReason() { return quorumExceptionReason; }
    public void setQuorumExceptionReason(String value) { quorumExceptionReason = value; }
    public String getQuorumExceptionRequestedBy() { return quorumExceptionRequestedBy; }
    public void setQuorumExceptionRequestedBy(String value) { quorumExceptionRequestedBy = value; }
    public Timestamp getQuorumExceptionRequestedOn() { return quorumExceptionRequestedOn; }
    public void setQuorumExceptionRequestedOn(Timestamp value) { quorumExceptionRequestedOn = value; }
    public String getQuorumExceptionDecidedBy() { return quorumExceptionDecidedBy; }
    public void setQuorumExceptionDecidedBy(String value) { quorumExceptionDecidedBy = value; }
    public Timestamp getQuorumExceptionDecidedOn() { return quorumExceptionDecidedOn; }
    public void setQuorumExceptionDecidedOn(Timestamp value) { quorumExceptionDecidedOn = value; }
    public String getQuorumExceptionDecisionNote() { return quorumExceptionDecisionNote; }
    public void setQuorumExceptionDecisionNote(String value) { quorumExceptionDecisionNote = value; }
    public boolean isCanRequestQuorumException() { return canRequestQuorumException; }
    public void setCanRequestQuorumException(boolean value) { canRequestQuorumException = value; }
    public boolean isCanDecideQuorumException() { return canDecideQuorumException; }
    public void setCanDecideQuorumException(boolean value) { canDecideQuorumException = value; }

    public Timestamp getCutOffOn() { return cutOffOn; }
    public void setCutOffOn(Timestamp cutOffOn) { this.cutOffOn = cutOffOn; }

    public List<AgendaParticipantDTO> getParticipants() { return participants; }
    public void setParticipants(List<AgendaParticipantDTO> participants) { this.participants = participants; }

    public List<AgendaWorkflowActionDTO> getActions() { return actions; }
    public void setActions(List<AgendaWorkflowActionDTO> actions) { this.actions = actions; }

    public List<AgendaConditionDTO> getConditions() { return conditions; }
    public void setConditions(List<AgendaConditionDTO> conditions) { this.conditions = conditions; }

    public AgendaQuorumResultDTO getQuorum() { return quorum; }
    public void setQuorum(AgendaQuorumResultDTO quorum) { this.quorum = quorum; }

}
