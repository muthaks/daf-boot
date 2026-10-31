# Agenda API data contract

This is a generated backend contract reference for the Agenda Builder API in daf-boot. Literal declarations below are part of the specification; requirements.md and design.md explain them. It is not evidence of a live service, environment or database.

Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. No API, directory or database is accessed.


Every request and response type in `model/agenda`. Fields are listed exactly as declared, with their initialisers. Accessors that only read or write a field are omitted; any accessor with logic is reproduced. Timestamps are `java.sql.Timestamp` and serialise as epoch milliseconds unless an annotation says otherwise.

## AgendaBookingDraftDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaBookingDraftDTO.java`. SHA-256: `553117db513c0a3c6d1bd19c213df24c631bfc875c6d744bee2eab28b032740c`.

A booking the analyst saved part-way through the booking wizard, to continue later from any PC. `payload` is the wizard's own state (JSON) and is not interpreted here; the other fields are copies of it for the drafts list. A draft reserves no slot and is not an agenda item.

Declaration: `public class AgendaBookingDraftDTO`

```java
private int agbdId;
private Integer agcId;
private String committeeName;
private Timestamp meetingDate;
private String counterpartyName;
private int step;
private String payload;
private Timestamp createdOn;
private Timestamp updatedOn;
```

## AgendaCasdmApplicationDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaCasdmApplicationDTO.java`. SHA-256: `6bde69ec183fa777c0cad835d0078196e68882fcf91876ab5bc296591a4b3811`.

Result of the direct CASDM REST check for the latest credit application of a counterparty (GET /rest/credit-application/latest-for-inkomba). Drives the agenda booking flag: found + status tell the analyst whether a WIP / sub-approval application exists on CASDM.

Declaration: `public class AgendaCasdmApplicationDTO`

```java
private boolean configured = true;
private boolean found;
private Integer applicationId;
private String status;
private boolean inSubReview;
private boolean inSubApproval;
private boolean approved;
// Prepopulation data lifted from the CASDM application (same source DAF capture uses),
    // so the agenda booking can prefill instead of re-capturing.
    private String creditAnalyst;
private String creditAnalystEmail;
private String reviewAnalyst;
private String reviewAnalystEmail;
private String proposedRating;
private String countryOfRisk;
private String purposeOfSubmission;
private String meetingName;
```

## AgendaCommitteeDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaCommitteeDTO.java`. SHA-256: `243c6cb1f0ac6f104a11a38b5018c5b8092b4b678ab36d06b2162035e3cbcd69`.

Declaration: `public class AgendaCommitteeDTO`

```java
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
```

## AgendaConditionDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaConditionDTO.java`. SHA-256: `99baf77c22bd7ec0d3848b9dffc3cc504bb924253980aad8b766d1efac40ac82`.

A condition, action or follow-up on an agenda item (BRS section 6.8).

Declaration: `public class AgendaConditionDTO`

```java
private int agcaId;
private int agiId;
private String conditionType;
private String description;
private String ownerId;
private String ownerName;
private Timestamp dueDate;
private String status;
private String evidenceRef;
private String closureComments;
private String closedBy;
private Timestamp closedOn;
private String createdBy;
private Timestamp createdOn;
```

## AgendaInvitationResultDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaInvitationResultDTO.java`. SHA-256: `db06d84b79e2cc8f9be995592a9140109e7e3527cca14d021a8a62c3d9f38534`.

The outcome of one calendar invitation in a Send invitations / Send update run: the meeting's own event for the committee's standing roles (agiId null) or one approved item's event.

Declaration: `public class AgendaInvitationResultDTO`

```java
public static final String SENT = "Sent";
public static final String UPDATED = "Updated";
public static final String UNCHANGED = "Unchanged";
public static final String SKIPPED = "Skipped";
public static final String FAILED = "Failed";
private Integer agiId;
private String label;
private String outcome;
private String message;
```
```java
    public AgendaInvitationResultDTO() {
    }
```
```java
    public AgendaInvitationResultDTO(Integer agiId, String label, String outcome, String message) {
        this.agiId = agiId;
        this.label = label;
        this.outcome = outcome;
        this.message = message;
    }
```

## AgendaItemDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaItemDTO.java`. SHA-256: `9f5f1319102ebb7274b8658494577d4eda6dd1ccd2b4f62787d5a33224ec3d6b`.

Declaration: `public class AgendaItemDTO`

```java
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
private AgendaPostMeetingDTO postMeeting;
// ---- BRS alignment (V13) ----
    private boolean canReassignReviewer;
/** The caller may move this item to another meeting date and change its quorum roster (see AgendaService). */
    private boolean canChangeSchedule;
private String submittedBy;
private Timestamp submittedOn;
private String quorumOverrideReason;
private Timestamp cutOffOn;
private List<AgendaParticipantDTO> participants;
private List<AgendaWorkflowActionDTO> actions;
private List<AgendaConditionDTO> conditions;
private AgendaQuorumResultDTO quorum;
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
// ---- The item's own calendar invitation (V17) ----
    private String graphEventId;
private String teamsJoinUrl;
private Timestamp invitationSentOn;
private String invitationSentBy;
/** The invitation was sent and the item's slot, people, details or paper changed since. */
    private boolean invitationNeedsUpdate;
```

## AgendaItemDocumentDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaItemDocumentDTO.java`. SHA-256: `36ab5fa6884471ab32bf4469bcea1ac5ece57bf5a3b8e895c42b3fbc0d1ff26c`.

Declaration: `public class AgendaItemDocumentDTO`

```java
private int agidId;
private int agiId;
private String docStashId;
private String docType;
private String fileName;
private String uploadedBy;
private Timestamp uploadedOn;
private Long fileSize;
private String fileExtension;
// ---- BRS alignment (V13) ----
    private boolean mandatory;
private String validationStatus;
private String validatedBy;
private Timestamp validatedOn;
private String validationComment;
private int version;
```

## AgendaMeetingDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaMeetingDTO.java`. SHA-256: `559fc2ed3e238c26af4957f1a18a150305ff27c11def5a801401b0161c2c1756`.

Declaration: `public class AgendaMeetingDTO`

```java
private int agmId;
private int agcId;
private String committeeName;
private String mandateLevel;
private int quorumSize;
private String chair;
private Timestamp meetingDate;
private String heldVia;
private String status;
private Timestamp cutOffOn;
private String lockedBy;
private Timestamp lockedOn;
private List<AgendaItemDTO> items;
// ---- BRS alignment (V13) ----
    private String startTime;
private String endTime;
private String venue;
private String teamsJoinUrl;
private String graphEventId;
private Timestamp invitationSentOn;
private String invitationSentBy;
private boolean invitationNeedsUpdate;
/** Every approved item with people to invite, and the standing roles, hold a sent invitation. */
    private boolean invitationsSent;
private String actualStartTime;
private String actualEndTime;
private Boolean quorumAchieved;
private String cancelledBy;
private Timestamp cancelledOn;
private String cancelReason;
private String closedBy;
private Timestamp closedOn;
private List<AgendaMeetingRecordDTO> records;
private List<AgendaWorkflowActionDTO> actions;
/** Set on the answer to Send invitations / Send update: what happened to each invitation. */
    private List<AgendaInvitationResultDTO> invitationResults;
```

## AgendaMeetingRecordDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaMeetingRecordDTO.java`. SHA-256: `912b510c4c92815b49bfc019b20148ff6594cc3f36ec07b3d184d1b0d1978315`.

Reference to minutes / transcript / recording / pack for a meeting (BR-020).

Declaration: `public class AgendaMeetingRecordDTO`

```java
private int agmrId;
private int agmId;
private String recordType;
private String name;
private String location;
private String createdBy;
private Timestamp createdOn;
```

## AgendaNotificationDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaNotificationDTO.java`. SHA-256: `29d4b79aa1700e7146267fb9ba216d7b77f477188ae3009492857e6c78de29b2`.

Declaration: `public class AgendaNotificationDTO`

```java
private int agnId;
private Integer agiId;
private Integer agmId;
private String notificationType;
private String recipients;
private String subject;
private String status;
private String errorMessage;
private Timestamp sentOn;
private String reason;
private String actionBy;
private String message;
private Integer workflowActionId;
```

## AgendaParticipantDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaParticipantDTO.java`. SHA-256: `b22cfc0ade9eeaee687185ed335e121ca5da01cf7e89c3b7534bb6a4f4067834`.

A person in a committee role on an agenda item (BRS section 6.4).

Declaration: `public class AgendaParticipantDTO`

```java
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
```

## AgendaPostMeetingDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaPostMeetingDTO.java`. SHA-256: `ee4e12260753df7cc7122a9b979f0555bb79d41e924cbce7eb7a54c99e32d440`.

Declaration: `public class AgendaPostMeetingDTO`

```java
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
// ---- BRS alignment (V13) ----
    private String decisionType;
private Timestamp decisionDate;
```

## AgendaQuorumResultDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaQuorumResultDTO.java`. SHA-256: `975c72f02f7ceb44c67a37fc32a0342151bf1ed40514be056beaf7d475c8a739`.

Result of the role-based quorum evaluation for an item (RULE-005).

Declaration: `public class AgendaQuorumResultDTO`

```java
private int agiId;
private boolean met;
private int required;
private int eligibleCount;
private List<String> countedRoles = new ArrayList<>();
private List<String> missingRoles = new ArrayList<>();
private String overrideReason;
```

## AgendaReportSummaryDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaReportSummaryDTO.java`. SHA-256: `cea8002e9873d7018b63ebf07fd815809fb59ca09d7499fdf26abae99291cb41`.

Operational reporting aggregates for a date range (BRS section 10).

Declaration: `public class AgendaReportSummaryDTO`

```java
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
```

## AgendaWorkflowActionDTO

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/AgendaWorkflowActionDTO.java`. SHA-256: `72e3c4c725d7712e0679ed66c28019112e8aaae41323276c6aee26d800322dbb`.

One workflow action on an item or meeting (BRS section 6.6).

Declaration: `public class AgendaWorkflowActionDTO`

```java
private int agwId;
private Integer agiId;
private Integer agmId;
private String action;
private String fromStatus;
private String toStatus;
private String comments;
private boolean overrideFlag;
private String overrideReason;
private String actionBy;
private Timestamp actionOn;
```

## BookAgendaItemRequest

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/BookAgendaItemRequest.java`. SHA-256: `7c95a7e39d38e9ac6809b0efb632e2b43c62da3219fbc488dc1b44addd1e9312`.

Declaration: `public class BookAgendaItemRequest`

```java
private int agcId;
private Timestamp meetingDate;
private String heldVia;
private AgendaItemDTO item;
/** The saved booking draft this booking was continued from; it is discarded once the item is booked. */
    private Integer draftId;
```

## MoveAgendaItemRequest

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/MoveAgendaItemRequest.java`. SHA-256: `bfc2f95d7a5256d535ac5c14073e06f81489643a35e4a7e541ee0e186a652812`.

Moves a booked item to another meeting date of its committee, or another start time on the same date.

Declaration: `public class MoveAgendaItemRequest`

```java
private Timestamp meetingDate;
private String startTime;
private String lateSubmissionReason;
private String reason;
```

## QuorumExceptionRequest

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/QuorumExceptionRequest.java`. SHA-256: `e34b10c0ee42ded4b35b7414d3176201c2ed24105c21f17dda6cab5a35da0e4f`.

Asking for a quorum exception (reason), or deciding one (approved, with a note; a decline needs the note).

Declaration: `public class QuorumExceptionRequest`

```java
private String reason;
private Boolean approved;
private String note;
```

## ReassignAgendaReviewerRequest

Source: `api/src/main/java/za/co/rmb/approvalforum/model/agenda/ReassignAgendaReviewerRequest.java`. SHA-256: `6b1ba07b9773bb0d6340d188979ed8a467deb44cf1c5188aaceaf7a801e6b310`.

A targeted, reasoned correction of the reviewer on a pending submission.

Declaration: `public class ReassignAgendaReviewerRequest`

```java
private String reviewerId;
private String reviewerName;
private String expectedReviewerId;
private String reason;
```
