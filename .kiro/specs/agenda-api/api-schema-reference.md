# Agenda API schema reference

This is a generated backend contract reference for the Agenda Builder API in daf-boot. Literal declarations below are part of the specification; requirements.md and design.md explain them. It is not evidence of a live service, environment or database.

Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. No API, directory or database is accessed.


The agenda tables on SQL Server (`wc_approvalforum`), as mapped by the JPA entities and created by the Flyway migrations. `V1__baseline.sql` is the pre-agenda DAF baseline and holds no agenda table; agenda history starts at V2. Entities marked `@Audited` keep Envers history in the matching `A_` table.

## Entities

### ApprovalForumAgbdAgendaBookingDraftEntity → `AGBD_AgendaBookingDraft`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgbdAgendaBookingDraftEntity.java`. SHA-256: `0ff40a776737b3afb3f4a1204383e2376c3b345d468ccaa05176d749b414526e`.

Class annotations: `@Entity`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGBD_ID | @Id @Column(name = "AGBD_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgbdId` | `int` |
| OwnerId | @Basic @Column(name = "OwnerId", nullable = false) | `getOwnerId` | `String` |
| AGC_ID | @Basic @Column(name = "AGC_ID") | `getAgcId` | `Integer` |
| MeetingDate | @Basic @Column(name = "MeetingDate") | `getMeetingDate` | `Timestamp` |
| CounterpartyName | @Basic @Column(name = "CounterpartyName") | `getCounterpartyName` | `String` |
| Step | @Basic @Column(name = "Step", nullable = false) | `getStep` | `int` |
| Payload | @Basic @Column(name = "Payload", nullable = false) | `getPayload` | `String` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| UpdatedOn | @Basic @Column(name = "UpdatedOn") | `getUpdatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |

### ApprovalForumAgcAgendaCommitteeEntity → `AGC_AgendaCommittee`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgcAgendaCommitteeEntity.java`. SHA-256: `76577a894c8a0d3b2f164a664ac6633826a82a05b976dd607f3dad47a33dd16b`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGC_ID | @Id @Column(name = "AGC_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgcId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| CommitteeName | @Basic @Column(name = "CommitteeName", nullable = false) | `getCommitteeName` | `String` |
| MandateLevel | @Basic @Column(name = "MandateLevel", nullable = false) | `getMandateLevel` | `String` |
| LevelDescription | @Basic @Column(name = "LevelDescription") | `getLevelDescription` | `String` |
| QuorumSize | @Basic @Column(name = "QuorumSize", nullable = false) | `getQuorumSize` | `int` |
| MeetingDay | @Basic @Column(name = "MeetingDay", nullable = false) | `getMeetingDay` | `String` |
| Chair | @Basic @Column(name = "Chair") | `getChair` | `String` |
| ChairId | @Basic @Column(name = "ChairId") | `getChairId` | `String` |
| AltChair | @Basic @Column(name = "AltChair") | `getAltChair` | `String` |
| AltChairId | @Basic @Column(name = "AltChairId") | `getAltChairId` | `String` |
| MeetingSecretary | @Basic @Column(name = "MeetingSecretary") | `getMeetingSecretary` | `String` |
| MeetingSecretaryId | @Basic @Column(name = "MeetingSecretaryId") | `getMeetingSecretaryId` | `String` |
| MinuteTaker | @Basic @Column(name = "MinuteTaker") | `getMinuteTaker` | `String` |
| MinuteTakerId | @Basic @Column(name = "MinuteTakerId") | `getMinuteTakerId` | `String` |
| Administrator | @Basic @Column(name = "Administrator") | `getAdministrator` | `String` |
| AdministratorId | @Basic @Column(name = "AdministratorId") | `getAdministratorId` | `String` |
| ApproverComposition | @Basic @Column(name = "ApproverComposition") | `getApproverComposition` | `String` |
| PageGuidance | @Basic @Column(name = "PageGuidance") | `getPageGuidance` | `Integer` |
| ListName | @Basic @Column(name = "ListName") | `getListName` | `String` |
| EffectiveFrom | @Basic @Column(name = "EffectiveFrom") | `getEffectiveFrom` | `Timestamp` |
| EffectiveTo | @Basic @Column(name = "EffectiveTo") | `getEffectiveTo` | `Timestamp` |
| DefaultHeldVia | @Basic @Column(name = "DefaultHeldVia", length = 50) | `getDefaultHeldVia` | `String` |
| DefaultDurationMinutes | @Basic @Column(name = "DefaultDurationMinutes") | `getDefaultDurationMinutes` | `Integer` |
| CutOffDayOfWeek | @Basic @Column(name = "CutOffDayOfWeek", length = 20) | `getCutOffDayOfWeek` | `String` |
| CutOffTime | @Basic @Column(name = "CutOffTime", length = 5) | `getCutOffTime` | `String` |
| RequiredDocTypes | @Basic @Column(name = "RequiredDocTypes", length = 500) | `getRequiredDocTypes` | `String` |
| QuorumRoles | @Basic @Column(name = "QuorumRoles", length = 1000) | `getQuorumRoles` | `String` |

### ApprovalForumAgcaAgendaConditionEntity → `AGCA_AgendaCondition`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgcaAgendaConditionEntity.java`. SHA-256: `7aec9a31214c65647ae53901ff685a0a2c345cbb2abcb65607b82fba26d45afb`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGCA_ID | @Id @Column(name = "AGCA_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgcaId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| ConditionType | @Basic @Column(name = "ConditionType", nullable = false, length = 50) | `getConditionType` | `String` |
| Description | @Basic @Column(name = "Description", nullable = false) | `getDescription` | `String` |
| OwnerId | @Basic @Column(name = "OwnerId") | `getOwnerId` | `String` |
| OwnerName | @Basic @Column(name = "OwnerName") | `getOwnerName` | `String` |
| DueDate | @Basic @Column(name = "DueDate") | `getDueDate` | `Timestamp` |
| Status | @Basic @Column(name = "Status", nullable = false, length = 50) | `getStatus` | `String` |
| EvidenceRef | @Basic @Column(name = "EvidenceRef", length = 1000) | `getEvidenceRef` | `String` |
| ClosureComments | @Basic @Column(name = "ClosureComments") | `getClosureComments` | `String` |
| ClosedBy | @Basic @Column(name = "ClosedBy") | `getClosedBy` | `String` |
| ClosedOn | @Basic @Column(name = "ClosedOn") | `getClosedOn` | `Timestamp` |
| AGI_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false) | `getAgiAgendaItemByAgiId` | `ApprovalForumAgiAgendaItemEntity` |

### ApprovalForumAgiAgendaItemEntity → `AGI_AgendaItem`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgiAgendaItemEntity.java`. SHA-256: `7d6baee480a9a41d9835c4cbb5ce5d83f9612a06059220e5e6981c9fde0bdfae`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGI_ID | @Id @Column(name = "AGI_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgiId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| InkombaId | @Basic @Column(name = "InkombaId") | `getInkombaId` | `String` |
| CounterpartyName | @Basic @Column(name = "CounterpartyName", nullable = false) | `getCounterpartyName` | `String` |
| BusinessUnit | @Basic @Column(name = "BusinessUnit") | `getBusinessUnit` | `String` |
| PurposeOfSubmission | @Basic @Column(name = "PurposeOfSubmission", nullable = false) | `getPurposeOfSubmission` | `String` |
| PurposeDetails | @Basic @Column(name = "PurposeDetails") | `getPurposeDetails` | `String` |
| DcpZar | @Basic @Column(name = "DcpZar") | `getDcpZar` | `Double` |
| RatingBand | @Basic @Column(name = "RatingBand") | `getRatingBand` | `String` |
| NumberOfPages | @Basic @Column(name = "NumberOfPages") | `getNumberOfPages` | `Integer` |
| WithinPageGuidance | @Basic @Column(name = "WithinPageGuidance", nullable = false) | `isWithinPageGuidance` | `boolean` |
| EstimatedMinutes | @Basic @Column(name = "EstimatedMinutes") | `getEstimatedMinutes` | `Integer` |
| PrimaryAnalyst | @Basic @Column(name = "PrimaryAnalyst") | `getPrimaryAnalyst` | `String` |
| PrimaryAnalystId | @Basic @Column(name = "PrimaryAnalystId") | `getPrimaryAnalystId` | `String` |
| ReviewAnalyst | @Basic @Column(name = "ReviewAnalyst") | `getReviewAnalyst` | `String` |
| ReviewAnalystId | @Basic @Column(name = "ReviewAnalystId") | `getReviewAnalystId` | `String` |
| CreditTeam | @Basic @Column(name = "CreditTeam") | `getCreditTeam` | `String` |
| BusinessTeam | @Basic @Column(name = "BusinessTeam") | `getBusinessTeam` | `String` |
| CreditSponsor | @Basic @Column(name = "CreditSponsor") | `getCreditSponsor` | `String` |
| CreditSponsorId | @Basic @Column(name = "CreditSponsorId") | `getCreditSponsorId` | `String` |
| BuHead | @Basic @Column(name = "BuHead") | `getBuHead` | `String` |
| BuHeadId | @Basic @Column(name = "BuHeadId") | `getBuHeadId` | `String` |
| LateSubmission | @Basic @Column(name = "LateSubmission", nullable = false) | `isLateSubmission` | `boolean` |
| LateApprovedBy | @Basic @Column(name = "LateApprovedBy") | `getLateApprovedBy` | `String` |
| LateApprovedOn | @Basic @Column(name = "LateApprovedOn") | `getLateApprovedOn` | `Timestamp` |
| OutOfGuidanceApprovedBy | @Basic @Column(name = "OutOfGuidanceApprovedBy") | `getOutOfGuidanceApprovedBy` | `String` |
| OutOfGuidanceApprovedOn | @Basic @Column(name = "OutOfGuidanceApprovedOn") | `getOutOfGuidanceApprovedOn` | `Timestamp` |
| ItemStatus | @Basic @Column(name = "ItemStatus", nullable = false) | `getItemStatus` | `String` |
| AgendaNumber | @Basic @Column(name = "AgendaNumber") | `getAgendaNumber` | `String` |
| StartTime | @Basic @Column(name = "StartTime") | `getStartTime` | `String` |
| EndTime | @Basic @Column(name = "EndTime") | `getEndTime` | `String` |
| RoundRobin | @Basic @Column(name = "RoundRobin", nullable = false) | `isRoundRobin` | `boolean` |
| Chair | @Basic @Column(name = "Chair") | `getChair` | `String` |
| AttendeeEmails | @Basic @Column(name = "AttendeeEmails") | `getAttendeeEmails` | `String` |
| LecRep | @Basic @Column(name = "LecRep") | `getLecRep` | `String` |
| EqMemberFrmExec | @Basic @Column(name = "EqMemberFrmExec") | `getEqMemberFrmExec` | `String` |
| BusinessRep1 | @Basic @Column(name = "BusinessRep1") | `getBusinessRep1` | `String` |
| SeniorBusinessRep | @Basic @Column(name = "SeniorBusinessRep") | `getSeniorBusinessRep` | `String` |
| PortfolioCce1 | @Basic @Column(name = "PortfolioCce1") | `getPortfolioCce1` | `String` |
| PortfolioCce2 | @Basic @Column(name = "PortfolioCce2") | `getPortfolioCce2` | `String` |
| IndependentMember | @Basic @Column(name = "IndependentMember") | `getIndependentMember` | `String` |
| Scribe | @Basic @Column(name = "Scribe") | `getScribe` | `String` |
| MezzOverlayAttendees | @Basic @Column(name = "MezzOverlayAttendees") | `getMezzOverlayAttendees` | `String` |
| EsgOverlayAttendees | @Basic @Column(name = "EsgOverlayAttendees") | `getEsgOverlayAttendees` | `String` |
| AdditionalTeamAttendees | @Basic @Column(name = "AdditionalTeamAttendees") | `getAdditionalTeamAttendees` | `String` |
| CountryOfRisk | @Basic @Column(name = "CountryOfRisk") | `getCountryOfRisk` | `String` |
| IndustryClassification | @Basic @Column(name = "IndustryClassification") | `getIndustryClassification` | `String` |
| LecCounterparty | @Basic @Column(name = "LecCounterparty", nullable = false) | `isLecCounterparty` | `boolean` |
| DistributionSensitivities | @Basic @Column(name = "DistributionSensitivities") | `getDistributionSensitivities` | `String` |
| ApplicantRelationship | @Basic @Column(name = "ApplicantRelationship") | `getApplicantRelationship` | `String` |
| ApplicationType | @Basic @Column(name = "ApplicationType") | `getApplicationType` | `String` |
| ProposalReceived | @Basic @Column(name = "ProposalReceived", nullable = false) | `isProposalReceived` | `boolean` |
| ProposalDistributedEmail | @Basic @Column(name = "ProposalDistributedEmail", nullable = false) | `isProposalDistributedEmail` | `boolean` |
| ProposalOnDiligent | @Basic @Column(name = "ProposalOnDiligent", nullable = false) | `isProposalOnDiligent` | `boolean` |
| Comments | @Basic @Column(name = "Comments") | `getComments` | `String` |
| LateSubmissionReason | @Basic @Column(name = "LateSubmissionReason") | `getLateSubmissionReason` | `String` |
| SubmittedBy | @Basic @Column(name = "SubmittedBy") | `getSubmittedBy` | `String` |
| SubmittedOn | @Basic @Column(name = "SubmittedOn") | `getSubmittedOn` | `Timestamp` |
| QuorumOverrideReason | @Basic @Column(name = "QuorumOverrideReason", length = 1000) | `getQuorumOverrideReason` | `String` |
| QuorumExceptionStatus | @Basic @Column(name = "QuorumExceptionStatus", length = 20) | `getQuorumExceptionStatus` | `String` |
| QuorumExceptionReason | @Basic @Column(name = "QuorumExceptionReason", length = 1000) | `getQuorumExceptionReason` | `String` |
| QuorumExceptionRequestedBy | @Basic @Column(name = "QuorumExceptionRequestedBy") | `getQuorumExceptionRequestedBy` | `String` |
| QuorumExceptionRequestedOn | @Basic @Column(name = "QuorumExceptionRequestedOn") | `getQuorumExceptionRequestedOn` | `Timestamp` |
| QuorumExceptionDecidedBy | @Basic @Column(name = "QuorumExceptionDecidedBy") | `getQuorumExceptionDecidedBy` | `String` |
| QuorumExceptionDecidedOn | @Basic @Column(name = "QuorumExceptionDecidedOn") | `getQuorumExceptionDecidedOn` | `Timestamp` |
| QuorumExceptionDecisionNote | @Basic @Column(name = "QuorumExceptionDecisionNote", length = 1000) | `getQuorumExceptionDecisionNote` | `String` |
| GraphEventId | @Basic @Column(name = "GraphEventId") | `getGraphEventId` | `String` |
| TeamsJoinUrl | @Basic @Column(name = "TeamsJoinUrl", length = 1000) | `getTeamsJoinUrl` | `String` |
| InvitationSnapshot | @Basic @Column(name = "InvitationSnapshot", length = 64) | `getInvitationSnapshot` | `String` |
| InvitationSentOn | @Basic @Column(name = "InvitationSentOn") | `getInvitationSentOn` | `Timestamp` |
| InvitationSentBy | @Basic @Column(name = "InvitationSentBy") | `getInvitationSentBy` | `String` |
| AGM_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGM_ID", referencedColumnName = "AGM_ID", nullable = false) | `getAgmAgendaMeetingByAgmId` | `ApprovalForumAgmAgendaMeetingEntity` |

### ApprovalForumAgidAgendaItemDocumentEntity → `AGID_AgendaItemDocument`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgidAgendaItemDocumentEntity.java`. SHA-256: `2855639e289638f0675b21fe2cef30a07ce55cd8ff1239d21aeb93a95b67a08b`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGID_ID | @Id @Column(name = "AGID_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgidId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| DocStashId | @Basic @Column(name = "DocStashId") | `getDocStashId` | `String` |
| DocType | @Basic @Column(name = "DocType") | `getDocType` | `String` |
| FileName | @Basic @Column(name = "FileName") | `getFileName` | `String` |
| UploadedBy | @Basic @Column(name = "UploadedBy") | `getUploadedBy` | `String` |
| UploadedOn | @Basic @Column(name = "UploadedOn") | `getUploadedOn` | `Timestamp` |
| FileData | @NotAudited @Basic(fetch = FetchType.LAZY) @Column(name = "FileData") | `getFileData` | `byte[]` |
| FileSize | @Basic @Column(name = "FileSize") | `getFileSize` | `Long` |
| FileExtension | @Basic @Column(name = "FileExtension") | `getFileExtension` | `String` |
| Mandatory | @Basic @Column(name = "Mandatory", nullable = false) | `isMandatory` | `boolean` |
| ValidationStatus | @Basic @Column(name = "ValidationStatus", length = 20) | `getValidationStatus` | `String` |
| ValidatedBy | @Basic @Column(name = "ValidatedBy") | `getValidatedBy` | `String` |
| ValidatedOn | @Basic @Column(name = "ValidatedOn") | `getValidatedOn` | `Timestamp` |
| ValidationComment | @Basic @Column(name = "ValidationComment", length = 1000) | `getValidationComment` | `String` |
| Version | @Basic @Column(name = "Version", nullable = false) | `getVersion` | `int` |
| AGI_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false) | `getAgiAgendaItemByAgiId` | `ApprovalForumAgiAgendaItemEntity` |

