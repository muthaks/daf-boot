/**
 * Shapes returned by DAF's agenda API (`approvalforum-service` model.agenda DTOs, served by
 * `ApprovalForumAgendaResource` under /approvalForum/agenda). DAF serialises `Timestamp` fields as
 * epoch milliseconds; request bodies may send ISO strings.
 */

/** A DAF timestamp: epoch milliseconds in responses, ISO string when the UI builds one. */
export type DafTimestamp = string | number;

export type ItemStatus = "Captured" | "Confirmed" | "Removed" | string;
export type MeetingStatus =
  | "Draft"
  | "Preliminary"
  | "Locked"
  | "Final"
  | "Completed"
  | "Held"
  | string;

export interface AgendaItemDocument {
  agidId: number;
  agiId: number;
  docStashId?: string;
  docType: string;
  fileName: string;
  uploadedBy: string;
  uploadedOn: string;
  fileSize?: number;
  fileExtension?: string;
  /** Mandatory paper for the committee (committee `requiredDocTypes`). */
  mandatory?: boolean;
  /** Pending | Accepted | Rejected | Waived. */
  validationStatus?: string;
  validatedBy?: string;
  validatedOn?: DafTimestamp;
  validationComment?: string;
  version?: number;
}

export interface AgendaPostMeeting {
  agpmId?: number;
  agiId: number;
  actualPages?: number | null;
  actualMinutes?: number | null;
  decision?: string;
  creditConditions?: string;
  attendeesPresent?: string;
  minuteTaker?: string;
  scribe?: string;
  administrator?: string;
  analystSignOffBy?: string;
  analystSignOffOn?: string;
  chairSignOffBy?: string;
  chairSignOffOn?: string;
  docStashOutputId?: string;
  actualRating?: string;
  actualPurpose?: string;
  actualDcp?: number | null;
  dateOfCc?: string;
  decisionType?: string;
  decisionDate?: DafTimestamp;
  locked?: boolean;
  lockedBy?: string;
  lockedOn?: string;
}

export interface AgendaItem {
  agiId: number;
  agmId: number;
  committeeName: string;
  mandateLevel: string;
  meetingDate: string;
  meetingStatus?: MeetingStatus;
  createdBy?: string;
  createdOn?: string;
  inkombaId?: string;
  counterpartyName: string;
  businessUnit?: string;
  purposeOfSubmission: string;
  purposeDetails?: string;
  dcpZar?: number | null;
  ratingBand?: string;
  numberOfPages?: number | null;
  withinPageGuidance?: boolean;
  estimatedMinutes?: number | null;
  primaryAnalyst?: string;
  primaryAnalystId?: string;
  reviewAnalyst?: string;
  reviewAnalystId?: string;
  creditTeam?: string;
  businessTeam?: string;
  creditSponsor?: string;
  creditSponsorId?: string;
  buHead?: string;
  buHeadId?: string;
  lateSubmission?: boolean;
  lateApprovedBy?: string;
  lateApprovedOn?: string;
  outOfGuidanceApprovedBy?: string;
  outOfGuidanceApprovedOn?: string;
  itemStatus: ItemStatus;
  agendaNumber?: string;
  startTime?: string;
  endTime?: string;
  roundRobin?: boolean;
  chair?: string;
  attendeeEmails?: string;
  lecRep?: string;
  eqMemberFrmExec?: string;
  businessRep1?: string;
  seniorBusinessRep?: string;
  portfolioCce1?: string;
  portfolioCce2?: string;
  independentMember?: string;
  scribe?: string;
  mezzOverlayAttendees?: string;
  esgOverlayAttendees?: string;
  additionalTeamAttendees?: string;
  countryOfRisk?: string;
  industryClassification?: string;
  lecCounterparty?: boolean;
  distributionSensitivities?: string;
  applicantRelationship?: string;
  applicationType?: string;
  proposalReceived?: boolean;
  proposalDistributedEmail?: boolean;
  proposalOnDiligent?: boolean;
  comments?: string;
  lateSubmissionReason?: string;
  documents?: AgendaItemDocument[];
  postMeeting?: AgendaPostMeeting | null;
  submittedBy?: string;
  submittedOn?: DafTimestamp;
  quorumOverrideReason?: string;
  cutOffOn?: DafTimestamp;
  participants?: AgendaParticipant[];
  actions?: AgendaWorkflowAction[];
  conditions?: AgendaCondition[];
  quorum?: AgendaQuorumResult | null;
}

