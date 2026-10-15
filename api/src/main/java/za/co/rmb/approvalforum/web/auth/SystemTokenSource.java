package za.co.rmb.approvalforum.web.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived system tokens from the auth service for DAF's calls where no user is signed in: DAF proves
 * itself as its registered system ({@code af.system.caller} + {@code af.system.clientSecret})
 * at {@code af.system.tokenUrl} and receives a token for the audience system (MAILSERVICE,
 * CREDITDATA). The auth service must list DAF for that audience ({@code AUTH_SYSTEM_TOKENS_<AUDIENCE>}).
 * Each audience's token is cached until shortly before it expires.
 */
public class SystemTokenSource {
    private static final Logger logger = LoggerFactory.getLogger(SystemTokenSource.class);
    private static final long REFRESH_MARGIN_MS = 60_000L;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 20000;

    private final Map<String, String> afSettings;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    private record CachedToken(String token, long expiresAtMs) { }

    public SystemTokenSource(Map<String, String> afSettings) {
        this.afSettings = afSettings;
    }

    /** True when DAF's system credentials and the token URL are all set. */
    public boolean isConfigured() {
        return !blank(setting("af.system.tokenUrl")) && !blank(setting("af.system.caller"))
                && !blank(setting("af.system.clientSecret"));
    }

    /** A token for the audience system, or null when the auth service did not issue one. */
    public synchronized String token(String audience) {
        long now = System.currentTimeMillis();
        CachedToken cached = cache.get(audience);
        if (cached != null && now < cached.expiresAtMs()) {
            return cached.token();
        }
        String url = setting("af.system.tokenUrl").trim();
        Map<String, String> request = new LinkedHashMap<>();
        request.put("systemCode", setting("af.system.caller").trim());
        request.put("clientSecret", setting("af.system.clientSecret").trim());
        request.put("audience", audience);

        Client client = ClientBuilder.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
        try {
            logger.info("System token: POST {} for {}", url, audience);
            Response response = client.target(url).request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(mapper.writeValueAsString(request), MediaType.APPLICATION_JSON));
            String text = response.readEntity(String.class);
            JsonNode node = text == null || text.isEmpty() ? null : mapper.readTree(text);
            String token = node == null ? null : node.path("token").asText(null);
            if (response.getStatus() != 200 || blank(token)) {
                logger.warn("System token for {} refused ({}): {}", audience, response.getStatus(),
                        node == null ? "" : node.path("reason").asText(""));
                return null;
            }
            long ttlMs = node.path("expiresInSeconds").asLong(900) * 1000L;
            cache.put(audience, new CachedToken(token, now + Math.max(REFRESH_MARGIN_MS, ttlMs - REFRESH_MARGIN_MS)));
            return token;
        } catch (Exception ex) {
            logger.error("System token for {} failed: {}", audience, ex.toString(), ex);
            return null;
        } finally {
            client.close();
        }
    }

    private String setting(String key) {
        String value = afSettings.get(key);
        return value == null ? "" : value;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