### ApprovalForumAgmAgendaMeetingEntity → `AGM_AgendaMeeting`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgmAgendaMeetingEntity.java`. SHA-256: `b09bd763716d380468c60349f6b90f4341ee1924ffe048e2f7417bf76d11bb77`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGM_ID | @Id @Column(name = "AGM_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgmId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| MeetingDate | @Basic @Column(name = "MeetingDate", nullable = false) | `getMeetingDate` | `Timestamp` |
| HeldVia | @Basic @Column(name = "HeldVia") | `getHeldVia` | `String` |
| Status | @Basic @Column(name = "Status", nullable = false) | `getStatus` | `String` |
| CutOffOn | @Basic @Column(name = "CutOffOn") | `getCutOffOn` | `Timestamp` |
| LockedBy | @Basic @Column(name = "LockedBy") | `getLockedBy` | `String` |
| LockedOn | @Basic @Column(name = "LockedOn") | `getLockedOn` | `Timestamp` |
| StartTime | @Basic @Column(name = "StartTime", length = 5) | `getStartTime` | `String` |
| EndTime | @Basic @Column(name = "EndTime", length = 5) | `getEndTime` | `String` |
| Venue | @Basic @Column(name = "Venue") | `getVenue` | `String` |
| TeamsJoinUrl | @Basic @Column(name = "TeamsJoinUrl", length = 1000) | `getTeamsJoinUrl` | `String` |
| GraphEventId | @Basic @Column(name = "GraphEventId") | `getGraphEventId` | `String` |
| InvitationSnapshot | @Basic @Column(name = "InvitationSnapshot", length = 64) | `getInvitationSnapshot` | `String` |
| InvitationSentOn | @Basic @Column(name = "InvitationSentOn") | `getInvitationSentOn` | `Timestamp` |
| InvitationSentBy | @Basic @Column(name = "InvitationSentBy") | `getInvitationSentBy` | `String` |
| InvitationReopened | @Basic @Column(name = "InvitationReopened", nullable = false) | `isInvitationReopened` | `boolean` |
| ActualStartTime | @Basic @Column(name = "ActualStartTime", length = 5) | `getActualStartTime` | `String` |
| ActualEndTime | @Basic @Column(name = "ActualEndTime", length = 5) | `getActualEndTime` | `String` |
| QuorumAchieved | @Basic @Column(name = "QuorumAchieved") | `getQuorumAchieved` | `Boolean` |
| CancelledBy | @Basic @Column(name = "CancelledBy") | `getCancelledBy` | `String` |
| CancelledOn | @Basic @Column(name = "CancelledOn") | `getCancelledOn` | `Timestamp` |
| CancelReason | @Basic @Column(name = "CancelReason", length = 1000) | `getCancelReason` | `String` |
| ClosedBy | @Basic @Column(name = "ClosedBy") | `getClosedBy` | `String` |
| ClosedOn | @Basic @Column(name = "ClosedOn") | `getClosedOn` | `Timestamp` |
| AGC_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGC_ID", referencedColumnName = "AGC_ID", nullable = false) | `getAgcAgendaCommitteeByAgcId` | `ApprovalForumAgcAgendaCommitteeEntity` |

### ApprovalForumAgmrAgendaMeetingRecordEntity → `AGMR_AgendaMeetingRecord`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgmrAgendaMeetingRecordEntity.java`. SHA-256: `4f213888e1acd80b4e035a2f1664792e96b58bba1867f250819c8cddef901cc3`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGMR_ID | @Id @Column(name = "AGMR_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgmrId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| RecordType | @Basic @Column(name = "RecordType", nullable = false, length = 50) | `getRecordType` | `String` |
| Name | @Basic @Column(name = "Name", nullable = false) | `getName` | `String` |
| Location | @Basic @Column(name = "Location", nullable = false, length = 1000) | `getLocation` | `String` |
| AGM_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGM_ID", referencedColumnName = "AGM_ID", nullable = false) | `getAgmAgendaMeetingByAgmId` | `ApprovalForumAgmAgendaMeetingEntity` |

### ApprovalForumAgnAgendaNotificationEntity → `AGN_AgendaNotification`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgnAgendaNotificationEntity.java`. SHA-256: `a8ffed751e1f66e4eeca7017b954a97fa7ed0f45c47621211c8319a2861bc128`.

