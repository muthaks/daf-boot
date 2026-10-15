package za.co.rmb.approvalforum.security.service;

import za.co.rmb.approvalforum.security.model.AuthenticationTokenDetails;

public interface AuthenticationTokenIssuer {

    /**
     * Issue a JWT token
     *
     * @param authenticationTokenDetails
     * @return
     */
    String issueToken(AuthenticationTokenDetails authenticationTokenDetails);
}
