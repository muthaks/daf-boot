package za.co.rmb.credit.daf.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.stereotype.Component;
import za.co.rmb.approvalforum.security.filter.AuthenticationFilter;
import za.co.rmb.approvalforum.web.resources.ApprovalForumAgendaResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumCreditDataResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumAuthResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumDocstashResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumDocumentsResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumEmailReceivedResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumExternalResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumResource;
import za.co.rmb.approvalforum.web.resources.ApprovalForumSchedulerResource;
import za.co.rmb.approvalforum.web.resources.GitResource;
import za.co.rmb.credit.daf.jaxrs.multipart.MultipartFormDataInputReader;
import za.co.rmb.credit.daf.platform.PlatformTokenHeaderFilter;
import za.co.rmb.credit.daf.platform.PlatformTokenResource;
import za.co.rmb.credit.daf.support.RestEasyBehaviour;

/**
 * The JAX-RS application: what the legacy web.xml and ApprovalForumWebResourcesModule registered with
 * RESTEasy under {@code /api}. With the {@code /approvalforum} context path the URLs are the legacy ones,
 * {@code /approvalforum/api/...}.
 *
 * <p>Only {@link AuthenticationFilter} is registered, as in legacy: the legacy AuthorizationFilter was
 * never bound, and resources check the caller themselves. The platform sign-in ({@code /auth/platformToken})
 * is the one addition: its filter only takes that path's bearer away from AuthenticationFilter.</p>
 */
@Component
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(ApprovalForumResource.class);
        register(ApprovalForumSchedulerResource.class);
        register(ApprovalForumDocstashResource.class);
        register(ApprovalForumDocumentsResource.class);
        register(ApprovalForumExternalResource.class);
        register(ApprovalForumEmailReceivedResource.class);
        register(GitResource.class);
        register(ApprovalForumAuthResource.class);
        register(ApprovalForumAgendaResource.class);
        register(ApprovalForumCreditDataResource.class);
        // Not legacy: a DAF session for a user signed in to the Credit platform (auth-service token for DAF)
        register(PlatformTokenResource.class);
        register(PlatformTokenHeaderFilter.class);
        register(AuthenticationFilter.class);
        register(MultiPartFeature.class);
        register(MultipartFormDataInputReader.class);
        RestEasyBehaviour.providers().forEach(this::register);
        // RESTEasy answered an error status without an entity (404, 405, 406, Response.serverError()) with an
        // empty body; Jersey otherwise calls sendError and the servlet container renders its HTML error page
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
    }
}