Class annotations: `@Entity`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGN_ID | @Id @Column(name = "AGN_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgnId` | `int` |
| AGI_ID | @Basic @Column(name = "AGI_ID") | `getAgiId` | `Integer` |
| AGM_ID | @Basic @Column(name = "AGM_ID") | `getAgmId` | `Integer` |
| NotificationType | @Basic @Column(name = "NotificationType") | `getNotificationType` | `String` |
| Recipients | @Basic @Column(name = "Recipients", length = 2000) | `getRecipients` | `String` |
| Subject | @Basic @Column(name = "Subject", length = 500) | `getSubject` | `String` |
| Status | @Basic @Column(name = "Status") | `getStatus` | `String` |
| ErrorMessage | @Basic @Column(name = "ErrorMessage", length = 2000) | `getErrorMessage` | `String` |
| SentOn | @Basic @Column(name = "SentOn") | `getSentOn` | `Timestamp` |

### ApprovalForumAgpAgendaParticipantEntity → `AGP_AgendaParticipant`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgpAgendaParticipantEntity.java`. SHA-256: `7c575af35a5b995cc9b666e047b242b1ffa6e6d6e09b0e557d7b91e27f34022a`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGP_ID | @Id @Column(name = "AGP_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgpId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| PersonId | @Basic @Column(name = "PersonId") | `getPersonId` | `String` |
| PersonName | @Basic @Column(name = "PersonName", nullable = false) | `getPersonName` | `String` |
| Email | @Basic @Column(name = "Email") | `getEmail` | `String` |
| CommitteeRole | @Basic @Column(name = "CommitteeRole", nullable = false, length = 100) | `getCommitteeRole` | `String` |
| AttendanceRequired | @Basic @Column(name = "AttendanceRequired", nullable = false) | `isAttendanceRequired` | `boolean` |
| QuorumEligible | @Basic @Column(name = "QuorumEligible", nullable = false) | `isQuorumEligible` | `boolean` |
| VotingEligible | @Basic @Column(name = "VotingEligible", nullable = false) | `isVotingEligible` | `boolean` |
| IndependentFlag | @Basic @Column(name = "IndependentFlag", nullable = false) | `isIndependentFlag` | `boolean` |
| PlannedAttendance | @Basic @Column(name = "PlannedAttendance", length = 20) | `getPlannedAttendance` | `String` |
| InvitationStatus | @Basic @Column(name = "InvitationStatus", length = 20) | `getInvitationStatus` | `String` |
| ActualAttendance | @Basic @Column(name = "ActualAttendance") | `getActualAttendance` | `Boolean` |
| OverrideReason | @Basic @Column(name = "OverrideReason", length = 1000) | `getOverrideReason` | `String` |
| AGI_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false) | `getAgiAgendaItemByAgiId` | `ApprovalForumAgiAgendaItemEntity` |

### ApprovalForumAgpmAgendaPostMeetingEntity → `AGPM_AgendaPostMeeting`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgpmAgendaPostMeetingEntity.java`. SHA-256: `c53b5733359c5631a6e2b5f4e54909d3534be4991da48a55bae6e28ba2afe478`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGPM_ID | @Id @Column(name = "AGPM_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgpmId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| ActualPages | @Basic @Column(name = "ActualPages") | `getActualPages` | `Integer` |
| ActualMinutes | @Basic @Column(name = "ActualMinutes") | `getActualMinutes` | `Integer` |
| Decision | @Basic @Column(name = "Decision") | `getDecision` | `String` |
| CreditConditions | @Basic @Column(name = "CreditConditions") | `getCreditConditions` | `String` |
| AttendeesPresent | @Basic @Column(name = "AttendeesPresent") | `getAttendeesPresent` | `String` |
| MinuteTaker | @Basic @Column(name = "MinuteTaker") | `getMinuteTaker` | `String` |
| Scribe | @Basic @Column(name = "Scribe") | `getScribe` | `String` |
| Administrator | @Basic @Column(name = "Administrator") | `getAdministrator` | `String` |
| AnalystSignOffBy | @Basic @Column(name = "AnalystSignOffBy") | `getAnalystSignOffBy` | `String` |
| AnalystSignOffOn | @Basic @Column(name = "AnalystSignOffOn") | `getAnalystSignOffOn` | `Timestamp` |
| ChairSignOffBy | @Basic @Column(name = "ChairSignOffBy") | `getChairSignOffBy` | `String` |
| ChairSignOffOn | @Basic @Column(name = "ChairSignOffOn") | `getChairSignOffOn` | `Timestamp` |
| DocStashOutputId | @Basic @Column(name = "DocStashOutputId") | `getDocStashOutputId` | `String` |
| ActualRating | @Basic @Column(name = "ActualRating") | `getActualRating` | `String` |
| ActualPurpose | @Basic @Column(name = "ActualPurpose") | `getActualPurpose` | `String` |
| ActualDcp | @Basic @Column(name = "ActualDcp") | `getActualDcp` | `Double` |
| DateOfCc | @Basic @Column(name = "DateOfCc") | `getDateOfCc` | `Timestamp` |
| Locked | @Basic @Column(name = "Locked") | `isLocked` | `boolean` |
| LockedBy | @Basic @Column(name = "LockedBy") | `getLockedBy` | `String` |
| LockedOn | @Basic @Column(name = "LockedOn") | `getLockedOn` | `Timestamp` |
| DecisionType | @Basic @Column(name = "DecisionType", length = 50) | `getDecisionType` | `String` |
| DecisionDate | @Basic @Column(name = "DecisionDate") | `getDecisionDate` | `Timestamp` |
| AGI_ID | @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "AGI_ID", referencedColumnName = "AGI_ID", nullable = false) | `getAgiAgendaItemByAgiId` | `ApprovalForumAgiAgendaItemEntity` |

### ApprovalForumAgwAgendaWorkflowActionEntity → `AGW_AgendaWorkflowAction`

Source: `api/src/main/java/za/co/rmb/approvalforum/data/entity/ApprovalForumAgwAgendaWorkflowActionEntity.java`. SHA-256: `54c3d56f17076419dd6e5712fca3a79de1acd0f40ecf08a3e9a30e68716bba3b`.

Class annotations: `@Entity`, `@Audited`, `@Table`

| Column | Mapping annotations | Property | Java type |
| --- | --- | --- | --- |
| AGW_ID | @Id @Column(name = "AGW_ID", nullable = false) @GeneratedValue(strategy = GenerationType.IDENTITY) | `getAgwId` | `int` |
| CreatedBy | @Basic @Column(name = "CreatedBy", nullable = false) | `getCreatedBy` | `String` |
| CreatedOn | @Basic @Column(name = "CreatedOn", nullable = false) | `getCreatedOn` | `Timestamp` |
| Active | @Basic @Column(name = "Active", nullable = false) | `isActive` | `boolean` |
| AGI_ID | @Basic @Column(name = "AGI_ID") | `getAgiId` | `Integer` |
| AGM_ID | @Basic @Column(name = "AGM_ID") | `getAgmId` | `Integer` |
| Action | @Basic @Column(name = "Action", nullable = false) | `getAction` | `String` |
| FromStatus | @Basic @Column(name = "FromStatus") | `getFromStatus` | `String` |
| ToStatus | @Basic @Column(name = "ToStatus") | `getToStatus` | `String` |
| Comments | @Basic @Column(name = "Comments") | `getComments` | `String` |
| OverrideFlag | @Basic @Column(name = "OverrideFlag", nullable = false) | `isOverrideFlag` | `boolean` |
| OverrideReason | @Basic @Column(name = "OverrideReason", length = 1000) | `getOverrideReason` | `String` |

## Repository queries

Source: `api/src/main/java/za/co/rmb/approvalforum/data/jpa/JPAApprovalForumAgendaRepository.java`. SHA-256: `da35301028281be458e7ad8fcde81a7bd45db1da82bb8692dbe3307fb4f0d5c8`.

