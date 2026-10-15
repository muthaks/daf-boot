package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import za.co.rmb.approvalforum.constants.ApprovalForumConstants;
import za.co.rmb.approvalforum.data.ApprovalForumAuthRepository;
import za.co.rmb.approvalforum.data.ApprovalForumDataRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAduActiveDirectoryUserEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumDsrDocStashRequestsEntity;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.*;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;
import za.co.rmb.docstash.client.DocStashService;
import za.co.rmb.docstash.model.DocStashAttachment;
import za.co.rmb.docstash.model.DocStashDocument;
import za.co.rmb.docstash.model.DocStashMetadata;
import za.co.rmb.docstashrules.model.DisplayMetadata;
import za.co.rmb.docstashrules.model.DocStashResponseDTO;

import jakarta.inject.Named;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

public class ApprovalForumDocstashServiceImpl implements ApprovalForumDocstashService {

    @Inject
    private ApprovalForumDocumentsService documentsService;

    @Inject
    private ApprovalForumService approvalForumService;

    @Inject
    private DocStashService docStashService;

    @Inject
    private ApprovalForumAuthService approvalForumAuthService;

    @Inject
    private ApprovalForumExternalService externalService;

    @Inject
    private ApprovalForumMailService mailService;

    private ApprovalForumDataRepository approvalForumDataRepository;

//    private ApprovalForumAuthRepository approvalForumAuthRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CAT_KEY = "CA";
    private static final String CAT_TYPE_KEY = "CA_CA";

