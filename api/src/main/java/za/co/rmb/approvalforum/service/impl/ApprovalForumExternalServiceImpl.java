package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.entity.ApprovalForumWfflWorkflowFundLimitInformationEntity;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.approvalforum.service.ApprovalForumExternalService;
import za.co.rmb.approvalforum.service.ApprovalForumService;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.approvalforum.service.util.ApprovalForumServiceUtil;
import za.co.rmb.credit.application.client.ApplicationService;
import za.co.rmb.credit.application.client.http.ApplicationServiceImpl;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.facility.client.FacilityService;
import za.co.rmb.credit.facility.client.http.FacilityServiceImpl;
import za.co.rmb.credit.lookupservice.client.CounterpartyService;
import za.co.rmb.credit.lookupservice.client.PESearch;
import za.co.rmb.credit.lookupservice.client.http.CounterpartyServiceImpl;
import za.co.rmb.credit.lookupservice.client.http.HttpPESearch;
import za.co.rmb.credit.referencedata.client.RefDataService;
import za.co.rmb.credit.referencedata.client.RefDataServiceImpl;
import za.co.rmb.credit.referencedata.model.CreditDecisionType;
import za.co.rmb.credit.referencedata.model.Currency;
import za.co.rmb.credit.referencedata.model.WholesaleCreditPortfolio;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