```java
public class JPAApprovalForumAgendaRepository implements ApprovalForumAgendaRepository {

    private final EntityManager em;

    @Inject
    JPAApprovalForumAgendaRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<ApprovalForumAgcAgendaCommitteeEntity> getCommittees() {
        return em.createQuery("from ApprovalForumAgcAgendaCommitteeEntity t " +
                        "where t.active = :active " +
                        "order by t.committeeName, t.mandateLevel",
                ApprovalForumAgcAgendaCommitteeEntity.class)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgcAgendaCommitteeEntity getCommittee(int agcId) {
        return em.find(ApprovalForumAgcAgendaCommitteeEntity.class, agcId);
    }

    @Override
    public void lockCommitteeBookings(int agcId) {
        em.find(ApprovalForumAgcAgendaCommitteeEntity.class, agcId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public ApprovalForumAgcAgendaCommitteeEntity saveCommittee(ApprovalForumAgcAgendaCommitteeEntity committee) {
        return em.merge(committee);
    }

    @Override
    public List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsForCommittee(int agcId) {
        return em.createQuery("from ApprovalForumAgmAgendaMeetingEntity t " +
                        "where t.agcAgendaCommitteeByAgcId.agcId = :agcId " +
                        "and t.active = :active " +
                        "order by t.meetingDate",
                ApprovalForumAgmAgendaMeetingEntity.class)
                .setParameter("agcId", agcId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgmAgendaMeetingEntity findMeeting(int agcId, Timestamp meetingDate) {
        try {
            return em.createQuery("from ApprovalForumAgmAgendaMeetingEntity t " +
                            "where t.agcAgendaCommitteeByAgcId.agcId = :agcId " +
                            "and t.meetingDate = :meetingDate " +
                            "and t.active = :active",
                    ApprovalForumAgmAgendaMeetingEntity.class)
                    .setParameter("agcId", agcId)
                    .setParameter("meetingDate", meetingDate)
                    .setParameter("active", true)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsPastCutOff(Timestamp now) {
        return em.createQuery("from ApprovalForumAgmAgendaMeetingEntity t " +
                        "where t.cutOffOn <= :now " +
                        "and t.status in ('Draft', 'Preliminary') " +
                        "and t.active = :active",
                ApprovalForumAgmAgendaMeetingEntity.class)
                .setParameter("now", now)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsWithCutOffBetween(Timestamp from, Timestamp to) {
        return em.createQuery("from ApprovalForumAgmAgendaMeetingEntity t " +
                        "where t.cutOffOn > :from and t.cutOffOn <= :to " +
                        "and t.status in ('Draft', 'Preliminary') " +
                        "and t.active = :active",
                ApprovalForumAgmAgendaMeetingEntity.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgmAgendaMeetingEntity getMeeting(int agmId) {
        return em.find(ApprovalForumAgmAgendaMeetingEntity.class, agmId);
    }

    @Override
    public ApprovalForumAgmAgendaMeetingEntity getMeetingForUpdate(int agmId) {
        return em.find(ApprovalForumAgmAgendaMeetingEntity.class, agmId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public List<ApprovalForumAgiAgendaItemEntity> getAllItems() {
        return em.createQuery("from ApprovalForumAgiAgendaItemEntity t " +
                        "where t.active = :active " +
                        "order by t.createdOn desc",
                ApprovalForumAgiAgendaItemEntity.class)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgmAgendaMeetingEntity saveMeeting(ApprovalForumAgmAgendaMeetingEntity meeting) {
        return em.merge(meeting);
    }

    @Override
    public List<ApprovalForumAgiAgendaItemEntity> getItemsForMeeting(int agmId) {
        return em.createQuery("from ApprovalForumAgiAgendaItemEntity t " +
                        "where t.agmAgendaMeetingByAgmId.agmId = :agmId " +
                        "and t.active = :active " +
                        "order by t.agendaNumber",
                ApprovalForumAgiAgendaItemEntity.class)
                .setParameter("agmId", agmId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public List<ApprovalForumAgiAgendaItemEntity> getItemsCreatedBy(String userId) {
        return em.createQuery("from ApprovalForumAgiAgendaItemEntity t " +
                        "where t.createdBy = :userId " +
                        "and t.active = :active " +
                        "order by t.createdOn desc",
                ApprovalForumAgiAgendaItemEntity.class)
                .setParameter("userId", userId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public List<ApprovalForumAgiAgendaItemEntity> getItemsForUser(String userId) {
        return em.createQuery("from ApprovalForumAgiAgendaItemEntity t " +
                        "where (t.createdBy = :userId or t.primaryAnalystId = :userId or t.reviewAnalystId = :userId) " +
                        "and t.active = :active " +
                        "order by t.createdOn desc",
                ApprovalForumAgiAgendaItemEntity.class)
                .setParameter("userId", userId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgiAgendaItemEntity getLatestItemForCommittee(int agcId) {
        List<ApprovalForumAgiAgendaItemEntity> list = em.createQuery(
                        "from ApprovalForumAgiAgendaItemEntity i " +
                        "where i.agmAgendaMeetingByAgmId.agcAgendaCommitteeByAgcId.agcId = :agcId " +
                        "and i.active = :active and i.itemStatus <> :removed " +
                        "order by i.createdOn desc",
                        ApprovalForumAgiAgendaItemEntity.class)
                .setParameter("agcId", agcId)
                .setParameter("active", true)
                .setParameter("removed", "Removed")
                .setMaxResults(1)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public ApprovalForumAgiAgendaItemEntity getItem(int agiId) {
        return em.find(ApprovalForumAgiAgendaItemEntity.class, agiId);
    }

    @Override
    public ApprovalForumAgiAgendaItemEntity getItemForUpdate(int agiId) {
        return em.find(ApprovalForumAgiAgendaItemEntity.class, agiId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public ApprovalForumAgiAgendaItemEntity saveItem(ApprovalForumAgiAgendaItemEntity item) {
        return em.merge(item);
    }

    @Override
    public List<ApprovalForumAgidAgendaItemDocumentEntity> getDocumentsForItem(int agiId) {
        return em.createQuery("from ApprovalForumAgidAgendaItemDocumentEntity t " +
                        "where t.agiAgendaItemByAgiId.agiId = :agiId " +
                        "and t.active = :active " +
                        "order by t.uploadedOn desc",
                ApprovalForumAgidAgendaItemDocumentEntity.class)
                .setParameter("agiId", agiId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgidAgendaItemDocumentEntity getDocument(int agidId) {
        return em.find(ApprovalForumAgidAgendaItemDocumentEntity.class, agidId);
    }

    @Override
    public ApprovalForumAgidAgendaItemDocumentEntity getDocumentForValidation(int agidId) {
        return em.find(ApprovalForumAgidAgendaItemDocumentEntity.class, agidId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public ApprovalForumAgidAgendaItemDocumentEntity saveDocument(ApprovalForumAgidAgendaItemDocumentEntity document) {
        return em.merge(document);
    }

    @Override
    public ApprovalForumAgpmAgendaPostMeetingEntity getPostMeetingForItem(int agiId) {
        try {
            return em.createQuery("from ApprovalForumAgpmAgendaPostMeetingEntity t " +
                            "where t.agiAgendaItemByAgiId.agiId = :agiId " +
                            "and t.active = :active",
                    ApprovalForumAgpmAgendaPostMeetingEntity.class)
                    .setParameter("agiId", agiId)
                    .setParameter("active", true)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public ApprovalForumAgpmAgendaPostMeetingEntity savePostMeeting(ApprovalForumAgpmAgendaPostMeetingEntity postMeeting) {
        return em.merge(postMeeting);
    }

    @Override
    public ApprovalForumAgnAgendaNotificationEntity saveNotification(ApprovalForumAgnAgendaNotificationEntity notification) {
        return em.merge(notification);
    }

    @Override
    public List<ApprovalForumAgnAgendaNotificationEntity> getNotificationsForItem(int agiId) {
        return em.createQuery("from ApprovalForumAgnAgendaNotificationEntity n " +
                        "where n.agiId = :agiId order by n.sentOn desc",
                ApprovalForumAgnAgendaNotificationEntity.class)
                .setParameter("agiId", agiId)
                .getResultList();
    }

    // ---- BRS alignment (V13) ----

    @Override
    public List<ApprovalForumAgcAgendaCommitteeEntity> getAllCommittees() {
        return em.createQuery("from ApprovalForumAgcAgendaCommitteeEntity t " +
                        "order by t.committeeName, t.mandateLevel",
                ApprovalForumAgcAgendaCommitteeEntity.class)
                .getResultList();
    }

    @Override
    public List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsBetween(Timestamp from, Timestamp to) {
        return em.createQuery("from ApprovalForumAgmAgendaMeetingEntity t " +
                        "where t.meetingDate >= :from and t.meetingDate <= :to " +
                        "and t.active = :active " +
                        "order by t.meetingDate",
                ApprovalForumAgmAgendaMeetingEntity.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public List<ApprovalForumAgiAgendaItemEntity> getItemsBetween(Timestamp from, Timestamp to) {
        return em.createQuery("from ApprovalForumAgiAgendaItemEntity t " +
                        "where t.agmAgendaMeetingByAgmId.meetingDate >= :from " +
                        "and t.agmAgendaMeetingByAgmId.meetingDate <= :to " +
                        "and t.active = :active " +
                        "order by t.agmAgendaMeetingByAgmId.meetingDate, t.agendaNumber",
                ApprovalForumAgiAgendaItemEntity.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgwAgendaWorkflowActionEntity saveWorkflowAction(ApprovalForumAgwAgendaWorkflowActionEntity action) {
        return em.merge(action);
    }

    @Override
    public List<ApprovalForumAgwAgendaWorkflowActionEntity> getWorkflowActionsForItem(int agiId) {
        return em.createQuery("from ApprovalForumAgwAgendaWorkflowActionEntity t " +
                        "where t.agiId = :agiId and t.active = :active " +
                        "order by t.createdOn desc",
                ApprovalForumAgwAgendaWorkflowActionEntity.class)
                .setParameter("agiId", agiId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public List<ApprovalForumAgwAgendaWorkflowActionEntity> getWorkflowActionsForMeeting(int agmId) {
        return em.createQuery("from ApprovalForumAgwAgendaWorkflowActionEntity t " +
                        "where t.agmId = :agmId and t.active = :active " +
                        "order by t.createdOn desc",
                ApprovalForumAgwAgendaWorkflowActionEntity.class)
                .setParameter("agmId", agmId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public List<za.co.rmb.approvalforum.data.entity.ApprovalForumAgbdAgendaBookingDraftEntity> getBookingDrafts(String ownerId) {
        return em.createQuery("from ApprovalForumAgbdAgendaBookingDraftEntity t " +
                        "where t.ownerId = :ownerId and t.active = :active " +
                        "order by coalesce(t.updatedOn, t.createdOn) desc",
                za.co.rmb.approvalforum.data.entity.ApprovalForumAgbdAgendaBookingDraftEntity.class)
                .setParameter("ownerId", ownerId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public za.co.rmb.approvalforum.data.entity.ApprovalForumAgbdAgendaBookingDraftEntity getBookingDraft(int agbdId) {
        return em.find(za.co.rmb.approvalforum.data.entity.ApprovalForumAgbdAgendaBookingDraftEntity.class, agbdId);
    }

    @Override
    public za.co.rmb.approvalforum.data.entity.ApprovalForumAgbdAgendaBookingDraftEntity saveBookingDraft(
            za.co.rmb.approvalforum.data.entity.ApprovalForumAgbdAgendaBookingDraftEntity draft) {
        return em.merge(draft);
    }

    @Override
    public List<ApprovalForumAgpAgendaParticipantEntity> getParticipantsForItem(int agiId) {
        return em.createQuery("from ApprovalForumAgpAgendaParticipantEntity t " +
                        "where t.agiAgendaItemByAgiId.agiId = :agiId and t.active = :active " +
                        "order by t.agpId",
                ApprovalForumAgpAgendaParticipantEntity.class)
                .setParameter("agiId", agiId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgpAgendaParticipantEntity saveParticipant(ApprovalForumAgpAgendaParticipantEntity participant) {
        return em.merge(participant);
    }

    @Override
    public List<ApprovalForumAgcaAgendaConditionEntity> getConditionsForItem(int agiId) {
        return em.createQuery("from ApprovalForumAgcaAgendaConditionEntity t " +
                        "where t.agiAgendaItemByAgiId.agiId = :agiId and t.active = :active " +
                        "order by t.agcaId",
                ApprovalForumAgcaAgendaConditionEntity.class)
                .setParameter("agiId", agiId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgcaAgendaConditionEntity getCondition(int agcaId) {
        return em.find(ApprovalForumAgcaAgendaConditionEntity.class, agcaId);
    }

    @Override
    public ApprovalForumAgcaAgendaConditionEntity saveCondition(ApprovalForumAgcaAgendaConditionEntity condition) {
        return em.merge(condition);
    }

    @Override
    public List<ApprovalForumAgmrAgendaMeetingRecordEntity> getRecordsForMeeting(int agmId) {
        return em.createQuery("from ApprovalForumAgmrAgendaMeetingRecordEntity t " +
                        "where t.agmAgendaMeetingByAgmId.agmId = :agmId and t.active = :active " +
                        "order by t.agmrId",
                ApprovalForumAgmrAgendaMeetingRecordEntity.class)
                .setParameter("agmId", agmId)
                .setParameter("active", true)
                .getResultList();
    }

    @Override
    public ApprovalForumAgmrAgendaMeetingRecordEntity saveRecord(ApprovalForumAgmrAgendaMeetingRecordEntity record) {
        return em.merge(record);
    }

}
```

## Migrations

### V2__CASDM-1352_agenda_tables.sql

Source: `api/src/main/resources/db/migration/V2__CASDM-1352_agenda_tables.sql`. SHA-256: `2e73489eca5be37516e944db72e246752a6869e8c12f9f98b000594f7d7a7f57`.

