package za.co.rmb.approvalforum.security.service;

public interface AuthenticationTokenSettings {

    /**
     * Secret for signing and verifying the token signature.
     */
    String getSecret();

    /**
     * Allowed clock skew for verifying the token signature (in seconds).
     */
    Long getClockSkew();

    /**
     * Identifies the recipients that the JWT token is intended for.
     */
    String getAudience();

    /**
     * Identifies the JWT token issuer.
     */
    String getIssuer();

    /**
     * JWT claim for the token refreshment count.
     */
    String getRefreshCountClaimName();

    /**
     * JWT claim for the maximum times that a token can be refreshed.
     */
    String getRefreshLimitClaimName();

    /**
     * Identifies the recipients that the JWT token is intended for in Redhat Single Sign On.
     */
    String getSsoAudience();

    /**
     * Get Redhat Single Sign Public Key in Base64
     */
    String getSsoPublicKey();

    /**
     * Get Redhat Single Sign client id
     */
    String getSsoClientId();
}
