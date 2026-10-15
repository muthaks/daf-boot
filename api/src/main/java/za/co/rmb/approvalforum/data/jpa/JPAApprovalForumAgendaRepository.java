package za.co.rmb.approvalforum.data.jpa;

import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
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

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.sql.Timestamp;
import java.util.List;

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