```sql
-- CASDM-1352: Credit Committee Agenda Builder tables
-- Live tables + Envers audit tables (A_ prefix), following WF_Workflow conventions.

CREATE TABLE [dbo].[AGC_AgendaCommittee]  (
    [AGC_ID]              	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [CommitteeName]       	nvarchar(255) NOT NULL,
    [MandateLevel]        	nvarchar(50) NOT NULL,
    [LevelDescription]    	nvarchar(100) NULL,
    [QuorumSize]          	int NOT NULL,
    [MeetingDay]          	nvarchar(20) NOT NULL,
    [Chair]               	nvarchar(255) NULL,
    [ChairId]             	nvarchar(255) NULL,
    [AltChair]            	nvarchar(255) NULL,
    [AltChairId]          	nvarchar(255) NULL,
    [MeetingSecretary]    	nvarchar(255) NULL,
    [MeetingSecretaryId]  	nvarchar(255) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [MinuteTakerId]       	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AdministratorId]     	nvarchar(255) NULL,
    [ApproverComposition] 	nvarchar(max) NULL,
    [PageGuidance]        	int NULL,
    CONSTRAINT [PK_AGC] PRIMARY KEY CLUSTERED([AGC_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGM_AgendaMeeting]  (
    [AGM_ID]              	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGC_ID]              	int NOT NULL,
    [MeetingDate]         	datetime NOT NULL,
    [HeldVia]             	nvarchar(50) NULL,
    [Status]              	nvarchar(50) NOT NULL DEFAULT ('Draft'),
    [CutOffOn]            	datetime NULL,
    [LockedBy]            	nvarchar(255) NULL,
    [LockedOn]            	datetime NULL,
    CONSTRAINT [PK_AGM] PRIMARY KEY CLUSTERED([AGM_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGI_AgendaItem]  (
    [AGI_ID]                  	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]               	nvarchar(255) NOT NULL,
    [CreatedOn]               	datetime NOT NULL DEFAULT (getdate()),
    [Active]                  	bit NOT NULL DEFAULT ((1)),
    [AGM_ID]                  	int NOT NULL,
    [InkombaId]               	nvarchar(100) NULL,
    [CounterpartyName]        	nvarchar(255) NOT NULL,
    [BusinessUnit]            	nvarchar(255) NULL,
    [PurposeOfSubmission]     	nvarchar(255) NOT NULL,
    [PurposeDetails]          	nvarchar(max) NULL,
    [DcpZar]                  	float NULL,
    [RatingBand]              	nvarchar(100) NULL,
    [NumberOfPages]           	int NULL,
    [WithinPageGuidance]      	bit NOT NULL DEFAULT ((1)),
    [EstimatedMinutes]        	int NULL,
    [PrimaryAnalyst]          	nvarchar(255) NULL,
    [PrimaryAnalystId]        	nvarchar(255) NULL,
    [ReviewAnalyst]           	nvarchar(255) NULL,
    [ReviewAnalystId]         	nvarchar(255) NULL,
    [CreditTeam]              	nvarchar(max) NULL,
    [BusinessTeam]            	nvarchar(max) NULL,
    [CreditSponsor]           	nvarchar(255) NULL,
    [CreditSponsorId]         	nvarchar(255) NULL,
    [BuHead]                  	nvarchar(255) NULL,
    [BuHeadId]                	nvarchar(255) NULL,
    [LateSubmission]          	bit NOT NULL DEFAULT ((0)),
    [LateApprovedBy]          	nvarchar(255) NULL,
    [LateApprovedOn]          	datetime NULL,
    [OutOfGuidanceApprovedBy] 	nvarchar(255) NULL,
    [OutOfGuidanceApprovedOn] 	datetime NULL,
    [ItemStatus]              	nvarchar(50) NOT NULL DEFAULT ('Captured'),
    [AgendaNumber]            	nvarchar(20) NULL,
    CONSTRAINT [PK_AGI] PRIMARY KEY CLUSTERED([AGI_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGID_AgendaItemDocument]  (
    [AGID_ID]             	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]              	int NOT NULL,
    [DocStashId]          	nvarchar(255) NOT NULL,
    [FileName]            	nvarchar(255) NULL,
    [UploadedBy]          	nvarchar(255) NULL,
    [UploadedOn]          	datetime NULL,
    CONSTRAINT [PK_AGID] PRIMARY KEY CLUSTERED([AGID_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[AGPM_AgendaPostMeeting]  (
    [AGPM_ID]             	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]              	int NOT NULL,
    [ActualPages]         	int NULL,
    [ActualMinutes]       	int NULL,
    [Decision]            	nvarchar(100) NULL,
    [CreditConditions]    	nvarchar(max) NULL,
    [AttendeesPresent]    	nvarchar(max) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [Scribe]              	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AnalystSignOffBy]    	nvarchar(255) NULL,
    [AnalystSignOffOn]    	datetime NULL,
    [ChairSignOffBy]      	nvarchar(255) NULL,
    [ChairSignOffOn]      	datetime NULL,
    [DocStashOutputId]    	nvarchar(255) NULL,
    CONSTRAINT [PK_AGPM] PRIMARY KEY CLUSTERED([AGPM_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== Envers audit tables =====

CREATE TABLE [dbo].[A_AGC_AgendaCommittee]  (
    [AGC_ID]              	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [CommitteeName]       	nvarchar(255) NULL,
    [MandateLevel]        	nvarchar(50) NULL,
    [LevelDescription]    	nvarchar(100) NULL,
    [QuorumSize]          	int NULL,
    [MeetingDay]          	nvarchar(20) NULL,
    [Chair]               	nvarchar(255) NULL,
    [ChairId]             	nvarchar(255) NULL,
    [AltChair]            	nvarchar(255) NULL,
    [AltChairId]          	nvarchar(255) NULL,
    [MeetingSecretary]    	nvarchar(255) NULL,
    [MeetingSecretaryId]  	nvarchar(255) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [MinuteTakerId]       	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AdministratorId]     	nvarchar(255) NULL,
    [ApproverComposition] 	nvarchar(max) NULL,
    [PageGuidance]        	int NULL,
    CONSTRAINT [PK_A_AGC] PRIMARY KEY CLUSTERED([AGC_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGM_AgendaMeeting]  (
    [AGM_ID]              	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGC_ID]              	int NULL,
    [MeetingDate]         	datetime NULL,
    [HeldVia]             	nvarchar(50) NULL,
    [Status]              	nvarchar(50) NULL,
    [CutOffOn]            	datetime NULL,
    [LockedBy]            	nvarchar(255) NULL,
    [LockedOn]            	datetime NULL,
    CONSTRAINT [PK_A_AGM] PRIMARY KEY CLUSTERED([AGM_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGI_AgendaItem]  (
    [AGI_ID]                  	int NOT NULL,
    [REV]                     	int NOT NULL,
    [REVTYPE]                 	tinyint NULL,
    [REVEND]                  	int NULL,
    [CreatedBy]               	nvarchar(255) NULL,
    [CreatedOn]               	datetime NULL,
    [Active]                  	bit NULL,
    [AGM_ID]                  	int NULL,
    [InkombaId]               	nvarchar(100) NULL,
    [CounterpartyName]        	nvarchar(255) NULL,
    [BusinessUnit]            	nvarchar(255) NULL,
    [PurposeOfSubmission]     	nvarchar(255) NULL,
    [PurposeDetails]          	nvarchar(max) NULL,
    [DcpZar]                  	float NULL,
    [RatingBand]              	nvarchar(100) NULL,
    [NumberOfPages]           	int NULL,
    [WithinPageGuidance]      	bit NULL,
    [EstimatedMinutes]        	int NULL,
    [PrimaryAnalyst]          	nvarchar(255) NULL,
    [PrimaryAnalystId]        	nvarchar(255) NULL,
    [ReviewAnalyst]           	nvarchar(255) NULL,
    [ReviewAnalystId]         	nvarchar(255) NULL,
    [CreditTeam]              	nvarchar(max) NULL,
    [BusinessTeam]            	nvarchar(max) NULL,
    [CreditSponsor]           	nvarchar(255) NULL,
    [CreditSponsorId]         	nvarchar(255) NULL,
    [BuHead]                  	nvarchar(255) NULL,
    [BuHeadId]                	nvarchar(255) NULL,
    [LateSubmission]          	bit NULL,
    [LateApprovedBy]          	nvarchar(255) NULL,
    [LateApprovedOn]          	datetime NULL,
    [OutOfGuidanceApprovedBy] 	nvarchar(255) NULL,
    [OutOfGuidanceApprovedOn] 	datetime NULL,
    [ItemStatus]              	nvarchar(50) NULL,
    [AgendaNumber]            	nvarchar(20) NULL,
    CONSTRAINT [PK_A_AGI] PRIMARY KEY CLUSTERED([AGI_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGID_AgendaItemDocument]  (
    [AGID_ID]             	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGI_ID]              	int NULL,
    [DocStashId]          	nvarchar(255) NULL,
    [FileName]            	nvarchar(255) NULL,
    [UploadedBy]          	nvarchar(255) NULL,
    [UploadedOn]          	datetime NULL,
    CONSTRAINT [PK_A_AGID] PRIMARY KEY CLUSTERED([AGID_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

CREATE TABLE [dbo].[A_AGPM_AgendaPostMeeting]  (
    [AGPM_ID]             	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGI_ID]              	int NULL,
    [ActualPages]         	int NULL,
    [ActualMinutes]       	int NULL,
    [Decision]            	nvarchar(100) NULL,
    [CreditConditions]    	nvarchar(max) NULL,
    [AttendeesPresent]    	nvarchar(max) NULL,
    [MinuteTaker]         	nvarchar(255) NULL,
    [Scribe]              	nvarchar(255) NULL,
    [Administrator]       	nvarchar(255) NULL,
    [AnalystSignOffBy]    	nvarchar(255) NULL,
    [AnalystSignOffOn]    	datetime NULL,
    [ChairSignOffBy]      	nvarchar(255) NULL,
    [ChairSignOffOn]      	datetime NULL,
    [DocStashOutputId]    	nvarchar(255) NULL,
    CONSTRAINT [PK_A_AGPM] PRIMARY KEY CLUSTERED([AGPM_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== Constraints =====

ALTER TABLE [dbo].[AGM_AgendaMeeting]
    ADD CONSTRAINT [FK_AGM_AGC]
   FOREIGN KEY([AGC_ID])
   REFERENCES [dbo].[AGC_AgendaCommittee]([AGC_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AGI_AgendaItem]
    ADD CONSTRAINT [FK_AGI_AGM]
   FOREIGN KEY([AGM_ID])
   REFERENCES [dbo].[AGM_AgendaMeeting]([AGM_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ADD CONSTRAINT [FK_AGID_AGI]
   FOREIGN KEY([AGI_ID])
   REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[AGPM_AgendaPostMeeting]
    ADD CONSTRAINT [FK_AGPM_AGI]
   FOREIGN KEY([AGI_ID])
   REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGC_AgendaCommittee]
    ADD CONSTRAINT [FK_A_AGC_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGC_AgendaCommittee]
    ADD CONSTRAINT [FK_A_AGC_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGM_AgendaMeeting]
    ADD CONSTRAINT [FK_A_AGM_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGM_AgendaMeeting]
    ADD CONSTRAINT [FK_A_AGM_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGI_AgendaItem]
    ADD CONSTRAINT [FK_A_AGI_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem]
    ADD CONSTRAINT [FK_A_AGI_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD CONSTRAINT [FK_A_AGID_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD CONSTRAINT [FK_A_AGID_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO

ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting]
    ADD CONSTRAINT [FK_A_AGPM_Revision]
   FOREIGN KEY([REV])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting]
    ADD CONSTRAINT [FK_A_AGPM_RevisionEnd]
   FOREIGN KEY([REVEND])
   REFERENCES [dbo].[RI_RevisionInfo]([Rev])
   ON DELETE NO ACTION
   ON UPDATE NO ACTION
GO
```

