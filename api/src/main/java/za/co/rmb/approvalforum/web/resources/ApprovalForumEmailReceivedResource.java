package za.co.rmb.approvalforum.web.resources;

import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.service.ApprovalForumEmailReceived;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

@Api(tags = "emailReceived")
@Path("/emailReceived/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumEmailReceivedResource {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumEmailReceivedResource.class);

    @Context
    private SecurityContext context;

    private ApprovalForumEmailReceived approvalForumEmailReceived;

    @Inject
    public ApprovalForumEmailReceivedResource(final ApprovalForumEmailReceived approvalForumEmailReceived) {
        this.approvalForumEmailReceived = approvalForumEmailReceived;
    }
    @GET
    @ApiOperation(value = "Ping service")
    @Path("/ping")
    @Produces({MediaType.APPLICATION_JSON})
    public Response ping() {
        logger.info("Ping service");
        return Response.ok().entity("pong").build();
    }

    @GET
    @ApiOperation(value = "Process Email Received for approval",
            authorizations = @Authorization(value = "Bearer"))
    @Path("/process")
    @Produces({MediaType.APPLICATION_JSON})
    @Transactional
    public Response process() {
        logger.info("Process Email Received...");
        tokenValidation(context);
        try{
            approvalForumEmailReceived.processIncomingEmails();
            return Response.ok().build();
        }catch(Exception e){
            e.printStackTrace();
            return Response.serverError().build();
        }
    }

}
