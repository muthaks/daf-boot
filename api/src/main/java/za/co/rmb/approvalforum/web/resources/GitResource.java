package za.co.rmb.approvalforum.web.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.GitRepositoryState;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.io.IOException;
import java.util.Properties;

@Api(tags = "git")
@Path("/git")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class GitResource {

    private static final Logger logger = LoggerFactory.getLogger(GitResource.class);

    @GET
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
