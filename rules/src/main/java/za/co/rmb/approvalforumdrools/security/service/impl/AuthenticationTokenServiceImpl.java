package za.co.rmb.approvalforumdrools.security.service.impl;

import za.co.rmb.approvalforumdrools.security.exception.AuthenticationTokenRefreshmentException;
import za.co.rmb.approvalforumdrools.security.model.AuthenticationTokenDetails;
import za.co.rmb.approvalforumdrools.security.service.AuthenticationTokenIssuer;
import za.co.rmb.approvalforumdrools.security.service.AuthenticationTokenParser;
import za.co.rmb.approvalforumdrools.security.service.AuthenticationTokenService;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Service which provides operations for authentication tokens.
 *
 * @author cassiomolin
 */
public class AuthenticationTokenServiceImpl implements AuthenticationTokenService {

    @Context
    private Configuration configuration;

    @Inject
    private AuthenticationTokenIssuer tokenIssuer;

    @Inject
    private AuthenticationTokenParser tokenParser;

    /**
     * How long the token is valid for (in seconds).
     */
    private Long getValidFor() {
        return (Long)configuration.getProperty("authentication.jwt.validFor");
    }

    /**
     * How many times the token can be refreshed.
     */
    private Integer getRefreshLimit() {
        return (Integer)configuration.getProperty("authentication.jwt.refreshLimit");
    }

    /**
     * Issue a token for a user with the given authorities.
     *
     * @param username
     * @return
     */
    @Override
    public String issueToken(String username) {

        String id = generateTokenIdentifier();
        ZonedDateTime issuedDate = ZonedDateTime.now();
        ZonedDateTime expirationDate = calculateExpirationDate(issuedDate);

        AuthenticationTokenDetails authenticationTokenDetails = new AuthenticationTokenDetails.Builder()
                .withId(id)
                .withUsername(username)
                //.withAuthorities(authorities)
                .withIssuedDate(issuedDate)
                .withExpirationDate(expirationDate)
                .withRefreshCount(0)
                .withRefreshLimit(this.getRefreshLimit())
                .build();

        return tokenIssuer.issueToken(authenticationTokenDetails);
    }

    /**
     * Parse and validate the token.
     *
     * @param token
     * @return
     */
    @Override
    public AuthenticationTokenDetails parseToken(String token) {
        return tokenParser.parseToken(token);
    }

    /**
     * Parse and validate the Redhat Single Sign On token.
     *
     * @param token
     * @return
     */
    @Override
    public AuthenticationTokenDetails parseSsoToken(String token) {
        return tokenParser.parseSsoToken(token);
    }

    /**
     * Refresh a token.
     *
     * @param currentTokenDetails
     * @return
     */
    @Override
    public String refreshToken(AuthenticationTokenDetails currentTokenDetails) {

        if (!currentTokenDetails.isEligibleForRefreshment()) {
            throw new AuthenticationTokenRefreshmentException("This token cannot be refreshed");
        }

        ZonedDateTime issuedDate = ZonedDateTime.now();
        ZonedDateTime expirationDate = calculateExpirationDate(issuedDate);

        AuthenticationTokenDetails newTokenDetails = new AuthenticationTokenDetails.Builder()
                // Reuse the same id
                .withId(currentTokenDetails.getId())
                .withUsername(currentTokenDetails.getUsername())
                .withIssuedDate(issuedDate)
                .withExpirationDate(expirationDate)
                .withRefreshCount(currentTokenDetails.getRefreshCount() + 1)
                .withRefreshLimit(this.getRefreshLimit())
                .build();

        return tokenIssuer.issueToken(newTokenDetails);
    }

    /**
     * Calculate the expiration date for a token.
     *
     * @param issuedDate
     * @return
     */
    private ZonedDateTime calculateExpirationDate(ZonedDateTime issuedDate) {
        return issuedDate.plusSeconds(this.getValidFor());
    }

    /**
     * Generate a token identifier.
     *
     * @return
     */
    private String generateTokenIdentifier() {
        return UUID.randomUUID().toString();
    }

}