    private Map<String, String> afSettings;

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumDocstashServiceImpl.class);

    @Inject
    public ApprovalForumDocstashServiceImpl(ApprovalForumDataRepository approvalForumDataRepository,
                                            ApprovalForumAuthRepository approvalForumAuthRepository,
                                            @Named("af_settings") Map<String, String> afSettings) {
        this.approvalForumDataRepository = approvalForumDataRepository;
//        this.approvalForumAuthRepository = approvalForumAuthRepository;
//        this.afSettings = afSettings;
    }

    @Override
    public void processDocStashRequests() {
        logger.info("processDocStashRequests(START)");
        approvalForumDataRepository.getOutstandingDocStashRequests().forEach(entity -> {
            try {
                entity.setDocStashDocumentId(uploadDocumentsToDocstash(entity.getWfId()));
                completeDocStashRequest(entity);

                mailService.sendUploadToDocstashSuccessMail(entity.getWfId());
            } catch (Exception e) {
                java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());

                if (entity.getLastTriedOn() == null) {
                    mailService.sendUploadToDocstashFailMail(entity.getWfId(), false);
                } else if (now.toLocalDateTime().getHour() != entity.getLastTriedOn().toLocalDateTime().getHour()) {
                    mailService.sendUploadToDocstashFailMail(entity.getWfId(), true);
                }
                updateDocStashRequest(entity, e.getMessage());
            }
            logger.info("processDocStashRequests(END)");
        });
    }

    /**
     * This method is to refresh the ADU table so the users details are up-to-date
     * and eliminating the docstash error of not being able to find users
     *
     * @throws ResourceException
     */
    @Override
    public void processAduCleanup() throws ResourceException {

        //create uuth client
//        AuthServiceClient client = new AuthServiceClient(afSettings.get(ApprovalForumConstants.AUTHSERVICEBASE));
//
//        //Get all Credit Analysts from Active Directory
//        List<ActiveDirectoryUserModel> result = new ArrayList<>();
//        result = client.Users("RMB", "Credit Analyst");
//
//        if (result == null) {
//            result = new ArrayList<>();
//        }
//
//        // Track AD users so we can detect deletions
//        Set<String> adUserKeys = new HashSet<>();
//
//        // Handle users that exist in AD
//        for (ActiveDirectoryUserModel adUser : result) {
//
//            if (adUser == null) {
//                continue;
//            }
//
//            String alias = adUser.getUserPrincipalName();
//            String displayName = adUser.getDisplayName();
//
//            if (alias == null || displayName == null) {
//                continue;
//            }
//
//            adUserKeys.add(alias + "|" + displayName);
//
//            ApprovalForumAduActiveDirectoryUserEntity entity =
//                    approvalForumAuthRepository.getActiveDirectoryUserByDisplayNameAndAlias(displayName, alias);
//
//            // - User not in ADU → INSERT
//            // - User in ADU but details changed → UPDATE
//            if (entity == null ||
//                    adUserDetailsHaveChanged(entity, adUser)) {
//
//                approvalForumAuthRepository.createUpdateActiveDirectoryUser(
//                        alias,
//                        displayName,
//                        adUser.getGivenName(),
//                        adUser.getSn(),
//                        adUser.getMail()
//                );
//            }
//            // else: user exists in both AD and ADU with same details → skip
//        }

        // Handle users that exist in ADU but NOT in AD → DELETE

//        List<ApprovalForumAduActiveDirectoryUserEntity> allAduUsers =
//                approvalForumAuthRepository.getAllActiveDirectoryUsers();

        /*for (ApprovalForumAduActiveDirectoryUserEntity aduUser : allAduUsers) {

            String key = aduUser.getAlias() + "|" + aduUser.getDisplayName();

            if (!adUserKeys.contains(key)) {

                approvalForumAuthRepository.deleteActiveDirectoryUserByDisplayNameAndAlias(
                        aduUser.getDisplayName(),
                        aduUser.getAlias()
                );
            }
        }*/

    }

    private boolean adUserDetailsHaveChanged(
            ApprovalForumAduActiveDirectoryUserEntity entity,
            ActiveDirectoryUserModel adUser) {

        return !Objects.equals(entity.getDisplayName(), adUser.getDisplayName())
                || !Objects.equals(entity.getGivenName(), adUser.getGivenName())
                || !Objects.equals(entity.getSurname(), adUser.getSn())
                || !Objects.equals(entity.getMail(), adUser.getMail());
    }


    private void updateDocStashRequest(ApprovalForumDsrDocStashRequestsEntity entity, String message) {
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        entity.setLastTriedMessage(message);
        entity.setRetryCount(entity.getRetryCount() + 1);
        entity.setLastTriedOn(now);
        approvalForumDataRepository.updateApprovalForumDsrDocStashRequestsEntity(entity);
    }

    private void completeDocStashRequest(ApprovalForumDsrDocStashRequestsEntity entity) {
        java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        entity.setComplete(true);
        entity.setCompletedOn(now);
        entity.setLastTriedMessage("Successfully uploaded to docstash");
        entity.setRetryCount(entity.getRetryCount() + 1);
        entity.setLastTriedOn(now);
        approvalForumDataRepository.updateApprovalForumDsrDocStashRequestsEntity(entity);
    }

    @Override
    public int uploadDocumentsToDocstash(int workflowId) throws ResourceException {
        logger.info("uploadDocumentsToDocstash(START)");
        WorkflowDocumentListResponse listResponse = documentsService.getWorkflowDocuments(Integer.valueOf(workflowId).toString());
        RequestData requestData = approvalForumService.getRequest(workflowId, null, false, null);
        Map<String, Object> docstashResponse = createDocument(workflowId, requestData);

        int docstashDocumentId;
        if (docstashResponse.get("status").equals("success")) {
            docstashDocumentId = (Integer) docstashResponse.get("documentId");
        } else {
            throw new ResourceException("Non success status from docstash");
        }

        if (listResponse != null && listResponse.getDocuments() != null && !listResponse.getDocuments().isEmpty()) {
            for (ApplicationDocumentListModel model : listResponse.getDocuments()) {
                uploadAttachmentToDocstash(model, docstashDocumentId);
            }
        }
        logger.info("uploadDocumentsToDocstash(END)");
        return docstashDocumentId;
    }

    private Map<String, Object> createDocument(int workflowId, RequestData requestData) throws ResourceException {
        logger.info("createDocument(START)");
        DocStashDocument docStashDocument = new DocStashDocument();
        docStashDocument.setCategory(docStashService.getDocStashCategory(CAT_KEY));
        docStashDocument.setCategoryType(docStashService.getDocStashCategoryType(CAT_TYPE_KEY));
        docStashDocument.setExternalDocumentId(Integer.valueOf(workflowId).toString());
        docStashDocument.setMetadata(setDocstashMetadata(requestData));
        logger.info("createDocument(END)");
        return docStashService.createDocument(docStashDocument);
    }

    private List<DocStashMetadata> setDocstashMetadata(RequestData requestData) throws ResourceException {
        List<DocStashMetadata> docStashMetadataList = new ArrayList<>();
        DocStashResponseDTO responseDTO = docStashService.displayMetadata(CAT_KEY, CAT_TYPE_KEY);
        logger.info("request data:" + requestData.getCounterpartyFullDetail().getCreditApplication().getMeetingName());
        if (responseDTO == null || responseDTO.getDisplayMetadata() == null || responseDTO.getDisplayMetadata().isEmpty()) {
            throw new ResourceException("Could not pull metadata from DocStash");
        }
        for (DisplayMetadata displayMetadata : responseDTO.getDisplayMetadata()) {
            switch (displayMetadata.getName()) {
                case "counterpartyName":
                    docStashMetadataList.add(
                            getDocStashMetadata(displayMetadata, "[\"" + requestData.getCounterpartyFullDetail().getCounterpartyDetail().getCounterpartyName() + ";" +
                                    requestData.getCounterpartyFullDetail().getCounterpartyDetail().getCreditInkombaId() + "\"]"));
                    break;
                case "creditDecision":
                    docStashMetadataList.add(getDocStashMetadata(displayMetadata, getCreditDecision(requestData)));
                    break;
                case "approvalBody":
                    docStashMetadataList.add(getDocStashMetadata(displayMetadata, getMeetingName(requestData)));
                    break;
                case "approvalDate":
                    docStashMetadataList.add(getDocStashMetadata(displayMetadata,
                            getDateAsString(Date.from(requestData.getCompletedOn().atZone(ZoneId.systemDefault()).toInstant()), "yyyy-MM-dd")));
                    break;
                case "portfolio":
                    docStashMetadataList.add(getDocStashMetadata(displayMetadata, getPortfolio(requestData)));
                    break;
                case "creditAnalyst":
                    docStashMetadataList.add(getDocStashMetadata(displayMetadata, getCreditAnalyst(requestData)));
                    break;
                case "description":
                    docStashMetadataList.add(getDocStashMetadata(displayMetadata, "Digital Approval Forum System Upload"));
                    break;
            }
        }
        docStashMetadataList.add(new DocStashMetadata().withName("uploadedDate").withSearchLabel("Uploaded Date").withSimpleSearchable(true).withValue(
                getDateAsString(Calendar.getInstance().getTime(), "yyyy-MM-dd'T'HH:mm:ss.SSSZ")));
        docStashMetadataList.add(new DocStashMetadata().withName("uploadedBy").withSearchLabel("Uploaded By").withSimpleSearchable(true).withValue(
                "Digital Approval Forum System"));
        return docStashMetadataList;
    }

    private DocStashMetadata getDocStashMetadata(DisplayMetadata displayMetadata, String value) {
        DocStashMetadata metadata = new DocStashMetadata();
        metadata.setName(displayMetadata.getName());
        metadata.setValue(value);
        metadata.setSearchLabel(displayMetadata.getSearchLabel());
        metadata.setSimpleSearchable(displayMetadata.isSimpleSearchable());
        return metadata;
    }

    private String getCreditDecision(RequestData requestData) throws ResourceException {
        CreditDecision creditDecision;
        try {
            if (null != requestData.getTenantId() && requestData.getTenantId() == 2) {
                creditDecision = externalService.getAsbCreditDecisions().stream().filter(
                        decision -> decision != null && decision.getCode() != null && decision.getCode().equalsIgnoreCase(requestData.getStatus().value())).findAny().orElse(null);
                logger.info("getCreditDecision::" + creditDecision);
            } else {
                creditDecision = externalService.getCreditDecisions().stream().filter(
                        decision -> decision != null && decision.getCode() != null && decision.getCode().equalsIgnoreCase(requestData.getStatus().value())).findAny().orElse(null);
                logger.info("getCreditDecision:else:" + creditDecision);
            }

            if (creditDecision == null) {
                throw new ResourceException("Could not find credit decision equals to " + requestData.getStatus().value());
            }
            return creditDecision.getCode() + ";" + creditDecision.getId();
        } catch (ExternalResourceException e) {
            throw new ResourceException("Error calling service to pull credit decisions");
        }
    }

    private String getMeetingName(RequestData requestData) throws ResourceException {
        MeetingName meetingName = null;
        try {
            if (null != requestData.getTenantId() && requestData.getTenantId() == 2) {
                meetingName = externalService.getAsbMeetingNames().stream().filter(
                        meeting -> meeting != null && meeting.getCode() != null &&
                                meeting.getCode().equalsIgnoreCase(requestData.getCounterpartyFullDetail().getCreditApplication().getMeetingName())).findAny().orElse(null);
            } else {
                meetingName = externalService.getMeetingNames().stream().filter(
                        meeting -> meeting != null && meeting.getCode() != null &&
                                meeting.getCode().equalsIgnoreCase(requestData.getCounterpartyFullDetail().getCreditApplication().getMeetingName())).findAny().orElse(null);
            }
            if (meetingName == null) {
                throw new ResourceException("Could not find meeting name equal to " +
                        requestData.getCounterpartyFullDetail().getCreditApplication().getMeetingName());
            }
            return meetingName.getCode() + ";" + meetingName.getId();
        } catch (ExternalResourceException e) {
            throw new ResourceException("Error calling service to pull meeting names");
        }
    }

    private String getPortfolio(RequestData requestData) throws ResourceException {
        WholesaleCreditPortfolioData portfolioData;
        try {
            if (null != requestData.getTenantId() && requestData.getTenantId() == 2) {
                portfolioData = externalService.getAsbWholesaleCreditPortfolios().stream().filter(
                        portfolio -> portfolio != null && portfolio.getCode() != null &&
                                portfolio.getCode().equalsIgnoreCase(requestData.getCounterpartyFullDetail().getCreditApplication().getWholesaleCreditPortfolio())).findAny().orElse(null);
            } else {
                portfolioData = externalService.getWholesaleCreditPortfolios().stream().filter(
                        portfolio -> portfolio != null && portfolio.getCode() != null &&
                                portfolio.getCode().equalsIgnoreCase(requestData.getCounterpartyFullDetail().getCreditApplication().getWholesaleCreditPortfolio())).findAny().orElse(null);
            }

            if (portfolioData == null) {
                throw new ResourceException("Could not find Wholesale credit portfolio equal to " +
                        requestData.getCounterpartyFullDetail().getCreditApplication().getWholesaleCreditPortfolio());
            }
            return portfolioData.getCode() + ";" + portfolioData.getId();
        } catch (ExternalResourceException e) {
            throw new ResourceException("Error calling service to pull wholesale credit portfolios");
        }
    }

    /**
     * Finds the Credit Analyst in Docstash's U_Users table for the given request.
     * <p>
     * How it works:
     * 1. Looks up the user in DAF's ADU_ActiveDirectoryUser table by alias (requestData.submittedBy)
     * 2. Calls Docstash GET /api/auth/users?role=Credit Analyst&displayName=<name>
     * - Docstash queries its U_Users table: WHERE DisplayName LIKE '%name%' AND DocStashGroup = 'Credit Analyst'
     * 3. Filters the returned list by matching either:
     * - sAMAccountName (stripped of @domain) == mnemonic (stripped of @domain), case-insensitive
     * - OR mail == email, case-insensitive
     * 4. Returns "name;sAMAccountName" for the matched user
     * <p>
     * The stripDomain() call handles cases where Docstash U_Users.SamAccountName
     * stores the full email (e.g. user@rmb.co.za) while DAF stores just the alias.
     */
    private String getCreditAnalyst(RequestData requestData) throws ResourceException {
        logger.info("***getCreditAnalyst");
        ActiveDirectoryUserModel userModel;

        UserDetails userDetails = approvalForumAuthService.getUserDetailsByAliasIgnoreCase(requestData.getSubmittedBy());
        logger.info("***userDetails(name: {}, surname: {}, email: {}, displayName: {}, mnemonic: {})",
                userDetails.getName(), userDetails.getSurname(), userDetails.getEmail(), userDetails.getDisplayName(),
                userDetails.getMnemonic());

        if (userDetails == null) {
            throw new ResourceException("Could not get user details for " + requestData.getSubmittedBy());
        }

        userModel = docStashService.getUsersForRole("Credit Analyst",
                        userDetails.getName().indexOf("@") > 0 ?
                                userDetails.getName().substring(0, userDetails.getName().indexOf("@")) :
                                userDetails.getName())
                .stream()
                .filter(user -> user != null &&
                        (
                                (user.getsAMAccountName() != null &&
                                        stripDomain(user.getsAMAccountName())
                                                .equalsIgnoreCase(stripDomain(userDetails.getMnemonic()))
                                ) ||
                                        (user.getMail() != null && user.getMail().equalsIgnoreCase(userDetails.getEmail()))
                        )
                ).findAny().orElse(null);

        logger.info("*** getCreditAnalyst().userModel({})", userModel);

        if (userModel == null) {
            throw new ResourceException("Could not find Docstash Credit Analyst for " + requestData.getSubmittedBy());
        }
        return userModel.getName() + ";" + userModel.getsAMAccountName();
    }

    private String getDateAsString(Date date, String dateFormat) {
        DateFormat df = new SimpleDateFormat(dateFormat);
        return df.format(date);
    }

    private void uploadAttachmentToDocstash(ApplicationDocumentListModel documentListModel, int docstashDocumentId) {
        logger.info("uploadAttachmentToDocstash(START)");
        ApplicationDocumentModel documentModel = documentsService.downloadDocument(Integer.valueOf(documentListModel.getAsdID()).toString());

        DocStashAttachment docStashAttachment = new DocStashAttachment();
        docStashAttachment.setDocumentId(docstashDocumentId);
        docStashAttachment.setFilesize(documentModel.getFileSize());
        docStashAttachment.setFiledata(documentModel.getFileData());
        docStashAttachment.setFilename(documentModel.getFileName());
        docStashAttachment.setOcrProcessed(false);

        docStashService.uploadAttachment(docStashAttachment);
        logger.info("uploadAttachmentToDocstash(END)");
    }

    private String stripDomain(String value) {
        if (value == null) return "";
        int atIndex = value.indexOf("@");
        return atIndex > 0 ? value.substring(0, atIndex) : value;
    }

}
