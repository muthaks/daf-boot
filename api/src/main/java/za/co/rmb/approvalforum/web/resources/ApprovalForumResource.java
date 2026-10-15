package za.co.rmb.approvalforum.web.resources;

import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.*;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;
import za.co.rmb.approvalforum.service.ApprovalForumMailService;
import za.co.rmb.approvalforum.service.ApprovalForumService;
import za.co.rmb.approvalforum.service.exception.ResourceException;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

@SwaggerDefinition(info = @Info(version = "1.0.0", title = "APPROVAL FORUM REST API", contact = @Contact(name = "Arnold Human", email = "arnold.human@rmb.co.za")), consumes = {
        "application/json" }, produces = { "application/json" }, schemes = { SwaggerDefinition.Scheme.HTTP,
        SwaggerDefinition.Scheme.HTTPS }, tags = { @Tag(name = "approvalForum"), @Tag(name = "external"),
        @Tag(name = "auth") }, securityDefinition = @SecurityDefinition(apiKeyAuthDefintions = {
        @ApiKeyAuthDefinition(key = "Bearer", name = "Authorization", in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER) }))
@Api(tags = "approvalForum")
@Path("/approvalForum/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumResource.class);

    @Context
    private SecurityContext context;

    private ApprovalForumService approvalForumService;

    private ApprovalForumMailService approvalForumMailService;

    private ApprovalForumListManagementService approvalForumListManagementService;

    private Map<String, String> afSettings;

    @Inject
    public ApprovalForumResource(ApprovalForumService approvalForumService,
                                 ApprovalForumMailService approvalForumMailService,
                                 @Named("af_settings") Map<String, String> afSettings,
                                 ApprovalForumListManagementService approvalForumListManagementService) {
        this.approvalForumService = approvalForumService;
        this.approvalForumMailService = approvalForumMailService;
        this.approvalForumListManagementService = approvalForumListManagementService;
        this.afSettings = afSettings;
    }

    @GET
    @ApiOperation(value = "Ping service")
    @Path("/ping/")
    @Produces("application/json")
    public Response ping() {
        return Response.ok().build();
    }

    @POST
    @ApiOperation(value = "Create wip request", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/wip")
    @Produces("application/json")
    @Transactional
    public Response createWipRequest(CreateRequestDTO requestDTO, @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                     @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("createWipRequest()");
        tokenValidation(context);
        try {

            String userName = lumosUserName;
            logger.info("createWipRequest():tenantId:" + tenantId+", userName:" + userName);
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            }
            if(tenantId ==2){
                requestDTO.getData().setTenantId(2);
            } else if (tenantId>2) {
                requestDTO.getData().setTenantId(tenantId);
            } else{
                requestDTO.getData().setTenantId(1);
            }
            CreateRequestResponseDTO responseDTO = new CreateRequestResponseDTO().withWorkflowId(
                    approvalForumService.createRequest(requestDTO.isExternalRequest(), requestDTO.getData(), userName));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @DELETE
    @ApiOperation(value = "Cancel wip request", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/wip/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response cancelWipRequest(@PathParam("workflowId") int workflowId, @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("cancelWipRequest()");
        tokenValidation(context);

        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            }
            CancelRequestResponseDTO responseDTO = new CancelRequestResponseDTO().withWorkflowId(
                    approvalForumService.cancelRequest(workflowId, userName));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @PUT
    @ApiOperation(value = "Update wip request", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/wip")
    @Produces("application/json")
    @Transactional
    public Response updateWipRequest(UpdateRequestDTO requestDTO, @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("updateWipRequest()");
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            }

            logger.info("updateWipRequest:getWorkflowId:" + requestDTO.getWorkflowId());
            approvalForumService.updateRequest(requestDTO.getWorkflowId(), requestDTO.getData(),
                    userName);
            return Response.ok().build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Submit a request", authorizations = @Authorization(value = "Bearer"))
    @Path("/request")
    @Produces("application/json")
    @Transactional
    public Response submitRequest(SubmitRequestDTO requestDTO, @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                  @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("submitRequest()");
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            }
            logger.info("submitRequest():tenantId:" + tenantId);
            if (tenantId == 2) {
                requestDTO.getData().setTenantId(2);
            } else if (tenantId > 2) {
                requestDTO.getData().setTenantId(tenantId);
            } else{
                requestDTO.getData().setTenantId(1);
            }
            SubmitRequestResponseDTO submitRequestResponseDTO = new SubmitRequestResponseDTO()
                    .withWorkflowId(approvalForumService.submitRequest(requestDTO.getWorkflowId(), requestDTO.getData(),
                            userName));
            return Response.ok(submitRequestResponseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Submit a request to chairperson", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/chairperson")
    @Produces("application/json")
    @Transactional
    public Response submitRequestToChairperson(SubmitRequestDTO requestDTO,
                                               @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("submitRequestToChairperson()");
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            }

            SubmitRequestResponseDTO submitRequestResponseDTO = new SubmitRequestResponseDTO()
                    .withWorkflowId(approvalForumService.submitRequestToChairperson(requestDTO.getWorkflowId(), requestDTO.getData(),
                            userName));
            return Response.ok(submitRequestResponseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Create memo", authorizations = @Authorization(value = "Bearer"))
    @Path("/memo")
    @Produces("application/json")
    @Transactional
    public Response createMemo(CreateMemoDTO requestDTO,
                               @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("createMemo()");
        tokenValidation(context);
        String userName = lumosUserName;
        if ("NONE".equalsIgnoreCase(lumosUserName)) {
            if (context != null && context.getUserPrincipal() != null) {
                userName = context.getUserPrincipal().getName();
            } else {
                logger.warn("User principal is null, unable to resolve actual username.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid user context").build();
            }
        }
        CreateMemoResponseDTO responseDTO = new CreateMemoResponseDTO()
                .withWorkflowId(approvalForumService.createMemo(requestDTO, userName));
        return Response.ok(responseDTO).build();
    }

    @GET
    @ApiOperation(value = "Get request", authorizations = @Authorization(value = "Bearer"))
    @Path("/capturer/{workflowId}")
    @Produces("application/json")
    public Response getCapturerRequest(@PathParam("workflowId") int workflowId, @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                       @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("getCapturerRequest({})", workflowId);
        tokenValidation(context);
        if (tenantId == 2) {
            logger.info("getCapturerRequest({})", tenantId);
            return getRequestForAshburton(workflowId, WorkflowListSearchTypes.CAPTURER);
        } else if (tenantId>2) {
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("getCapturerRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return getRequestForLumosBroaderAfricaTenants(workflowId, WorkflowListSearchTypes.CAPTURER, tenantId, lumosUserName);
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
        } else {
            return getRequest(workflowId, WorkflowListSearchTypes.CAPTURER,tenantId);
        }
    }

    @GET
    @ApiOperation(value = "Get request", authorizations = @Authorization(value = "Bearer"))
    @Path("/chairperson/{workflowId}")
    @Produces("application/json")
    public Response getChairpersonRequest(@PathParam("workflowId") int workflowId, @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                          @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("getChairpersonRequest({})", workflowId);
        tokenValidation(context);
        if (tenantId == 2) {
            return getRequestForAshburton(workflowId, WorkflowListSearchTypes.CHAIRPERSON);
        }
        else if (tenantId>2) {
            logger.info("getChairpersonRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("getChairpersonRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return getRequestForLumosBroaderAfricaTenants(workflowId, WorkflowListSearchTypes.CHAIRPERSON, tenantId, lumosUserName);
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
        }
        else {
            return getRequest(workflowId, WorkflowListSearchTypes.CHAIRPERSON, tenantId);
        }

    }

    @GET
    @ApiOperation(value = "Get request", authorizations = @Authorization(value = "Bearer"))
    @Path("/reviewer/{workflowId}")
    @Produces("application/json")
    public Response getRevieweRequest(@PathParam("workflowId") int workflowId, @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                      @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("getReviewerRequest({})", workflowId);
        tokenValidation(context);
        if(tenantId == 2){
            return getRequestForAshburton(workflowId, WorkflowListSearchTypes.REVIEWER);
        }
        else if (tenantId>2) {
            logger.info("getRevieweRequest:getChairpersonRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("getRevieweRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return getRequestForLumosBroaderAfricaTenants(workflowId, WorkflowListSearchTypes.REVIEWER, tenantId, lumosUserName);
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
        }
        else {
            return getRequest(workflowId, WorkflowListSearchTypes.REVIEWER, tenantId);
        }

    }

    @GET
    @ApiOperation(value = "Get request", authorizations = @Authorization(value = "Bearer"))
    @Path("/approver/{workflowId}")
    @Produces("application/json")
    public Response getApproverRequest(@PathParam("workflowId") int workflowId,
                                       @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                       @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("getApproverRequest({})", workflowId);
        tokenValidation(context);
        if(tenantId == 2){
            return getRequestForAshburton(workflowId, WorkflowListSearchTypes.APPROVER);
        }
        else if (tenantId>2) {
            logger.info("getApproverRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("getApproverRequest:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return getRequestForLumosBroaderAfricaTenants(workflowId, WorkflowListSearchTypes.APPROVER, tenantId, lumosUserName);
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }

        }
        else{
            return getRequest(workflowId, WorkflowListSearchTypes.APPROVER, tenantId);
        }

    }

    private Response getRequestForAshburton(int workflowId, WorkflowListSearchTypes searchTypes) {
        try {
            RequestData requestData = approvalForumService.getRequestForAshburton(workflowId, context.getUserPrincipal().getName(), true,
                    searchTypes);
            if (requestData == null) {
                return Response.noContent().build();
            }
            return Response.ok(new GetRequestResponseDTO().withRequestData(requestData)).build();
        } catch (ResourceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    private Response getRequestForLumosBroaderAfricaTenants(int workflowId, WorkflowListSearchTypes searchTypes, int tenantId, String lumosUserName) {
        try{
            RequestData requestData = approvalForumService.getRequestForLumosBroaderAfricaTenants(workflowId, lumosUserName, true,
                    searchTypes, tenantId);
            if (requestData == null) {
                return Response.noContent().build();
            }
            return Response.ok(new GetRequestResponseDTO().withRequestData(requestData)).build();
        }catch(ResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    private Response getRequest(int workflowId, WorkflowListSearchTypes searchTypes, int tenantId){
        try{
            RequestData requestData = approvalForumService.getRequest(workflowId, context.getUserPrincipal().getName(), true,
                    searchTypes);
            if (requestData == null) {
                return Response.noContent().build();
            }
            if(tenantId>2){
                requestData.setTenantId(tenantId);
            }

            return Response.ok(new GetRequestResponseDTO().withRequestData(requestData)).build();
        }catch(ResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/new/{inkombaId}")
    @Produces("application/json")
    @ApiOperation(value = "Get new request details",
            authorizations = @Authorization(value = "Bearer"))
    public Response getNewRequest(@PathParam("inkombaId") String inkombaId) {
        logger.info("getNewRequest({})", inkombaId);
        logger.info("getNewRequest TOKEN ", context);

        tokenValidation(context);
        try {
            return Response.ok(approvalForumService.getNewRequestDetails(inkombaId)).build();
        } catch (ResourceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @GET
    @Path("/new/ashburton/{inkombaId}")
    @Produces("application/json")
    @ApiOperation(value = "Get new request details",
            authorizations = @Authorization(value = "Bearer"))
    public Response getAshburtonNewRequest(@PathParam("inkombaId") String inkombaId) {
        logger.info("getAshburtonNewRequest({})", inkombaId);
        logger.info("getNewRequest TOKEN ", context);

        tokenValidation(context);
        try{
            return Response.ok(approvalForumService.getNewRequestDetailsForAshburton(inkombaId, 0)).build();
        }catch(ResourceException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if a new workflow can be assigned to a specific user", authorizations = @Authorization(value = "Bearer"))
    @Path("/canAssign/new/specific/{userId}")
    @Produces("application/json")
    public Response checkIfWipWorkflowCanBeAssignedToSpecific(CheckUserAssignSpecificRequestDTO requestDTO,
                                                              @PathParam("userId") String userId) {
        logger.info("checkIfWipWorkflowCanBeAssignedToSpecific({})", userId);
        tokenValidation(context);
        try {
            CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWipWorkflowCanBeAssigned(requestDTO.getRulesResponse(),
                    requestDTO.getUserResponse(), userId, true));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if a new chairperson workflow can be assigned to a specific user", authorizations = @Authorization(value = "Bearer"))
    @Path("/canAssign/new/chair/specific/{userId}/{workflowId}")
    @Produces("application/json")
    public Response checkIfChairpersonWorkflowCanBeAssignedToSpecific(CheckUserAssignSpecificRequestDTO requestDTO,
                                                                      @PathParam("userId") String userId, @PathParam("workflowId") int workflowId) {
        logger.info("checkIfChairpersonWorkflowCanBeAssignedToSpecific({})", userId, workflowId);
        tokenValidation(context);
        try {
            CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfChairpersonWorkflowCanBeAssigned(requestDTO.getRulesResponse(),
                    requestDTO.getUserResponse(), userId, true, workflowId));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if a new workflow can be assigned to an additional user", authorizations = @Authorization(value = "Bearer"))
    @Path("/canAssign/new/additional/{userId}")
    @Produces("application/json")
    public Response checkIfWipWorkflowCanBeAssignedToAdditional(RulesResponse rulesResponse,
                                                                @PathParam("userId") String userId) {
        logger.info("checkIfWipWorkflowCanBeAssignedToAdditional({})", userId);
        tokenValidation(context);
        try {
            CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
            responseDTO
                    .setResponse(approvalForumService.checkIfWipWorkflowCanBeAssigned(rulesResponse, null, userId, false));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if a new chairperson workflow can be assigned to an additional user", authorizations = @Authorization(value = "Bearer"))
    @Path("/canAssign/new/chair/additional/{userId}/{workflowId}")
    @Produces("application/json")
    public Response checkIfChairpersonWorkflowCanBeAssignedToAdditional(RulesResponse rulesResponse,
                                                                        @PathParam("userId") String userId, @PathParam("workflowId") int workflowId) {
        logger.info("checkIfChairpersonWorkflowCanBeAssignedToAdditional({})", userId, workflowId);
        tokenValidation(context);
        try {
            CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
            responseDTO
                    .setResponse(approvalForumService.checkIfChairpersonWorkflowCanBeAssigned(rulesResponse, null, userId, false, workflowId));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if an existing workflow can be assigned to the user", authorizations = @Authorization(value = "Bearer"))
    @Path("/canAssign/{workflowId}")
    @Produces("application/json")
    public Response checkIfWorkflowCanBeAssigned(@PathParam("workflowId") int workflowId,
                                                 @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("checkIfWorkflowCanBeAssigned({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("checkIfWorkflowCanBeAssigned({})", userName);
            }

            CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWorkflowCanBeAssigned(workflowId,
                    userName));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if a wip workflow can be assigned to the user", authorizations = @Authorization(value = "Bearer"))
    @Path("/canAssign/wip/{workflowId}")
    @Produces("application/json")
    public Response checkIfWipWorkflowCanBeAssigned(@PathParam("workflowId") int workflowId,
                                                    @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("checkIfWipWorkflowCanBeAssigned({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("checkIfWorkflowCanBeAssigned({})", userName);
            }

            CanBeAssignedResponseDTO responseDTO = new CanBeAssignedResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWipWorkflowCanBeAssigned(workflowId,
                    userName));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Checks if an workflow can be viewedby the user",
            authorizations = @Authorization(value = "Bearer"))
    @Path("/canView/{workflowId}")
    @Produces("application/json")
    public Response checkIfWorkflowCanBeViewed(@PathParam("workflowId") int workflowId,
                                               @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("checkIfWorkflowCanBeViewed({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("checkIfWorkflowCanBeAssigned({})", userName);
            }

            CanBeViewedResponseDTO responseDTO = new CanBeViewedResponseDTO();
            responseDTO.setCanBeViewed(approvalForumService.checkIfWorkflowCanBeViewed(workflowId, userName));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Assign workflow to user",
            authorizations = @Authorization(value = "Bearer"))
    @Path("/assign/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response assignWorkflow(@PathParam("workflowId") int workflowId,
                                   @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("assignWorkflow({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("assignWorkflow({})", userName);
            }
            AssignResponseDTO responseDTO = new AssignResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWorkflowCanBeAssigned(workflowId,
                    userName));
            if (AssignedTypes.CAN_BE_ASSIGNED.equals(responseDTO.getResponse().getResponse())) {
                responseDTO.setResponse(
                        approvalForumService.assignWorkflow(workflowId, userName));
            }
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Assign wip workflow to user",
            authorizations = @Authorization(value = "Bearer"))
    @Path("/assign/wip/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response assignWipWorkflow(@PathParam("workflowId") int workflowId, @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("assignWipWorkflow({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("assignWipWorkflow({})", userName);
            }
            AssignResponseDTO responseDTO = new AssignResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWipWorkflowCanBeAssigned(workflowId,
                    userName));
            if (AssignedTypes.CAN_BE_ASSIGNED.equals(responseDTO.getResponse().getResponse())) {
                responseDTO.setResponse(
                        approvalForumService.assignWipWorkflow(workflowId, userName));
            }
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Un assign workflow to user", authorizations = @Authorization(value = "Bearer"))
    @Path("/unassign/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response unAssignWorkflow(@PathParam("workflowId") int workflowId,
                                     @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("unAssignWorkflow({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("unAssignWorkflow({})", userName);
            }

            UnAssignResponseDTO responseDTO = new UnAssignResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWorkflowCanBeAssigned(workflowId,
                    userName));
            if (AssignedTypes.QUORUM_REACHED_AND_ASSIGNED.equals(responseDTO.getResponse().getResponse())
                    || AssignedTypes.ALREADY_ASSIGNED.equals(responseDTO.getResponse().getResponse())) {
                responseDTO.setResponse(
                        approvalForumService.unAssignWorkflow(workflowId, userName));
            }
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Un assign wip workflow to user", authorizations = @Authorization(value = "Bearer"))
    @Path("/unassign/wip/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response unAssignWipWorkflow(@PathParam("workflowId") int workflowId,
                                        @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("unAssignWipWorkflow({})", workflowId);
        tokenValidation(context);
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("unAssignWipWorkflow({})", userName);
            }

            UnAssignResponseDTO responseDTO = new UnAssignResponseDTO();
            responseDTO.setResponse(approvalForumService.checkIfWipWorkflowCanBeAssigned(workflowId,
                    userName));
            if (AssignedTypes.ALREADY_ASSIGNED.equals(responseDTO.getResponse().getResponse())) {
                responseDTO.setResponse(
                        approvalForumService.unAssignWipWorkflow(workflowId, userName));
            }
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @PUT
    @ApiOperation(value = "Update user decision", authorizations = @Authorization(value = "Bearer"))
    @Path("/decision/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response updateWorkflowDecision(@PathParam("workflowId") int workflowId,
                                           UpdateDecisionRequestDTO updateDecisionRequestDTO,
                                           @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName
    ) {
        logger.info("updateWorkflowDecision({}, {})", workflowId, updateDecisionRequestDTO.getDecision() != null ? updateDecisionRequestDTO.getDecision().value() : "");
        tokenValidation(context);

        UpdateDecisionResponseDTO responseDTO = new UpdateDecisionResponseDTO();
        try {
            String userName = lumosUserName;
            if ("NONE".equalsIgnoreCase(lumosUserName)) {
                if (context != null && context.getUserPrincipal() != null) {
                    userName = context.getUserPrincipal().getName();
                } else {
                    logger.warn("User principal is null, unable to resolve actual username.");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Invalid user context").build();
                }
            } else {
                logger.info("updateWorkflowDecision({})", userName);
            }
            responseDTO.setSuccess(true);
            AssignResponse assignResponse = approvalForumService.checkIfWorkflowCanBeSubmitted(workflowId, userName);
            if (assignResponse.getResponse().equals(AssignedTypes.CAN_BE_SUBMITTED)) {
                approvalForumService.updateWorkflowDecision(workflowId, userName,
                        updateDecisionRequestDTO);
            } else {
                responseDTO.setSuccess(false);
                responseDTO.setMessage(assignResponse.getResponse().value());
            }
        } catch (ResourceException e) {
            e.printStackTrace();
            responseDTO.setSuccess(false);
            responseDTO.setMessage(e.getMessage());
        }
        return Response.ok(responseDTO).build();
    }

    @POST
    @ApiOperation(value = "Submit user decision", authorizations = @Authorization(value = "Bearer"))
    @Path("/decision/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response submitWorkflowDecision(@PathParam("workflowId") int workflowId,
                                           SubmitDecisionRequestDTO submitDecisionRequestDTO,
                                           @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("submitWorkflowDecision({}, {})", workflowId, submitDecisionRequestDTO.getDecision().value());
        tokenValidation(context);
        String userName = lumosUserName;
        if ("NONE".equalsIgnoreCase(lumosUserName)) {
            if (context != null && context.getUserPrincipal() != null) {
                userName = context.getUserPrincipal().getName();
            } else {
                logger.warn("User principal is null, unable to resolve actual username.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid user context").build();
            }
        } else {
            logger.info("submitSystemWorkflowDecision({})", userName);
        }
        return submitDecision(workflowId, submitDecisionRequestDTO, false, userName);
    }

    @POST
    @ApiOperation(value = "Submit system decision", authorizations = @Authorization(value = "Bearer"))
    @Path("/decision/system/{workflowId}")
    @Produces("application/json")
    @Transactional
    public Response submitSystemWorkflowDecision(@PathParam("workflowId") int workflowId,
                                                 SubmitDecisionRequestDTO submitDecisionRequestDTO,
                                                 @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        logger.info("submitSystemWorkflowDecision({}, {})", workflowId, submitDecisionRequestDTO.getDecision().value());

        tokenValidation(context);
        String userName = lumosUserName;
        if ("NONE".equalsIgnoreCase(lumosUserName)) {
            if (context != null && context.getUserPrincipal() != null) {
                userName = context.getUserPrincipal().getName();
            } else {
                logger.warn("User principal is null, unable to resolve actual username.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid user context").build();
            }
        } else {
            logger.info("submitSystemWorkflowDecision({})", userName);
        }

        return submitDecision(workflowId, submitDecisionRequestDTO, true, userName);
    }

    private Response submitDecision(int workflowId, SubmitDecisionRequestDTO submitDecisionRequestDTO,
                                    boolean isSystem, String userName) {
        return approvalForumService.submitDecision(workflowId, submitDecisionRequestDTO, isSystem, userName);
    }

    @POST
    @ApiOperation(value = "Execute rules engine to retrieve quorum details", authorizations = @Authorization(value = "Bearer"))
    @Path("/rules")
    @Produces("application/json")
    public Response executeRules(RulesRequestDTO rulesRequestDTO) {
        logger.info("executeRules()");
        tokenValidation(context);
        try {
            RulesResponseDTO responseDTO = new RulesResponseDTO();
            responseDTO.setResponse(approvalForumService.processRules(rulesRequestDTO));
            return Response.ok(responseDTO).build();
        } catch (ResourceException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @GET
    @ApiOperation(value = "Get a list of all users which can be added to a requests/memos", authorizations = @Authorization(value = "Bearer"))
    @Path("/users/all")
    @Produces("application/json")
    public Response getUsersAll() {
        logger.info("getUsersAll()");
        tokenValidation(context);
        UsersResponseDTO responseDTO = new UsersResponseDTO();
        responseDTO.setResponse(approvalForumListManagementService.getAllUsers());
        return Response.ok(responseDTO).build();
    }

    @GET
    @Path("/refdata/requestTypes")
    @Produces("application/json")
    @ApiOperation(value = "Get a list of request types to select from", authorizations = @Authorization(value = "Bearer"))
    public Response getRequestTypes() {
        logger.info("getRequestTypes()");
        tokenValidation(context);
        List<String> modelList = new ArrayList<>();
        for (RequestType requestType : RequestType.values()) {
            modelList.add(requestType.value());
        }
        return Response.ok(new RequestTypesResponseDTO().withType(modelList)).build();
    }

    @GET
    @Path("/refdata/workflowStatus")
    @Produces("application/json")
    @ApiOperation(value = "Get a list of workflow status to select from", authorizations = @Authorization(value = "Bearer"))
    public Response getWorkflowStatus() {
        logger.info("getWorkflowStatus()");
        tokenValidation(context);
        List<String> modelList = new ArrayList<>();
        for (WorkflowStatus workflowStatus : WorkflowStatus.values()) {
            modelList.add(workflowStatus.value());
        }
        return Response.ok(new WorkflowStatusResponseDTO().withStatus(modelList)).build();
    }

    @GET
    @Path("/refdata/approvalDecisions/{prospect}")
    @Produces("application/json")
    @ApiOperation(value = "Get a list of approval decisions", authorizations = @Authorization(value = "Bearer"))
    public Response getApprovalDecisions(@PathParam("prospect") boolean prospect) {
        logger.info("getApprovalDecisions({})", prospect);
        tokenValidation(context);
        List<String> modelList = new ArrayList<>();
        for (ApprovalDecision approvalDecision : ApprovalDecision.values()) {
            if (!approvalDecision.value().contains("(CASDM)") && !approvalDecision.value().contains("(LUMOS)")) {
                if (prospect) {
                    if (!approvalDecision.equals(ApprovalDecision.APPROVED)) {
                        modelList.add(approvalDecision.value());
                    }
                } else {
                    modelList.add(approvalDecision.value());
                }
            }
        }
        return Response.ok(new ApprovalDecisionResponseDTO().withDecision(modelList)).build();
    }

    @GET
    @Path("/list/analyst")
    @Produces("application/json")
    @ApiOperation(value = "Get list of credit analyst requests", authorizations = @Authorization(value = "Bearer"))
    public Response listAnalyst(@DefaultValue("") @QueryParam("assignedTo") String assignedTo,
                                @DefaultValue("") @QueryParam("searchText") String searchText,
                                @DefaultValue("false") @QueryParam("onlyActive") boolean onlyActive, @QueryParam("start") int start,
                                @QueryParam("size") int size, @DefaultValue("All") @QueryParam("filterType") String filterType,
                                @DefaultValue("NONE") @QueryParam("sortByType") String sortByType,
                                @DefaultValue("false") @QueryParam("descending") boolean descending,
                                @DefaultValue("") @QueryParam("approvalMandate") String approvalMandate,
                                @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName

    ) {
        logger.info("listAnalyst({})", tenantId);
        tokenValidation(context);
        if (assignedTo != null && !"".equals(assignedTo)) {
            if (assignedTo.contains("RMB-NT")) {
                assignedTo = assignedTo.substring(7);
            }
        }
        if(tenantId == 2){
            return Response.ok(approvalForumService.getWorkflowsForAshburton(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CAPTURER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }
        else if (tenantId>2) {
            logger.info("listAnalyst:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("listAnalyst:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                        WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CAPTURER,
                        WorkflowSortByTypes.fromValue(sortByType), descending, lumosUserName, approvalMandate,tenantId)).build();
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
           /* return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CAPTURER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate,tenantId)).build();*/

        }
        else{
            return Response.ok(approvalForumService.getWorkflows(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CAPTURER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }

    }

    @GET
    @Path("/list/chairperson")
    @Produces("application/json")
    @ApiOperation(value = "Get list of chairperson requests", authorizations = @Authorization(value = "Bearer"))
    public Response listChairperson(@DefaultValue("") @QueryParam("assignedTo") String assignedTo,
                                    @DefaultValue("") @QueryParam("searchText") String searchText,
                                    @DefaultValue("false") @QueryParam("onlyActive") boolean onlyActive, @QueryParam("start") int start,
                                    @QueryParam("size") int size, @DefaultValue("All") @QueryParam("filterType") String filterType,
                                    @DefaultValue("NONE") @QueryParam("sortByType") String sortByType,
                                    @DefaultValue("false") @QueryParam("descending") boolean descending,
                                    @DefaultValue("") @QueryParam("approvalMandate") String approvalMandate,
                                    @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                    @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        tokenValidation(context);
        if (assignedTo != null && !"".equals(assignedTo)) {
            if (assignedTo.contains("RMB-NT")) {
                assignedTo = assignedTo.substring(7);
            }
        }
        if (tenantId == 2) {
            return Response.ok(approvalForumService.getWorkflowsForAshburton(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CHAIRPERSON,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }
        else if (tenantId>2) {
            logger.info("listChairperson:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("listChairperson:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                        WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CHAIRPERSON,
                        WorkflowSortByTypes.fromValue(sortByType), descending, lumosUserName, approvalMandate, tenantId)).build();
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
           /* return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CHAIRPERSON,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate, tenantId)).build();*/

        }

        else{
            return Response.ok(approvalForumService.getWorkflows(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.CHAIRPERSON,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }

    }

    @GET
    @Path("/list/approver")
    @Produces("application/json")
    @ApiOperation(value = "Get list of approver requests", authorizations = @Authorization(value = "Bearer"))
    public Response listApprover(@DefaultValue("") @QueryParam("assignedTo") String assignedTo,
                                 @DefaultValue("") @QueryParam("searchText") String searchText,
                                 @DefaultValue("false") @QueryParam("onlyActive") boolean onlyActive, @QueryParam("start") int start,
                                 @QueryParam("size") int size, @DefaultValue("All") @QueryParam("filterType") String filterType,
                                 @DefaultValue("NONE") @QueryParam("sortByType") String sortByType,
                                 @DefaultValue("false") @QueryParam("descending") boolean descending,
                                 @DefaultValue("") @QueryParam("approvalMandate") String approvalMandate,
                                 @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                 @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        tokenValidation(context);
        logger.info("listApprover():" + tenantId);
        if (assignedTo != null && !"".equals(assignedTo)) {
            if (assignedTo.contains("RMB-NT")) {
                assignedTo = assignedTo.substring(7);
            }
        }
        if (tenantId == 2) {
            return Response.ok(approvalForumService.getWorkflowsForAshburton(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.APPROVER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        } else if (tenantId > 2 ) {
            logger.info("listApprover:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("listApprover:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                        WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.APPROVER,
                        WorkflowSortByTypes.fromValue(sortByType), descending, lumosUserName, approvalMandate, tenantId)).build();
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
            /*return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.APPROVER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate, tenantId)).build();*/
        } else {
            return Response.ok(approvalForumService.getWorkflows(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.APPROVER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }

    }

    @GET
    @Path("/list/reviewer")
    @Produces("application/json")
    @ApiOperation(value = "Get list of reviewer requests", authorizations = @Authorization(value = "Bearer"))
    public Response listReviewer(@DefaultValue("") @QueryParam("assignedTo") String assignedTo,
                                 @DefaultValue("") @QueryParam("searchText") String searchText,
                                 @DefaultValue("false") @QueryParam("onlyActive") boolean onlyActive, @QueryParam("start") int start,
                                 @QueryParam("size") int size, @DefaultValue("All") @QueryParam("filterType") String filterType,
                                 @DefaultValue("NONE") @QueryParam("sortByType") String sortByType,
                                 @DefaultValue("false") @QueryParam("descending") boolean descending,
                                 @DefaultValue("") @QueryParam("approvalMandate") String approvalMandate,
                                 @ApiParam(value = "", required = false) @DefaultValue("1") @QueryParam("tenantId") int tenantId,
                                 @ApiParam(value = "", required = false) @DefaultValue("NONE") @QueryParam("userName") String lumosUserName) {
        tokenValidation(context);
        if (assignedTo != null && !"".equals(assignedTo)) {
            if (assignedTo.contains("RMB-NT")) {
                assignedTo = assignedTo.substring(7);
            }
        }
        if (tenantId == 2) {
            return Response.ok(approvalForumService.getWorkflowsForAshburton(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.REVIEWER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }
        else if (tenantId > 2 ) {
            logger.info("listApprover:getRequestForLumosBroaderAfricaTenants({})", tenantId);
            if (!"NONE".equalsIgnoreCase(lumosUserName)) {
                logger.info("listApprover:getRequestForLumosBroaderAfricaTenants({})", tenantId);
                return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                        WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.REVIEWER,
                        WorkflowSortByTypes.fromValue(sortByType), descending, lumosUserName, approvalMandate, tenantId)).build();
            } else{
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Please provide username").build();
            }
           /* return Response.ok(approvalForumService.getWorkflowsForLumosBroaderAfricaTenants(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.REVIEWER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate, tenantId)).build();*/
        }

        else {
            return Response.ok(approvalForumService.getWorkflows(assignedTo, start, size, onlyActive, searchText,
                    WorkflowFilterTypes.fromValue(filterType), WorkflowListSearchTypes.REVIEWER,
                    WorkflowSortByTypes.fromValue(sortByType), descending, context.getUserPrincipal().getName(), approvalMandate)).build();
        }

    }

    @POST
    @ApiOperation(value = "Submit a chat", authorizations = @Authorization(value = "Bearer"))
    @Path("/chat")
    @Produces("application/json")
    @Transactional
    public Response submitChat(ChatCreateRequestDTO requestDTO) {
        logger.info("submitChat()");
        tokenValidation(context);
        approvalForumService.createChat(requestDTO.getWorkflowId(), requestDTO.getChatText(),
                context.getUserPrincipal());
        return Response.ok().build();
    }

    @GET
    @ApiOperation(value = "Get workflow chats", authorizations = @Authorization(value = "Bearer"))
    @Path("/chat/{workflowId}")
    @Produces("application/json")
    public Response getChats(@PathParam("workflowId") int workflowId) {
        logger.info("getChats({})", workflowId);
        tokenValidation(context);
        ChatResponseDTO responseDTO = new ChatResponseDTO().withChats(approvalForumService.getWorkflowChats(workflowId))
                .withWorkflowId(workflowId);
        return Response.ok(responseDTO).build();
    }

    @POST
    @ApiOperation(value = "Create a new external request using the CASDM applicationId", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/external/create")
    @Produces("application/json")
    @Transactional
    public Response createRequestExternal(@DefaultValue("0") @QueryParam("appId") int appId,
                                          @DefaultValue("false") @QueryParam("lumos") boolean lumos, @DefaultValue("0") @QueryParam("tenantId") int tenantId) {
        logger.info("createRequestExternal(appId: {}, lumos: {}, tenantId: {})", appId, lumos, tenantId);
        tokenValidation(context);
        try {
            approvalForumService.createRequestExternal(appId, true, lumos, tenantId);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Create a new external request using the CASDM-ashburton applicationId", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/external/ashburton/create")
    @Produces("application/json")
    @Transactional
    public Response createAshburtonRequestExternal(@DefaultValue("0") @QueryParam("appId") int appId) {
        logger.info("createAshburtonRequestExternal({})", appId);
        tokenValidation(context);
        try {
            approvalForumService.createAshburtonRequestExternal(appId, true);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Cancel an existing request using the CASDM applicationId", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/external/cancel")
    @Produces("application/json")
    @Transactional
    public Response cancelRequestExternal(@DefaultValue("0") @QueryParam("appId") int appId,
                                          @DefaultValue("false") @QueryParam("lumos") boolean lumos, @DefaultValue("0") @QueryParam("tenantId") int tenantId) {
        logger.info("cancelRequestExternal({})", appId, lumos);
        tokenValidation(context);
        try {
            approvalForumService.createRequestExternal(appId, false, lumos, tenantId);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Cancel an existing request using the CASDM applicationId", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/external/ashburton/cancel")
    @Produces("application/json")
    @Transactional
    public Response cancelAshburtonRequestExternal(@DefaultValue("0") @QueryParam("appId") int appId) {
        logger.info("cancelRequestExternal({})", appId);
        tokenValidation(context);
        try {
            approvalForumService.createAshburtonRequestExternal(appId, false);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Process incomplete external requests", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/external/process")
    @Produces("application/json")
    @Transactional
    public Response processExternalRequests() {
        tokenValidation(context);
        try {
            approvalForumService.processExternalRequests();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Check status of casdm for applications, and if the status has changed from 'submitted for approval', we need to update daf", authorizations = @Authorization(value = "Bearer"))
    @Path("/request/cleanup")
    @Produces("application/json")
    @Transactional
    public Response cleanupRequests() {
        logger.info("cleanupRequests()");
        tokenValidation(context);
        try {
            approvalForumService.cleanupRequests();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Send escalations", authorizations = @Authorization(value = "Bearer"))
    @Path("/mail/escalations")
    @Produces("application/json")
    @Transactional
    public Response sendMailEscalations() {
        tokenValidation(context);
        try {
            approvalForumService.sendMailEscalations();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Send approver escalation email", authorizations = @Authorization(value = "Bearer"))
    @Path("/mail/approver")
    @Produces("application/json")
    @Transactional
    public Response sendApproverMailEscalation() {
        tokenValidation(context);
        try {
            approvalForumService.sendApproverMailEscalation();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Send capturer escalation email", authorizations = @Authorization(value = "Bearer"))
    @Path("/mail/capturer")
    @Produces("application/json")
    @Transactional
    public Response sendCapturerMailEscalation() {
        tokenValidation(context);
        try {
            approvalForumService.sendCapturerMailEscalation();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @Path("/mail/test")
    @GET
    @Produces("application/json")
    public Response testMail() {
        logger.info("testMail()");
        approvalForumMailService.sendTestMail();
        return Response.ok().build();
    }

    @POST
    @ApiOperation(value = "Send unsent mails", authorizations = @Authorization(value = "Bearer"))
    @Path("/mail/sendUnsent")
    @Produces("application/json")
    @Transactional
    public Response sendUnsentMails() {
        tokenValidation(context);
        try {
            approvalForumMailService.sendUnsentMails();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Resend failed daily mails (every 2 hours)", authorizations = @Authorization(value = "Bearer"))
    @Path("/mail/resendDaily")
    @Produces("application/json")
    @Transactional
    public Response resendDailyMails() {
        tokenValidation(context);
        try {
            approvalForumMailService.resendDailyMails();
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @PUT
    @ApiOperation(value = "Update lastSeenChatId for capturer", authorizations = @Authorization(value = "Bearer"))
    @Path("/chat/capturer")
    @Produces("application/json")
    @Transactional
    public Response updateCapturerLastSeenChatId(UpdateLastSeenChatRequestDTO requestDTO) {
        logger.info("updateCapturerLastSeenChatId({}, {})", requestDTO.getChatId(), requestDTO.getWorkflowId());
        tokenValidation(context);
        approvalForumService.updateCapturerLastSeenChatId(requestDTO.getWorkflowId(), requestDTO.getChatId(), context.getUserPrincipal().getName());
        return Response.ok().build();
    }

    @PUT
    @ApiOperation(value = "Update lastSeenChatId for reviewer", authorizations = @Authorization(value = "Bearer"))
    @Path("/chat/reviewer")
    @Produces("application/json")
    @Transactional
    public Response updateReviewerLastSeenChatId(UpdateLastSeenChatRequestDTO requestDTO) {
        logger.info("updateReviewerLastSeenChatId({}, {})", requestDTO.getChatId(), requestDTO.getWorkflowId());
        tokenValidation(context);
        approvalForumService.updateReviewerLastSeenChatId(requestDTO.getWorkflowId(), requestDTO.getChatId(), context.getUserPrincipal().getName());
        return Response.ok().build();
    }

    @PUT
    @ApiOperation(value = "Update lastSeenChatId for approver", authorizations = @Authorization(value = "Bearer"))
    @Path("/chat/approver")
    @Produces("application/json")
    @Transactional
    public Response updateApproverLastSeenChatId(UpdateLastSeenChatRequestDTO requestDTO) {
        logger.info("updateApproverLastSeenChatId({}, {})", requestDTO.getChatId(), requestDTO.getWorkflowId());
        tokenValidation(context);
        approvalForumService.updateApproverLastSeenChatId(requestDTO.getWorkflowId(), requestDTO.getChatId(), context.getUserPrincipal().getName());
        return Response.ok().build();
    }

    @PUT
    @ApiOperation(value = "Update lastSeenChatId for chairperson", authorizations = @Authorization(value = "Bearer"))
    @Path("/chat/chairperson")
    @Produces("application/json")
    @Transactional
    public Response updateChairpersonLastSeenChatId(UpdateLastSeenChatRequestDTO requestDTO) {
        logger.info("updateApproverLastSeenChatId({}, {})", requestDTO.getChatId(), requestDTO.getWorkflowId());
        tokenValidation(context);
        approvalForumService.updateChairpersonLastSeenChatId(requestDTO.getWorkflowId(), requestDTO.getChatId(), context.getUserPrincipal().getName());
        return Response.ok().build();
    }

    @GET
    @ApiOperation(value = "Get environment", authorizations = @Authorization(value = "Bearer"))
    @Path("/property/environment")
    @Produces("application/json")
    public Response getEnvironment() {
        logger.info("getEnvironment()");
        tokenValidation(context);
        List<Property> properties = new ArrayList<>();
        properties.add(new Property().withName("af.environment").withValue(afSettings.get("af.environment")));
        return Response.ok(new PropertiesDTO().withProperties(properties)).build();
    }

    @GET
    @Path("/user/preference")
    @Produces("application/json")
    @ApiOperation(value = "Get user preferences", authorizations = @Authorization(value = "Bearer"))
    @Transactional
    public Response getUserPreference() {
        logger.info("getUserPreference()");
        tokenValidation(context);
        return Response.ok(approvalForumService.getUserPreference(context.getUserPrincipal().getName())).build();
    }

    @PUT
    @ApiOperation(value = "Update user preference", authorizations = @Authorization(value = "Bearer"))
    @Path("/user/preference")
    @Produces("application/json")
    @Transactional
    public Response updateUserPreference(UpdateUserPreferenceRequestDTO requestDTO) {
        logger.info("updateUserPreference({}, {})", requestDTO.getUserPreference().getUserId(), requestDTO.getUserPreference().isChatSortAscending());
        tokenValidation(context);
        approvalForumService.updateUserPreference(requestDTO.getUserPreference().getUserId(), requestDTO.getUserPreference().isChatSortAscending());
        return Response.ok().build();
    }

}
