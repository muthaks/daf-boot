package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import za.co.rmb.approvalforum.data.ApprovalForumAuthRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAduActiveDirectoryUserEntity;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.ApprovalForumEmailReceived;
import za.co.rmb.approvalforum.service.ApprovalForumMailService;
import za.co.rmb.approvalforum.service.ApprovalForumService;
import za.co.rmb.approvalforum.service.exception.ResourceException;

import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Slf4j
public class ApprovalForumEmailReceivedImpl implements ApprovalForumEmailReceived {

    private final ApprovalForumService approvalForumService;

    private final ApprovalForumAuthRepository approvalForumAuthRepository;

    private final ApprovalForumMailService approvalForumMailService;

    private final Map<String, String> afSettings;

    private static final String LUMOS_BASE_URL = "af.lumos.mailservice.base";

    private static final String ASSIGN = "assign";
    private static final String APPROVE = "approve";
    private static final String REFER_BACK = "referback";
    private static final String CONDITIONALLY_APPROVE = "conditionallyapprove";

    @Inject
    public ApprovalForumEmailReceivedImpl(ApprovalForumService approvalForumService,
                                          @Named("af_settings") Map<String, String> afSettings,
                                          ApprovalForumAuthRepository approvalForumAuthRepository, ApprovalForumMailService approvalForumMailService) {
        this.approvalForumService = approvalForumService;
        this.afSettings = afSettings;
        this.approvalForumAuthRepository = approvalForumAuthRepository;
        this.approvalForumMailService = approvalForumMailService;
    }

    @Override
    public void processIncomingEmails() {
        log.info("Fetching email received");
        RestTemplate restTemplate = new RestTemplate();
        String lumosMailServiceBase = afSettings.get(LUMOS_BASE_URL);
        final String FETCH_UNREAD_EMAILS_ENDPOINT =  "/mailservice/api/v1/receivedEmail/fetchUnreadEmails";
        String fromEmail = afSettings.get("af.mailservice.approval.mailto");
        String limit = afSettings.get("af.lumos.mailservice.read.limit");

        HttpEntity<EmailReceived[]> entity = new HttpEntity<>(getHttpHeaders());

        String url = lumosMailServiceBase +FETCH_UNREAD_EMAILS_ENDPOINT+ "?email="+fromEmail+"&limit="+limit;
        log.info("url:{}",url);

       //getting responseEntity and retrieve incoming emails
        getResponseEntity(restTemplate, url, entity);

        log.info("Done processing incoming emails...");
    }

