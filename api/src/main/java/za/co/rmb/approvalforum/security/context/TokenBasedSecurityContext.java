package za.co.rmb.approvalforum.security.context;

import za.co.rmb.approvalforum.security.filter.AuthenticatedUserDetails;
import za.co.rmb.approvalforum.security.model.AuthenticationTokenDetails;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * {@link SecurityContext} implementation for token-based authentication.
 *
 * @author cassiomolin
 */
public class TokenBasedSecurityContext implements SecurityContext {

    private AuthenticatedUserDetails authenticatedUserDetails;
    private AuthenticationTokenDetails authenticationTokenDetails;
    private final boolean secure;

    public TokenBasedSecurityContext(AuthenticationTokenDetails authenticationTokenDetails, boolean secure) {
        this.authenticatedUserDetails = new AuthenticatedUserDetails(authenticationTokenDetails.getUsername(), authenticationTokenDetails.getRoles());
        this.authenticationTokenDetails = authenticationTokenDetails;
        this.secure = secure;
    }

    public TokenBasedSecurityContext(AuthenticatedUserDetails authenticatedUserDetails, AuthenticationTokenDetails authenticationTokenDetails, boolean secure) {
        this.authenticatedUserDetails = authenticatedUserDetails;
        this.authenticationTokenDetails = authenticationTokenDetails;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return authenticatedUserDetails;
    }

    @Override
    public boolean isUserInRole(String s) {
        return authenticatedUserDetails.getRoles().stream().anyMatch(role -> role.toLowerCase().equals(s.toLowerCase()));
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }

    public AuthenticationTokenDetails getAuthenticationTokenDetails() {
        return authenticationTokenDetails;
    }

}
