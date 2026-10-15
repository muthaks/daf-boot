package za.co.rmb.approvalforum.security.util;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.SecurityContext;

public class SecurityHelper {

    public static void tokenValidation(SecurityContext context) {
        if(context == null) {
            throw new NotAuthorizedException("Require token");
        }
        else {
            if(context.getUserPrincipal() == null) {
                throw new NotAuthorizedException("Require token");
            }
            /*else {
                AuthenticationTokenDetails tokenDetails = (AuthenticationTokenDetails)context.getUserPrincipal();
                //logTokenUsage(tokenDetails.getId());
                //boolean isTokenRevoked = schedulerRepository.isTokenRevoked(tokenDetails.getId());
                if(isTokenRevoked) {
                    throw new NotAuthorizedException("Token is revoked");
                }
            }*/
        }
    }
}
