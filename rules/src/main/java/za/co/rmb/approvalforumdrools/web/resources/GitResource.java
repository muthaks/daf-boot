package za.co.rmb.approvalforumdrools.web.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforumrules.model.GitRepositoryState;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Properties;

@Api(tags = "git")
@Path("/git/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class GitResource {

    private static final Logger logger = LoggerFactory.getLogger(GitResource.class);

    @Context
    private SecurityContext context;

    @GET
    @Path("/")
    @ApiOperation(value = "Returns git commit information for this build",
            notes = "")
    @Produces("application/json")
    @PermitAll
    public GitRepositoryState getGitRepositoryState() throws IOException
    {
        GitRepositoryState gitRepositoryState = null;
        if (gitRepositoryState == null)
        {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("git.properties"));

            gitRepositoryState = new GitRepositoryState(properties);
        }
        return gitRepositoryState;
    }
}
