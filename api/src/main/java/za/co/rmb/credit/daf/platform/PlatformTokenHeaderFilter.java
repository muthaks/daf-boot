package za.co.rmb.credit.daf.platform;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

/**
 * On {@code POST /auth/platformToken} only, moves the bearer (an auth-service token, not a DAF token) from the
 * Authorization header into a request property before DAF's legacy {@code AuthenticationFilter} runs, which
 * would otherwise try to parse it as a DAF token. Every other request is left untouched.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class PlatformTokenHeaderFilter implements ContainerRequestFilter {

    /** Request property holding the platform bearer for {@link PlatformTokenResource}. */
    static final String PLATFORM_TOKEN = PlatformTokenHeaderFilter.class.getName() + ".token";
    private static final String BEARER = "Bearer ";

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath().replaceAll("^/+", "").replaceAll("/+$", "");
        if (!PlatformTokenResource.PATH.equals(path)) return;
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        request.getHeaders().remove(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            request.setProperty(PLATFORM_TOKEN, header.substring(BEARER.length()).trim());
        }
    }
}
