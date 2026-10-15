package za.co.rmb.approvalforum.web.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.web.auth.SystemTokenSource;
import za.co.rmb.credit.mailservice.model.MailModel;
import za.co.rmb.credit.mailservice.model.MailResponseModel;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;

import jakarta.ws.rs.client.WebTarget;
import java.util.Map;

/**
 * The mail service client with the bearer the platform Mail Service accepts: a short-lived system token
 * for {@code af.mailservice.audience} (default MAILSERVICE), issued by the auth service to DAF's own
 * registered system ({@code af.system.caller} + {@code af.system.clientSecret}) at
 * {@code af.system.tokenUrl}. The token is cached until shortly before it expires.
 *
 * When the system-token settings are not all set, the token each caller passes
 * ({@code af.mailservice.token}) is sent unchanged, as before.
 */
public class SystemTokenMailServiceClient extends MailServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(SystemTokenMailServiceClient.class);
    private static final String DEFAULT_AUDIENCE = "MAILSERVICE";

    private final Map<String, String> afSettings;
    private final SystemTokenSource tokens;

    public SystemTokenMailServiceClient(WebTarget webTarget, Map<String, String> afSettings) {
        super(webTarget);
        this.afSettings = afSettings;
        this.tokens = new SystemTokenSource(afSettings);
    }

    @Override
    public MailResponseModel Send(String token, MailModel mailModel) {
        if (!isConfigured()) {
            return super.Send(token, mailModel);
        }
        String systemToken = getSystemToken();
        if (systemToken == null) {
            MailResponseModel failure = new MailResponseModel();
            failure.setSuccess(false);
            failure.setReason("No mail service token from the auth service");
            return failure;
        }
        return super.Send(systemToken, mailModel);
    }

    boolean isConfigured() {
        return tokens.isConfigured();
    }

    private String getSystemToken() {
        String audience = blank(setting("af.mailservice.audience")) ? DEFAULT_AUDIENCE : setting("af.mailservice.audience").trim();
        return tokens.token(audience);
    }

    private String setting(String key) {
        String value = afSettings.get(key);
        return value == null ? "" : value;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
