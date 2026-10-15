package za.co.rmb.approvalforumsoap.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import za.co.rmb.credit.approvalforum.external.CancelExternalRequest;
import za.co.rmb.credit.approvalforum.external.CancelExternalResponse;
import za.co.rmb.credit.approvalforum.external.CreateExternalRequest;
import za.co.rmb.credit.approvalforum.external.CreateExternalResponse;

import jakarta.xml.ws.Response;
import java.util.Collections;

@Endpoint
public class ApprovalForumExternalEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumExternalEndpoint.class);
    private static final String NAMESPACE_URI = "http://credit.rmb.co.za/approvalforum/external";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    public ApprovalForumExternalEndpoint() {

    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createExternalRequest")
    @ResponsePayload
    public CreateExternalResponse createExternalRequest(@RequestPayload CreateExternalRequest request) {
        CreateExternalResponse response = new CreateExternalResponse();

        if(request.getTenantId() != null){
            logger.info("createExternalRequest new app for application id:"+request.getApplicationId()+":tenantId:"+request.getTenantId());
            response.setSuccess(callRestService(request.getApplicationId(), request.getTenantId(),true));
            return response;
        } else{
            logger.info("createExternalRequest old app");
            logger.info("createExternalRequest old app for application id:"+request.getApplicationId());
            response.setSuccess(callRestService(request.getApplicationId(),true));
        }
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "cancelExternalRequest")
    @ResponsePayload
    public CancelExternalResponse cancelExternalRequest(@RequestPayload CancelExternalRequest request) {
        CancelExternalResponse response = new CancelExternalResponse();
        if(request.getTenantId() != null){
            logger.info("cancelExternalRequest new app");
            response.setSuccess(callRestService(request.getApplicationId(), request.getTenantId(),false));
            return response;
        } else{
            logger.info("cancelExternalRequest old app");
            response.setSuccess(callRestService(request.getApplicationId(),false));
        }
        return response;
    }

    private boolean callRestService(int appId, int tenantId, boolean isCreate){
        String uri = getEnvVariable("AF_BASE", "http://rmb-axu1-w:8197/approvalforum/api/");
        logger.info("callRestService:appId:"+appId+" tenantId:"+tenantId);
        if(!uri.endsWith("/")) uri = uri + "/";
        if(isCreate){
            if(tenantId == 2){
                logger.info("createExternalRequest new app:Ashburton");
                uri = uri + "approvalForum/request/external/ashburton/create";
            } else {
                logger.info("createExternalRequest new app:RMB");
                uri = uri + "approvalForum/request/external/create";
            }

        }else{
            if(tenantId == 2){
                logger.info("cancelExternalRequest for app:Ashburton");
                uri = uri + "approvalForum/request/external/ashburton/cancel";
            } else {
                logger.info("cancelExternalRequest for app:RMB");
                uri = uri + "approvalForum/request/external/cancel";
            }
        }
        String token = getEnvVariable("AF_TOKEN",
                "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4MWM2MzIxYi0wMTQ1LTQzZDEtOTFmYS05OWE5OWYyOTUwODQiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2FwcHJvdmFsZm9ydW0iLCJzdWIiOiJTTkFQQVBQX1JSQ2FwdHVyZXJAcm1iLmNvLnphIiwiaWF0IjoxNTcyOTQwNzYyLCJleHAiOjQ3MzUxODA3NjIsInJvbGVzIjpbIkNyZWRpdEFuYWx5c3QiXSwicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.CLzf8a-2Sll3wiKNF2rSvf1GO59XXS-ithiIOjsZcD8");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        requestHeaders.add("Authorization", "Bearer " + token);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri).queryParam("appId", appId);

        HttpEntity<Integer> requestEntity = new HttpEntity<>(appId, requestHeaders);

        ResponseEntity<Response> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, requestEntity, Response.class);

        if(responseEntity.getStatusCode() == HttpStatus.OK){
            return true;
        }
        return false;
    }

    private boolean callRestService(int appId, boolean isCreate){
        String uri = getEnvVariable("AF_BASE", "http://rmb-axu1-w:8197/approvalforum/api/");
        logger.info("createExternalRequest:callRestService old app");
        if(!uri.endsWith("/")) uri = uri + "/";
        if(isCreate){
            logger.info("createExternalRequest:callRestService create");
            uri = uri + "approvalForum/request/external/create";
        }
        else{
            logger.info("createExternalRequest:callRestService cancel");
            uri = uri + "approvalForum/request/external/cancel";
        }
        String token = getEnvVariable("AF_TOKEN",
                "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4MWM2MzIxYi0wMTQ1LTQzZDEtOTFmYS05OWE5OWYyOTUwODQiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2FwcHJvdmFsZm9ydW0iLCJzdWIiOiJTTkFQQVBQX1JSQ2FwdHVyZXJAcm1iLmNvLnphIiwiaWF0IjoxNTcyOTQwNzYyLCJleHAiOjQ3MzUxODA3NjIsInJvbGVzIjpbIkNyZWRpdEFuYWx5c3QiXSwicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.CLzf8a-2Sll3wiKNF2rSvf1GO59XXS-ithiIOjsZcD8");

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        requestHeaders.add("Authorization", "Bearer " + token);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uri).queryParam("appId", appId);

        HttpEntity<Integer> requestEntity = new HttpEntity<>(appId, requestHeaders);

        ResponseEntity<Response> responseEntity = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, requestEntity, Response.class);

        if(responseEntity.getStatusCode() == HttpStatus.OK){
            return true;
        }
        return false;
    }

    private String getEnvVariable(String variable, String def){
        String val = System.getenv(variable);
        if(val == null || "".equals(val.trim())) return def;
        return val;
    }

}
