package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import za.co.rmb.approvalforum.model.CounterpartyFullDetail;
import za.co.rmb.approvalforum.model.MeetingName;
import za.co.rmb.approvalforum.model.lumos.*;
import za.co.rmb.approvalforum.service.LumosRestService;
import za.co.rmb.approvalforum.service.exception.ResourceException;

import jakarta.xml.ws.Response;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Thakhani Tharage
 */
public class LumosRestServiceImpl implements LumosRestService {

    private static final Logger LOG = LoggerFactory.getLogger(LumosRestServiceImpl.class);

    private Map<String, String> afSettings;

    @Inject
    public LumosRestServiceImpl(@Named("af_settings") Map<String, String> afSettings) {
        this.afSettings = afSettings;
    }

    /**
     * @param workflowId : This is the equivalent of appID in CASDM, workflow is used to retrieve application full details from Lumos
     * @return CounterpartyFullDetail which gets to be validated is this is a DAF WIP
     */
    @Override
    public CounterpartyFullDetail getLumosWorkflow(int workflowId) {
        LOG.info("getLumosWorkflow : Retrieving lumos daf workflow via service ...");
        try {
            String urlPath = afSettings.get("af.lumosservice.base") + "/orchestration/dafintegration/v1/" + workflowId;
            logMethod(urlPath, afSettings.get("af.lumosservice.base"), afSettings.get("af.lumosservice.orchestration.token"));

            RestTemplate restTemplate = timedRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + afSettings.get("af.lumosservice.orchestration.token"));

            HttpEntity<CounterpartyFullDetail> entity = new HttpEntity<>(headers);
            ResponseEntity<CounterpartyFullDetail> response = restTemplate.exchange(urlPath, HttpMethod.GET, entity, new ParameterizedTypeReference<CounterpartyFullDetail>() {
            });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && response.getBody().getCounterpartyDetail() != null) {
                LOG.info("Request to fetch data from Lumos service was successful.");
                LOG.info(response.getBody().getCounterpartyDetail().getCounterpartyName());
            } else {
                LOG.error("Request to fetch data from Lumos service failed to return results ..");
                LOG.info("Error code : " + response.getStatusCode());
            }

