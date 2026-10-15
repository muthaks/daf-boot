package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumDataAuditRepository;
import za.co.rmb.approvalforum.data.ApprovalForumDataRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.model.lumos.WorkflowResponseDTO;
import za.co.rmb.approvalforum.service.*;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.approvalforum.service.exception.RulesException;
import za.co.rmb.approvalforum.service.util.ApprovalForumServiceUtil;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesModel;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesResponseDTO;
import za.co.rmb.approvalforumrules.model.ListModelData;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.*;
import java.net.URISyntaxException;
import java.security.Principal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static za.co.rmb.approvalforum.model.WorkflowStatus.AWAITING_CHAIRPERSON_APPROVAL;

public class ApprovalForumServiceImpl extends ApprovalForumServiceUtil implements ApprovalForumService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumServiceImpl.class);
    private final ApprovalForumDataRepository approvalForumDataRepository;
    private final ApprovalForumDataAuditRepository approvalForumDataAuditRepository;
    private final ApprovalForumRulesService approvalForumRulesService;
    private final ApprovalForumMailService approvalForumMailService;
    private final ApprovalForumAuthService approvalForumAuthService;
    private final ApprovalForumDocumentsService approvalForumDocumentService;
    private final ApprovalForumExternalService approvalForumExternalService;
    private final ApprovalForumListManagementService approvalForumListManagementService;
    private final LumosRestService lumosRestService;
    private Map<String, String> afSettings;
    private WebTarget docGenTarget;

    @Inject
    public ApprovalForumServiceImpl(ApprovalForumDataRepository approvalForumDataRepository,
                                    ApprovalForumDataAuditRepository approvalForumDataAuditRepository,
                                    ApprovalForumRulesService approvalForumRulesService,
                                    ApprovalForumMailService approvalForumMailService,
                                    ApprovalForumAuthService approvalForumAuthService,
                                    ApprovalForumDocumentsService approvalForumDocumentService,
                                    ApprovalForumExternalService approvalForumExternalService,
                                    ApprovalForumListManagementService approvalForumListManagementService,
                                    LumosRestService lumosRestService,
                                    @Named("af_settings") Map<String, String> afSettings, @Named("docgen") WebTarget docGenTarget) {
        this.approvalForumDataRepository = approvalForumDataRepository;
        this.approvalForumDataAuditRepository = approvalForumDataAuditRepository;
        this.approvalForumRulesService = approvalForumRulesService;
        this.approvalForumMailService = approvalForumMailService;
        this.approvalForumAuthService = approvalForumAuthService;
        this.approvalForumDocumentService = approvalForumDocumentService;
        this.approvalForumExternalService = approvalForumExternalService;
        this.approvalForumListManagementService = approvalForumListManagementService;
        this.lumosRestService = lumosRestService;
        this.afSettings = afSettings;
        this.docGenTarget = docGenTarget;
    }

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public int createRequest(Boolean isExternal, RequestData requestData, String user) throws ResourceException {
        logger.info("Saving the workflow. Is this a Lumos request? : " + requestData.isIsLumos()+", user:"+user);
        ApprovalForumWfWorkflowEntity workflowEntity = persistRequest(isExternal, requestData, user);

        if(workflowEntity.getWorkflowSystemName().equalsIgnoreCase("Ashburton")){
            logger.info("Saving the workflow. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
            if(requestData.getCounterpartyFullDetail().getAsbDetails() != null
                    && requestData.getCounterpartyFullDetail().getAsbDetails().getWfAppId() ==
                    Integer.valueOf(requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId())){
                logger.info("ASBLimits is already existing for applicationId: " + requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
            } else {
                logger.info("Saving the asbdetails. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
                persistAsbFundLimits(requestData);
            }
        }

        if (requestData.getRulesResponse() != null) {
            persistWipRulesResponseData(workflowEntity, requestData.getRulesResponse());
        }
        if (requestData.getUsersResponse() != null) {
            persistWipUsersResponseData(workflowEntity, requestData.getUsersResponse());
        }
        logger.info("persistCounterpartyFullData");
        persistCounterpartyFullData(workflowEntity, requestData.getCounterpartyFullDetail(), requestData.isIsLumos());
        logger.info("persistWorkflowData");
        persistWorkflowData(workflowEntity, requestData.getRecommendation(), requestData.getChairRecommendation());
        logger.info("persistWorkflowData11");
        return workflowEntity.getWfId();
    }

    @Override
    public int cancelRequest(int workflowId, String userName) throws ResourceException {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        if (!"wip".equalsIgnoreCase(workflowEntity.getStatus())) {
            throw new ResourceException("Workflow not in WIP status");
        }
        if (!workflowEntity.isActive()) {
            throw new ResourceException("Workflow not active, cannot cancel");
        }
        workflowEntity.setStatus(WorkflowStatus.CANCELLED.value());
        workflowEntity.setActive(false);
        workflowEntity.setCompletedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        approvalForumDataRepository.updateWorkflow(workflowEntity);
        setAuditAndLastUpdatedDetails(userName, "Workflow Cancelled", workflowEntity.getWfId());
        notifyFrontEndUser(workflowId);
        return workflowEntity.getWfId();
    }

    @Override
    public int updateRequest(int workflowId, RequestData requestData, String user) throws ResourceException {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        logger.info("updateRequest is this is workflowEntity? : " + workflowEntity.getWfId()+" systemname:"+workflowEntity.getWorkflowSystemName());
        if (workflowEntity == null) {
            throw new ResourceException("Workflow with id " + workflowId + " not found");
        }
        if (requestData.isSpecific() && requestData.isConfidential()) {
            throw new ResourceException("Request can't be marked and specific and confidential");
        }

        logger.info("updateRequest is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
        if(workflowEntity.getWorkflowSystemName().equalsIgnoreCase("Ashburton")){
            logger.info("updateRequest is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
            if(isASBLimitFundsPersisted(Integer.valueOf(requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId()))){
                logger.info("ASBLimits is already existing for applicationId: " + requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
            } else {
                logger.info("updateRequest the asbdetails. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
                persistAsbFundLimits(requestData);
            }
        }

        if (workflowEntity.isConfidential() != requestData.isConfidential()
                || workflowEntity.isSpecific() != requestData.isSpecific()
                || (workflowEntity.getSpecificReason() == null && requestData.getSpecificReason() != null
                || workflowEntity.getSpecificReason() != null && requestData.getSpecificReason() == null
                || (workflowEntity.getSpecificReason() != null && requestData.getSpecificReason() != null
                && !workflowEntity.getSpecificReason().equals(requestData.getSpecificReason())))) {
            workflowEntity.setConfidential(requestData.isConfidential());
            workflowEntity.setSpecific(requestData.isSpecific());
            workflowEntity.setSpecificReason(requestData.getSpecificReason());
            approvalForumDataRepository.updateWorkflow(workflowEntity);
        }
        if (requestData.getRulesResponse() != null) {
            updateWipRulesResponseData(workflowEntity, requestData.getRulesResponse());
        }
        if (requestData.getUsersResponse() != null) {
            updateWipUsersResponseData(workflowEntity, requestData.getUsersResponse());
        }

        updateWorkflowData(workflowEntity, requestData.getRecommendation(), requestData.getChairRecommendation());
        setAuditAndLastUpdatedDetails(user, "Workflow Update", workflowEntity.getWfId());
        return workflowEntity.getWfId();
    }

    @Override
    public int submitRequest(int workflowId, RequestData requestData, String user) throws ResourceException {
        ApprovalForumWfWorkflowEntity workflowEntity;
        logger.info("submitRequest the workflow. tenantId : " + requestData.getTenantId());
        if (workflowId == 0) {
            workflowEntity = approvalForumDataRepository.getWorkflow(createRequest(false, requestData, user));
        } else {
            workflowEntity = approvalForumDataRepository.getWorkflow(updateRequest(workflowId, requestData, user));
        }

        if(workflowEntity.getWorkflowSystemName().equalsIgnoreCase("Ashburton")){
            logger.info("submitRequest the workflow. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
            if(isASBLimitFundsPersisted(Integer.valueOf(requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId()))){
                logger.info("submitRequest ASBLimits is already existing for applicationId: " + requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
            } else {
                logger.info("submitRequest Saving the asbdetails. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
                persistAsbFundLimits(requestData);
            }
        }

        workflowEntity.setSubmittedBy(user);
        workflowEntity.setSubmittedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        workflowEntity.setStatus(WorkflowStatus.AWAITING_APPROVAL.value());

        approvalForumDataRepository.updateWorkflow(workflowEntity);
        List<String> specificApprovers = new ArrayList<>();
        if (requestData.getRulesResponse() != null) {
            Map<String, ApprovalForumAflmmApprovalForumListManagementMappingEntity> mappingEntityMap = getListManagementMappingsAsMap();
            if (requestData.getRulesResponse().getModelData() != null
                    && !requestData.getRulesResponse().getModelData().isEmpty()) {
                requestData.getRulesResponse().getModelData().forEach(rulesModelData -> {
                    persistQuorum(workflowEntity, rulesModelData, mappingEntityMap);
                });
            }
            if (requestData.getRulesResponse().getListManagementList() != null
                    && !requestData.getRulesResponse().getListManagementList().isEmpty()) {
                requestData.getRulesResponse().getListManagementList().forEach(listManagementList -> {
                    if(!listManagementList.isChairpersonList()){
                        persistRulesResponseListModelDataUsers(workflowEntity, listManagementList, mappingEntityMap,
                                specificApprovers);
                    }
                });
            }
            // If request is marked as confidential or specific, list of assigned users
            // needs to meet quorum
            if ((requestData.isConfidential() || requestData.isSpecific())
                    && !hasQuorumBeenReachedForAssignment(workflowEntity.getWfId())) {
                if (requestData.isConfidential()) {
                    throw new RuntimeException("Quorum details not met on request which is marked as confidential");
                } else {
                    throw new RuntimeException("Quorum details not met on request which is marked as specific");
                }
            }
        }
        if (requestData.getUsersResponse() != null) {
            persistListManagementUsers(workflowEntity, requestData.getUsersResponse().getUsers(), null, true,
                    specificApprovers, false);
        }

        int totalNumberOfApproversRequired = requestData.getRulesResponse().getModelData().get(0).getTotalNumberOfApprovalsRequired();

        if(totalNumberOfApproversRequired != 0) {//da4 is 0
            logger.info("submitRequest the workflow. totalNumberOfApproversRequired != 0 : " + totalNumberOfApproversRequired);
            approvalForumMailService.sendRequestSubmitMail(workflowEntity.getWfId());
            setAuditAndLastUpdatedDetails(user, "Workflow Submit", workflowEntity.getWfId());
        }else{
            logger.info("submitRequest the workflow. totalNumberOfApproversRequired? : " + totalNumberOfApproversRequired);
            finaliseWorkflow(workflowEntity, ApprovalDecision.APPROVED, null);
            setAuditAndLastUpdatedDetails(user, "Workflow Approved", workflowEntity.getWfId());
        }

        return workflowEntity.getWfId();
    }

    @Override
    public int submitRequestToChairperson(int workflowId, RequestData requestData, String user) throws ResourceException {
        logger.info("submitRequestToChairperson the workflow workflowId : " + workflowId+" requestData:"+requestData.getTenantId());
        ApprovalForumWfWorkflowEntity workflowEntity;
        if (workflowId == 0) {
            workflowEntity = approvalForumDataRepository.getWorkflow(createRequest(false, requestData, user));
        } else {
            workflowEntity = approvalForumDataRepository.getWorkflow(updateRequest(workflowId, requestData, user));
        }

        if(workflowEntity.getWorkflowSystemName().equalsIgnoreCase("Ashburton")){
            logger.info("submitRequestToChairperson the workflow. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
            if(isASBLimitFundsPersisted(Integer.valueOf(requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId()))){
                logger.info("submitRequestToChairperson ASBLimits is already existing for applicationId: " + requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
            } else {
                logger.info("submitRequestToChairperson Saving the asbdetails. is this is ashburton workflow? : " + workflowEntity.getWorkflowSystemName());
                persistAsbFundLimits(requestData);
            }
        }


        workflowEntity.setSubmittedBy(user);
        workflowEntity.setSubmittedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        workflowEntity.setStatus(AWAITING_CHAIRPERSON_APPROVAL.value());
        logger.info("submitRequestToChairperson the workflow workflowId : " + workflowId+" workflowsystemname:"+workflowEntity.getWorkflowSystemName());
        approvalForumDataRepository.updateWorkflow(workflowEntity);
        List<String> specificApprovers = new ArrayList<>();
        List<ListManagementList> chairpersonLists = new ArrayList<>();
        if (requestData.getRulesResponse() != null) {
            Map<String, ApprovalForumAflmmApprovalForumListManagementMappingEntity> mappingEntityMap = getListManagementMappingsAsMap();

            if (requestData.getRulesResponse().getListManagementList() != null
                    && !requestData.getRulesResponse().getListManagementList().isEmpty()) {
                requestData.getRulesResponse().getListManagementList().forEach(listManagementList -> {
                    if(listManagementList.isChairpersonList()){
                        persistRulesResponseListModelDataUsers(workflowEntity, listManagementList, mappingEntityMap,
                                specificApprovers);
                        chairpersonLists.add(listManagementList);
                    }
                });
            }

        }

        approvalForumMailService.sendRequestSubmitToChairpersonMail(workflowEntity.getWfId(), chairpersonLists);
        setAuditAndLastUpdatedDetails(user, "Workflow Submit to Chairperson", workflowEntity.getWfId());
        return workflowEntity.getWfId();
    }

    @Override
    public int createMemo(CreateMemoDTO requestDTO, String user) {
        return 0;
    }

    @Override
    public RulesResponse processRules(RulesRequestDTO requestDTO) throws ResourceException {
        ApprovalForumRulesResponseDTO responseDTO;
        try {
            responseDTO = approvalForumRulesService.executeApprovalForumRules(requestDTO.getMeetingRule(),
                    requestDTO.getDistressStatus(), requestDTO.getFinancialInstitutionType(),
                    requestDTO.isBank(), requestDTO.getRiskTakerGroupCode(), mapFacilityMovements(requestDTO), requestDTO.getProposalType());
            if (!responseDTO.isValid()) {
                throw new ResourceException("Rules service return not valid response");
            }
        } catch (RulesException e) {
            throw new ResourceException(e.getMessage());
        }
        return mapRulesResponse(responseDTO);
    }

    private RulesResponse mapRulesResponse(ApprovalForumRulesResponseDTO responseDTO) {
        RulesResponse rulesResponse = new RulesResponse().withModelData(new ArrayList<>())
                .withListManagementList(new ArrayList<>());
        Map<String, ListManagementList> listManagementListMap = new HashMap<>();
        boolean selected = true;
        for (ApprovalForumRulesModel rulesModel : responseDTO.getResponseData().getModelData()) {
            RulesModelData modelData = new RulesModelData().withListModelData(new ArrayList<>());
            modelData.setTotalNumberOfApprovalsRequired(rulesModel.getTotalNumberOfApprovalsRequired());
            modelData.setSelected(selected);
            List<RulesResponseListModelData> listModelDataList = new ArrayList<>();
            for (ListModelData listModelData : rulesModel.getListModelData()) {
                RulesResponseListModelData rulesResponseListModelData = new RulesResponseListModelData();
                rulesResponseListModelData.setListName(listModelData.getListName());
                rulesResponseListModelData.setMinRequired(listModelData.getMinRequired());
                rulesResponseListModelData.setMaxRequired(listModelData.getMaxRequired());
                rulesResponseListModelData.setRank(listModelData.getRank());
                if (!listManagementListMap.containsKey(listModelData.getListName())) {
                    listManagementListMap.put(listModelData.getListName(), approvalForumListManagementService
                            .getListManagementListByName(listModelData.getListName()));
                }
                listModelDataList.add(rulesResponseListModelData);
            }
            modelData.setListModelData(listModelDataList);
            rulesResponse.getModelData().add(modelData);
            selected = false;
        }
        if (!listManagementListMap.isEmpty())
            rulesResponse.setListManagementList(new ArrayList<>(listManagementListMap.values()));
        return rulesResponse;
    }

    private List<za.co.rmb.approvalforumrules.model.FacilityMovementDTO> mapFacilityMovements(RulesRequestDTO requestDTO) {
        List<za.co.rmb.approvalforumrules.model.FacilityMovementDTO> facilityMovements = new ArrayList<>();

        for (FacilityMovementDTO facilityMovement : requestDTO.getFacilityMovement()) {
            za.co.rmb.approvalforumrules.model.FacilityMovementDTO facilityMovementDTO = new za.co.rmb.approvalforumrules.model.FacilityMovementDTO();
            facilityMovementDTO.setMovement(facilityMovement.getMovement());
            facilityMovementDTO.setRiskTakerGroupCode(facilityMovement.getRiskTakerGroupCode());
            facilityMovements.add(facilityMovementDTO);
        }

        return facilityMovements;
    }

    private ApprovalForumWfWorkflowEntity persistRequest(Boolean isExternal, RequestData requestData, String user)
            throws ResourceException {
        logger.info("persistRequest(START)");
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        ApprovalForumWfWorkflowEntity workflowEntity = new ApprovalForumWfWorkflowEntity();
        workflowEntity.setActive(true);
        workflowEntity.setCreatedBy(user);
        if (isExternal == null || !isExternal) {
            workflowEntity.setAssignedTo(user);
            workflowEntity.setAssignedOn(now);
        }
        workflowEntity.setCreatedOn(now);
        if (requestData.isSpecific() && requestData.isConfidential()) {
            throw new ResourceException("Request can't be marked and specific and confidential");
        }
        workflowEntity.setConfidential(requestData.isConfidential());
        workflowEntity.setSpecific(requestData.isSpecific());
        workflowEntity.setSpecificReason(requestData.getSpecificReason());
        workflowEntity.setChairpersonApprovalRequired(requestData.isChairpersonApprovalRequired());
        workflowEntity.setRtRequestTypeByRtId(
                approvalForumDataRepository.getApprovalForumRequestTypeByType(RequestType.CREDIT_APPLICATION.value()));
        workflowEntity.setStatus(WorkflowStatus.WIP.value());
        workflowEntity.setFirstEscalationSent(false);
        workflowEntity.setSecondEscalationSent(false);
        logger.info("persistRequest:getTenantId::"+requestData.getTenantId());
        if (requestData.isIsLumos()) {
            workflowEntity.setWorkflowSystemName("LUMOS");
        }  else if(requestData.getTenantId() == 2){
            logger.info("persistRequest:getTenantId2::"+requestData.getTenantId());
            workflowEntity.setWorkflowSystemName("ASHBURTON");
        }
        else {
            workflowEntity.setWorkflowSystemName("CASDM");
        }
        if (requestData.getReferencedWorkflowId() != null && requestData.getReferencedWorkflowId() > 0) {
            workflowEntity.setReferencedWorkfowId(requestData.getReferencedWorkflowId());
        }
        approvalForumDataRepository.persistWorkflow(workflowEntity);
        setAuditAndLastUpdatedDetails(user, "Workflow Create", workflowEntity.getWfId());
        logger.info("persistRequest(END)");
        return workflowEntity;
    }

    //Ashburton changes
    private void persistAsbFundLimits(RequestData requestData) throws ResourceException{
        logger.info("persistAsbFundLimits(START)");
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        ApprovalForumWfflWorkflowFundLimitInformationEntity workflowEntity = new ApprovalForumWfflWorkflowFundLimitInformationEntity();
        workflowEntity.setCreatedOn(now);
        ASBLimitFundsDTO dto = (ASBLimitFundsDTO)requestData.getCounterpartyFullDetail().getAsbDetails();
        workflowEntity.setWffappId(requestData.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
        workflowEntity.setCptyLimit(dto.getLimits().getProposedCPYLZar());
        workflowEntity.setConvertedEquityLimit(dto.getLimits().getProposedConvertedEZar());
        workflowEntity.setJuniorDebtLimit(dto.getLimits().getProposedJuniorDZar());
        workflowEntity.setGovernmentGuaranteeLimit(dto.getLimits().getProposedGovermentGZar());
        workflowEntity.setLiquidationLimit(dto.getLimits().getProposedLiquidationZar());
        workflowEntity.setMezzanineDebtLimit(dto.getLimits().getProposedMezzanineDZar());
        workflowEntity.setPreferredSharesLimit(dto.getLimits().getProposedPreferredSZar());
        workflowEntity.setSeniorSecuredLimit(dto.getLimits().getProposedSeniorSZar());
        workflowEntity.setSeniorUnsecuredLimit(dto.getLimits().getProposedSeniorUZar());
        workflowEntity.setSubordinatedSecuredLimit(dto.getLimits().getProposedSubordinatedSZar());
        workflowEntity.setSubordinatedUnsecuredLimit(dto.getLimits().getProposedSubordinatedUZar());
        workflowEntity.setCreditFund(dto.getFunds().getCreditPercentage());
        workflowEntity.setLdiFund(dto.getFunds().getLdiPercentage());
        workflowEntity.setMultiAssetFund(dto.getFunds().getMultiAssetPercentage());
        workflowEntity.setMultiManagerFund(dto.getFunds().getMultiManagerPercentage());
        workflowEntity.setFixedIncomeFund(dto.getFunds().getFixedIncomePercentage());
        approvalForumDataRepository.persistASBLimitFundsWorkflow(workflowEntity);
        logger.info("persistAsbFundLimits(END)");
    }
    private void persistQuorum(ApprovalForumWfWorkflowEntity workflowEntity, RulesModelData rulesModelData,
                               Map<String, ApprovalForumAflmmApprovalForumListManagementMappingEntity> mappingEntityMap) {
        ApprovalForumWfqWorkflowQuorumEntity quorumEntity = new ApprovalForumWfqWorkflowQuorumEntity();
        quorumEntity.setTotalNumberOfApprovalsRequired(rulesModelData.getTotalNumberOfApprovalsRequired());
        quorumEntity.setSelected(rulesModelData.isSelected());
        quorumEntity.setWfApprovalForumWorkflowByWfId(workflowEntity);
        approvalForumDataRepository.persistQuorumEntity(quorumEntity);
        rulesModelData.getListModelData().forEach(rulesResponseListModelData -> {
            ApprovalForumWfqdWorkflowQuorumDataEntity quorumDataEntity = new ApprovalForumWfqdWorkflowQuorumDataEntity();
            logger.info("persistQuorum:meeting name::"+rulesResponseListModelData.getListName());
            logger.info("persistQuorum:mapping entity map::"+mappingEntityMap.get(rulesResponseListModelData.getListName()));
            quorumDataEntity.setAflmmApprovalForumListManagementMappingByAflmmId(
                    mappingEntityMap.get(rulesResponseListModelData.getListName().trim()));
            quorumDataEntity.setMinRequired(rulesResponseListModelData.getMinRequired());
            quorumDataEntity.setMaxRequired(rulesResponseListModelData.getMaxRequired());
            quorumDataEntity.setRank(rulesResponseListModelData.getRank());
            quorumDataEntity.setWfqApprovalForumWorkflowQuorumByWfqId(quorumEntity);
            approvalForumDataRepository.persistQuorumDataEntity(quorumDataEntity);
        });
    }

    private void persistRulesResponseListModelDataUsers(ApprovalForumWfWorkflowEntity workflowEntity,
                                                        ListManagementList listManagementList,
                                                        Map<String, ApprovalForumAflmmApprovalForumListManagementMappingEntity> mappingEntityMap,
                                                        List<String> specificApprovers) {
        persistListManagementUsers(workflowEntity, listManagementList.getUsers(),
                mappingEntityMap.get(listManagementList.getListManagementName()), false, specificApprovers, listManagementList.isChairpersonList());
    }

    private void persistListManagementUsers(ApprovalForumWfWorkflowEntity workflowEntity,
                                            List<ListManagementListUser> listManagementListUsers,
                                            ApprovalForumAflmmApprovalForumListManagementMappingEntity mappingEntity, boolean isAdditionalApprover,
                                            List<String> specificApprovers, boolean isChairpersonApprover) {
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        for (ListManagementListUser listUser : listManagementListUsers) {
            if (isAdditionalApprover) {
                if (listUser.isAssigned()) {
                    if (specificApprovers.indexOf(listUser.getUserId()) >= 0) {
                        throw new RuntimeException("User can not be a specific, and an additional approver");
                    }
                    persistListManagementApprovalUser(workflowEntity, listUser, now, mappingEntity,
                            isAdditionalApprover, isChairpersonApprover);
                }
            } else {
                if (specificApprovers.indexOf(listUser.getUserId()) < 0 && listUser.isAssigned())
                    specificApprovers.add(listUser.getUserId());
                persistListManagementApprovalUser(workflowEntity, listUser, now, mappingEntity, isAdditionalApprover, isChairpersonApprover);
            }
        }
    }

    private void persistWipRulesResponseData(ApprovalForumWfWorkflowEntity workflowEntity,
                                             RulesResponse rulesResponse) {
        ApprovalForumWiprrWipRulesResponseEntity rulesResponseEntity = new ApprovalForumWiprrWipRulesResponseEntity();
        rulesResponseEntity.setWfApprovalForumWorkflowByWfId(workflowEntity);
        rulesResponseEntity.setRulesResponseData(convertObjectToByteArray(rulesResponse));
        approvalForumDataRepository.persistRulesResponseEntity(rulesResponseEntity);
    }

    private void updateWipRulesResponseData(ApprovalForumWfWorkflowEntity workflowEntity, RulesResponse rulesResponse) {
        ApprovalForumWiprrWipRulesResponseEntity rulesResponseEntity = workflowEntity
                .getWiprrWipRulesResponseByWiprrId();
        if (rulesResponseEntity == null) {
            persistWipRulesResponseData(workflowEntity, rulesResponse);
        } else {
            rulesResponseEntity.setRulesResponseData(convertObjectToByteArray(rulesResponse));
            approvalForumDataRepository.updateRulesResponseEntity(rulesResponseEntity);
        }
    }

    private void persistWipUsersResponseData(ApprovalForumWfWorkflowEntity workflowEntity,
                                             UsersResponse usersResponse) {
        ApprovalForumWipurWipUsersResponseEntity usersResponseEntity = new ApprovalForumWipurWipUsersResponseEntity();
        usersResponseEntity.setWfApprovalForumWorkflowByWfId(workflowEntity);
        usersResponseEntity.setUsersResponseData(convertObjectToByteArray(usersResponse));
        approvalForumDataRepository.persistUsersResponseEntity(usersResponseEntity);
    }

    private void updateWipUsersResponseData(ApprovalForumWfWorkflowEntity workflowEntity, UsersResponse usersResponse) {
        ApprovalForumWipurWipUsersResponseEntity usersResponseEntity = workflowEntity
                .getWipurWipUsersResponseByWipurId();
        if (usersResponse == null) {
            persistWipUsersResponseData(workflowEntity, usersResponse);
        } else {
            usersResponseEntity.setUsersResponseData(convertObjectToByteArray(usersResponse));
            approvalForumDataRepository.updateUsersResponseEntity(usersResponseEntity);
        }
    }

    private byte[] convertObjectToByteArray(Object o) {
        byte[] byteArrayObject = null;
        try {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);

            oos.close();
            bos.close();
            byteArrayObject = bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return byteArrayObject;
        }
        return byteArrayObject;
    }

    private Object convertByteArrayToObject(byte[] convertObject) {
        Object o = null;

        ByteArrayInputStream bais;
        ObjectInputStream ins;
        try {

            bais = new ByteArrayInputStream(convertObject);

            ins = new ObjectInputStream(bais);
            o = ins.readObject();

            ins.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return o;
    }

    private void persistCounterpartyFullData(ApprovalForumWfWorkflowEntity workflowEntity,
                                             CounterpartyFullDetail counterpartyFullDetail, boolean isIsLumos) {
        logger.info("persistCounterpartyFullData(START)");
        ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity = persistCounterpartyData(
                workflowEntity, counterpartyFullDetail.getCounterpartyDetail());
        if("Ashburton".equalsIgnoreCase(workflowEntity.getWorkflowSystemName())) {
            persistAshburtonApplication(counterpartyDetailEntity, counterpartyFullDetail.getCreditApplication());
        } else {
            persistApplication(counterpartyDetailEntity, counterpartyFullDetail.getCreditApplication(), isIsLumos);
        }
        if (counterpartyFullDetail.getFacility() != null && !counterpartyFullDetail.getFacility().isEmpty()) {
            counterpartyFullDetail.getFacility()
                    .forEach(facilityModel -> persistFacility(counterpartyDetailEntity, facilityModel));
        }
        logger.info("persistCounterpartyFullData(END)");
    }

    private void persistWorkflowData(ApprovalForumWfWorkflowEntity workflowEntity, String recommendation, String chairRecommendation) {
        logger.info("persistWorkflowData(START)");
        if (recommendation == null)
            return;
        ApprovalForumWfdaWorkflowDataEntity workflowDataEntity = new ApprovalForumWfdaWorkflowDataEntity();
        workflowDataEntity.setWfApprovalForumWorkflowByWfId(workflowEntity);
        workflowDataEntity.setRecommendation(recommendation);
        workflowDataEntity.setChairRecommendation(chairRecommendation);

        approvalForumDataRepository.persistWorkflowData(workflowDataEntity);
        logger.info("persistWorkflowData(END)");
    }

    private void updateWorkflowData(ApprovalForumWfWorkflowEntity workflowEntity, String recommendation, String chairRecommendation) {
        ApprovalForumWfdaWorkflowDataEntity workflowDataEntity = workflowEntity.getWfdaWorkflowDataByWfdaId();
        if (workflowDataEntity == null) {
            persistWorkflowData(workflowEntity, recommendation, chairRecommendation);
        } else {
            if (recommendation == null && chairRecommendation == null) {
                approvalForumDataRepository.deleteWorkflowData(workflowDataEntity);
            } else {
                if(recommendation == null){
                    workflowDataEntity.setRecommendation("");
                } else {
                    workflowDataEntity.setRecommendation(recommendation);
                }

                workflowDataEntity.setChairRecommendation(chairRecommendation);
                approvalForumDataRepository.updateWorkflowData(workflowDataEntity);
            }
        }
    }

    private ApprovalForumWfcdWorkflowCounterpartyDetailEntity persistCounterpartyData(
            ApprovalForumWfWorkflowEntity workflowEntity, CounterpartyModel counterpartyModel) {
        ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity = new ApprovalForumWfcdWorkflowCounterpartyDetailEntity();
        counterpartyDetailEntity.setCounterpartyName(counterpartyModel.getCounterpartyName());
        counterpartyDetailEntity.setCreditInkombaId(counterpartyModel.getCreditInkombaId());
        counterpartyDetailEntity.setCreditAnalyst(counterpartyModel.getCreditAnalyst());
        counterpartyDetailEntity.setCreditAnalystId(counterpartyModel.getCreditAnalystId());
        counterpartyDetailEntity.setCreditAnalystEmail(counterpartyModel.getCreditAnalystMail());
        counterpartyDetailEntity.setWfApprovalForumWorkflowByWfId(workflowEntity);
        counterpartyDetailEntity.setBank(counterpartyModel.isBank());
        counterpartyDetailEntity.setProspect(counterpartyModel.isProspect());
        counterpartyDetailEntity.setFinancialInstitutionType(counterpartyModel.getFinancialInstitutionType());
        approvalForumDataRepository.persistWorkflowCounterpartyDetail(counterpartyDetailEntity);
        return counterpartyDetailEntity;
    }

    private void persistApplication(ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity,
                                    CreditApplicationModel applicationModel, boolean isIsLumos) {
        ApprovalForumWfadWorkflowApplicationDataEntity applicationDataEntity = new ApprovalForumWfadWorkflowApplicationDataEntity();
        applicationDataEntity.setLastApprovedFrRating(applicationModel.getLastApprovedFrRating());
        applicationDataEntity.setProposedFrRating(applicationModel.getProposedFrRating());
        applicationDataEntity.setLastApprovedDcp(applicationModel.getLastApprovedDcp());
        applicationDataEntity.setLastApprovedSettlementDcp(applicationModel.getLastApprovedSettlementDcp());
        applicationDataEntity.setProposedDcp(applicationModel.getProposedDcp());
        applicationDataEntity.setProposedSettlementDcp(applicationModel.getProposedSettlementDcp());
        applicationDataEntity.setDistressStatus(applicationModel.getDistressStatus());
        applicationDataEntity.setWholesaleCreditPortfolio(applicationModel.getWholesaleCreditPortfolio());
        applicationDataEntity.setMeetingName(applicationModel.getMeetingName());
        applicationDataEntity.setProposalType(applicationModel.getProposalType());
        applicationDataEntity.setProposalAction(applicationModel.getProposalAction());
        applicationDataEntity.setReviewAnalyst(applicationModel.getReviewAnalyst());
        applicationDataEntity.setReviewAnalystEmail(applicationModel.getReviewAnalystEmail());
        applicationDataEntity.setReviewAnalystId(applicationModel.getReviewAnalystId());
        applicationDataEntity.setReviewAnalystSignatureDate(applicationModel.getReviewAnalystSignatureDate() != null ?
                Timestamp.valueOf(applicationModel.getReviewAnalystSignatureDate()) : null);
        applicationDataEntity.setCreditAnalyst(applicationModel.getCreditAnalyst());
        applicationDataEntity.setCreditAnalystEmail(applicationModel.getCreditAnalystEmail());
        applicationDataEntity.setCreditAnalystId(applicationModel.getCreditAnalystId());
        applicationDataEntity.setCreditAnalystSignatureDate(applicationModel.getCreditAnalystSignatureDate() != null ?
                Timestamp.valueOf(applicationModel.getCreditAnalystSignatureDate()) : null);
        applicationDataEntity.setWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId(counterpartyDetailEntity);
        applicationDataEntity.setApplicationId(applicationModel.getApplicationId());
        applicationDataEntity.setLumos(isIsLumos);
        approvalForumDataRepository.persistWorkflowApplicationData(applicationDataEntity);
    }

    private void persistAshburtonApplication(ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity,
                                             CreditApplicationModel applicationModel) {
        logger.info("persistAshburtonApplication(START)");
        ApprovalForumWfadWorkflowApplicationDataEntity applicationDataEntity = new ApprovalForumWfadWorkflowApplicationDataEntity();
        applicationDataEntity.setLastApprovedFrRating(applicationModel.getLastApprovedFrRating());
        applicationDataEntity.setProposedFrRating(applicationModel.getProposedFrRating());
        applicationDataEntity.setLastApprovedDcp(applicationModel.getLastApprovedDcp());
        applicationDataEntity.setLastApprovedSettlementDcp(applicationModel.getLastApprovedSettlementDcp());
        applicationDataEntity.setProposedDcp(applicationModel.getProposedDcp());
        applicationDataEntity.setProposedSettlementDcp(applicationModel.getProposedSettlementDcp());
        applicationDataEntity.setDistressStatus(applicationModel.getDistressStatus());
        applicationDataEntity.setWholesaleCreditPortfolio(applicationModel.getWholesaleCreditPortfolio());
        applicationDataEntity.setMeetingName(applicationModel.getMeetingName());
        applicationDataEntity.setProposalType(applicationModel.getProposalType());
        applicationDataEntity.setProposalAction(applicationModel.getProposalAction());
        applicationDataEntity.setReviewAnalyst(applicationModel.getReviewAnalyst());
        applicationDataEntity.setReviewAnalystEmail(applicationModel.getReviewAnalystEmail());
        applicationDataEntity.setReviewAnalystId(applicationModel.getReviewAnalystId());
        applicationDataEntity.setReviewAnalystSignatureDate(applicationModel.getReviewAnalystSignatureDate() != null ?
                Timestamp.valueOf(applicationModel.getReviewAnalystSignatureDate()) : null);
        applicationDataEntity.setCreditAnalyst(applicationModel.getCreditAnalyst());
        applicationDataEntity.setCreditAnalystEmail(applicationModel.getCreditAnalystEmail());
        applicationDataEntity.setCreditAnalystId(applicationModel.getCreditAnalystId());
        applicationDataEntity.setCreditAnalystSignatureDate(applicationModel.getCreditAnalystSignatureDate() != null ?
                Timestamp.valueOf(applicationModel.getCreditAnalystSignatureDate()) : null);
        applicationDataEntity.setWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId(counterpartyDetailEntity);
        applicationDataEntity.setApplicationId(applicationModel.getApplicationId());
        applicationDataEntity.setLumos(false);

        approvalForumDataRepository.persistWorkflowApplicationData(applicationDataEntity);
        logger.info("persistAshburtonApplication(END)");
    }
    private void persistFacility(ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity,
                                 FacilityModel facilityModel) {
        ApprovalForumWffiWorkflowFacilityInformationEntity facilityInformationEntity = new ApprovalForumWffiWorkflowFacilityInformationEntity();
        facilityInformationEntity.setFacilityReference(facilityModel.getFacilityReference());
        facilityInformationEntity.setRiskType(facilityModel.getRiskType());
        facilityInformationEntity.setTenor(facilityModel.getTenor());
        facilityInformationEntity.setCurrency(facilityModel.getCurrency());
        facilityInformationEntity.setNetLimit(facilityModel.getNetLimit());
        facilityInformationEntity.setNetLimitZar(facilityModel.getNetLimitZar());
        facilityInformationEntity.setFacilityDescription(facilityModel.getFacilityDescription());
        facilityInformationEntity.setCreditApplicationStatus(facilityModel.getCreditApplicationStatus());
        facilityInformationEntity.setRiskTaker(facilityModel.getRiskTaker());
        facilityInformationEntity.setDealSpecific(facilityModel.isDealSpecific());
        facilityInformationEntity.setProposedRating(facilityModel.getProposedRating());
        facilityInformationEntity.setLgd(facilityModel.getProposedLgd());
        facilityInformationEntity.setLgdZar(facilityModel.getProposedLgdZar());
        facilityInformationEntity.setFacilityId(facilityModel.getFacilityId());
        facilityInformationEntity.setFacilityRating(facilityModel.getFacilityRating());
        facilityInformationEntity.setWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId(counterpartyDetailEntity);
        facilityInformationEntity.setFacilityInvestmentType(facilityModel.getFacilityInvestmentType());
        facilityInformationEntity.setFacilityMovementType(facilityModel.getFacilityMovementType());
        facilityInformationEntity.setRiskTakerGroupCode(facilityModel.getRiskTakerGroupCode());
        approvalForumDataRepository.persistWorkflowFacilityInformation(facilityInformationEntity);
    }

    private ApprovalForumWfaWorkflowApprovalsEntity mapListManagementApprovalUserEntity(
            ApprovalForumWfWorkflowEntity workflowEntity, ListManagementListUser listManagementListUser,
            java.sql.Timestamp now, boolean isAdditionalApprover, boolean isChairpersonApprover) {
        ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity = new ApprovalForumWfaWorkflowApprovalsEntity();
        approvalsEntity.setWfApprovalForumWorkflowByWfId(workflowEntity);
        approvalsEntity.setUserId(listManagementListUser.getUserId());
        approvalsEntity.setName(listManagementListUser.getName());
        approvalsEntity.setSurname(listManagementListUser.getSurname());
        approvalsEntity.setEmail(listManagementListUser.getEmail());
        approvalsEntity.setTitle(listManagementListUser.getTitle());
        approvalsEntity.setDomain(listManagementListUser.getDomain());
        if (listManagementListUser.isAssigned()) {
            approvalsEntity.setDateAssigned(now);
            if (!isAdditionalApprover)
                approvalsEntity.setSpecificApprover(true);
        }
        approvalsEntity.setAdditionalApprover(isAdditionalApprover);
        approvalsEntity.setOnlyForDistribution(listManagementListUser.isDistribution());
        approvalsEntity.setChairpersonApprover(isChairpersonApprover);
        return approvalsEntity;
    }

    private void persistListManagementApprovalUser(ApprovalForumWfWorkflowEntity workflowEntity,
                                                   ListManagementListUser listManagementListUser, java.sql.Timestamp now,
                                                   ApprovalForumAflmmApprovalForumListManagementMappingEntity mappingEntity, boolean isAdditionalApprover, boolean isChaipersonApprover) {
        ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity = mapListManagementApprovalUserEntity(workflowEntity,
                listManagementListUser, now, isAdditionalApprover, isChaipersonApprover);
        if (mappingEntity != null) {
            approvalsEntity.setAflmmApprovalForumListManagementMappingByAflmmId(mappingEntity);
        }
        approvalForumDataRepository.persistWorkflowApprovals(approvalsEntity);
    }

    private Map<String, ApprovalForumAflmmApprovalForumListManagementMappingEntity> getListManagementMappingsAsMap() {
        Map<String, ApprovalForumAflmmApprovalForumListManagementMappingEntity> mappingEntityMap = new HashMap<>();
        List<ApprovalForumAflmmApprovalForumListManagementMappingEntity> mappingEntities = approvalForumDataRepository
                .getAllListManagementMappings();
        mappingEntities.forEach(entity -> mappingEntityMap.put(entity.getListName(), entity));
        return mappingEntityMap;
    }

    @Override
    public RequestData getRequest(int workflowId, String userName, boolean userDisplayNames,
                                  WorkflowListSearchTypes searchTypes) throws ResourceException {
        logger.info("getRequest({})",workflowId);
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        if (workflowEntity == null) {
            return null;
        }
        CounterpartyFullDetail detail = new CounterpartyFullDetail();
        workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId().forEach(wfcdWorkflowCounterpartyDetailEntity -> {
            detail.setCounterpartyDetail(getCounterpartyModelFromEntity(wfcdWorkflowCounterpartyDetailEntity));
            detail.setCreditApplication(getCreditApplicationModelFromEntity(
                    wfcdWorkflowCounterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId(),
                    detail.getCounterpartyDetail().getCreditInkombaId()));
            List<FacilityModel> facilityModels = new ArrayList<>();
            wfcdWorkflowCounterpartyDetailEntity.getWffiWorkflowFacilityInformationByWffiId()
                    .forEach(facilityEntity -> {
                        facilityModels.add(getFacilityModelFromEntity(facilityEntity));
                    });
            detail.setFacility(facilityModels);
            detail.setFacilitySummary(getFacilitySummary(facilityModels));

            if(workflowEntity.getWorkflowSystemName().equalsIgnoreCase("ASHBURTON")){
                try {
                    detail.setAsbDetails(approvalForumExternalService.getAshburtonLimitFunds(String.valueOf(getApplicationId(workflowEntity.getWfId()))));
                } catch (ExternalResourceException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        RequestData requestData = new RequestData()
                .withType(RequestType.fromValue(workflowEntity.getRtRequestTypeByRtId().getType()))
                .withWip(true)
                .withActive(false)
                .withAssignedTo(workflowEntity.getAssignedTo())
                .withAssignedOn(workflowEntity.getAssignedOn() == null ? null
                        : workflowEntity.getAssignedOn().toLocalDateTime())
                .withCounterpartyFullDetail(detail)
                .withRulesResponse(workflowEntity.getWiprrWipRulesResponseByWiprrId() != null
                        ? (RulesResponse) convertByteArrayToObject(
                        workflowEntity.getWiprrWipRulesResponseByWiprrId().getRulesResponseData())
                        : null)
                .withUsersResponse(workflowEntity.getWipurWipUsersResponseByWipurId() != null
                        ? (UsersResponse) convertByteArrayToObject(
                        workflowEntity.getWipurWipUsersResponseByWipurId().getUsersResponseData())
                        : null)
                .withConfidential(workflowEntity.isConfidential()).withSpecific(workflowEntity.isSpecific())
                .withSpecificReason(workflowEntity.getSpecificReason())
                .withRecommendation(workflowEntity.getWfdaWorkflowDataByWfdaId() != null
                        ? workflowEntity.getWfdaWorkflowDataByWfdaId().getRecommendation()
                        : null)
                .withChairRecommendation(workflowEntity.getWfdaWorkflowDataByWfdaId() != null
                        ? workflowEntity.getWfdaWorkflowDataByWfdaId().getChairRecommendation()
                        : null)
                .withDecision(getDecisionData(workflowId, userName))
                .withCanSubmit(true)
                .withChairpersonApprovalRequired(workflowEntity.isChairpersonApprovalRequired())
                .withReferencedWorkflowId(workflowEntity.getReferencedWorkfowId())
                .withStatus(workflowEntity.getStatus() != null ? WorkflowStatus.fromValue(workflowEntity.getStatus())
                        : WorkflowStatus.WIP);
        if(AWAITING_CHAIRPERSON_APPROVAL.value().equals(workflowEntity.getStatus())) {
            requestData.setWip(false);
            requestData.setApprovalsResponse(getApprovals(workflowId));
            if (userDisplayNames) {
                requestData.setSubmittedBy(approvalForumAuthService
                        .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(workflowEntity.getSubmittedBy()));
            } else {
                requestData.setSubmittedBy(workflowEntity.getSubmittedBy());
            }
        } else if (!WorkflowStatus.WIP.value().equals(workflowEntity.getStatus())
                && !WorkflowStatus.CANCELLED.value().equals(workflowEntity.getStatus())) {
            requestData.setWip(false);
            requestData.setApprovalsResponse(getApprovals(workflowId));
            requestData.setOutstandingApprovalsResponse(getOutstandingApprovals(workflowId));
            requestData.setType(RequestType.fromValue(workflowEntity.getRtRequestTypeByRtId().getType()));
            if (userDisplayNames) {
                requestData.setSubmittedBy(approvalForumAuthService
                        .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(workflowEntity.getSubmittedBy()));
            } else {
                requestData.setSubmittedBy(workflowEntity.getSubmittedBy());
            }
            requestData.setSubmittedOn(workflowEntity.getSubmittedOn().toLocalDateTime());
        } else {
            if (WorkflowStatus.WIP.value().equals(workflowEntity.getStatus()) &&
                    (requestData.isSpecific() || requestData.isConfidential())) {
                requestData.setCanSubmit(checkIfQuorumBeenReachedOnWipGetRequest(requestData.getRulesResponse()));
            }
            // Get Approvals for Double DAF request
            if (requestData.getReferencedWorkflowId() != null) {
                requestData.setApprovalsResponse(getApprovals(workflowId));
            }
        }
        if (workflowEntity.getCompletedOn() != null)
            requestData.setCompletedOn(workflowEntity.getCompletedOn().toLocalDateTime());
        if (workflowEntity.isActive())
            requestData.setActive(true);
        if (searchTypes != null) {
            requestData.setUnseenChats(getChatUnseenCount(searchTypes, workflowId, userName));
        }
        if("Ashburton".equalsIgnoreCase(workflowEntity.getWorkflowSystemName())){
            requestData.setTenantId(2);
        }
        else if ("LUMOS".equalsIgnoreCase(workflowEntity.getWorkflowSystemName())){
            int tenantId = getTenantIdForWfId(workflowId);
            if(tenantId>2){
                logger.info("getRequest for broader africa tenants:"+tenantId);
                requestData.setTenantId(tenantId);
            } else{
                requestData.setTenantId(1);
            }
        }
        else {
            requestData.setTenantId(1);
        }
        logger.info("getRequest(END)");
        return requestData;
    }

    private int getApplicationId(int wfId) {
        int appId = approvalForumDataRepository.getApplicationId(wfId);
        logger.info("getApplicationId({})", appId);
        return appId;

    }

    private boolean checkIfQuorumBeenReachedOnWipGetRequest(RulesResponse rulesResponse) throws ResourceException {
        RulesModelData rulesModelData = validateAndGetSelectedRulesResponse(rulesResponse);
        List<ListManagementList> listsForModelData = getListManagementListUsersForRulesModelData(rulesResponse.getListManagementList(),
                rulesModelData);

        setDynamicMaxForRules(rulesModelData, listsForModelData);
        List<String> uniqueListOfApprovers = getUniqueListOfApproversFromRulesResponse(listsForModelData, rulesModelData);
        Map<Integer, RulesResponseListModelData> sortedListModelMap = getSortedListModelMap(rulesModelData);

        return hasQuorumBeenReachedForWipAssignment(rulesModelData, listsForModelData, uniqueListOfApprovers, sortedListModelMap);
    }

    private ApprovalsResponse getApprovals(int workflowId) {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        Map<Long, Approvals> approvalsMap = getApprovalsMap(workflowId, getMeetingNameFromCounterpartyEntities(workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId()));
        Integer referencedWorkflowId = workflowEntity.getReferencedWorkfowId();
        while (referencedWorkflowId != null && referencedWorkflowId > 0) {
            workflowEntity = approvalForumDataRepository.getWorkflow(referencedWorkflowId);
            approvalsMap.putAll(getApprovalsMap(referencedWorkflowId, getMeetingNameFromCounterpartyEntities(workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId())));
            referencedWorkflowId = workflowEntity.getReferencedWorkfowId();
        }
        return new ApprovalsResponse().withApprovals(approvalsMap.values());
    }

    private String getFieldFromCounterpartyEntities(Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities, FieldFromCounterpartyList field) {
        String fieldName = "";
        if (counterpartyDetailEntities != null && !counterpartyDetailEntities.isEmpty()) {
            for (ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity : counterpartyDetailEntities) {
                if (FieldFromCounterpartyList.MEETING_NAME.equals(field)) {
                    fieldName = counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId().getMeetingName();
                } else if (FieldFromCounterpartyList.REVIEW_ANALYST_ID.equals(field)) {
                    fieldName = counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId().getReviewAnalystId();
                } else {
                    fieldName = counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId().getCreditAnalystId();
                }
                break;
            }
        }
        return fieldName;
    }

    private String getMeetingNameFromCounterpartyEntities(Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities) {
        return getFieldFromCounterpartyEntities(counterpartyDetailEntities, FieldFromCounterpartyList.MEETING_NAME);
    }

    private String getReviewAnalystIdFromCounterpartyEntities(Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities) {
        return getFieldFromCounterpartyEntities(counterpartyDetailEntities, FieldFromCounterpartyList.REVIEW_ANALYST_ID);
    }

    private String getCreditAnalystIdFromCounterpartyEntities(Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities) {
        return getFieldFromCounterpartyEntities(counterpartyDetailEntities, FieldFromCounterpartyList.CREDIT_ANALYST_ID);
    }

    private Map<Long, Approvals> getApprovalsMap(int workflowId, String meetingName) {
        Map<String, Approvals> approvalsMap = new TreeMap<>();
        Approvals approvals;
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository
                .getCompletedApprovalsForWorkflowId(workflowId);
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalsEntities) {
            approvals = mapApprovalsFromApprovalEntity(approvalsEntity, meetingName);
            if (!approvalsMap.containsKey(approvals.getName())) {
                approvalsMap.put(approvals.getName(), approvals);
            }
        }
        Map<Long, Approvals> approvalsTreeMap = new TreeMap<>();
        for (Approvals approval : approvalsMap.values()) {
            approvalsTreeMap.put(Timestamp.valueOf(approval.getDate()).getTime(), approval);
        }
        return approvalsTreeMap;
    }

    private DecisionData getDecisionData(int workflowId, String userId) {
        DecisionData decisionData = null;
        if (userId == null) return decisionData;
        List<ApprovalForumWfaWorkflowApprovalsEntity> entities =
                approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userId);
        if (entities != null && !entities.isEmpty()) {
            decisionData = new DecisionData();
            decisionData.setConditions(entities.get(0).getApprovalConditions());
            decisionData.setDecision(entities.get(0).getDecision() == null ? null : ApprovalDecision.fromValue(entities.get(0).getDecision()));
            decisionData.setPolicyApprovalMandate(entities.get(0).getRecommendedPolicyApprovalMandate());
        }
        return decisionData;
    }

    private Approvals mapApprovalsFromApprovalEntity(ApprovalForumWfaWorkflowApprovalsEntity entity, String meetingName) {
        Approvals approvals = new Approvals();
        approvals.setAction(ApprovalDecision.fromValue(entity.getDecision()));
        approvals.setConditionsOfApproval(entity.getApprovalConditions());
        approvals.setDate(entity.getDateCompleted().toLocalDateTime());
        if (entity.getName() != null && !"".equals(entity.getName().trim()) && entity.getSurname() != null
                && !"".equals(entity.getSurname().trim())) {
            approvals.setName(entity.getName().trim() + " " + entity.getSurname().trim());
        } else {
            approvals.setName(
                    approvalForumAuthService.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(entity.getUserId()));
        }
        approvals.setApprovalMandate(meetingName);
        approvals.setRecommendedMandate(entity.getRecommendedPolicyApprovalMandate());
        return approvals;
    }

    private OutstandingApprovalsResponse getOutstandingApprovals(int workflowId) {
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        if (!entity.isActive())
            return new OutstandingApprovalsResponse().withOutstandingApprovals(new ArrayList<>());
        if (entity.isSpecific() || entity.isConfidential()) {
            return new OutstandingApprovalsResponse().withOutstandingApprovals(getSpecificOutstandingApprovals(entity));
        }
        return new OutstandingApprovalsResponse().withOutstandingApprovals(getGeneralOutstandingApprovals(entity));
    }

    private List<OutstandingApprovals> getSpecificOutstandingApprovals(ApprovalForumWfWorkflowEntity entity) {
        Map<String, Integer> listMap = new HashMap<>();
        int count;
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository
                .getApprovalsForWorkflowId(entity.getWfId());
        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalEntities = getApprovalEntitiesForQuorum(
                approvalsEntities, quorumDataEntities, false);

        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumAdditionalApprovalEntities = getApprovalEntitiesForQuorum(
                approvalsEntities, quorumDataEntities, true);

        listMap.put("Additional Approvers", quorumAdditionalApprovalEntities.size());

        // First loop and set total counts
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumApprovalEntities) {
            if (approvalsEntity.isSpecificApprover()) {
                count = listMap.getOrDefault(
                        approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName(), 0) + 1;
                listMap.put(approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName(), count);
            }
        }
        // Then loop and subtract which have been approved
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumApprovalEntities) {
            if (approvalsEntity.getDateCompleted() != null) {
                if (approvalsEntity.isSpecificApprover()) {
                    count = listMap.get(
                            approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName()) - 1;
                    listMap.put(approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName(),
                            count);
                }
            }
        }
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumAdditionalApprovalEntities) {
            if (approvalsEntity.getDateCompleted() != null) {
                count = listMap.get("Additional Approvers") - 1;
                listMap.put("Additional Approvers", count);
            }
        }
        return getListFromMap(listMap);
    }

    private List<OutstandingApprovals> getGeneralOutstandingApprovals(ApprovalForumWfWorkflowEntity entity) {
        Map<String, Integer> listMap = new HashMap<>();
        listMap.put("Approvers", getNumberOfApproversRequired(entity.getWfId(), false));
        List<String> uniqueListOfApprovers = new ArrayList<>();
        int count;
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository
                .getApprovalsForWorkflowId(entity.getWfId());
        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalEntities = getApprovalEntitiesForQuorum(
                approvalsEntities, quorumDataEntities, false);
        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumAdditionalApprovalEntities = getApprovalEntitiesForQuorum(
                approvalsEntities, quorumDataEntities, true);

        listMap.put("Additional Approvers", quorumAdditionalApprovalEntities.size());

        // Then loop and subtract which have been approved
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumApprovalEntities) {
            if (approvalsEntity.getDateCompleted() != null) {
                if (uniqueListOfApprovers.indexOf(approvalsEntity.getUserId()) < 0) {
                    uniqueListOfApprovers.add(approvalsEntity.getUserId());
                    count = listMap.get("Approvers") - 1;
                    listMap.put("Approvers", count);
                }
            }
        }
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumAdditionalApprovalEntities) {
            if (approvalsEntity.getDateCompleted() != null) {
                if (uniqueListOfApprovers.indexOf(approvalsEntity.getUserId()) < 0) {
                    uniqueListOfApprovers.add(approvalsEntity.getUserId());
                    count = listMap.get("Additional Approvers") - 1;
                    listMap.put("Additional Approvers", count);
                }
            }
        }
        return getListFromMap(listMap);
    }

    private List<OutstandingApprovals> getListFromMap(Map<String, Integer> listMap) {
        List<OutstandingApprovals> outstandingApprovalsList = new ArrayList<>();
        for (String key : listMap.keySet()) {
            OutstandingApprovals outstandingApprovals = new OutstandingApprovals();
            outstandingApprovals.setName(key);
            outstandingApprovals.setCount(listMap.get(key));
            outstandingApprovalsList.add(outstandingApprovals);
        }
        return outstandingApprovalsList;
    }

    private CounterpartyModel getCounterpartyModelFromEntity(
            ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity) {
        return new CounterpartyModel().withCounterpartyName(counterpartyDetailEntity.getCounterpartyName())
                .withCreditInkombaId(counterpartyDetailEntity.getCreditInkombaId())
                .withCreditAnalyst(counterpartyDetailEntity.getCreditAnalyst())
                .withCreditAnalystId(counterpartyDetailEntity.getCreditAnalystId())
                .withCreditAnalystMail(counterpartyDetailEntity.getCreditAnalystEmail())
                .withFinancialInstitutionType(counterpartyDetailEntity.getFinancialInstitutionType())
                .withBank(counterpartyDetailEntity.isBank())
                .withProspect(counterpartyDetailEntity.isProspect());
    }

    private CreditApplicationModel getCreditApplicationModelFromEntity(
            ApprovalForumWfadWorkflowApplicationDataEntity applicationDataEntity, String creditInkombaId) {
        CreditApplicationModel model = new CreditApplicationModel();
        model.setCreditInkombaId(creditInkombaId);
        model.setLastApprovedFrRating(applicationDataEntity.getLastApprovedFrRating());
        model.setProposedFrRating(applicationDataEntity.getProposedFrRating());
        model.setLastApprovedDcp(applicationDataEntity.getLastApprovedDcp());
        model.setProposedDcp(applicationDataEntity.getProposedDcp());
        model.setLastApprovedSettlementDcp(applicationDataEntity.getLastApprovedSettlementDcp());
        model.setProposedSettlementDcp(applicationDataEntity.getProposedSettlementDcp());
        model.setDistressStatus(applicationDataEntity.getDistressStatus());
        model.setMeetingName(applicationDataEntity.getMeetingName());
        model.setWholesaleCreditPortfolio(applicationDataEntity.getWholesaleCreditPortfolio());
        model.setProposalType(applicationDataEntity.getProposalType());
        model.setProposalAction(applicationDataEntity.getProposalAction());
        model.setReviewAnalystEmail(applicationDataEntity.getReviewAnalystEmail());
        model.setReviewAnalyst(applicationDataEntity.getReviewAnalyst());
        model.setReviewAnalystId(applicationDataEntity.getReviewAnalystId());
        model.setReviewAnalystSignatureDate(applicationDataEntity.getReviewAnalystSignatureDate() != null ?
                applicationDataEntity.getReviewAnalystSignatureDate().toLocalDateTime() : null);
        model.setCreditAnalystEmail(applicationDataEntity.getCreditAnalystEmail());
        model.setCreditAnalyst(applicationDataEntity.getCreditAnalyst());
        model.setCreditAnalystId(applicationDataEntity.getCreditAnalystId());
        model.setCreditAnalystSignatureDate(applicationDataEntity.getCreditAnalystSignatureDate() != null ?
                applicationDataEntity.getCreditAnalystSignatureDate().toLocalDateTime() : null);
        return model;
    }

    private FacilityModel getFacilityModelFromEntity(
            ApprovalForumWffiWorkflowFacilityInformationEntity facilityInformationEntity) {
        FacilityModel model = new FacilityModel();
        model.setFacilityReference(facilityInformationEntity.getFacilityReference());
        model.setRiskType(facilityInformationEntity.getRiskType());
        model.setTenor(facilityInformationEntity.getTenor());
        model.setNetLimit(facilityInformationEntity.getNetLimit());
        model.setNetLimitZar(facilityInformationEntity.getNetLimitZar());
        model.setCurrency(facilityInformationEntity.getCurrency());
        model.setFacilityDescription(facilityInformationEntity.getFacilityDescription());
        model.setCreditApplicationStatus(facilityInformationEntity.getCreditApplicationStatus());
        model.setRiskTaker(facilityInformationEntity.getRiskTaker());
        model.setDealSpecific(facilityInformationEntity.isDealSpecific());
        model.setProposedRating(facilityInformationEntity.getProposedRating());
        model.setProposedLgd(facilityInformationEntity.getLgd());
        model.setProposedLgdZar(facilityInformationEntity.getLgdZar());
        model.setFacilityId(facilityInformationEntity.getFacilityId());
        model.setFacilityRating(facilityInformationEntity.getFacilityRating());
        model.setFacilityInvestmentType(facilityInformationEntity.getFacilityInvestmentType());
        model.setRiskTakerGroupCode(facilityInformationEntity.getRiskTakerGroupCode());
        return model;
    }

    @Override
    public UserPreferenceResponseDTO getUserPreference(String userId) {
        UserPreferenceResponseDTO responseDTO = new UserPreferenceResponseDTO();
        ApprovalForumUpUserPreferenceEntity entity = approvalForumDataRepository.getUserPreference(userId);
        if (entity == null) {
            entity = new ApprovalForumUpUserPreferenceEntity();
            entity.setUserId(userId);
            entity.setChatSortAscending(false);
            approvalForumDataRepository.persistUserPreference(entity);
        }
        responseDTO.setUserPreference(new UserPreference().withUserId(entity.getUserId()).withChatSortAscending(entity.isChatSortAscending()));
        return responseDTO;
    }

    @Override
    public WorkflowListResponseDTO getWorkflows(String assignedTo, int start, int size, boolean onlyActive,
                                                String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate) {

        WorkflowListResponseDTO responseDTO = new WorkflowListResponseDTO();
        List<WorkflowListData> mappings = new ArrayList<>();
        responseDTO.setWorkflowListData(approvalForumDataRepository
                .getWorkflows(assignedTo, start, size, onlyActive, searchText, filterType, listSearchTypes,
                        sortByType, descending, loggedInUserId, approvalMandate)
                .stream().map(this::mapFromWorkflowEntity).collect(Collectors.toCollection(() -> mappings)));
        responseDTO.setStart(start);
        responseDTO.setSize(size);
        responseDTO.setTotal(approvalForumDataRepository.getWorkflowsCount(assignedTo, onlyActive, searchText,
                filterType, listSearchTypes, sortByType, descending, loggedInUserId, approvalMandate));

        setChatUnseenCount(responseDTO.getWorkflowListData(), listSearchTypes, loggedInUserId);

        List<String> approvalMandates = new ArrayList<>();
        approvalForumDataRepository
                .getWorkflowsMeetingNames(assignedTo, onlyActive, searchText, filterType, listSearchTypes,
                        sortByType, descending, loggedInUserId, approvalMandate)
                .forEach(workflowListData -> {
                    if (approvalMandates.indexOf(workflowListData) < 0) {
                        approvalMandates.add(workflowListData);
                    }
                });

        Collections.sort(approvalMandates);
        responseDTO.setApprovalMandate(approvalMandates);
        return responseDTO;
    }

    @Override
    public WorkflowListResponseDTO getWorkflowsForAshburton(String assignedTo, int start, int size, boolean onlyActive,
                                                            String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                            WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate) {

        WorkflowListResponseDTO responseDTO = new WorkflowListResponseDTO();
        List<WorkflowListData> mappings = new ArrayList<>();
        responseDTO.setWorkflowListData(approvalForumDataRepository
                .getWorkflowsForAshburton(assignedTo, start, size, onlyActive, searchText, filterType, listSearchTypes,
                        sortByType, descending, loggedInUserId, approvalMandate)
                .stream().map(this::mapFromWorkflowEntity).collect(Collectors.toCollection(() -> mappings)));
        responseDTO.setStart(start);
        responseDTO.setSize(size);
        responseDTO.setTotal(approvalForumDataRepository.getWorkflowsCountForAshburton(assignedTo, onlyActive, searchText,
                filterType, listSearchTypes, sortByType, descending, loggedInUserId, approvalMandate));

        setChatUnseenCount(responseDTO.getWorkflowListData(), listSearchTypes, loggedInUserId);

        List<String> approvalMandates = new ArrayList<>();
        approvalForumDataRepository
                .getWorkflowsMeetingNamesForAshburton(assignedTo, onlyActive, searchText, filterType, listSearchTypes,
                        sortByType, descending, loggedInUserId, approvalMandate)
                .forEach(workflowListData -> {
                    if (approvalMandates.indexOf(workflowListData) < 0) {
                        approvalMandates.add(workflowListData);
                    }
                });

        Collections.sort(approvalMandates);

        responseDTO.setApprovalMandate(approvalMandates);
        return responseDTO;
    }

    @Override
    public WorkflowListResponseDTO getWorkflowsForLumosBroaderAfricaTenants(String assignedTo, int start, int size, boolean onlyActive,
                                                            String searchText, WorkflowFilterTypes filterType, WorkflowListSearchTypes listSearchTypes,
                                                            WorkflowSortByTypes sortByType, boolean descending, String loggedInUserId, String approvalMandate, int tenantId) {

        WorkflowListResponseDTO responseDTO = new WorkflowListResponseDTO();
        List<WorkflowListData> mappings = new ArrayList<>();
        responseDTO.setWorkflowListData(approvalForumDataRepository
                .getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText, filterType, listSearchTypes,
                        sortByType, descending, loggedInUserId, approvalMandate, tenantId)
                .stream().map(this::mapFromWorkflowEntity).collect(Collectors.toCollection(() -> mappings)));
        responseDTO.setStart(start);
        responseDTO.setSize(size);
        responseDTO.setTotal(approvalForumDataRepository.getWorkflowsCountForLumosBroaderAfricaTenants(assignedTo, onlyActive, searchText,
                filterType, listSearchTypes, sortByType, descending, loggedInUserId, approvalMandate,tenantId));

        setChatUnseenCount(responseDTO.getWorkflowListData(), listSearchTypes, loggedInUserId);

        List<String> approvalMandates = new ArrayList<>();
        approvalForumDataRepository
                .getWorkflowsMeetingNamesForLumosBroaderAfricaTenants(assignedTo, onlyActive, searchText, filterType, listSearchTypes,
                        sortByType, descending, loggedInUserId, approvalMandate, tenantId)
                .forEach(workflowListData -> {
                    if (approvalMandates.indexOf(workflowListData) < 0) {
                        approvalMandates.add(workflowListData);
                    }
                });

        Collections.sort(approvalMandates);

        responseDTO.setApprovalMandate(approvalMandates);
        return responseDTO;
    }

    @Override
    public RequestData getRequestForAshburton(int workflowId, String userName, boolean userDisplayNames, WorkflowListSearchTypes searchTypes) throws ResourceException {
        logger.info("getRequestForAshburton(START)");
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflowForAshburton(workflowId);
        if (workflowEntity == null) {
            return null;
        }
        CounterpartyFullDetail detail = new CounterpartyFullDetail();
        workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId().forEach(wfcdWorkflowCounterpartyDetailEntity -> {
            detail.setCounterpartyDetail(getCounterpartyModelFromEntity(wfcdWorkflowCounterpartyDetailEntity));
            detail.setCreditApplication(getCreditApplicationModelFromEntity(
                    wfcdWorkflowCounterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId(),
                    detail.getCounterpartyDetail().getCreditInkombaId()));
            List<FacilityModel> facilityModels = new ArrayList<>();
            wfcdWorkflowCounterpartyDetailEntity.getWffiWorkflowFacilityInformationByWffiId()
                    .forEach(facilityEntity -> {
                        facilityModels.add(getFacilityModelFromEntity(facilityEntity));
                    });
            detail.setFacility(facilityModels);
            detail.setFacilitySummary(getFacilitySummary(facilityModels));
            if(workflowEntity.getWorkflowSystemName().equalsIgnoreCase("ASHBURTON")){
                try {
                    logger.info("getRequestForAshburton inside workflow system name block:"+workflowEntity.getWorkflowSystemName());
                    detail.setAsbDetails(approvalForumExternalService.getAshburtonLimitFunds(String.valueOf(getApplicationId(workflowEntity.getWfId()))));
                } catch (ExternalResourceException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        RequestData requestData = new RequestData()
                .withType(RequestType.fromValue(workflowEntity.getRtRequestTypeByRtId().getType()))
                .withWip(true)
                .withActive(false)
                .withAssignedTo(workflowEntity.getAssignedTo())
                .withAssignedOn(workflowEntity.getAssignedOn() == null ? null
                        : workflowEntity.getAssignedOn().toLocalDateTime())
                .withCounterpartyFullDetail(detail)
                .withRulesResponse(workflowEntity.getWiprrWipRulesResponseByWiprrId() != null
                        ? (RulesResponse) convertByteArrayToObject(
                        workflowEntity.getWiprrWipRulesResponseByWiprrId().getRulesResponseData())
                        : null)
                .withUsersResponse(workflowEntity.getWipurWipUsersResponseByWipurId() != null
                        ? (UsersResponse) convertByteArrayToObject(
                        workflowEntity.getWipurWipUsersResponseByWipurId().getUsersResponseData())
                        : null)
                .withConfidential(workflowEntity.isConfidential()).withSpecific(workflowEntity.isSpecific())
                .withSpecificReason(workflowEntity.getSpecificReason())
                .withRecommendation(workflowEntity.getWfdaWorkflowDataByWfdaId() != null
                        ? workflowEntity.getWfdaWorkflowDataByWfdaId().getRecommendation()
                        : null)
                .withChairRecommendation(workflowEntity.getWfdaWorkflowDataByWfdaId() != null
                        ? workflowEntity.getWfdaWorkflowDataByWfdaId().getChairRecommendation()
                        : null)
                .withDecision(getDecisionData(workflowId, userName))
                .withCanSubmit(true)
                .withChairpersonApprovalRequired(workflowEntity.isChairpersonApprovalRequired())
                .withReferencedWorkflowId(workflowEntity.getReferencedWorkfowId())
                .withStatus(workflowEntity.getStatus() != null ? WorkflowStatus.fromValue(workflowEntity.getStatus())
                        : WorkflowStatus.WIP);
        if(AWAITING_CHAIRPERSON_APPROVAL.value().equals(workflowEntity.getStatus())) {
            requestData.setWip(false);
            requestData.setApprovalsResponse(getApprovals(workflowId));
            if (userDisplayNames) {
                requestData.setSubmittedBy(approvalForumAuthService
                        .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(workflowEntity.getSubmittedBy()));
            } else {
                requestData.setSubmittedBy(workflowEntity.getSubmittedBy());
            }
        } else if (!WorkflowStatus.WIP.value().equals(workflowEntity.getStatus())
                && !WorkflowStatus.CANCELLED.value().equals(workflowEntity.getStatus())) {
            requestData.setWip(false);
            requestData.setApprovalsResponse(getApprovals(workflowId));
            requestData.setOutstandingApprovalsResponse(getOutstandingApprovals(workflowId));
            requestData.setType(RequestType.fromValue(workflowEntity.getRtRequestTypeByRtId().getType()));
            if (userDisplayNames) {
                requestData.setSubmittedBy(approvalForumAuthService
                        .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(workflowEntity.getSubmittedBy()));
            } else {
                requestData.setSubmittedBy(workflowEntity.getSubmittedBy());
            }
            requestData.setSubmittedOn(workflowEntity.getSubmittedOn().toLocalDateTime());
        } else {
            if (WorkflowStatus.WIP.value().equals(workflowEntity.getStatus()) &&
                    (requestData.isSpecific() || requestData.isConfidential())) {
                requestData.setCanSubmit(checkIfQuorumBeenReachedOnWipGetRequest(requestData.getRulesResponse()));
            }
            // Get Approvals for Double DAF request
            if (requestData.getReferencedWorkflowId() != null) {
                requestData.setApprovalsResponse(getApprovals(workflowId));
            }
        }
        if (workflowEntity.getCompletedOn() != null)
            requestData.setCompletedOn(workflowEntity.getCompletedOn().toLocalDateTime());
        if (workflowEntity.isActive())
            requestData.setActive(true);
        if (searchTypes != null) {
            requestData.setUnseenChats(getChatUnseenCount(searchTypes, workflowId, userName));
        }
        if("Ashburton".equalsIgnoreCase(workflowEntity.getWorkflowSystemName())){
            requestData.setTenantId(2);
        } else {
            requestData.setTenantId(1);
        }
        logger.info("getRequestForAshburton(END)");
        return requestData;
    }

    @Override
    public RequestData getRequestForLumosBroaderAfricaTenants(int workflowId, String userName, boolean userDisplayNames, WorkflowListSearchTypes searchTypes, int tenantId) throws ResourceException {
        logger.info("getRequestForLumosBroaderAfricaTenants({})",workflowId, tenantId);
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        if (workflowEntity == null) {
            return null;
        }
        CounterpartyFullDetail detail = new CounterpartyFullDetail();
        workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId().forEach(wfcdWorkflowCounterpartyDetailEntity -> {
            detail.setCounterpartyDetail(getCounterpartyModelFromEntity(wfcdWorkflowCounterpartyDetailEntity));
            detail.setCreditApplication(getCreditApplicationModelFromEntity(
                    wfcdWorkflowCounterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId(),
                    detail.getCounterpartyDetail().getCreditInkombaId()));
            List<FacilityModel> facilityModels = new ArrayList<>();
            wfcdWorkflowCounterpartyDetailEntity.getWffiWorkflowFacilityInformationByWffiId()
                    .forEach(facilityEntity -> {
                        facilityModels.add(getFacilityModelFromEntity(facilityEntity));
                    });
            detail.setFacility(facilityModels);
            detail.setFacilitySummary(getFacilitySummary(facilityModels));

        });

        RequestData requestData = new RequestData()
                .withType(RequestType.fromValue(workflowEntity.getRtRequestTypeByRtId().getType()))
                .withWip(true)
                .withActive(false)
                .withAssignedTo(workflowEntity.getAssignedTo())
                .withAssignedOn(workflowEntity.getAssignedOn() == null ? null
                        : workflowEntity.getAssignedOn().toLocalDateTime())
                .withCounterpartyFullDetail(detail)
                .withRulesResponse(workflowEntity.getWiprrWipRulesResponseByWiprrId() != null
                        ? (RulesResponse) convertByteArrayToObject(
                        workflowEntity.getWiprrWipRulesResponseByWiprrId().getRulesResponseData())
                        : null)
                .withUsersResponse(workflowEntity.getWipurWipUsersResponseByWipurId() != null
                        ? (UsersResponse) convertByteArrayToObject(
                        workflowEntity.getWipurWipUsersResponseByWipurId().getUsersResponseData())
                        : null)
                .withConfidential(workflowEntity.isConfidential()).withSpecific(workflowEntity.isSpecific())
                .withSpecificReason(workflowEntity.getSpecificReason())
                .withRecommendation(workflowEntity.getWfdaWorkflowDataByWfdaId() != null
                        ? workflowEntity.getWfdaWorkflowDataByWfdaId().getRecommendation()
                        : null)
                .withChairRecommendation(workflowEntity.getWfdaWorkflowDataByWfdaId() != null
                        ? workflowEntity.getWfdaWorkflowDataByWfdaId().getChairRecommendation()
                        : null)
                .withDecision(getDecisionData(workflowId, userName))
                .withCanSubmit(true)
                .withChairpersonApprovalRequired(workflowEntity.isChairpersonApprovalRequired())
                .withReferencedWorkflowId(workflowEntity.getReferencedWorkfowId())
                .withStatus(workflowEntity.getStatus() != null ? WorkflowStatus.fromValue(workflowEntity.getStatus())
                        : WorkflowStatus.WIP);
        if(AWAITING_CHAIRPERSON_APPROVAL.value().equals(workflowEntity.getStatus())) {
            requestData.setWip(false);
            requestData.setApprovalsResponse(getApprovals(workflowId));
            if (userDisplayNames) {
                requestData.setSubmittedBy(approvalForumAuthService
                        .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(workflowEntity.getSubmittedBy()));
            } else {
                requestData.setSubmittedBy(workflowEntity.getSubmittedBy());
            }
        } else if (!WorkflowStatus.WIP.value().equals(workflowEntity.getStatus())
                && !WorkflowStatus.CANCELLED.value().equals(workflowEntity.getStatus())) {
            requestData.setWip(false);
            requestData.setApprovalsResponse(getApprovals(workflowId));
            requestData.setOutstandingApprovalsResponse(getOutstandingApprovals(workflowId));
            requestData.setType(RequestType.fromValue(workflowEntity.getRtRequestTypeByRtId().getType()));
            if (userDisplayNames) {
                requestData.setSubmittedBy(approvalForumAuthService
                        .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(workflowEntity.getSubmittedBy()));
            } else {
                requestData.setSubmittedBy(workflowEntity.getSubmittedBy());
            }
            requestData.setSubmittedOn(workflowEntity.getSubmittedOn().toLocalDateTime());
        } else {
            if (WorkflowStatus.WIP.value().equals(workflowEntity.getStatus()) &&
                    (requestData.isSpecific() || requestData.isConfidential())) {
                requestData.setCanSubmit(checkIfQuorumBeenReachedOnWipGetRequest(requestData.getRulesResponse()));
            }
            // Get Approvals for Double DAF request
            if (requestData.getReferencedWorkflowId() != null) {
                requestData.setApprovalsResponse(getApprovals(workflowId));
            }
        }
        if (workflowEntity.getCompletedOn() != null)
            requestData.setCompletedOn(workflowEntity.getCompletedOn().toLocalDateTime());
        if (workflowEntity.isActive())
            requestData.setActive(true);
        if (searchTypes != null) {
            requestData.setUnseenChats(getChatUnseenCount(searchTypes, workflowId, userName));
        }
            requestData.setTenantId(tenantId);

        logger.info("getRequest(END)");
        return requestData;
    }

    @Override
    public List<String> getInkombaIdsForActiveWorkflowsForAshburton() {
        List<String> inkombaIds = new ArrayList<>();
        approvalForumDataRepository.getInkombaIdsForActiveWorkflowsForAshburton().forEach(entity -> {
            if (inkombaIds.indexOf(entity.getCreditInkombaId()) < 0) {
                inkombaIds.add(entity.getCreditInkombaId());
            }
        });
        return inkombaIds;
    }

    private void setChatUnseenCount(List<WorkflowListData> workflowListData, WorkflowListSearchTypes listSearchTypes,
                                    String loggedInUserId) {
        workflowListData.forEach(workflow -> {
            workflow.setUnseenChats(getChatUnseenCount(listSearchTypes, workflow.getId(), loggedInUserId));
        });
    }

    private boolean getChatUnseenCount(WorkflowListSearchTypes listSearchTypes, int workflowId, String loggedInUserId) {
        int lastChatId = approvalForumDataRepository.getLastWorkflowChatId(workflowId);
        String loggedInUserShort = loggedInUserId;
        if(loggedInUserShort.contains("@")) loggedInUserShort = loggedInUserShort.substring(0, loggedInUserShort.indexOf("@"));
        if(lastChatId > 0) {
            if (WorkflowListSearchTypes.APPROVER.equals(listSearchTypes) || WorkflowListSearchTypes.CHAIRPERSON.equals(listSearchTypes)) {
                for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, loggedInUserId)){
                    if (approvalsEntity.getLastSeenChatId() < lastChatId) {
                        return true;
                    }
                }
            } else {
                ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
                if (WorkflowListSearchTypes.REVIEWER.equals(listSearchTypes)) {
                    String reviewAnalystId = workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId().iterator().next().
                            getWfadApprovalForumWorkflowApplicationDataByWfadId().getReviewAnalystId();

                    if (reviewAnalystId.equalsIgnoreCase(loggedInUserId) || reviewAnalystId.equalsIgnoreCase(loggedInUserShort)) {
                        return workflowEntity.getRaLastSeenChatId() < lastChatId;
                    }
                } else {
                    if(workflowEntity.isChairpersonApprovalRequired()){
                        return (workflowEntity.getCaLastSeenChatId() < lastChatId && workflowEntity.getAssignedTo() != null &&
                                (workflowEntity.getAssignedTo().equalsIgnoreCase(loggedInUserId) ||
                                        workflowEntity.getAssignedTo().equalsIgnoreCase(loggedInUserShort)));
                    } else {
                        return (workflowEntity.getCaLastSeenChatId() < lastChatId && workflowEntity.getSubmittedBy() != null &&
                                (workflowEntity.getSubmittedBy().equalsIgnoreCase(loggedInUserId) ||
                                        workflowEntity.getSubmittedBy().equalsIgnoreCase(loggedInUserShort)));
                    }

                }
            }
        }
        return false;
    }

    private WorkflowListData mapFromWorkflowEntity(ApprovalForumWfWorkflowEntity entity) {
        long now = Calendar.getInstance().getTime().getTime();
        WorkflowListData data = new WorkflowListData();
        data.setId(entity.getWfId());
        data.setCreatedBy(
                approvalForumAuthService.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(entity.getCreatedBy()));
        if (entity.getWfcdWorkflowCounterpartyDetailByWfId() != null
                && !entity.getWfcdWorkflowCounterpartyDetailByWfId().isEmpty()) {
            entity.getWfcdWorkflowCounterpartyDetailByWfId().forEach(counterparty -> {
                data.setCounterpartyName(counterparty.getCounterpartyName());
                data.setApprovalMandate(
                        counterparty.getWfadApprovalForumWorkflowApplicationDataByWfadId().getMeetingName());
            });
        }
        data.setCreatedOn(entity.getCreatedOn().toLocalDateTime());
        data.setConfidential(entity.isConfidential());
        if (entity.getAssignedTo() != null) {
            data.setCaAssignedTo(approvalForumAuthService.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(entity.getAssignedTo()));
        } else {
            data.setCaAssignedTo("");
        }
        if (entity.getSubmittedOn() != null) {
            data.setSubmittedBy(approvalForumAuthService.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(entity.getSubmittedBy()));
            data.setSubmittedOn(entity.getSubmittedOn().toLocalDateTime());
        }
        if (entity.getStatus() != null && !WorkflowStatus.WIP.value().equals(entity.getStatus())
                && !WorkflowStatus.CANCELLED.value().equals(entity.getStatus()) && entity.isActive()) {
            if(AWAITING_CHAIRPERSON_APPROVAL.value().equals(entity.getStatus())){
                data.setOverviewData(getChairpersonWorkflowOverviewData(entity));
            } else {
                data.setOverviewData(getWorkflowOverviewData(entity));
                data.setOutstandingApprovals(getOutstandingNumberOfApprovers(entity));
            }
            data.setResponseDeadline(calculateRequestDeadline(entity.getCreatedOn().getTime(), now));

            data.setSpecificApprover(entity.isSpecific());
        }
        data.setStatus(entity.getStatus() != null ? WorkflowStatus.fromValue(entity.getStatus()) : WorkflowStatus.WIP);
        return data;
    }

    private RequestDeadline calculateRequestDeadline(long createdOn, long now) {
        long diff = now - createdOn;
        if (diff <= 86400000) {
            return RequestDeadline.GREEN;
        } else if (diff <= 172800000) {
            return RequestDeadline.ORANGE;
        }
        return RequestDeadline.RED;
    }

    private int getOutstandingNumberOfApprovers(ApprovalForumWfWorkflowEntity entity) {
        if (!entity.isActive())
            return 0;
        List<String> uniqueListOfApprovers = new ArrayList<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalEntity : entity.getWfaWorkflowApprovalsByWfId()) {
            if (approvalEntity.getDateCompleted() != null && approvalEntity.getDecision() != null
                    && (ApprovalDecision.APPROVED.value().equals(approvalEntity.getDecision())
                    || ApprovalDecision.CONDITIONALLY_APPROVED.value().equals(approvalEntity.getDecision()))) {
                if (!uniqueListOfApprovers.contains(approvalEntity.getUserId())) {
                    uniqueListOfApprovers.add(approvalEntity.getUserId());
                }
            }
        }
        return getNumberOfApproversRequired(entity.getWfId(), true) - uniqueListOfApprovers.size();
    }

    private int getNumberOfApproversRequired(int workflowId, boolean includeAdditional) {
        int totalNumberRequired = approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(workflowId)
                .getTotalNumberOfApprovalsRequired();
        if (includeAdditional) {
            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalForumDataRepository
                    .getApprovalsForWorkflowId(workflowId)) {
                if (approvalsEntity.isAdditionalApprover()) {
                    totalNumberRequired++;
                }
            }
        }
        return totalNumberRequired;
    }

    private WorkflowOverviewData getWorkflowOverviewData(ApprovalForumWfWorkflowEntity entity) {
        return new WorkflowOverviewData().withWorkflowAssignedToData(getWorkflowAssignedToData(entity))
                .withWorkflowQuorumData(getWorkflowQuorum(entity));
    }

    private WorkflowOverviewData getChairpersonWorkflowOverviewData(ApprovalForumWfWorkflowEntity entity) {
        return new WorkflowOverviewData().withWorkflowAssignedToData(getChairpersonWorkflowAssignedToData(entity));
    }

    private List<WorkflowQuorumData> getWorkflowQuorum(ApprovalForumWfWorkflowEntity entity) {
        List<WorkflowQuorumData> workflowQuorumDataList = new ArrayList<>();
        approvalForumDataRepository.getWorkflowQuorumByWorkflowId(entity.getWfId())
                .forEach(approvalForumWfqWorkflowQuorumEntity -> {
                    workflowQuorumDataList.add(getWorkflowQuorumData(approvalForumWfqWorkflowQuorumEntity));
                });
        return workflowQuorumDataList;
    }

    private WorkflowQuorumData getWorkflowQuorumData(ApprovalForumWfqWorkflowQuorumEntity entity) {
        List<QuorumData> quorumData = new ArrayList<>();
        approvalForumDataRepository.getApprovalForumQuorumDataForQuorumId(entity.getWfqId()).forEach(entity1 -> {
            QuorumData data = new QuorumData();
            data.setListName(entity1.getAflmmApprovalForumListManagementMappingByAflmmId().getListName());
            data.setMaxNumberOfApprover(entity1.getMaxRequired());
            data.setMinNumberOfApprover(entity1.getMinRequired());
            data.setRank(entity1.getRank());
            quorumData.add(data);
        });
        return new WorkflowQuorumData().withQuorumData(quorumData);
    }

    private WorkflowAssignedToData getWorkflowAssignedToData(ApprovalForumWfWorkflowEntity entity) {
        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());
        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities = getApprovalEntitiesForQuorum(
                new ArrayList<>(entity.getWfaWorkflowApprovalsByWfId()), quorumDataEntities, false);
        quorumApprovalsEntities.addAll(getApprovalEntitiesForQuorum(
                new ArrayList<>(entity.getWfaWorkflowApprovalsByWfId()), quorumDataEntities, true));
        Map<String, AssignedToData> assignedToDataMap = new TreeMap<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalEntity : quorumApprovalsEntities) {
            if (approvalEntity.getDateAssigned() != null && approvalEntity.getDateCompleted() == null) {
                String user;
                if (approvalEntity.getName() != null && !"".equals(approvalEntity.getName().trim())
                        && approvalEntity.getSurname() != null && !"".equals(approvalEntity.getSurname().trim())) {
                    user = approvalEntity.getName().trim() + " " + approvalEntity.getSurname().trim();
                } else {
                    user = approvalForumAuthService
                            .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(approvalEntity.getUserId());
                }
                if (!assignedToDataMap.containsKey(user)) {
                    AssignedToData data = new AssignedToData();
                    data.setDisplayName(user);
                    assignedToDataMap.put(user, data);
                }
            }
        }
        return new WorkflowAssignedToData().withAssignedToData(assignedToDataMap.values());
    }

    private WorkflowAssignedToData getChairpersonWorkflowAssignedToData(ApprovalForumWfWorkflowEntity entity) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> chairpersonApprovalEntities = new ArrayList<>(entity.getWfaWorkflowApprovalsByWfId());
        Map<String, AssignedToData> assignedToDataMap = new TreeMap<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalEntity : chairpersonApprovalEntities) {
            if (approvalEntity.getDateAssigned() != null && approvalEntity.getDateCompleted() == null) {
                String user;
                if (approvalEntity.getName() != null && !"".equals(approvalEntity.getName().trim())
                        && approvalEntity.getSurname() != null && !"".equals(approvalEntity.getSurname().trim())) {
                    user = approvalEntity.getName().trim() + " " + approvalEntity.getSurname().trim();
                } else {
                    user = approvalForumAuthService
                            .getActiveDirectoryUserDisplayNameByAliasIgnoreCase(approvalEntity.getUserId());
                }
                if (!assignedToDataMap.containsKey(user)) {
                    AssignedToData data = new AssignedToData();
                    data.setDisplayName(user);
                    assignedToDataMap.put(user, data);
                }
            }
        }
        return new WorkflowAssignedToData().withAssignedToData(assignedToDataMap.values());
    }

    private QuorumCheckData getQuorumCheckData(int workflowId){
        QuorumCheckData quorumCheckData = new QuorumCheckData();
        quorumCheckData.setSelectedWorkflowQuorumEntity(approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(workflowId));
        quorumCheckData.setQuorumDataEntities(approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(quorumCheckData.getSelectedWorkflowQuorumEntity().getWfqId()));
        quorumCheckData.setSortedListModelMap(getSortedListModelMap(quorumCheckData.getQuorumDataEntities()));
        quorumCheckData.setQuorumApprovalsEntities(getApprovalEntitiesForQuorum(
                approvalForumDataRepository.getApprovalsForWorkflowId(workflowId), quorumCheckData.getQuorumDataEntities(), false));
        quorumCheckData.setQuorumAdditionalApprovalsEntities(getApprovalEntitiesForQuorum(
                approvalForumDataRepository.getApprovalsForWorkflowId(workflowId), quorumCheckData.getQuorumDataEntities(), true));
        setDynamicMaxForRulesGeneral(quorumCheckData.getSelectedWorkflowQuorumEntity(),
                quorumCheckData.getQuorumDataEntities(), quorumCheckData.getQuorumApprovalsEntities(), quorumCheckData.getSortedListModelMap());
        return quorumCheckData;
    }

    @Override
    public AssignResponse checkIfWorkflowCanBeAssigned(int workflowId, String userName) throws ResourceException {
        AssignResponse response = new AssignResponse().withResponse(null);
        logger.info("checkIfWorkflowCanBeAssigned: response {}", response);
        String userNameShort = userName;
        if (userNameShort.contains("@")) userNameShort = userNameShort.substring(0, userNameShort.indexOf("@"));
        logger.info("checkIfWorkflowCanBeAssigned: userNameShort {}, username {}", userNameShort, userName);
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        logger.info("checkIfWorkflowCanBeAssigned: workflowEntity {}", workflowEntity);
        if (!workflowEntity.isActive()) {
            logger.info("checkIfWorkflowCanBeAssigned: !workflowEntity.isActive {}, workflowId {}", !workflowEntity.isActive(), workflowId);
            response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
            return response;
        }
        logger.info("Work flow is not Active: responseAfter {}", response.getResponse());

        String submittedby = workflowEntity.getSubmittedBy();
        if (submittedby == null || "".equalsIgnoreCase(submittedby)) {
            throw new ResourceException("Workflow not submitted");
        }

        //Submitted by user cannot assign to himself
        if (submittedby.toLowerCase().trim().equalsIgnoreCase(userName.trim()) ||
                submittedby.toLowerCase().trim().equalsIgnoreCase(userNameShort.trim())) {
            response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
            logger.info("Case can not be assigned to the user submitted the request: responseAfter {}", response.getResponse());
            return response;
        }

        //Reviewer cannot assign to himself
        String reviewAnalystId = getReviewAnalystIdFromCounterpartyEntities(workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId());
        logger.info("checkIfWorkflowCanBeAssigned: reviewAnalystId {}", reviewAnalystId);
        int tenantId = getTenantIdForWfId(workflowId);
        logger.info("checkIfWorkflowCanBeAssigned: tenantId {}", tenantId);
        if(tenantId>2){
            logger.info("checkIfWorkflowCanBeAssigned: review analyst check is skipped for tenantId {}", tenantId);
        } else {
            if (userName.equalsIgnoreCase(reviewAnalystId) ||
                    userNameShort.equalsIgnoreCase(reviewAnalystId)) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                logger.info("Review analyst can not assign to himself: responseAfter {}", response.getResponse());
                return response;
            }
        }

        //Credit analyst cannot assign to himself
        String creditAnalystId = getCreditAnalystIdFromCounterpartyEntities(workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId());
        logger.info("checkIfWorkflowCanBeAssigned: creditAnalystId {}", creditAnalystId);
        //int tenantId = getTenantIdForWfId(workflowId);
        logger.info("checkIfWorkflowCanBeAssigned: tenantId {}", tenantId);
        if(tenantId>2){
            logger.info("checkIfWorkflowCanBeAssigned: credit AnalystId check is skipped for tenantId {}", tenantId);
        } else {
            if (userName.equalsIgnoreCase(creditAnalystId) ||
                    userNameShort.equalsIgnoreCase(creditAnalystId)) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                logger.info("Credit analyst can not assign to himself: responseAfter {}", response.getResponse());
                return response;
            }
        }

        if(workflowEntity.isChairpersonApprovalRequired() && workflowEntity.getStatus().equals(AWAITING_CHAIRPERSON_APPROVAL.value())){

            boolean found = false;
            boolean assigned = false;
            boolean assignedToOther = false;

            for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: workflowEntity.getWfaWorkflowApprovalsByWfId()) {
                if (approvalsEntity.isChairpersonApprover()) {
                    if(!approvalsEntity.getUserId().equalsIgnoreCase(userName) && approvalsEntity.getDateAssigned() != null){
                        assignedToOther = true;
                        logger.info("Case assigned to other: responseAfter {}", response.getResponse());
                        break;
                    }
                }
            }

            if(assignedToOther){
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                logger.info("Case assigned to others, can not be assigned to you: responseAfter {}", response.getResponse());
                return response;
            }

            for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: workflowEntity.getWfaWorkflowApprovalsByWfId()){
                if(approvalsEntity.isChairpersonApprover()){
                    if(approvalsEntity.getUserId().equalsIgnoreCase(userName)){
                        found = true;
                        if(approvalsEntity.getDateAssigned() != null){
                            assigned = true;
                            break;
                        }
                    }
                }

            }

            if (!found) {
                logger.info("Chair person not found: responseAfter {}", response.getResponse());
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }

            if (assigned) {
                logger.info("!assigned: {}", assigned);
                response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                return response;
            }

        } else {
            logger.info("Quorum data check start: responseAfter {}", response.getResponse());
            QuorumCheckData quorumCheckData = getQuorumCheckData(workflowId);

            boolean found = false;
            boolean assigned = false;
            for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: quorumCheckData.getQuorumApprovalsEntities()){
                logger.info("Approval entities data - User id check: responseAfter {}", response.getResponse());
                if(approvalsEntity.getUserId().equalsIgnoreCase(userName)){
                    found = true;
                    if(approvalsEntity.getDateAssigned() != null){
                        assigned = true;
                        break;
                    }
                }
            }
            for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: quorumCheckData.getQuorumAdditionalApprovalsEntities()){
                logger.info("Additional approval entities data - User id check: responseAfter {}", response.getResponse());
                if(approvalsEntity.getUserId().equalsIgnoreCase(userName)){
                    found = true;
                    if(approvalsEntity.getDateAssigned() != null){
                        assigned = true;
                        break;
                    }
                }
            }
            logger.info("Found - User id check: responseAfter {}", found);
            if (!found) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }

            if (hasQuorumBeenReachedForAssignment(quorumCheckData)) {
                response.setResponse(AssignedTypes.QUORUM_REACHED);
            }

            if (assigned) {
                if (response.getResponse() != null && AssignedTypes.QUORUM_REACHED.equals(response.getResponse())) {
                    response.setResponse(AssignedTypes.QUORUM_REACHED_AND_ASSIGNED);
                } else {
                    response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                }
                return response;
            }

            if (response.getResponse() != null && AssignedTypes.QUORUM_REACHED.equals(response.getResponse())) {
                return response;
            }

            List<String> uniqueListOfApprovers = getUniqueListOfApproversFromApproversEntity(quorumCheckData.getSelectedWorkflowQuorumEntity(), false);
            logger.info("Workflow quorum entity data - User id check: responseAfter {}", response.getResponse());
            if (!quorumNotReachedForAssignmentAndCanUserBeAssigned(quorumCheckData.getSelectedWorkflowQuorumEntity(),
                    userName, uniqueListOfApprovers, quorumCheckData.getSortedListModelMap())) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            logger.info("Selected workflow quorum data - User id check: responseAfter {}", response.getResponse());
            if (!isAllRequiredListsPopulated(quorumCheckData.getSelectedWorkflowQuorumEntity(), userName, uniqueListOfApprovers,
                    quorumCheckData.getQuorumApprovalsEntities(), quorumCheckData.getSortedListModelMap())) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
        }

        response.setResponse(AssignedTypes.CAN_BE_ASSIGNED);
        logger.info("Selected workflow quorum data - User id check: CAN_BE_ASSIGNED {}", response.getResponse());
        return response;
    }

    @Override
    public AssignResponse checkIfWipWorkflowCanBeAssigned(int workflowId, String userName) throws ResourceException {
        AssignResponse response = new AssignResponse().withResponse(null);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        if (entity.getAssignedTo() != null) {
            if (entity.getAssignedTo().equalsIgnoreCase(userName)) {
                response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
            } else {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
            }
            return response;
        }
        response.setResponse(AssignedTypes.CAN_BE_ASSIGNED);
        return response;
    }

    private List<ApprovalForumWfaWorkflowApprovalsEntity> getApprovalEntitiesForQuorum(
            List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities,
            List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities, boolean additionalApprovers) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalEntities = new ArrayList<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalsEntities) {
            if (additionalApprovers) {
                if (approvalsEntity.isAdditionalApprover()) {
                    quorumApprovalEntities.add(approvalsEntity);
                }
            } else {
                if (approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId() != null && quorumDataEntities
                        .stream().filter(
                                quorumDataEntity -> quorumDataEntity
                                        .getAflmmApprovalForumListManagementMappingByAflmmId().getListName()
                                        .equalsIgnoreCase
                                                (approvalsEntity
                                                        .getAflmmApprovalForumListManagementMappingByAflmmId().getListName()))
                        .findFirst().orElse(null) != null) {
                    quorumApprovalEntities.add(approvalsEntity);
                }
            }
        }
        return quorumApprovalEntities;
    }

    @Override
    public boolean checkIfWorkflowCanBeViewed(int workflowId, String userName) throws ResourceException {
        ApprovalForumWfWorkflowEntity approvalForumWfWorkflowEntity = approvalForumDataRepository
                .getWorkflow(workflowId);
        if ((approvalForumWfWorkflowEntity.getSubmittedBy() != null
                && approvalForumWfWorkflowEntity.getSubmittedBy().equalsIgnoreCase(userName))
                || approvalForumWfWorkflowEntity.getCreatedBy().equalsIgnoreCase(userName)) {
            return true;
        }
        if(!WorkflowStatus.WIP.value().equalsIgnoreCase(approvalForumWfWorkflowEntity.getStatus()) &&
                !WorkflowStatus.CANCELLED.value().equalsIgnoreCase(approvalForumWfWorkflowEntity.getStatus()) &&
                !AWAITING_CHAIRPERSON_APPROVAL.value().equalsIgnoreCase(approvalForumWfWorkflowEntity.getStatus())
        ){

            //Review analyst should be able to view
            ApprovalForumWfadWorkflowApplicationDataEntity applicationDataEntity =
                    approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId).get(0).getWfadApprovalForumWorkflowApplicationDataByWfadId();

            String userNameShort = userName;
            if (userNameShort.contains("@")) userNameShort = userNameShort.substring(0, userNameShort.indexOf("@"));

            if (userName.equalsIgnoreCase(applicationDataEntity.getReviewAnalystId()) ||
                    userNameShort.equalsIgnoreCase(applicationDataEntity.getReviewAnalystId())) {
                return true;
            }

            List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository
                    .getApprovalsForWorkflowIdAndUserName(workflowId, userName);
            List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                    .getApprovalForumQuorumDataForQuorumId(
                            approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(workflowId).getWfqId());

            if (approvalsEntities.isEmpty()) {
                return false;
            }

            List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities = getApprovalEntitiesForQuorum(
                    approvalsEntities, quorumDataEntities, false);
            quorumApprovalsEntities.addAll(getApprovalEntitiesForQuorum(approvalsEntities, quorumDataEntities, true));

            if (quorumApprovalsEntities.isEmpty()) {
                return false;
            }

            for (ApprovalForumWfaWorkflowApprovalsEntity entity : quorumApprovalsEntities) {
                if (entity.getUserId().equalsIgnoreCase(userName))
                    return true;
            }
        }
        if (WorkflowStatus.WIP.value().equalsIgnoreCase(approvalForumWfWorkflowEntity.getStatus())) {
            // TODO - NEED TO ADD LOGIC TO DISPLAY ONLY TO CERTAIN ANALYSTS
            return true;
        }
        if (AWAITING_CHAIRPERSON_APPROVAL.value().equalsIgnoreCase(approvalForumWfWorkflowEntity.getStatus())) {
            // TODO - NEED TO ADD LOGIC TO DISPLAY ONLY TO CERTAIN ANALYSTS
            return true;
        }
        return false;
    }

    @Override
    public AssignResponse checkIfWorkflowCanBeSubmitted(int workflowId, String userName) throws ResourceException {
        AssignResponse response = new AssignResponse();

        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);

        if (workflowEntity == null || !workflowEntity.isActive()) {
            response.setResponse(AssignedTypes.WORKFLOW_CLOSED);
            return response;
        }

        int tenantId = getTenantIdForWfId(workflowId);
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities;

        if (tenantId > 2) {
            logger.info("BroaderAfrica workflowId {}, tenantId {}", workflowId, tenantId);
            approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName);
        } else {
            logger.info("Standard workflowId {}, tenantId {}", workflowId, tenantId);
            approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName);
        }

        if (approvalsEntities == null || approvalsEntities.isEmpty()) {
            logger.info("checkIfWorkflowCanBeSubmitted:approvalsEntities is null/empty");
            response.setResponse(AssignedTypes.CAN_NOT_BE_SUBMITTED);
            return response;
        }

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(workflowId).getWfqId());

        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities = getApprovalEntitiesForQuorum(
                approvalsEntities, quorumDataEntities, false);
        quorumApprovalsEntities.addAll(getApprovalEntitiesForQuorum(approvalsEntities, quorumDataEntities, true));

        if (quorumApprovalsEntities.isEmpty()) {
            logger.info("checkIfWorkflowCanBeSubmitted:quorumApprovalsEntities is null/empty");
            response.setResponse(AssignedTypes.CAN_NOT_BE_SUBMITTED);
            return response;
        }

        for (ApprovalForumWfaWorkflowApprovalsEntity entity : quorumApprovalsEntities) {
            logger.info("checkIfWorkflowCanBeSubmitted:ApprovalForumWfaWorkflowApprovalsEntity");
            if (entity.getDateAssigned() != null) {
                if (hasQuorumBeenReachedForApprovals(workflowId)) {
                    logger.info("checkIfWorkflowCanBeSubmitted:hasQuorumBeenReachedForApprovals is null/empty");
                    response.setResponse(AssignedTypes.QUORUM_REACHED);
                    return response;
                } else {
                    if (entity.getDateCompleted() != null) {
                        logger.info("checkIfWorkflowCanBeSubmitted:getDateCompleted is null/empty");
                        response.setResponse(AssignedTypes.CAN_NOT_BE_SUBMITTED);
                        return response;
                    }
                    logger.info("checkIfWorkflowCanBeSubmitted:else");
                    response.setResponse(AssignedTypes.CAN_BE_SUBMITTED);
                    return response;
                }
            }
        }
        logger.info("checkIfWorkflowCanBeSubmitted:default");
        response.setResponse(AssignedTypes.CAN_NOT_BE_SUBMITTED);
        return response;
    }


    private RulesModelData validateAndGetSelectedRulesResponse(RulesResponse rulesResponse) throws ResourceException {
        int selectedCount = 0;
        RulesModelData selectedRulesModelData = null;
        for (RulesModelData rulesModelData : rulesResponse.getModelData()) {
            if (rulesModelData.isSelected()) {
                selectedCount++;
                selectedRulesModelData = rulesModelData;
                if (selectedCount > 1) {
                    throw new ResourceException("More than one quorum option selected");
                }
            }
        }
        if (selectedCount == 0)
            throw new ResourceException("No quorum data selected");
        return selectedRulesModelData;
    }

    @Override
    public AssignResponse checkIfWipWorkflowCanBeAssigned(RulesResponse rulesResponse, UsersResponse usersResponse,
                                                          String userName, boolean isForSpecific) throws ResourceException {
        AssignResponse response = new AssignResponse().withResponse(null);

        RulesModelData rulesModelData = validateAndGetSelectedRulesResponse(rulesResponse);
        List<ListManagementList> listsForModelData = getListManagementListUsersForRulesModelData(rulesResponse.getListManagementList(),
                rulesModelData);

        setDynamicMaxForRules(rulesModelData, listsForModelData);
        List<String> uniqueListOfApprovers = getUniqueListOfApproversFromRulesResponse(listsForModelData, rulesModelData);
        Map<Integer, RulesResponseListModelData> sortedListModelMap = getSortedListModelMap(rulesModelData);

        boolean found = false;
        boolean assigned = false;
        if (listsForModelData != null && !listsForModelData.isEmpty()) {
            for (ListManagementList listManagementList : listsForModelData) {
                for (ListManagementListUser listUser : listManagementList.getUsers()) {
                    if (userName.equalsIgnoreCase(listUser.getUserId())) {
                        found = true;
                        if (listUser.isAssigned()) {
                            assigned = true;
                        }
                        break;
                    }
                }
                if (found && assigned) {
                    break;
                }
            }
        }

        if (isForSpecific) {
            if (!found) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            if (hasQuorumBeenReachedForWipAssignment(rulesModelData, listsForModelData, uniqueListOfApprovers, sortedListModelMap)) {
                response.setResponse(AssignedTypes.QUORUM_REACHED);
            }
            if (assigned) {
                if (response.getResponse() != null && AssignedTypes.QUORUM_REACHED.equals(response.getResponse())) {
                    response.setResponse(AssignedTypes.QUORUM_REACHED_AND_ASSIGNED);
                } else {
                    response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                }
                return response;
            }
            if (response.getResponse() != null && AssignedTypes.QUORUM_REACHED.equals(response.getResponse())) {
                return response;
            }
            if (!quorumNotReachedForWipAssignmentAndCanUserBeAssigned(rulesModelData, listsForModelData, userName, uniqueListOfApprovers,
                    sortedListModelMap)) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            if (!isAllRequiredListsPopulated(rulesModelData, listsForModelData, userName, uniqueListOfApprovers, sortedListModelMap)) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            if (usersResponse != null && usersResponse.getUsers() != null && !usersResponse.getUsers().isEmpty()) {
                for (ListManagementListUser listUser : usersResponse.getUsers()) {
                    if (listUser.isAssigned() && userName.equalsIgnoreCase(listUser.getUserId())) {
                        response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                        return response;
                    }
                }
            }
            response.setResponse(AssignedTypes.CAN_BE_ASSIGNED);
        } else {
            if (assigned) {
                response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                return response;
            }
            response.setResponse(AssignedTypes.CAN_BE_ASSIGNED);
        }
        return response;
    }

    @Override
    public AssignResponse checkIfChairpersonWorkflowCanBeAssigned(RulesResponse rulesResponse, UsersResponse usersResponse,
                                                                  String userName, boolean isForSpecific, int workflowId) throws ResourceException {
        AssignResponse response = new AssignResponse().withResponse(null);

        RulesModelData rulesModelData = validateAndGetSelectedRulesResponse(rulesResponse);
        List<ListManagementList> listsForModelData = getListManagementListUsersForRulesModelData(rulesResponse.getListManagementList(),
                rulesModelData);


        for (ListManagementList listManagementList : listsForModelData) {
            if(listManagementList.isChairpersonList()){
                List<ApprovalForumWfaWorkflowApprovalsEntity> assignedApprovals =
                        approvalForumDataRepository.getAssignedApprovalsForWorkflowIdAndListName(workflowId, listManagementList.getListManagementName());
                List<String> assignedChairpersonUsers = new ArrayList<>();
                if(assignedApprovals != null){
                    assignedChairpersonUsers = assignedApprovals.stream()
                            .filter(line -> line.isChairpersonApprover())
                            .map(ApprovalForumWfaWorkflowApprovalsEntity::getUserId)
                            .collect(Collectors.toList());
                }
                for (ListManagementListUser listUser : listManagementList.getUsers()){
                    if(assignedChairpersonUsers.contains(listUser.getUserId())){
                        listUser.setAssigned(true);
                    }
                }
            }
        }

        setDynamicMaxForRules(rulesModelData, listsForModelData);
        List<String> uniqueListOfApprovers = getUniqueListOfApproversFromRulesResponse(listsForModelData, rulesModelData);
        Map<Integer, RulesResponseListModelData> sortedListModelMap = getSortedListModelMap(rulesModelData);



        boolean found = false;
        boolean assigned = false;
        if (listsForModelData != null && !listsForModelData.isEmpty()) {
            for (ListManagementList listManagementList : listsForModelData) {
                for (ListManagementListUser listUser : listManagementList.getUsers()) {
                    if (userName.equalsIgnoreCase(listUser.getUserId())) {
                        found = true;
                        if (listUser.isAssigned()) {
                            assigned = true;
                        }
                        break;
                    }
                }
                if (found && assigned) {
                    break;
                }
            }
        }

        if (isForSpecific) {
            if (!found) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            if (hasQuorumBeenReachedForWipAssignment(rulesModelData, listsForModelData, uniqueListOfApprovers, sortedListModelMap)) {
                response.setResponse(AssignedTypes.QUORUM_REACHED);
            }
            if (assigned) {
                if (response.getResponse() != null && AssignedTypes.QUORUM_REACHED.equals(response.getResponse())) {
                    response.setResponse(AssignedTypes.QUORUM_REACHED_AND_ASSIGNED);
                } else {
                    response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                }
                return response;
            }
            if (response.getResponse() != null && AssignedTypes.QUORUM_REACHED.equals(response.getResponse())) {
                return response;
            }
            if (!quorumNotReachedForWipAssignmentAndCanUserBeAssigned(rulesModelData, listsForModelData, userName, uniqueListOfApprovers,
                    sortedListModelMap)) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            if (!isAllRequiredListsPopulated(rulesModelData, listsForModelData, userName, uniqueListOfApprovers, sortedListModelMap)) {
                response.setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
                return response;
            }
            if (usersResponse != null && usersResponse.getUsers() != null && !usersResponse.getUsers().isEmpty()) {
                for (ListManagementListUser listUser : usersResponse.getUsers()) {
                    if (listUser.isAssigned() && userName.equalsIgnoreCase(listUser.getUserId())) {
                        response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                        return response;
                    }
                }
            }
            response.setResponse(AssignedTypes.CAN_BE_ASSIGNED);
        } else {
            if (assigned) {
                response.setResponse(AssignedTypes.ALREADY_ASSIGNED);
                return response;
            }
            response.setResponse(AssignedTypes.CAN_BE_ASSIGNED);
        }
        return response;
    }

    private void setDynamicMaxForRules(RulesModelData rulesModelData, List<ListManagementList> listManagementLists)
            throws ResourceException {

        int total = rulesModelData.getTotalNumberOfApprovalsRequired();
        int min = 0;

        //Loop through and get number of min approvers accross all lists
        for (RulesResponseListModelData data : rulesModelData.getListModelData()) {
            min += data.getMinRequired();
        }
        int spare = total - min;

        List<String> uniqueListOfApprovers = new ArrayList<>();
        Map<Integer, RulesResponseListModelData> sortedListModelMap = getSortedListModelMap(rulesModelData);
        for (Integer key : sortedListModelMap.keySet()) {
            if (spare == 0) continue;
            RulesResponseListModelData rulesResponseListModelData = sortedListModelMap.get(key);
            ListManagementList listManagementList = getListManagementListFromListForName(listManagementLists,
                    rulesResponseListModelData.getListName());
            int listTotal = 0;
            for (ListManagementListUser listUser : listManagementList.getUsers()) {
                if (listUser.isAssigned() && uniqueListOfApprovers.indexOf(listUser.getUserId()) < 0) {
                    uniqueListOfApprovers.add(listUser.getUserId());
                    listTotal++;
                }
            }

            if (listTotal > 0) {
                if (rulesResponseListModelData.getMaxRequired() == 0) {
                    if (rulesResponseListModelData.getMinRequired() > 0) {
                        spare = spare - listTotal + rulesResponseListModelData.getMinRequired();
                    }
                }
                if (rulesResponseListModelData.getMinRequired() == 0) {
                    if (rulesResponseListModelData.getMaxRequired() > 0) {
                        spare = spare - listTotal;
                    }
                }
            }
        }

        uniqueListOfApprovers = new ArrayList<>();
        for (Integer key : sortedListModelMap.keySet()) {
            RulesResponseListModelData rulesResponseListModelData = sortedListModelMap.get(key);
            ListManagementList listManagementList = getListManagementListFromListForName(listManagementLists,
                    rulesResponseListModelData.getListName());
            int listTotal = 0;
            for (ListManagementListUser listUser : listManagementList.getUsers()) {
                if (listUser.isAssigned() && uniqueListOfApprovers.indexOf(listUser.getUserId()) < 0) {
                    uniqueListOfApprovers.add(listUser.getUserId());
                    listTotal++;
                }
            }
            if (rulesResponseListModelData.getMaxRequired() == 0) {
                if (listTotal > rulesResponseListModelData.getMinRequired()) {
                    if (rulesResponseListModelData.getMinRequired() == 0) {
                        rulesResponseListModelData.setMinRequired(listTotal);
                    }
                    rulesResponseListModelData.setMaxRequired(listTotal + spare);
                } else {
                    rulesResponseListModelData.setMaxRequired(rulesResponseListModelData.getMinRequired() + spare);
                    if (rulesResponseListModelData.getMaxRequired() > total) {
                        rulesResponseListModelData.setMaxRequired(total);
                    }
                }
            } else {
                if (rulesResponseListModelData.getMinRequired() == 0 && spare == 0) {
                    if (listTotal > 0) {
                        rulesResponseListModelData.setMaxRequired(listTotal);
                    } else {
                        rulesResponseListModelData.setMaxRequired(0);
                    }
                }
            }
        }
    }

    private void setDynamicMaxForRulesGeneral(ApprovalForumWfqWorkflowQuorumEntity selectedWorkflowQuorumEntity,
                                              List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities,
                                              List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities,
                                              Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> sortedListModelMap) {

        int total = selectedWorkflowQuorumEntity.getTotalNumberOfApprovalsRequired();
        int min = 0;

        //Loop through and get number of min approvers accross all lists
        for (ApprovalForumWfqdWorkflowQuorumDataEntity entity : quorumDataEntities) {
            min += entity.getMinRequired();
        }
        int spare = total - min;

        List<String> uniqueListOfApprovers = new ArrayList<>();
        for (Integer key : sortedListModelMap.keySet()) {
            if (spare == 0) continue;
            ApprovalForumWfqdWorkflowQuorumDataEntity quorumDataEntity = sortedListModelMap.get(key);
            int listTotal = 0;
            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumApprovalsEntities) {
                if (approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName().equalsIgnoreCase(
                        quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName())) {
                    if (approvalsEntity.getDateAssigned() != null && uniqueListOfApprovers.indexOf(approvalsEntity.getUserId()) < 0) {
                        uniqueListOfApprovers.add(approvalsEntity.getUserId());
                        listTotal++;
                    }
                }
            }

            if (listTotal > 0) {
                if (quorumDataEntity.getMaxRequired() == 0) {
                    if (quorumDataEntity.getMinRequired() > 0) {
                        spare = spare - listTotal + quorumDataEntity.getMinRequired();
                    }
                }
                if (quorumDataEntity.getMinRequired() == 0) {
                    if (quorumDataEntity.getMaxRequired() > 0) {
                        spare = spare - listTotal;
                    }
                }
            }
        }

        uniqueListOfApprovers = new ArrayList<>();
        for (Integer key : sortedListModelMap.keySet()) {
            ApprovalForumWfqdWorkflowQuorumDataEntity quorumDataEntity = sortedListModelMap.get(key);
            int listTotal = 0;
            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumApprovalsEntities) {
                if (approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName().equalsIgnoreCase(
                        quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName())) {
                    if (approvalsEntity.getDateAssigned() != null && uniqueListOfApprovers.indexOf(approvalsEntity.getUserId()) < 0) {
                        uniqueListOfApprovers.add(approvalsEntity.getUserId());
                        listTotal++;
                    }
                }
            }
            if (quorumDataEntity.getMaxRequired() == 0) {
                if (listTotal > quorumDataEntity.getMinRequired()) {
                    if (quorumDataEntity.getMinRequired() == 0) {
                        quorumDataEntity.setMinRequired(listTotal);
                    }
                    quorumDataEntity.setMaxRequired(listTotal + spare);
                } else {
                    quorumDataEntity.setMaxRequired(quorumDataEntity.getMinRequired() + spare);
                    if (quorumDataEntity.getMaxRequired() > total) {
                        quorumDataEntity.setMaxRequired(total);
                    }
                }
            } else {
                if (quorumDataEntity.getMinRequired() == 0 && spare == 0) {
                    if (listTotal > 0) {
                        quorumDataEntity.setMaxRequired(listTotal);
                    } else {
                        quorumDataEntity.setMaxRequired(0);
                    }
                }
            }
        }
    }

    private boolean hasQuorumBeenReachedForAssignment(int workflowId) {
        return hasQuorumBeenReachedForAssignment(getQuorumCheckData(workflowId));
    }

    private boolean hasQuorumBeenReachedForAssignment(QuorumCheckData quorumCheckData) {
        return hasQuorumBeenReached(false, quorumCheckData);
    }

    private boolean hasQuorumBeenReachedForApprovals(int workflowId) {
        return hasQuorumBeenReachedForApprovals(getQuorumCheckData(workflowId));
    }

    private boolean hasQuorumBeenReachedForApprovals(QuorumCheckData quorumCheckData) {
        return hasQuorumBeenReached(true, quorumCheckData);
    }

    private boolean hasQuorumBeenReached(boolean isForApprovals,
                                         QuorumCheckData quorumCheckData) {
        List<String> uniqueListOfApprovers = getUniqueListOfApproversFromApproversEntity
                (quorumCheckData.getSelectedWorkflowQuorumEntity(), isForApprovals);
        int totalRequired = quorumCheckData.getSelectedWorkflowQuorumEntity().getTotalNumberOfApprovalsRequired();
        if (isForApprovals) {
            if (quorumCheckData.getQuorumAdditionalApprovalsEntities() != null && !
                    quorumCheckData.getQuorumAdditionalApprovalsEntities().isEmpty()) {
                totalRequired += quorumCheckData.getQuorumAdditionalApprovalsEntities().size();
            }
        }
        boolean quorum = true;
        List<String> assignedUniqueListOfApprovers = new ArrayList<>();
        for (Integer key : quorumCheckData.getSortedListModelMap().keySet()) {
            if (quorum) {
                quorum = checkForQuorumOnList(quorumCheckData.getSortedListModelMap().get(key),
                        quorumCheckData.getSelectedWorkflowQuorumEntity().getWfApprovalForumWorkflowByWfId().getWfId(), isForApprovals,
                        uniqueListOfApprovers.size(), totalRequired, assignedUniqueListOfApprovers);
            }
        }
        if (quorum) {
            return (uniqueListOfApprovers.size() >= totalRequired);
        }
        return quorum;
    }

    private Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> getSortedListModelMap(
            List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities) {
        Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> sortedListModelMap = new TreeMap<>();
        for (ApprovalForumWfqdWorkflowQuorumDataEntity entity : quorumDataEntities) {
            sortedListModelMap.put(entity.getRank(), entity);
        }
        return sortedListModelMap;
    }

    private boolean checkForQuorumOnList(ApprovalForumWfqdWorkflowQuorumDataEntity quorumEntity, int workflowId,
                                         boolean isForApprovals, int uniqueListOfApprovers, int totalNumRequired, List<String> assignedUniqueListOfApprovers) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities;
        int count = 0;
        if (isForApprovals) {
            approvalsEntities = approvalForumDataRepository.getCompletedApprovalsForWorkflowIdAndListName(workflowId,
                    quorumEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName());
        } else {
            approvalsEntities = approvalForumDataRepository.getAssignedApprovalsForWorkflowIdAndListName(workflowId,
                    quorumEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName());
        }
        for (ApprovalForumWfaWorkflowApprovalsEntity entity : approvalsEntities) {
            if (!assignedUniqueListOfApprovers.contains(entity.getUserId())) {
                count++;
                assignedUniqueListOfApprovers.add(entity.getUserId());
            }
        }
        return checkForQuorum(count, quorumEntity.getMinRequired(), quorumEntity.getMaxRequired(),
                totalNumRequired, uniqueListOfApprovers);
    }

    private boolean quorumNotReachedForAssignmentAndCanUserBeAssigned(ApprovalForumWfqWorkflowQuorumEntity entity,
                                                                      String userId,
                                                                      List<String> uniqueListOfApprovers,
                                                                      Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> sortedListModelMap) {
        List<String> assignedUniqueListOfApprovers = new ArrayList<>();
        for (Integer key : sortedListModelMap.keySet()) {
            if (!checkForQuorumOnList(sortedListModelMap.get(key), entity.getWfApprovalForumWorkflowByWfId().getWfId(),
                    false, uniqueListOfApprovers.size(), entity.getTotalNumberOfApprovalsRequired(), assignedUniqueListOfApprovers)) {
                if (canUserBeAssignedToList(sortedListModelMap.get(key), userId,
                        entity.getWfApprovalForumWorkflowByWfId().getWfId()))
                    return true;
            }
        }
        return false;
    }

    private boolean canUserBeAssignedToList(ApprovalForumWfqdWorkflowQuorumDataEntity quorumEntity, String userId,
                                            int workflowId) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository
                .getApprovalsForWorkflowIdAndListName(workflowId,
                        quorumEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName());
        for (ApprovalForumWfaWorkflowApprovalsEntity userEntity : approvalsEntities) {
            if (userEntity.getUserId().equalsIgnoreCase(userId) && userEntity.getDateAssigned() == null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasQuorumBeenReachedForWipAssignment(RulesModelData modelData,
                                                         List<ListManagementList> listManagementLists,
                                                         List<String> uniqueListOfApprovers,
                                                         Map<Integer, RulesResponseListModelData> sortedListModelMap) throws ResourceException {
        boolean quorum = true;
        for (Integer key : sortedListModelMap.keySet()) {
            if (quorum) {
                quorum = checkForQuorumOnWipList(sortedListModelMap.get(key), uniqueListOfApprovers.size(),
                        modelData.getTotalNumberOfApprovalsRequired(), getListManagementListFromListForName(
                                listManagementLists, sortedListModelMap.get(key).getListName()));
            }
        }
        if (quorum) {
            return (uniqueListOfApprovers.size() >= modelData.getTotalNumberOfApprovalsRequired());
        }
        return quorum;
    }

    private List<ListManagementList> getListManagementListUsersForRulesModelData(
            List<ListManagementList> listManagementLists, RulesModelData modelData) throws ResourceException {
        List<ListManagementList> newList = new ArrayList<>();
        for (RulesResponseListModelData rulesResponseListModelData : modelData.getListModelData()) {
            newList.add(getListManagementListFromListForName(listManagementLists,
                    rulesResponseListModelData.getListName()));
        }
        return newList;
    }

    private Map<Integer, RulesResponseListModelData> getSortedListModelMap(RulesModelData modelData) {
        Map<Integer, RulesResponseListModelData> sortedListModelMap = new TreeMap<>();
        for (RulesResponseListModelData listModelData : modelData.getListModelData()) {
            sortedListModelMap.put(listModelData.getRank(), listModelData);
        }
        return sortedListModelMap;
    }

    private List<String> getUniqueListOfApproversFromApproversEntity(ApprovalForumWfqWorkflowQuorumEntity quorumEntity, boolean isForApprovals) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities;
        List<String> uniqueListOfApprovers = new ArrayList<>();
        for (ApprovalForumWfqdWorkflowQuorumDataEntity quorumDataEntity : approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(quorumEntity.getWfqId())) {
            if (isForApprovals) {
                approvalsEntities = approvalForumDataRepository.getCompletedApprovalsForWorkflowIdAndListName(
                        quorumEntity.getWfApprovalForumWorkflowByWfId().getWfId(),
                        quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName());
            } else {
                approvalsEntities = approvalForumDataRepository.getAssignedApprovalsForWorkflowIdAndListName(
                        quorumEntity.getWfApprovalForumWorkflowByWfId().getWfId(),
                        quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName());
            }
            for (ApprovalForumWfaWorkflowApprovalsEntity entity : approvalsEntities) {
                if (uniqueListOfApprovers.indexOf(entity.getUserId()) < 0) {
                    uniqueListOfApprovers.add(entity.getUserId());
                }
            }
        }
        if (isForApprovals) {
            for (ApprovalForumWfaWorkflowApprovalsEntity entity :
                    approvalForumDataRepository.getCompletedAdditionalApprovalsForWorkflowId(
                            quorumEntity.getWfApprovalForumWorkflowByWfId().getWfId())) {
                if (uniqueListOfApprovers.indexOf(entity.getUserId()) < 0) {
                    uniqueListOfApprovers.add(entity.getUserId());
                }
            }
        }
        return uniqueListOfApprovers;
    }

    private List<String> getUniqueListOfApproversFromRulesResponse(List<ListManagementList> listManagementLists,
                                                                   RulesModelData modelData) throws ResourceException {
        List<String> uniqueListOfApprovers = new ArrayList<>();
        Map<Integer, RulesResponseListModelData> sortedListModelMap = getSortedListModelMap(modelData);
        for (Integer key : sortedListModelMap.keySet()) {
            ListManagementList listManagementList = getListManagementListFromListForName(listManagementLists,
                    sortedListModelMap.get(key).getListName());
            for (ListManagementListUser listUser : listManagementList.getUsers()) {
                if (listUser.isAssigned() && uniqueListOfApprovers.indexOf(listUser.getUserId()) < 0) {
                    uniqueListOfApprovers.add(listUser.getUserId());
                }
            }
        }
        return uniqueListOfApprovers;
    }

    private ListManagementList getListManagementListFromListForName(List<ListManagementList> listManagementLists,
                                                                    String name) throws ResourceException {
        for (ListManagementList listManagementList : listManagementLists) {
            if (listManagementList.getListManagementName().equalsIgnoreCase(name)) {
                return listManagementList;
            }
        }
        throw new ResourceException("Could not find ListManagementList in list for name: " + name);
    }

    private boolean checkForQuorumOnWipList(RulesResponseListModelData modelData, int uniqueListOfApprovers,
                                            int totalNumberRequired, ListManagementList listManagementList) {
        int numberOfApprovers = 0;
        for (ListManagementListUser listUser : listManagementList.getUsers()) {
            if (listUser.isAssigned()) {
                numberOfApprovers++;
            }
        }
        return checkForQuorum(numberOfApprovers, modelData.getMinRequired(), modelData.getMaxRequired(),
                totalNumberRequired, uniqueListOfApprovers);
    }

    private boolean quorumNotReachedForWipAssignmentAndCanUserBeAssigned(RulesModelData modelData,
                                                                         List<ListManagementList> listManagementList,
                                                                         String userId,
                                                                         List<String> uniqueListOfApprovers,
                                                                         Map<Integer, RulesResponseListModelData> sortedListModelMap)
            throws ResourceException {
        for (Integer key : sortedListModelMap.keySet()) {
            ListManagementList list = getListManagementListFromListForName(listManagementList,
                    sortedListModelMap.get(key).getListName());
            if (!checkForQuorumOnWipList(sortedListModelMap.get(key), uniqueListOfApprovers.size(),
                    modelData.getTotalNumberOfApprovalsRequired(), list)) {
                if (canUserBeAssignedToList(list, userId))
                    return true;
            }
        }
        return false;
    }

    private boolean isAllRequiredListsPopulated(RulesModelData modelData,
                                                List<ListManagementList> listManagementList,
                                                String userId, List<String> uniqueListOfApprovers,
                                                Map<Integer, RulesResponseListModelData> sortedListModelMap)
            throws ResourceException {
        int availableToAssign = modelData.getTotalNumberOfApprovalsRequired() - uniqueListOfApprovers.size();
        int listCount;
        boolean isAvailableToAssign;
        for (Integer key : sortedListModelMap.keySet()) {
            listCount = 0;
            isAvailableToAssign = false;
            RulesResponseListModelData listModelData = sortedListModelMap.get(key);
            ListManagementList list = getListManagementListFromListForName(listManagementList,
                    listModelData.getListName());
            for (ListManagementListUser listUser : list.getUsers()) {
                if (uniqueListOfApprovers.contains(listUser.getUserId())) {
                    listCount++;
                }
                if (listUser.getUserId().equalsIgnoreCase(userId) && !listUser.isAssigned()) {
                    isAvailableToAssign = true;
                }
            }
            if (listModelData.getMinRequired() > 0 && listCount < listModelData.getMinRequired()) {
                if (!isAvailableToAssign && (availableToAssign - (listModelData.getMinRequired() - listCount)) <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAllRequiredListsPopulated(ApprovalForumWfqWorkflowQuorumEntity selectedWorkflowQuorumEntity,
                                                String userId, List<String> uniqueListOfApprovers,
                                                List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities,
                                                Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> sortedListModelMap) {
        int availableToAssign = selectedWorkflowQuorumEntity.getTotalNumberOfApprovalsRequired() - uniqueListOfApprovers.size();
        int listCount;
        boolean isAvailableToAssign;
        for (Integer key : sortedListModelMap.keySet()) {
            listCount = 0;
            isAvailableToAssign = false;
            ApprovalForumWfqdWorkflowQuorumDataEntity quorumDataEntity = sortedListModelMap.get(key);
            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : quorumApprovalsEntities) {
                if (approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName().equalsIgnoreCase(
                        quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getListName())) {
                    if (uniqueListOfApprovers.contains(approvalsEntity.getUserId())) {
                        listCount++;
                    }
                    if (approvalsEntity.getUserId().equalsIgnoreCase(userId) && approvalsEntity.getDateAssigned() == null) {
                        isAvailableToAssign = true;
                    }
                }
            }
            if (quorumDataEntity.getMinRequired() > 0 && listCount < quorumDataEntity.getMinRequired()) {
                if (!isAvailableToAssign && (availableToAssign - (quorumDataEntity.getMinRequired() - listCount)) <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canUserBeAssignedToList(ListManagementList listManagementList, String userId) {
        for (ListManagementListUser listUser : listManagementList.getUsers()) {
            if (listUser.getUserId().equalsIgnoreCase(userId) && !listUser.isAssigned()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkForQuorum(int numberOfApprovers, int minRequired, int maxRequired, int totalNumberRequired,
                                   int uniqueListOfApprovers) {
        if (maxRequired == 0) {
            return true;
        }
        if (minRequired > 0 && maxRequired > 0) {
            return ((numberOfApprovers >= minRequired && numberOfApprovers >= maxRequired)
                    || uniqueListOfApprovers >= totalNumberRequired);
        } else if (minRequired == 0 && maxRequired > 0) {
            return (numberOfApprovers >= maxRequired || uniqueListOfApprovers >= totalNumberRequired);
        } else {
            return (uniqueListOfApprovers >= totalNumberRequired);
        }
    }

    @Override
    public AssignResponse assignWorkflow(int workflowId, String userName) {
        approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName).forEach(entity -> {
            entity.setDateAssigned(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            approvalForumDataRepository.updateWorkflowApproval(entity);
        });
        setAuditAndLastUpdatedDetails(userName, "Assigned workflow to " + userName, workflowId);
        return new AssignResponse().withResponse(AssignedTypes.ASSIGNED);
    }

    @Override
    public AssignResponse assignWipWorkflow(int workflowId, String userName) {
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        entity.setAssignedTo(userName);
        entity.setAssignedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        approvalForumDataRepository.updateWorkflow(entity);
        setAuditAndLastUpdatedDetails(userName, "Assigned wip workflow to " + userName, workflowId);
        return new AssignResponse().withResponse(AssignedTypes.ASSIGNED);
    }

    @Override
    public AssignResponse unAssignWorkflow(int workflowId, String userName) {
        approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName).forEach(entity -> {
            entity.setDateAssigned(null);
            approvalForumDataRepository.updateWorkflowApproval(entity);
        });
        setAuditAndLastUpdatedDetails(userName, "Un Assigned workflow from " + userName, workflowId);
        return new AssignResponse().withResponse(AssignedTypes.UN_ASSIGNED);
    }

    @Override
    public AssignResponse unAssignWipWorkflow(int workflowId, String userName) throws ResourceException {
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        entity.setAssignedTo(null);
        entity.setAssignedOn(null);
        approvalForumDataRepository.updateWorkflow(entity);
        setAuditAndLastUpdatedDetails(userName, "Un Assigned wip workflow from " + userName, workflowId);
        return new AssignResponse().withResponse(AssignedTypes.UN_ASSIGNED);
    }

    @Override
    public void updateWorkflowDecision(int workflowId, String userName, UpdateDecisionRequestDTO updateDecisionRequestDTO) {
        approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName).forEach(entity -> {
            entity.setDecision(updateDecisionRequestDTO.getDecision() != null ? updateDecisionRequestDTO.getDecision().value() : null);
            entity.setApprovalConditions(updateDecisionRequestDTO.getConditions());
            if (updateDecisionRequestDTO.getDecision() != null &&
                    ApprovalDecision.ESCALATED.equals(updateDecisionRequestDTO.getDecision())) {
                entity.setRecommendedPolicyApprovalMandate(updateDecisionRequestDTO.getPolicyApprovalMandate());
            }
            approvalForumDataRepository.updateWorkflowApproval(entity);
        });
        setAuditAndLastUpdatedDetails(userName, "Update workflow decision for " + userName, workflowId);
    }

    @Override
    public void submitWorkflow(int workflowId, String userName, SubmitDecisionRequestDTO submitDecisionRequestDTO)
            throws ResourceException {
        logger.info("submitWorkflow workflowId:{}, submitDecisionRequestDTO:{}, userName:{}",workflowId,submitDecisionRequestDTO.getDecision().value(), userName);
        approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName).forEach(entity -> {
            entity.setDateCompleted(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            entity.setDecision(submitDecisionRequestDTO.getDecision().value());
            entity.setApprovalConditions(submitDecisionRequestDTO.getConditions());
            if (submitDecisionRequestDTO.getDecision() != null &&
                    ApprovalDecision.ESCALATED.equals(submitDecisionRequestDTO.getDecision())) {
                entity.setRecommendedPolicyApprovalMandate(submitDecisionRequestDTO.getPolicyApprovalMandate());
            }
            logger.info("submitWorkflow workflowId:{}, entity:{}, ",workflowId,entity.getDecision());
            approvalForumDataRepository.updateWorkflowApproval(entity);
        });
        setAuditAndLastUpdatedDetails(userName, "Submitted workflow decision for " + userName, workflowId);
        updateWorkflowStatusAfterSubmit(submitDecisionRequestDTO, workflowId, userName);
    }

    private void updateWorkflowStatusAfterSubmit(SubmitDecisionRequestDTO submitDecisionRequestDTO, int workflowId,
                                                 String userName) throws ResourceException {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        if (ApprovalDecision.CONDITIONALLY_APPROVED.equals(submitDecisionRequestDTO.getDecision())
                || ApprovalDecision.DECLINED.equals(submitDecisionRequestDTO.getDecision())
                || ApprovalDecision.ESCALATED.equals(submitDecisionRequestDTO.getDecision())
                || ApprovalDecision.REFERRED_BACK.equals(submitDecisionRequestDTO.getDecision())) {
            if (ApprovalDecision.CONDITIONALLY_APPROVED.equals(submitDecisionRequestDTO.getDecision())) {
                if (!ApprovalDecision.CONDITIONALLY_APPROVED.value().equals(workflowEntity.getStatus())) {
                    workflowEntity.setStatus(submitDecisionRequestDTO.getDecision().value());
                    approvalForumDataRepository.updateWorkflow(workflowEntity);
                    setAuditAndLastUpdatedDetails(userName,
                            "Workflow Status Updated to " + submitDecisionRequestDTO.getDecision().value(), workflowId);
                }
            } else {
                workflowEntity.setStatus(submitDecisionRequestDTO.getDecision().value());
                setAuditAndLastUpdatedDetails(userName,
                        "Workflow Status Updated to " + submitDecisionRequestDTO.getDecision().value(), workflowId);
                    finaliseWorkflow(workflowEntity, submitDecisionRequestDTO.getDecision(), submitDecisionRequestDTO.getPolicyApprovalMandate());
            }
        }
        if (ApprovalDecision.CONDITIONALLY_APPROVED.equals(submitDecisionRequestDTO.getDecision())
                || ApprovalDecision.APPROVED.equals(submitDecisionRequestDTO.getDecision())) {
            logger.info("submitWorkflow APPROVED:{}, userName:{}",workflowId,userName);
            if (hasQuorumBeenReachedForApprovals(workflowId)) {
                logger.info("submitWorkflow hasQuorumBeenReachedForApprovals:{}",workflowId);
                if (!ApprovalDecision.CONDITIONALLY_APPROVED.value().equals(workflowEntity.getStatus())) {
                    workflowEntity.setStatus(submitDecisionRequestDTO.getDecision().value());
                }
                try {
                    logger.info("submitWorkflow hasQuorumBeenReachedForApprovals else:{}",workflowId);
                    finaliseWorkflow(workflowEntity, submitDecisionRequestDTO.getDecision(), null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


     private void finaliseWorkflow(ApprovalForumWfWorkflowEntity workflowEntity,
                                  ApprovalDecision decision,
                                  String policyApprovalMandate) throws ResourceException {
         String meetingName = approvalForumDataRepository.getMeetingNameByWorkflowId(workflowEntity.getWfId());
         logger.info("finaliseWorkflow meetingName:{}",meetingName);
         if(meetingName.contains("Delegated Authority 4")){
             logger.info("finaliseWorkflow for Delegated Authority 4:{}",meetingName);
             workflowEntity.setStatus(decision.value());
         }
        setToInactive(workflowEntity);
        logger.info("finaliseWorkflow decision: {}", decision);

        if (ApprovalDecision.ESCALATED.equals(decision)) {  logger.info("finaliseWorkflow decision1: {}", decision);
            if (!isMandateHandledInDaf(policyApprovalMandate)) {
                logger.info("finaliseWorkflow decision2: {}", decision);
                handleEscalationOutsideDaf(workflowEntity);
            } else {
                logger.info("finaliseWorkflow decision3: {}", decision);
                handleEscalationInsideDaf(workflowEntity, policyApprovalMandate);
            }
        } else {
            logger.info("finaliseWorkflow decision4: {}", decision);
            handleNonEscalatedWorkflow(workflowEntity, decision.value());
        }
    }

    private void handleEscalationOutsideDaf(ApprovalForumWfWorkflowEntity workflowEntity) throws ResourceException  {
        setTenantIdAndGeneratePdf(workflowEntity);
        approvalForumMailService.sendRequestCompleteMail(workflowEntity.getWfId());
    }

    private void handleEscalationInsideDaf(ApprovalForumWfWorkflowEntity workflowEntity, String mandate) throws ResourceException {
        workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId().forEach(detail -> {
            boolean isLumos = detail.getWfadApprovalForumWorkflowApplicationDataByWfadId().getLumos();
            int applicationId = detail.getWfadApprovalForumWorkflowApplicationDataByWfadId().getApplicationId();

            ProcessExternalRequestResponse response = isLumos
                    ? processLumosExternalRequestsCreate(applicationId, workflowEntity.getWfId(), mandate, getTenantIdForWfId(workflowEntity.getWfId()))
                    : processCasdmExternalRequestsCreate(applicationId, workflowEntity.getWfId(), mandate);

            saveChatsAndAttachmentsToNewRequest(workflowEntity.getWfId(), response.getWorkflowId());
        });

        setTenantIdAndGeneratePdf(workflowEntity);
        approvalForumMailService.sendRequestCompleteMail(workflowEntity.getWfId());
    }

    private void handleNonEscalatedWorkflow(ApprovalForumWfWorkflowEntity workflowEntity, String value) throws ResourceException {
        logger.info("handleNonEscalatedWorkflow workflow id: {}", workflowEntity.getWfId());
        setTenantIdAndGeneratePdf(workflowEntity);
        approvalForumMailService.sendRequestCompleteMail(workflowEntity.getWfId());

        if (isLumosTenantPersistable(workflowEntity)) {
            ApprovalForumDsrDocStashRequestsEntity docEntity = createDocStashRequest(workflowEntity.getWfId());
            logger.info("handleNonEscalatedWorkflow isLumosTenantPersistable id: {}", workflowEntity.getWfId());
            approvalForumDataRepository.persistApprovalForumDsrDocStashRequestsEntity(docEntity);
            return;
        }
        String meetingName = getMeetingNameByWorkflowId(workflowEntity.getWfId());
        logger.info("handleNonEscalatedWorkflow workflow meetingName: {}", meetingName);
        String inkombaId = approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowEntity.getWfId()).get(0).getCreditInkombaId();

        logger.info("Lumos Broader Africa workflow inkombaId: {}",inkombaId );
        if (inkombaId == null || inkombaId.isEmpty()) {
            logger.warn("Inkomba ID not found for workflowId: {}", workflowEntity.getWfId());
            return;
        }
        int decisionId = resolveDecisionId(value);
        logger.info("decisionId for workflowId: {}", decisionId);
        //boolean isLumosBroaderAfricaWorkflowUpdated=lumosRestService.processLumosBroaderAfricaWorkflow(inkombaId,meetingName, decisionId, workflowEntity.getSpecificReason());
        if (inkombaId != null && !inkombaId.trim().isEmpty()
                && meetingName != null && !meetingName.trim().isEmpty()
                && decisionId > 0
                /*&& workflowEntity.getSpecificReason() != null && !workflowEntity.getSpecificReason().trim().isEmpty()*/) {
            logger.info("inside if decision: {}", decisionId);
            String specificReson = workflowEntity.getSpecificReason().trim().isEmpty()?"no comments available":workflowEntity.getSpecificReason();
            boolean isLumosBroaderAfricaWorkflowUpdated = lumosRestService.processLumosBroaderAfricaWorkflow(
                    inkombaId,
                    meetingName,
                    decisionId,
                    specificReson
            );

            logger.info("Lumos Broader Africa workflow update status: {}", isLumosBroaderAfricaWorkflowUpdated);

        } else {
            String msg = "Missing required parameters for processing Lumos Broader Africa workflow, InkombaId:" + inkombaId + ",meetingName:" + meetingName+",decisionId:" + decisionId+",SpecificReason:" + workflowEntity.getSpecificReason();
            logger.warn(msg);
            throw new ResourceException(msg);
        }
    }

    private ApprovalForumDsrDocStashRequestsEntity createDocStashRequest(int workflowId) {
        ApprovalForumDsrDocStashRequestsEntity docEntity = new ApprovalForumDsrDocStashRequestsEntity();
        docEntity.setRetryCount(0);
        docEntity.setComplete(false);
        docEntity.setCreatedOn(new Timestamp(System.currentTimeMillis()));
        docEntity.setWfId(workflowId);
        return docEntity;
    }

    private int resolveDecisionId(String decision) {
        //List<ApprovalForumWfaWorkflowApprovalsEntity> approvals = approvalForumDataRepository.getApprovalsForWorkflowId(workflowId);
        if (decision.isEmpty()) {
            logger.warn("No approval records found for workflowId: {}", decision);
            return 0;
        }

        int decisionId = 0;

        if (decision.equalsIgnoreCase("approved")) {
            logger.info("resolveDecisionId decision: {}", decision);
            return 1;
        } else if(decision.equalsIgnoreCase("conditionally approved")) {
            logger.info("resolveDecisionId decision: {}", decision);
            return 2;
        } else if(decision.equalsIgnoreCase("declined")) {
            logger.info("resolveDecisionId decision: {}", decision);
            return 3;
        } else {
            logger.info("resolveDecisionId decision: {}", decision);
            return 0;
        }
    }

    private String getMeetingNameByWorkflowId(int wfId) {
        RequestData data = null;
        try {
            data = getRequest(wfId, null, true, null);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
        return data.getCounterpartyFullDetail().getCreditApplication().getMeetingName();
    }


    private void setTenantIdAndGeneratePdf(ApprovalForumWfWorkflowEntity workflowEntity) throws ResourceException  {
        String systemName = workflowEntity.getWorkflowSystemName();
        this.afSettings = new HashMap<>(afSettings);
        logger.info("handleNonEscalatedWorkflow workflow systemName: {}", systemName);
        switch (systemName.toUpperCase()) {
            case "ASHBURTON":
                afSettings.put("tenantId", "2");
                generatePdfForAshburton(workflowEntity);
                break;
            case "LUMOS":
                logger.info("handleNonEscalatedWorkflow workflow systemName: {}", systemName);
                generatePdfForBroaderAfricaLumos(workflowEntity);
                break;
            default:
                afSettings.put("tenantId", "1");
                generatePdf(workflowEntity);
                break;
        }
    }

    private boolean isLumosTenantPersistable(ApprovalForumWfWorkflowEntity workflowEntity) {
        if (!workflowEntity.getWorkflowSystemName().equalsIgnoreCase("LUMOS")) return true;

        int tenantId = getTenantIdForWfId(workflowEntity.getWfId());
        if (tenantId > 2) {
            logger.info("It's a Broader Africa tenant, so not persisting it in database.");
            return false;
        }
        return true;
    }


    private void generatePdfForBroaderAfricaLumos(ApprovalForumWfWorkflowEntity workflowEntity) throws ResourceException {
        SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy/MM/dd");
        logger.info("generatePdfForBroaderAfricaLumos workflow id: {}", workflowEntity.getWfId());
        NumberFormat nf = DecimalFormat.getInstance(Locale.ENGLISH);
        DecimalFormat decimalFormatter = (DecimalFormat) nf;
        decimalFormatter.applyPattern("#,###,###.##");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        SimpleDateFormat dateFileFormatter = new SimpleDateFormat("yyyy-MM-dd");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        ReportOutputModel report = new ReportOutputModel();

        RequestData data = getRequest(workflowEntity.getWfId(), null, true, null);

        report.setCurrentDate(simpleDateFormatter.format(new Date()));
        report.setProposalTypeAndAction(data.getCounterpartyFullDetail().getCreditApplication().getProposalType() + " - " + data.getCounterpartyFullDetail().getCreditApplication().getProposalAction());

        // Application Summary
        report.setMeetingName(data.getCounterpartyFullDetail().getCreditApplication().getMeetingName());
        report.setCounterparty(data.getCounterpartyFullDetail().getCounterpartyDetail().getCounterpartyName());
        report.setWholesaleCreditPortfolio(data.getCounterpartyFullDetail().getCreditApplication().getWholesaleCreditPortfolio());
        report.setDistressedStatus(data.getCounterpartyFullDetail().getCreditApplication().getDistressStatus());
        report.setWorkflowId(workflowEntity.getWfId() + "");
        report.setSubmittedBy(data.getSubmittedBy());
        report.setCasdmCreditAnalyst(data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalyst());
        report.setCasdmReviewAnalyst(data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalyst());
        report.setConfidentialStatus(data.isConfidential() ? "Yes" : "No");
        report.setSubmittedDate(dateFormatter.format(data.getSubmittedOn()));

        boolean isProspectWithFacilities = isProspectWithFacilities(data.getCounterpartyFullDetail());
        report.setKycStatus(isProspectWithFacilities ? "KYC Process Incomplete" : "KYC Process Complete");

        if (null == data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystSignatureDate()) {
            report.setCasdmCaSubmittedDate("-");
        } else {
            report.setCasdmCaSubmittedDate(dateFormatter.format(data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystSignatureDate()));
        }

        if (null == data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystSignatureDate()) {
            report.setCasdmRaSubmittedDate("-");
        } else {
            report.setCasdmRaSubmittedDate(dateFormatter.format(data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystSignatureDate()));
        }

        // Ratings
        report.setCurrentFrRating(data.getCounterpartyFullDetail().getCreditApplication().getLastApprovedFrRating());
        report.setProposedFrRating(data.getCounterpartyFullDetail().getCreditApplication().getProposedFrRating());

        // Limit Request Summary
        FacilitySummaryModel facSummary = data.getCounterpartyFullDetail().getFacilitySummary();
        report.setApprovedDcpZar(decimalFormatter.format(facSummary.getApprovedDcp()));
        report.setProposedDcpZar(decimalFormatter.format(facSummary.getProposedDcp()));
        report.setMovementDcpZar(decimalFormatter.format(facSummary.getProposedDcp() - facSummary.getApprovedDcp()));

        report.setApprovedSettZar(decimalFormatter.format(facSummary.getApprovedSettle()));
        report.setProposedSettZar(decimalFormatter.format(facSummary.getProposedSettle()));
        report.setMovementSettZar(decimalFormatter.format(facSummary.getProposedSettle() - facSummary.getApprovedSettle()));

        report.setApprovedUnderwritingZar(decimalFormatter.format(facSummary.getApprovedUnderwritingAndCredit()));
        report.setProposedUnderwritingZar(decimalFormatter.format(facSummary.getProposedUnderwritingAndCredit()));
        report.setMovementUnderwritingZar(decimalFormatter.format(facSummary.getProposedUnderwritingAndCredit() - facSummary.getApprovedUnderwritingAndCredit()));

        report.setApprovedChurnZar(decimalFormatter.format(facSummary.getApprovedChurn()));
        report.setProposedChurnZar(decimalFormatter.format(facSummary.getProposedChurn()));
        report.setMovementChurnZar(decimalFormatter.format(facSummary.getProposedChurn() - facSummary.getApprovedChurn()));

        report.setApprovedCreditFundZar(decimalFormatter.format(facSummary.getApprovedCreditFund()));
        report.setProposedCreditFundZar(decimalFormatter.format(facSummary.getProposedCreditFund()));
        report.setMovementCreditFundZar(decimalFormatter.format(facSummary.getProposedCreditFund() - facSummary.getApprovedCreditFund()));

        report.setApprovedTradedCreditZar(decimalFormatter.format(facSummary.getApprovedTradedCredit()));
        report.setProposedTradedCreditZar(decimalFormatter.format(facSummary.getProposedTradedCredit()));
        report.setMovementTradedCreditZar(decimalFormatter.format(facSummary.getProposedTradedCredit() - facSummary.getApprovedTradedCredit()));

        String recommendation = data.getRecommendation() == null ? "" : data.getRecommendation();
        report.setRecommendation(StringEscapeUtils.escapeHtml4(recommendation.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

        String chairRecommendation = data.getChairRecommendation() == null ? "" : data.getChairRecommendation();
        report.setChairRecommendation(StringEscapeUtils.escapeHtml4(chairRecommendation.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

        List<ApprovalForumWfqWorkflowQuorumEntity> workflowQuorumEntities =
                approvalForumDataRepository.getWorkflowQuorumByWorkflowId(workflowEntity.getWfId());
        if (workflowQuorumEntities != null &&
                !workflowQuorumEntities.isEmpty() &&
                workflowQuorumEntities.stream().filter(ApprovalForumWfqWorkflowQuorumEntity::isSelected).findFirst().orElse(null) != null) {
            report.setApprovalQuorumNumber(workflowQuorumEntities.stream().filter(
                    ApprovalForumWfqWorkflowQuorumEntity::isSelected).findFirst().orElse(null).getTotalNumberOfApprovalsRequired() + "");
        } else {
            if (data.getRulesResponse().getModelData().size() > 0) {
                report.setApprovalQuorumNumber(data.getRulesResponse().getModelData().get(0).getTotalNumberOfApprovalsRequired() + "");
            } else {
                report.setApprovalQuorumNumber("-");
            }
        }

        report.setSpecificApproversCheckbox(data.isSpecific() ? "Yes" : "No");
        report.setSpecificApproversReason(data.getSpecificReason().replaceAll("(\r\n|\n\r|\r|\n)", "<br>"));

        report.setOverallRulesEngineResultCreditDecision(data.getStatus().toString());

        // List of Approvals
        List<Approval> approvalList = new ArrayList<>();
        List<String> commentList = new ArrayList<>();

        data.getApprovalsResponse().getApprovals().forEach(new Consumer<Approvals>() {
            @Override
            public void accept(Approvals approvals) {
                Approval approval = new Approval();
                approval.setApproverName(approvals.getName());
                approval.setApproverDecision(approvals.getAction().name());
                approval.setDecisionDate(dateFormatter.format(approvals.getDate()));
                approval.setCategory("");
                approval.setConditionsOfApproval(approvals.getConditionsOfApproval() != null ?
                        StringEscapeUtils.escapeHtml4(approvals.getConditionsOfApproval().replaceAll("(\r\n|\n\r|\r|\n)", "<br>")) : "");
                approval.setApprovalMandate(approvals.getApprovalMandate());

                report.setRecommendedMandate(approvals.getRecommendedMandate());
                commentList.add(approvals.getName() + " - " + approvals.getAction().name() + " - " + approvals.getConditionsOfApproval());
                approvalList.add(approval);
            }
        });

        String comments = commentList.stream().map(n -> String.valueOf(n))
                .collect(Collectors.joining("<br>"));
        if (data.getStatus().equals(WorkflowStatus.CONDITIONALLY_APPROVED)) {
            report.setConditionsOfApproval(StringEscapeUtils.escapeHtml4(comments));
        } else if (data.getStatus().equals(WorkflowStatus.REFERRED_BACK)) {
            report.setComments(StringEscapeUtils.escapeHtml4(comments));
        }

        report.setApprovals(approvalList);

        // Chat History

        List<Chat> chatList = getWorkflowChats(workflowEntity.getWfId());

        List<ChatHistory> chatHistoryList = new ArrayList<>();

        chatList.forEach(new Consumer<Chat>() {
            @Override
            public void accept(Chat chat) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setDate(dateTimeFormatter.format(chat.getChatDate()));
                chatHistory.setName(chat.getDisplayName());
                chatHistory.setComment(StringEscapeUtils.escapeHtml4(chat.getChatText().replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

                chatHistoryList.add(chatHistory);
            }
        });

        report.setChatHistory(chatHistoryList);

        UploadDocForm uploadDoc = new UploadDocForm();

        uploadDoc.setAppID(workflowEntity.getWfId() + "");
        uploadDoc.setCreatedBy("System");
        uploadDoc.setDocDescription("DAF PDF");
        uploadDoc.setDocType("DAF PDF");
        uploadDoc.setFilename(report.getCounterparty().replaceAll("[^a-zA-Z0-9 ,.()]", "") + " - DAF Report - " + dateFileFormatter.format(new Date()) + ".pdf");
        uploadDoc.setFileExtension("pdf");
        Response response = null;
        if(data.getTenantId()>2){
            response = this.docGenTarget.path("/generatePdfForBroaderAfrica").request()
                    .post(Entity.entity(report, MediaType.APPLICATION_JSON));
        } else{
            response = this.docGenTarget.path("/generate").request()
                    .post(Entity.entity(report, MediaType.APPLICATION_JSON));
        }

        try {
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatus());
            }

            InputStream is = (InputStream) response.getEntity();

            byte[] fileData = IOUtils.toByteArray(is);
            uploadDoc.setFiledata(fileData);
            uploadDoc.setFileSize(fileData.length);

            this.approvalForumDocumentService.uploadDocument(uploadDoc);

        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceException(e.getMessage());
        } finally {
            response.close();
        }
    }


    private void generatePdfForAshburton(ApprovalForumWfWorkflowEntity workflowEntity) throws ResourceException {
        SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy/MM/dd");

        NumberFormat nf = DecimalFormat.getInstance(Locale.ENGLISH);
        DecimalFormat decimalFormatter = (DecimalFormat) nf;
        decimalFormatter.applyPattern("#,###,###.##");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        SimpleDateFormat dateFileFormatter = new SimpleDateFormat("yyyy-MM-dd");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        AshburtonReportOutputModel report = new AshburtonReportOutputModel();

        RequestData data = getRequestForAshburton(workflowEntity.getWfId(), null, true, null);

        report.setCurrentDate(simpleDateFormatter.format(new Date()));
        report.setProposalTypeAndAction(data.getCounterpartyFullDetail().getCreditApplication().getProposalType() + " - " + data.getCounterpartyFullDetail().getCreditApplication().getProposalAction());

        // Application Summary
        report.setMeetingName(data.getCounterpartyFullDetail().getCreditApplication().getMeetingName());
        report.setCounterparty(data.getCounterpartyFullDetail().getCounterpartyDetail().getCounterpartyName());
        report.setWholesaleCreditPortfolio(data.getCounterpartyFullDetail().getCreditApplication().getWholesaleCreditPortfolio());
        report.setDistressedStatus(data.getCounterpartyFullDetail().getCreditApplication().getDistressStatus());
        report.setWorkflowId(workflowEntity.getWfId() + "");
        report.setSubmittedBy(data.getSubmittedBy());
        report.setCasdmCreditAnalyst(data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalyst());
        report.setCasdmReviewAnalyst(data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalyst());
        report.setConfidentialStatus(data.isConfidential() ? "Yes" : "No");
        report.setSubmittedDate(dateFormatter.format(data.getSubmittedOn()));

        boolean isProspectWithFacilities = isProspectWithFacilities(data.getCounterpartyFullDetail());
        report.setKycStatus(isProspectWithFacilities ? "KYC Process Incomplete" : "KYC Process Complete");

        if (null == data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystSignatureDate()) {
            report.setCasdmCaSubmittedDate("-");
        } else {
            report.setCasdmCaSubmittedDate(dateFormatter.format(data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystSignatureDate()));
        }

        if (null == data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystSignatureDate()) {
            report.setCasdmRaSubmittedDate("-");
        } else {
            report.setCasdmRaSubmittedDate(dateFormatter.format(data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystSignatureDate()));
        }

        // Ratings
        report.setCurrentFrRating(data.getCounterpartyFullDetail().getCreditApplication().getLastApprovedFrRating());
        report.setProposedFrRating(data.getCounterpartyFullDetail().getCreditApplication().getProposedFrRating());

        // Limit Request Summary
        FacilitySummaryModel facSummary = data.getCounterpartyFullDetail().getFacilitySummary();
        report.setApprovedDcpZar(decimalFormatter.format(facSummary.getApprovedDcp()));
        report.setProposedDcpZar(decimalFormatter.format(facSummary.getProposedDcp()));
        report.setMovementDcpZar(decimalFormatter.format(facSummary.getProposedDcp() - facSummary.getApprovedDcp()));

        report.setApprovedSettZar(decimalFormatter.format(facSummary.getApprovedSettle()));
        report.setProposedSettZar(decimalFormatter.format(facSummary.getProposedSettle()));
        report.setMovementSettZar(decimalFormatter.format(facSummary.getProposedSettle() - facSummary.getApprovedSettle()));

        report.setApprovedUnderwritingZar(decimalFormatter.format(facSummary.getApprovedUnderwritingAndCredit()));
        report.setProposedUnderwritingZar(decimalFormatter.format(facSummary.getProposedUnderwritingAndCredit()));
        report.setMovementUnderwritingZar(decimalFormatter.format(facSummary.getProposedUnderwritingAndCredit() - facSummary.getApprovedUnderwritingAndCredit()));

        report.setApprovedChurnZar(decimalFormatter.format(facSummary.getApprovedChurn()));
        report.setProposedChurnZar(decimalFormatter.format(facSummary.getProposedChurn()));
        report.setMovementChurnZar(decimalFormatter.format(facSummary.getProposedChurn() - facSummary.getApprovedChurn()));

        report.setApprovedCreditFundZar(decimalFormatter.format(facSummary.getApprovedCreditFund()));
        report.setProposedCreditFundZar(decimalFormatter.format(facSummary.getProposedCreditFund()));
        report.setMovementCreditFundZar(decimalFormatter.format(facSummary.getProposedCreditFund() - facSummary.getApprovedCreditFund()));

        report.setApprovedTradedCreditZar(decimalFormatter.format(facSummary.getApprovedTradedCredit()));
        report.setProposedTradedCreditZar(decimalFormatter.format(facSummary.getProposedTradedCredit()));
        report.setMovementTradedCreditZar(decimalFormatter.format(facSummary.getProposedTradedCredit() - facSummary.getApprovedTradedCredit()));

        String recommendation = data.getRecommendation() == null ? "" : data.getRecommendation();
        report.setRecommendation(StringEscapeUtils.escapeHtml4(recommendation.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

        String chairRecommendation = data.getChairRecommendation() == null ? "" : data.getChairRecommendation();
        report.setChairRecommendation(StringEscapeUtils.escapeHtml4(chairRecommendation.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

        List<ApprovalForumWfqWorkflowQuorumEntity> workflowQuorumEntities =
                approvalForumDataRepository.getWorkflowQuorumByWorkflowId(workflowEntity.getWfId());
        if (workflowQuorumEntities != null &&
                !workflowQuorumEntities.isEmpty() &&
                workflowQuorumEntities.stream().filter(ApprovalForumWfqWorkflowQuorumEntity::isSelected).findFirst().orElse(null) != null) {
            report.setApprovalQuorumNumber(workflowQuorumEntities.stream().filter(
                    ApprovalForumWfqWorkflowQuorumEntity::isSelected).findFirst().orElse(null).getTotalNumberOfApprovalsRequired() + "");
        } else {
            if (data.getRulesResponse().getModelData().size() > 0) {
                report.setApprovalQuorumNumber(data.getRulesResponse().getModelData().get(0).getTotalNumberOfApprovalsRequired() + "");
            } else {
                report.setApprovalQuorumNumber("-");
            }
        }

        report.setSpecificApproversCheckbox(data.isSpecific() ? "Yes" : "No");
        report.setSpecificApproversReason(data.getSpecificReason().replaceAll("(\r\n|\n\r|\r|\n)", "<br>"));

        report.setOverallRulesEngineResultCreditDecision(data.getStatus().toString());

        //Ashburton changes --limits requested

        try{
            logger.info("application id:"+data.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
            ASBLimitFundsDTO asbLimitFundsDTO = approvalForumExternalService.getAshburtonLimitFunds(String.valueOf(data.getCounterpartyFullDetail().getCreditApplication().getApplicationId()));
            LimitsRequestedDTO limitsRequestedDTO = (LimitsRequestedDTO)asbLimitFundsDTO.getLimits();
            report.setProposedSeniorSZar(String.valueOf(limitsRequestedDTO.getProposedSeniorSZar()));
            report.setProposedLiquidationZar(String.valueOf(limitsRequestedDTO.getProposedLiquidationZar()));
            report.setProposedMezzanineDZar(String.valueOf(limitsRequestedDTO.getProposedMezzanineDZar()));
            report.setProposedSubordinatedSZar(String.valueOf(limitsRequestedDTO.getProposedSubordinatedSZar()));
            report.setProposedSubordinatedUZar(String.valueOf(limitsRequestedDTO.getProposedSubordinatedUZar()));
            report.setProposedGovermentGZar(String.valueOf(limitsRequestedDTO.getProposedGovermentGZar()));
            report.setProposedSeniorUZar(String.valueOf(limitsRequestedDTO.getProposedSeniorUZar()));
            report.setProposedJuniorDZar(String.valueOf(limitsRequestedDTO.getProposedJuniorDZar()));
            report.setProposedConvertedEZar(String.valueOf(limitsRequestedDTO.getProposedConvertedEZar()));
            report.setProposedCPYLZar(String.valueOf(limitsRequestedDTO.getProposedCPYLZar()));

            //Ashburton changes -- fund percentage limits
            logger.info("application id:"+data.getCounterpartyFullDetail().getCreditApplication().getApplicationId());
            ASBFundsDTO fundsDTO = (ASBFundsDTO)asbLimitFundsDTO.getFunds();
            report.setLdiPercentage(String.valueOf(fundsDTO.getLdiPercentage()));
            report.setCreditPercentage(String.valueOf(fundsDTO.getCreditPercentage()));
            report.setMultiAssetPercentage(String.valueOf(fundsDTO.getMultiAssetPercentage()));
            report.setMultiManagerPercentage(String.valueOf(fundsDTO.getMultiManagerPercentage()));
            report.setFixedIncomePercentage(String.valueOf(fundsDTO.getFixedIncomePercentage()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceException(e.getMessage());
        }


        // List of Approvals
        List<Approval> approvalList = new ArrayList<>();
        List<String> commentList = new ArrayList<>();

        data.getApprovalsResponse().getApprovals().forEach(new Consumer<Approvals>() {
            @Override
            public void accept(Approvals approvals) {
                Approval approval = new Approval();
                approval.setApproverName(approvals.getName());
                approval.setApproverDecision(approvals.getAction().name());
                approval.setDecisionDate(dateFormatter.format(approvals.getDate()));
                approval.setCategory("");
                approval.setConditionsOfApproval(approvals.getConditionsOfApproval() != null ?
                        StringEscapeUtils.escapeHtml4(approvals.getConditionsOfApproval().replaceAll("(\r\n|\n\r|\r|\n)", "<br>")) : "");
                approval.setApprovalMandate(approvals.getApprovalMandate());

                report.setRecommendedMandate(approvals.getRecommendedMandate());
                commentList.add(approvals.getName() + " - " + approvals.getAction().name() + " - " + approvals.getConditionsOfApproval());
                approvalList.add(approval);
            }
        });

        String comments = commentList.stream().map(n -> String.valueOf(n))
                .collect(Collectors.joining("<br>"));
        if (data.getStatus().equals(WorkflowStatus.CONDITIONALLY_APPROVED)) {
            report.setConditionsOfApproval(StringEscapeUtils.escapeHtml4(comments));
        } else if (data.getStatus().equals(WorkflowStatus.REFERRED_BACK)) {
            report.setComments(StringEscapeUtils.escapeHtml4(comments));
        }

        report.setApprovals(approvalList);

        // Chat History

        List<Chat> chatList = getWorkflowChats(workflowEntity.getWfId());

        List<ChatHistory> chatHistoryList = new ArrayList<>();

        chatList.forEach(new Consumer<Chat>() {
            @Override
            public void accept(Chat chat) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setDate(dateTimeFormatter.format(chat.getChatDate()));
                chatHistory.setName(chat.getDisplayName());
                chatHistory.setComment(StringEscapeUtils.escapeHtml4(chat.getChatText().replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

                chatHistoryList.add(chatHistory);
            }
        });

        report.setChatHistory(chatHistoryList);

        UploadDocForm uploadDoc = new UploadDocForm();

        uploadDoc.setAppID(workflowEntity.getWfId() + "");
        uploadDoc.setCreatedBy("System");
        uploadDoc.setDocDescription("DAF PDF");
        uploadDoc.setDocType("DAF PDF");
        uploadDoc.setFilename(report.getCounterparty().replaceAll("[^a-zA-Z0-9 ,.()]", "") + " - DAF Report - " + dateFileFormatter.format(new Date()) + ".pdf");
        uploadDoc.setFileExtension("pdf");

        Response response = this.docGenTarget.path("/generateASB").request()
                .post(Entity.entity(report, MediaType.APPLICATION_JSON));

        try {
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatus());
            }

            InputStream is = (InputStream) response.getEntity();

            byte[] fileData = IOUtils.toByteArray(is);
            uploadDoc.setFiledata(fileData);
            uploadDoc.setFileSize(fileData.length);

            this.approvalForumDocumentService.uploadDocument(uploadDoc);

        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceException(e.getMessage());
        } finally {
            response.close();
        }
    }


    private void saveChatsAndAttachmentsToNewRequest(int oldWorkflowId, int newWorkflowId) {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(newWorkflowId);
        approvalForumDataRepository.getWorkflowChats(oldWorkflowId).forEach(chatEntity -> {
            ApprovalForumWfcWorkflowChatEntity entity = new ApprovalForumWfcWorkflowChatEntity();
            entity.setCreatedOn(chatEntity.getCreatedOn());
            entity.setDisplayName(chatEntity.getDisplayName());
            entity.setUserId(chatEntity.getUserId());
            entity.setChatText(chatEntity.getChatText());
            entity.setWfApprovalForumWorkflowByWfId(workflowEntity);
            approvalForumDataRepository.createChat(entity);
        });
        approvalForumDocumentService.cloneDocuments(oldWorkflowId, newWorkflowId);
    }

    private boolean isMandateHandledInDaf(String policyApprovalMandate) {
        ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity entity =
                approvalForumDataRepository.getMeetingQuorumRuleByMeetingName(policyApprovalMandate);
        if (entity != null) {
            return approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName());
        }
        return false;
    }

    private void generatePdf(ApprovalForumWfWorkflowEntity workflowEntity) throws ResourceException {
        SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy/MM/dd");

        NumberFormat nf = DecimalFormat.getInstance(Locale.ENGLISH);
        DecimalFormat decimalFormatter = (DecimalFormat) nf;
        decimalFormatter.applyPattern("#,###,###.##");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        SimpleDateFormat dateFileFormatter = new SimpleDateFormat("yyyy-MM-dd");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        ReportOutputModel report = new ReportOutputModel();

        RequestData data = getRequest(workflowEntity.getWfId(), null, true, null);

        report.setCurrentDate(simpleDateFormatter.format(new Date()));
        report.setProposalTypeAndAction(data.getCounterpartyFullDetail().getCreditApplication().getProposalType() + " - " + data.getCounterpartyFullDetail().getCreditApplication().getProposalAction());

        // Application Summary
        report.setMeetingName(data.getCounterpartyFullDetail().getCreditApplication().getMeetingName());
        report.setCounterparty(data.getCounterpartyFullDetail().getCounterpartyDetail().getCounterpartyName());
        report.setWholesaleCreditPortfolio(data.getCounterpartyFullDetail().getCreditApplication().getWholesaleCreditPortfolio());
        report.setDistressedStatus(data.getCounterpartyFullDetail().getCreditApplication().getDistressStatus());
        report.setWorkflowId(workflowEntity.getWfId() + "");
        report.setSubmittedBy(data.getSubmittedBy());
        report.setCasdmCreditAnalyst(data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalyst());
        report.setCasdmReviewAnalyst(data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalyst());
        report.setConfidentialStatus(data.isConfidential() ? "Yes" : "No");
        report.setSubmittedDate(dateFormatter.format(data.getSubmittedOn()));

        boolean isProspectWithFacilities = isProspectWithFacilities(data.getCounterpartyFullDetail());
        report.setKycStatus(isProspectWithFacilities ? "KYC Process Incomplete" : "KYC Process Complete");

        if (null == data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystSignatureDate()) {
            report.setCasdmCaSubmittedDate("-");
        } else {
            report.setCasdmCaSubmittedDate(dateFormatter.format(data.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystSignatureDate()));
        }

        if (null == data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystSignatureDate()) {
            report.setCasdmRaSubmittedDate("-");
        } else {
            report.setCasdmRaSubmittedDate(dateFormatter.format(data.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystSignatureDate()));
        }

        // Ratings
        report.setCurrentFrRating(data.getCounterpartyFullDetail().getCreditApplication().getLastApprovedFrRating());
        report.setProposedFrRating(data.getCounterpartyFullDetail().getCreditApplication().getProposedFrRating());

        // Limit Request Summary
        FacilitySummaryModel facSummary = data.getCounterpartyFullDetail().getFacilitySummary();
        report.setApprovedDcpZar(decimalFormatter.format(facSummary.getApprovedDcp()));
        report.setProposedDcpZar(decimalFormatter.format(facSummary.getProposedDcp()));
        report.setMovementDcpZar(decimalFormatter.format(facSummary.getProposedDcp() - facSummary.getApprovedDcp()));

        report.setApprovedSettZar(decimalFormatter.format(facSummary.getApprovedSettle()));
        report.setProposedSettZar(decimalFormatter.format(facSummary.getProposedSettle()));
        report.setMovementSettZar(decimalFormatter.format(facSummary.getProposedSettle() - facSummary.getApprovedSettle()));

        report.setApprovedUnderwritingZar(decimalFormatter.format(facSummary.getApprovedUnderwritingAndCredit()));
        report.setProposedUnderwritingZar(decimalFormatter.format(facSummary.getProposedUnderwritingAndCredit()));
        report.setMovementUnderwritingZar(decimalFormatter.format(facSummary.getProposedUnderwritingAndCredit() - facSummary.getApprovedUnderwritingAndCredit()));

        report.setApprovedChurnZar(decimalFormatter.format(facSummary.getApprovedChurn()));
        report.setProposedChurnZar(decimalFormatter.format(facSummary.getProposedChurn()));
        report.setMovementChurnZar(decimalFormatter.format(facSummary.getProposedChurn() - facSummary.getApprovedChurn()));

        report.setApprovedCreditFundZar(decimalFormatter.format(facSummary.getApprovedCreditFund()));
        report.setProposedCreditFundZar(decimalFormatter.format(facSummary.getProposedCreditFund()));
        report.setMovementCreditFundZar(decimalFormatter.format(facSummary.getProposedCreditFund() - facSummary.getApprovedCreditFund()));

        report.setApprovedTradedCreditZar(decimalFormatter.format(facSummary.getApprovedTradedCredit()));
        report.setProposedTradedCreditZar(decimalFormatter.format(facSummary.getProposedTradedCredit()));
        report.setMovementTradedCreditZar(decimalFormatter.format(facSummary.getProposedTradedCredit() - facSummary.getApprovedTradedCredit()));

        String recommendation = data.getRecommendation() == null ? "" : data.getRecommendation();
        report.setRecommendation(StringEscapeUtils.escapeHtml4(recommendation.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

        String chairRecommendation = data.getChairRecommendation() == null ? "" : data.getChairRecommendation();
        report.setChairRecommendation(StringEscapeUtils.escapeHtml4(chairRecommendation.replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

        List<ApprovalForumWfqWorkflowQuorumEntity> workflowQuorumEntities =
                approvalForumDataRepository.getWorkflowQuorumByWorkflowId(workflowEntity.getWfId());
        if (workflowQuorumEntities != null &&
                !workflowQuorumEntities.isEmpty() &&
                workflowQuorumEntities.stream().filter(ApprovalForumWfqWorkflowQuorumEntity::isSelected).findFirst().orElse(null) != null) {
            report.setApprovalQuorumNumber(workflowQuorumEntities.stream().filter(
                    ApprovalForumWfqWorkflowQuorumEntity::isSelected).findFirst().orElse(null).getTotalNumberOfApprovalsRequired() + "");
        } else {
            if (data.getRulesResponse().getModelData().size() > 0) {
                report.setApprovalQuorumNumber(data.getRulesResponse().getModelData().get(0).getTotalNumberOfApprovalsRequired() + "");
            } else {
                report.setApprovalQuorumNumber("-");
            }
        }

        report.setSpecificApproversCheckbox(data.isSpecific() ? "Yes" : "No");
        report.setSpecificApproversReason(data.getSpecificReason().replaceAll("(\r\n|\n\r|\r|\n)", "<br>"));

        report.setOverallRulesEngineResultCreditDecision(data.getStatus().toString());

        // List of Approvals
        List<Approval> approvalList = new ArrayList<>();
        List<String> commentList = new ArrayList<>();

        data.getApprovalsResponse().getApprovals().forEach(new Consumer<Approvals>() {
            @Override
            public void accept(Approvals approvals) {
                Approval approval = new Approval();
                approval.setApproverName(approvals.getName());
                approval.setApproverDecision(approvals.getAction().name());
                approval.setDecisionDate(dateFormatter.format(approvals.getDate()));
                approval.setCategory("");
                approval.setConditionsOfApproval(approvals.getConditionsOfApproval() != null ?
                        StringEscapeUtils.escapeHtml4(approvals.getConditionsOfApproval().replaceAll("(\r\n|\n\r|\r|\n)", "<br>")) : "");
                approval.setApprovalMandate(approvals.getApprovalMandate());

                report.setRecommendedMandate(approvals.getRecommendedMandate());
                commentList.add(approvals.getName() + " - " + approvals.getAction().name() + " - " + approvals.getConditionsOfApproval());
                approvalList.add(approval);
            }
        });

        String comments = commentList.stream().map(n -> String.valueOf(n))
                .collect(Collectors.joining("<br>"));
        if (data.getStatus().equals(WorkflowStatus.CONDITIONALLY_APPROVED)) {
            report.setConditionsOfApproval(StringEscapeUtils.escapeHtml4(comments));
        } else if (data.getStatus().equals(WorkflowStatus.REFERRED_BACK)) {
            report.setComments(StringEscapeUtils.escapeHtml4(comments));
        }

        report.setApprovals(approvalList);

        // Chat History

        List<Chat> chatList = getWorkflowChats(workflowEntity.getWfId());

        List<ChatHistory> chatHistoryList = new ArrayList<>();

        chatList.forEach(new Consumer<Chat>() {
            @Override
            public void accept(Chat chat) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setDate(dateTimeFormatter.format(chat.getChatDate()));
                chatHistory.setName(chat.getDisplayName());
                chatHistory.setComment(StringEscapeUtils.escapeHtml4(chat.getChatText().replaceAll("(\r\n|\n\r|\r|\n)", "<br>")));

                chatHistoryList.add(chatHistory);
            }
        });

        report.setChatHistory(chatHistoryList);

        UploadDocForm uploadDoc = new UploadDocForm();

        uploadDoc.setAppID(workflowEntity.getWfId() + "");
        uploadDoc.setCreatedBy("System");
        uploadDoc.setDocDescription("DAF PDF");
        uploadDoc.setDocType("DAF PDF");
        uploadDoc.setFilename(report.getCounterparty().replaceAll("[^a-zA-Z0-9 ,.()]", "") + " - DAF Report - " + dateFileFormatter.format(new Date()) + ".pdf");
        uploadDoc.setFileExtension("pdf");

        Response response = this.docGenTarget.path("/generate").request()
                .post(Entity.entity(report, MediaType.APPLICATION_JSON));

        try {
            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed with HTTP error code : " + response.getStatus());
            }

            InputStream is = (InputStream) response.getEntity();

            byte[] fileData = IOUtils.toByteArray(is);
            uploadDoc.setFiledata(fileData);
            uploadDoc.setFileSize(fileData.length);

            this.approvalForumDocumentService.uploadDocument(uploadDoc);

        } catch (IOException e) {
            e.printStackTrace();
            throw new ResourceException(e.getMessage());
        } finally {
            response.close();
        }
    }

    private boolean isProspectWithFacilities(CounterpartyFullDetail detail) {
        boolean found = false;
        for (FacilityModel fac : detail.getFacility()) {
            if (!"Approved".equalsIgnoreCase(fac.getCreditApplicationStatus())) {
                found = true;
                break;
            }
        }
        return detail.getCounterpartyDetail().isProspect() && found;
    }

    private void setToInactive(ApprovalForumWfWorkflowEntity workflowEntity) {
        workflowEntity.setActive(false);
        workflowEntity.setCompletedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        approvalForumDataRepository.updateWorkflow(workflowEntity);
    }

    private void setAuditAndLastUpdatedDetails(String user, String description, int workflowId) {
        AuditDetails auditDetails = new AuditDetails();
        auditDetails.setUser(user);
        auditDetails.setDescription(description);
        auditDetails.setWorkflowId(workflowId);
        approvalForumDataAuditRepository.setAuditDetails(auditDetails);
    }

    @Override
    public void createChat(int workflowId, String chatText, Principal user) {
        ApprovalForumWfcWorkflowChatEntity chatEntity = new ApprovalForumWfcWorkflowChatEntity();
        chatEntity.setChatText(chatText);
        chatEntity.setWfApprovalForumWorkflowByWfId(approvalForumDataRepository.getWorkflow(workflowId));
        chatEntity.setUserId(user.getName());
        chatEntity.setCreatedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        chatEntity.setDisplayName(
                approvalForumAuthService.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(user.getName()));
        approvalForumDataRepository.createChat(chatEntity);
    }

    @Override
    public List<Chat> getWorkflowChats(int workflowId) {
        List<Chat> chats = new ArrayList<>();
        approvalForumDataRepository.getWorkflowChats(workflowId).stream().map(this::mapChatFromEntity)
                .collect(Collectors.toCollection(() -> chats));
        return chats;
    }

    private Chat mapChatFromEntity(ApprovalForumWfcWorkflowChatEntity entity) {
        Chat chat = new Chat();
        chat.setWorkflowId(entity.getWfApprovalForumWorkflowByWfId().getWfId());
        chat.setChatId(entity.getWfcId());
        chat.setChatDate(entity.getCreatedOn().toLocalDateTime());
        chat.setChatText(entity.getChatText());
        chat.setDisplayName(entity.getDisplayName());
        chat.setUserId(entity.getUserId());
        return chat;
    }

    @Override
    public void createRequestExternal(int appId, boolean isCreate, boolean lumos, int tenantId) {
        logger.info("***createRequestExternal(START)");

        // Check if request already received
        if (!approvalForumDataRepository.getApprovalForumErExternalRequestsEntityByAppIdAndIsCreate(appId, isCreate, lumos)
                .isEmpty())
            return;

        ProcessExternalRequestResponse response;
        List<ApprovalForumWfadWorkflowApplicationDataEntity> applicationDataEntities = approvalForumDataRepository
                .getApplicationDataByAppIdAndLumos(appId, lumos);
        if (isCreate) {
            if (applicationDataEntities.isEmpty()) {
                if (!lumos) {
                    logger.info("Creating CASDM external request.");
                    response = processCasdmExternalRequestsCreate(appId, 0, null);

                } else {
                    logger.info("Creating Lumos external request.");
                    response = processLumosExternalRequestsCreate(appId, 0, null, tenantId);
                }

            } else {
                response = new ProcessExternalRequestResponse();
                response.setSuccess(true);
                response.setWorkflowId(
                        applicationDataEntities.get(0).getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId()
                                .getWfApprovalForumWorkflowByWfId().getWfId());
                response.setMessage("Already an active request for applicationId " + appId);
            }
            persistRequestExternal(appId, isCreate, response.isSuccess(), response.getWorkflowId(),
                    response.getMessage(), lumos, tenantId);
        } else {
            logger.info("***isCreate? {}", isCreate);
            if (!applicationDataEntities.isEmpty()) {
                response = processExternalRequestsCancel(applicationDataEntities.get(0));
                logger.info("***cancel response.getMessage({}), response.getWorkflowId({})", response.getMessage(), response.getWorkflowId());
            } else {
                response = new ProcessExternalRequestResponse();
                response.setSuccess(true);
                response.setMessage("No active requests found for applicationId " + appId);
            }
            persistRequestExternal(appId, isCreate, response.isSuccess(), response.getWorkflowId(),
                    response.getMessage(), lumos, tenantId);
        }
        logger.info("***createRequestExternal(END)");
    }

    //ashburton code
    public void createAshburtonRequestExternal(int appId, boolean isCreate) {
        logger.info("Creating CASDM-Ashburton1 external request:"+appId);
        // Check if request already received
        if (!approvalForumDataRepository.getAshburtonApprovalForumErExternalRequestsEntityByAppIdAndIsCreate(appId, isCreate, 2)
                .isEmpty())
            return;

        ProcessExternalRequestResponse response;
        List<ApprovalForumWfadWorkflowApplicationDataEntity> applicationDataEntities = approvalForumDataRepository
                .getAshburtonApplicationDataByAppIdAndTenantId(appId, 2);
        if (isCreate) {
            if (applicationDataEntities.isEmpty()) {

                logger.info("Creating CASDM-Ashburton external request:"+appId);
                response = processCasdmAshburtonExternalRequestsCreate(appId, 0, null);
                logger.info("Creating CASDM-Ashburton external request:wfid:"+response.getWorkflowId());
            }

            else {
                response = new ProcessExternalRequestResponse();
                response.setSuccess(true);
                response.setWorkflowId(
                        applicationDataEntities.get(0).getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId()
                                .getWfApprovalForumWorkflowByWfId().getWfId());
                response.setMessage("Already an active request for applicationId " + appId);
            }
            logger.info("Creating CASDM-Ashburton external request:wfid:"+response.getWorkflowId());
            persistAshburtonRequestExternal(appId, isCreate, response.isSuccess(), response.getWorkflowId(),
                    response.getMessage());
        } else {
            if (!applicationDataEntities.isEmpty()) {
                response = processExternalRequestsCancel(applicationDataEntities.get(0));
            } else {
                response = new ProcessExternalRequestResponse();
                response.setSuccess(true);
                response.setMessage("No active requests found for applicationId " + appId);
            }
            persistAshburtonRequestExternal(appId, isCreate, response.isSuccess(), response.getWorkflowId(),
                    response.getMessage());
        }
    }
    private void persistRequestExternal(int appId, boolean isCreate, boolean isComplete, Integer workflowId,
                                        String message, boolean lumos, int tenantId) {
        ApprovalForumErExternalRequestsEntity entity = new ApprovalForumErExternalRequestsEntity();
        entity.setCreate(isCreate);
        entity.setApplicationId(appId);
        entity.setCreatedBy("System");
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        entity.setCreatedOn(now);
        entity.setComplete(isComplete);
        entity.setRetryCount(0);
        entity.setLastTriedOn(now);
        entity.setLastTriedMessage(message);
        if (workflowId != null && workflowId > 0)
            entity.setWfId(workflowId);
        if (isComplete) {
            entity.setCompletedOn(now);
        }
        entity.setLumos(lumos);
        entity.setTenantId(tenantId);
        if(!lumos) {
            entity.setTenantId((Integer)1);
        }

        approvalForumDataRepository.persistApprovalForumErExternalRequestsEntity(entity);
    }

//persistAshburtonRequestExternal
    private void persistAshburtonRequestExternal(int appId, boolean isCreate, boolean isComplete, Integer workflowId,
                                                 String message) {
        logger.info("persistAshburtonRequestExternal:wfid:"+workflowId);
        ApprovalForumErExternalRequestsEntity entity = new ApprovalForumErExternalRequestsEntity();
        entity.setCreate(isCreate);
        entity.setApplicationId(appId);
        entity.setCreatedBy("System");
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        entity.setCreatedOn(now);
        entity.setComplete(isComplete);
        entity.setRetryCount(0);
        entity.setLastTriedOn(now);
        entity.setLastTriedMessage(message);
        if (workflowId != null && workflowId > 0)
            entity.setWfId(workflowId);
        if (isComplete) {
            entity.setCompletedOn(now);
        }
        entity.setLumos(false);
        entity.setTenantId((Integer)2);
        logger.info("persistAshburtonRequestExternal1:wfid:"+entity.getWfId());
        approvalForumDataRepository.persistApprovalForumErExternalRequestsEntity(entity);
    }
    private ProcessExternalRequestResponse processExternalRequestsCreate(int appId) {
        return processCasdmExternalRequestsCreate(appId, 0, null);
    }

    private ProcessExternalRequestResponse processLumosExternalRequestsCreate(int appId, int referencedWorkflowId, String policyApprovalMandate, int tenantId) {
        ProcessExternalRequestResponse response = new ProcessExternalRequestResponse();
        response.setSuccess(true);
        response.setWorkflowId(null);
        response.setMessage("Successfully created request");

        try {

            // Calling Lumos Service to get counterpartyFullDetail
            CounterpartyFullDetail counterpartyFullDetail = lumosRestService.getLumosWorkflow(appId);

            // Check if lumos service response if a complete object if no fail the response
            if (counterpartyFullDetail != null) {
                CounterpartyModel counterpartyModel = counterpartyFullDetail.getCounterpartyDetail();
                if (counterpartyModel == null || (counterpartyModel.getCreditInkombaId() == null || "".equals(counterpartyModel.getCreditInkombaId().trim()))) {
                    logger.warn("Could not find application info from Lumos for applicationId " + appId);
                    response.setMessage("Could not find application info from Lumos for applicationId " + appId);
                    response.setSuccess(false);
                    return response;
                }

                List<FacilityModel> facilityModels = counterpartyFullDetail.getFacility();

                if (facilityModels == null) {
                    logger.warn("There are no facilities to approve in this Lumos DAF Request, terminating the process ...");
                    response.setMessage("There are no facilities to approve in this Lumos DAF Request, terminating the process ...");
                    return response;
                }

                CreditApplicationModel creditApplicationModel = counterpartyFullDetail.getCreditApplication();
                logger.info("Get meeting rule by quorum name ...");
                ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity entity = approvalForumDataRepository.getMeetingQuorumRuleByMeetingName(creditApplicationModel.getMeetingName());

                if (entity == null) {
                    logger.warn("The Lumos meeting name " + creditApplicationModel.getMeetingName() + " is not catered for in DAF ...");
                    response.setMessage("The Lumos meeting name " + creditApplicationModel.getMeetingName() + " is not catered for in DAF ...");
                    return response;
                } else {
                    logger.info("Meeting name : " + creditApplicationModel.getMeetingName());
                }

                if (!approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName())) {
                    logger.warn("Mapped meeting name not catered for in the rules: " + entity.getMqrMeetingQuorumRulesByMqrId().getName());
                    response.setMessage("Mapped meeting name not catered for in the rules: " + entity.getMqrMeetingQuorumRulesByMqrId().getName());
                    return response;
                } else {
                    logger.info("Meeting name is catered for ...");
                }

                if ("Admin Changes".equalsIgnoreCase(creditApplicationModel.getProposalType())) {
                    logger.warn("Lumos Application proposal type is 'Admin Changes'. This type  is not catered for in DAF ...");
                    response.setMessage("Lumos Application proposal type is 'Admin Changes'. This type  is not catered for in DAF ...");
                    return response;
                } else {
                    logger.info("Lumos Application proposal type is not 'Admin Changes'");
                }

                RequestData requestData = new RequestData();
                requestData.setType(RequestType.CREDIT_APPLICATION);
                requestData.setConfidential(false);
                requestData.setRecommendation("");
                requestData.setSpecificReason("");

                NewRequestResponseDTO newRequestResponseDTO;
                if (referencedWorkflowId > 0) {
                    logger.info("Referenced Workflow Id : " + referencedWorkflowId);
                    requestData.setReferencedWorkflowId(referencedWorkflowId);
                    newRequestResponseDTO = getLumosNewRequestDetails(counterpartyFullDetail);
                } else {
                    logger.warn("No referenced WorkflowId ID");
                    newRequestResponseDTO = getLumosNewRequestDetails(counterpartyFullDetail);
                }

                requestData.setCounterpartyFullDetail(newRequestResponseDTO.getCounterpartyFullDetail());
                requestData.setRulesResponse(newRequestResponseDTO.getRulesResponse().getResponse());
                requestData.setSpecific(false);
                requestData.setStatus(WorkflowStatus.WIP);
                requestData.setUsersResponse(newRequestResponseDTO.getUsersResponse().getResponse());
                requestData.setIsLumos(true);
                if(!isNull(tenantId)&& tenantId==0){
                    requestData.setTenantId(1);
                } else {
                    logger.info("Creating workflow request for tenantId:"+tenantId);
                    requestData.setTenantId(tenantId);
                }

                logger.info("Creating workflow request");
                CreateRequestResponseDTO responseDTO = callRequestToCreateRequest(requestData);
                if (responseDTO.getWorkflowId() > 0) {
                    response.setWorkflowId(responseDTO.getWorkflowId());
                    approvalForumMailService.sendNewRequestMail(responseDTO.getWorkflowId());
                } else {
                    logger.warn("Error calling service to create request");
                    response.setMessage("Error calling service to create request");
                    response.setSuccess(false);
                }
            } else {
                logger.warn("Not enough information to proceed, DAF could not get application information for workflow id " + appId + " in Lumos service");
                response.setMessage("Not enough information to proceed, DAF could not get application information for workflow id " + appId + " in Lumos service");
                response.setSuccess(false);
                return response;
            }

        } catch (Exception e) {
            logger.error("Error occurred while processing Lumos Request, Reason : " + e.getMessage(), e);
            response.setMessage(e.getMessage());
            response.setSuccess(false);
        }
        return response;
    }

    private ProcessExternalRequestResponse processCasdmExternalRequestsCreate(int appId, int referencedWorkflowId, String policyApprovalMandate) {
        ProcessExternalRequestResponse response = new ProcessExternalRequestResponse();
        response.setSuccess(true);
        response.setWorkflowId(null);
        response.setMessage("Successfully created request");
        try {
            CreditApplicationByIdModel applicationByIdModel = approvalForumExternalService
                    .getCreditApplicationUsingIdAllStatuses(appId);
            logger.info("Creating workflow request---processCasdmExternalRequestsCreate:"+applicationByIdModel.getCreditApplicationStatus());
            if (applicationByIdModel == null || (applicationByIdModel.getCreditInkombaId() == null || "".equals(applicationByIdModel.getCreditInkombaId().trim()))) {
                response.setMessage("Could not find application info from CASDM for applicationId " + appId);
                response.setSuccess(false);
                return response;
            }
            if (!"Submitted for Approval".equalsIgnoreCase(applicationByIdModel.getCreditApplicationStatus())) {
                if(applicationByIdModel.getMeetingName().contains("Ashburton")){
                    response.setMessage(
                            "Status for application on CASDM-Ashburton is '" + applicationByIdModel.getCreditApplicationStatus() + "', not creating");
                    return response;
                }
                response.setMessage(
                        "Status for application on CASDM is '" + applicationByIdModel.getCreditApplicationStatus() + "', not creating");
                return response;
            }

            ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity entity =
                    approvalForumDataRepository.getMeetingQuorumRuleByMeetingName(applicationByIdModel.getMeetingName());

            if (entity == null) {
                response.setMessage(
                        "Application for a non catered meeting type (var): " + applicationByIdModel.getMeetingName());
                return response;
            }

            if (!approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName())) {
                response.setMessage(
                        "Mapped meeting name not catered for in the rules: " + entity.getMqrMeetingQuorumRulesByMqrId().getName());
                return response;
            }

            if ("Admin Changes".equalsIgnoreCase(applicationByIdModel.getProposalType())) {
                response.setMessage(
                        "Application proposal type is 'Admin Changes', excluding");
                return response;
            }
            RequestData requestData = new RequestData();
            requestData.setType(RequestType.CREDIT_APPLICATION);
            requestData.setConfidential(false);
            requestData.setRecommendation("");
            requestData.setChairRecommendation("");
            requestData.setSpecificReason("");

            NewRequestResponseDTO newRequestResponseDTO;
            if (referencedWorkflowId > 0) {
                requestData.setReferencedWorkflowId(referencedWorkflowId);
                newRequestResponseDTO = getNewRequestDetails(applicationByIdModel.getCreditInkombaId(), policyApprovalMandate);
            } else {
                newRequestResponseDTO = getNewRequestDetails(applicationByIdModel.getCreditInkombaId());
            }

            logger.info("Creating workflow request--NONASHBURTON");
            requestData.setTenantId(1);
            requestData.setCounterpartyFullDetail(newRequestResponseDTO.getCounterpartyFullDetail());
            requestData.setRulesResponse(newRequestResponseDTO.getRulesResponse().getResponse());
            requestData.setSpecific(false);
            requestData.setStatus(WorkflowStatus.WIP);
            requestData.setUsersResponse(newRequestResponseDTO.getUsersResponse().getResponse());
            requestData.setIsLumos(false);
            requestData.setChairpersonApprovalRequired(newRequestResponseDTO.isChairpersonApprovalRequired());
            CreateRequestResponseDTO responseDTO = callRequestToCreateRequest(requestData);
            if (responseDTO.getWorkflowId() > 0) {
                response.setWorkflowId(responseDTO.getWorkflowId());
                approvalForumMailService.sendNewRequestMail(responseDTO.getWorkflowId());
            } else {
                response.setMessage("Error calling service to create request");
                response.setSuccess(false);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setSuccess(false);
        }
        return response;
    }

//processCasdmAshburtonExternalRequestsCreate
    private ProcessExternalRequestResponse processCasdmAshburtonExternalRequestsCreate(int appId, int referencedWorkflowId, String policyApprovalMandate) {
        logger.info("processCasdmAshburtonExternalRequestsCreate..");
        ProcessExternalRequestResponse response = new ProcessExternalRequestResponse();
        response.setSuccess(true);
        response.setWorkflowId(null);
        response.setMessage("Successfully created request");
        try {
            CreditApplicationByIdModel applicationByIdModel = approvalForumExternalService
                    .getAshburtonCreditApplicationUsingIdAllStatuses(appId);
            if (applicationByIdModel == null || (applicationByIdModel.getCreditInkombaId() == null || "".equals(applicationByIdModel.getCreditInkombaId().trim()))) {
                response.setMessage("Could not find application info from CASDM-Ashburton for applicationId " + appId);
                response.setSuccess(false);
                return response;
            }
            if (!"Submitted for Approval".equalsIgnoreCase(applicationByIdModel.getCreditApplicationStatus())) {
                response.setMessage(
                        "Status for application on CASDM-Ashburton is '" + applicationByIdModel.getCreditApplicationStatus() + "', not creating");
                return response;
            }

            ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity entity =
                    approvalForumDataRepository.getMeetingQuorumRuleByMeetingName(applicationByIdModel.getMeetingName());

            if (entity == null) {
                response.setMessage(
                        "Application for a non catered meeting type (var): " + applicationByIdModel.getMeetingName());
                return response;
            }

            if (!approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName())) {
                response.setMessage(
                        "Mapped meeting name not catered for in the rules: " + entity.getMqrMeetingQuorumRulesByMqrId().getName());
                return response;
            }

            if ("Admin Changes".equalsIgnoreCase(applicationByIdModel.getProposalType())) {
                response.setMessage(
                        "Application proposal type is 'Admin Changes', excluding");
                return response;
            }
            RequestData requestData = new RequestData();
            requestData.setType(RequestType.CREDIT_APPLICATION);
            requestData.setConfidential(false);
            requestData.setRecommendation("");
            requestData.setChairRecommendation("");
            requestData.setSpecificReason("");
            NewRequestResponseDTO newRequestResponseDTO;
            if (referencedWorkflowId > 0) {
                logger.info("processCasdmAshburtonExternalRequestsCreate:referencedWorkflowId:"+referencedWorkflowId);
                requestData.setReferencedWorkflowId(referencedWorkflowId);
                newRequestResponseDTO = getNewRequestDetailsForAshburton(applicationByIdModel.getCreditInkombaId(), policyApprovalMandate, appId);
            } else {
                newRequestResponseDTO = getNewRequestDetailsForAshburton(applicationByIdModel.getCreditInkombaId(), appId);
            }

            logger.info("Creating workflow request--ASHBURTON");
            requestData.setTenantId(2);
            requestData.setCounterpartyFullDetail(newRequestResponseDTO.getCounterpartyFullDetail());
            requestData.setRulesResponse(newRequestResponseDTO.getRulesResponse().getResponse());
            requestData.setSpecific(false);
            requestData.setStatus(WorkflowStatus.WIP);
            requestData.setUsersResponse(newRequestResponseDTO.getUsersResponse().getResponse());
            requestData.setIsLumos(false);
            requestData.setChairpersonApprovalRequired(newRequestResponseDTO.isChairpersonApprovalRequired());
            CreateRequestResponseDTO responseDTO = callRequestToCreateRequest(requestData);
            if (responseDTO.getWorkflowId() > 0) {
                logger.info("Creating workflow request--ASHBURTON:"+responseDTO.getWorkflowId());
                response.setWorkflowId(responseDTO.getWorkflowId());
                approvalForumMailService.sendNewRequestMail(responseDTO.getWorkflowId());
            } else {
                logger.info("Creating workflow request--ASHBURTON:Error calling service to create request");
                response.setMessage("Error calling service to create request");
                response.setSuccess(false);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setSuccess(false);
        }
        return response;
    }


    private CreateRequestResponseDTO callRequestToCreateRequest(RequestData requestData) {
        CreateRequestDTO createRequestDTO = new CreateRequestDTO().withData(requestData);
        int tenantId= 1;
        logger.info("callRequestToCreateRequest1--tenantid:"+tenantId);
        if(requestData.getTenantId() != null){
            tenantId = requestData.getTenantId();
            logger.info("callRequestToCreateRequest--tenantid:"+tenantId);
        }
        createRequestDTO.setExternalRequest(true);
        Client client = ClientBuilder.newClient();
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.token"));
        client.register(feature);
        try {
            logger.info("--------------------------------------------------");
            logger.info("Creating the actual wip request in approval forum!");
            logger.info("--------------------------------------------------");
            logger.info("callRequestToCreateRequest--tenantid:"+requestData.getTenantId());
            Response response = client.target(afSettings.get("af.base") + "/approvalForum/request/wip?tenantId="+tenantId).request()
                    .post(Entity.entity(createRequestDTO, MediaType.APPLICATION_JSON));
            if (response.getStatus() == 200) {
                logger.info("creating wip returned status: {}", response.getStatus());
                return response.readEntity(CreateRequestResponseDTO.class);
            } else {
                logger.error("Error occurred while sending creating a request via service. Reason : " + response.getStatus());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            client.close();
        }
        return new CreateRequestResponseDTO();
    }



    /**
     * @param entity
     * @return TODO : Does this apply for Lumos
     */
    private ProcessExternalRequestResponse processExternalRequestsCancel(
            ApprovalForumWfadWorkflowApplicationDataEntity entity) {
        logger.info("***processExternalRequestsCancel");
        int workflowId = entity.getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId()
                .getWfApprovalForumWorkflowByWfId().getWfId();
        boolean isWip = "wip".equalsIgnoreCase(entity.getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId().getWfApprovalForumWorkflowByWfId().getStatus());

        ProcessExternalRequestResponse externalRequestResponse = new ProcessExternalRequestResponse();
        externalRequestResponse.setSuccess(true);
        externalRequestResponse.setWorkflowId(workflowId);
        externalRequestResponse.setMessage("Successfully cancelled request");

        SubmitDecisionRequestDTO requestDTO = new SubmitDecisionRequestDTO();
        requestDTO.setWorkflowId(workflowId);

        Client client = getClient();
        try {
            Response response;
            if (isWip) {
                response = client
                        .target(afSettings.get("af.base") + "/approvalForum/request/wip/" + workflowId).request()
                        .delete();
            } else {
                try {
                    if (entity.getLumos()) {
                        WorkflowResponseDTO workflowResponseDTO = lumosRestService.findLumosWorkflowById(entity.getApplicationId());

                        if (workflowResponseDTO.getWorkflow() == null || !"SUBMITTED___FOR___APPROVAL".equalsIgnoreCase(workflowResponseDTO.getWorkflow().getStatus())) {
                            externalRequestResponse.setMessage("Could not find application " + entity.getApplicationId() + " on LUMOS");
                            externalRequestResponse.setSuccess(false);
                            return externalRequestResponse;
                        } else {
                            logger.info("Status on Lumos for application {} is now '{}'", entity.getApplicationId(), workflowResponseDTO.getWorkflow().getStatus());
                            requestDTO.setDecision(ApprovalDecision.fromValue(workflowResponseDTO.getWorkflow().getStatus() + " (LUMOS)"));
                        }
                    } else {
                        CreditApplicationByIdModel applicationByIdModel =
                                approvalForumExternalService.getCreditApplicationUsingIdAllStatuses(entity.getApplicationId());
                        if (applicationByIdModel == null || (applicationByIdModel.getCreditInkombaId() == null || "".equals(applicationByIdModel.getCreditInkombaId().trim()))) {
                            externalRequestResponse.setMessage("Could not find application " + entity.getApplicationId() + " on CASDM");
                            externalRequestResponse.setSuccess(false);
                            return externalRequestResponse;
                        } else {
                            logger.info("Status on CASDM for application {} is now '{}'", entity.getApplicationId(), applicationByIdModel.getCreditApplicationStatus());
                            if(applicationByIdModel.getMeetingName().contains("ASHBURTON")){
                                requestDTO.setDecision(ApprovalDecision.fromValue(applicationByIdModel.getCreditApplicationStatus() + " (ASHBURTON)"));
                            } else {
                                logger.info("***applicationByIdModel.getCreditApplicationStatus(): {}", applicationByIdModel.getCreditApplicationStatus());
                                requestDTO.setDecision(ApprovalDecision.fromValue(applicationByIdModel.getCreditApplicationStatus() + " (CASDM)"));
                                logger.info("***requestDTO.setDecision(): {}", requestDTO.getDecision());
                            }

                        }
                    }
                } catch (ExternalResourceException e) {
                    externalRequestResponse.setMessage("Exception calling Could not find application approvalForumExternalService.getCreditApplicationUsingId");
                    externalRequestResponse.setSuccess(false);
                    return externalRequestResponse;
                }

                response = client
                        .target(afSettings.get("af.base") + "/approvalForum/decision/system/" + workflowId).request()
                        .post(Entity.entity(requestDTO, MediaType.APPLICATION_JSON));
            }
            if (response.getStatus() != 200) {
                externalRequestResponse.setMessage("Non success status calling service to cancel request");
                externalRequestResponse.setSuccess(false);
                return externalRequestResponse;
            } else {
                if (!isWip) {
                    SubmitDecisionResponseDTO submitDecisionResponseDTO = response
                            .readEntity(SubmitDecisionResponseDTO.class);
                    if (!submitDecisionResponseDTO.isSuccess()) {
                        externalRequestResponse.setMessage(submitDecisionResponseDTO.getMessage());
                        externalRequestResponse.setSuccess(false);
                        return externalRequestResponse;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            externalRequestResponse.setMessage("Error calling service to create request");
            externalRequestResponse.setSuccess(false);
        } finally {
            client.close();
        }
        return externalRequestResponse;
    }

    private void notifyFrontEndUser(int workflowId) {
        Socket socket;
        try {
            socket = IO.socket(afSettings.get("af.socketio.url"));
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("Connect");
                    Map<String, String> token = new HashMap<>();
                    token.put("token", afSettings.get("af.socketio.token"));
                    socket.emit("authenticate", token);
                    socket.emit("join-room", workflowId);

                    Map<String, Object> msg = new HashMap<>();
                    msg.put("workflowId", workflowId);
                    msg.put("request-cancelled", true);

                    socket.emit("request-cancelled", msg);
                    socket.disconnect();
                }

            }).on("event", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("ConnectError");
                }

            }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("ConnectTimeOut");
                }

            }).on(Socket.EVENT_CONNECTING, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("Connecting");
                }

            }).on(Socket.EVENT_ERROR, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("Error");
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private Client getClient() {
        Client client = ClientBuilder.newClient();
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.token"));
        client.register(feature);
        return client;
    }

    @Override
    public void cleanupRequests() throws ResourceException {
        logger.info("cleanupRequests()");
        List<ApprovalForumWfWorkflowEntity> workflowEntities =
                Optional.ofNullable(approvalForumDataRepository.getWorkflows(true))
                        .orElse(Collections.emptyList());

        List<CounterpartyModel> counterpartyModels;
        List<CounterpartyModel> ashburtonCounterpartyModels;

        try {
            counterpartyModels = Optional.ofNullable(approvalForumExternalService.getCounterparties(false, 0))
                    .orElse(Collections.emptyList());

            logger.info("Fetched {} CASDM counterparties for cleanup", counterpartyModels.size());

            counterpartyModels.forEach(cpymodel -> logger.info("***cpyModel: (inkombaId:{}, cpy:{}, CA:{})",
                    cpymodel.getCreditInkombaId(), cpymodel.getCounterpartyName(), cpymodel.getCreditAnalyst()));

            ashburtonCounterpartyModels = Optional.ofNullable(approvalForumExternalService.getCounterpartiesForAshburton(false))
                    .orElse(Collections.emptyList());

            logger.info("Fetched {} ASHBURTON counterparties for cleanup", ashburtonCounterpartyModels.size());

        } catch (ExternalResourceException e) {
            throw new ResourceException(e.getMessage());
        }

        for (ApprovalForumWfWorkflowEntity entity : workflowEntities) {
            // Ensure one bad entity doesn't stop the entire cleanup
            try {
                final Integer workflowId = (entity != null) ? entity.getWfId() : null;
                final String systemName = (entity != null) ? entity.getWorkflowSystemName() : null;

                Collection<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> details =
                        (entity != null)
                                ? entity.getWfcdWorkflowCounterpartyDetailByWfId()
                                : null;

                if (details == null || details.isEmpty()) {
                    logger.warn("Skipping workflowId={} (no counterparty details found).", workflowId);
                    continue;
                }

                // Safely get the first detail
                Iterator<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> iterator = details.iterator();
                if (!iterator.hasNext()) {
                    logger.warn("Skipping workflowId={} (counterparty details iterator empty).", workflowId);
                    continue;
                }
                ApprovalForumWfcdWorkflowCounterpartyDetailEntity detail = iterator.next();

                String workflowCreditInkombaId = trimToNull(detail.getCreditInkombaId());
                String workflowCounterpartyName = trimToNull(detail.getCounterpartyName());

                String tenantId = trimToNull(getTenantIdByWfId(workflowId));

                logger.info("***Processing workflowId={}, system={}, tenant={}, inkombaId={}, counterparty={}",
                        workflowId, systemName, tenantId, workflowCreditInkombaId, workflowCounterpartyName);

                // LUMOS: always cancel
                if ("LUMOS".equals(systemName)) {
                    logger.info("LUMOS -> cancel workflowId={}", workflowId);
                    cancelCleanupRequest(entity);
                    continue;
                }

                // ASHBURTON: cancel if not present in Ashburton active list
                if ("ASHBURTON".equalsIgnoreCase(tenantId)) {
                    boolean exists = ashburtonCounterpartyModels.stream()
                            .filter(Objects::nonNull)
                            .map(CounterpartyModel::getCreditInkombaId)
                            .anyMatch(id -> safeEqualsIgnoreCase(id, workflowCreditInkombaId));

                    if (!exists) {
                        logger.info("ASHBURTON: No active counterparty for inkombaId={} -> cancel workflowId={}",
                                workflowCreditInkombaId, workflowId);
                        cancelCleanupRequest(entity);
                    } else {
                        logger.debug("ASHBURTON: Active counterparty still present for inkombaId={} (workflowId={})",
                                workflowCreditInkombaId, workflowId);
                    }
                    continue;
                }

                // CASDM: cancel if not present in CASDM active list
                if ("CASDM".equalsIgnoreCase(tenantId)) {
                    CounterpartyModel match = counterpartyModels.stream()
                            .filter(Objects::nonNull)
                            .filter(cp -> safeEqualsIgnoreCase(cp.getCreditInkombaId(), workflowCreditInkombaId))
                            .findFirst()
                            .orElse(null);

                    if (match == null) {
                        logger.info("CASDM: No active counterparty for inkombaId={} -> cancel workflowId={}",
                                workflowCreditInkombaId, workflowId);
                        cancelCleanupRequest(entity);
                    } else {
                        // Safe, diagnostic logging guarded against nulls
                        logger.debug("CASDM: Found active counterparty for workflowId={} (name={}, id={}, CA={}, CAEmail={}, finInstType={})",
                                workflowId,
                                nullToEmpty(match.getCounterpartyName()),
                                nullToEmpty(match.getCreditInkombaId()),
                                nullToEmpty(match.getCreditAnalyst()),
                                nullToEmpty(match.getCreditAnalystMail()),
                                nullToEmpty(match.getFinancialInstitutionType()));
                    }
                    continue;
                }

                logger.info("workflowId={} has unsupported/unknown tenant='{}'; no cleanup performed.", workflowId, tenantId);

            } catch (Exception ex) {
                // Prevent one failure from halting the cleanup run
                logger.error("Cleanup failed for workflowId={} due to {}",
                        (entity != null ? entity.getWfId() : null), ex.toString(), ex);
            }
        }
    }

    private static boolean safeEqualsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String nullToEmpty(String s) {
        return (s == null) ? "" : s;
    }

    private String getTenantIdByWfId(int wfId) {
        logger.info("getTenantIdByWfId:"+wfId);
        int tenantId= approvalForumDataRepository.getTenantId(wfId);
        logger.info("getTenantIdByWfId:tenantId:"+tenantId);
        if(tenantId == 2){
            return "ASHBURTON";
        } else {
            return "CASDM";
        }
    }

    private int getTenantIdForWfId(int wfId) {
        logger.info("getTenantIdByWfId:"+wfId);
        int tenantId= approvalForumDataRepository.getTenantId(wfId);
        logger.info("getTenantIdByWfId:tenantId:"+tenantId);
        if(tenantId != 0) {
            return tenantId;
        }
        return tenantId;
    }

    /**
     * Case-insensitive wrapper for WorkflowStatus.fromValue()
     * Matches the status value against all WorkflowStatus enum values in a case-insensitive manner
     * @param statusValue The status value to match (case-insensitive)
     * @return The matching WorkflowStatus
     * @throws IllegalArgumentException if no match is found
     */
    private WorkflowStatus getWorkflowStatusCaseInsensitive(String statusValue) {
        for (WorkflowStatus status : WorkflowStatus.values()) {
            if (status.value().equalsIgnoreCase(statusValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No matching WorkflowStatus found for value: " + statusValue);
    }

    private void cancelCleanupRequest(ApprovalForumWfWorkflowEntity entity) throws ExternalResourceException, ResourceException {
        //Get current status on CASDM and Lumos
        logger.info("Get current status on CASDM / Lumos");
        int applicationId=0;
      /*  int applicationId = entity.getWfcdWorkflowCounterpartyDetailByWfId().iterator().next().
                getWfadApprovalForumWorkflowApplicationDataByWfadId().getApplicationId();*/
        ApprovalForumWfadWorkflowApplicationDataEntity application = entity.getWfcdWorkflowCounterpartyDetailByWfId().iterator().next().
                getWfadApprovalForumWorkflowApplicationDataByWfadId();
        String tenantId = getTenantIdByWfId(entity.getWfId());
        /*if (applicationId > 0) {*/
        if (!isNull(application) && application.getApplicationId() > 0) {
            applicationId = application.getApplicationId();
            if ("LUMOS".equals(entity.getWorkflowSystemName())) {
                WorkflowResponseDTO workflowResponseDTO = lumosRestService.findLumosWorkflowById(applicationId);

                if (isNull(workflowResponseDTO) || isNull(workflowResponseDTO.getWorkflow())) {
                    logger.info("Could not find application {} on Lumos", applicationId);
                } else {
                    logger.info("Status on LUMOS for application {} is now '{}'", applicationId, workflowResponseDTO.getWorkflow().getStatus());
                    if (!"SUBMITTED___FOR___APPROVAL".equalsIgnoreCase(workflowResponseDTO.getWorkflow().getStatus())) {
                        cleanupCancelRequest(entity, workflowResponseDTO.getWorkflow().getStatus());
                    }
                }
            } else if("ASHBURTON".equalsIgnoreCase(tenantId)) {
                CreditApplicationByIdModel applicationByIdModel =
                        approvalForumExternalService.getAshburtonCreditApplicationUsingIdAllStatuses(applicationId);
                if (applicationByIdModel == null || (applicationByIdModel.getCreditInkombaId() == null || "".equals(applicationByIdModel.getCreditInkombaId().trim()))) {
                    logger.info("Could not find application {} on ASHBURTON:", applicationId);

                } else {
                    logger.info("Status on ASHBURTON for application {} is now '{}'", applicationId, applicationByIdModel.getCreditApplicationStatus());
                    if (!"Submitted for Approval".equalsIgnoreCase(applicationByIdModel.getCreditApplicationStatus())) {
                        cleanupCancelRequest(entity, applicationByIdModel.getCreditApplicationStatus());
                    }
                }
            }
            else {
                CreditApplicationByIdModel applicationByIdModel =
                        approvalForumExternalService.getCreditApplicationUsingIdAllStatuses(applicationId);
                logger.info("***applicationByIdModel(inkombaId:{}, appId:{}, appStatus:{}, meetingName:{})",
                        applicationByIdModel.getCreditInkombaId(), applicationByIdModel.getApplicationId(),
                        applicationByIdModel.getCreditApplicationStatus(), applicationByIdModel.getMeetingName());

                if (applicationByIdModel == null || (applicationByIdModel.getCreditInkombaId() == null ||
                        "".equals(applicationByIdModel.getCreditInkombaId().trim()))) {
                    logger.info("Could not find application {} on CASDM", applicationId);
                } else {
                    logger.info("Status on CASDM for application {} is now '{}'", applicationId, applicationByIdModel.getCreditApplicationStatus());
                    if (!"Submitted for Approval".equalsIgnoreCase(applicationByIdModel.getCreditApplicationStatus())) {
                        cleanupCancelRequest(entity, applicationByIdModel.getCreditApplicationStatus());
                    }
                }
            }
        } else {
            logger.error("Workflow / Application Id is 0, skipping DAF workflow : " + entity.getWfId());
        }
    }

    /**
     * @param entity
     * @param decision
     * @throws ResourceException TODO : Check if this cleanupCancelRequest does not impact Lumos
     */
    private void cleanupCancelRequest(ApprovalForumWfWorkflowEntity entity, String decision) throws ResourceException {
        logger.info("***cleanupCancelRequest entity({}), decision({})", entity.toString(), decision);
        if (entity == null) return;
        boolean isWip = "wip".equalsIgnoreCase(entity.getStatus());
        String userName = "System";
        if (isWip) {
            cancelRequest(entity.getWfId(), userName);
        } else {
            setAuditAndLastUpdatedDetails("System", "Submitted workflow decision for " + userName, entity.getWfId());
            String tenantId = getTenantIdByWfId(entity.getWfId());
            String statusToSet;

            // Determine the status value based on whether it already has a system suffix
            if (decision.endsWith(" (CASDM)") || decision.endsWith(" (ASHBURTON)") || decision.endsWith(" (LUMOS)")) {
                // Decision already has system suffix, use as-is
                statusToSet = decision;
                logger.info("DEBUG cleanupCancelRequest: decision already has system suffix: {}", decision);
            } else {
                // Append appropriate system suffix based on workflow system
                if ("LUMOS".equals(entity.getWorkflowSystemName())) {
                    statusToSet = decision + " (LUMOS)";
                } else if ("ASHBURTON".equalsIgnoreCase(tenantId)) {
                    statusToSet = decision + " (ASHBURTON)";
                } else {
                    statusToSet = decision + " (CASDM)";
                }
                logger.info("DEBUG cleanupCancelRequest: appending system suffix. Original: '{}', Final: '{}'", decision, statusToSet);
            }

            // Use case-insensitive matching to find the WorkflowStatus
            WorkflowStatus workflowStatus = getWorkflowStatusCaseInsensitive(statusToSet);
            entity.setStatus(workflowStatus.value());
            setAuditAndLastUpdatedDetails(userName, "Workflow Status Updated to " + workflowStatus.value(), entity.getWfId());

            setToInactive(entity);
            approvalForumMailService.sendRequestCompleteMail(entity.getWfId());

            notifyFrontEndUser(entity.getWfId());
        }
    }

    @Override
    public void processExternalRequests() {
        approvalForumDataRepository.getOutstandingApprovalForumErExternalRequests().forEach(entity -> {
            ProcessExternalRequestResponse response;
            List<ApprovalForumWfadWorkflowApplicationDataEntity> applicationDataEntities = approvalForumDataRepository
                    .getApplicationDataByAppIdAndLumos(entity.getApplicationId(), entity.isLumos());
            if (entity.isCreate()) {
                if (applicationDataEntities.isEmpty()) {

                    if (!entity.isLumos()) {
                        logger.info("Processing CASDM external request.");
                        response = processExternalRequestsCreate(entity.getApplicationId());
                        logger.info("Done processing CASDM external request.");
                    } else {
                        Integer tempTenantId = entity.getTenantId();
                        if (tempTenantId != null && tempTenantId != 0) {
                            logger.info("Processing Lumos external request for tenantId:"+entity.getTenantId());
                            response = processLumosExternalRequestsCreate(entity.getApplicationId(), 0, null, tempTenantId);
                            // tenantId is non-null and non-zero
                        } else{
                            logger.info("Processing Lumos external request.");
                            response = processLumosExternalRequestsCreate(entity.getApplicationId(), 0, null, 0);
                        }
                        logger.info("Done processing Lumos external request.");
                    }
                } else {
                    response = new ProcessExternalRequestResponse();
                    response.setSuccess(true);
                    response.setWorkflowId(
                            applicationDataEntities.get(0).getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId()
                                    .getWfApprovalForumWorkflowByWfId().getWfId());
                    response.setMessage("Already an active request for applicationId " + entity.getApplicationId());
                }
            } else {
                if (!applicationDataEntities.isEmpty()) {
                    response = processExternalRequestsCancel(applicationDataEntities.get(0));
                } else {
                    response = new ProcessExternalRequestResponse();
                    response.setSuccess(true);
                    response.setMessage("No active requests found for applicationId " + entity.getApplicationId());
                }
            }
            logger.info("Updating table ER_ExternalRequests request for external request wid ID : " + entity.getApplicationId());
            updateRequestExternal(entity, response);
            logger.info("Done updating table ER_ExternalRequests request for external request wid ID : " + entity.getApplicationId());
        });
    }

    private void updateRequestExternal(ApprovalForumErExternalRequestsEntity entity,
                                       ProcessExternalRequestResponse response) {
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        entity.setComplete(response.isSuccess());
        if (entity.isComplete()) entity.setCompletedOn(now);
        entity.setRetryCount(entity.getRetryCount() + 1);
        entity.setLastTriedOn(now);
        entity.setLastTriedMessage(response.getMessage());
        if (entity.getWfId() == null || entity.getWfId() == 0) {
            if (response.getWorkflowId() != null && response.getWorkflowId() > 0) {
                entity.setWfId(response.getWorkflowId());
            }
        }
        approvalForumDataRepository.updateApprovalForumErExternalRequestsEntity(entity);
    }

    @Override
    public void sendMailEscalations() {
        Date now = Calendar.getInstance().getTime();
        int firstEscalationHours = Integer.parseInt(afSettings.get("af.firstescalation.hours"));
        int secondEscalationHours = Integer.parseInt(afSettings.get("af.secondescalation.hours"));
        for (ApprovalForumWfWorkflowEntity entity : approvalForumDataRepository.getWorkflows(true)) {
            if (entity.getSubmittedOn() != null) {
                int hours = approvalForumExternalService.hoursBetweenBusinessDates(new Date(entity.getSubmittedOn().getTime()), now);
                if (hours < secondEscalationHours && hours >= firstEscalationHours && !entity.isFirstEscalationSent()) {
                    approvalForumMailService.sendSlaReminderMail(entity.getWfId(), true);
                    entity.setFirstEscalationSent(true);
                    approvalForumDataRepository.updateWorkflow(entity);
                } else if (hours >= secondEscalationHours && !entity.isSecondEscalationSent()) {
                    approvalForumMailService.sendSlaReminderMail(entity.getWfId(), false);
                    entity.setSecondEscalationSent(true);
                    approvalForumDataRepository.updateWorkflow(entity);
                }
            }
        }
    }

    @Override
    public void sendApproverMailEscalation() {

        approvalForumMailService.sendApproverSlaReminderMail(buildApproverEmailNotificationMap());

    }

    private HashMap<String, HashMap<String, List<WorkflowMailDTO>>> buildApproverEmailNotificationMap() {

        // HashMap<emailOfPersonToSendTo, HashMap<KeyForWhetherActionIsRequiredOrNot, List<WorkflowMailDTO>>>
        // HashMap<shc@rmb.co.za, HashMap<ACTION, List<WorkflowMailDTO>>>
        //                        HashMap<NO_ACTION, List<WorkflowMailDTO>>>

        HashMap<String, HashMap<String, List<WorkflowMailDTO>>> recipientsToMailTo = new HashMap<String, HashMap<String, List<WorkflowMailDTO>>>();

        try {
            List<ApprovalForumWfWorkflowEntity> workflowsAwaitingApproval = approvalForumDataRepository.getWorkflows(true).stream()
                    .filter(entity -> entity.getStatus().equalsIgnoreCase(WorkflowStatus.AWAITING_APPROVAL.value()))
                    .collect(Collectors.toCollection(ArrayList::new));

            for (ApprovalForumWfWorkflowEntity entity : workflowsAwaitingApproval) {

                List<OutstandingApprovals> outstandingApprovals = getOutstandingApprovals(entity.getWfId()).getOutstandingApprovals();

                if (!outstandingApprovals.isEmpty()) {

                    Integer numberOfOutstandingApprovals = outstandingApprovals.stream()
                            .collect(Collectors.summingInt(s -> s.getCount()));

                    Collection<ApprovalForumWfaWorkflowApprovalsEntity> approversForThisWorkflow = entity.getWfaWorkflowApprovalsByWfId();

                    logger.info("WORKFLOW " + entity.getWfId());
                    approversForThisWorkflow.stream().forEach(app -> logger.info("Approver for workflow " + entity.getWfId() + " "
                            + app.getName() + " " + app.getSurname()
                            + " DateAssigned: " + app.getDateAssigned() + " DateCompleted: " + app.getDateCompleted()));

                    // Returns the emails of the people who have already approved this workflow
                    outstandingApprovals.stream().forEach(out -> logger.info("Outstanding approvals " + out.getName() + " " + out.getCount()));

                    List<String> noActionRequired = getEmailsOfApprovalsWithNoActionRequired(entity);

                    List<String> actionRequired = getEmailsOfApprovals(entity, false);

                    actionRequired.stream().forEach(x -> noActionRequired.remove(x));

                    noActionRequired.stream().forEach(e -> logger.info("NO ACTION REQUIRED " + e));

                    actionRequired.stream().forEach(e -> logger.info("ACTION REQUIRED " + e));

                    WorkflowMailDTO workflowMailDTO = buildWorkflowMail(entity.getWfId());

                    workflowMailDTO.setOutstanding(numberOfOutstandingApprovals);

                    if (noActionRequired.size() > 0) {
                        noActionRequired.stream().forEach(email -> buildEmailRecipientsMap(recipientsToMailTo, workflowMailDTO, email, ApprovalForumMailServiceImpl.Notification.NO_ACTION));
                    }

                    if (actionRequired.size() > 0) {
                        actionRequired.stream().forEach(email -> buildEmailRecipientsMap(recipientsToMailTo, workflowMailDTO, email, ApprovalForumMailServiceImpl.Notification.ACTION));
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return recipientsToMailTo;
    }

    /*    private HashMap<String, HashMap<String, List<WorkflowMailDTO>>> buildApproverEmailNotificationMap() {

        // HashMap<emailOfPersonToSendTo, HashMap<KeyForWhetherActionIsRequiredOrNot, List<WorkflowMailDTO>>>
        // HashMap<shc@rmb.co.za, HashMap<ACTION, List<WorkflowMailDTO>>>
        //                        HashMap<NO_ACTION, List<WorkflowMailDTO>>>

        HashMap<String, HashMap<String, List<WorkflowMailDTO>>> recipientsToMailTo = new HashMap<String, HashMap<String, List<WorkflowMailDTO>>> ();

        try {
            List<ApprovalForumWfWorkflowEntity> workflowsAwaitingApproval = approvalForumDataRepository.getWorkflows(true).stream()
                    .filter(entity -> entity.getStatus().equalsIgnoreCase(WorkflowStatus.AWAITING_APPROVAL.value()))
                    .collect(Collectors.toCollection(ArrayList::new));

            for (ApprovalForumWfWorkflowEntity entity : workflowsAwaitingApproval) {

                String meetingName = getMeetingNameFromCounterpartyEntities(approvalForumDataRepository.getCounterpartyDetailForWorkflowId(entity.getWfId()));

                List<OutstandingApprovals> outstandingApprovals = getOutstandingApprovals(entity.getWfId()).getOutstandingApprovals();

                if (!outstandingApprovals.isEmpty()) {

                    Integer numberOfOutstandingApprovals = outstandingApprovals.stream()
                            .collect(Collectors.summingInt(s -> s.getCount()));

                    Collection<ApprovalForumWfaWorkflowApprovalsEntity> approversForThisWorkflow = entity.getWfaWorkflowApprovalsByWfId();

                    // Returns the emails of the people who have already approved this workflow
                    List<String> haveAlreadyApprovedWorkflow = getEmailsOfApprovals(entity, true);

                    for (ApprovalForumWfaWorkflowApprovalsEntity workflowApprover : approversForThisWorkflow) {

                        WorkflowMailDTO workflowMailDTO = buildWorkflowMail(entity.getWfId());

                        workflowMailDTO.setOutstanding(numberOfOutstandingApprovals);

                        String approverEmail = workflowApprover.getEmail();

                        // This user has already approved the mandate so gets added to their NO_ACTION list
                        if (haveAlreadyApprovedWorkflow.stream().anyMatch(email -> email.equalsIgnoreCase(workflowApprover.getEmail()) )) {
                            buildEmailRecipientsMap(recipientsToMailTo, workflowMailDTO, approverEmail, ApprovalForumMailServiceImpl.Notification.NO_ACTION);
                        } else {
                            // This user hasn't approved the mandate and is on the list of possible approvers
                            // so we need to check if they're part of specific approvers/quorum...if so, they need to action
                            // if not it can be added to their NO ACTION list
                            AssignResponse assignResponse = checkIfWorkflowCanBeAssigned(entity.getWfId(), workflowApprover.getUserId());

                            if (assignResponse.getResponse().equals(AssignedTypes.CAN_BE_ASSIGNED)) {
                                buildEmailRecipientsMap(recipientsToMailTo, workflowMailDTO, approverEmail, ApprovalForumMailServiceImpl.Notification.ACTION);
                            } else {
                                buildEmailRecipientsMap(recipientsToMailTo, workflowMailDTO, approverEmail, ApprovalForumMailServiceImpl.Notification.NO_ACTION);
                            }
                        }

                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return recipientsToMailTo;
    }
*/

    private void buildEmailRecipientsMap(HashMap<String, HashMap<String, List<WorkflowMailDTO>>> recipientsToMailTo, WorkflowMailDTO workflowMailDTO, String approverEmail, ApprovalForumMailServiceImpl.Notification action2) {

        try {

            List<WorkflowMailDTO> action;

            if (recipientsToMailTo.containsKey(approverEmail)) {
                logger.info("*** Recipient already has " + approverEmail);
                if (recipientsToMailTo.get(approverEmail).containsKey(action2.name())) {
                    logger.info("*** " + approverEmail + " has " + action2.name());

                    action = recipientsToMailTo.get(approverEmail).get(action2.name());

                    action.add(workflowMailDTO);

                    // Remove duplicate workflows
                    action = action.stream()
                            .filter(distinctByKey(p -> p.getWorkflowId()))
                            .collect(Collectors.toList());

                    recipientsToMailTo.get(approverEmail).replace(action2.name(), action);

                } else {
                    logger.info("*** " + action2.name() + " doesn't have " + approverEmail + ", added ");
                    action = new ArrayList<>();
                    action.add(workflowMailDTO);
                    recipientsToMailTo.get(approverEmail).put(action2.name(), action);
                }

            } else {
                action = new ArrayList<>();
                logger.info("*** " + action2.name() + " adding " + approverEmail);

                action.add(workflowMailDTO);
                HashMap<String, List<WorkflowMailDTO>> actionMap = new HashMap<>();
                actionMap.put(action2.name(), action);
                recipientsToMailTo.put(approverEmail, actionMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendCapturerMailEscalation() {

        approvalForumMailService.sendCapturerSlaReminderMail(buildCapturerEmailNotificationMap());

    }

    private HashMap<String, List<WorkflowMailDTO>> buildCapturerEmailNotificationMap() {

        // HashMap<emailOfPersonToSendTo, List<WorkflowMailDTO>>
        // e.g.
        // HashMap<shc@rmb.co.za, List of Workflows shc has captured that have not yet been approved>
        //

        HashMap<String, List<WorkflowMailDTO>> recipientsToMailTo = new HashMap<String, List<WorkflowMailDTO>>();

        try {

            List<ApprovalForumWfWorkflowEntity> workflowsAwaitingApproval = approvalForumDataRepository.getWorkflows(true).stream()
                    .filter(entity -> entity.getStatus().equalsIgnoreCase(WorkflowStatus.AWAITING_APPROVAL.value()))
                    .collect(Collectors.toCollection(ArrayList::new));

            for (ApprovalForumWfWorkflowEntity entity : workflowsAwaitingApproval) {

                String meetingName = getMeetingNameFromCounterpartyEntities(approvalForumDataRepository.getCounterpartyDetailForWorkflowId(entity.getWfId()));

                List<OutstandingApprovals> outstandingApprovals = getOutstandingApprovals(entity.getWfId()).getOutstandingApprovals();

                List<WorkflowMailDTO> workflowsPerCapturer = new ArrayList<WorkflowMailDTO>();

                if (!outstandingApprovals.isEmpty()) {

                    Integer numberOfOutstandingApprovals = outstandingApprovals.stream()
                            .collect(Collectors.summingInt(s -> s.getCount()));

                    WorkflowMailDTO dto = buildWorkflowMail(entity.getWfId());

                    dto.setOutstanding(numberOfOutstandingApprovals);

                    if (recipientsToMailTo.containsKey(entity.getSubmittedBy())) {
                        workflowsPerCapturer = recipientsToMailTo.get(entity.getSubmittedBy());
                        workflowsPerCapturer.add(dto);

                        // Remove duplicate workflows
                        workflowsPerCapturer = workflowsPerCapturer.stream()
                                .filter(distinctByKey(p -> p.getWorkflowId()))
                                .collect(Collectors.toList());

                        recipientsToMailTo.replace(entity.getSubmittedBy(), workflowsPerCapturer);

                    } else {
                        workflowsPerCapturer.add(dto);
                        recipientsToMailTo.put(entity.getSubmittedBy(), workflowsPerCapturer);
                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return recipientsToMailTo;
    }

    @SuppressWarnings("Duplicates")
    private List<String> getEmailsOfApprovals(ApprovalForumWfWorkflowEntity entity, boolean approved) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvals = new ArrayList<>();
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowId(entity.getWfId());

        if (approvalsEntities == null || approvalsEntities.isEmpty()) return new ArrayList<>();

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<Integer> listManagementIds = new ArrayList<>();
        quorumDataEntities.forEach(quorumDataEntity -> {
            if (!listManagementIds.contains(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())) {
                listManagementIds.add(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId());
            }
        });

        if (approved) {
            if (entity.isSpecific() || entity.isConfidential()) {
                approvalsEntities.stream().filter(approver -> (approver.isSpecificApprover() || approver.isAdditionalApprover())
                                && approver.getDateCompleted() != null)
                        .collect(Collectors.toCollection(() -> approvals));
            } else {
                approvalsEntities.stream().filter((approver -> approver.isAdditionalApprover() ||
                                listManagementIds.contains(approver.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId()) &&
                                        approver.getDateCompleted() != null))
                        .collect(Collectors.toCollection(() -> approvals));
            }
        } else {
            if (entity.isSpecific() || entity.isConfidential()) {
                approvalsEntities.stream().filter(approver -> (approver.isSpecificApprover() || approver.isAdditionalApprover())
                                && approver.getDateCompleted() == null)
                        .collect(Collectors.toCollection(() -> approvals));
            } else {
                approvalsEntities.stream().filter((approver -> approver.isAdditionalApprover() ||
                                listManagementIds.contains(approver.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId()) &&
                                        approver.getDateCompleted() == null))
                        .collect(Collectors.toCollection(() -> approvals));
            }
        }
        List<String> emails = new ArrayList<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvals) {
            if (emails.indexOf(approvalsEntity.getEmail().trim()) < 0) {
                emails.add(approvalsEntity.getEmail().trim());
            }
        }
        return emails;
    }

    @SuppressWarnings("Duplicates")
    private List<String> getEmailsOfApprovalsWithActionRequired(ApprovalForumWfWorkflowEntity entity) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvals = new ArrayList<>();
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowId(entity.getWfId());

        if (approvalsEntities == null || approvalsEntities.isEmpty()) return new ArrayList<>();

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<Integer> listManagementIds = new ArrayList<>();
        quorumDataEntities.forEach(quorumDataEntity -> {
            if (!listManagementIds.contains(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())) {
                listManagementIds.add(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId());
            }
        });

        if (entity.isSpecific() || entity.isConfidential()) {

            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalsEntities) {
                if (approvalsEntity.isSpecificApprover() || approvalsEntity.isAdditionalApprover()) {
                    if (approvalsEntity.getDateCompleted() == null) {
                        approvals.add(approvalsEntity);
                    }
                }
            }

        } else {
            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalsEntities) {
                if (approvalsEntity.isSpecificApprover() || approvalsEntity.isAdditionalApprover()) {
                    if (approvalsEntity.getDateCompleted() == null) {
                        approvals.add(approvalsEntity);
                    }
                } else {
                    if (listManagementIds.contains(approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())
                            && approvalsEntity.getDateAssigned() != null) {
                        approvals.add(approvalsEntity);
                    }
                }
            }
        }

        List<String> emails = new ArrayList<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvals) {
            if (emails.indexOf(approvalsEntity.getEmail().trim()) < 0) {
                emails.add(approvalsEntity.getEmail().trim());
            }
        }
        return emails;
    }

    @SuppressWarnings("Duplicates")
    private List<String> getEmailsOfApprovalsWithNoActionRequired(ApprovalForumWfWorkflowEntity entity) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvals = new ArrayList<>();
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowId(entity.getWfId());

        if (approvalsEntities == null || approvalsEntities.isEmpty()) return new ArrayList<>();

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<Integer> listManagementIds = new ArrayList<>();
        quorumDataEntities.forEach(quorumDataEntity -> {
            if (!listManagementIds.contains(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())) {
                listManagementIds.add(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId());
            }
        });

        if (entity.isSpecific() || entity.isConfidential()) {

            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalsEntities) {
                if (approvalsEntity.isSpecificApprover() || approvalsEntity.isAdditionalApprover()) {
                    if (approvalsEntity.getDateCompleted() != null) {
                        approvals.add(approvalsEntity);
                    }
                } else {
                    approvals.add(approvalsEntity);
                }
            }

        } else {
            for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvalsEntities) {
                if (approvalsEntity.isSpecificApprover() || approvalsEntity.isAdditionalApprover()) {
                    if (approvalsEntity.getDateCompleted() != null) {
                        approvals.add(approvalsEntity);
                    }
                } else {
                    if (listManagementIds.contains(approvalsEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())
                            && approvalsEntity.getDateAssigned() == null) {
                        approvals.add(approvalsEntity);
                    }
                }
            }
        }

        List<String> emails = new ArrayList<>();
        for (ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity : approvals) {
            if (emails.indexOf(approvalsEntity.getEmail().trim()) < 0) {
                emails.add(approvalsEntity.getEmail().trim());
            }
        }
        return emails;
    }

    private WorkflowMailDTO buildWorkflowMail(int workflowId) {

        int firstEscalationHours = Integer.parseInt(afSettings.get("af.firstescalation.hours"));
        int secondEscalationHours = Integer.parseInt(afSettings.get("af.secondescalation.hours"));

        WorkflowMailDTO mailDTO = new WorkflowMailDTO();

        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);

        mailDTO.setWorkflowId(workflowId);
        mailDTO.setCounterpartyName(getCounterPartyName(counterpartyDetailEntities));
        mailDTO.setMandate(getMandate(counterpartyDetailEntities));

        mailDTO.setSubmittedBy(approvalForumAuthService.getActiveDirectoryUserDisplayNameByAliasIgnoreCase(entity.getSubmittedBy()));

        String createdDatePattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter createdDateFormatter = DateTimeFormatter.ofPattern(createdDatePattern);

        mailDTO.setCreatedDate(createdDateFormatter.format(entity.getCreatedOn().toLocalDateTime()));

        String dafLink = "";
        if (afSettings.get("af.mail.base").endsWith("/")) {
            dafLink = "<a href=\"" + afSettings.get("af.mail.base") + "approve/" + workflowId + "\">" + workflowId + "</a>";
        } else {
            dafLink = "<a href=\"" + afSettings.get("af.mail.base") + "/approve/" + workflowId + "\">" + workflowId + "</a>";
        }

        mailDTO.setDafLink(dafLink);
        Date now = Calendar.getInstance().getTime();
        int hours = approvalForumExternalService.hoursBetweenBusinessDates(new Date(entity.getSubmittedOn().getTime()), now);

        if (hours < firstEscalationHours) {
            mailDTO.setFirst(true);
        } else if ((hours >= firstEscalationHours) && (hours <= secondEscalationHours)) {
            mailDTO.setSecond(true);
        } else {
            mailDTO.setThird(true);
        }

        mailDTO.setAgingStatus(hours + " hours");

        return mailDTO;

    }

    private String getCounterPartyName(List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities) {
        if (counterpartyDetailEntities == null || counterpartyDetailEntities.isEmpty()) {
            return "Unknown";
        }
        return counterpartyDetailEntities.iterator().next().getCounterpartyName();
    }

    private String getMandate(List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities) {
        if (counterpartyDetailEntities == null || counterpartyDetailEntities.isEmpty()) {
            return "Unknown";
        }
        return counterpartyDetailEntities.iterator().next().getWfadApprovalForumWorkflowApplicationDataByWfadId().getMeetingName();
    }

    @Override
    public List<String> getInkombaIdsForActiveWorkflows() {
        List<String> inkombaIds = new ArrayList<>();
        approvalForumDataRepository.getInkombaIdsForActiveWorkflows().forEach(entity -> {
            if (inkombaIds.indexOf(entity.getCreditInkombaId()) < 0) {
                inkombaIds.add(entity.getCreditInkombaId());
            }
        });
        return inkombaIds;
    }


    @Override
    public NewRequestResponseDTO getNewRequestDetails(String inkombaId, String meetingName) throws ResourceException {
        NewRequestResponseDTO responseDTO = new NewRequestResponseDTO();
        boolean reviewAnalystFound = true;
        boolean creditAnalystFound = true;

        try {
            responseDTO.setCounterpartyFullDetail(approvalForumExternalService.getCounterpartyFullDetail(inkombaId, afSettings.get("af.facility.statuses")));
            if (meetingName != null && !"".equals(meetingName.trim())) {
                responseDTO.getCounterpartyFullDetail().getCreditApplication().setMeetingName(meetingName);
            }
            responseDTO.setRulesResponse(
                    new RulesResponseDTO().withResponse(processRules(getRulesRequestFromCounterpartyFull(responseDTO.getCounterpartyFullDetail()))));
            responseDTO.setUsersResponse(new UsersResponseDTO().withResponse(approvalForumListManagementService.getAllUsers()));

            String reviewAnalystId = responseDTO.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystId();
            String reviewAnalystEmail = responseDTO.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystEmail();
            if ((reviewAnalystId == null || "".equals(reviewAnalystId.trim())) && (reviewAnalystEmail == null || "".equals(reviewAnalystEmail.trim())))
                reviewAnalystFound = false;
            if (reviewAnalystFound) {
                //Remove review analyst from all lists
                removeUserFromAllLists(responseDTO, reviewAnalystId, reviewAnalystEmail);
            }

            String creditAnalystId = responseDTO.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystId();
            String creditAnalystEmail = responseDTO.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystEmail();
            if ((creditAnalystId == null || "".equals(creditAnalystId.trim())) && (creditAnalystEmail == null || "".equals(creditAnalystEmail.trim())))
                creditAnalystFound = false;
            if (creditAnalystFound) {
                //Remove review analyst from all lists
                removeUserFromAllLists(responseDTO, creditAnalystId, creditAnalystEmail);
            }

            String meeting = responseDTO.getCounterpartyFullDetail().getCreditApplication().getMeetingName();

            responseDTO.setChairpersonApprovalRequired(isChairpersonApprovalRequired(meeting));

        }catch(ExternalResourceException e){
            throw new ResourceException(e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public NewRequestResponseDTO getNewRequestDetailsForAshburton(String inkombaId, String meetingName, int appId) throws ResourceException {
        NewRequestResponseDTO responseDTO = new NewRequestResponseDTO();
        boolean reviewAnalystFound = true;
        boolean creditAnalystFound = true;

        try {
            logger.info("getNewRequestDetailsForAshburton1 ..."+meetingName);
            responseDTO.setCounterpartyFullDetail(approvalForumExternalService.getAshburtonCounterpartyFullDetail(inkombaId, appId, afSettings.get("af.facility.statuses")));
            logger.info("getNewRequestDetailsForAshburton2 ...");
            if (meetingName != null && !"".equals(meetingName.trim())) {
                responseDTO.getCounterpartyFullDetail().getCreditApplication().setMeetingName(meetingName);
            }
            logger.info("getNewRequestDetailsForAshburton3 ..."+responseDTO.getCounterpartyFullDetail().getCreditApplication().getMeetingName());
            responseDTO.setRulesResponse(
                    new RulesResponseDTO().withResponse(processRules(getRulesRequestFromCounterpartyFull(responseDTO.getCounterpartyFullDetail()))));
            responseDTO.setUsersResponse(new UsersResponseDTO().withResponse(approvalForumListManagementService.getAllUsers()));
            logger.info("getNewRequestDetailsForAshburton4 ..."+meetingName);
            String reviewAnalystId = responseDTO.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystId();
            String reviewAnalystEmail = responseDTO.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystEmail();
            logger.info("getNewRequestDetailsForreviewAnalystId ..."+reviewAnalystId);
            if ((reviewAnalystId == null || "".equals(reviewAnalystId.trim())) && (reviewAnalystEmail == null || "".equals(reviewAnalystEmail.trim())))
                reviewAnalystFound = false;
            if (reviewAnalystFound) {
                //Remove review analyst from all lists
                removeUserFromAllLists(responseDTO, reviewAnalystId, reviewAnalystEmail);
            }

            String creditAnalystId = responseDTO.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystId();
            String creditAnalystEmail = responseDTO.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystEmail();
            if ((creditAnalystId == null || "".equals(creditAnalystId.trim())) && (creditAnalystEmail == null || "".equals(creditAnalystEmail.trim())))
                creditAnalystFound = false;
            if (creditAnalystFound) {
                //Remove review analyst from all lists
                removeUserFromAllLists(responseDTO, creditAnalystId, creditAnalystEmail);
            }

            String meeting = responseDTO.getCounterpartyFullDetail().getCreditApplication().getMeetingName();

            responseDTO.setChairpersonApprovalRequired(isChairpersonApprovalRequired(meeting));

        }catch(ExternalResourceException e){
            throw new ResourceException(e.getMessage());
        }
        return responseDTO;
    }

    private NewRequestResponseDTO getLumosNewRequestDetails(CounterpartyFullDetail counterpartyFullDetail) throws ResourceException {
        logger.info("getLumosNewRequestDetails ...");
        NewRequestResponseDTO responseDTO = new NewRequestResponseDTO();
        boolean reviewAnalystFound = true;
        boolean creditAnalystFound = true;

        responseDTO.setCounterpartyFullDetail(counterpartyFullDetail);
        if (counterpartyFullDetail.getCreditApplication().getMeetingName() != null && !"".equals(counterpartyFullDetail.getCreditApplication().getMeetingName().trim())) {
            responseDTO.getCounterpartyFullDetail().getCreditApplication().setMeetingName(counterpartyFullDetail.getCreditApplication().getMeetingName());
        } else {
            logger.warn("Working on counterparty : " + counterpartyFullDetail.getCreditApplication().getMeetingName() + ". Meeting name not available ...");
        }
        responseDTO.setRulesResponse(
                new RulesResponseDTO().withResponse(processRules(getRulesRequestFromCounterpartyFull(responseDTO.getCounterpartyFullDetail()))));
        responseDTO.setUsersResponse(new UsersResponseDTO().withResponse(approvalForumListManagementService.getAllUsers()));

        String reviewAnalystId = responseDTO.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystId();
        String reviewAnalystEmail = responseDTO.getCounterpartyFullDetail().getCreditApplication().getReviewAnalystEmail();
        if ((reviewAnalystId == null || "".equals(reviewAnalystId.trim())) && (reviewAnalystEmail == null || "".equals(reviewAnalystEmail.trim()))) {
            logger.warn("No review analyst not found ...");
            reviewAnalystFound = false;
        } else {
            logger.info("Review analyst : " + reviewAnalystId);
        }

        if (reviewAnalystFound) {
            //Remove review analyst from all lists
            removeUserFromAllLists(responseDTO, reviewAnalystId, reviewAnalystEmail);
        }

        String creditAnalystId = responseDTO.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystId();
        String creditAnalystEmail = responseDTO.getCounterpartyFullDetail().getCreditApplication().getCreditAnalystEmail();
        if ((creditAnalystId == null || "".equals(creditAnalystId.trim())) && (creditAnalystEmail == null || "".equals(creditAnalystEmail.trim()))) {
            logger.warn("No credit analyst found ...");
            creditAnalystFound = false;
        } else {
            logger.info("Credit analyst : " + creditAnalystId);
        }

        if (creditAnalystFound) {
            //Remove review analyst from all lists
            logger.warn("No review analyst found ...");
            removeUserFromAllLists(responseDTO, creditAnalystId, creditAnalystEmail);
        } else {
            logger.info("Review analyst found ...");
        }

        return responseDTO;
    }

    private void removeUserFromAllLists(NewRequestResponseDTO responseDTO, String userId, String email) {
        logger.info("Checking  list management...");
        logger.info("List management list count : " + responseDTO.getRulesResponse().getResponse().getListManagementList().size());
        List<ListManagementListUser> users;
        for (ListManagementList list : responseDTO.getRulesResponse().getResponse().getListManagementList()) {
            users = new ArrayList<>();
            for (ListManagementListUser listUser : list.getUsers()) {
                logger.info("Checking  list management..:"+listUser.getName()+":email:"+userId);
                if (!isUserEqualToListManUser(userId, email, listUser)) {
                    users.add(listUser);
                }
            }
            list.setUsers(users);
        }
        users = new ArrayList<>();
        for (ListManagementListUser listUser : responseDTO.getUsersResponse().getResponse().getUsers()) {
            if (!isUserEqualToListManUser(userId, email, listUser)) {
                users.add(listUser);
            }
        }
        responseDTO.getUsersResponse().getResponse().setUsers(users);
        logger.info("List  management user count : " + responseDTO.getUsersResponse().getResponse().getUsers().size());
    }

    @Override
    public NewRequestResponseDTO getNewRequestDetails(String inkombaId) throws ResourceException {
        return getNewRequestDetails(inkombaId, null);
    }

    @Override
    public NewRequestResponseDTO getNewRequestDetailsForAshburton(String inkombaId, int appId) throws ResourceException {
        return getNewRequestDetailsForAshburton(inkombaId, null, appId);
    }
    private RulesRequestDTO getRulesRequestFromCounterpartyFull(CounterpartyFullDetail counterpartyFullDetail) {
        logger.info("getRulesRequestFromCounterpartyFull(START)");
        RulesRequestDTO rulesRequestDTO = new RulesRequestDTO();
        rulesRequestDTO.setBank(counterpartyFullDetail.getCounterpartyDetail().isBank());
        rulesRequestDTO.setFinancialInstitutionType(counterpartyFullDetail.getCounterpartyDetail().getFinancialInstitutionType());

        rulesRequestDTO.setMeetingRule(approvalForumDataRepository.getMeetingQuorumRuleByMeetingName(
                        counterpartyFullDetail.getCreditApplication().getMeetingName())
                .getMqrMeetingQuorumRulesByMqrId().getName());
        rulesRequestDTO.setProposalType(counterpartyFullDetail.getCreditApplication().getProposalType());
        rulesRequestDTO.setDistressStatus(counterpartyFullDetail.getCreditApplication().getDistressStatus());

        List<String> riskTakerGroupCode = new ArrayList<>(0);
        List<FacilityMovementDTO> facilityMovement = new ArrayList<>(0);
        List<FacilityModel> facilities = counterpartyFullDetail.getFacility();
        for (FacilityModel facility : facilities) {
            if (!"".equals(facility.getRiskTakerGroupCode()) && !riskTakerGroupCode.contains(facility.getRiskTakerGroupCode())) {
                riskTakerGroupCode.add(facility.getRiskTakerGroupCode());
            }

            if (!"".equals(facility.getFacilityMovementType())) {

                FacilityMovementDTO facilityMovementDTO = facilityMovement.stream().filter(o -> facility.getRiskTakerGroupCode().equals(o.getRiskTakerGroupCode()))
                        .findFirst()
                        .orElse(null);

                if (facilityMovementDTO != null) {
                    if ("Unchanged".equals(facilityMovementDTO.getMovement())
                            && !"Unchanged".equals(facility.getFacilityMovementType())) {
                        facilityMovementDTO.setMovement("Changed");
                    }
                } else {
                    facilityMovementDTO = new FacilityMovementDTO();
                    facilityMovementDTO.setRiskTakerGroupCode(facility.getRiskTakerGroupCode());
                    if ("Unchanged".equals(facility.getFacilityMovementType())) {
                        facilityMovementDTO.setMovement(facility.getFacilityMovementType());
                    } else {
                        facilityMovementDTO.setMovement("Changed");
                    }

                    facilityMovement.add(facilityMovementDTO);
                }
            }
        }
        rulesRequestDTO.setRiskTakerGroupCode(riskTakerGroupCode);
        rulesRequestDTO.setFacilityMovement(facilityMovement);
        logger.info("getRulesRequestFromCounterpartyFull(END)");
        return rulesRequestDTO;
    }

    private boolean isUserEqualToListManUser(String userId, String email, ListManagementListUser listManagementListUser) {
        if (userId != null) {
            if (userId.equalsIgnoreCase(listManagementListUser.getUserId()) ||
                    (userId + "@rmb.co.za").equalsIgnoreCase(listManagementListUser.getUserId())) {
                return true;
            }
        }
        if (email != null && email.equalsIgnoreCase(listManagementListUser.getEmail())) {
            return true;
        }
        return false;
    }

    @Override
    public Response submitDecision(int workflowId, SubmitDecisionRequestDTO submitDecisionRequestDTO, boolean isSystem, String username) {
        SubmitDecisionResponseDTO responseDTO = new SubmitDecisionResponseDTO();
        logger.info("submitDecision workflowId:{}, isSystem:{}, username:{}",workflowId,isSystem,username);
        logger.info("***submitDecision(decision: {}, comments_conditions: {})", submitDecisionRequestDTO.getDecision(),
                submitDecisionRequestDTO.getConditions());
        try {
            responseDTO.setSuccess(true);
            if (!isSystem) {
                AssignResponse assignResponse = checkIfWorkflowCanBeSubmitted(workflowId, username);
                if (assignResponse.getResponse().equals(AssignedTypes.CAN_BE_SUBMITTED)) {
                    logger.info("submitDecision workflowId:{}, assignResponse:{}",workflowId,assignResponse.getResponse());
                    submitWorkflow(workflowId, username,
                            submitDecisionRequestDTO);
                } else {
                    responseDTO.setSuccess(false);
                    responseDTO.setMessage(assignResponse.getResponse().value());
                }
            } else {
                cleanupCancelRequest(approvalForumDataRepository.getWorkflow(workflowId), submitDecisionRequestDTO.getDecision().value());
            }
        } catch (ResourceException e) {
            e.printStackTrace();
            responseDTO.setSuccess(false);
            responseDTO.setMessage(e.getMessage());
        }
        logger.info("responseDTO workflowId:{}, username:{}, message:{}, isSuccess:{}",workflowId, username, responseDTO.getMessage(), responseDTO.isSuccess());
        return Response.ok(responseDTO).build();
    }

    @Override
    public void updateCapturerLastSeenChatId(int workflowId, int chatId, String userName) {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        if (workflowEntity.getAssignedTo().equalsIgnoreCase(userName) && chatId > workflowEntity.getCaLastSeenChatId()) {
            workflowEntity.setCaLastSeenChatId(chatId);
            approvalForumDataRepository.updateWorkflow(workflowEntity);
        }
    }

    @Override
    public void updateReviewerLastSeenChatId(int workflowId, int chatId, String userName) {
        ApprovalForumWfWorkflowEntity workflowEntity = approvalForumDataRepository.getWorkflow(workflowId);
        ApprovalForumWfadWorkflowApplicationDataEntity applicationDataEntity =
                workflowEntity.getWfcdWorkflowCounterpartyDetailByWfId().iterator().next().getWfadApprovalForumWorkflowApplicationDataByWfadId();
        String userNameShort = userName;
        if (userNameShort.contains("@")) userNameShort = userNameShort.substring(0, userNameShort.indexOf("@"));
        if ((applicationDataEntity.getReviewAnalystId().equalsIgnoreCase(userName) ||
                applicationDataEntity.getReviewAnalystId().equalsIgnoreCase(userNameShort)) && chatId > workflowEntity.getRaLastSeenChatId()) {
            workflowEntity.setRaLastSeenChatId(chatId);
            approvalForumDataRepository.updateWorkflow(workflowEntity);
        }
    }

    @Override
    public void updateApproverLastSeenChatId(int workflowId, int chatId, String userName) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> workflowApprovals =
                approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName);
        workflowApprovals.forEach(entity -> {
            if (chatId > entity.getLastSeenChatId()) {
                entity.setLastSeenChatId(chatId);
                approvalForumDataRepository.updateWorkflowApproval(entity);
            }
        });
    }


    @Override
    public void updateChairpersonLastSeenChatId(int workflowId, int chatId, String userName) {
        List<ApprovalForumWfaWorkflowApprovalsEntity> workflowApprovals =
                approvalForumDataRepository.getApprovalsForWorkflowIdAndUserName(workflowId, userName);
        workflowApprovals.forEach(entity -> {
            if(chatId > entity.getLastSeenChatId()){
                entity.setLastSeenChatId(chatId);
                approvalForumDataRepository.updateWorkflowApproval(entity);
            }
        });
    }

    @Override
    public void updateUserPreference(String userId, boolean chatSortAsc) {
        ApprovalForumUpUserPreferenceEntity entity = approvalForumDataRepository.getUserPreference(userId);
        if (entity == null) {
            entity = new ApprovalForumUpUserPreferenceEntity();
            entity.setUserId(userId);
            entity.setChatSortAscending(chatSortAsc);
            approvalForumDataRepository.persistUserPreference(entity);
        } else {
            entity.setChatSortAscending(chatSortAsc);
            approvalForumDataRepository.updateUserPreference(entity);
        }
    }

    @Override
    public List<String> getAllMeetingNames() {
        logger.info("getAllMeetingNames(START)");
        List<String> meetingNames = new ArrayList<>();
       approvalForumDataRepository.getMeetingQuorumRules().forEach(entity -> {
            String meetingName = entity.getMeetingName();
            if (approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName())) {
                if (!meetingName.contains("Ashburton") &&
                        !meetingName.contains("Zambia") &&
                        !meetingName.contains("Namibia") &&
                        !meetingName.contains("Eswatini") &&
                        !meetingName.contains("Botswana") &&
                        !meetingName.contains("Lesotho")) {

                    logger.info("getAllMeetingNames: {}", meetingName);
                    meetingNames.add(meetingName);
                }
            }
        });
        logger.info("getAllMeetingNames(START)"+meetingNames.toString());
        return meetingNames;
    }

    @Override
    public List<String> getAllMeetingNamesForAshburton() {
        logger.info("getAllMeetingNamesForAshburton(START)");
        List<String> meetingNames = new ArrayList<>();
        approvalForumDataRepository.getMeetingQuorumRulesForAshburton().forEach(entity -> {
            if (approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName())) {
                meetingNames.add(entity.getMeetingName());
            }
        });
        logger.info("getAllMeetingNamesForAshburton(END)");
        return meetingNames;
    }

    private class ProcessExternalRequestResponse {

        private Integer workflowId;
        private String message;
        private boolean success;

        private Integer getWorkflowId() {
            return workflowId;
        }

        private void setWorkflowId(Integer workflowId) {
            this.workflowId = workflowId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

    }

    private class QuorumCheckData {

        ApprovalForumWfqWorkflowQuorumEntity selectedWorkflowQuorumEntity;
        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities;
        Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> sortedListModelMap;
        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities;
        List<ApprovalForumWfaWorkflowApprovalsEntity> quorumAdditionalApprovalsEntities;

        ApprovalForumWfqWorkflowQuorumEntity getSelectedWorkflowQuorumEntity() {
            return selectedWorkflowQuorumEntity;
        }

        void setSelectedWorkflowQuorumEntity(ApprovalForumWfqWorkflowQuorumEntity selectedWorkflowQuorumEntity) {
            this.selectedWorkflowQuorumEntity = selectedWorkflowQuorumEntity;
        }

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> getQuorumDataEntities() {
            return quorumDataEntities;
        }

        void setQuorumDataEntities(List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities) {
            List<ApprovalForumWfqdWorkflowQuorumDataEntity> unManagedQuorumDataEntities = new ArrayList<>();
            for (ApprovalForumWfqdWorkflowQuorumDataEntity quorumDataEntity : quorumDataEntities) {
                ApprovalForumWfqdWorkflowQuorumDataEntity entity = new ApprovalForumWfqdWorkflowQuorumDataEntity();
                entity.setWfqApprovalForumWorkflowQuorumByWfqId(quorumDataEntity.getWfqApprovalForumWorkflowQuorumByWfqId());
                entity.setRank(quorumDataEntity.getRank());
                entity.setMinRequired(quorumDataEntity.getMinRequired());
                entity.setMaxRequired(quorumDataEntity.getMaxRequired());
                entity.setAflmmApprovalForumListManagementMappingByAflmmId(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId());
                unManagedQuorumDataEntities.add(entity);
            }
            this.quorumDataEntities = unManagedQuorumDataEntities;
        }

        Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> getSortedListModelMap() {
            return sortedListModelMap;
        }

        void setSortedListModelMap(Map<Integer, ApprovalForumWfqdWorkflowQuorumDataEntity> sortedListModelMap) {
            this.sortedListModelMap = sortedListModelMap;
        }

        List<ApprovalForumWfaWorkflowApprovalsEntity> getQuorumApprovalsEntities() {
            return quorumApprovalsEntities;
        }

        void setQuorumApprovalsEntities(List<ApprovalForumWfaWorkflowApprovalsEntity> quorumApprovalsEntities) {
            this.quorumApprovalsEntities = quorumApprovalsEntities;
        }

        List<ApprovalForumWfaWorkflowApprovalsEntity> getQuorumAdditionalApprovalsEntities() {
            return quorumAdditionalApprovalsEntities;
        }

        void setQuorumAdditionalApprovalsEntities(List<ApprovalForumWfaWorkflowApprovalsEntity> quorumAdditionalApprovalsEntities) {
            this.quorumAdditionalApprovalsEntities = quorumAdditionalApprovalsEntities;
        }

    }

    @Override
    public boolean isChairpersonApprovalRequired(String meetingName){
        ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity entity =
                approvalForumDataRepository.getMeetingQuorumRuleByMeetingName(meetingName);

        if(entity != null){
            return entity.isChairpersonApprovalRequired();
        }

        return false;

    }

    @Override
    public boolean isASBLimitFundsPersisted(int applicationId){
        logger.info("isASBLimitFundsPersisted(START)");
        ApprovalForumWfflWorkflowFundLimitInformationEntity entity =
                approvalForumDataRepository.getASBLimitFunds(applicationId);
        if(entity != null){
            return true;
        }
        logger.info("isASBLimitFundsPersisted(END)");
        return false;
    }

    @Override
    @Transactional
    public ASBLimitFundsDTO persistASBLimitFundsWorkflow(ApprovalForumWfflWorkflowFundLimitInformationEntity workflowEntity) {
        logger.info("persistASBLimitFundsWorkflow(START)");
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        workflowEntity.setCreatedOn(now);
        approvalForumDataRepository.persistASBLimitFundsWorkflow(workflowEntity);
        logger.info("persistASBLimitFundsWorkflow(END)");
        return mapASBLimitFunds(workflowEntity);
    }

    @Override
    public List<MeetingName> getBroaderAfricaMeetingNames(int tenantId) {
        List<MeetingName> meetingNames = lumosRestService.getBroaderAfricaMeetingNamesForTenantId(tenantId);
        return meetingNames;
    }

    @Override
    public List<String> getInternalBroaderAfricaMeetingNames(int tenantId) {
        logger.info("getInternalBroaderAfricaMeetingNames(START) for "+tenantId);
        List<String> meetingNames = new ArrayList<>();
        approvalForumDataRepository.getMeetingQuorumRulesForBAClients(tenantId).forEach(entity -> {
            if (approvalForumDataRepository.isMandateMappedInRules(entity.getMqrMeetingQuorumRulesByMqrId().getName())) {
                meetingNames.add(entity.getMeetingName());
            }
        });
        logger.info("getInternalBroaderAfricaMeetingNames:"+meetingNames.toString());
        logger.info("getInternalBroaderAfricaMeetingNames(END)");
        return meetingNames;
    }

    @Override
    public ASBLimitFundsDTO getASBLimitFunds(int applicationId) {
        ApprovalForumWfflWorkflowFundLimitInformationEntity entity =
                approvalForumDataRepository.getASBLimitFunds(applicationId);
        if(entity != null){
            return mapASBLimitFunds(entity);
        } else{
            return null;
        }
    }

    private ASBLimitFundsDTO mapASBLimitFunds(ApprovalForumWfflWorkflowFundLimitInformationEntity entity) {
        logger.info("mapASBLimitFunds:"+entity.getWffappId());
        ASBLimitFundsDTO asbLimitFundsDTO = new ASBLimitFundsDTO();
        asbLimitFundsDTO.setWfAppId(entity.getWffappId());
        LimitsRequestedDTO dto = new LimitsRequestedDTO();
        dto.setProposedConvertedEZar(entity.getConvertedEquityLimit());
        dto.setProposedCPYLZar(entity.getCptyLimit());
        dto.setProposedGovermentGZar(entity.getGovernmentGuaranteeLimit());
        dto.setProposedJuniorDZar(entity.getJuniorDebtLimit());
        dto.setProposedLiquidationZar(entity.getLiquidationLimit());
        dto.setProposedMezzanineDZar(entity.getMezzanineDebtLimit());
        dto.setProposedSeniorSZar(entity.getSeniorSecuredLimit());
        dto.setProposedSeniorUZar(entity.getSeniorUnsecuredLimit());
        dto.setProposedSubordinatedSZar(entity.getSubordinatedSecuredLimit());
        dto.setProposedSubordinatedUZar(entity.getSubordinatedUnsecuredLimit());
        dto.setProposedPreferredSZar(entity.getPreferredSharesLimit());
        ASBFundsDTO asbFundsDTO = new ASBFundsDTO();
        asbFundsDTO.setCreditPercentage(entity.getCreditFund());
        asbFundsDTO.setFixedIncomePercentage(entity.getFixedIncomeFund());
        asbFundsDTO.setLdiPercentage(entity.getLdiFund());
        asbFundsDTO.setMultiAssetPercentage(entity.getMultiAssetFund());
        asbFundsDTO.setMultiManagerPercentage(entity.getMultiManagerFund());
        return asbLimitFundsDTO.withLimits(dto).withFunds(asbFundsDTO);
    }

    @Override
    public AssignResponse sendAssignToMeNofitication(int workflowId, String username, CanBeAssignedResponseDTO message) throws ResourceException {
        logger.info("***sendAssignToMeNofitication(message: {}, workflow: {}, username: {})", message, workflowId, username);
        try {
            approvalForumMailService.sendAssignToMeEmailNotification(workflowId, username, message.getResponse().getResponse().name());
        } catch (UnsupportedEncodingException e) {
            throw new ResourceException("sendAssignToMeNofitication failed: " + e.getMessage());
        }

        return message.getResponse();
    }

    @Override
    public AssignResponse sendAssignToMeNofiticationFailure(int workflowId, String username,
                                                            CanBeAssignedResponseDTO message, String workflowIdStr) {
        logger.info("***sendAssignToMeNofiticationFailure(message: {}, workflow: {}, username: {}, workflowIdStr: {})",
                message, workflowId, username, workflowIdStr);
        approvalForumMailService.sendAssignToMeEmailNotificationFailure(workflowId, username,
                message.getResponse().getResponse().name(), workflowIdStr);

        return message.getResponse();
    }

    @Override
    public AssignResponse checkIfCanAssignWorkflowToMe(int workflowId, String username) throws ResourceException {
        CanBeAssignedResponseDTO response = new CanBeAssignedResponseDTO();
        response.setResponse(checkIfWorkflowCanBeAssigned(workflowId, username));
        //FIXME: revert
        logger.info("***checkIfCanAssignWorkflowToMe - response: ({})", response.getResponse());
        return response.getResponse();
    }
}
