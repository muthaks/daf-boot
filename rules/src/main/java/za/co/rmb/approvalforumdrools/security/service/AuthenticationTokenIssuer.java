package za.co.rmb.approvalforumdrools.security.service;

import za.co.rmb.approvalforumdrools.security.model.AuthenticationTokenDetails;

public interface AuthenticationTokenIssuer {

    /**
     * Issue a JWT token
     *
     * @param authenticationTokenDetails
     * @return
     */
    String issueToken(AuthenticationTokenDetails authenticationTokenDetails);
}
