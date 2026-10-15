package za.co.rmb.approvalforum.data;

import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.WorkflowFilterTypes;
import za.co.rmb.approvalforum.model.WorkflowListSearchTypes;
import za.co.rmb.approvalforum.model.WorkflowSortByTypes;

import java.util.Arrays;
import java.util.List;

public interface ApprovalForumDataRepository {

    ApprovalForumAflmmApprovalForumListManagementMappingEntity getListManagementMappingByName(String name);

    void persistWorkflow(ApprovalForumWfWorkflowEntity wfWorkflowEntity);

    void updateWorkflow(ApprovalForumWfWorkflowEntity wfWorkflowEntity);

    void persistWorkflowCounterpartyDetail(ApprovalForumWfcdWorkflowCounterpartyDetailEntity wfcdWorkflowCounterpartyDetailEntity);

    void persistWorkflowData(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataEntity);

    void updateWorkflowData(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataEntity);

    void deleteWorkflowData(ApprovalForumWfdaWorkflowDataEntity wfdaWorkflowDataEntity);

    void persistWorkflowApplicationData(ApprovalForumWfadWorkflowApplicationDataEntity wfadWorkflowApplicationDataEntity);

    void persistWorkflowFacilityInformation(ApprovalForumWffiWorkflowFacilityInformationEntity wffiWorkflowFacilityInformationEntity);

    ApprovalForumWfWorkflowEntity getWorkflow(int workflowId);

    List<ApprovalForumAflmmApprovalForumListManagementMappingEntity> getAllListManagementMappings();

    void persistWorkflowApprovals(ApprovalForumWfaWorkflowApprovalsEntity entity);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalsForWorkflowIdAndUserName(int workflowId, String userName);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalsForWorkflowId(int workflowId);

    List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getCounterpartyDetailForWorkflowId(int workflowId);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getSpecificApprovalsForWorkflowId(int workflowId);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalsForWorkflowIdAndListName(int workflowId, String listName);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getAssignedApprovalsForWorkflowIdAndListName(int workflowId, String listName);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getAssignedApprovalsForWorkflowId(int workflowId);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getCompletedApprovalsForWorkflowIdAndListName(int workflowId, String listName);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getCompletedAdditionalApprovalsForWorkflowId(int workflowId);

    List<ApprovalForumWfaWorkflowApprovalsEntity> getCompletedApprovalsForWorkflowId(int workflowId);

    void updateWorkflowApproval(ApprovalForumWfaWorkflowApprovalsEntity entity);

    ApprovalForumRtRequestTypeEntity getApprovalForumRequestTypeByType(String type);

    List<ApprovalForumWfqdWorkflowQuorumDataEntity> getApprovalForumQuorumDataForQuorumId(int quorumId);

    void persistQuorumDataEntity(ApprovalForumWfqdWorkflowQuorumDataEntity entity);

    void persistQuorumEntity(ApprovalForumWfqWorkflowQuorumEntity entity);

    List<ApprovalForumWfqWorkflowQuorumEntity> getWorkflowQuorumByWorkflowId(int workflowId);

    ApprovalForumWfqWorkflowQuorumEntity getSelectedWorkflowQuorumByWorkflowId(int workflowId);

    ApprovalForumWfqWorkflowQuorumEntity getWorkflowQuorumById(int id);

    ApprovalForumUpUserPreferenceEntity getUserPreference(String userId);

    void persistUserPreference(ApprovalForumUpUserPreferenceEntity entity);

    void updateUserPreference(ApprovalForumUpUserPreferenceEntity entity);

    List<ApprovalForumWfWorkflowEntity> getWorkflows(String assignedTo, int start, int size, boolean onlyActive, String searchText,
                                                     WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                     WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate);

    int getWorkflowsCount(String assignedTo, boolean onlyActive, String searchText,
                          WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                          WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate);

    List<String> getWorkflowsMeetingNames(String assignedTo, boolean onlyActive, String searchText,
                                          WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                          WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate);

    void persistRulesResponseEntity(ApprovalForumWiprrWipRulesResponseEntity rulesResponseEntity);

    void updateRulesResponseEntity(ApprovalForumWiprrWipRulesResponseEntity rulesResponseEntity);

    void persistUsersResponseEntity(ApprovalForumWipurWipUsersResponseEntity usersResponseEntity);

    void updateUsersResponseEntity(ApprovalForumWipurWipUsersResponseEntity usersResponseEntity);

