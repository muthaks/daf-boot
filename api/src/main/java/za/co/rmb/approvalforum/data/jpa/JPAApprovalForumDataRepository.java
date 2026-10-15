package za.co.rmb.approvalforum.data.jpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumDataRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.WorkflowFilterTypes;
import za.co.rmb.approvalforum.model.WorkflowListSearchTypes;
import za.co.rmb.approvalforum.model.WorkflowSortByTypes;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;

public class JPAApprovalForumDataRepository implements ApprovalForumDataRepository {

    private final EntityManager em;

    private static final Logger logger = LoggerFactory.getLogger(JPAApprovalForumDataRepository.class);

    @Inject
    public JPAApprovalForumDataRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public ApprovalForumAflmmApprovalForumListManagementMappingEntity getListManagementMappingByName(String name) {
        try {
            return em.createQuery("from ApprovalForumAflmmApprovalForumListManagementMappingEntity t " +
                                    "where t.listName = :name ",
                            ApprovalForumAflmmApprovalForumListManagementMappingEntity.class).setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void persistWorkflow(ApprovalForumWfWorkflowEntity wfWorkflowEntity) {
        logger.info("persistWorkflow:getWorkflowSystemName::" + wfWorkflowEntity.getWorkflowSystemName());
        em.persist(wfWorkflowEntity);
    }

    @Override
    public void updateWorkflow(ApprovalForumWfWorkflowEntity wfWorkflowEntity) {
        logger.info("updateWorkflow:workflowId::" + wfWorkflowEntity.getWfId());
        em.merge(wfWorkflowEntity);
    }

    @Override
    public void persistWorkflowCounterpartyDetail(ApprovalForumWfcdWorkflowCounterpartyDetailEntity wfcdWorkflowCounterpartyDetailEntity) {
        em.persist(wfcdWorkflowCounterpartyDetailEntity);
    }

    @Override
    public void persistWorkflowData(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataEntity) {
        em.persist(wfdaWorkflowDataEntity);
    }

    @Override
    public void updateWorkflowData(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataEntity) {
        em.merge(wfdaWorkflowDataEntity);
    }

    @Override
    public void deleteWorkflowData(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataEntity) {
        em.remove(wfdaWorkflowDataEntity);
    }

    @Override
    public void persistWorkflowApplicationData(ApprovalForumWfadWorkflowApplicationDataEntity wfadWorkflowApplicationDataEntity) {
        em.persist(wfadWorkflowApplicationDataEntity);
    }

    @Override
    public void persistWorkflowFacilityInformation(ApprovalForumWffiWorkflowFacilityInformationEntity wffiWorkflowFacilityInformationEntity) {
        em.persist(wffiWorkflowFacilityInformationEntity);
    }

    @Override
    public ApprovalForumWfWorkflowEntity getWorkflow(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfWorkflowEntity t " +
                                    "where t.wfId = :workflowId",
                            ApprovalForumWfWorkflowEntity.class).setParameter("workflowId", workflowId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<ApprovalForumAflmmApprovalForumListManagementMappingEntity> getAllListManagementMappings() {
        try {
            return em.createQuery("from ApprovalForumAflmmApprovalForumListManagementMappingEntity t",
                            ApprovalForumAflmmApprovalForumListManagementMappingEntity.class)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void persistWorkflowApprovals(ApprovalForumWfaWorkflowApprovalsEntity entity) {
        em.persist(entity);
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalsForWorkflowIdAndUserName(int workflowId, String userName) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and lower(t.userId) = :userName",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .setParameter("userName", userName.toLowerCase())
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalsForWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getCounterpartyDetailForWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfcdWorkflowCounterpartyDetailEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId",
                            ApprovalForumWfcdWorkflowCounterpartyDetailEntity.class)
                    .setParameter("workflowId", workflowId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getSpecificApprovalsForWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and t.specificApprover = :specific",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .setParameter("specific", true)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalsForWorkflowIdAndListName(int workflowId, String listName) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId " +
                                    "and t.aflmmApprovalForumListManagementMappingByAflmmId.listName = :listName",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .setParameter("listName", listName)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getAssignedApprovalsForWorkflowIdAndListName(int workflowId, String listName) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and not t.dateAssigned is null " +
                                    "and t.aflmmApprovalForumListManagementMappingByAflmmId.listName = :listName",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .setParameter("listName", listName)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getAssignedApprovalsForWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and not t.dateAssigned is null",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getCompletedApprovalsForWorkflowIdAndListName(int workflowId, String listName) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and not t.dateCompleted is null " +
                                    "and t.aflmmApprovalForumListManagementMappingByAflmmId.listName = :listName",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .setParameter("listName", listName)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfaWorkflowApprovalsEntity> getCompletedAdditionalApprovalsForWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and not t.dateCompleted is null " +
                                    "and t.additionalApprover = :additional",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .setParameter("additional", true)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    public List<ApprovalForumWfaWorkflowApprovalsEntity> getCompletedApprovalsForWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfaWorkflowApprovalsEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId and not t.dateCompleted is null " +
                                    "order by t.userId",
                            ApprovalForumWfaWorkflowApprovalsEntity.class)
                    .setParameter("workflowId", workflowId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateWorkflowApproval(ApprovalForumWfaWorkflowApprovalsEntity entity) {
        logger.info("updateWorkflowApproval:workflowId::" + entity.getDecision());
        em.merge(entity);
        logger.info("updateWorkflowApproval:workflowId1::" + entity.getDecision());
    }

    @Override
    public ApprovalForumRtRequestTypeEntity getApprovalForumRequestTypeByType(String type) {
        try {
            return em.createQuery("from ApprovalForumRtRequestTypeEntity t " +
                                    "where t.type = :type ",
                            ApprovalForumRtRequestTypeEntity.class).setParameter("type", type)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<ApprovalForumWfqdWorkflowQuorumDataEntity> getApprovalForumQuorumDataForQuorumId(int quorumId) {
        try {
            return em.createQuery("from ApprovalForumWfqdWorkflowQuorumDataEntity t " +
                                    "where t.wfqApprovalForumWorkflowQuorumByWfqId.wfqId = :quorumId",
                            ApprovalForumWfqdWorkflowQuorumDataEntity.class)
                    .setParameter("quorumId", quorumId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void persistQuorumDataEntity(ApprovalForumWfqdWorkflowQuorumDataEntity entity) {
        em.persist(entity);
    }

    @Override
    public void persistQuorumEntity(ApprovalForumWfqWorkflowQuorumEntity entity) {
        em.persist(entity);
    }

    @Override
    public List<ApprovalForumWfqWorkflowQuorumEntity> getWorkflowQuorumByWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfqWorkflowQuorumEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId",
                            ApprovalForumWfqWorkflowQuorumEntity.class)
                    .setParameter("workflowId", workflowId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public ApprovalForumWfqWorkflowQuorumEntity getSelectedWorkflowQuorumByWorkflowId(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfqWorkflowQuorumEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId " +
                                    "and t.selected = :selected",
                            ApprovalForumWfqWorkflowQuorumEntity.class).setParameter("workflowId", workflowId)
                    .setParameter("selected", true)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public ApprovalForumWfqWorkflowQuorumEntity getWorkflowQuorumById(int id) {
        try {
            return em.createQuery("from ApprovalForumWfqWorkflowQuorumEntity t " +
                                    "where t.wfqId = :id ",
                            ApprovalForumWfqWorkflowQuorumEntity.class).setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public ApprovalForumUpUserPreferenceEntity getUserPreference(String userId) {
        try {
            return em.createQuery("from ApprovalForumUpUserPreferenceEntity t " +
                                    "where lower(t.userId) = :userId ",
                            ApprovalForumUpUserPreferenceEntity.class).setParameter("userId", userId.toLowerCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void persistUserPreference(ApprovalForumUpUserPreferenceEntity entity) {
        em.persist(entity);
    }

    @Override
    public void updateUserPreference(ApprovalForumUpUserPreferenceEntity entity) {
        em.merge(entity);
    }

    @Override
    public int getWorkflowsCount(String assignedTo, boolean onlyActive, String searchText,
                                 WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType,
                                 boolean descending, String loggedInUserId, String approvalMandate) {
        try {
            Query query = getQuery(assignedTo, onlyActive, searchText, true, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, false);
            return (Integer) query.getSingleResult();
        } catch (NoResultException e) {
            return 0;
        }
    }

    @Override
    public List<ApprovalForumWfWorkflowEntity> getWorkflows(String assignedTo, int start, int size, boolean onlyActive, String searchText,
                                                            WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                            WorkflowSortByTypes sortByType, boolean descending,
                                                            String loggedInUserId, String approvalMandate) {
        try {
            Query query = getQuery(assignedTo, onlyActive, searchText, false, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, false);
            if (size > 0) query.setFirstResult(start).setMaxResults(size);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getWorkflowsMeetingNames(String assignedTo, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate) {
        try {
            Query query = getQuery(assignedTo, onlyActive, searchText, false, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, true);

            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private Query getQuery(String assignedTo, boolean onlyActive, String searchText, boolean count,
                           WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                           WorkflowSortByTypes sortByType, boolean descending,
                           String loggedInUserId, String approvalMandate, boolean allMandates) {

        String qry;

        if (allMandates) {
            qry = "select distinct ad.MeetingName ";
        } else {
            qry = "select distinct t.*, c.CounterpartyName, ad.MeetingName ";
        }
        qry = qry + " from WF_Workflow t left outer join WFQ_WorkflowQuorum q on (t.WF_ID = q.WF_ID and q.Selected = :selected) " +
                "left outer join WFQD_WorkflowQuorumData qd on (qd.WFQ_ID = q.WFQ_ID) " +
                "left outer join WFA_WorkflowApprovals a on (a.WF_ID = t.WF_ID and (a.AFLMM_ID = qd.AFLMM_ID or a.AdditionalApprover = :additional or a.ChairpersonApprover = :chairperson)) " +
                "join WFCD_WorkflowCounterpartyDetail c on (c.WF_ID = t.WF_ID) " +
                "join WFAD_WorkflowApplicationData ad on (ad.WFCD_ID = c.WFCD_ID) "+
                "join ER_ExternalRequests er on (er.WF_ID = t.WF_ID) ";

        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " lower(a.userId) = :loggedInUserId ");
            if (onlyActive)
                qry = appendToQuery(qry, " a.dateCompleted is null ");
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                qry = appendToQuery(qry, " not a.dateAssigned is null ");
            }
        }

        if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " (lower(ad.reviewAnalystId) = :reviewAnalystId or lower(ad.reviewAnalystId) = :reviewAnalystIdShort) ");
        }

        if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " ChairApprovalRequired = 'true' ");
        }

        if (onlyActive) qry = appendToQuery(qry, " t.active = :active ");
        if (notEmpty(searchText)) qry = appendToQuery(qry, " lower(c.counterpartyName) like :searchText ");
        if (!WorkflowFilterTypes.ALL.equals(filterType)) {
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                qry = appendToQuery(qry, " (t.confidential = :confidential or t.specific = :specific or a.AdditionalApprover = :additionalFilter) ");
            } else {
                qry = appendToQuery(qry, " (t.confidential = :confidential and t.specific = :specific and a.AdditionalApprover = :additionalFilter) ");
            }
        }
        if (notEmpty(assignedTo) && !WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            if (WorkflowListSearchTypes.CAPTURER.equals(listSearchTypes)) {
                qry = appendToQuery(qry, " lower(t.assignedTo) = :userId");
            } else if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
                qry = appendToQuery(qry, " (lower(a.userId) = :userId and not a.dateAssigned is null)");
            } else {
                qry = appendToQuery(qry, " (lower(a.userId) = :userId and not a.dateAssigned is null and a.dateCompleted is null)");
            }
        }

        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes) || WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " not lower(t.Status) = 'wip' and not lower(t.Status) = 'awaiting chairperson approval'");
        }

        if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " not lower(t.Status) = 'wip'");
        }

        if (approvalMandate != null && !"".equals(approvalMandate.trim()) && !allMandates) {
            qry = appendToQuery(qry, " lower(ad.meetingName) = :meetingName");
        }

        //Ashburton changes
        qry = appendToQuery(qry, " t.WorkFlowSystemName != 'Ashburton' ");

        //broader-africa changes
        qry = appendToQuery(qry, " er.TenantId not in (2,3,4,5,6,7)");

        if (!count && !allMandates) {
            if (!WorkflowSortByTypes.NONE.equals(sortByType) && !count) {
                if (WorkflowSortByTypes.REQUESTID.equals(sortByType)) {
                    qry = qry + " order by t.wf_id ";
                } else if (WorkflowSortByTypes.COUNTERPARTY.equals(sortByType)) {
                    qry = qry + " order by c.counterpartyName ";
                } else if (WorkflowSortByTypes.CAASSIGNEDTO.equals(sortByType)) {
                    qry = qry + " order by t.assignedTo ";
                } else if (WorkflowSortByTypes.CREATEDON.equals(sortByType)) {
                    qry = qry + " order by t.createdOn ";
                } else if (WorkflowSortByTypes.MANDATE.equals(sortByType)) {
                    qry = qry + " order by ad.meetingName ";
                } else if (WorkflowSortByTypes.STATUS.equals(sortByType)) {
                    qry = qry + " order by t.status ";
                } else if (WorkflowSortByTypes.SUBMITTEDBY.equals(sortByType)) {
                    qry = qry + " order by t.submittedBy ";
                } else if (WorkflowSortByTypes.SUBMITTEDON.equals(sortByType)) {
                    qry = qry + " order by t.submittedOn ";
                }
                if (descending) qry = qry + " desc";
            }
        } else {
            if (count) {
                qry = "select count(1) from (" + qry + ") as foo";
            }
        }

        Query query;
        if (count || allMandates) {
            query = em.createNativeQuery(qry);
        } else {
            query = em.createNativeQuery(qry, "ApprovalForumWfWorkflowEntity");
        }
        query.setParameter("selected", true);
        query.setParameter("additional", true);
        query.setParameter("chairperson", true);

        if (onlyActive) query.setParameter("active", onlyActive);
        if (notEmpty(searchText)) query.setParameter("searchText", "%" + searchText.toLowerCase() + "%");
        if (notEmpty(assignedTo) && !WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            query.setParameter("userId", assignedTo.toLowerCase());
        }
        if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            query.setParameter("reviewAnalystId", loggedInUserId.toLowerCase());
            if (loggedInUserId.toLowerCase().contains("@rmb.co.za")) {
                query.setParameter("reviewAnalystIdShort", loggedInUserId.toLowerCase().substring(0, loggedInUserId.toLowerCase().indexOf("@rmb.co.za")));
            } else {
                query.setParameter("reviewAnalystIdShort", loggedInUserId.toLowerCase());
            }
        }
        if (!WorkflowFilterTypes.ALL.equals(filterType)) {
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                query.setParameter("confidential", true);
                query.setParameter("specific", true);
                query.setParameter("additionalFilter", true);
            } else {
                query.setParameter("confidential", false);
                query.setParameter("specific", false);
                query.setParameter("additionalFilter", false);
            }
        }
        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes)) {
            query.setParameter("loggedInUserId", loggedInUserId.toLowerCase());
        }
        if (approvalMandate != null && !"".equals(approvalMandate.trim()) && !allMandates) {
            query.setParameter("meetingName", approvalMandate.toLowerCase());
        }

        return query;
    }

    private String appendToQuery(String current, String append) {
        if (current.contains("where")) {
            return current + " and " + append;
        }
        return current + "where" + append;
    }

    private boolean notEmpty(String value) {
        return value != null && !"".equals(value);
    }

    @Override
    public void persistRulesResponseEntity(ApprovalForumWiprrWipRulesResponseEntity rulesResponseEntity) {
        em.persist(rulesResponseEntity);
    }

    @Override
    public void updateRulesResponseEntity(ApprovalForumWiprrWipRulesResponseEntity rulesResponseEntity) {
        em.merge(rulesResponseEntity);
    }

    @Override
    public void persistUsersResponseEntity(ApprovalForumWipurWipUsersResponseEntity usersResponseEntity) {
        em.persist(usersResponseEntity);
    }

    @Override
    public void updateUsersResponseEntity(ApprovalForumWipurWipUsersResponseEntity usersResponseEntity) {
        em.merge(usersResponseEntity);
    }

    @Override
    public void createChat(ApprovalForumWfcWorkflowChatEntity chatEntity) {
        em.persist(chatEntity);
    }

    @Override
    public List<ApprovalForumWfcWorkflowChatEntity> getWorkflowChats(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfcWorkflowChatEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.wfId = :workflowId order by t.createdOn",
                            ApprovalForumWfcWorkflowChatEntity.class)
                    .setParameter("workflowId", workflowId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void persistApprovalForumErExternalRequestsEntity(ApprovalForumErExternalRequestsEntity entity) {
        em.persist(entity);
    }

    @Override
    public void updateApprovalForumErExternalRequestsEntity(ApprovalForumErExternalRequestsEntity entity) {
        em.merge(entity);
    }

    @Override
    public List<ApprovalForumErExternalRequestsEntity> getApprovalForumErExternalRequestsEntityByAppIdAndIsCreate(int appId, boolean isCreate, boolean isLumos) {
        try {
            return em.createQuery("from ApprovalForumErExternalRequestsEntity t where t.applicationId = :appId and t.create = :isCreate and t.complete = :isComplete and t.lumos = :isLumos",
                            ApprovalForumErExternalRequestsEntity.class)
                    .setParameter("appId", appId)
                    .setParameter("isCreate", isCreate)
                    .setParameter("isComplete", false)
                    .setParameter("isLumos", isLumos)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumErExternalRequestsEntity> getAshburtonApprovalForumErExternalRequestsEntityByAppIdAndIsCreate(int appId, boolean isCreate, int tenantId) {
        try {
            return em.createQuery("from ApprovalForumErExternalRequestsEntity t where t.applicationId = :appId and t.create = :isCreate and t.complete = :isComplete and t.tenantId = :tenantId",
                            ApprovalForumErExternalRequestsEntity.class)
                    .setParameter("appId", appId)
                    .setParameter("isCreate", isCreate)
                    .setParameter("isComplete", false)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumErExternalRequestsEntity> getOutstandingApprovalForumErExternalRequests() {
        try {
            return em.createQuery("from ApprovalForumErExternalRequestsEntity t " +
                                    "where t.complete = :completed order by t.erId",
                            ApprovalForumErExternalRequestsEntity.class)
                    .setParameter("completed", false)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }


    @Override
    public List<ApprovalForumDsrDocStashRequestsEntity> getOutstandingDocStashRequests() {
        try {
            return em.createQuery("from ApprovalForumDsrDocStashRequestsEntity t " +
                                    "where t.complete = :completed",
                            ApprovalForumDsrDocStashRequestsEntity.class)
                    .setParameter("completed", false)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void persistApprovalForumDsrDocStashRequestsEntity(ApprovalForumDsrDocStashRequestsEntity entity) {
        em.persist(entity);
    }

    @Override
    public void updateApprovalForumDsrDocStashRequestsEntity(ApprovalForumDsrDocStashRequestsEntity entity) {
        em.merge(entity);
    }

    @Override
    public List<ApprovalForumWfadWorkflowApplicationDataEntity> getApplicationDataByAppId(int appId) {
        try {
            return em.createQuery("from ApprovalForumWfadWorkflowApplicationDataEntity t " +
                                    "where t.applicationId = :appId and t.wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId.wfApprovalForumWorkflowByWfId.active = :active",
                            ApprovalForumWfadWorkflowApplicationDataEntity.class)
                    .setParameter("appId", appId)
                    .setParameter("active", true)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfadWorkflowApplicationDataEntity> getApplicationDataByAppIdAndLumos(int appId, boolean isLumos) {
        try {
            return em.createQuery("from ApprovalForumWfadWorkflowApplicationDataEntity t " +
                                    "where t.applicationId = :appId and t.wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId.wfApprovalForumWorkflowByWfId.active = :active" +
                                    " and t.lumos = :isLumos",
                            ApprovalForumWfadWorkflowApplicationDataEntity.class)
                    .setParameter("appId", appId)
                    .setParameter("active", true)
                    .setParameter("isLumos", isLumos)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfadWorkflowApplicationDataEntity> getAshburtonApplicationDataByAppIdAndTenantId(int appId, int tenantId) {
        try {
            return em.createQuery("from ApprovalForumWfadWorkflowApplicationDataEntity t " +
                                    "where t.applicationId = :appId and t.wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId.wfApprovalForumWorkflowByWfId.active = :active",
                            ApprovalForumWfadWorkflowApplicationDataEntity.class)
                    .setParameter("appId", appId)
                    .setParameter("active", true)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfWorkflowEntity> getWorkflows(boolean onlyActive) {
        try {
            String qry = "from ApprovalForumWfWorkflowEntity t";
            if (onlyActive) {
                qry = qry + " where t.active = :active";
            }

            Query query = em.createQuery(qry, ApprovalForumWfWorkflowEntity.class);
            if (onlyActive) query.setParameter("active", true);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getInkombaIdsForActiveWorkflows() {
        try {
            return em.createQuery("from ApprovalForumWfcdWorkflowCounterpartyDetailEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.active = :active",
                            ApprovalForumWfcdWorkflowCounterpartyDetailEntity.class)
                    .setParameter("active", true)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public ApprovalForumDsrDocStashRequestsEntity getDocStashRequest(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumDsrDocStashRequestsEntity t " +
                                    "where t.complete = :completed and t.wfId = :workflowId",
                            ApprovalForumDsrDocStashRequestsEntity.class)
                    .setParameter("completed", true)
                    .setParameter("workflowId", workflowId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void persistMailRequest(ApprovalForumMrMailRequestsEntity approvalForumMrMailRequestsEntity) {
        em.persist(approvalForumMrMailRequestsEntity);
    }

    @Override
    public List<ApprovalForumMrMailRequestsEntity> getUnsentMails() {
        try {
            return em.createQuery("from ApprovalForumMrMailRequestsEntity t " +
                                    "where t.complete = :completed order by t.id",
                            ApprovalForumMrMailRequestsEntity.class)
                    .setParameter("completed", false)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMailRequest(ApprovalForumMrMailRequestsEntity approvalForumMrMailRequestsEntity) {
        em.merge(approvalForumMrMailRequestsEntity);
    }

    @Override
    public int getLastWorkflowChatId(int workflowId) {
        List<ApprovalForumWfcWorkflowChatEntity> entities = em.createQuery("from ApprovalForumWfcWorkflowChatEntity t " +
                                "where t.wfApprovalForumWorkflowByWfId.id = :workflowId " +
                                "order by t.id desc",
                        ApprovalForumWfcWorkflowChatEntity.class)
                .setParameter("workflowId", workflowId)
                .setMaxResults(1).getResultList();
        if (entities != null && !entities.isEmpty()) return entities.get(0).getWfcId();
        return 0;
    }


    @Override
    public ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity getMeetingQuorumRuleByMeetingName(String meetingName) {
        try {
            return em.createQuery("from ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity t " +
                                    "where lower(t.meetingName) = :meetingName",
                            ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity.class)
                    .setParameter("meetingName", meetingName.toLowerCase())
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> getMeetingQuorumRules() {
        try {
            return em.createQuery("from ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity t where t.meetingName not like :mname",
                            ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity.class)
                    .setParameter("mname", "Ashburton" + "%")
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }


    @Override
    public List<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> getMeetingQuorumRulesForAshburton() {
        try {
            return em.createQuery("from ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity t where t.meetingName like :mname",
                            ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity.class)
                    .setParameter("mname", "%" + "Ashburton" + "%")
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> getMeetingQuorumRulesForBAClients(int tenantId) {
        try {
            String countryName = getCountryNameForTenant(tenantId);
            logger.info("getMeetingQuorumRulesForBAClients country name: " + countryName);
            return em.createQuery("from ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity t where t.meetingName like :mname",
                            ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity.class)
                    .setParameter("mname", "%" + countryName + "%")
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public int getApplicationId(int workflowId) {
        List<ApprovalForumErExternalRequestsEntity> entities = em.createQuery("from ApprovalForumErExternalRequestsEntity t where t.wfId = :wfId ",
                        ApprovalForumErExternalRequestsEntity.class)
                .setParameter("wfId", workflowId)
                .setMaxResults(1).getResultList();
        if (entities != null && !entities.isEmpty()) return entities.get(0).getApplicationId();
        return 0;
    }

    @Override
    public void persistASBLimitFundsWorkflow(ApprovalForumWfflWorkflowFundLimitInformationEntity workflowEntity) {
        em.persist(workflowEntity);
    }

    @Override
    public boolean isMandateMappedInRules(String name) {
        List<ApprovalForumRRulesEntity> entities = em.createQuery("from ApprovalForumRRulesEntity r " +
                                "where r.active = :active and r.rules like :name ",
                        ApprovalForumRRulesEntity.class)
                .setParameter("active", true)
                .setParameter("name", "%" + name.toUpperCase() + "%")
                .setMaxResults(1).getResultList();
        return entities != null && !entities.isEmpty();
    }

    @Override
    public void persistDailyMailRequest(ApprovalForumMrdaMailRequestsDailyAllEntity approvalForumMrdaMailRequestsDailyAllEntity) {
        em.persist(approvalForumMrdaMailRequestsDailyAllEntity);
    }

    @Override
    public List<ApprovalForumMrdaMailRequestsDailyAllEntity> getUnsentDailyMails() {
        try {
            return em.createQuery("from ApprovalForumMrdaMailRequestsDailyAllEntity t " +
                                    "where t.complete = :completed order by t.id",
                            ApprovalForumMrdaMailRequestsDailyAllEntity.class)
                    .setParameter("completed", false)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateDailyMailRequest(ApprovalForumMrdaMailRequestsDailyAllEntity approvalForumMrdaMailRequestsDailyAllEntity) {
        em.merge(approvalForumMrdaMailRequestsDailyAllEntity);
    }

    @Override
    public List<ApprovalForumWfWorkflowEntity>  getWorkflowsForAshburton(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate) {
        try {
            Query query = getQueryForAshburton(assignedTo, onlyActive, searchText, false, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, false);
            if (size > 0) query.setFirstResult(start).setMaxResults(size);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApprovalForumWfWorkflowEntity> getWorkflowsForLumosBroaderAfricaTenants(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId) {
        try {
            Query query = getQueryForLumosBroaderAfricaTenants(assignedTo, onlyActive, searchText, false, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, false, tenantId);
            if (size > 0) query.setFirstResult(start).setMaxResults(size);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private Query getQueryForLumosBroaderAfricaTenants(String assignedTo, boolean onlyActive, String searchText, boolean count, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, boolean allMandates, int tenantId) {
        String qry;

        if (allMandates) {
            qry = "select distinct ad.MeetingName ";
        } else {
            qry = "select distinct t.*, c.CounterpartyName, ad.MeetingName ";
        }
        qry = qry + " from WF_Workflow t left outer join WFQ_WorkflowQuorum q on (t.WF_ID = q.WF_ID and q.Selected = :selected) " +
                "left outer join WFQD_WorkflowQuorumData qd on (qd.WFQ_ID = q.WFQ_ID) " +
                "left outer join WFA_WorkflowApprovals a on (a.WF_ID = t.WF_ID and (a.AFLMM_ID = qd.AFLMM_ID or a.AdditionalApprover = :additional or a.ChairpersonApprover = :chairperson)) " +
                "join WFCD_WorkflowCounterpartyDetail c on (c.WF_ID = t.WF_ID) " +
                "join WFAD_WorkflowApplicationData ad on (ad.WFCD_ID = c.WFCD_ID) " +
                "join ER_ExternalRequests er on (er.WF_ID = t.WF_ID) ";

        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " lower(a.userId) = :loggedInUserId ");
            if (onlyActive)
                qry = appendToQuery(qry, " a.dateCompleted is null ");
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                qry = appendToQuery(qry, " not a.dateAssigned is null ");
            }
        }

        if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " (lower(ad.reviewAnalystId) = :reviewAnalystId or lower(ad.reviewAnalystId) = :reviewAnalystIdShort) ");
        }

        if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " ChairApprovalRequired = 'true' ");
        }

        if (onlyActive) qry = appendToQuery(qry, " t.active = :active ");
        if (notEmpty(searchText)) qry = appendToQuery(qry, " lower(c.counterpartyName) like :searchText ");
        if (!WorkflowFilterTypes.ALL.equals(filterType)) {
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                qry = appendToQuery(qry, " (t.confidential = :confidential or t.specific = :specific or a.AdditionalApprover = :additionalFilter) ");
            } else {
                qry = appendToQuery(qry, " (t.confidential = :confidential and t.specific = :specific and a.AdditionalApprover = :additionalFilter) ");
            }
        }
        if (notEmpty(assignedTo) && !WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            if (WorkflowListSearchTypes.CAPTURER.equals(listSearchTypes)) {
                qry = appendToQuery(qry, " lower(t.assignedTo) = :userId");
            } else if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
                qry = appendToQuery(qry, " (lower(a.userId) = :userId and not a.dateAssigned is null)");
            } else {
                qry = appendToQuery(qry, " (lower(a.userId) = :userId and not a.dateAssigned is null and a.dateCompleted is null)");
            }
        }

        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes) || WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " not lower(t.Status) = 'wip' and not lower(t.Status) = 'awaiting chairperson approval'");
        }

        if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " not lower(t.Status) = 'wip'");
        }

        if (approvalMandate != null && !"".equals(approvalMandate.trim()) && !allMandates) {
            qry = appendToQuery(qry, " lower(ad.meetingName) = :meetingName");
        }
        if (tenantId > 2) {
            //BroaderAfrica changes
            qry = appendToQuery(qry, " er.tenantId = '" + tenantId + "' ");

        }
        logger.info("getQueryForLumosBroaderAfricaTenants:" + qry.toString());

        if (!count && !allMandates) {
            if (!WorkflowSortByTypes.NONE.equals(sortByType) && !count) {
                if (WorkflowSortByTypes.REQUESTID.equals(sortByType)) {
                    qry = qry + " order by t.wf_id ";
                } else if (WorkflowSortByTypes.COUNTERPARTY.equals(sortByType)) {
                    qry = qry + " order by c.counterpartyName ";
                } else if (WorkflowSortByTypes.CAASSIGNEDTO.equals(sortByType)) {
                    qry = qry + " order by t.assignedTo ";
                } else if (WorkflowSortByTypes.CREATEDON.equals(sortByType)) {
                    qry = qry + " order by t.createdOn ";
                } else if (WorkflowSortByTypes.MANDATE.equals(sortByType)) {
                    qry = qry + " order by ad.meetingName ";
                } else if (WorkflowSortByTypes.STATUS.equals(sortByType)) {
                    qry = qry + " order by t.status ";
                } else if (WorkflowSortByTypes.SUBMITTEDBY.equals(sortByType)) {
                    qry = qry + " order by t.submittedBy ";
                } else if (WorkflowSortByTypes.SUBMITTEDON.equals(sortByType)) {
                    qry = qry + " order by t.submittedOn ";
                }
                if (descending) qry = qry + " desc";
            }
        } else {
            if (count) {
                qry = "select count(1) from (" + qry + ") as foo";
            }
        }

        Query query;
        if (count || allMandates) {
            query = em.createNativeQuery(qry);
        } else {
            query = em.createNativeQuery(qry, "ApprovalForumWfWorkflowEntity");
        }
        query.setParameter("selected", true);
        query.setParameter("additional", true);
        query.setParameter("chairperson", true);

        if (onlyActive) query.setParameter("active", onlyActive);
        if (notEmpty(searchText)) query.setParameter("searchText", "%" + searchText.toLowerCase() + "%");
        if (notEmpty(assignedTo) && !WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            query.setParameter("userId", assignedTo.toLowerCase());
        }
        if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            query.setParameter("reviewAnalystId", loggedInUserId.toLowerCase());
            if (loggedInUserId.toLowerCase().contains("@rmb.co.za")) {
                query.setParameter("reviewAnalystIdShort", loggedInUserId.toLowerCase().substring(0, loggedInUserId.toLowerCase().indexOf("@rmb.co.za")));
            } else {
                query.setParameter("reviewAnalystIdShort", loggedInUserId.toLowerCase());
            }
        }
        if (!WorkflowFilterTypes.ALL.equals(filterType)) {
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                query.setParameter("confidential", true);
                query.setParameter("specific", true);
                query.setParameter("additionalFilter", true);
            } else {
                query.setParameter("confidential", false);
                query.setParameter("specific", false);
                query.setParameter("additionalFilter", false);
            }
        }
        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes)) {
            query.setParameter("loggedInUserId", loggedInUserId.toLowerCase());
        }
        if (approvalMandate != null && !"".equals(approvalMandate.trim()) && !allMandates) {
            query.setParameter("meetingName", approvalMandate.toLowerCase());
        }

        return query;
    }


    @Override
    public int getWorkflowsCountForAshburton(String assignedTo, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate) {
        try {
            Query query = getQueryForAshburton(assignedTo, onlyActive, searchText, true, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, false);
            return (Integer) query.getSingleResult();
        } catch (NoResultException e) {
            return 0;
        }
    }

    //getWorkflowsCountForLumosBroaderAfricaTenants
    @Override
    public int getWorkflowsCountForLumosBroaderAfricaTenants(String assignedTo, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId) {
        try {
            Query query = getQueryForLumosBroaderAfricaTenants(assignedTo, onlyActive, searchText, true, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, false, tenantId);
            return (Integer) query.getSingleResult();
        } catch (NoResultException e) {
            return 0;
        }
    }

    @Override
    public List<String> getWorkflowsMeetingNamesForAshburton(String assignedTo, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate) {
        try {
            Query query = getQueryForAshburton(assignedTo, onlyActive, searchText, false, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, true);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    //getWorkflowsMeetingNamesForLumosBroaderAfricaTenants
    @Override
    public List<String> getWorkflowsMeetingNamesForLumosBroaderAfricaTenants(String assignedTo, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId) {
        try {
            Query query = getQueryForLumosBroaderAfricaTenants(assignedTo, onlyActive, searchText, false, filterType, listSearchTypes, sortByType,
                    descending, loggedInUserId, approvalMandate, true, tenantId);
            return query.getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public ApprovalForumWfWorkflowEntity getWorkflowForAshburton(int workflowId) {
        try {
            return em.createQuery("from ApprovalForumWfWorkflowEntity t " +
                                    "where t.wfId = :workflowId and t.workflowSystemName='Ashburton'",
                            ApprovalForumWfWorkflowEntity.class).setParameter("workflowId", workflowId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getInkombaIdsForActiveWorkflowsForAshburton() {
        try {
            return em.createQuery("from ApprovalForumWfcdWorkflowCounterpartyDetailEntity t " +
                                    "where t.wfApprovalForumWorkflowByWfId.active = :active and t.wfApprovalForumWorkflowByWfId.workflowSystemName='Ashburton'",
                            ApprovalForumWfcdWorkflowCounterpartyDetailEntity.class)
                    .setParameter("active", true)
                    .getResultList();
        } catch (NoResultException e) {
            return new ArrayList<>();
        }
    }

    private Query getQueryForAshburton(String assignedTo, boolean onlyActive, String searchText, boolean count,
                                       WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                       WorkflowSortByTypes sortByType, boolean descending,
                                       String loggedInUserId, String approvalMandate, boolean allMandates) {
        String qry;

        if (allMandates) {
            qry = "select distinct ad.MeetingName ";
        } else {
            qry = "select distinct t.*, c.CounterpartyName, ad.MeetingName ";
        }
        qry = qry + " from WF_Workflow t left outer join WFQ_WorkflowQuorum q on (t.WF_ID = q.WF_ID and q.Selected = :selected) " +
                "left outer join WFQD_WorkflowQuorumData qd on (qd.WFQ_ID = q.WFQ_ID) " +
                "left outer join WFA_WorkflowApprovals a on (a.WF_ID = t.WF_ID and (a.AFLMM_ID = qd.AFLMM_ID or a.AdditionalApprover = :additional or a.ChairpersonApprover = :chairperson)) " +
                "join WFCD_WorkflowCounterpartyDetail c on (c.WF_ID = t.WF_ID) " +
                "join WFAD_WorkflowApplicationData ad on (ad.WFCD_ID = c.WFCD_ID) ";

        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " lower(a.userId) = :loggedInUserId ");
            if (onlyActive)
                qry = appendToQuery(qry, " a.dateCompleted is null ");
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                qry = appendToQuery(qry, " not a.dateAssigned is null ");
            }
        }

        if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " (lower(ad.reviewAnalystId) = :reviewAnalystId or lower(ad.reviewAnalystId) = :reviewAnalystIdShort) ");
        }

        if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " ChairApprovalRequired = 'true' ");
        }

        if (onlyActive) qry = appendToQuery(qry, " t.active = :active ");
        if (notEmpty(searchText)) qry = appendToQuery(qry, " lower(c.counterpartyName) like :searchText ");
        if (!WorkflowFilterTypes.ALL.equals(filterType)) {
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                qry = appendToQuery(qry, " (t.confidential = :confidential or t.specific = :specific or a.AdditionalApprover = :additionalFilter) ");
            } else {
                qry = appendToQuery(qry, " (t.confidential = :confidential and t.specific = :specific and a.AdditionalApprover = :additionalFilter) ");
            }
        }
        if (notEmpty(assignedTo) && !WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            if (WorkflowListSearchTypes.CAPTURER.equals(listSearchTypes)) {
                qry = appendToQuery(qry, " lower(t.assignedTo) = :userId");
            } else if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)){
                qry = appendToQuery(qry, " (lower(a.userId) = :userId and not a.dateAssigned is null)");
            } else {
                qry = appendToQuery(qry, " (lower(a.userId) = :userId and not a.dateAssigned is null and a.dateCompleted is null)");
            }
        }

        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes) || WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " not lower(t.Status) = 'wip' and not lower(t.Status) = 'awaiting chairperson approval'");
        }

        if (WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
            qry = appendToQuery(qry, " not lower(t.Status) = 'wip'");
        }

        if (approvalMandate != null && !"".equals(approvalMandate.trim()) && !allMandates) {
            qry = appendToQuery(qry, " lower(ad.meetingName) = :meetingName");
        }

        //Ashburton changes
        qry = appendToQuery(qry, " t.WorkFlowSystemName = 'Ashburton' ");

        logger.info("getQueryForAshburton:" + qry.toString());
        if (!count && !allMandates) {
            if (!WorkflowSortByTypes.NONE.equals(sortByType) && !count) {
                if (WorkflowSortByTypes.REQUESTID.equals(sortByType)) {
                    qry = qry + " order by t.wf_id ";
                } else if (WorkflowSortByTypes.COUNTERPARTY.equals(sortByType)) {
                    qry = qry + " order by c.counterpartyName ";
                } else if (WorkflowSortByTypes.CAASSIGNEDTO.equals(sortByType)) {
                    qry = qry + " order by t.assignedTo ";
                } else if (WorkflowSortByTypes.CREATEDON.equals(sortByType)) {
                    qry = qry + " order by t.createdOn ";
                } else if (WorkflowSortByTypes.MANDATE.equals(sortByType)) {
                    qry = qry + " order by ad.meetingName ";
                } else if (WorkflowSortByTypes.STATUS.equals(sortByType)) {
                    qry = qry + " order by t.status ";
                } else if (WorkflowSortByTypes.SUBMITTEDBY.equals(sortByType)) {
                    qry = qry + " order by t.submittedBy ";
                } else if (WorkflowSortByTypes.SUBMITTEDON.equals(sortByType)) {
                    qry = qry + " order by t.submittedOn ";
                }
                if (descending) qry = qry + " desc";
            }
        } else {
            if (count) {
                qry = "select count(1) from (" + qry + ") as foo";
            }
        }

        Query query;
        if (count || allMandates) {
            query = em.createNativeQuery(qry);
        } else {
            query = em.createNativeQuery(qry, "ApprovalForumWfWorkflowEntity");
        }
        query.setParameter("selected", true);
        query.setParameter("additional", true);
        query.setParameter("chairperson", true);

        if (onlyActive) query.setParameter("active", onlyActive);
        if (notEmpty(searchText)) query.setParameter("searchText", "%" + searchText.toLowerCase() + "%");
        if (notEmpty(assignedTo) && !WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            query.setParameter("userId", assignedTo.toLowerCase());
        }
        if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
            query.setParameter("reviewAnalystId", loggedInUserId.toLowerCase());
            if (loggedInUserId.toLowerCase().contains("@rmb.co.za")) {
                query.setParameter("reviewAnalystIdShort", loggedInUserId.toLowerCase().substring(0, loggedInUserId.toLowerCase().indexOf("@rmb.co.za")));
            } else {
                query.setParameter("reviewAnalystIdShort", loggedInUserId.toLowerCase());
            }
        }
        if (!WorkflowFilterTypes.ALL.equals(filterType)) {
            if (WorkflowFilterTypes.SPECIFIC.equals(filterType)) {
                query.setParameter("confidential", true);
                query.setParameter("specific", true);
                query.setParameter("additionalFilter", true);
            } else {
                query.setParameter("confidential", false);
                query.setParameter("specific", false);
                query.setParameter("additionalFilter", false);
            }
        }
        if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes)) {
            query.setParameter("loggedInUserId", loggedInUserId.toLowerCase());
        }
        if (approvalMandate != null && !"".equals(approvalMandate.trim()) && !allMandates) {
            query.setParameter("meetingName", approvalMandate.toLowerCase());
        }

        return query;
    }

    @Override
    public ApprovalForumWfflWorkflowFundLimitInformationEntity getASBLimitFunds(int applicationId) {
        logger.info("ApprovalForumWfflWorkflowFundLimitInformationEntity:" + applicationId);
        try {
            return em.createQuery("from ApprovalForumWfflWorkflowFundLimitInformationEntity t " +
                                    "where t.wffappId = :applicationId",
                            ApprovalForumWfflWorkflowFundLimitInformationEntity.class)
                    .setParameter("applicationId", applicationId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public int getTenantId(int workflowId) {
        List<ApprovalForumErExternalRequestsEntity> entities = em.createQuery("from ApprovalForumErExternalRequestsEntity t where t.wfId = :wfId and tenantId != null",
                        ApprovalForumErExternalRequestsEntity.class)
                .setParameter("wfId", workflowId)
                .setMaxResults(1).getResultList();
        if (entities != null && !entities.isEmpty()) return entities.get(0).getTenantId();
        return 0;
    }

    @Override
    public String getMeetingNameByWorkflowId(int wfId) {
        logger.info("getMeetingNameByWorkflowId:"+wfId);
        List<ApprovalForumWfadWorkflowApplicationDataEntity>  entities = em.createQuery("from ApprovalForumWfadWorkflowApplicationDataEntity t " +
                        "where t.wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId.wfApprovalForumWorkflowByWfId.wfId=:wfId", ApprovalForumWfadWorkflowApplicationDataEntity.class)
                .setParameter("wfId", wfId)
                .setMaxResults(1).getResultList();
        if (entities != null && !entities.isEmpty()) return entities.get(0).getMeetingName();
        return "";
    }

    @Override
    public String getInkombaIdByWorkflowId(int workflowId) {
        try {
            List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> entities = em.createQuery(
                            "FROM ApprovalForumWfcdWorkflowCounterpartyDetailEntity t " +
                                    "WHERE t.wfApprovalForumWorkflowByWfId.active = :active AND t.wfApprovalForumWorkflowByWfId.wfId = :wfId",
                            ApprovalForumWfcdWorkflowCounterpartyDetailEntity.class)
                    .setParameter("active", true)
                    .setParameter("wfId", workflowId)
                    .setMaxResults(1)
                    .getResultList();

            if (!entities.isEmpty()) {
                return entities.get(0).getCreditInkombaId();
            }
            return "";
        } catch (Exception e) {
            logger.error("Error fetching Inkomba ID for workflowId {}: {}", workflowId, e.getMessage(), e);
            return "";
        }
    }

    private String getCountryNameForTenant(int tenantId) {
        switch (tenantId) {
            case 3:
                return "Botswana";
            case 4:
                return "Eswatini";
            case 5:
                return "Lesotho";
            case 6:
                return "Namibia";
            case 7:
                return "Zambia";
            default:
                return "";
        }
    }
}