### V3__CASDM-1352_agenda_static_data.sql

Source: `api/src/main/resources/db/migration/V3__CASDM-1352_agenda_static_data.sql`. SHA-256: `67d035e8a6fc5a537a779c295b1d7d47e5cc4279ee29e88d956935e356b15862`.

```sql
-- CASDM-1352: Committee & level reference data (Agenda_Builder_Spec.md section 3).
-- Chair values are initials pending business confirmation of full names/ids.

INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'FSR Wholesale Credit Committee (FSRWCC)', 'L1', 'EQ+', 7, 'Thursday', 'PC')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'FSR Wholesale Credit Committee (FSRWCC)', 'L2', 'EQ', 6, 'Thursday', 'PC')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'International and Specialised Finance CC (I&SF)', 'L3', 'SQ+', 5, 'Tuesday', 'LvS')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'International and Specialised Finance CC (I&SF)', 'L4', 'SQ (CCIB)', 4, 'Thursday', 'PC')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Leveraged Finance CC (LF)', 'L3', 'SQ+', 5, 'Wednesday', 'PC')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Leveraged Finance CC (LF)', 'L4', 'SQ', 4, 'Wednesday', 'PC')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Family Office Group (FOGS)', 'L3', 'SQ+', 5, 'Wednesday', 'WR')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Family Office Group (FOGS)', 'L4', 'SQ', 4, 'Wednesday', 'WR')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Property Finance Credit Committee (PFCC)', 'L3', 'SQ+', 5, 'Wednesday', 'NN')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Property Finance Credit Committee (PFCC)', 'L4', 'SQ', 4, 'Wednesday', 'NN')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Corporates Credit Committee (CCC)', 'L3', 'SQ+', 5, 'Tuesday', 'WP')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'Corporates Credit Committee (CCC)', 'L4', 'SQ', 4, 'Tuesday', 'WP')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'RMB Institutions Credit Committee (ICC)', 'L3', 'SQ+', 5, 'Wednesday', 'WP')
GO
INSERT INTO [AGC_AgendaCommittee]([CreatedBy], [CommitteeName], [MandateLevel], [LevelDescription], [QuorumSize], [MeetingDay], [Chair])
  VALUES('SYSTEM', 'RMB Institutions Credit Committee (ICC)', 'L4', 'SQ', 4, 'Wednesday', 'WP')
GO
```

### V4__CASDM-1352_agenda_document_data.sql

Source: `api/src/main/resources/db/migration/V4__CASDM-1352_agenda_document_data.sql`. SHA-256: `0ab7476a6a9f6aed28f06fc2609ac6a5b8ec29345d4a4f68ef9db4220dac4c11`.

```sql
-- CASDM-1352: store the credit paper in AGID (docstash push deferred to agenda outputs).

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ALTER COLUMN [DocStashId] nvarchar(255) NULL
GO

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ADD [FileData] varbinary(max) NULL,
        [FileSize] bigint NULL,
        [FileExtension] nvarchar(20) NULL
GO

-- Audit table: file metadata is audited, file content is not (@NotAudited).
ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD [FileSize] bigint NULL,
        [FileExtension] nvarchar(20) NULL
GO
```

### V5__CASDM-1352_agenda_document_type.sql

Source: `api/src/main/resources/db/migration/V5__CASDM-1352_agenda_document_type.sql`. SHA-256: `32b780118354961d4a889d4ed049e80ba00dac7345377e1007fc14cf7d5fe1b3`.

```sql
-- CASDM-1352: document type per agenda document (Credit Paper, Annexure, ...).

ALTER TABLE [dbo].[AGID_AgendaItemDocument]
    ADD [DocType] nvarchar(100) NULL
GO

ALTER TABLE [dbo].[A_AGID_AgendaItemDocument]
    ADD [DocType] nvarchar(100) NULL
GO
```

### V6__CASDM-1352_agenda_item_full_fields.sql

Source: `api/src/main/resources/db/migration/V6__CASDM-1352_agenda_item_full_fields.sql`. SHA-256: `522f167fb4ac8680ab21e47cf2dcf6e288e98af32e610266894325d68ca3e0a9`.

```sql
-- CASDM-1352: extend agenda items to cover the full "Call for Agenda Items" sheet
-- (scheduling, per-item committee roster, risk classification, distribution tracking, notes).

ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [StartTime]                 nvarchar(10)  NULL,
    [EndTime]                   nvarchar(10)  NULL,
    [RoundRobin]                bit           NOT NULL DEFAULT ((0)),
    [LecRep]                    nvarchar(255) NULL,
    [EqMemberFrmExec]           nvarchar(255) NULL,
    [BusinessRep1]              nvarchar(255) NULL,
    [SeniorBusinessRep]         nvarchar(255) NULL,
    [PortfolioCce1]             nvarchar(255) NULL,
    [PortfolioCce2]             nvarchar(255) NULL,
    [IndependentMember]         nvarchar(255) NULL,
    [Scribe]                    nvarchar(255) NULL,
    [MezzOverlayAttendees]      nvarchar(max) NULL,
    [EsgOverlayAttendees]       nvarchar(max) NULL,
    [AdditionalTeamAttendees]   nvarchar(max) NULL,
    [CountryOfRisk]             nvarchar(255) NULL,
    [IndustryClassification]    nvarchar(255) NULL,
    [LecCounterparty]           bit           NOT NULL DEFAULT ((0)),
    [DistributionSensitivities] nvarchar(max) NULL,
    [ApplicantRelationship]     nvarchar(50)  NULL,
    [ApplicationType]           nvarchar(100) NULL,
    [ProposalReceived]          bit           NOT NULL DEFAULT ((0)),
    [ProposalDistributedEmail]  bit           NOT NULL DEFAULT ((0)),
    [ProposalOnDiligent]        bit           NOT NULL DEFAULT ((0)),
    [Comments]                  nvarchar(max) NULL,
    [LateSubmissionReason]      nvarchar(max) NULL
GO

-- Audit table mirrors the same columns (all nullable, no defaults - Envers writes explicit values).
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [StartTime]                 nvarchar(10)  NULL,
    [EndTime]                   nvarchar(10)  NULL,
    [RoundRobin]                bit           NULL,
    [LecRep]                    nvarchar(255) NULL,
    [EqMemberFrmExec]           nvarchar(255) NULL,
    [BusinessRep1]              nvarchar(255) NULL,
    [SeniorBusinessRep]         nvarchar(255) NULL,
    [PortfolioCce1]             nvarchar(255) NULL,
    [PortfolioCce2]             nvarchar(255) NULL,
    [IndependentMember]         nvarchar(255) NULL,
    [Scribe]                    nvarchar(255) NULL,
    [MezzOverlayAttendees]      nvarchar(max) NULL,
    [EsgOverlayAttendees]       nvarchar(max) NULL,
    [AdditionalTeamAttendees]   nvarchar(max) NULL,
    [CountryOfRisk]             nvarchar(255) NULL,
    [IndustryClassification]    nvarchar(255) NULL,
    [LecCounterparty]           bit           NULL,
    [DistributionSensitivities] nvarchar(max) NULL,
    [ApplicantRelationship]     nvarchar(50)  NULL,
    [ApplicationType]           nvarchar(100) NULL,
    [ProposalReceived]          bit           NULL,
    [ProposalDistributedEmail]  bit           NULL,
    [ProposalOnDiligent]        bit           NULL,
    [Comments]                  nvarchar(max) NULL,
    [LateSubmissionReason]      nvarchar(max) NULL
GO
```

### V7__CASDM-1352_post_meeting_actuals.sql

Source: `api/src/main/resources/db/migration/V7__CASDM-1352_post_meeting_actuals.sql`. SHA-256: `2ff4234f6158d149ead8cd3ed6445e8853c0015bf19ac4d03caf7b9e5afb87f4`.

```sql
-- CASDM-1352: post-meeting performance-monitoring actuals (Agenda_Builder_Spec.md section 9).
-- Captures the actual rating, purpose, DCP and committee date alongside the existing
-- actual pages / time / decision / conditions.

ALTER TABLE [dbo].[AGPM_AgendaPostMeeting] ADD
    [ActualRating]   nvarchar(100) NULL,
    [ActualPurpose]  nvarchar(255) NULL,
    [ActualDcp]      float         NULL,
    [DateOfCc]       datetime      NULL
GO

ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting] ADD
    [ActualRating]   nvarchar(100) NULL,
    [ActualPurpose]  nvarchar(255) NULL,
    [ActualDcp]      float         NULL,
    [DateOfCc]       datetime      NULL
GO
```

### V8__CASDM-1352_post_meeting_lock.sql

Source: `api/src/main/resources/db/migration/V8__CASDM-1352_post_meeting_lock.sql`. SHA-256: `00a4884d1621ad6890672d16ecc79f9bf2b8300d85e57652a6bd79c397720269`.

```sql
-- CASDM-1352: post-meeting lock. Once the chair signs off the post-meeting record it
-- is locked from further edits; only a reviewer may unlock it (Agenda_Builder_Spec.md
-- section 9). Post-meeting capture itself is gated to meetings that have been marked Held.

ALTER TABLE [dbo].[AGPM_AgendaPostMeeting] ADD
    [Locked]   bit           NOT NULL DEFAULT ((0)),
    [LockedBy] nvarchar(255) NULL,
    [LockedOn] datetime      NULL
GO

ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting] ADD
    [Locked]   bit           NULL,
    [LockedBy] nvarchar(255) NULL,
    [LockedOn] datetime      NULL
GO
```

### V9__CASDM-1352_committee_lms_list.sql

Source: `api/src/main/resources/db/migration/V9__CASDM-1352_committee_lms_list.sql`. SHA-256: `8b9c1de921cc9cf757a1a2d3a236c3086619082f2e1d8760cc921c158e8a59a0`.

```sql
-- CASDM-1352: link an agenda committee (per mandate level) to a List Management list,
-- so committee membership can be sourced from LMS instead of free-typed. Nullable so
-- existing committees keep their free-text members until a list is assigned.

ALTER TABLE [dbo].[AGC_AgendaCommittee] ADD
    [ListName] nvarchar(255) NULL
GO

ALTER TABLE [dbo].[A_AGC_AgendaCommittee] ADD
    [ListName] nvarchar(255) NULL
GO
```

### V10__CASDM-1352_item_chair_and_drop_seeded_people.sql

Source: `api/src/main/resources/db/migration/V10__CASDM-1352_item_chair_and_drop_seeded_people.sql`. SHA-256: `7009d86de526b898808aca7a5d6998b5a9a7402521775d540159528e11c95912`.

```sql
-- CASDM-1352: the chair is decided per agenda item (it varies within a meeting - see the
-- workbook "Agenda output for editing" examples), and people are never predefined on the
-- committee. Add a per-item Chair, and clear the seeded committee chair initials.

ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [Chair] nvarchar(255) NULL
GO

ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [Chair] nvarchar(255) NULL
GO

-- Committee people are resolved from the RMB name directory / LMS at item time, not seeded.
UPDATE [dbo].[AGC_AgendaCommittee]
   SET [Chair] = NULL, [ChairId] = NULL
GO
```

