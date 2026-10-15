package za.co.rmb.approvalforum.security.service;

import za.co.rmb.approvalforum.security.model.AuthenticationTokenDetails;

public interface AuthenticationTokenService {

    /**
     * Issue a token for a user with the given authorities.
     *
     * @param username
     * @return
     */
    String issueToken(String username);

    /**
     * Parse and validate the token.
     *
     * @param token
     * @return
     */
    AuthenticationTokenDetails parseToken(String token);

    /**
     * Refresh a token.
     *
     * @param currentTokenDetails
     * @return
     */
    String refreshToken(AuthenticationTokenDetails currentTokenDetails);

    /**
     * Parse and validate the Redhat Single Sign On token.
     *
     * @param token
     * @return
     */
    AuthenticationTokenDetails parseSsoToken(String token);
}
