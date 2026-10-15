package za.co.rmb.approvalforum.web.resources;

import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.service.ApprovalForumDocstashService;
import za.co.rmb.approvalforum.service.exception.ResourceException;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

@Api(tags = "docstash")
@Path("/docstash/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumDocstashResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumDocstashResource.class);

    private ApprovalForumDocstashService approvalForumDocstashService;

    @Context
    private SecurityContext context;

    @Inject
    public ApprovalForumDocstashResource(ApprovalForumDocstashService approvalForumDocstashService){
        this.approvalForumDocstashService = approvalForumDocstashService;
    }

    @POST
    @Path("/upload")
    @Produces("application/json")
    @ApiOperation(value = "Upload workflow attachments to docstash", authorizations = @Authorization(value = "Bearer"))
    public Response uploadDocuments(@DefaultValue("0") @QueryParam("workflowId") int workflowId) {
        logger.info("uploadDocuments()");
        try {
            approvalForumDocstashService.uploadDocumentsToDocstash(workflowId);
            return Response.ok().build();
        }catch(ResourceException e){
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Process incomplete docstash requests",
            authorizations = @Authorization(value = "Bearer"))
    @Path("/process")
    @Produces("application/json")
    @Transactional
    public Response processDocStashRequests() {
        tokenValidation(context);
        try{
            approvalForumDocstashService.processDocStashRequests();
            return Response.ok().build();
        }catch(Exception e){
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

    @POST
    @ApiOperation(value = "Cleanup ADU for Docstash upload",
            authorizations = @Authorization(value = "Bearer"))
    @Path("/adu/cleanup")
    @Produces("application/json")
    @Transactional
    public Response aduCleanupProcess() {
        tokenValidation(context);
        try{
//            approvalForumDocstashService.processAduCleanup();
            return Response.ok().build();
        }catch(Exception e){
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

}