    private void getResponseEntity(RestTemplate restTemplate, String url, HttpEntity<EmailReceived[]> entity) {
        ResponseEntity<EmailReceived[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, EmailReceived[].class);
        log.info("***getResponseEntity - response code:{}",responseEntity.getStatusCode());
        if(responseEntity.getStatusCode().is2xxSuccessful()) {
            EmailReceived[] emails = responseEntity.getBody();
            log.info("***Emails Found {}", (Object[]) emails);
            if(emails != null) {
                for(EmailReceived email:emails) {
                    log.info("email:{}", email);
                    if(StringUtils.isNotEmpty(email.getSubject()) && StringUtils.isNotEmpty(email.getFrom())) {
                        int workflowId = extractWorkflowId(email.getSubject());

                        String workFlowIdStr = null;
                        if (workflowId < 0) {
                            workFlowIdStr = extractWorkflowIdStr(email.getSubject());
                        }
                        ApprovalForumAduActiveDirectoryUserEntity activeDirectoryUser = approvalForumAuthRepository.getActiveDirectoryUserByMailIgnoreCase(email.getFrom());
                        log.info("***activeDirectoryUser:{}", activeDirectoryUser.getMail());
                        if(activeDirectoryUser!=null) {
                            String username = activeDirectoryUser.getAlias();
                            log.info("workflowId: {}, username: {}", workflowId, username);
                            //determine which action to take assign, approve, conditional approve, refer back
                            String action = determineAction(email.getSubject());
                            log.info("action: {}, for workflowId: {}", action, workflowId);
                            boolean success = routeAction(action, workflowId, username, email, workFlowIdStr);
                            log.info("success({})", success);

                            if (workflowId < 0 && !success) {
                                log.info("*** workflowId is invalid and routing success if false");
                                //markAsRead
                                markRead(email.getEmailId());
                            }

                            if (success) {
                                markRead(email.getEmailId());
                                //send emails for approval email
                                if (!ASSIGN.equalsIgnoreCase(action)) {
                                    approvalForumMailService.sendRequestCompleteMail(workflowId);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String token = afSettings.get("af.lumos.mailservice.token");
        headers.set(HttpHeaders.AUTHORIZATION,"Bearer "+token);

        return headers;
    }

    private String determineAction(String subject) {
        log.info("determineAction email subject: {}", subject);
        String content = (subject).toLowerCase();

        if (content.contains(ASSIGN)) return ASSIGN;
        if (content.contains("approval")) return APPROVE;
        if (content.contains("refer back")) return REFER_BACK;
        if (content.contains("conditional approval")) return CONDITIONALLY_APPROVE;

        return "unknown";
    }

    private boolean routeAction(String action, int workflowId, String username, EmailReceived email, String workflowIdStr) {
        String emailBody = email.getBody();
        try {
            switch (action.toLowerCase()) {
                case ASSIGN:
                    return handleAssign(workflowId, username, workflowIdStr);

                case APPROVE:
                    return submitDecision(workflowId, username, ApprovalDecision.APPROVED, emailBody);

                case REFER_BACK:
                    return submitDecision(workflowId, username, ApprovalDecision.REFERRED_BACK, emailBody);

                case CONDITIONALLY_APPROVE:
                    return submitDecision(workflowId, username, ApprovalDecision.CONDITIONALLY_APPROVED, emailBody);

                default:
                    log.warn("Unknown action: {}", action);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error routing action ({}) for workflowId ({}): {}", action, workflowId, e.getMessage());
            return false;
        }
    }

    private boolean handleAssign(int workflowId, String username, String workflowIdStr) throws ResourceException, UnsupportedEncodingException {
        log.info("Handling assign for workflowId {}, username {}, workflowIdStr: {}", workflowId, username, workflowIdStr);
        CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
        AssignResponse response = new AssignResponse();
        //handling -1 workflowId
        if (workflowId < 0) {
            log.info("***canNotBeAssigned workflowId({}) is invalid", workflowId);
            responseDTO.setResponse(response);
            responseDTO.getResponse().setResponse(AssignedTypes.CAN_NOT_BE_ASSIGNED);
            approvalForumService.sendAssignToMeNofiticationFailure(workflowId, username, responseDTO, workflowIdStr);
            response = responseDTO.getResponse();

        } else {

            responseDTO.setResponse(approvalForumService.checkIfCanAssignWorkflowToMe(workflowId, username));

            log.info("***canBeAssignedResponse:{}", responseDTO.getResponse().getResponse());

            if (AssignedTypes.CAN_BE_ASSIGNED.equals(responseDTO.getResponse().getResponse())) {
                log.info("***assigning workflowId: {}", workflowId);
                responseDTO.setResponse(approvalForumService.assignWorkflow(workflowId, username));
            }

            log.info("Assign response: {}", responseDTO.getResponse().getResponse());

            if (AssignedTypes.QUORUM_REACHED.equals(responseDTO.getResponse().getResponse()) ||
                    AssignedTypes.QUORUM_REACHED_AND_ASSIGNED.equals(responseDTO.getResponse().getResponse()) ||
                    AssignedTypes.ASSIGNED.equals(responseDTO.getResponse().getResponse())) {
                response = approvalForumService.sendAssignToMeNofitication(workflowId, username, responseDTO); //.getResponse().getResponse());
            }
        }

        log.info("***after sendingAssignedToMeNotification response({})", response);

        return AssignedTypes.ASSIGNED.name()
                .equalsIgnoreCase(responseDTO.getResponse().getResponse().name());
    }

    private boolean submitDecision(int workflowId, String username, ApprovalDecision decision, String emailBody) {
        log.info("Handling submit decision {} for workflowId {}", decision, workflowId);

        SubmitDecisionRequestDTO dto = new SubmitDecisionRequestDTO();
        dto.setWorkflowId(workflowId);
        dto.setDecision(decision);
        dto.setConditions(extractCommentsConditions(emailBody, ("Conditionally Approved".equalsIgnoreCase(decision.value())) ? "Conditions:" : "Comments:"));
//        dto.setPolicyApprovalMandate(extractCommentsConditions(emailBody, "comments")); //TODO: to confirm if comment should be set here

        Response response = approvalForumService.submitDecision(workflowId, dto, false, username);
        SubmitDecisionResponseDTO responseDTO = (SubmitDecisionResponseDTO) response.getEntity();

        log.info("Decision response: {}", responseDTO.toString());
        return responseDTO.isSuccess();
    }

    private String extractCommentsConditions(String body, String value) {
        log.info("***extractCommentsConditions");
        if (body == null || body.trim().isEmpty()) {
            return "";
        }

        log.info("email body: {}", body);

        String[] lines = body.split("\\r?\\n");
        log.info("email body lines: {}", Arrays.toString(lines));

        List<String> extractedSections = new ArrayList<>();

        boolean collecting = false;
        StringBuilder currentSection = new StringBuilder();

        for (String line : lines) {
            log.info("***iterate through lines: {}", line);
            String plainText = line.replaceAll("<[^>]+>", "").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
            log.info("***plaintext: {}", plainText);
            String trimmedLine = plainText.trim();
            log.info("***trimmedLine: {}", trimmedLine);
//            String trimmedLine = line.trim();

            if (trimmedLine.equalsIgnoreCase("Credit Says Yes in Seconds")) {
                if (collecting && currentSection.length() > 0) {
                    extractedSections.add(currentSection.toString().trim());
                }
                break;
            }
            log.info("***after credit say yes...");

            if (trimmedLine.startsWith("Comments:") || trimmedLine.startsWith("Conditions:")) {
                log.info("***trimmedLine startsWith comments: or conditions:");
                if (collecting && currentSection.length() > 0) {
                    extractedSections.add(currentSection.toString().trim());
                    currentSection.setLength(0);
                }

                collecting = true;

                String label = trimmedLine.startsWith("Comments:") ? "Comments:" : "Conditions:";
                if (trimmedLine.length() > label.length()) {
                    currentSection.append(trimmedLine.substring(label.length()).trim()).append("\n");
                }
            } else if (collecting) {
                currentSection.append(trimmedLine).append("\n");
            }
        }

        // Add the last collected section if not already added
        log.info("***adding the last collected section");
        if (collecting && currentSection.length() > 0) {
            extractedSections.add(currentSection.toString().trim());
        }

        for (String section : extractedSections) {
            log.info("***Extracted Section: {}", section);
            return section;
        }

        return "";
    }

    @SuppressWarnings("Duplicates")
    private void markRead(int emailId){
        log.info("markRead({})", emailId);

        RestTemplate restTemplate = new RestTemplate();
        String lumosMailServiceBase = afSettings.get(LUMOS_BASE_URL);
        final String MARK_READ_ENDPOINT = "/mailservice/api/v1/receivedEmail/markAsRead";

        HttpEntity<String> entity = new HttpEntity<>(getHttpHeaders());

        String url = lumosMailServiceBase + MARK_READ_ENDPOINT + "?emailId="+emailId;

        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

        log.info("markRead response getStatusCode: {}", responseEntity.getStatusCode());
    }

    private int extractWorkflowId(String subject){
        int indexOf = subject.indexOf("workflowId=");
        log.info("indexOf:{}",indexOf);
        int workflowId =-1;
        try {
            String workflow = subject.substring(indexOf + 11);
            log.info("workflow: {}", workflow);
            workflowId = Integer.parseInt(workflow);
        }catch (Exception ex){
            log.error(ex.getMessage());
        }
        return workflowId;
    }

    private String extractWorkflowIdStr(String subject){
        int indexOf = subject.indexOf("workflowId=");
        log.info("indexOf:{}",indexOf);
        String workflowId = null;
        try {
            String workflow = subject.substring(indexOf + 11);
            log.info("workflow: {}", workflow);
            workflowId = workflow;
        }catch (Exception ex){
            log.error(ex.getMessage());
        }
        return workflowId;
    }

}