public class ApprovalForumExternalServiceImpl extends ApprovalForumServiceUtil implements ApprovalForumExternalService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumExternalServiceImpl.class);

    @Inject
    private PESearch counterpartySearch;

    @Inject
    private CounterpartyService counterpartyService;

    @Inject
    private FacilityService facilityService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApprovalForumAuthService approvalForumAuthService;

    @Inject
    private ApprovalForumService approvalForumService;

    @Inject
    private RefDataService refDataService;

    @Inject
    private ApprovalForumExternalService approvalForumExternalService;

    private Map<String, String> afSettings;

    @Inject
    public ApprovalForumExternalServiceImpl(@Named("af_settings") Map<String, String> afSettings){
        this.afSettings = afSettings;
    }

    private static final String APPROVED_STATUS = "Approved";
    private static final String SUBMITTED_FOR_APPROVAL_STATUS = "Submitted for Approval";

    private static final List<String> limits = new ArrayList<String>(Arrays.asList("Subordinated Unsecured","Subordinated Secured",
            "Senior Unsecured","Senior Secured","Preference Shares","Mezzanine Debt","Liquidation","Junior Debt","Government Guarantee","Converted Equity"));

    /*
    'LDI','Fixed Income','Credit', 'Multi Asset',  'Multi Managers'
     */
    private static final List<String> funds = new ArrayList<String>(Arrays.asList("Liability Driven Investments (LDI)","Fixed Income Funds","Credit Funds", "Multi-Asset",  "Multi-Manager"));

    @Override
    public List<CounterpartyModel> getCounterparties(boolean excludeActive, int tenantId) throws ExternalResourceException {
        return getCounterparties(excludeActive, tenantId,
                Arrays.asList(afSettings.get("af.application.statuses").split(";")));
    }

    @Override
    public List<CounterpartyModel> getCounterparties(boolean excludeActive, int tenantId, List<String> applicationStatuses) throws ExternalResourceException {
        Map<String, CounterpartyModel> counterpartyModelMap = new TreeMap<>();
        List<String> activeInkombaIds = approvalForumService.getInkombaIdsForActiveWorkflows();
        List<String> meetingNames = null;
        List<Map<String, Object>> applications;
        try{
            if(tenantId>2){
                CounterpartyModel model = new CounterpartyModel();
                model.setCreditInkombaId("This request for credit_inkomba_id not applicable for Broader Africa tenants");
                model.setCounterpartyName("This request for counterparty not applicable for Broader Africa tenants") ;
                counterpartyModelMap.putIfAbsent(("Broader Africa Tenants:") , model);
            } else{
                meetingNames = approvalForumService.getAllMeetingNames();
                applications =
                        applicationService.getCounterpartyApplication(applicationStatuses,
                                meetingNames);
                if(applications != null && !applications.isEmpty()) {
                    applications.forEach(stringObjectMap -> {
                        if(!"Admin Changes".equalsIgnoreCase((String) stringObjectMap.get("proposal_type"))){
                            if(!excludeActive || activeInkombaIds.indexOf((String) stringObjectMap.get("credit_inkomba_id")) < 0) {
                                CounterpartyModel model = new CounterpartyModel();
                                model.setCreditInkombaId((String) stringObjectMap.get("credit_inkomba_id"));
                                model.setCounterpartyName((String) stringObjectMap.get("name"));
                                counterpartyModelMap.putIfAbsent((String) stringObjectMap.get("name"), model);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return new ArrayList<>(counterpartyModelMap.values());
    }

    @Override
    public List<CounterpartyModel> getCounterparties(String search, boolean isExact) throws ExternalResourceException {
        logger.info("getCounterparties()");
        List<CounterpartyModel> counterparties = new ArrayList<>();
        if(search != null && !"".equals(search.trim())) {
            try {
                List<Map<String, String>> list = counterpartySearch.search(search).exact(isExact).execute();
                if(list != null && !list.isEmpty()){
                    list.forEach(stringStringMap -> {
                        CounterpartyModel countertparty = new CounterpartyModel();
                        countertparty.setCounterpartyName(stringStringMap.get("name"));
                        countertparty.setCreditInkombaId(stringStringMap.get("credit_inkomba_id"));
                        counterparties.add(countertparty);
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        return counterparties;
    }

    @Override
    public CounterpartyFullDetail getCounterpartyFullDetail(String inkombaId, String facilityStatus) throws ExternalResourceException{
        CounterpartyFullDetail fullDetail = new CounterpartyFullDetail()
                .withCounterpartyDetail(getCounterpartyDetail(inkombaId))
                .withCreditApplication(getCreditApplication(inkombaId))
                .withFacility(getFacility(inkombaId, facilityStatus))
                .withCounterpartySectorIndustry(getCounterpartySectorIndustry(inkombaId));
        fullDetail.setFacilitySummary(getFacilitySummary(fullDetail.getFacility()));
        return fullDetail;
    }

    @Override
    public CounterpartyFullDetail getAshburtonCounterpartyFullDetail(String inkombaId, int appId, String facilityStatus) throws ExternalResourceException {
        int tempId=0;
        CounterpartyFullDetail fullDetail = new CounterpartyFullDetail()
                .withCounterpartyDetail(getAshburtonCounterpartyDetail(inkombaId))
                .withCreditApplication(getAshburtonCreditApplication(inkombaId))
                .withFacility(getAshburtonFacility(inkombaId, facilityStatus))
                .withCounterpartySectorIndustry(getAshburtonCounterpartySectorIndustry(inkombaId));
        fullDetail.setFacilitySummary(getAshburtonFacilitySummary(fullDetail.getFacility()));
        if(appId == 0){
            tempId = fullDetail.getCreditApplication().getApplicationId();
            logger.info("Temp Id: " + tempId);
            fullDetail.setAsbDetails(getASBDetails(String.valueOf(tempId)));
        } else {
            logger.info("App id: " + appId);
            fullDetail.setAsbDetails(getASBDetails(String.valueOf(appId)));
        }
        logger.info("getAshCounterpartyFullDetail(END)");
        return fullDetail;
    }

    private ASBLimitFundsDTO getASBDetails(String applicationId) throws ExternalResourceException {
        logger.info("getASBDetails(START)");
        ASBLimitFundsDTO asbLimitFundsDTO = new ASBLimitFundsDTO();
        if(applicationId != null && !"".equals(applicationId.trim())) {

            try {
                // check if details available in daf database, otherwise pull it from counterparty service and update
                if(approvalForumService.isASBLimitFundsPersisted(Integer.valueOf(applicationId))){
                    logger.info("isASBLimitFundsPersisted");
                    asbLimitFundsDTO = (ASBLimitFundsDTO)approvalForumService.getASBLimitFunds( Integer. valueOf(applicationId));
                    if(asbLimitFundsDTO!= null){
                        return asbLimitFundsDTO;
                    }
                } else {
                    logger.info("isASBLimitFundsPersisted--else");
                    ApprovalForumWfflWorkflowFundLimitInformationEntity workflowEntity = new ApprovalForumWfflWorkflowFundLimitInformationEntity();
                    ASBFundsDTO asbFundsDTO = approvalForumExternalService.getAsbFunds(applicationId);
                    LimitsRequestedDTO limitsRequestedDTO  = approvalForumExternalService.getLimitRequest(applicationId);
                    if(asbFundsDTO == null){
                        asbFundsDTO= new ASBFundsDTO().withCreditPercentage(Double.valueOf("o"))
                                .withLdiPercentage(Double.valueOf("o"))
                                .withFixedIncomePercentage(Double.valueOf("o"))
                                .withMultiAssetPercentage(Double.valueOf("o"))
                                .withMultiManagerPercentage(Double.valueOf("o"));
                    } else {
                        workflowEntity.setWffappId(Integer.valueOf(applicationId));
                        workflowEntity.setFixedIncomeFund(asbFundsDTO.getFixedIncomePercentage());
                        workflowEntity.setMultiManagerFund(asbFundsDTO.getMultiManagerPercentage());
                        workflowEntity.setMultiAssetFund(asbFundsDTO.getMultiAssetPercentage());
                        workflowEntity.setLdiFund(asbFundsDTO.getLdiPercentage());
                        workflowEntity.setCreditFund(asbFundsDTO.getCreditPercentage());
                        asbLimitFundsDTO.setFunds(asbFundsDTO);
                    }

                    if(null == limitsRequestedDTO){
                        limitsRequestedDTO = new LimitsRequestedDTO().withProposedConvertedEZar(Double.valueOf("o")).withProposedCPYLZar(Double.valueOf("o")).withProposedGovermentGZar(Double.valueOf("o"))
                                .withProposedJuniorDZar(Double.valueOf("o")).withProposedMezzanineDZar(Double.valueOf("o")).withProposedPreferredSZar(Double.valueOf("o")).withProposedSeniorSZar(Double.valueOf("o"))
                                .withProposedLiquidationZar(Double.valueOf("o")).withProposedSeniorUZar(Double.valueOf("o")).withProposedSubordinatedUZar(Double.valueOf("o"))
                                .withProposedSubordinatedSZar(Double.valueOf("o"));
                    } else {
                        workflowEntity.setSubordinatedUnsecuredLimit(limitsRequestedDTO.getProposedSubordinatedUZar());
                        workflowEntity.setSubordinatedSecuredLimit(limitsRequestedDTO.getProposedSubordinatedSZar());
                        workflowEntity.setSeniorSecuredLimit(limitsRequestedDTO.getProposedSeniorSZar());
                        workflowEntity.setSeniorUnsecuredLimit(limitsRequestedDTO.getProposedSeniorUZar());
                        workflowEntity.setPreferredSharesLimit(limitsRequestedDTO.getProposedPreferredSZar());
                        workflowEntity.setLiquidationLimit(limitsRequestedDTO.getProposedLiquidationZar());
                        workflowEntity.setMezzanineDebtLimit(limitsRequestedDTO.getProposedMezzanineDZar());
                        workflowEntity.setGovernmentGuaranteeLimit(limitsRequestedDTO.getProposedGovermentGZar());
                        workflowEntity.setJuniorDebtLimit(limitsRequestedDTO.getProposedJuniorDZar());
                        workflowEntity.setConvertedEquityLimit(limitsRequestedDTO.getProposedConvertedEZar());
                        workflowEntity.setCptyLimit(limitsRequestedDTO.getProposedCPYLZar());
                        asbLimitFundsDTO.setLimits(limitsRequestedDTO);
                    }
                    workflowEntity.setWffappId(Integer.valueOf(applicationId));

                    ASBLimitFundsDTO asbLimitFunds = new ASBLimitFundsDTO();

                    asbLimitFunds = approvalForumService.persistASBLimitFundsWorkflow(workflowEntity);

                    return asbLimitFunds;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }

        }
        logger.info("getASBDetails(END)");
        return asbLimitFundsDTO;
    }

    @Override
    public CounterpartyModel getCounterpartyDetail(String inkombaId) throws ExternalResourceException {
        logger.info("getCounterpartyDetail({})", inkombaId);
        CounterpartyModel model = new CounterpartyModel();
        if(inkombaId != null && !"".equals(inkombaId.trim())) {
            try {
                Map<String, Object> response = counterpartyService.getCounterpartyData(inkombaId);
                model.setCounterpartyName(getStringFromObject(response.get("counterparty_name")));
                model.setCreditInkombaId(getStringFromObject(response.get("credit_inkomba_id")));
                model.setCreditAnalyst(getStringFromObject(response.get("primary_analyst")));
                model.setCreditAnalystId(getStringFromObject(response.get("primary_analyst_id")));
                model.setCreditAnalystMail(
                        getEmailFromAd(getStringFromObject(response.get("primary_analyst_id")),
                                getStringFromObject(response.get("primary_analyst_email"))));
                model.setBank(getBooleanFromObject(response.get("bank")));
                model.setFinancialInstitutionType(getStringFromObject(response.get("financial_institution_type")));
                String party_source_type = getStringFromObject(response.get("party_source_type"));
                model.setProspect("".equals(party_source_type.trim()) || "Prospect".equals(party_source_type));
                model.setBank(getBooleanFromObject(response.get("bank")));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        return model;
    }

    @Override
    public CounterpartyModel getAshburtonCounterpartyDetail(String inkombaId) throws ExternalResourceException {
        logger.info("getAshburtonCounterpartyDetail({})", inkombaId);
        CounterpartyModel model = new CounterpartyModel();
        if(inkombaId != null && !"".equals(inkombaId.trim())) {
            try {
                Map<String, Object> response = getCounterpartyService().getCounterpartyData(inkombaId);
                model.setCounterpartyName(getStringFromObject(response.get("counterparty_name")));
                model.setCreditInkombaId(getStringFromObject(response.get("credit_inkomba_id")));
                model.setCreditAnalyst(getStringFromObject(response.get("primary_analyst")));
                model.setCreditAnalystId(getStringFromObject(response.get("primary_analyst_id")));
                model.setCreditAnalystMail(
                        getEmailFromAd(getStringFromObject(response.get("primary_analyst_id")),
                                getStringFromObject(response.get("primary_analyst_email"))));
                model.setBank(getBooleanFromObject(response.get("bank")));
                model.setFinancialInstitutionType(getStringFromObject(response.get("financial_institution_type")));
                String party_source_type = getStringFromObject(response.get("party_source_type"));
                model.setProspect("".equals(party_source_type.trim()) || "Prospect".equals(party_source_type));
                model.setBank(getBooleanFromObject(response.get("bank")));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        logger.info("getAshburtonCounterpartyDetail(END)");
        return model;
    }

    @Override
    public ASBLimitFundsDTO getAshburtonLimitFunds(String applicationId) throws ExternalResourceException {
        return getASBDetails(applicationId);
    }

    @Override
    public List<MeetingName> getBroaderAfricaMeetingNames(int tenantId) throws ExternalResourceException {
        logger.info("getBroaderAfricaMeetingNames()");
        List<MeetingName> models = new ArrayList<>();
        try{
            List<za.co.rmb.credit.referencedata.model.MeetingName> meetingNames = getRefDataService().getMeetingName();
            if(meetingNames != null && !meetingNames.isEmpty()){
                meetingNames.forEach(meetingName -> {
                    MeetingName model = new MeetingName();
                    model.setId(new Long(meetingName.getId()).intValue());
                    model.setName(meetingName.getName());
                    model.setCode(meetingName.getCode());
                    model.setDisabled(meetingName.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        logger.info("getAsbMeetingNames()"+models.size());
        for(MeetingName meetingName: models){
            logger.info("meeting code: "+meetingName.getCode());
        }
        return models;
    }

    private String getEmailFromAd(String userId, String email){
        if(userId == null) return email;
        try{
            ActiveDirectoryUserModel userModel = approvalForumAuthService.getUserDetailFromAd(userId);
            if(userModel == null) return email;

            String mail = userModel.getMail();
            if(mail == null || "".equals(mail.trim())){
                return email;
            }
            return mail;
        }catch(ResourceException e){
            return email;
        }
    }

    @Override
    public CreditApplicationModel getCreditApplication(String inkombaId) throws ExternalResourceException {
        logger.info("getCreditApplication({})", inkombaId);
        CreditApplicationModel model = new CreditApplicationModel();
        List<String> statuses = new ArrayList<>();
        statuses.add(APPROVED_STATUS);
        statuses.add(SUBMITTED_FOR_APPROVAL_STATUS);
        try{
            List<Map<String, Object>> applications = applicationService.getApplicationRatingAndDCP(inkombaId, statuses);
            if(applications != null && !applications.isEmpty()){
                applications.forEach(stringObjectMap -> {
                    if(APPROVED_STATUS.equals(stringObjectMap.get("credit_application_status"))){
                        model.setLastApprovedFrRating((String)stringObjectMap.get("counterparty_rating"));
                        model.setLastApprovedDcp(getDoubleValueFromMap("proposed_total_dcp", stringObjectMap));
                        model.setLastApprovedSettlementDcp(getDoubleValueFromMap("proposed_sett_dcp", stringObjectMap));
                    }else{
                        model.setProposedFrRating((String)stringObjectMap.get("counterparty_rating"));
                        model.setProposedDcp(getDoubleValueFromMap("proposed_total_dcp", stringObjectMap));
                        model.setProposedSettlementDcp(getDoubleValueFromMap("proposed_sett_dcp", stringObjectMap));
                        model.setCreditInkombaId((String)stringObjectMap.get("credit_inkomba_id"));
                        model.setDistressStatus((String)stringObjectMap.get("distress_status"));
                        model.setWatchlistIndicator((Boolean)stringObjectMap.get("watchlist_ind"));
                        model.setApplicationId((Integer)stringObjectMap.get("application_id"));
                        Map<String, Object> applicationInfoObjectMap = applicationService.getApplicationInfo((Integer)stringObjectMap.get("application_id"));
                        model.setProposalAction((String)applicationInfoObjectMap.get("proposal_action"));
                        model.setProposalType((String)applicationInfoObjectMap.get("proposal_type"));
                        model.setWholesaleCreditPortfolio((String)applicationInfoObjectMap.get("wholesale_credit_portfolio"));
                        model.setMeetingName((String)applicationInfoObjectMap.get("meeting_name"));
                        model.setReviewAnalyst((String)applicationInfoObjectMap.get("review_analyst"));
                        model.setReviewAnalystId((String)applicationInfoObjectMap.get("review_analyst_id"));
                        model.setReviewAnalystEmail(
                                getEmailFromAd((String)applicationInfoObjectMap.get("review_analyst_id"),
                                        (String)applicationInfoObjectMap.get("review_analyst_email")));
                        model.setReviewAnalystSignatureDate(
                                getLocalDatetimeFromString((String)applicationInfoObjectMap.get("review_analyst_signature_date"), "yyyy-MM-dd HH:mm:ss.SSS"));
                        model.setCreditAnalyst((String)applicationInfoObjectMap.get("credit_analyst"));
                        model.setCreditAnalystId((String)applicationInfoObjectMap.get("credit_analyst_id"));
                        model.setCreditAnalystEmail(
                                getEmailFromAd((String)applicationInfoObjectMap.get("credit_analyst_id"),
                                        (String)applicationInfoObjectMap.get("credit_analyst_email")));
                        model.setCreditAnalystSignatureDate(
                                getLocalDatetimeFromString((String)applicationInfoObjectMap.get("credit_analyst_signature_date"), "yyyy-MM-dd HH:mm:ss.SSS"));

                    }
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return model;
    }

    @Override
    public CreditApplicationModel getAshburtonCreditApplication(String inkombaId) throws ExternalResourceException {
        logger.info("getAshburtonCreditApplication({})", inkombaId);
        CreditApplicationModel model = new CreditApplicationModel();
        List<String> statuses = new ArrayList<>();
        statuses.add(APPROVED_STATUS);
        statuses.add(SUBMITTED_FOR_APPROVAL_STATUS);
        try{
            List<Map<String, Object>> applications = getApplicationService().getApplicationRatingAndDCP(inkombaId, statuses);
            if(applications != null && !applications.isEmpty()){
                applications.forEach(stringObjectMap -> {
                    if(APPROVED_STATUS.equals(stringObjectMap.get("credit_application_status"))){
                        model.setLastApprovedFrRating((String)stringObjectMap.get("counterparty_rating"));
                        model.setLastApprovedDcp(getDoubleValueFromMap("proposed_total_dcp", stringObjectMap));
                        model.setLastApprovedSettlementDcp(getDoubleValueFromMap("proposed_sett_dcp", stringObjectMap));
                    }else{
                        model.setProposedFrRating((String)stringObjectMap.get("counterparty_rating"));
                        model.setProposedDcp(getDoubleValueFromMap("proposed_total_dcp", stringObjectMap));
                        model.setProposedSettlementDcp(getDoubleValueFromMap("proposed_sett_dcp", stringObjectMap));
                        model.setCreditInkombaId((String)stringObjectMap.get("credit_inkomba_id"));
                        model.setDistressStatus((String)stringObjectMap.get("distress_status"));
                        model.setWatchlistIndicator((Boolean)stringObjectMap.get("watchlist_ind"));
                        model.setApplicationId((Integer)stringObjectMap.get("application_id"));
                        Map<String, Object> applicationInfoObjectMap = getApplicationService().getApplicationInfo((Integer)stringObjectMap.get("application_id"));
                        model.setProposalAction((String)applicationInfoObjectMap.get("proposal_action"));
                        model.setProposalType((String)applicationInfoObjectMap.get("proposal_type"));
                        model.setWholesaleCreditPortfolio((String)applicationInfoObjectMap.get("wholesale_credit_portfolio"));
                        model.setMeetingName((String)applicationInfoObjectMap.get("meeting_name"));
                        model.setReviewAnalyst((String)applicationInfoObjectMap.get("review_analyst"));
                        model.setReviewAnalystId((String)applicationInfoObjectMap.get("review_analyst_id"));
                        model.setReviewAnalystEmail(
                                getEmailFromAd((String)applicationInfoObjectMap.get("review_analyst_id"),
                                        (String)applicationInfoObjectMap.get("review_analyst_email")));
                        model.setReviewAnalystSignatureDate(
                                getLocalDatetimeFromString((String)applicationInfoObjectMap.get("review_analyst_signature_date"), "yyyy-MM-dd HH:mm:ss.SSS"));
                        model.setCreditAnalyst((String)applicationInfoObjectMap.get("credit_analyst"));
                        model.setCreditAnalystId((String)applicationInfoObjectMap.get("credit_analyst_id"));
                        model.setCreditAnalystEmail(
                                getEmailFromAd((String)applicationInfoObjectMap.get("credit_analyst_id"),
                                        (String)applicationInfoObjectMap.get("credit_analyst_email")));
                        model.setCreditAnalystSignatureDate(
                                getLocalDatetimeFromString((String)applicationInfoObjectMap.get("credit_analyst_signature_date"), "yyyy-MM-dd HH:mm:ss.SSS"));

                    }
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        logger.info("getAshburtonCreditApplication(END)");
        return model;
    }

    @Override
    public CreditApplicationByIdModel getCreditApplicationUsingIdAllStatuses(int applicationId) throws ExternalResourceException {
        logger.info("getCreditApplicationUsingId({})", applicationId);
        return mapCreditApplicationByIdModel(applicationService.getApplicationInfoAllStatus(applicationId), applicationId);
    }

    //getAshburtonCreditApplicationUsingIdAllStatuses
    @Override
    public CreditApplicationByIdModel getAshburtonCreditApplicationUsingIdAllStatuses(int applicationId) throws ExternalResourceException {
        logger.info("getAshburtonCreditApplicationUsingIdAllStatuses({})", applicationId);
        Map<String, Object> applicationInfoObjectMap;
        applicationInfoObjectMap = getApplicationService().getApplicationInfoAllStatus(applicationId);
        logger.info("end of getAshburtonCreditApplicationUsingIdAllStatuses({})");
        return mapCreditApplicationByIdModel(applicationInfoObjectMap, applicationId);
    }

    private CreditApplicationByIdModel mapCreditApplicationByIdModel(Map<String, Object> applicationInfoObjectMap, int applicationId)
            throws ExternalResourceException{
        CreditApplicationByIdModel model = null;
        try{
            if(applicationInfoObjectMap != null && !applicationInfoObjectMap.isEmpty()) {
                logger.info("mapCreditApplicationByIdModel---"+applicationId);
                model = new CreditApplicationByIdModel();
                model.setCreditInkombaId((String)applicationInfoObjectMap.get("credit_inkomba_id"));
                model.setCreditApplicationStatus((String)applicationInfoObjectMap.get("credit_application_status"));
                model.setReviewAnalyst((String)applicationInfoObjectMap.get("review_analyst"));
                model.setReviewAnalystId((String)applicationInfoObjectMap.get("review_analyst_id"));
                model.setReviewAnalystEmail((String)applicationInfoObjectMap.get("review_analyst_email"));
                model.setWholesaleCreditPortfolio((String)applicationInfoObjectMap.get("wholesale_credit_portfolio"));
                model.setMeetingName((String)applicationInfoObjectMap.get("meeting_name"));
                model.setProposalType((String)applicationInfoObjectMap.get("proposal_type"));
                model.setProposalAction((String)applicationInfoObjectMap.get("proposal_action"));
                model.setApplicationType((String)applicationInfoObjectMap.get("application_type"));
                model.setApplicationId(applicationId);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return model;

    }

    private double getDoubleValueFromMap(String fieldName, Map<String, Object> stringObjectMap){
        return new BigDecimal(((Double)stringObjectMap.get(fieldName))).setScale(0, BigDecimal.ROUND_HALF_DOWN).doubleValue();
    }

    @Override
    public List<FacilityModel> getFacility(String inkombaId, String statuses) throws ExternalResourceException {
        logger.info("getFacilities()");
        List<FacilityModel> facilityModels = new ArrayList<>();
        Map<String, Currency> currencyMap = getCurrenciesAsMap();
        BigDecimal zarSpotRate = new BigDecimal(BigInteger.ONE).divide(currencyMap.get("ZAR").getSpotRate(), 20, RoundingMode.HALF_UP);
        if(inkombaId != null && !"".equals(inkombaId.trim())) {
            try {
                List<Map<String, Object>> response = facilityService.getFacilityData(inkombaId, Arrays.asList(statuses.split(";")));
                if(response != null && !response.isEmpty()){
                    response.forEach(stringObjectMap -> {
                        FacilityModel facilityModel = new FacilityModel();
                        facilityModel.setFacilityReference(getStringFromObject(stringObjectMap.get("facility_reference")));
                        facilityModel.setRiskType(getStringFromObject(stringObjectMap.get("risk_type")));
                        facilityModel.setTenor(getStringFromObject(stringObjectMap.get("tenor")));
                        facilityModel.setCurrency(getStringFromObject(stringObjectMap.get("currency")));
                        facilityModel.setNetLimit((Double)stringObjectMap.get("proposed_amount"));
                        facilityModel.setFacilityDescription(getStringFromObject(stringObjectMap.get("facility_description")));
                        facilityModel.setCreditApplicationStatus(getStringFromObject(stringObjectMap.get("credit_application_status")));
                        facilityModel.setRiskTaker(getStringFromObject(stringObjectMap.get("risk_taker")));
                        facilityModel.setDealSpecific((Boolean)stringObjectMap.get("deal_specific"));
                        facilityModel.setProposedRating(getStringFromObject(stringObjectMap.get("proposed_rating")));
                        facilityModel.setProposedLgd(Double.valueOf(getStringFromObject(stringObjectMap.get("proposed_lgd"))));
                        facilityModel.setFacilityId(getStringFromObject(stringObjectMap.get("facility_id")));
                        facilityModel.setFacilityRating(getStringFromObject(stringObjectMap.get("facility_rating")));
                        facilityModel.setFacilityInvestmentType(getStringFromObject(stringObjectMap.get("facility_investment_type")));
                        facilityModel.setRiskTakerGroupCode(getStringFromObject(stringObjectMap.get("risk_taker_grouping_code")));
                        facilityModel.setFacilityMovementType(getStringFromObject(stringObjectMap.get("facility_movement_type")));

                        if(!"ZAR".equals(facilityModel.getCurrency())){
                            if(!currencyMap.containsKey(facilityModel.getCurrency()) ||
                                    currencyMap.get(facilityModel.getCurrency()).getSpotRate().doubleValue() == -1){
                                facilityModel.setNetLimitZar(facilityModel.getNetLimit());
                                facilityModel.setProposedLgdZar(facilityModel.getProposedLgd());
                            }else {
                                BigDecimal exchangeRate = new BigDecimal(BigInteger.ONE).divide(
                                        currencyMap.get(facilityModel.getCurrency()).getSpotRate(), 20, RoundingMode.HALF_UP);
                                BigDecimal netLimitValueZar = BigDecimal.valueOf(facilityModel.getNetLimit()).divide(exchangeRate, 20, RoundingMode.HALF_UP).multiply(zarSpotRate);
                                BigDecimal proposedLgdValueZar = BigDecimal.valueOf(facilityModel.getProposedLgd()).divide(exchangeRate, 20, RoundingMode.HALF_UP).multiply(zarSpotRate);
                                facilityModel.setNetLimitZar(netLimitValueZar.setScale(0, RoundingMode.HALF_UP).doubleValue());
                                facilityModel.setProposedLgdZar(proposedLgdValueZar.setScale(0, RoundingMode.HALF_UP).doubleValue());
                            }
                        }else{
                            facilityModel.setNetLimitZar(facilityModel.getNetLimit());
                            facilityModel.setProposedLgdZar(facilityModel.getProposedLgd());
                        }

                        facilityModels.add(facilityModel);
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        return facilityModels;
    }

    @Override
    public List<CounterpartySectorIndustryModel> getCounterpartySectorIndustry(String inkombaId) throws ExternalResourceException {
        logger.info("getCounterpartySectorIndustry({})", inkombaId);
        List<CounterpartySectorIndustryModel> models = new ArrayList<>();
        try{
            List<Map<String, Object>> counterpartySectorIndustries = counterpartyService.getCounterpartySectorIndustry(inkombaId);
            if(counterpartySectorIndustries != null && !counterpartySectorIndustries.isEmpty()){
                counterpartySectorIndustries.forEach(stringObjectMap -> {
                    CounterpartySectorIndustryModel model = new CounterpartySectorIndustryModel();
                    model.setCounterpartyDetail(new CounterpartyModel()
                            .withCounterpartyName((String)stringObjectMap.get("counterparty_name"))
                            .withCreditInkombaId((String)stringObjectMap.get("credit_inkomba_id")));
                    model.setIndustry((String)stringObjectMap.get("industry_name"));
                    model.setSector((String)stringObjectMap.get("sector_name"));
                    model.setPercentage((Double)stringObjectMap.get("percentage"));
                    model.setStatus((String)stringObjectMap.get("counterparty_status"));
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return models;
    }

    @Override
    public List<CounterpartySectorIndustryModel> getAshburtonCounterpartySectorIndustry(String inkombaId) throws ExternalResourceException {
        logger.info("getAshburtonCounterpartySectorIndustry({})", inkombaId);
        List<CounterpartySectorIndustryModel> models = new ArrayList<>();
        try{
            List<Map<String, Object>> counterpartySectorIndustries = getCounterpartyService().getCounterpartySectorIndustry(inkombaId);
            if(counterpartySectorIndustries != null && !counterpartySectorIndustries.isEmpty()){
                counterpartySectorIndustries.forEach(stringObjectMap -> {
                    CounterpartySectorIndustryModel model = new CounterpartySectorIndustryModel();
                    model.setCounterpartyDetail(new CounterpartyModel()
                            .withCounterpartyName((String)stringObjectMap.get("counterparty_name"))
                            .withCreditInkombaId((String)stringObjectMap.get("credit_inkomba_id")));
                    model.setIndustry((String)stringObjectMap.get("industry_name"));
                    model.setSector((String)stringObjectMap.get("sector_name"));
                    model.setPercentage((Double)stringObjectMap.get("percentage"));
                    model.setStatus((String)stringObjectMap.get("counterparty_status"));
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        logger.info("getAshburtonCounterpartySectorIndustry(END)");
        return models;
    }

    @Override
    public List<CreditDecision> getCreditDecisions() throws ExternalResourceException{
        logger.info("getCreditDecisions()");
        List<CreditDecision> models = new ArrayList<>();
        try{
            List<CreditDecisionType> creditDecisionTypes = refDataService.getCreditDecisionType();
            if(creditDecisionTypes != null && !creditDecisionTypes.isEmpty()){
                creditDecisionTypes.forEach(creditDecisionType -> {
                    CreditDecision model = new CreditDecision();
                    model.setId(new Long(creditDecisionType.getId()).intValue());
                    model.setName(creditDecisionType.getName());
                    model.setCode(creditDecisionType.getCode());
                    model.setDisabled(creditDecisionType.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return models;
    }

    @Override
    public List<CreditDecision> getAsbCreditDecisions() throws ExternalResourceException{
        logger.info("getAsbCreditDecisions(START)");
        List<CreditDecision> models = new ArrayList<>();
        try{
            List<CreditDecisionType> creditDecisionTypes = getRefDataService().getCreditDecisionType();
            if(creditDecisionTypes != null && !creditDecisionTypes.isEmpty()){
                creditDecisionTypes.forEach(creditDecisionType -> {
                    CreditDecision model = new CreditDecision();
                    model.setId(new Long(creditDecisionType.getId()).intValue());
                    model.setName(creditDecisionType.getName());
                    model.setCode(creditDecisionType.getCode());
                    model.setDisabled(creditDecisionType.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        logger.info("getAsbCreditDecisions(END)");
        return models;
    }

    @Override
    public List<WholesaleCreditPortfolioData> getWholesaleCreditPortfolios() throws ExternalResourceException{
        logger.info("getWholesaleCreditPortfolios()");
        List<WholesaleCreditPortfolioData> models = new ArrayList<>();
        try{
            List<WholesaleCreditPortfolio> wholesaleCreditPortfolios = refDataService.getWholesaleCreditPortfolio();
            if(wholesaleCreditPortfolios != null && !wholesaleCreditPortfolios.isEmpty()){
                wholesaleCreditPortfolios.forEach(wholesaleCreditPortfolio -> {
                    WholesaleCreditPortfolioData model = new WholesaleCreditPortfolioData();
                    model.setId(new Long(wholesaleCreditPortfolio.getId()).intValue());
                    model.setName(wholesaleCreditPortfolio.getName());
                    model.setCode(wholesaleCreditPortfolio.getCode());
                    model.setDisabled(wholesaleCreditPortfolio.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return models;
    }

    @Override
    public List<WholesaleCreditPortfolioData> getAsbWholesaleCreditPortfolios() throws ExternalResourceException{
        logger.info("getAsbWholesaleCreditPortfolios()");
        List<WholesaleCreditPortfolioData> models = new ArrayList<>();
        try{
            List<WholesaleCreditPortfolio> wholesaleCreditPortfolios = getRefDataService().getWholesaleCreditPortfolio();
            if(wholesaleCreditPortfolios != null && !wholesaleCreditPortfolios.isEmpty()){
                wholesaleCreditPortfolios.forEach(wholesaleCreditPortfolio -> {
                    WholesaleCreditPortfolioData model = new WholesaleCreditPortfolioData();
                    model.setId(new Long(wholesaleCreditPortfolio.getId()).intValue());
                    model.setName(wholesaleCreditPortfolio.getName());
                    model.setCode(wholesaleCreditPortfolio.getCode());
                    model.setDisabled(wholesaleCreditPortfolio.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        logger.info("getAsbWholesaleCreditPortfolios()");
        return models;
    }
    @Override
    public List<MeetingName> getMeetingNames() throws ExternalResourceException{
        logger.info("getMeetingNames()");
        List<MeetingName> models = new ArrayList<>();
        try{
            List<za.co.rmb.credit.referencedata.model.MeetingName> meetingNames = refDataService.getMeetingName();
            if(meetingNames != null && !meetingNames.isEmpty()){
                meetingNames.forEach(meetingName -> {
                    MeetingName model = new MeetingName();
                    model.setId(new Long(meetingName.getId()).intValue());
                    model.setName(meetingName.getName());
                    model.setCode(meetingName.getCode());
                    model.setDisabled(meetingName.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return models;
    }

    //getAsbMeetingNames
    @Override
    public List<MeetingName> getAsbMeetingNames() throws ExternalResourceException{
        logger.info("getAsbMeetingNames()");
        List<MeetingName> models = new ArrayList<>();
        try{
            List<za.co.rmb.credit.referencedata.model.MeetingName> meetingNames = getRefDataService().getMeetingName();
            if(meetingNames != null && !meetingNames.isEmpty()){
                meetingNames.forEach(meetingName -> {
                    MeetingName model = new MeetingName();
                    model.setId(new Long(meetingName.getId()).intValue());
                    model.setName(meetingName.getName());
                    model.setCode(meetingName.getCode());
                    model.setDisabled(meetingName.isDisabled());
                    models.add(model);
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        logger.info("getAsbMeetingNames()"+models.size());
        for(MeetingName meetingName: models){
            logger.info("meeting code: "+meetingName.getCode());
        }
        return models;
    }
    private String getStringFromObject(Object value){
        if(value == null) return null;
        return (String) value;
    }

    private boolean getBooleanFromObject(Object value){
        if(value == null) return false;
        return (Boolean) value;
    }

    private LocalDateTime getLocalDatetimeFromString(String date, String dateFormat){
        DateFormat formatter = new SimpleDateFormat(dateFormat);
        try {
            Date parsedDate = formatter.parse(date);
            return new java.sql.Timestamp(parsedDate.getTime()).toLocalDateTime();
        }catch(ParseException r){
            return null;
        }
    }

    @Override
    public Map<String, Currency> getCurrenciesAsMap() {
        List<za.co.rmb.credit.referencedata.model.Currency> currencies = refDataService.getCurrencies();
        Map<String, za.co.rmb.credit.referencedata.model.Currency> currencyMap = new HashMap<>();
        for(za.co.rmb.credit.referencedata.model.Currency currency: currencies){
            currencyMap.put(currency.getCode(), currency);
        }
        return currencyMap;
    }

    @Override
    public int hoursBetweenBusinessDates(Date fromDate, Date toDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return callHolidayService(afSettings.get("af.holidayservice.url"),
                dateFormat.format(fromDate), dateFormat.format(toDate)).getCount();
    }

    @Override
    public List<CounterpartyModel> getCounterpartiesForAshburton(boolean excludeActive) throws ExternalResourceException {
        Map<String, CounterpartyModel> counterpartyModelMap = new TreeMap<>();
        List<String> activeInkombaIds = approvalForumService.getInkombaIdsForActiveWorkflowsForAshburton();
        try{
            List<Map<String, Object>> applications =
                    getApplicationService().getCounterpartyApplication(Arrays.asList(afSettings.get("af.application.statuses").split(";")),
                            approvalForumService.getAllMeetingNamesForAshburton());//check for applications of ashburton----
            if(applications != null && !applications.isEmpty()) {
                applications.forEach(stringObjectMap -> {
                    if(!"Admin Changes".equalsIgnoreCase((String) stringObjectMap.get("proposal_type"))){
                        if(!excludeActive || activeInkombaIds.indexOf((String) stringObjectMap.get("credit_inkomba_id")) < 0) {

                            CounterpartyModel model = new CounterpartyModel();
                            model.setCreditInkombaId((String) stringObjectMap.get("credit_inkomba_id"));
                            model.setCounterpartyName((String) stringObjectMap.get("name"));
                            counterpartyModelMap.putIfAbsent((String) stringObjectMap.get("name"), model);
                        }
                    }
                });
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExternalResourceException(e.getMessage());
        }
        return new ArrayList<>(counterpartyModelMap.values());
    }

    @Override
    public List<CounterpartyModel> getCounterpartiesForAshburtonExternal(String search, boolean isExact) throws ExternalResourceException {
        logger.info("getCounterpartiesForAshburtonExternal(START)");
        List<CounterpartyModel> counterparties = new ArrayList<>();
        if(search != null && !"".equals(search.trim())) {
            try {
                List<Map<String, String>> list = getPeSearch().search(search).exact(isExact).execute();
                if(list != null && !list.isEmpty()){
                    list.forEach(stringStringMap -> {
                        CounterpartyModel countertparty = new CounterpartyModel();
                        countertparty.setCounterpartyName(stringStringMap.get("name"));
                        countertparty.setCreditInkombaId(stringStringMap.get("credit_inkomba_id"));
                        counterparties.add(countertparty);
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        return counterparties;

    }

    @Override
    public LimitsRequestedDTO getLimitRequest(String applicationId) throws ExternalResourceException {
        logger.info("getLimitRequest({}):", applicationId);
        LimitsRequestedDTO dto = new LimitsRequestedDTO();
        if(applicationId != null && !"".equals(applicationId.trim())) {
            try {
                List<Map<String, Object>> list = getCounterpartyService().getLimitsRequested(applicationId);
                logger.info("getLimitRequest()"+list.size());
                if(list != null && !list.isEmpty()){
                    list.forEach(stringStringMap -> {
                        for(String strLimits : limits){
                            if(strLimits.equals(stringStringMap.get("limits_name"))){

                                if(strLimits.equalsIgnoreCase("Subordinated Unsecured")){

                                    dto.setProposedSubordinatedUZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Subordinated Secured")){
                                    dto.setProposedSubordinatedSZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Senior Unsecured")){
                                    dto.setProposedSeniorUZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Senior Secured")){
                                    dto.setProposedSeniorSZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Preference Shares")){
                                    dto.setProposedPreferredSZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Mezzanine Debt")){
                                    dto.setProposedMezzanineDZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Liquidation")){
                                    dto.setProposedLiquidationZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Junior Debt")){
                                    dto.setProposedJuniorDZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Government Guarantee")){
                                    dto.setProposedGovermentGZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                                if(strLimits.equalsIgnoreCase("Converted Equity")){
                                    dto.setProposedConvertedEZar( parseStringToDouble ((String)stringStringMap.get("amounts"), 0));
                                }
                            }
                            dto.setProposedCPYLZar((Double)stringStringMap.get("cptylimit"));
                        }
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        logger.info("getLimitRequest(END)");
        return dto;
    }

    @Override
    public ASBFundsDTO getAsbFunds(String applicationId) throws ExternalResourceException {
        logger.info("getAsbFunds({})", applicationId);
        ASBFundsDTO dto = new ASBFundsDTO();
        if(applicationId != null && !"".equals(applicationId.trim())) {
            try {
                List<Map<String, Object>> list = getCounterpartyService().getAsbFunds(applicationId);
                logger.info("getAsbFunds()11"+list.size());
                if(list != null && !list.isEmpty()){
                    list.forEach(stringStringMap -> {
                        for(String strFunds : funds){
                            if(strFunds.equals(stringStringMap.get("risk_taker_group"))){
                                if(strFunds.equalsIgnoreCase("Liability Driven Investments (LDI)")){
                                    dto.setLdiPercentage(parseStringToDouble ((String)stringStringMap.get("fund_percentage"), 0));
                                }
                                if(strFunds.equalsIgnoreCase("Fixed Income Funds")){
                                    dto.setFixedIncomePercentage( parseStringToDouble ((String)stringStringMap.get("fund_percentage"), 0));
                                }
                                if(strFunds.equalsIgnoreCase("Credit Funds")){
                                    dto.setCreditPercentage(parseStringToDouble ((String)stringStringMap.get("fund_percentage"), 0));
                                }
                                if(strFunds.equalsIgnoreCase("Multi-Asset")){
                                    dto.setMultiAssetPercentage(parseStringToDouble ((String)stringStringMap.get("fund_percentage"), 0));
                                }
                                if(strFunds.equalsIgnoreCase("Multi-Manager")){
                                    dto.setMultiManagerPercentage(parseStringToDouble ((String)stringStringMap.get("fund_percentage"), 0));
                                }
                            }

                        }

                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        logger.info("getAsbFunds(END)");
        return dto;
    }

    private static double parseStringToDouble(String value, double defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : Double.parseDouble(value);
    }
    @Override
    public List<FacilityModel> getAshburtonFacility(String inkombaId, String statuses) throws ExternalResourceException {
        logger.info("getAshburtonFacility({})", inkombaId);
        List<FacilityModel> facilityModels = new ArrayList<>();
        Map<String, Currency> currencyMap = getCurrenciesAsMap();
        BigDecimal zarSpotRate = new BigDecimal(BigInteger.ONE).divide(currencyMap.get("ZAR").getSpotRate(), 20, RoundingMode.HALF_UP);
        if(inkombaId != null && !"".equals(inkombaId.trim())) {
            try {
                List<Map<String, Object>> response = getFacilityService().getFacilityData(inkombaId, Arrays.asList(statuses.split(";")));
                if(response != null && !response.isEmpty()){
                    response.forEach(stringObjectMap -> {
                        FacilityModel facilityModel = new FacilityModel();
                        facilityModel.setFacilityReference(getStringFromObject(stringObjectMap.get("facility_reference")));
                        facilityModel.setRiskType(getStringFromObject(stringObjectMap.get("risk_type")));
                        facilityModel.setTenor(getStringFromObject(stringObjectMap.get("tenor")));
                        facilityModel.setCurrency(getStringFromObject(stringObjectMap.get("currency")));
                        facilityModel.setNetLimit((Double)stringObjectMap.get("proposed_amount"));
                        facilityModel.setFacilityDescription(getStringFromObject(stringObjectMap.get("facility_description")));
                        facilityModel.setCreditApplicationStatus(getStringFromObject(stringObjectMap.get("credit_application_status")));
                        facilityModel.setRiskTaker(getStringFromObject(stringObjectMap.get("risk_taker")));
                        facilityModel.setDealSpecific((Boolean)stringObjectMap.get("deal_specific"));
                        facilityModel.setProposedRating(getStringFromObject(stringObjectMap.get("proposed_rating")));
                        facilityModel.setProposedLgd(Double.valueOf(getStringFromObject(stringObjectMap.get("proposed_lgd"))));
                        facilityModel.setFacilityId(getStringFromObject(stringObjectMap.get("facility_id")));
                        facilityModel.setFacilityRating(getStringFromObject(stringObjectMap.get("facility_rating")));
                        facilityModel.setFacilityInvestmentType(getStringFromObject(stringObjectMap.get("facility_investment_type")));
                        facilityModel.setRiskTakerGroupCode(getStringFromObject(stringObjectMap.get("risk_taker_grouping_code")));
                        facilityModel.setFacilityMovementType(getStringFromObject(stringObjectMap.get("facility_movement_type")));

                        if(!"ZAR".equals(facilityModel.getCurrency())){
                            if(!currencyMap.containsKey(facilityModel.getCurrency()) ||
                                    currencyMap.get(facilityModel.getCurrency()).getSpotRate().doubleValue() == -1){
                                facilityModel.setNetLimitZar(facilityModel.getNetLimit());
                                facilityModel.setProposedLgdZar(facilityModel.getProposedLgd());
                            }else {
                                BigDecimal exchangeRate = new BigDecimal(BigInteger.ONE).divide(
                                        currencyMap.get(facilityModel.getCurrency()).getSpotRate(), 20, RoundingMode.HALF_UP);
                                BigDecimal netLimitValueZar = BigDecimal.valueOf(facilityModel.getNetLimit()).divide(exchangeRate, 20, RoundingMode.HALF_UP).multiply(zarSpotRate);
                                BigDecimal proposedLgdValueZar = BigDecimal.valueOf(facilityModel.getProposedLgd()).divide(exchangeRate, 20, RoundingMode.HALF_UP).multiply(zarSpotRate);
                                facilityModel.setNetLimitZar(netLimitValueZar.setScale(0, RoundingMode.HALF_UP).doubleValue());
                                facilityModel.setProposedLgdZar(proposedLgdValueZar.setScale(0, RoundingMode.HALF_UP).doubleValue());
                            }
                        }else{
                            facilityModel.setNetLimitZar(facilityModel.getNetLimit());
                            facilityModel.setProposedLgdZar(facilityModel.getProposedLgd());
                        }

                        facilityModels.add(facilityModel);
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new ExternalResourceException(e.getMessage());
            }
        }
        logger.info("getAshburtonFacility()");
        return facilityModels;
    }


    private BusinessDay callHolidayService(String url, String fromDate, String toDate){
        try(Response response = getWebTarget(url, fromDate, toDate).request(MediaType.APPLICATION_JSON).get()){
            if (response.getStatus() >= 300) {
                throw new BadRequestException(response.getStatusInfo().toString());
            }
            return (BusinessDay) response.readEntity(new GenericType(BusinessDay.class));
        }catch(Exception e){
            if (e.getCause() != null) {
                throw new ProcessingException(e.getCause().toString() + " - " + url + "businessday/between/hours");
            } else {
                throw e;
            }
        }
    }

    private WebTarget getWebTarget(String url, String fromDate, String toDate) {
        WebTarget webTarget = ClientBuilder.newClient().target(url).path("businessday/between/hours");
        webTarget = webTarget.queryParam("startDate", fromDate);
        webTarget = webTarget.queryParam("endDate", toDate);
        return webTarget;
    }

    private PESearch getPeSearch(){
        logger.info("getPeSearch()");
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(afSettings.get("af.counterpartyservice.ash.base").replace("/api",""));
        za.co.rmb.credit.security.client.InvocationAuth auth = new za.co.rmb.credit.security.client.BearerTokenAuth(afSettings.get("af.counterpartyservice.token"));
        return new HttpPESearch(target, auth, false);
    }

    private CounterpartyService getCounterpartyService(){
        logger.info("getCounterpartyService()");
        Client client = ClientBuilder.newClient();
        //afSettings.get("af.holidayservice.url")
        WebTarget target = client.target(afSettings.get("af.counterpartyservice.ash.base").replace("/api",""));
        za.co.rmb.credit.security.client.InvocationAuth auth = new za.co.rmb.credit.security.client.BearerTokenAuth(afSettings.get("af.counterpartyservice.token"));
        return new CounterpartyServiceImpl(target, auth, false);
    }

    private ApplicationService getApplicationService(){
        logger.info("getApplicationService()");
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(afSettings.get("af.applicationservice.ash.base").replace("/api",""));
        za.co.rmb.credit.security.client.InvocationAuth auth = new za.co.rmb.credit.security.client.BearerTokenAuth(afSettings.get("af.applicationservice.token"));
        return new ApplicationServiceImpl(target, auth, false);
    }

    private FacilityService getFacilityService(){
        logger.info("getFacilityService()");
        Client client = ClientBuilder.newClient();
        //afSettings.get("af.holidayservice.url")
        WebTarget target = client.target(afSettings.get("af.facilityservice.ash.base").replace("/api",""));
        za.co.rmb.credit.security.client.InvocationAuth auth = new za.co.rmb.credit.security.client.BearerTokenAuth(afSettings.get("af.facilityservice.token"));
        return new FacilityServiceImpl(target, auth, false);
    }

    private RefDataService getRefDataService(){
        logger.info("getRefDataService()");
        logger.info("settings"+afSettings.get("af.refdataservice.ash.base"));
        Client client = ClientBuilder.newClient();
        //afSettings.get("af.holidayservice.url")
        WebTarget target = client.target(afSettings.get("af.refdataservice.ash.base").replace("/api",""));
        za.co.rmb.credit.security.client.InvocationAuth auth = new za.co.rmb.credit.security.client.BearerTokenAuth(afSettings.get("af.refdataservice.token"));
        return new RefDataServiceImpl(target, auth, false);
    }

}
