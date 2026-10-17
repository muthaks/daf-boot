package za.co.rmb.approvalforum.data;

import za.co.rmb.approvalforum.data.entity.ApprovalForumAgcAgendaCommitteeEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgiAgendaItemEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgidAgendaItemDocumentEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgmAgendaMeetingEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgnAgendaNotificationEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgpmAgendaPostMeetingEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgwAgendaWorkflowActionEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgpAgendaParticipantEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgcaAgendaConditionEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgmrAgendaMeetingRecordEntity;

import java.sql.Timestamp;
import java.util.List;

public interface ApprovalForumAgendaRepository {

    List<ApprovalForumAgcAgendaCommitteeEntity> getCommittees();

    ApprovalForumAgcAgendaCommitteeEntity getCommittee(int agcId);

    /** Serializes interval checks and writes, including creation of a committee's first meeting. */
    void lockCommitteeBookings(int agcId);

    ApprovalForumAgcAgendaCommitteeEntity saveCommittee(ApprovalForumAgcAgendaCommitteeEntity committee);

    List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsForCommittee(int agcId);

    ApprovalForumAgmAgendaMeetingEntity findMeeting(int agcId, Timestamp meetingDate);

    List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsPastCutOff(Timestamp now);

    List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsWithCutOffBetween(Timestamp from, Timestamp to);

    ApprovalForumAgmAgendaMeetingEntity getMeeting(int agmId);

    /** Serialize sending, cancellation and reinstatement of a calendar invitation. */
    ApprovalForumAgmAgendaMeetingEntity getMeetingForUpdate(int agmId);

    ApprovalForumAgmAgendaMeetingEntity saveMeeting(ApprovalForumAgmAgendaMeetingEntity meeting);

    List<ApprovalForumAgiAgendaItemEntity> getAllItems();

    List<ApprovalForumAgiAgendaItemEntity> getItemsForMeeting(int agmId);

    List<ApprovalForumAgiAgendaItemEntity> getItemsForUser(String userId);

    /** Items this user booked (CreatedBy): the owner rule an analyst's agenda list follows. */
    List<ApprovalForumAgiAgendaItemEntity> getItemsCreatedBy(String userId);

    ApprovalForumAgiAgendaItemEntity getLatestItemForCommittee(int agcId);

    ApprovalForumAgiAgendaItemEntity getItem(int agiId);

    ApprovalForumAgiAgendaItemEntity getItemForUpdate(int agiId);

    ApprovalForumAgiAgendaItemEntity saveItem(ApprovalForumAgiAgendaItemEntity item);

    List<ApprovalForumAgidAgendaItemDocumentEntity> getDocumentsForItem(int agiId);

    ApprovalForumAgidAgendaItemDocumentEntity getDocument(int agidId);

    /** Serializes final decisions on the same document version. Call inside a transaction. */
    ApprovalForumAgidAgendaItemDocumentEntity getDocumentForValidation(int agidId);

    ApprovalForumAgidAgendaItemDocumentEntity saveDocument(ApprovalForumAgidAgendaItemDocumentEntity document);

    ApprovalForumAgpmAgendaPostMeetingEntity getPostMeetingForItem(int agiId);

    ApprovalForumAgpmAgendaPostMeetingEntity savePostMeeting(ApprovalForumAgpmAgendaPostMeetingEntity postMeeting);

    ApprovalForumAgnAgendaNotificationEntity saveNotification(ApprovalForumAgnAgendaNotificationEntity notification);

    List<ApprovalForumAgnAgendaNotificationEntity> getNotificationsForItem(int agiId);

    // ---- BRS alignment (V13) ----

    List<ApprovalForumAgcAgendaCommitteeEntity> getAllCommittees();

    List<ApprovalForumAgmAgendaMeetingEntity> getMeetingsBetween(Timestamp from, Timestamp to);

    List<ApprovalForumAgiAgendaItemEntity> getItemsBetween(Timestamp from, Timestamp to);

    ApprovalForumAgwAgendaWorkflowActionEntity saveWorkflowAction(ApprovalForumAgwAgendaWorkflowActionEntity action);

    List<ApprovalForumAgwAgendaWorkflowActionEntity> getWorkflowActionsForItem(int agiId);

    List<ApprovalForumAgwAgendaWorkflowActionEntity> getWorkflowActionsForMeeting(int agmId);

    List<ApprovalForumAgpAgendaParticipantEntity> getParticipantsForItem(int agiId);

    ApprovalForumAgpAgendaParticipantEntity saveParticipant(ApprovalForumAgpAgendaParticipantEntity participant);

    List<ApprovalForumAgcaAgendaConditionEntity> getConditionsForItem(int agiId);

    ApprovalForumAgcaAgendaConditionEntity getCondition(int agcaId);

    ApprovalForumAgcaAgendaConditionEntity saveCondition(ApprovalForumAgcaAgendaConditionEntity condition);

    List<ApprovalForumAgmrAgendaMeetingRecordEntity> getRecordsForMeeting(int agmId);

    ApprovalForumAgmrAgendaMeetingRecordEntity saveRecord(ApprovalForumAgmrAgendaMeetingRecordEntity record);

}
