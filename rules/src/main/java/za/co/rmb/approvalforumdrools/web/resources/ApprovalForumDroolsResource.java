package za.co.rmb.approvalforumdrools.web.resources;

import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforumdrools.model.RulesDataResponeDTO;
import za.co.rmb.approvalforumdrools.web.resources.util.ApprovalForumDroolsResourceUtil;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesRequestDTO;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import static za.co.rmb.approvalforumdrools.security.util.SecurityHelper.tokenValidation;

@SwaggerDefinition(
        info = @Info(
                description = "",
                version = "1.0.0",
                title = "APPROVAL FORUM Drools REST API",
                termsOfService = "",
                contact = @Contact(
                        name = "Arnold Human",
                        email = "arnold.human@rmb.co.za",
                        url = ""
                )
        ),
        consumes = {"application/json"},
        produces = {"application/json"},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        tags = {
                @Tag(name = "approvalforumdrools", description = ""),
                @Tag(name = "git", description = "")
        },
        securityDefinition = @SecurityDefinition(
                apiKeyAuthDefintions = { @ApiKeyAuthDefinition(
                        key = "Bearer",
                        name = "Authorization",
                        in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER
                ) }
        )
)
@Api(tags = "approvalForumDrools")
@Path("/approvalForumDrools/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumDroolsResource extends ApprovalForumDroolsResourceUtil {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumDroolsResource.class);

    @Context
    private SecurityContext context;

    @POST
    @Path("/approvalForum")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(value = "Provide rules for Approval Forum", authorizations = @Authorization(value = "Bearer"))
    public Response approvalForum(ApprovalForumRulesRequestDTO requestDTO) {
        logger.info("approvalForumDrools/approvalForum()");
        tokenValidation(context);
        return processApprovalForumRules(requestDTO);
    }

    @POST
    @Path("/rules/reload")
    @Consumes("application/json")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Upload new rules and reload rules engine", authorizations = @Authorization(value = "Bearer"))
    public Response updateRules(@DefaultValue("") @QueryParam("md5sum") String md5sum, @DefaultValue("") @QueryParam("filePath") String path) {
        logger.info("updateRules({}, {})", md5sum, path);
        tokenValidation(context);
        RulesDataResponeDTO responeDTO = new RulesDataResponeDTO();
        try {
            processReloadRules(md5sum, path, context.getUserPrincipal().getName());
            responeDTO.setSuccess(true);
            responeDTO.setMessage("");
            return Response.ok(responeDTO).build();
        }catch(Exception e){
            responeDTO.setSuccess(false);
            responeDTO.setMessage(e.getMessage());
            return Response.ok(responeDTO).build();
        }
    }

}
