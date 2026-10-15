package za.co.rmb.approvalforumdrools.security.filter;

import za.co.rmb.approvalforumdrools.security.context.TokenBasedSecurityContext;
import za.co.rmb.approvalforumdrools.security.model.AuthenticationTokenDetails;
import za.co.rmb.approvalforumdrools.security.service.AuthenticationTokenService;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * JWT authentication filter.
 *
 * @author cassiomolin
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationSsoFilter implements ContainerRequestFilter {

    @Inject
    private AuthenticationTokenService authenticationTokenService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        String bearer = "Bearer ";
        if (authorizationHeader != null && authorizationHeader.startsWith(bearer)) {
            String authenticationToken = authorizationHeader.substring(bearer.length());
            handleTokenBasedAuthentication(authenticationToken, requestContext);
        } else {
            SecurityContext securityContext = new TokenBasedSecurityContext(null, false);
            requestContext.setSecurityContext(securityContext);
        }

        // Other authentication schemes (such as Basic) could be supported
    }

    private void handleTokenBasedAuthentication(String authenticationToken, ContainerRequestContext requestContext) {

        AuthenticationTokenDetails authenticationTokenDetails = authenticationTokenService.parseSsoToken(authenticationToken);

        boolean isSecure = requestContext.getSecurityContext().isSecure();

        SecurityContext securityContext = new TokenBasedSecurityContext(authenticationTokenDetails, isSecure);
        requestContext.setSecurityContext(securityContext);
    }
}