    void createChat(ApprovalForumWfcWorkflowChatEntity chatEntity);

    List<ApprovalForumWfcWorkflowChatEntity> getWorkflowChats(int workflowId);

    void persistApprovalForumErExternalRequestsEntity(ApprovalForumErExternalRequestsEntity entity);

    void updateApprovalForumErExternalRequestsEntity(ApprovalForumErExternalRequestsEntity entity);

    List<ApprovalForumErExternalRequestsEntity> getApprovalForumErExternalRequestsEntityByAppIdAndIsCreate(int appId, boolean isCreate, boolean isLumos);

    List<ApprovalForumErExternalRequestsEntity> getOutstandingApprovalForumErExternalRequests();

    List<ApprovalForumDsrDocStashRequestsEntity> getOutstandingDocStashRequests();

    void persistApprovalForumDsrDocStashRequestsEntity(ApprovalForumDsrDocStashRequestsEntity entity);

    void updateApprovalForumDsrDocStashRequestsEntity(ApprovalForumDsrDocStashRequestsEntity entity);

    ApprovalForumDsrDocStashRequestsEntity getDocStashRequest(int workflowId);

    List<ApprovalForumWfadWorkflowApplicationDataEntity> getApplicationDataByAppId(int appId);

    List<ApprovalForumWfadWorkflowApplicationDataEntity> getApplicationDataByAppIdAndLumos(int appId, boolean isLumos);

    List<ApprovalForumWfWorkflowEntity> getWorkflows(boolean onlyActive);

    List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getInkombaIdsForActiveWorkflows();

    void persistMailRequest(ApprovalForumMrMailRequestsEntity approvalForumMrMailRequestsEntity);

    List<ApprovalForumMrMailRequestsEntity> getUnsentMails();

    void updateMailRequest(ApprovalForumMrMailRequestsEntity approvalForumMrMailRequestsEntity);

    int getLastWorkflowChatId(int workflowId);

    ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity getMeetingQuorumRuleByMeetingName(String meetingName);

    List<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> getMeetingQuorumRules();

    boolean isMandateMappedInRules(String name);

    void persistDailyMailRequest(ApprovalForumMrdaMailRequestsDailyAllEntity approvalForumMrdaMailRequestsDailyAllEntity);

    List<ApprovalForumMrdaMailRequestsDailyAllEntity> getUnsentDailyMails();

    void updateDailyMailRequest(ApprovalForumMrdaMailRequestsDailyAllEntity approvalForumMrdaMailRequestsDailyAllEntity);

    List<ApprovalForumWfadWorkflowApplicationDataEntity> getAshburtonApplicationDataByAppIdAndTenantId(int appId, int tenantId);

    List<ApprovalForumWfWorkflowEntity> getWorkflowsForAshburton(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate);

    int getWorkflowsCountForAshburton(String assignedTo, boolean onlyActive, String searchText,
                                      WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                      WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate);

    int getWorkflowsCountForLumosBroaderAfricaTenants(String assignedTo, boolean onlyActive, String searchText,
                                                      WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                      WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId);

    List<String> getWorkflowsMeetingNamesForAshburton(String assignedTo, boolean onlyActive, String searchText,
                                                      WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                      WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate);

    List<String> getWorkflowsMeetingNamesForLumosBroaderAfricaTenants(String assignedTo, boolean onlyActive, String searchText,
                                                                      WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                                      WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId);

    ApprovalForumWfWorkflowEntity getWorkflowForAshburton(int workflowId);

    List<ApprovalForumWfWorkflowEntity> getWorkflowsForLumosBroaderAfricaTenants(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId);

    List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity>getInkombaIdsForActiveWorkflowsForAshburton();

    List<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity>  getMeetingQuorumRulesForAshburton();

    List<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity>  getMeetingQuorumRulesForBAClients(int tenantId);

    int getApplicationId(int workflowId);

    List<ApprovalForumErExternalRequestsEntity> getAshburtonApprovalForumErExternalRequestsEntityByAppIdAndIsCreate(int appId, boolean isCreate, int tenantId);

    void persistASBLimitFundsWorkflow(ApprovalForumWfflWorkflowFundLimitInformationEntity workflowEntity);

    ApprovalForumWfflWorkflowFundLimitInformationEntity getASBLimitFunds(int applicationId);

    int getTenantId(int workflowId);

    String  getInkombaIdByWorkflowId(int workflowId);

    String getMeetingNameByWorkflowId(int wfId);

    // End
}
