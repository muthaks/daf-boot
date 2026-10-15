package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.data.entity.ApprovalForumWfflWorkflowFundLimitInformationEntity;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.exception.ResourceException;

import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.List;

public interface ApprovalForumService {

    int createRequest(Boolean isExternal, RequestData requestData, String user) throws ResourceException;

    int cancelRequest(int workflowId, String userName) throws ResourceException;

    int updateRequest(int workflowId, RequestData requestData, String user) throws ResourceException;

    int submitRequest(int workflowId, RequestData requestData, String user) throws ResourceException;

    int submitRequestToChairperson(int workflowId, RequestData requestData, String user) throws ResourceException;

    void createChat(int workflowId, String chatText, Principal user);

    List<Chat> getWorkflowChats(int workflowId);

    int createMemo(CreateMemoDTO requestDTO, String user);

    RequestData getRequest(int workflowId, String userName, boolean userDisplayNames,
                           WorkflowListSearchTypes searchTypes) throws ResourceException;

    UserPreferenceResponseDTO getUserPreference(String userId);

    WorkflowListResponseDTO getWorkflows(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType,
                                         WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending,
                                         String loggedInUserId, String approvalMandate);

    AssignResponse checkIfWorkflowCanBeAssigned(int workflowId, String userName) throws ResourceException;

    AssignResponse checkIfWipWorkflowCanBeAssigned(int workflowId, String userName) throws ResourceException;

    boolean checkIfWorkflowCanBeViewed(int workflowId, String userName) throws ResourceException;

    AssignResponse checkIfWorkflowCanBeSubmitted(int workflowId, String userName) throws ResourceException;

    AssignResponse checkIfWipWorkflowCanBeAssigned(RulesResponse rulesResponse, UsersResponse usersResponse, String userName, boolean isForSpecific)
            throws ResourceException;

    AssignResponse checkIfChairpersonWorkflowCanBeAssigned(RulesResponse rulesResponse, UsersResponse usersResponse,
                                                           String userName, boolean isForSpecific, int workflowId) throws ResourceException;

    AssignResponse assignWorkflow(int workflowId, String userName) throws ResourceException;

    AssignResponse assignWipWorkflow(int workflowId, String userName);

    AssignResponse unAssignWorkflow(int workflowId, String userName) throws ResourceException;

    AssignResponse unAssignWipWorkflow(int workflowId, String userName) throws ResourceException;

    void submitWorkflow(int workflowId, String userName, SubmitDecisionRequestDTO submitDecisionRequestDTO) throws ResourceException;

    void updateWorkflowDecision(int workflowId, String userName, UpdateDecisionRequestDTO updateDecisionRequestDTO);

    RulesResponse processRules(RulesRequestDTO rulesRequestDTO) throws ResourceException;

    void createRequestExternal(int appId, boolean isCreate, boolean lumos, int tenantId);

    void cleanupRequests() throws ResourceException;

    void processExternalRequests();

    void sendMailEscalations();

    void sendApproverMailEscalation();

    void sendCapturerMailEscalation();

    List<String> getInkombaIdsForActiveWorkflows();

    NewRequestResponseDTO getNewRequestDetails(String inkombaId) throws ResourceException;

    NewRequestResponseDTO getNewRequestDetails(String inkombaId, String meetingName) throws ResourceException;

    Response submitDecision(int workflowId, SubmitDecisionRequestDTO submitDecisionRequestDTO,
                                    boolean isSystem, String username);

    void updateCapturerLastSeenChatId(int workflowId, int chatId, String userName);

    void updateReviewerLastSeenChatId(int workflowId, int chatId, String userName);

    void updateApproverLastSeenChatId(int workflowId, int chatId, String userName);

    void updateChairpersonLastSeenChatId(int workflowId, int chatId, String userName);

    void updateUserPreference(String userId, boolean chatSortAsc);

    List<String> getAllMeetingNames();

    boolean isChairpersonApprovalRequired(String meetingName);

    void createAshburtonRequestExternal(int appId, boolean isCreate);

    NewRequestResponseDTO getNewRequestDetailsForAshburton(String inkombaId, int appId) throws ResourceException;

    NewRequestResponseDTO getNewRequestDetailsForAshburton(String inkombaId, String meetingName,  int appId) throws ResourceException;

    WorkflowListResponseDTO getWorkflowsForAshburton(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType,
                                         WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending,
                                         String loggedInUserId, String approvalMandate);

    WorkflowListResponseDTO getWorkflowsForLumosBroaderAfricaTenants(String assignedTo, int start, int size, boolean onlyActive, String searchText, WorkflowFilterTypes filterType,
                                                     WorkflowListSearchTypes listSearchTypes, WorkflowSortByTypes sortByType, boolean descending,
                                                     String loggedInUserId, String approvalMandate, int tenantId);

    RequestData getRequestForAshburton(int workflowId, String userName, boolean userDisplayNames,
                                       WorkflowListSearchTypes searchTypes) throws ResourceException;


    RequestData getRequestForLumosBroaderAfricaTenants(int workflowId, String userName, boolean userDisplayNames,
                                       WorkflowListSearchTypes searchTypes, int tenantId) throws ResourceException;


    List<String> getInkombaIdsForActiveWorkflowsForAshburton();

    List<String> getAllMeetingNamesForAshburton();

    ASBLimitFundsDTO getASBLimitFunds(int applicationId);

    boolean isASBLimitFundsPersisted(int applicationId);

    ASBLimitFundsDTO persistASBLimitFundsWorkflow(ApprovalForumWfflWorkflowFundLimitInformationEntity workflowEntity);

    List<MeetingName> getBroaderAfricaMeetingNames(int tenantId);

    List<String> getInternalBroaderAfricaMeetingNames(int tenantId);

    AssignResponse sendAssignToMeNofitication(int workflow, String username, CanBeAssignedResponseDTO message) throws ResourceException, UnsupportedEncodingException;

    AssignResponse checkIfCanAssignWorkflowToMe(int workflowId, String username) throws ResourceException;

    AssignResponse sendAssignToMeNofiticationFailure(int workflow, String username, CanBeAssignedResponseDTO message, String workflowIdStr) throws ResourceException, UnsupportedEncodingException;

}