export interface AgendaMeeting {
  agmId: number;
  agcId: number;
  committeeName: string;
  mandateLevel: string;
  quorumSize: number;
  chair?: string;
  meetingDate: string;
  heldVia?: string;
  status: MeetingStatus;
  cutOffOn?: string;
  lockedBy?: string;
  lockedOn?: string;
  items: AgendaItem[];
  startTime?: string;
  endTime?: string;
  venue?: string;
  teamsJoinUrl?: string;
  graphEventId?: string;
  actualStartTime?: string;
  actualEndTime?: string;
  quorumAchieved?: boolean | null;
  cancelledBy?: string;
  cancelledOn?: DafTimestamp;
  cancelReason?: string;
  closedBy?: string;
  closedOn?: DafTimestamp;
  records?: AgendaMeetingRecord[];
  actions?: AgendaWorkflowAction[];
}

export interface AgendaCommittee {
  agcId: number;
  committeeName: string;
  mandateLevel: string;
  levelDescription?: string;
  quorumSize: number;
  meetingDay?: string;
  chair?: string;
  chairId?: string;
  altChair?: string;
  altChairId?: string;
  meetingSecretary?: string;
  meetingSecretaryId?: string;
  minuteTaker?: string;
  minuteTakerId?: string;
  administrator?: string;
  administratorId?: string;
  approverComposition?: string;
  pageGuidance?: number | null;
  listName?: string;
  active?: boolean;
  effectiveFrom?: DafTimestamp | null;
  effectiveTo?: DafTimestamp | null;
  defaultHeldVia?: string;
  defaultDurationMinutes?: number | null;
  /** e.g. MONDAY. */
  cutOffDayOfWeek?: string;
  /** HH:mm. */
  cutOffTime?: string;
  /** Semicolon-separated document types every item must carry. */
  requiredDocTypes?: string;
  /** Semicolon-separated committee roles that count towards quorum. */
  quorumRoles?: string;
}

export interface AgendaNotification {
  agnId: number;
  agiId?: number;
  agmId?: number;
  notificationType: string;
  recipients?: string;
  subject?: string;
  status: string;
  errorMessage?: string;
  sentOn?: string;
}

export interface BookAgendaItemRequest {
  agcId: number;
  meetingDate: string;
  heldVia?: string;
  item: Partial<AgendaItem>;
}

/** DAF's live check of a CASDM application by Inkomba id. */
export interface AgendaCasdmApplication {
  configured: boolean;
  found: boolean;
  applicationId?: number;
  status?: string;
  inSubReview?: boolean;
  inSubApproval?: boolean;
  approved?: boolean;
  creditAnalyst?: string;
  creditAnalystEmail?: string;
  reviewAnalyst?: string;
  reviewAnalystEmail?: string;
  proposedRating?: string;
  countryOfRisk?: string;
  purposeOfSubmission?: string;
  meetingName?: string;
}

/** A DAF / List Management / AD user (DAF `ListManagementUser` / `ActiveDirectoryUserModel`). */
export interface AgendaUser {
  userPrincipalName?: string;
  displayName?: string;
  mail?: string;
  emailAddress?: string;
  samAccountName?: string;
  [key: string]: unknown;
}

/** `/approvalForum/agenda/users`: DAF's List Management user directory. */
export interface AgendaUserList {
  users: AgendaUser[];
}

/** `/approvalForum/agenda/committees/{agcId}/members`: DAF `ListManagementList`. */
export interface ListManagementList {
  listManagementName?: string;
  users: AgendaUser[];
}

