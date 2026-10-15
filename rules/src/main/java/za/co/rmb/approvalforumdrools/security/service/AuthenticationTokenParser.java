package za.co.rmb.approvalforumdrools.security.service;

import za.co.rmb.approvalforumdrools.security.model.AuthenticationTokenDetails;

public interface AuthenticationTokenParser {
    /**
     * Parse a JWT token.
     *
     * @param token
     * @return
     */
    public AuthenticationTokenDetails parseToken(String token);

    /**
     * Parse a Redhat Single Sign On JWT token.
     *
     * @param token
     * @return
     */
    public AuthenticationTokenDetails parseSsoToken(String token);
}

