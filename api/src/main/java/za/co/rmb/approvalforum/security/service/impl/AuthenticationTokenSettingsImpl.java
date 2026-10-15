package za.co.rmb.approvalforum.security.service.impl;


import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.rmb.approvalforum.security.service.AuthenticationTokenSettings;

/**
 * Settings for signing and verifying JWT tokens.
 *
 * @author cassiomolin
 */
public class AuthenticationTokenSettingsImpl implements AuthenticationTokenSettings {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationTokenSettings.class);

    private final ByteSource appProps = Resources.asByteSource(Resources.getResource("application.properties"));

    private String getProperty(String name) {
        try {
            Properties properties = new Properties();
            try (InputStream resourceStream = appProps.openStream()) {
                properties.load(resourceStream);
                return properties.getProperty(name);
            }
        }catch(Exception e) {
            logger.error("Caught exception", e);
        }
        return null;
    }

    private String getEnvProperty(String name) {
        try {
            // A blank environment value (an empty line in env.properties) counts as not set.
            String value = System.getenv(name);
            return value == null || value.isBlank() ? getProperty(name) : value;
        }catch(Exception e) {
           // logger.error("Caught exception", e);
        }
        return null;
    }

    /**
     * Secret for signing and verifying the token signature.
     */
    @Override
    public String getSecret() {
        return getEnvProperty("authentication.jwt.secret");
    }

    /**
     * Allowed clock skew for verifying the token signature (in seconds).
     */
    @Override
    public Long getClockSkew() {
        return Long.parseLong(getProperty("authentication.jwt.clockSkew"));
    }

    /**
     * Identifies the recipients that the JWT token is intended for.
     */
    @Override
    public String getAudience() {
        return getProperty("authentication.jwt.audience");
    }

    /**
     * Identifies the JWT token issuer.
     */
    @Override
    public String getIssuer() {
        return getProperty("authentication.jwt.issuer");
    }

    /**
     * JWT claim for the token refreshment count.
     */
    @Override
    public String getRefreshCountClaimName() {
        return getProperty("authentication.jwt.claimNames.refreshCount");
    }

    /**
     * JWT claim for the maximum times that a token can be refreshed.
     */
    @Override
    public String getRefreshLimitClaimName() {
        return getProperty("authentication.jwt.claimNames.refreshLimit");
    }

    /**
     * Get Redhat Single Sign Public Key in Base64
     */
    @Override
    public String getSsoPublicKey() {
        return getEnvProperty("LMS_RHSSO_PUBKEY");
    }

    /**
     * Identifies the recipients that the JWT token is intended for Redhat Single Sign On
     */
    @Override
    public String getSsoAudience() {
        return getEnvProperty("LMS_RHSSO_AUDIENCE");
    }

    /**
     * Get Redhat Single Sign client id
     */
    @Override
    public String getSsoClientId() {
        return getEnvProperty("LMS_RHSSO_CLIENTID");
    }
}
