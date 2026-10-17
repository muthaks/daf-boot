package za.co.rmb.credit.daf.platform;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.rmb.approvalforum.model.WindowsAuthDetail;
import za.co.rmb.approvalforum.web.resources.ApprovalForumAuthResource;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@code POST /auth/platformToken}: a DAF session for a user already signed in to the Credit platform, so the
 * DAF and Agenda Builder remotes hosted in the shell need no second sign-in.
 *
 * <p>The bearer is the auth-service token the shell exchanged for DAF ({@link PlatformTokenVerifier}). The
 * user's details and groups are then read from the auth service by DAF itself, never taken from the caller,
 * and DAF's own {@code /auth/tenant} and {@code /auth/ssoToken} logic issues the tenant and the DAF role token
 * exactly as for a Windows SSO sign-in (a blank host domain makes it read the groups from the auth service).</p>
 *
 * <p>Answers {@code {"Token": "<DAF token>", "tenantId": 1|2|null, "domain": "<AD domain>", "displayName": ...}},
 * or 401 with {@code {"reason": ...}}. Domain and display name are what Windows SSO would describe.</p>
 */
@Component
@Path("/" + PlatformTokenResource.PATH)
public class PlatformTokenResource {
    private static final Logger logger = LoggerFactory.getLogger(PlatformTokenResource.class);
    static final String PATH = "auth/platformToken";

    private final PlatformTokenVerifier verifier;
    private final AuthServiceClient authClient;
    private final ObjectProvider<ApprovalForumAuthResource> authResources;

    public PlatformTokenResource(PlatformTokenVerifier verifier, AuthServiceClient authClient,
                                 ObjectProvider<ApprovalForumAuthResource> authResources) {
        this.verifier = verifier;
        this.authClient = authClient;
        this.authResources = authResources;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response platformToken(@Context ContainerRequestContext request) {
        if (!verifier.configured()) {
            return refuse("Platform sign-in is not configured for DAF (AF_SYSTEM_CALLER, AF_SYSTEM_BOOTSTRAP_CLIENT_SECRET, AF_AUTHSERVICE_BASE).");
        }
        Object bearer = request.getProperty(PlatformTokenHeaderFilter.PLATFORM_TOKEN);
        Optional<PlatformTokenVerifier.PlatformUser> user =
                bearer instanceof String token ? verifier.verify(token) : Optional.empty();
        if (user.isEmpty()) {
            return refuse("A platform token for DAF is required.");
        }

        ActiveDirectoryUserModel adUser;
        try {
            adUser = authClient.User(user.get().domain(), user.get().accountName());
        } catch (RuntimeException e) {
            logger.warn("platformToken: AD lookup for {}\\{} failed: {}", user.get().domain(), user.get().accountName(), e.toString());
            adUser = null;
        }
        if (adUser == null || adUser.getUserPrincipalName() == null || adUser.getUserPrincipalName().isBlank()) {
            return refuse("The signed-in user was not found in Active Directory.");
        }
        logger.info("platformToken: {}", adUser.getUserPrincipalName());

        WindowsAuthDetail detail = new WindowsAuthDetail();
        detail.setUserPrincipalName(adUser.getUserPrincipalName());
        detail.setSamAccountName(adUser.getsAMAccountName());
        detail.setUsername(adUser.getsAMAccountName());
        detail.setDomain(adUser.getDomain());
        detail.setDisplayName(adUser.getDisplayName());
        detail.setName(adUser.getName());
        detail.setGivenName(adUser.getGivenName());
        detail.setSurname(adUser.getSn());
        detail.setEmailAddress(adUser.getMail());
        detail.setEmployeeId(adUser.getEmployeeId());
        detail.setDistinguishedName(adUser.getDistinguishedName());

        ApprovalForumAuthResource auth = authResources.getObject();
        // As the remotes do after a Windows SSO sign-in: a tenant DAF cannot determine leaves it unset
        Object tenant;
        try {
            tenant = auth.ssoTenant(detail).getEntity();
        } catch (RuntimeException e) {
            logger.warn("platformToken: tenant for {} not determined: {}", adUser.getUserPrincipalName(), e.toString());
            tenant = null;
        }
        Object token = auth.ssoToken(detail).getEntity();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Token", token instanceof Map<?, ?> map ? map.get("Token") : null);
        body.put("tenantId", tenant instanceof Map<?, ?> map ? map.get("tenantId") : null);
        body.put("domain", adUser.getDomain());
        body.put("displayName", adUser.getDisplayName());
        return Response.ok(body).build();
    }

    private static Response refuse(String reason) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("reason", reason))
                .build();
    }
}
