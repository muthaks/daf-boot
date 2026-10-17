package za.co.rmb.credit.daf.platform;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import za.co.rmb.approvalforum.model.WindowsAuthDetail;
import za.co.rmb.approvalforum.web.resources.ApprovalForumAuthResource;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@code POST /auth/platformToken} and the filter that hands it the platform bearer. */
class PlatformTokenResourceTest {
    private PlatformTokenVerifier verifier;
    private AuthServiceClient authClient;
    private ApprovalForumAuthResource authResource;
    private PlatformTokenResource resource;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        verifier = mock(PlatformTokenVerifier.class);
        authClient = mock(AuthServiceClient.class);
        authResource = mock(ApprovalForumAuthResource.class);
        ObjectProvider<ApprovalForumAuthResource> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(authResource);
        when(verifier.configured()).thenReturn(true);
        resource = new PlatformTokenResource(verifier, authClient, provider);
    }

    private static ContainerRequestContext request(String bearerProperty) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getProperty(PlatformTokenHeaderFilter.PLATFORM_TOKEN)).thenReturn(bearerProperty);
        return request;
    }

    private static ActiveDirectoryUserModel adUser() {
        ActiveDirectoryUserModel user = mock(ActiveDirectoryUserModel.class);
        when(user.getUserPrincipalName()).thenReturn("superuser@rmb.co.za");
        when(user.getsAMAccountName()).thenReturn("superuser");
        when(user.getDomain()).thenReturn("RMB");
        when(user.getDisplayName()).thenReturn("Super User");
        when(user.getGivenName()).thenReturn("Super");
        when(user.getSn()).thenReturn("User");
        return user;
    }

    @Test
    void issuesDafsTokenAndTenantForAVerifiedUser() {
        when(verifier.verify("t")).thenReturn(Optional.of(new PlatformTokenVerifier.PlatformUser("RMB", "superuser")));
        ActiveDirectoryUserModel user = adUser();
        when(authClient.User("RMB", "superuser")).thenReturn(user);
        when(authResource.ssoTenant(any())).thenReturn(Response.ok(Map.of("tenantId", 2)).build());
        when(authResource.ssoToken(any())).thenReturn(Response.ok(Map.of("Token", "daf-token")).build());

        Response response = resource.platformToken(request("t"));

        assertEquals(200, response.getStatus());
        assertEquals(Map.of("Token", "daf-token", "tenantId", 2, "domain", "RMB", "displayName", "Super User"),
                response.getEntity());
        ArgumentCaptor<WindowsAuthDetail> detail = ArgumentCaptor.forClass(WindowsAuthDetail.class);
        verify(authResource).ssoToken(detail.capture());
        assertEquals("superuser@rmb.co.za", detail.getValue().getUserPrincipalName());
        assertEquals("RMB", detail.getValue().getDomain());
        assertEquals("Super User", detail.getValue().getDisplayName());
        // A blank host domain: DAF reads the groups from the auth service, never from the caller
        assertNull(detail.getValue().getHostDomain());
        assertTrue(detail.getValue().getGroups() == null || detail.getValue().getGroups().isEmpty());
    }

    @Test
    void leavesTheTenantUnsetWhenDafCannotDetermineIt() {
        when(verifier.verify("t")).thenReturn(Optional.of(new PlatformTokenVerifier.PlatformUser("RMB", "superuser")));
        ActiveDirectoryUserModel user = adUser();
        when(authClient.User("RMB", "superuser")).thenReturn(user);
        when(authResource.ssoTenant(any())).thenThrow(new NullPointerException());
        when(authResource.ssoToken(any())).thenReturn(Response.ok(Map.of("Token", "daf-token")).build());

        Response response = resource.platformToken(request("t"));

        assertEquals(200, response.getStatus());
        Map<String, Object> expected = new HashMap<>();
        expected.put("Token", "daf-token");
        expected.put("tenantId", null);
        expected.put("domain", "RMB");
        expected.put("displayName", "Super User");
        assertEquals(expected, response.getEntity());
    }

    @Test
    void refusesARequestWithoutAValidToken() {
        when(verifier.verify(any())).thenReturn(Optional.empty());
        assertEquals(401, resource.platformToken(request("bad")).getStatus());
        assertEquals(401, resource.platformToken(request(null)).getStatus());
        verify(authResource, never()).ssoToken(any());
    }

    @Test
    void refusesAUserActiveDirectoryDoesNotKnow() {
        when(verifier.verify("t")).thenReturn(Optional.of(new PlatformTokenVerifier.PlatformUser("RMB", "gone")));
        when(authClient.User("RMB", "gone")).thenReturn(null);
        assertEquals(401, resource.platformToken(request("t")).getStatus());
        verify(authResource, never()).ssoToken(any());
    }

    @Test
    void refusesWhenPlatformSignInIsNotConfigured() {
        when(verifier.configured()).thenReturn(false);
        assertEquals(401, resource.platformToken(request("t")).getStatus());
        verify(verifier, never()).verify(any());
    }

    private static ContainerRequestContext filtered(String path, String authorization) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uri = mock(UriInfo.class);
        when(uri.getPath()).thenReturn(path);
        when(request.getUriInfo()).thenReturn(uri);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (authorization != null) headers.putSingle(HttpHeaders.AUTHORIZATION, authorization);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(authorization);
        new PlatformTokenHeaderFilter().filter(request);
        return request;
    }

    @Test
    void filterMovesThePlatformBearerOffTheAuthorizationHeader() {
        ContainerRequestContext request = filtered("/auth/platformToken", "Bearer abc");
        verify(request).setProperty(PlatformTokenHeaderFilter.PLATFORM_TOKEN, "abc");
        assertFalse(request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void filterLeavesEveryOtherRequestAlone() {
        ContainerRequestContext request = filtered("auth/ssoToken", "Bearer abc");
        verify(request, never()).setProperty(any(), any());
        assertTrue(request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION));
    }
}