### V11__CASDM-1352_notifications_and_attendee_emails.sql

Source: `api/src/main/resources/db/migration/V11__CASDM-1352_notifications_and_attendee_emails.sql`. SHA-256: `6f0abf2e28cf6c1a487df950672b1e5e10159be0df32e19aa512dcecd8a7f354`.

```sql
-- CASDM-1352: notification audit log (send status visible in the UI) and a place to hold the
-- resolved attendee e-mails on an item so the chair/attendees can be invited when the agenda
-- is compiled/locked.

CREATE TABLE [dbo].[AGN_AgendaNotification] (
    [AGN_ID]           int IDENTITY(1,1) NOT NULL,
    [AGI_ID]           int            NULL,
    [AGM_ID]           int            NULL,
    [NotificationType] nvarchar(50)   NULL,
    [Recipients]       nvarchar(2000) NULL,
    [Subject]          nvarchar(500)  NULL,
    [Status]           nvarchar(50)   NULL,
    [ErrorMessage]     nvarchar(2000) NULL,
    [SentOn]           datetime       NULL,
    CONSTRAINT [PK_AGN] PRIMARY KEY CLUSTERED([AGN_ID])
        WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
GO

ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [AttendeeEmails] nvarchar(max) NULL
GO

ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [AttendeeEmails] nvarchar(max) NULL
GO
```

### V12__CASDM-1352_multi_person_roster_roles.sql

Source: `api/src/main/resources/db/migration/V12__CASDM-1352_multi_person_roster_roles.sql`. SHA-256: `4558b6983196a84590c77078b8cf7a9a4c51881b316b65b04f712193c51a3f9f`.

```sql
-- CASDM-1352: roster roles allow more than one person per role. The captured value is a
-- `;`-joined list of display names, so the 255-char role columns are widened.

ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [Chair]             nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [LecRep]            nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [EqMemberFrmExec]   nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [BusinessRep1]      nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [SeniorBusinessRep] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [PortfolioCce1]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [PortfolioCce2]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [IndependentMember] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [Scribe]            nvarchar(1000) NULL
GO

-- Audit table mirrors the same widths.
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [Chair]             nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [LecRep]            nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [EqMemberFrmExec]   nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [BusinessRep1]      nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [SeniorBusinessRep] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [PortfolioCce1]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [PortfolioCce2]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [IndependentMember] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [Scribe]            nvarchar(1000) NULL
GO
```

### V13__CASDM-1352_brs_alignment.sql

Source: `api/src/main/resources/db/migration/V13__CASDM-1352_brs_alignment.sql`. SHA-256: `c1e376cd18901486f410a6221d305f9f6057993f449e6fd6d279973be5029d08`.

```sql
-- CASDM-1352: align the Agenda Builder with the Business Requirements Specification (v0.1).
-- Sections follow the BRS end-to-end workflow. Every live table change is mirrored on the
-- A_ Envers audit table; new tables get their own A_ table (same pattern as V2).

-- ===== 1. Committee & calendar configuration (BR-001, BR-002, RULE-004, RULE-007) =====
ALTER TABLE [dbo].[AGC_AgendaCommittee] ADD
    [EffectiveFrom]           datetime      NULL,
    [EffectiveTo]             datetime      NULL,
    [DefaultHeldVia]          nvarchar(50)  NULL,
    [DefaultDurationMinutes]  int           NULL,
    [CutOffDayOfWeek]         nvarchar(20)  NULL,
    [CutOffTime]              nvarchar(5)   NULL,
    [RequiredDocTypes]        nvarchar(500) NULL,
    [QuorumRoles]             nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGC_AgendaCommittee] ADD
    [EffectiveFrom]           datetime      NULL,
    [EffectiveTo]             datetime      NULL,
    [DefaultHeldVia]          nvarchar(50)  NULL,
    [DefaultDurationMinutes]  int           NULL,
    [CutOffDayOfWeek]         nvarchar(20)  NULL,
    [CutOffTime]              nvarchar(5)   NULL,
    [RequiredDocTypes]        nvarchar(500) NULL,
    [QuorumRoles]             nvarchar(1000) NULL
GO
-- Existing committees keep today's behaviour: Thursday 17:00 cut-off, credit paper mandatory,
-- quorum counted over the standard/enhanced roster roles captured on an item.
UPDATE [dbo].[AGC_AgendaCommittee]
   SET [CutOffDayOfWeek] = 'Thursday',
       [CutOffTime] = '17:00',
       [RequiredDocTypes] = 'Credit Paper',
       [QuorumRoles] = 'Chair;IndependentMember;PortfolioCce1;PortfolioCce2;BusinessRep1;SeniorBusinessRep;LecRep;EqMemberFrmExec'
 WHERE [CutOffDayOfWeek] IS NULL
GO

ALTER TABLE [dbo].[AGM_AgendaMeeting] ADD
    [StartTime]        nvarchar(5)    NULL,
    [EndTime]          nvarchar(5)    NULL,
    [Venue]            nvarchar(255)  NULL,
    [TeamsJoinUrl]     nvarchar(1000) NULL,
    [GraphEventId]     nvarchar(255)  NULL,
    [ActualStartTime]  nvarchar(5)    NULL,
    [ActualEndTime]    nvarchar(5)    NULL,
    [QuorumAchieved]   bit            NULL,
    [CancelledBy]      nvarchar(255)  NULL,
    [CancelledOn]      datetime       NULL,
    [CancelReason]     nvarchar(1000) NULL,
    [ClosedBy]         nvarchar(255)  NULL,
    [ClosedOn]         datetime       NULL
GO
ALTER TABLE [dbo].[A_AGM_AgendaMeeting] ADD
    [StartTime]        nvarchar(5)    NULL,
    [EndTime]          nvarchar(5)    NULL,
    [Venue]            nvarchar(255)  NULL,
    [TeamsJoinUrl]     nvarchar(1000) NULL,
    [GraphEventId]     nvarchar(255)  NULL,
    [ActualStartTime]  nvarchar(5)    NULL,
    [ActualEndTime]    nvarchar(5)    NULL,
    [QuorumAchieved]   bit            NULL,
    [CancelledBy]      nvarchar(255)  NULL,
    [CancelledOn]      datetime       NULL,
    [CancelReason]     nvarchar(1000) NULL,
    [ClosedBy]         nvarchar(255)  NULL,
    [ClosedOn]         datetime       NULL
GO

-- ===== 2. Late submission & readiness (BR-006, BR-007, RULE-003) =====
ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [SubmittedBy]          nvarchar(255)  NULL,
    [SubmittedOn]          datetime       NULL,
    [QuorumOverrideReason] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [SubmittedBy]          nvarchar(255)  NULL,
    [SubmittedOn]          datetime       NULL,
    [QuorumOverrideReason] nvarchar(1000) NULL
GO

-- ===== 3. Documents (section 6.5) =====
ALTER TABLE [dbo].[AGID_AgendaItemDocument] ADD
    [Mandatory]         bit           NOT NULL DEFAULT ((0)),
    [ValidationStatus]  nvarchar(20)  NULL,
    [ValidatedBy]       nvarchar(255) NULL,
    [ValidatedOn]       datetime      NULL,
    [ValidationComment] nvarchar(1000) NULL,
    [Version]           int           NOT NULL DEFAULT ((1))
GO
ALTER TABLE [dbo].[A_AGID_AgendaItemDocument] ADD
    [Mandatory]         bit           NULL,
    [ValidationStatus]  nvarchar(20)  NULL,
    [ValidatedBy]       nvarchar(255) NULL,
    [ValidatedOn]       datetime      NULL,
    [ValidationComment] nvarchar(1000) NULL,
    [Version]           int           NULL
GO

-- ===== 4. Review & approval workflow (BR-010, section 6.6, RULE-009) =====
CREATE TABLE [dbo].[AGW_AgendaWorkflowAction]  (
    [AGW_ID]          	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]       	nvarchar(255) NOT NULL,
    [CreatedOn]       	datetime NOT NULL DEFAULT (getdate()),
    [Active]          	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]          	int NULL,
    [AGM_ID]          	int NULL,
    [Action]          	nvarchar(50) NOT NULL,
    [FromStatus]      	nvarchar(50) NULL,
    [ToStatus]        	nvarchar(50) NULL,
    [Comments]        	nvarchar(max) NULL,
    [OverrideFlag]    	bit NOT NULL DEFAULT ((0)),
    [OverrideReason]  	nvarchar(1000) NULL,
    CONSTRAINT [PK_AGW] PRIMARY KEY CLUSTERED([AGW_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGW_AgendaWorkflowAction]  (
    [AGW_ID]          	int NOT NULL,
    [REV]             	int NOT NULL,
    [REVTYPE]         	tinyint NULL,
    [REVEND]          	int NULL,
    [CreatedBy]       	nvarchar(255) NULL,
    [CreatedOn]       	datetime NULL,
    [Active]          	bit NULL,
    [AGI_ID]          	int NULL,
    [AGM_ID]          	int NULL,
    [Action]          	nvarchar(50) NULL,
    [FromStatus]      	nvarchar(50) NULL,
    [ToStatus]        	nvarchar(50) NULL,
    [Comments]        	nvarchar(max) NULL,
    [OverrideFlag]    	bit NULL,
    [OverrideReason]  	nvarchar(1000) NULL,
    CONSTRAINT [PK_A_AGW] PRIMARY KEY CLUSTERED([AGW_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== 5. Participants, attendance & quorum (BR-008, BR-009, RULE-005, RULE-006, section 6.4) =====
CREATE TABLE [dbo].[AGP_AgendaParticipant]  (
    [AGP_ID]              	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]           	nvarchar(255) NOT NULL,
    [CreatedOn]           	datetime NOT NULL DEFAULT (getdate()),
    [Active]              	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]              	int NOT NULL,
    [PersonId]            	nvarchar(255) NULL,
    [PersonName]          	nvarchar(255) NOT NULL,
    [Email]               	nvarchar(255) NULL,
    [CommitteeRole]       	nvarchar(100) NOT NULL,
    [AttendanceRequired]  	bit NOT NULL DEFAULT ((1)),
    [QuorumEligible]      	bit NOT NULL DEFAULT ((1)),
    [VotingEligible]      	bit NOT NULL DEFAULT ((1)),
    [IndependentFlag]     	bit NOT NULL DEFAULT ((0)),
    [PlannedAttendance]   	nvarchar(20) NULL,
    [InvitationStatus]    	nvarchar(20) NULL,
    [ActualAttendance]    	bit NULL,
    [OverrideReason]      	nvarchar(1000) NULL,
    CONSTRAINT [PK_AGP] PRIMARY KEY CLUSTERED([AGP_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGP_AgendaParticipant]  (
    [AGP_ID]              	int NOT NULL,
    [REV]                 	int NOT NULL,
    [REVTYPE]             	tinyint NULL,
    [REVEND]              	int NULL,
    [CreatedBy]           	nvarchar(255) NULL,
    [CreatedOn]           	datetime NULL,
    [Active]              	bit NULL,
    [AGI_ID]              	int NULL,
    [PersonId]            	nvarchar(255) NULL,
    [PersonName]          	nvarchar(255) NULL,
    [Email]               	nvarchar(255) NULL,
    [CommitteeRole]       	nvarchar(100) NULL,
    [AttendanceRequired]  	bit NULL,
    [QuorumEligible]      	bit NULL,
    [VotingEligible]      	bit NULL,
    [IndependentFlag]     	bit NULL,
    [PlannedAttendance]   	nvarchar(20) NULL,
    [InvitationStatus]    	nvarchar(20) NULL,
    [ActualAttendance]    	bit NULL,
    [OverrideReason]      	nvarchar(1000) NULL,
    CONSTRAINT [PK_A_AGP] PRIMARY KEY CLUSTERED([AGP_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== 6. Decisions & conditions (BR-017, BR-018, section 6.7 / 6.8) =====
ALTER TABLE [dbo].[AGPM_AgendaPostMeeting] ADD
    [DecisionType]  nvarchar(50) NULL,
    [DecisionDate]  datetime     NULL
GO
ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting] ADD
    [DecisionType]  nvarchar(50) NULL,
    [DecisionDate]  datetime     NULL
GO
CREATE TABLE [dbo].[AGCA_AgendaCondition]  (
    [AGCA_ID]         	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]       	nvarchar(255) NOT NULL,
    [CreatedOn]       	datetime NOT NULL DEFAULT (getdate()),
    [Active]          	bit NOT NULL DEFAULT ((1)),
    [AGI_ID]          	int NOT NULL,
    [ConditionType]   	nvarchar(50) NOT NULL,
    [Description]     	nvarchar(max) NOT NULL,
    [OwnerId]         	nvarchar(255) NULL,
    [OwnerName]       	nvarchar(255) NULL,
    [DueDate]         	datetime NULL,
    [Status]          	nvarchar(50) NOT NULL DEFAULT ('Open'),
    [EvidenceRef]     	nvarchar(1000) NULL,
    [ClosureComments] 	nvarchar(max) NULL,
    [ClosedBy]        	nvarchar(255) NULL,
    [ClosedOn]        	datetime NULL,
    CONSTRAINT [PK_AGCA] PRIMARY KEY CLUSTERED([AGCA_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGCA_AgendaCondition]  (
    [AGCA_ID]         	int NOT NULL,
    [REV]             	int NOT NULL,
    [REVTYPE]         	tinyint NULL,
    [REVEND]          	int NULL,
    [CreatedBy]       	nvarchar(255) NULL,
    [CreatedOn]       	datetime NULL,
    [Active]          	bit NULL,
    [AGI_ID]          	int NULL,
    [ConditionType]   	nvarchar(50) NULL,
    [Description]     	nvarchar(max) NULL,
    [OwnerId]         	nvarchar(255) NULL,
    [OwnerName]       	nvarchar(255) NULL,
    [DueDate]         	datetime NULL,
    [Status]          	nvarchar(50) NULL,
    [EvidenceRef]     	nvarchar(1000) NULL,
    [ClosureComments] 	nvarchar(max) NULL,
    [ClosedBy]        	nvarchar(255) NULL,
    [ClosedOn]        	datetime NULL,
    CONSTRAINT [PK_A_AGCA] PRIMARY KEY CLUSTERED([AGCA_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

-- ===== 7. Records & evidence (BR-020) =====
CREATE TABLE [dbo].[AGMR_AgendaMeetingRecord]  (
    [AGMR_ID]     	int IDENTITY(1,1) NOT NULL,
    [CreatedBy]   	nvarchar(255) NOT NULL,
    [CreatedOn]   	datetime NOT NULL DEFAULT (getdate()),
    [Active]      	bit NOT NULL DEFAULT ((1)),
    [AGM_ID]      	int NOT NULL,
    [RecordType]  	nvarchar(50) NOT NULL,
    [Name]        	nvarchar(255) NOT NULL,
    [Location]    	nvarchar(1000) NOT NULL,
    CONSTRAINT [PK_AGMR] PRIMARY KEY CLUSTERED([AGMR_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE TABLE [dbo].[A_AGMR_AgendaMeetingRecord]  (
    [AGMR_ID]     	int NOT NULL,
    [REV]         	int NOT NULL,
    [REVTYPE]     	tinyint NULL,
    [REVEND]      	int NULL,
    [CreatedBy]   	nvarchar(255) NULL,
    [CreatedOn]   	datetime NULL,
    [Active]      	bit NULL,
    [AGM_ID]      	int NULL,
    [RecordType]  	nvarchar(50) NULL,
    [Name]        	nvarchar(255) NULL,
    [Location]    	nvarchar(1000) NULL,
    CONSTRAINT [PK_A_AGMR] PRIMARY KEY CLUSTERED([AGMR_ID],[REV])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO

ALTER TABLE [dbo].[AGW_AgendaWorkflowAction] ADD CONSTRAINT [FK_AGW_AGI]
    FOREIGN KEY ([AGI_ID]) REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
GO
ALTER TABLE [dbo].[AGP_AgendaParticipant] ADD CONSTRAINT [FK_AGP_AGI]
    FOREIGN KEY ([AGI_ID]) REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
GO
ALTER TABLE [dbo].[AGCA_AgendaCondition] ADD CONSTRAINT [FK_AGCA_AGI]
    FOREIGN KEY ([AGI_ID]) REFERENCES [dbo].[AGI_AgendaItem]([AGI_ID])
GO
ALTER TABLE [dbo].[AGMR_AgendaMeetingRecord] ADD CONSTRAINT [FK_AGMR_AGM]
    FOREIGN KEY ([AGM_ID]) REFERENCES [dbo].[AGM_AgendaMeeting]([AGM_ID])
GO
```

