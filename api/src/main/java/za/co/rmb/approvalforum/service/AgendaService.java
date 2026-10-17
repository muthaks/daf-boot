package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.agenda.*;

import java.sql.Timestamp;
import java.util.List;

public interface AgendaService {

    List<AgendaCommitteeDTO> getCommittees();

    AgendaCommitteeDTO getCommittee(int agcId);

    AgendaCommitteeDTO updateCommittee(AgendaCommitteeDTO committee, String user);

    ListManagementList getCommitteeMembers(int agcId);

    List<Timestamp> getAvailableMeetingDates(int agcId);

    List<AgendaMeetingDTO> getMeetingsForCommittee(int agcId);

    AgendaMeetingDTO getMeeting(int agmId);

    AgendaItemDTO bookItem(BookAgendaItemRequest request, String user);

    AgendaItemDTO updateItem(AgendaItemDTO item, String user);

    List<AgendaItemDTO> getAllItems();

    List<AgendaItemDTO> getItemsForUser(String userId);

    /** Items this user booked (CreatedBy): what an analyst's agenda list is limited to. */
    List<AgendaItemDTO> getItemsCreatedBy(String userId);

    /**
     * The agenda items {@code userId} is allowed to open, given the DAF roles they hold (BR-024).
     * Ownership always wins; the oversight roles are scoped - see {@link AgendaAccess}.
     */
    List<AgendaItemDTO> getVisibleItems(String userId, AgendaAccess access);

    /** Whether {@code userId} may open this one item, evaluated with the same rules as the list. */
    boolean canViewItem(int agiId, String userId, AgendaAccess access);

    /** Whether {@code userId} may change this item: the booking analyst or the primary analyst. */
    boolean canEditItem(int agiId, String userId);

    /**
     * Whether {@code userId} may capture meeting outcomes on this item - the owner, or the
     * secretary / chair of the committee the item sits on.
     */
    boolean canCaptureOutcome(int agiId, String userId, AgendaAccess access);

    /**
     * Whether {@code userId} may change the participant roster: the owner while the meeting has
     * not been held (the planned roster their submission is checked against), and the committee's
     * secretary or chair at any time.
     */
    boolean canEditParticipants(int agiId, String userId, AgendaAccess access);

    AgendaItemDTO getLastRoster(int agcId);

    AgendaItemDTO getItem(int agiId);

    AgendaItemDTO attachDocument(int agiId, String docStashId, String fileName, String user);

    AgendaItemDTO uploadDocument(int agiId, String docType, String fileName, String fileExtension, byte[] fileData, String user);

    AgendaItemDocumentDTO getDocument(int agidId);

    byte[] getDocumentData(int agidId);

    AgendaItemDTO removeItem(int agiId, String user, String reason);

    AgendaMeetingDTO compileAgenda(int agmId, String user);

    AgendaMeetingDTO rescheduleAgenda(int agmId, String user);

    AgendaMeetingDTO lockMeeting(int agmId, String user);

    AgendaMeetingDTO sendInvitation(int agmId, String user);

    AgendaMeetingDTO sendInvitationUpdate(int agmId, String user);

    AgendaMeetingDTO setMeetingStatus(int agmId, String status, String user);

    AgendaMeetingDTO markMeetingHeld(int agmId, String user);

    AgendaPostMeetingDTO unlockPostMeeting(int agiId, String user);

    int processCutOffs();

    int processReminders();

    List<AgendaNotificationDTO> getNotificationsForItem(int agiId);

    AgendaPostMeetingDTO getPostMeeting(int agiId);

    AgendaPostMeetingDTO savePostMeeting(AgendaPostMeetingDTO postMeeting, String user);

    // ---- BRS alignment: committee & calendar administration ----

    List<AgendaCommitteeDTO> getAllCommittees();

    AgendaCommitteeDTO createCommittee(AgendaCommitteeDTO committee, String user);

    AgendaCommitteeDTO deactivateCommittee(int agcId, String user);

    AgendaMeetingDTO createMeeting(AgendaMeetingDTO meeting, String user);

    AgendaMeetingDTO updateMeeting(AgendaMeetingDTO meeting, String user);

    AgendaMeetingDTO cancelMeeting(int agmId, String reason, String user);

    /** Restore a cancelled meeting for editing; a new invitation is sent explicitly. */
    AgendaMeetingDTO reinstateMeeting(int agmId, String reason, String user);

    List<AgendaMeetingDTO> getMeetingsBetween(Timestamp from, Timestamp to);

    // ---- review & approval workflow ----

    AgendaItemDTO submitItem(int agiId, String quorumOverrideReason, String user);

    AgendaItemDTO returnItem(int agiId, String reason, String user);

    AgendaItemDTO completeReview(int agiId, String comments, String user);

    boolean canReassignReviewer(int agiId, String user, AgendaAccess access);

    AgendaItemDTO reassignReviewer(int agiId, ReassignAgendaReviewerRequest request, String user, AgendaAccess access);

    AgendaItemDTO approveItem(int agiId, String comments, String user);

    AgendaItemDTO rejectItem(int agiId, String reason, String user);

    AgendaItemDTO reopenItem(int agiId, String reason, String user);

    List<AgendaWorkflowActionDTO> getWorkflowActions(int agiId);

    // ---- documents ----

    AgendaItemDocumentDTO validateDocument(int agidId, String status, String comment, String user);

    // ---- participants & quorum ----

    List<AgendaParticipantDTO> getParticipants(int agiId);

    List<AgendaParticipantDTO> saveParticipants(int agiId, List<AgendaParticipantDTO> participants, String user);

    AgendaQuorumResultDTO evaluateQuorum(int agiId);

    // ---- meeting execution, records, close ----

    AgendaMeetingDTO markMeetingHeld(int agmId, String actualStartTime, String actualEndTime, Boolean quorumAchieved, String user);

    AgendaMeetingDTO closeMeeting(int agmId, String user);

    AgendaMeetingDTO reopenMeeting(int agmId, String reason, String user);

    List<AgendaMeetingRecordDTO> getMeetingRecords(int agmId);

    AgendaMeetingRecordDTO addMeetingRecord(AgendaMeetingRecordDTO record, String user);

    // ---- conditions & actions ----

    List<AgendaConditionDTO> getConditions(int agiId);

    List<AgendaConditionDTO> saveConditions(int agiId, List<AgendaConditionDTO> conditions, String user);

    AgendaConditionDTO closeCondition(int agcaId, String status, String closureComments, String user);

    // ---- reporting ----

    AgendaReportSummaryDTO getReportSummary(Timestamp from, Timestamp to);

}
