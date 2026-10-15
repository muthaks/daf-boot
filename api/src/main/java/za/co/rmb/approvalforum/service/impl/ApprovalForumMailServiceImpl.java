package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.hashids.Hashids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumDataRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.*;
import za.co.rmb.approvalforum.service.exception.ResourceException;
import za.co.rmb.credit.mailservice.model.AttachmentModel;
import za.co.rmb.credit.mailservice.model.MailModel;
import za.co.rmb.credit.mailservice.model.MailResponseModel;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

public class ApprovalForumMailServiceImpl implements ApprovalForumMailService {

    @jakarta.inject.Inject
    private ApprovalForumExternalService approvalForumExternalService;

    @jakarta.inject.Inject
    private ApprovalForumService approvalForumService;

    @jakarta.inject.Inject
    private ApprovalForumListManagementService approvalForumListManagementService;

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumMailServiceImpl.class);

    private Map<String, String> afSettings;

    private MailServiceClient mailServiceClient;

    private ApprovalForumAuthService approvalForumAuthService;

    private ApprovalForumDocumentsService approvalForumDocumentsService;

    private final ApprovalForumDataRepository approvalForumDataRepository;

    private Map<String, Map<String, Integer>> approverMailToDetails;

    private static final String bodyStyle = "<body style=\"background-color:white;font-family:calibri;\">";

    private static final String globalTableStyle = "<head><style>\n" +
            "table, th, td {\n" +
            "  border: 1px solid black;\n" +
            "  border-collapse: collapse;\n" +
            "}\n" +
            "</style></head>";

    private static final String tableHeadingActionStyle = "<table style=\"width:100%\">\n" +
            "  <tr style=\"background-color:#D5D1D1\">\n" +
            "    <th colspan=\"7\">Action Required By You</th>\n" +
            "  </tr>";

    private static final String tableHeadingNoActionStyle = "<table style=\"width:100%\">\n" +
            "  <tr style=\"background-color:#D5D1D1\">\n" +
            "    <th colspan=\"7\">No Action Required By You</th>\n" +
            "  </tr>";

    private static final String tableHeadingAppDetailsStyle =
          "<tr style=\"background-color:#D5D1D1\">\n" +
            "<th>Request ID</th>\n" +
            "<th>Counterparty Name</th>\n" +
            "<th>Mandate</th>\n" +
            "<th>Created Date</th>\n" +
            "<th>Aging Status</th>\n" +
            "<th>Submitted By</th>\n" +
            "<th>Outstanding Approvals</th>\n" +
          "</tr>";

    private static final String tableHeadingCapturerAppDetailsStyle = "<table style=\"width:100%\">\n" +
            "<tr style=\"background-color:#D5D1D1\">\n" +
                    "<th>Request ID</th>\n" +
                    "<th>Counterparty Name</th>\n" +
                    "<th>Mandate</th>\n" +
                    "<th>Created Date</th>\n" +
                    "<th>Aging Status</th>\n" +
                    "<th>Submitted By</th>\n" +
                    "<th>Outstanding Approvals</th>\n" +
                    "</tr>";
    private static final String tableRowFirstStyle = "<tr style=\"background-color:#white\">";
    private static final String tableRowSecondStyle = "<tr style=\"background-color:#F9EB76\">";
    private static final String tableRowThirdStyle = "<tr style=\"background-color:#FC6E69\">";

    enum Notification {
        ACTION("ACTION"),
        NO_ACTION("NO_ACTION");

        private String description = null;

        private Notification(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final Hashids hashids;

    @Inject
    public ApprovalForumMailServiceImpl(MailServiceClient mailServiceClient,
                                        ApprovalForumDataRepository approvalForumDataRepository,
                                        @jakarta.inject.Named("af_settings") Map<String, String> afSettings,
                                        ApprovalForumAuthService approvalForumAuthService,
                                        ApprovalForumDocumentsService approvalForumDocumentsService){
        this.afSettings = afSettings;
        this.mailServiceClient = mailServiceClient;
        this.approvalForumDataRepository = approvalForumDataRepository;
        this.approvalForumAuthService = approvalForumAuthService;
        this.approvalForumDocumentsService = approvalForumDocumentsService;
        this.hashids = new Hashids("", 0, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
    }

    @Override
    public MailResponseModel sendTestMail() {
        logger.info("sendTestMail()");
        MailModel mailModel = new MailModel();
        mailModel.setFrom("approvalForum@rmb.co.za");
        String body;

        mailModel.setTo(afSettings.get("af.mailservice.test.dl"));
        mailModel.setSubject("APPROVAL FORUM - Test Submit mail");
        body = "This is a test mail";

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat(body);
        htmlStr = htmlStr.concat("</html>");

        mailModel.setBody(htmlStr);
        return mailServiceClient.Send(afSettings.get("af.mailservice.token"), mailModel);
    }

    @Override
    public MailResponseModel sendNewRequestMail(int workflowId) {
        logger.info("sendNewRequestMail({})", workflowId);
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);

        String creditAnalystEmail = getCreditAnalystEmail(counterpartyDetailEntities),
                creditAnalyst = getCreditAnalystName(counterpartyDetailEntities),
                counterpartyName = getCounterPartyName(counterpartyDetailEntities);

        if("Unknown".equals(creditAnalystEmail)) return new MailResponseModel();

        MailModel mailModel = createMailModel(creditAnalystEmail, "DAF request awaiting submission for '" + counterpartyName + "'");

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear " + creditAnalyst + ",</p>");
        htmlStr = htmlStr.concat("<p>Please note that a Credit Application is awaiting your submission on DAF.</p>");
        htmlStr = htmlStr.concat(getEmailEndingSubmission(workflowId, counterpartyName, null, getMandate(counterpartyDetailEntities)));

        mailModel.setBody(htmlStr);
        persistMailRequest(mailModel, workflowId, "NewRequest");
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendRequestSubmitMail(int workflowId) {
        logger.info("sendRequestSubmitMail({})", workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        MailModel mailModel;

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear Approver,</p>");

        if(entity.isSpecific()){
            mailModel = createMailModel(getMailTo(entity), "Specific Approval - DAF request awaiting approval for '" + counterpartyName + "'");
            htmlStr = htmlStr.concat("<p>Please note that you have been marked as a Specific Approver on Credit Application for '"
                    + counterpartyName + "' and is awaiting your review and approval on DAF.</p>");
        }else if(entity.isConfidential()){
            mailModel = createMailModel(getMailTo(entity), "CONFIDENTIAL - DAF request awaiting approval for '" + counterpartyName + "'");
            htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '"
                    + counterpartyName + "' has been marked as Confidential and is awaiting your review and approval on DAF.</p>");
        }else{
            mailModel = createMailModel(getMailTo(entity), "General Approval - DAF request awaiting approval for '" + counterpartyName + "'");
            htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '" + counterpartyName + "' is awaiting your review and approval on DAF.</p>");
        }

        mailModel.setBody(htmlStr.concat(getEmailEndingForApprovalOffline(workflowId, counterpartyName, null, getMandate(counterpartyDetailEntities))));
        persistMailRequest(mailModel, workflowId, "RequestSubmit");
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendRequestSubmitToChairpersonMail(int workflowId, List<ListManagementList> chairpersonLists) {
        logger.info("sendRequestSubmitToChairpersonMail({})", workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        MailModel mailModel;

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear Chairperson,</p>");

        mailModel = createMailModel(getChairpersonMailTo(entity, chairpersonLists), "Chairperson Approval - DAF request awaiting approval for '" + counterpartyName + "'");
        htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '" + counterpartyName + "' is awaiting your review and approval on DAF.</p>");


        mailModel.setBody(htmlStr.concat(getEmailEndingForChair(workflowId, counterpartyName, null, getMandate(counterpartyDetailEntities))));
        persistMailRequest(mailModel, workflowId, "RequestSubmitToChairperson");
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendRequestCompleteMail(int workflowId) {
        logger.info("sendRequestCompleteMail({})", workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        MailModel mailModel = createMailModel(getCompleteMailTo(entity, entity.getStatus()), "DAF request for '" + counterpartyName + "' has been " + entity.getStatus());

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear user,</p>");
        htmlStr = htmlStr.concat("<p>Please note that a Credit Decision on Credit Application for '" + counterpartyName + "' has been reached.</p>");

        mailModel.setBody(htmlStr.concat(getEmailEnding(workflowId, counterpartyName, entity.getStatus(), getMandate(counterpartyDetailEntities))));
        persistMailRequest(mailModel, workflowId, "RequestComplete");
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendUploadToDocstashSuccessMail(int workflowId) {
        logger.info("sendUploadToDocstashSuccessMail({})", workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        String mailTo = getSubmittedByEmail(entity.getSubmittedBy());
        mailTo = mailTo + afSettings.get("af.corp.credit.secretary.mail") + ",";
        mailTo = mailTo + afSettings.get("af.corp.credit.specialist.mail");

        MailModel mailModel = createMailModel(mailTo, "DAF request for '" + counterpartyName + "' has been Successfully uploaded onto DocStash");

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear user,</p>");
        htmlStr = htmlStr.concat("<p>Please note that Credit Application for '" + counterpartyName + "' has been Successfully uploaded onto DocStash.</p>");
        
        ApprovalForumDsrDocStashRequestsEntity docstashRequest = approvalForumDataRepository.getDocStashRequest(workflowId);

        mailModel.setBody(htmlStr.concat(getEmailEndingDocStash(workflowId, counterpartyName, entity.getStatus(), getMandate(counterpartyDetailEntities), docstashRequest.getDocStashDocumentId())));
        persistMailRequest(mailModel, workflowId, "UploadToDocstashSuccess");
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendUploadToDocstashFailMail(int workflowId, boolean onlyToSupport) {
        logger.info("sendUploadToDocstashFailMail({})", workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        String mailTo = afSettings.get("af.support.mail.dl") ;

        MailModel mailModel = createMailModel(mailTo, "URGENT- DAF request for '" + counterpartyName + "' could NOT be uploaded onto DocStash");

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear user,</p>");
        htmlStr = htmlStr.concat("<p>Please note that Credit Application for '" + counterpartyName + "' could NOT be uploaded onto DocStash.</p>");
        htmlStr = htmlStr.concat("<p>Please investigate urgently.</p>");
        htmlStr = htmlStr.concat("<p>WorkflowId: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("</html>");
        mailModel.setBody(htmlStr);
        persistMailRequest(mailModel, workflowId, "UploadToDocstashFailSupport");

        if(!onlyToSupport) {
            mailTo = getSubmittedByEmail(entity.getSubmittedBy());
            mailTo = mailTo + afSettings.get("af.corp.credit.secretary.mail") + ",";
            mailTo = mailTo + afSettings.get("af.corp.credit.specialist.mail");

            mailModel = createMailModel(mailTo, "DAF request for '" + counterpartyName + "' could NOT be uploaded onto DocStash");

            htmlStr = "<html>";
            htmlStr = htmlStr.concat(bodyStyle);
            htmlStr = htmlStr.concat("<p>Dear user,</p>");
            htmlStr = htmlStr.concat("<p>Please note that Credit Application for '" + counterpartyName + "' could NOT be uploaded onto DocStash.</p>");
            htmlStr = htmlStr.concat("<p>The Suppport team is busy investigating. Please wait for the Upload Success email before completing your process.</p>");

            mailModel.setBody(htmlStr.concat(getEmailEndingDocStash(workflowId, counterpartyName, entity.getStatus(), getMandate(counterpartyDetailEntities), 0)));
            persistMailRequest(mailModel, workflowId, "UploadToDocstashFail");
        }
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendSlaReminderMail(int workflowId, boolean isFirstEscalation) {
        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities =
                approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
        ApprovalForumWfWorkflowEntity entity = approvalForumDataRepository.getWorkflow(workflowId);
        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        MailModel mailModel;

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear Approver,</p>");

        List<String> approvals = getApprovals(entity, true);
        if(isFirstEscalation){
            mailModel = createMailModel(getMailTo(entity) + "," + getSubmittedByEmail(entity.getSubmittedBy()),
                    "DAF request awaiting approval for '" + counterpartyName + "' < " + getFirstEscalationHours() + " Hours left for approval");
            if(approvals.size() > 0){
                htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '" + counterpartyName + "' is awaiting further review and approval on DAF.</p>");
            }else{
                htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '" + counterpartyName + "' is awaiting review and approval on DAF.</p>");
            }
            htmlStr = htmlStr.concat("<p><b>There is less than " + getFirstEscalationHours() + " hours till SLA expiry.</b></p>");
        }else{
            mailModel = createMailModel(getMailTo(entity) + "," + getSubmittedByEmail(entity.getSubmittedBy()),
                    "DAF request awaiting approval for '" + counterpartyName + "' " + Integer.parseInt(afSettings.get("af.secondescalation.hours")) + " hour SLA has expired");
            if(approvals.size() > 0) {
                htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '" + counterpartyName + "' is awaiting further review and approval on DAF.</p>");
            }else{
                htmlStr = htmlStr.concat("<p>Please note that a Credit Application for '" + counterpartyName + "' is awaiting review and approval on DAF.</p>");
            }
            htmlStr = htmlStr.concat("<p><b>" + Integer.parseInt(afSettings.get("af.secondescalation.hours")) + " Hour SLA has been reached for this request.</b></p>");
        }
        if(approvals != null && !approvals.isEmpty()){
            htmlStr = htmlStr.concat("<p>");
            if(approvals.size() == 1) {
                htmlStr = htmlStr.concat("The following member has already submitted a decision:");
            }else{
                htmlStr = htmlStr.concat("The following members have already submitted a decision:");
            }
            for(String name: approvals){
                htmlStr = htmlStr.concat("<br>" + name + "");
            }
            htmlStr = htmlStr.concat("</p>");
        }

        mailModel.setBody(htmlStr.concat(getEmailEndingForApproval(workflowId, counterpartyName, null, getMandate(counterpartyDetailEntities))));
        persistMailRequest(mailModel, workflowId, "SlaReminder");
        return new MailResponseModel(true);
    }

    @Override
    public MailResponseModel sendApproverSlaReminderMail(HashMap<String, HashMap<String, List<WorkflowMailDTO>>> notificationMap) {

//        String emails = notificationMap.keySet().stream().distinct().collect(Collectors.joining(","));

//        int count = 0;
//        for(Map<String, List<WorkflowMailDTO>> map: notificationMap.values()){
//            count += map.values().stream().mapToInt(List::size).sum();
//        }
//
        for (Map.Entry<String, HashMap<String, List<WorkflowMailDTO>>> e : notificationMap.entrySet()) {

            String htmlStr = "<html>";
            htmlStr = htmlStr.concat(globalTableStyle);
            htmlStr = htmlStr.concat(bodyStyle);
            htmlStr = htmlStr.concat("<p>Dear Approver</p>");
            String singular = "<p>The following Credit Application still requires full quorum in order to be fully approved:</p>";
            String plural = "<p>The following Credit Applications still require full quorum in order to be fully approved:</p>";

            int count = 0;

            HashMap<String, List<WorkflowMailDTO>> listsForPerson =  e.getValue();

            count += listsForPerson.values().stream().mapToInt(List::size).sum();

            if (count > 1) {
                htmlStr = htmlStr.concat(plural);
            } else {
                htmlStr = htmlStr.concat(singular);
            }

            String personToNotify = e.getKey();

            MailModel mailModel = createMailModel(personToNotify, "Outstanding DigiApp Requests");
            mailModel.setCc("");

            // List of workflow item notifications to send to above person
            HashMap<String, List<WorkflowMailDTO>> type = e.getValue();

            String actionRequiredWorkflows = "";
            String noActionRequiredWorkflows = "";

            for (Map.Entry<String, List<WorkflowMailDTO>> entry : type.entrySet()) {
                String typeKey = entry.getKey();
                List<WorkflowMailDTO> applications = entry.getValue();
                if (typeKey.equals(Notification.ACTION.name())) {
                    htmlStr = htmlStr.concat(tableHeadingActionStyle);
                    htmlStr = htmlStr.concat(tableHeadingAppDetailsStyle);
                    for (WorkflowMailDTO workflowMailDTO : applications) {
                        if (actionRequiredWorkflows.length() < 1) {
                            actionRequiredWorkflows = actionRequiredWorkflows.concat(String.valueOf(workflowMailDTO.getWorkflowId()));
                        } else {
                            actionRequiredWorkflows = actionRequiredWorkflows.concat(", " + workflowMailDTO.getWorkflowId());
                        }
                        if (workflowMailDTO.isFirst()) {
                            htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowFirstStyle));
                        } else if (workflowMailDTO.isSecond()) {
                            htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowSecondStyle));
                        } else if (workflowMailDTO.isThird()) {
                            htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowThirdStyle));
                        }
                    }
                    htmlStr = htmlStr.concat("</table><br>");

                } else if (typeKey.equals(Notification.NO_ACTION.name())) {
                    htmlStr = htmlStr.concat(tableHeadingNoActionStyle);
                    htmlStr = htmlStr.concat(tableHeadingAppDetailsStyle);
                    for (WorkflowMailDTO workflowMailDTO : applications) {
                        if (noActionRequiredWorkflows.length() < 1) {
                            noActionRequiredWorkflows = noActionRequiredWorkflows.concat(String.valueOf(workflowMailDTO.getWorkflowId()));
                        } else {
                            noActionRequiredWorkflows = noActionRequiredWorkflows.concat(", " + workflowMailDTO.getWorkflowId());
                        }
                        if (workflowMailDTO.isFirst()) {
                            htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowFirstStyle));
                        } else if (workflowMailDTO.isSecond()) {
                            htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowSecondStyle));
                        } else if (workflowMailDTO.isThird()) {
                            htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowThirdStyle));
                        }
                    }
                    htmlStr = htmlStr.concat("</table><br>");
                }
            }

            htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
            htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
            htmlStr = htmlStr.concat("</body></html>");

            mailModel.setBody(htmlStr);

            MailResponseModel mailResponseModel = sendDailyMail(mailModel);

            ApprovalForumMrdaMailRequestsDailyAllEntity entity = new ApprovalForumMrdaMailRequestsDailyAllEntity();
            entity.setSentTo(personToNotify);
            entity.setCreatedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            entity.setRetryCount(0);
            entity.setApproverActionWfIds(actionRequiredWorkflows);
            entity.setApproverNoActionWfIds(noActionRequiredWorkflows);
            entity.setCapturerWfIds("");
            entity.setMailModel(convertObjectToByteArray(mailModel));
            entity.setMailProcess("DailyApproverSlaReminder");

            if(mailResponseModel.isSuccess()){
                java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
                entity.setCompletedOn(now);
                entity.setLastTriedMessage("Mail sent successfully");
                entity.setComplete(true);
            }else{
                entity.setLastTriedMessage(mailResponseModel.getReason());
            }
            approvalForumDataRepository.persistDailyMailRequest(entity);

        }

        return new MailResponseModel(true);

    }


    @Override
    public MailResponseModel sendCapturerSlaReminderMail(HashMap<String, List<WorkflowMailDTO>> notificationMap) {

        for (Map.Entry<String, List<WorkflowMailDTO>> e : notificationMap.entrySet()) {

            String htmlStr = "<html>";
            htmlStr = htmlStr.concat(globalTableStyle);
            htmlStr = htmlStr.concat(bodyStyle);
            htmlStr = htmlStr.concat("<p>Dear Credit Analyst</p>");
            String singular = "<p>The following Credit Application still requires full quorum in order to be fully approved:</p>";
            String plural = "<p>The following Credit Applications still require full quorum in order to be fully approved:</p>";

            List<WorkflowMailDTO> listForPerson =  e.getValue();

            int count =  listForPerson.size();

            if (count > 1) {
                htmlStr = htmlStr.concat(plural);
            } else {
                htmlStr = htmlStr.concat(singular);
            }

            htmlStr = htmlStr.concat(tableHeadingCapturerAppDetailsStyle);

            String personToNotify = e.getKey();

            MailModel mailModel = createMailModel(personToNotify, "Outstanding DigiApp Requests");
            mailModel.setCc("");

            String capturerNotificationWorkflows = "";

            // List of workflow item notifications to send to above person
            List<WorkflowMailDTO> items = e.getValue();

            for (WorkflowMailDTO workflowMailDTO : items) {
                if (capturerNotificationWorkflows.length() < 1) {
                    capturerNotificationWorkflows = capturerNotificationWorkflows.concat(String.valueOf(workflowMailDTO.getWorkflowId()));
                } else {
                    capturerNotificationWorkflows = capturerNotificationWorkflows.concat(", " + workflowMailDTO.getWorkflowId());
                }
                 if (workflowMailDTO.isFirst()) {
                    htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowFirstStyle));
                } else if (workflowMailDTO.isSecond()) {
                    htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowSecondStyle));
                } else if (workflowMailDTO.isThird()) {
                    htmlStr = htmlStr.concat(buildApplicationHtmlDetails(workflowMailDTO, tableRowThirdStyle));
                }

            }

            htmlStr = htmlStr.concat("</table><br>");

            htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
            htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
            htmlStr = htmlStr.concat("</body></html>");

            mailModel.setBody(htmlStr);

            MailResponseModel mailResponseModel = sendDailyMail(mailModel);

            ApprovalForumMrdaMailRequestsDailyAllEntity entity = new ApprovalForumMrdaMailRequestsDailyAllEntity();
            entity.setSentTo(personToNotify);
            entity.setCreatedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            entity.setRetryCount(0);
            entity.setApproverActionWfIds("");
            entity.setApproverNoActionWfIds("");
            entity.setCapturerWfIds(capturerNotificationWorkflows);
            entity.setMailModel(convertObjectToByteArray(mailModel));
            entity.setMailProcess("DailyCapturerSlaReminder");

            if(mailResponseModel.isSuccess()){
                java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
                entity.setCompletedOn(now);
                entity.setLastTriedMessage("Mail sent successfully");
                entity.setComplete(true);
            }else{
                entity.setLastTriedMessage(mailResponseModel.getReason());
            }

            approvalForumDataRepository.persistDailyMailRequest(entity);

        }

        return new MailResponseModel(true);

    }

    private String buildApplicationHtmlDetails(WorkflowMailDTO mailDTO, String tableRowStyle) {
        String html = tableRowStyle;
        html = html.concat("<td>" + mailDTO.getDafLink() + "</td>");
        html = html.concat("<td>" + mailDTO.getCounterpartyName() + "</td>");
        html = html.concat("<td>" + mailDTO.getMandate() + "</td>");
        html = html.concat("<td>" + mailDTO.getCreatedDate() + "</td>");
        html = html.concat("<td>" + mailDTO.getAgingStatus() + "</td>");
        html = html.concat("<td>" + mailDTO.getSubmittedBy() + "</td>");
        html = html.concat("<td>" + mailDTO.getOutstanding() + "</td></tr>");
        return html;
    }

    private MailResponseModel sendDailyMail(MailModel mailModel){
        logger.info("sendDailyMail({})", mailModel.getTo());
        if(!Boolean.valueOf(afSettings.get("af.mail.enabled"))){
            mailModel.setTo(afSettings.get("af.mail.test.emails"));
        }
        return mailServiceClient.Send(afSettings.get("af.mailservice.token"), mailModel);
    }

    private List getNewListWithDetails(Object name) {
        List list = new ArrayList();
        list.add(name);
        return list;
    }

    private MailModel createMailModel(String to, String subject){
        logger.info("***creatMailModel: email to:{}; subject:{}", to, subject);
        MailModel mailModel = new MailModel();
        mailModel.setFrom("approvalForum@rmb.co.za");
        List<String> toList = new ArrayList<>(Arrays.asList(to.split(",")));
        mailModel.setTo(toList.stream().distinct().collect(Collectors.joining(",")));
        mailModel.setSubject(subject);
        return mailModel;
    }

    private String getCounterPartyName(List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities){
        if(counterpartyDetailEntities == null || counterpartyDetailEntities.isEmpty()){
            return "Unknown";
        }
        return counterpartyDetailEntities.iterator().next().getCounterpartyName();
    }

    private String getCreditAnalystEmail(List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities){
        if(!"PROD".equals(afSettings.get("af.environment"))) return afSettings.get("af.mailservice.test.dl");
        if(counterpartyDetailEntities == null || counterpartyDetailEntities.isEmpty()){
            return "Unknown";
        }
        return counterpartyDetailEntities.iterator().next().getWfadApprovalForumWorkflowApplicationDataByWfadId().getCreditAnalystEmail();
    }

    private String getCreditAnalystName(List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities){
        if(counterpartyDetailEntities == null || counterpartyDetailEntities.isEmpty()){
            return "Credit Analyst";
        }
        String name =counterpartyDetailEntities.iterator().next().getWfadApprovalForumWorkflowApplicationDataByWfadId().getCreditAnalyst();
        if(name == null || "".equals(name.trim())) return "Credit Analyst";
        return name;
    }

    private String getMandate(List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities){
        if(counterpartyDetailEntities == null || counterpartyDetailEntities.isEmpty()){
            return "Unknown";
        }
        return counterpartyDetailEntities.iterator().next().getWfadApprovalForumWorkflowApplicationDataByWfadId().getMeetingName();
    }

    @SuppressWarnings("Duplicates")
    private String getMailTo(ApprovalForumWfWorkflowEntity entity){
        String mailTo = "";
        List<ApprovalForumWfaWorkflowApprovalsEntity> emails = new ArrayList<>();
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowId(entity.getWfId());

        if(approvalsEntities == null || approvalsEntities.isEmpty()) return "";

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<Integer> listManagementIds = new ArrayList<>();
        quorumDataEntities.forEach(quorumDataEntity -> {
            if(!listManagementIds.contains(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())){
                listManagementIds.add(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId());
            }
        });

        if(entity.isSpecific() || entity.isConfidential()){
            approvalsEntities.stream().filter(approver -> approver.isSpecificApprover() || approver.isAdditionalApprover())
                    .collect(Collectors.toCollection(() -> emails));
        }else{
            approvalsEntities.stream().filter(approver -> approver.isAdditionalApprover() ||
                                                          listManagementIds.contains(approver.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId()))
                    .collect(Collectors.toCollection(() -> emails));
        }
        for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: emails){
            mailTo = mailTo + approvalsEntity.getEmail() + ",";
        }
        return mailTo.substring(0, mailTo.length()-1);
    }

    private String getChairpersonMailTo(ApprovalForumWfWorkflowEntity entity, List<ListManagementList> chairpersonLists){
        String mailTo = "";
        List<ApprovalForumWfaWorkflowApprovalsEntity> emails = new ArrayList<>();
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowId(entity.getWfId());

        if(approvalsEntities == null || approvalsEntities.isEmpty()) return "";

        List<Integer> listManagementIds = new ArrayList<>();

        chairpersonLists.forEach(chairpersonList -> {
            if(!listManagementIds.contains(chairpersonList.getId())){
                listManagementIds.add(chairpersonList.getId());
            }
        });

        if(entity.isChairpersonApprovalRequired()){
            approvalsEntities.stream().filter(approver -> listManagementIds.contains(approver.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId()))
                    .collect(Collectors.toCollection(() -> emails));

            for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: emails){
                mailTo = mailTo + approvalsEntity.getEmail() + ",";
            }
        }

        return mailTo.substring(0, mailTo.length()-1);
    }

    @SuppressWarnings("Duplicates")
    private List<String> getApprovals(ApprovalForumWfWorkflowEntity entity, boolean approved){
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvals = new ArrayList<>();
        List<ApprovalForumWfaWorkflowApprovalsEntity> approvalsEntities = approvalForumDataRepository.getApprovalsForWorkflowId(entity.getWfId());

        if(approvalsEntities == null || approvalsEntities.isEmpty()) return new ArrayList<>();

        List<ApprovalForumWfqdWorkflowQuorumDataEntity> quorumDataEntities = approvalForumDataRepository
                .getApprovalForumQuorumDataForQuorumId(
                        approvalForumDataRepository.getSelectedWorkflowQuorumByWorkflowId(entity.getWfId()).getWfqId());

        List<Integer> listManagementIds = new ArrayList<>();
        quorumDataEntities.forEach(quorumDataEntity -> {
            if(!listManagementIds.contains(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId())){
                listManagementIds.add(quorumDataEntity.getAflmmApprovalForumListManagementMappingByAflmmId().getAflmmId());
            }
        });

        if(approved) {
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
        }else{
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
        List<String> names = new ArrayList<>();
        for(ApprovalForumWfaWorkflowApprovalsEntity approvalsEntity: approvals){
            if(names.indexOf(approvalsEntity.getName().trim() + " " + approvalsEntity.getSurname().trim()) < 0) {
                names.add(approvalsEntity.getName().trim() + " " + approvalsEntity.getSurname().trim());
            }
        }
        return names;
    }

    private String getCompleteMailTo(ApprovalForumWfWorkflowEntity entity, String status){
        String mailTo = getSubmittedByEmail(entity.getSubmittedBy());
        if(entity.getWfcdWorkflowCounterpartyDetailByWfId() != null &&
            !entity.getWfcdWorkflowCounterpartyDetailByWfId().isEmpty()){
            if(!"PROD".equals(afSettings.get("af.mail_environment"))){
                mailTo = mailTo +
                        afSettings.get("af.mailservice.test.dl") + ",";
                mailTo = mailTo +
                        afSettings.get("af.mailservice.reviewanalyst.test") + ",";
            }else {
                ApprovalForumWfcdWorkflowCounterpartyDetailEntity counterpartyDetailEntity =
                        entity.getWfcdWorkflowCounterpartyDetailByWfId().iterator().next();
                if (counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId() != null &&
                        counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId().getReviewAnalystEmail() != null &&
                        !counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId().getReviewAnalystEmail().trim().isEmpty()) {
                    mailTo = mailTo +
                            counterpartyDetailEntity.getWfadApprovalForumWorkflowApplicationDataByWfadId().getReviewAnalystEmail() + ",";
                }
            }
        }

        mailTo = mailTo + getMailTo(entity) + "," + getSubmittedByEmail(entity.getSubmittedBy()) + ",";
        
        if(!ApprovalDecision.ESCALATED.value().equalsIgnoreCase(status)) {
            mailTo = mailTo + afSettings.get("af.corp.credit.secretary.mail") + ",";
            mailTo = mailTo + afSettings.get("af.corp.credit.specialist.mail") + ",";
        }
        return mailTo.substring(0, mailTo.length()-1);
    }

    @SuppressWarnings("Duplicates")
    private String getEmailEnding(int workflowId, String counterpartyName, String decision, String mandate){
        String htmlStr = "";
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("<p>Counterparty Name: " + counterpartyName + "</p>");
        if(decision != null && !decision.trim().isEmpty())
        htmlStr = htmlStr.concat("<p><b>Credit Decision: " + decision + "</b></p>");
        htmlStr = htmlStr.concat("<p>Mandate: " + mandate + "</p>");
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");
        return htmlStr;
    }

    @SuppressWarnings("Duplicates")
    private String getEmailEndingSubmission(int workflowId, String counterpartyName, String decision, String mandate){
        String htmlStr = "";
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("<p>Counterparty Name: " + counterpartyName + "</p>");
        if(decision != null && !decision.trim().isEmpty())
        htmlStr = htmlStr.concat("<p><b>Credit Decision: " + decision + "</b></p>");
        htmlStr = htmlStr.concat("<p>Mandate: " + mandate + "</p>");
        String dafLink = "";
        if(afSettings.get("af.mail.base").endsWith("/")){
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "capture/" + workflowId + ">Click Here</a>";
        }else{
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "/capture/" + workflowId + ">Click Here</a>";
        }
        htmlStr = htmlStr.concat("<p>DAF Link: " + dafLink + "</p>");
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");
        return htmlStr;
    }

    @SuppressWarnings("Duplicates")
    private String getEmailEndingForApproval(int workflowId, String counterpartyName, String decision, String mandate){
        logger.info("getEmailEndingForApproval workflowId: {}, counterpartyName: {}, decision: {}, mandate: {}",
                workflowId, counterpartyName, decision, mandate);
        String htmlStr = "";
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("<p>Counterparty Name: " + counterpartyName + "</p>");
        if(decision != null && !decision.trim().isEmpty())
            htmlStr = htmlStr.concat("<p><b>Credit Decision: " + decision + "</b></p>");
        htmlStr = htmlStr.concat("<p>Mandate: " + mandate + "</p>");
        String dafLink = "";
        if(afSettings.get("af.mail.base").endsWith("/")){
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "approve/" + workflowId + ">Click Here</a>";
        }else{
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "/approve/" + workflowId + ">Click Here</a>";
        }
        htmlStr = htmlStr.concat("<p>DAF Link: " + dafLink + "</p>");
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");
        return htmlStr;
    }

    private String getEmailEndingForApprovalOffline(int workflowId, String counterpartyName, String decision, String mandate){
        logger.info("getEmailEndingForApprovalOffline workflowId: {}, counterpartyName: {}, decision: {}, mandate: {}",
                workflowId, counterpartyName, decision, mandate);
        String htmlStr = "";
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("<p>Counterparty Name: " + counterpartyName + "</p>");
        if(decision != null && !decision.trim().isEmpty())
            htmlStr = htmlStr.concat("<p><b>Credit Decision: " + decision + "</b></p>");
        htmlStr = htmlStr.concat("<p>Mandate: " + mandate + "</p>");

        String dafLink = "";
        String email = afSettings.get("af.mailservice.approval.mailto");
        String assignToMeLink = String.format("<a href=\"mailto:%s?subject=Assign to me workflowId=%s\">Assign to me</a>",
                email, workflowId);

        if(afSettings.get("af.mail.base").endsWith("/")){
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "approve/" + workflowId + ">Click Here</a>";
        }else{
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "/approve/" + workflowId + ">Click Here</a>";
        }
        htmlStr = htmlStr.concat("<p>DAF Link: " + dafLink + "</p>");
        htmlStr = htmlStr.concat("<p>" + assignToMeLink + "</p>");
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");
        return htmlStr;
    }

    @SuppressWarnings("Duplicates")
    private String getEmailEndingForChair(int workflowId, String counterpartyName, String decision, String mandate){
        String htmlStr = "";
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("<p>Counterparty Name: " + counterpartyName + "</p>");
        if(decision != null && !decision.trim().isEmpty())
            htmlStr = htmlStr.concat("<p><b>Credit Decision: " + decision + "</b></p>");
        htmlStr = htmlStr.concat("<p>Mandate: " + mandate + "</p>");
        String dafLink = "";
        if(afSettings.get("af.mail.base").endsWith("/")){
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "chair/" + workflowId + ">Click Here</a>";
        }else{
            dafLink = "<a href=" + afSettings.get("af.mail.base") + "/chair/" + workflowId + ">Click Here</a>";
        }
        htmlStr = htmlStr.concat("<p>DAF Link: " + dafLink + "</p>");
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");
        return htmlStr;
    }

    @SuppressWarnings("Duplicates")
    private String getEmailEndingDocStash(int workflowId, String counterpartyName, String decision, String mandate, int docstashId){
        String htmlStr = "";
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowId + "</p>");
        htmlStr = htmlStr.concat("<p>Counterparty Name: " + counterpartyName + "</p>");
        if(decision != null && !decision.trim().isEmpty())
        htmlStr = htmlStr.concat("<p><b>Credit Decision: " + decision + "</b></p>");
        htmlStr = htmlStr.concat("<p>Mandate: " + mandate + "</p>");
        
        if (docstashId > 0) {
            String docstashLink = "";
            String hash = hashids.encode(docstashId);
            if(afSettings.get("af.docstash.base").endsWith("/")){
                docstashLink = "<a href=" + afSettings.get("af.docstash.base") + hash + ">Click Here</a>";
            }else{
                docstashLink = "<a href=" + afSettings.get("af.docstash.base") + "/" + hash + ">Click Here</a>";
            }
            htmlStr = htmlStr.concat("<p>DocStash Link: " + docstashLink + "</p>");
        }
        
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");
        return htmlStr;
    }

    private String getSubmittedByEmail(String userId){
        if(userId == null) return "";
        UserDetails userDetails = approvalForumAuthService.getUserDetailsByAliasIgnoreCase(userId);
        if(userDetails != null) return userDetails.getEmail() + ",";
        return "";
    }

    private int getFirstEscalationHours(){
        return Integer.parseInt(afSettings.get("af.secondescalation.hours"))-Integer.parseInt(afSettings.get("af.firstescalation.hours"));
    }

    @Override
    public void sendUnsentMails(){
        try {
            List<ApprovalForumMrMailRequestsEntity> mailRequestsEntities = approvalForumDataRepository.getUnsentMails();
            mailRequestsEntities.forEach(entity -> {
                logger.info("Sending mail for workflowId " + entity.getWfId());
                java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
                entity.setRetryCount(entity.getRetryCount()+1);
                entity.setLastTriedOn(now);
                MailResponseModel responseModel = sendMail((MailModel)convertByteArrayToObject(entity.getMailModel()), entity.getWfId());
                if(responseModel.isSuccess()){
                    entity.setCompletedOn(now);
                    entity.setLastTriedMessage("Mail sent successfully");
                    entity.setComplete(true);
                }else{
                    entity.setLastTriedMessage(responseModel.getReason());
                }
                approvalForumDataRepository.updateMailRequest(entity);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resendDailyMails(){
        try {
            List<ApprovalForumMrdaMailRequestsDailyAllEntity> dailyMailRequestsEntities = approvalForumDataRepository.getUnsentDailyMails();
            dailyMailRequestsEntities.forEach(entity -> {
                String action = (entity.getApproverActionWfIds() != null) ? "Action WF_IDs " + entity.getApproverActionWfIds() + " " : "";
                String noAction = (entity.getApproverNoActionWfIds() != null) ? "No Action WF_IDs " + entity.getApproverNoActionWfIds() + " " : "";
                String capturer = (entity.getCapturerWfIds() != null) ? "Capturer WF_IDs " + entity.getCapturerWfIds() : "";
                logger.info("Resending daily mail for " + entity.getSentTo() + " " + action + noAction + capturer);
                java.sql.Timestamp now = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
                entity.setRetryCount(entity.getRetryCount() + 1);
                entity.setLastTriedOn(now);
                MailResponseModel responseModel = sendDailyMail((MailModel) convertByteArrayToObject(entity.getMailModel()));
                if (responseModel.isSuccess()) {
                    entity.setCompletedOn(now);
                    entity.setLastTriedMessage("Mail resent successfully");
                    entity.setComplete(true);
                } else {
                    entity.setLastTriedMessage(responseModel.getReason());
                }
                approvalForumDataRepository.updateDailyMailRequest(entity);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MailResponseModel sendMail(MailModel mailModel, int workflowId){
        if(!Boolean.valueOf(afSettings.get("af.mail.enabled"))){
            mailModel.setTo(afSettings.get("af.mail.test.emails"));
        }
        if(Boolean.valueOf(afSettings.get("af.mail.attachment.zip"))) return sendMailZip(mailModel, workflowId);
        return sendMailNormal(mailModel, workflowId);
    }

    private MailResponseModel sendMailNormal(MailModel mailModel, int workflowId){
        logger.info("sendMail({})", workflowId);
        logger.info("****sendMailNormal --> mailModel: To({}), From({}), Subject({})", mailModel.getTo(), mailModel.getFrom(), mailModel.getSubject());
        if(!attachmentsExceedLimit(workflowId)) {
            List<AttachmentModel> attachments = getAttachments(workflowId);
            if (!attachments.isEmpty()) {
                logger.info("Found {} attachments to add", attachments.size());
                mailModel.setAttachments(attachments);
            }
        }
        if (StringUtils.isEmpty(mailModel.getFrom()) || StringUtils.isEmpty(mailModel.getSubject())
                || StringUtils.isEmpty(mailModel.getTo())) {
            return null;
        } //added as it fails on the mailserver when user tries to send an email and they are not part of the mailing list

        logger.info("about to send email to mailserver for workflowId({})", workflowId);
        return mailServiceClient.Send(afSettings.get("af.mailservice.token"), mailModel);
    }

    private List<AttachmentModel> getAttachments(int workflowId){
        List<AttachmentModel> attachments  = new ArrayList<>();
        WorkflowDocumentListResponse documentListResponse = approvalForumDocumentsService.getWorkflowDocuments(Integer.valueOf(workflowId).toString());
        if(documentListResponse == null || documentListResponse.getDocuments() == null || documentListResponse.getDocuments().isEmpty()){
            return attachments;
        }
        ApplicationDocumentModel documentModel;
        for(ApplicationDocumentListModel listModel: documentListResponse.getDocuments()){
            documentModel = approvalForumDocumentsService.downloadDocument(Integer.valueOf(listModel.getAsdID()).toString());
            AttachmentModel model = new AttachmentModel();
            model.setData(documentModel.getFileData());
            model.setFilename(documentModel.getFileName());
            attachments.add(model);
        }
        return attachments;
    }

    private MailResponseModel sendMailZip(MailModel mailModel, int workflowId){
        logger.info("sendMailZip({})", workflowId);
        if(!attachmentsExceedLimit(workflowId)) {
            File zipFile = zipAttachments(workflowId);
            if (zipFile != null) {
                AttachmentModel model = new AttachmentModel();
                try {
                    model.setData(Files.readAllBytes(zipFile.toPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                model.setFilename(zipFile.getName());
                mailModel.setAttachments(Collections.singletonList(model));
            }
        }
        return mailServiceClient.Send(afSettings.get("af.mailservice.token"), mailModel);
    }

    private File zipAttachments(int workflowId){
        File zipfile = new File("Documents.zip");
        byte[] buffer = new byte[1024];
        List<String> fileNames = new ArrayList<>();
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipfile));

            WorkflowDocumentListResponse documentListResponse = approvalForumDocumentsService.getWorkflowDocuments(Integer.valueOf(workflowId).toString());
            if(documentListResponse == null || documentListResponse.getDocuments() == null || documentListResponse.getDocuments().isEmpty()){
                return null;
            }
            ApplicationDocumentModel documentModel;
            for(ApplicationDocumentListModel listModel: documentListResponse.getDocuments()){
                documentModel = approvalForumDocumentsService.downloadDocument(Integer.valueOf(listModel.getAsdID()).toString());
                if(fileNames.indexOf(documentModel.getFileName()) > 0) continue;
                fileNames.add(documentModel.getFileName());
                InputStream fis = new ByteArrayInputStream(documentModel.getFileData());
                zos.putNextEntry(new ZipEntry(documentModel.getFileName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return zipfile;
    }

    private boolean attachmentsExceedLimit(int workflowId){
        WorkflowDocumentListResponse documentListResponse = approvalForumDocumentsService.getWorkflowDocuments(Integer.valueOf(workflowId).toString());
        if(documentListResponse == null || documentListResponse.getDocuments() == null || documentListResponse.getDocuments().isEmpty()){
            return false;
        }
        long size = 0L;
        for(ApplicationDocumentListModel listModel: documentListResponse.getDocuments()){
            size+=listModel.getFileSize();
        }
        return size > Long.parseLong(afSettings.get("af.mail.attachment.limit"));
    }

    private void persistMailRequest(MailModel mailModel, int workflowId, String mailProcess){
        logger.info("persistMailRequest(mailModel: (to: {}, from: {}, subject: {}), workflowId: {}, mailProcess: {})",
                mailModel.getTo(), mailModel.getFrom(), mailModel.getSubject(), workflowId, mailProcess);
        ApprovalForumMrMailRequestsEntity entity = new ApprovalForumMrMailRequestsEntity();
        entity.setCreatedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        entity.setComplete(false);
        entity.setRetryCount(0);
        entity.setWfId(workflowId);
        entity.setMailModel(convertObjectToByteArray(mailModel));
        entity.setMailProcess(mailProcess);
        approvalForumDataRepository.persistMailRequest(entity);
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

    public MailResponseModel sendAssignToMeEmailNotification(int workflowId, String username, String message) throws ResourceException {
        logger.info("***sendAssignToMeEmailNotification(message: {}, workflow: {}, username: {})", message, workflowId, username);

        List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> counterpartyDetailEntities = getCounterpartyDetailsEntity(workflowId);

        String counterpartyName = getCounterPartyName(counterpartyDetailEntities);
        MailModel mailModel;

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear Approver,</p>");


        if (message.equalsIgnoreCase(AssignedTypes.ASSIGNED.name())) {
            logger.info("sendAssignToMeEmailNotification: Assigned to approver");
            mailModel = createMailModel(username, "DAF assigned successfully for '" + counterpartyName + "'" );
            mailModel.setBody(htmlStr.concat(getEmailEndingForSuccessfulApproval(workflowId, counterpartyName,
                    getMandate(counterpartyDetailEntities), username)));

            logger.info("****Assign to approver mailModel: To({}), From({}), Subject({})", mailModel.getTo(), mailModel.getFrom(), mailModel.getSubject());

            persistMailRequest(mailModel, workflowId, "AssignToMeNotificationSubmit");
            return new MailResponseModel(true);

        } else if (message.equalsIgnoreCase(AssignedTypes.QUORUM_REACHED.name()) ||
                message.equalsIgnoreCase(AssignedTypes.QUORUM_REACHED_AND_ASSIGNED.name())) {
            logger.info("sendAssignToMeEmailNotification: Quorum reached");
            mailModel = createMailModel(username, "DAF Quorum reached for '" + counterpartyName + "'");
            htmlStr = htmlStr.concat("<p>The application cannot be assigned. Quorum reached</p>");
            mailModel.setBody(htmlStr.concat(getUnsuccessfulQuorumReached(workflowId)));

            logger.info("****Quorum reached mailModel: To({}), From({}), Subject({})", mailModel.getTo(), mailModel.getFrom(), mailModel.getSubject());

            persistMailRequest(mailModel, workflowId, "QuorumReachedNotificationSubmit");
            return new MailResponseModel(true);
        } else if (message.equalsIgnoreCase(AssignedTypes.CAN_NOT_BE_ASSIGNED.name())) {
            logger.info("sendAssignToMeEmailNotification: Cannot be assigned workflowId {}", workflowId);

            mailModel = createMailModel(username, "URGENT - DAF request for '" + counterpartyName + "' could NOT be processed");
            htmlStr = htmlStr.concat("<p>The application cannot be assigned. Quorum reached</p>");
            mailModel.setBody(htmlStr.concat(getUnsuccessfulQuorumReached(workflowId)));

            logger.info("****Quorum reached mailModel: To({}), From({}), Subject({})", mailModel.getTo(), mailModel.getFrom(), mailModel.getSubject());

            persistMailRequest(mailModel, workflowId, "QuorumReachedNotificationSubmit");
            return new MailResponseModel(true);
        }

        return null;

    }

    public MailResponseModel sendAssignToMeEmailNotificationFailure(int workflowId, String username, String message,
                                                                    String workflowIdStr) {
        logger.info("***sendAssignToMeEmailNotificationFailure(message: {}, workflow: {}, username: {}, workflowIdStr: {})",
                message, workflowId, username, workflowIdStr);

        MailModel mailModel;

        String htmlStr = "<html>";
        htmlStr = htmlStr.concat(bodyStyle);
        htmlStr = htmlStr.concat("<p>Dear Approver,</p>");

        logger.info("sendAssignToMeEmailNotificationFailure: Cannot be assigned workflowId {}", workflowIdStr);

        mailModel = createMailModel(username, "URGENT - DAF request for this workflow " + workflowIdStr + " could NOT be processed");
        htmlStr = htmlStr.concat("<p>Please note that the Credit Application could NOT be processed.</p>");
        htmlStr = htmlStr.concat("<p>Application ID: " + workflowIdStr + "</p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");

        mailModel.setBody(htmlStr);

        logger.info("****Workflow cannot be assigned mailModel: To({}), From({}), Subject({})", mailModel.getTo(), mailModel.getFrom(), mailModel.getSubject());

//        persistMailRequest(mailModel, workflowId, "InvalidWorkflowIdEmailNotificationSubmit");
        sendMailForFailedWorkflowId(mailModel);
        return new MailResponseModel(true);

    }

    private void sendMailForFailedWorkflowId(MailModel mailModel) {
        logger.info("***sendMailForFailedWorkflowId");

//        mailModel.setFrom("approvalForum@rmb.co.za");

//        mailModel.setTo(username);
        mailModel.setCc(afSettings.get("af.mailservice.test.dl"));

        mailServiceClient.Send(afSettings.get("af.mailservice.token"), mailModel);
    }

    private String getEmailEndingForSuccessfulApproval(int workflowId, String counterpartyName, String mandate, String username) throws ResourceException {
        logger.info("***starting email approval");

        StringBuilder htmlStr = new StringBuilder();
        htmlStr.append("<p>Please note that a Credit Application for '").append(counterpartyName).append("' is awaiting your review and approval.</p>");
        htmlStr.append("<p>Application ID: ").append(workflowId).append("</p>");
        htmlStr.append("<p>Counterparty Name: ").append(counterpartyName).append("</p>");
        htmlStr.append("<p>Mandate: ").append(mandate).append("</p>");

        String email = afSettings.get("af.mailservice.approval.mailto");
        logger.info("offline approval email:[{}]", email);

        try {
            String approveBody = buildApprovalEmail(workflowId, username);
            String conditionallyBody = buildConditionallyApproveEmail(workflowId, username);
            String referBody = buildReferBackEmail(workflowId, username);

            // Encode once and replace '+' with '%20'
            String approveEncoded = URLEncoder.encode(approveBody, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String conditionallyEncoded = URLEncoder.encode(conditionallyBody, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String referEncoded = URLEncoder.encode(referBody, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            String approve = String.format("<a href=\"mailto:%s?subject=RE: Digital Email Approval %s workflowId=%s&body=%s\">Approve</a>",
                    email, counterpartyName, workflowId, approveEncoded);
            String conditionally = String.format("<a href=\"mailto:%s?subject=RE: Digital Email Conditional Approval %s workflowId=%s&body=%s\">Conditionally Approve</a>",
                    email, counterpartyName, workflowId, conditionallyEncoded);
            String refer = String.format("<a href=\"mailto:%s?subject=RE: Digital Email Refer Back %s workflowId=%s&body=%s\">Refer Back</a>",
                    email, counterpartyName, workflowId, referEncoded);

            htmlStr.append("<table style=\"width:100%\">\n<tr style=\"background-color:#D5D1D1\">\n");
            htmlStr.append("<td>").append(approve).append("</td>\n");
            htmlStr.append("<td>").append(conditionally).append("</td>\n");
            htmlStr.append("<td>").append(refer).append("</td>\n");
            htmlStr.append("</tr>\n</table>\n");

            String dafLink = "";
            if(afSettings.get("af.mail.base").endsWith("/")){
                dafLink = "<a href=" + afSettings.get("af.mail.base") + "approve/" + workflowId + ">Click Here</a>";
            }else{
                dafLink = "<a href=" + afSettings.get("af.mail.base") + "/approve/" + workflowId + ">Click Here</a>";
            }

            htmlStr = htmlStr.append("<p>DAF Link: " + dafLink + "</p>");
            if(attachmentsExceedLimit(workflowId)) {
                htmlStr = htmlStr.append("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
            }

            htmlStr.append("<p>\"Credit Says Yes in Seconds\"</p>");
            htmlStr.append("<p>- Digital Approval Forum -</p>");
            htmlStr.append("</html>");

        } catch (UnsupportedEncodingException e) {
            logger.error("getEmailEndingForApprovalOffline failed: {}", e.getMessage());
            throw new ResourceException(e.getMessage());
        }

        return htmlStr.toString();
    }


    private String getUnsuccessfulQuorumReached(int workflowId) {
        String htmlStr = "";
        if(attachmentsExceedLimit(workflowId))
            htmlStr = htmlStr.concat("<p><b>Note: Attachments could not be sent as it exceeds the mail size limit.</b></p>");
        htmlStr = htmlStr.concat("<p>\"Credit Says Yes in Seconds\"</p>");
        htmlStr = htmlStr.concat("<p>- Digital Approval Forum -</p>");
        htmlStr = htmlStr.concat("</html>");

        return htmlStr;
    }

    private String buildApprovalEmail(int workflowId, String username) {
        String counterpartyName = getCounterPartyName(getCounterpartyDetailsEntity(workflowId));
        return "Dear Digital Approval Forum\n\n" + getDigitalApproval(workflowId, counterpartyName, "Approval");
    }

    private String buildConditionallyApproveEmail(int workflowId, String username) {
        String counterpartyName = getCounterPartyName(getCounterpartyDetailsEntity(workflowId));
        return "Dear Digital Approval Forum\n\n" + getDigitalApproval(workflowId, counterpartyName, "Conditional Approval");
    }

    private String buildReferBackEmail(int workflowId, String username) {
        String counterpartyName = getCounterPartyName(getCounterpartyDetailsEntity(workflowId));
        return "Dear Digital Approval Forum\n\n" + getDigitalApproval(workflowId, counterpartyName, "Refer Back");
    }

    private String getDigitalApproval(int workflowId, String counterpartyName, String decision) {
        return buildEmailApprovalBody(workflowId, counterpartyName, decision) +
                "\"Credit Says Yes in Seconds\"\n" +
                "- Digital Approval Forum -\n";
    }

    private String buildEmailApprovalBody(int workflowId, String counterpartyName, String decision) {
        StringBuilder sb = new StringBuilder();
        sb.append("Decision:\n");
        sb.append("The application ").append(workflowId).append(" ").append(counterpartyName).append(" is ").append(decision).append("\n\n");

        if ("Conditional Approval".equalsIgnoreCase(decision)) {
            sb.append("Conditions:\n");
            sb.append("[Please enter conditions here]\n\n");
        } else {
            sb.append("Comments:\n");
            sb.append("[Please enter comments here]\n\n");
        }

        return sb.toString();
    }

    private List<ApprovalForumWfcdWorkflowCounterpartyDetailEntity> getCounterpartyDetailsEntity (int workflowId) {
        return approvalForumDataRepository.getCounterpartyDetailForWorkflowId(workflowId);
    }

}