### V14__agenda_invitation_state.sql

Source: `api/src/main/resources/db/migration/V14__agenda_invitation_state.sql`. SHA-256: `e52738a19e51c6e385b9b2c4eceb0eed0c2108e7634ba84a10f9c28b0c455aa1`.

```sql
-- Keep invitation delivery separate from the agenda editing lifecycle.
ALTER TABLE [dbo].[AGM_AgendaMeeting] ADD
    [InvitationSnapshot] nvarchar(64) NULL,
    [InvitationSentOn] datetime NULL,
    [InvitationSentBy] nvarchar(255) NULL,
    [InvitationReopened] bit NOT NULL CONSTRAINT [DF_AGM_InvitationReopened] DEFAULT 0
GO
ALTER TABLE [dbo].[A_AGM_AgendaMeeting] ADD
    [InvitationSnapshot] nvarchar(64) NULL,
    [InvitationSentOn] datetime NULL,
    [InvitationSentBy] nvarchar(255) NULL,
    [InvitationReopened] bit NULL
GO
```

### V15__CASDM-1352_agenda_booking_drafts.sql

Source: `api/src/main/resources/db/migration/V15__CASDM-1352_agenda_booking_drafts.sql`. SHA-256: `049d7c696d77a4f9220b46cd4961b4ac7c2eda1b38f0cedf1ccbb345a5caa7f2`.

```sql
-- Agenda Builder: bookings saved part-way through the booking wizard, to continue later from any PC.
-- A draft reserves no slot and is not an agenda item; Payload is the wizard's state as JSON.
CREATE TABLE [dbo].[AGBD_AgendaBookingDraft]  (
    [AGBD_ID]          	int IDENTITY(1,1) NOT NULL,
    [OwnerId]          	nvarchar(255) NOT NULL,
    [AGC_ID]           	int NULL,
    [MeetingDate]      	datetime NULL,
    [CounterpartyName] 	nvarchar(500) NULL,
    [Step]             	int NOT NULL DEFAULT ((0)),
    [Payload]          	nvarchar(max) NOT NULL,
    [CreatedBy]        	nvarchar(255) NOT NULL,
    [CreatedOn]        	datetime NOT NULL DEFAULT (getdate()),
    [UpdatedOn]        	datetime NULL,
    [Active]           	bit NOT NULL DEFAULT ((1)),
    CONSTRAINT [PK_AGBD] PRIMARY KEY CLUSTERED([AGBD_ID])
	WITH FILLFACTOR = 80 ON [PRIMARY])
ON [PRIMARY]
	TEXTIMAGE_ON [PRIMARY]
	WITH (
		DATA_COMPRESSION = NONE
	)
GO
CREATE NONCLUSTERED INDEX [IX_AGBD_OwnerId] ON [dbo].[AGBD_AgendaBookingDraft]([OwnerId], [Active])
GO
```

### V16__agenda_quorum_exception.sql

Source: `api/src/main/resources/db/migration/V16__agenda_quorum_exception.sql`. SHA-256: `c17a3212ccf9915bc9722370fe38215db78b0743e14d62b19f3cad54dec725f0`.

```sql
-- A quorum exception is asked for by the analyst and decided by the committee's secretary or chair,
-- separately from submission. Status: Requested, Approved or Declined.
ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [QuorumExceptionStatus]       nvarchar(20)   NULL,
    [QuorumExceptionReason]       nvarchar(1000) NULL,
    [QuorumExceptionRequestedBy]  nvarchar(255)  NULL,
    [QuorumExceptionRequestedOn]  datetime       NULL,
    [QuorumExceptionDecidedBy]    nvarchar(255)  NULL,
    [QuorumExceptionDecidedOn]    datetime       NULL,
    [QuorumExceptionDecisionNote] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [QuorumExceptionStatus]       nvarchar(20)   NULL,
    [QuorumExceptionReason]       nvarchar(1000) NULL,
    [QuorumExceptionRequestedBy]  nvarchar(255)  NULL,
    [QuorumExceptionRequestedOn]  datetime       NULL,
    [QuorumExceptionDecidedBy]    nvarchar(255)  NULL,
    [QuorumExceptionDecidedOn]    datetime       NULL,
    [QuorumExceptionDecisionNote] nvarchar(1000) NULL
GO
```

### V17__agenda_item_invitations.sql

Source: `api/src/main/resources/db/migration/V17__agenda_item_invitations.sql`. SHA-256: `d163b194e22fa430e0260b490109b6bb05028db8ec059c77c320c2f31dcb212e`.

```sql
-- One calendar invitation per approved agenda item (BR-013): only the item's own participants are
-- invited, for the item's slot, with the item's paper. The meeting keeps its own event for the
-- committee's standing roles (chair, alternate chair, secretary, minute taker, administrator).
ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [GraphEventId]       nvarchar(255) NULL,
    [TeamsJoinUrl]       nvarchar(1000) NULL,
    [InvitationSnapshot] nvarchar(64)  NULL,
    [InvitationSentOn]   datetime      NULL,
    [InvitationSentBy]   nvarchar(255) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [GraphEventId]       nvarchar(255) NULL,
    [TeamsJoinUrl]       nvarchar(1000) NULL,
    [InvitationSnapshot] nvarchar(64)  NULL,
    [InvitationSentOn]   datetime      NULL,
    [InvitationSentBy]   nvarchar(255) NULL
GO
```