            return response.getBody();
        } catch (HttpStatusCodeException e) {
            LOG.error("Lumos workflow {} returned HTTP {} - response body: {}",
                    workflowId, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("Error occurred while retrieving lumos workflow, Reason : " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * RestTemplate with connect/read timeouts so a slow or unresponsive Lumos endpoint
     * cannot hang the external-request scheduler thread indefinitely.
     */
    private RestTemplate timedRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(intSetting("af.lumosservice.connectTimeoutMs", 5000));
        factory.setReadTimeout(intSetting("af.lumosservice.readTimeoutMs", 20000));
        return new RestTemplate(factory);
    }

    private int intSetting(String key, int fallback) {
        String value = afSettings.get(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }


    @Override
    public WorkflowResponseDTO findLumosWorkflowById(int workflowId) {
        LOG.debug("findLumosWorkflowById : Retrieving lumos daf workflow via service ...");
        try {
            String urlPath = afSettings.get("af.lumosservice.base") + "/workflow/api/v1/" + workflowId;
            logMethod(urlPath, afSettings.get("af.lumosservice.base"), afSettings.get("af.lumosservice.workflow.token"));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            headers.set("Authorization", "Bearer " + afSettings.get("af.lumosservice.workflow.token"));

            HttpEntity<WorkflowResponseDTO> entity = new HttpEntity<>(headers);
            ResponseEntity<WorkflowResponseDTO> response = restTemplate.exchange(urlPath, HttpMethod.GET, entity, new ParameterizedTypeReference<WorkflowResponseDTO>() {
            });

            if (response.getStatusCode() == HttpStatus.OK) {
                LOG.info("Request to fetch application data from Lumos service was successful.");
                LOG.info(response.getBody().toString());
            } else {
                LOG.error("Request to fetch application data from Lumos service failed to return results ..");
                LOG.warn("Error code : " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            LOG.error("Error occurred while retrieving application data from lumos using workflow id " + workflowId + ", Reason : " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<MeetingName> getBroaderAfricaMeetingNamesForTenantId(int tenantId) {
        LOG.info("Retrieving Lumos DAF meeting names for tenantId: {}", tenantId);
        try{
            String countryName = getCountryNameForTenant(tenantId);
            LOG.info("Mapped country name for tenantId {}: {}", tenantId, countryName);
            if (countryName == null || countryName.isEmpty()) {
                LOG.warn("No country name mapped for tenantId: {}", tenantId);
                return Collections.emptyList();
            }

            List<CountryInfoDTO> countryInfoDTOS = getCodeForCountry(countryName);
            LOG.info("Retrieved country info for countryName '{}': {}", countryName, countryInfoDTOS);
            if (countryInfoDTOS == null || countryInfoDTOS.isEmpty()) {
                LOG.warn("No country info found for country name: {}", countryName);
                return Collections.emptyList();
            }

            int countryCode = countryInfoDTOS.get(0).getId(); // Assuming first match is valid
            if (isNullOrZero(countryCode)) {
                LOG.warn("Invalid country code retrieved for country name '{}'", countryName);
                return Collections.emptyList();
            }

            LOG.info("Fetching meeting names for countryCode: {}", countryCode);
            return getMeetingNames(countryCode);
        } catch (Exception e){
            LOG.error("Error occurred while retrieving getBroaderAfricaMeetingNamesForTenantId data from lumos using tenant id " + tenantId + ", Reason : " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean processLumosBroaderAfricaWorkflow(String inkombaId, String meetingName, int decisionId, String specificReason) throws ResourceException {
        LOG.info("processing Lumos Broader-Africa Workflow with lumos inkombaId: {}, meetingName:{}, decisionId:{}, specificReason:{}", inkombaId, meetingName, decisionId, specificReason);
        try {
            int lumosClientId = getLumosClientIdByInkombaId(inkombaId);
            LOG.info("lumosClientId retrieved response: {}", lumosClientId);
            if (lumosClientId == 0) {
                String msg = "No Lumos Client ID found for Inkomba ID: " + inkombaId;
                LOG.warn(msg);
                throw new ResourceException(msg);
            }

            WorkflowResponseDTO dto = findLumosWorkflowByClientId(lumosClientId);
            if (dto == null) {
                String msg = "No Lumos Workflow found for Client ID: " + lumosClientId;
            }
            int lumosWorkflowId = dto.getWorkflow().getId();
            LOG.info("lumosWorkflowId retrieved response: {}", lumosWorkflowId);
            if (lumosWorkflowId == 0) {
                String msg = "No Lumos Workflow ID found for Client ID: " + lumosClientId;
                LOG.warn(msg);
                throw new ResourceException(msg);
            }

            boolean commentUpdated = updateWorkflowComment(lumosWorkflowId, specificReason, meetingName, decisionId);
            LOG.info("lumosWorkflowId retrieved response: {}", lumosWorkflowId);
            if (!commentUpdated) {
                String msg = "Failed to update workflow comment for Workflow ID: " + lumosWorkflowId;
                LOG.warn(msg);
                throw new ResourceException(msg);
            }

            boolean decisionUpdated = updateFinalDecision(lumosClientId, decisionId);
            LOG.info("decisionUpdated retrieved response: {}", decisionUpdated);
            if (!decisionUpdated) {
                String msg = "Failed to update final decision for Client ID: " + lumosClientId + " and Decision ID: " + decisionId;
                LOG.warn(msg);
                throw new ResourceException(msg);
            }

            boolean approvedecision = updateApproveDecision(lumosClientId);
            LOG.info("approvedecision retrieved response: {}", approvedecision);
            if (!approvedecision) {
                String msg = "Failed to update final approve decision for Client ID: " + lumosClientId;
                LOG.warn(msg);
                throw new ResourceException(msg);
            }
            return true;
        } catch (Exception e){
            LOG.error("Error processing lumos Broader Africa Workflow. Status: {}", e.getMessage(), e);
            throw e;
        }
    }

    private List<MeetingName> getMeetingNames(int countryCode) {
        LOG.info("Retrieving MeetingNames based on lumos countycode: {}", countryCode);
        try {
            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.refdata.token"); // Correct token usage
            String urlPath = baseUrl + "/reference-data/credit-summary/v1/commercial-meeting-name?includeDisabled=false&countryId=" + countryCode;
            logMethod(urlPath, baseUrl, token);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<MeetingName>> response = restTemplate.exchange(
                    urlPath,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<MeetingName>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LOG.info("Successfully retrieved meeting names: {}", response.getBody());
                return response.getBody();
            } else {
                LOG.error("Failed to retrieve meeting names. Status: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            LOG.error("Error retrieving Lumos meeting names: {}", e.getMessage(), e);
            throw e;
        }
    }

    private List<CountryInfoDTO> getCodeForCountry(String countryName) {
        LOG.info("Retrieving CodeForCountry based on lumos countryName: {}", countryName);
        try {
            if (countryName.isEmpty()) {
                LOG.warn("No country name provided");
                return Collections.emptyList();
            }

            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.refdata.token"); // Correct token usage
            String urlPath = baseUrl + "/reference-data/country/v1/country-name?name=" + countryName;
            logMethod(urlPath, baseUrl, token);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CountryInfoDTO>> response = restTemplate.exchange(
                    urlPath,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CountryInfoDTO>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LOG.info("Successfully retrieved country info: {}", response.getBody());
                return response.getBody();
            } else {
                LOG.error("Failed to retrieve country info. Status: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            LOG.error("Error retrieving country info: {}", e.getMessage(), e);
            throw e;
        }
    }


    private int getLumosClientIdByInkombaId(String inkombaId) {
        LOG.info("Retrieving Lumos Client Id by inkombaId: {}", inkombaId);
        try {
            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.partymodel.token"); // Correct token usage
            String urlPath = baseUrl + "/partymodel/client/v1/lumosId/"+inkombaId+"?recordType=CURRENT";

            logMethod(urlPath, baseUrl, token);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<PartyRoleDto> entity = new HttpEntity<>(headers);
            ResponseEntity<PartyRoleDto> response = restTemplate.exchange(
                    urlPath,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<PartyRoleDto>() {}
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LOG.info("Successfully retrieved response: {}", response.getBody());
                return response.getBody().getClient().getId();
            } else {
                LOG.error("Failed to retrieve client id: {}", response.getStatusCode());
                return 0;
            }
        } catch (Exception e) {
            LOG.error("Error retrieving Lumos meeting names: {}", e.getMessage(), e);
            throw e;
        }

        }

    private WorkflowResponseDTO findLumosWorkflowByClientId(int clientId) {
        LOG.debug("findLumosWorkflowByClientId : Retrieving lumos daf workflow via service ...");
        try {
            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.workflow.token"); // Correct token usage
            String urlPath = baseUrl + "/workflow/api/v1/client/" + clientId;

            logMethod(urlPath, baseUrl, token);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<WorkflowResponseDTO> entity = new HttpEntity<>(headers);
            ResponseEntity<WorkflowResponseDTO> response = restTemplate.exchange(urlPath, HttpMethod.GET, entity, new ParameterizedTypeReference<WorkflowResponseDTO>() {
            });

            if (response.getStatusCode() == HttpStatus.OK) {
                LOG.info("Request to fetch application data from Lumos service was successful.");
                LOG.info(response.getBody().toString());
            } else {
                LOG.error("Request to fetch application data from Lumos service failed to return results ..");
                LOG.warn("Error code : " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            LOG.error("Error occurred while retrieving application data from lumos using cleitn id " + clientId + ", Reason : " + e.getMessage(), e);
            throw e;
        }
    }

    private boolean updateWorkflowComment(int lumosWorkflowId, String specificReason, String meetingName, int decisionId) {
        LOG.info("updating WorkflowComment for lumos workflowId: {}, comment:{}, meetingName:{}, decisionId:{}", lumosWorkflowId, specificReason, meetingName, decisionId);
        try {
            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.lumosworkflow.token"); // Correct token usage
            String urlPath = baseUrl + "/workflow/api/commentary/v1";
            logMethod(urlPath, baseUrl, token);

            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            requestHeaders.add("Authorization", "Bearer " + token);

            RestTemplate restTemplate = new RestTemplate();

            CreateCommentRequest createCommentRequest = new CreateCommentRequest();
            createCommentRequest.setComment(specificReason);
            createCommentRequest.setWorkflowId(lumosWorkflowId);
            createCommentRequest.setMeetingName(meetingName);
            createCommentRequest.setDisplayName("DAF-to-Lumos:adding comment");
            createCommentRequest.setWorkflowDecisionId(decisionId);
            createCommentRequest.setMeetingDate(LocalDateTime.now());

            HttpEntity<CreateCommentRequest> entity = new HttpEntity<>(createCommentRequest, requestHeaders);

            ResponseEntity<WorkflowCommentDTO> responseEntity = restTemplate.exchange(urlPath, HttpMethod.POST, entity, WorkflowCommentDTO.class);

            if(responseEntity.getStatusCode() == HttpStatus.OK){
                LOG.info("updateWorkflowComment retrieved response: {}", responseEntity.getStatusCode());
                return true;
            }
            LOG.info("updateWorkflowComment failed response: {}", responseEntity.getStatusCode());
            return false;
        } catch (Exception e) {
            LOG.error("Error retrieving updateWorkflowComment: {}", e.getMessage(), e);
            throw e;
        }

    }


    private boolean updateFinalDecision(int lumosClientId, int decisionId) {
        LOG.info("updating final decision to lumos clientId: {}, decisionId:{}", lumosClientId, decisionId);
        try {
            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.lumosapproval.token"); // Correct token usage
            String urlPath = baseUrl + "/workflow/api/v1/decision/" + lumosClientId+"?decisionId="+decisionId;

            //https://lumos-apisix-dev.apps.dev-01-rb.ocp.fnb.co.za/workflow/api/v1/decision/20675?decisionId=1

            logMethod(urlPath, baseUrl, token);


            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<WorkflowDTO> entity = new HttpEntity<>(headers);

            ResponseEntity<WorkflowDTO>  response = restTemplate.exchange(
                    urlPath,
                    HttpMethod.POST,
                    entity,
                    WorkflowDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LOG.info("Successfully updateFinalDecision: {}", response.getBody());
                return true;
            } else {
                LOG.error("Failed to updateFinalDecision. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            LOG.error("Error retrieving updateFinalDecision status: {}", e.getMessage(), e);
            throw e;
        }
    }



    private boolean updateApproveDecision(int lumosClientId) {
        LOG.info("processing updateApproveDecision lumosClientId: {}", lumosClientId);
        try {
            String baseUrl = afSettings.get("af.lumosservice.base");
            String token = afSettings.get("af.lumosservice.lumosworkflow.decision.token"); // Correct token usage
            String urlPath = baseUrl + "/workflow/api/v1/approveDecision/"+lumosClientId+"?skipValidation=false&skipIsProspectCheck=false";
            logMethod(urlPath, baseUrl, token);


            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<WorkflowDTO> entity = new HttpEntity<>(headers);

            ResponseEntity<WorkflowDTO>  response = restTemplate.exchange(
                    urlPath,
                    HttpMethod.POST,
                    entity,
                    WorkflowDTO.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                LOG.info("Successfully updateApproveDecision: {}", response.getBody());
                return true;
            } else {
                LOG.error("Failed to updateApproveDecision. Status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            LOG.error("Error retrieving updateApproveDecision. Status: {}", e.getMessage(), e);
            throw e;
        }
    }

   private String getCountryNameForTenant(int tenantId) {
        switch (tenantId) {
            case 3:
                return "Botswana";
            case 4:
                return "Swaziland";
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

    private void logMethod(String urlPath, String baseUrl, String token) {
        LOG.info("LUMOS Auth tocken : " + token);
        LOG.info("LUMOS Base URL : " + baseUrl);
        LOG.info("FULL REQUEST URL : " + urlPath);
    }

    public static boolean isNullOrZero(Integer value) {
        return value == null || value.equals(0);
    }

}