/** A counterparty row from the creditdata feed (DAF `CounterpartyDetail`). */
export interface AgendaCounterparty {
  creditInkombaId?: string;
  inkombaId?: string;
  counterpartyName?: string;
  [key: string]: unknown;
}

/** `/approvalForum/agenda/counterparties` and `/approvalForum/external/casdm/counterparties`. */
export interface CounterpartyDetailList {
  counterpartyDetail: AgendaCounterparty[];
}

/** Available meeting date for a committee. */
export interface AvailableDate {
  meetingDate: string;
  agmId?: number | null;
  status?: MeetingStatus;
  cutOffOn?: string;
  [key: string]: unknown;
}

/** Stored as-is by DAF; these are the spellings DAF's own booking page writes. */
export const HELD_VIA_OPTIONS = ["MS Teams", "In-person", "Hybrid"] as const;
export const DECISION_OPTIONS = [
  "Approved",
  "Conditionally approved",
  "Declined",
  "Recommended",
  "Deferred",
] as const;
export const DOCUMENT_TYPES = ["Credit paper", "Annexure", "Other"] as const;

/** One person on an item's roster (DAF `AgendaParticipantDTO`, BRS 6.4). */
export interface AgendaParticipant {
  agpId?: number;
  agiId?: number;
  personId?: string | null;
  personName: string;
  email?: string | null;
  committeeRole: string;
  attendanceRequired?: boolean;
  quorumEligible?: boolean;
  votingEligible?: boolean;
  independentFlag?: boolean;
  /** Planned | Apology | ... */
  plannedAttendance?: string;
  invitationStatus?: string;
  actualAttendance?: boolean | null;
  overrideReason?: string;
}

/** Quorum evaluation for an item by committee role (DAF `AgendaQuorumResultDTO`). */
export interface AgendaQuorumResult {
  agiId: number;
  met: boolean;
  required: number;
  eligibleCount: number;
  countedRoles?: string[];
  missingRoles?: string[];
  overrideReason?: string;
}

/** A credit condition or action raised on an item (DAF `AgendaConditionDTO`). */
export interface AgendaCondition {
  agcaId?: number;
  agiId?: number;
  conditionType: string;
  description: string;
  ownerId?: string;
  ownerName?: string;
  dueDate?: DafTimestamp | null;
  status?: string;
  evidenceRef?: string;
  closureComments?: string;
  closedBy?: string;
  closedOn?: DafTimestamp;
  createdBy?: string;
  createdOn?: DafTimestamp;
}

/** Audited workflow step on an item or meeting (DAF `AgendaWorkflowActionDTO`). */
export interface AgendaWorkflowAction {
  agwId: number;
  agiId?: number | null;
  agmId?: number | null;
  action: string;
  fromStatus?: string;
  toStatus?: string;
  comments?: string;
  overrideFlag?: boolean;
  overrideReason?: string;
  actionBy?: string;
  actionOn?: DafTimestamp;
}

/** A record attached to a meeting, e.g. minutes or recording (DAF `AgendaMeetingRecordDTO`). */
export interface AgendaMeetingRecord {
  agmrId?: number;
  agmId?: number;
  recordType: string;
  name: string;
  location?: string;
  createdBy?: string;
  createdOn?: DafTimestamp;
}

/** Report summary for a date range (DAF `AgendaReportSummaryDTO`). */
export interface AgendaReportSummary {
  from: DafTimestamp;
  to: DafTimestamp;
  meetings: number;
  items: number;
  itemsByStatus: Record<string, number>;
  itemsByCommittee: Record<string, number>;
  itemsByBusinessUnit: Record<string, number>;
  itemsByPurpose: Record<string, number>;
  decisions: Record<string, number>;
  meetingsByStatus: Record<string, number>;
  lateItems: number;
  outstandingMandatoryPapers: number;
  plannedMinutes: number;
  actualMinutes: number;
  invited: number;
  plannedAttending: number;
  actuallyAttended: number;
  quorumMet: number;
  quorumNotMet: number;
  openConditions: number;
  overdueConditions: number;
  lateItemDetails: AgendaItem[];
}
