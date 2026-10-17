package za.co.rmb.credit.daf.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Verifies a user token the auth service issued for DAF's registered system: the token the Credit shell
 * exchanges its platform sign-in for ({@code POST /auth/token/exchange {systemCode: DAF}}).
 *
 * <p>Same rules as the auth service's {@code authservice-token-verifier} library: HMAC signature with DAF's
 * JWT signing secret, expiry, {@code systemCode} = DAF and, when {@code AF_PLATFORM_TOKEN_ISSUER} is set, the
 * issuer. The signing secret is fetched with DAF's bootstrap client secret ({@code POST <auth>/api/v1/auth/bootstrap},
 * the {@code AF_SYSTEM_*} sign-in DAF already uses for its system tokens) and fetched again, at most every
 * 30 seconds, when it is missing or a signature fails (secret rotation). System tokens are refused: only a
 * signed-in user gets a DAF session. The library itself is not used because it needs JJWT 0.12, and DAF's
 * legacy token code is written against JJWT 0.11.</p>
 */
@Component
public class PlatformTokenVerifier {
    private static final Logger logger = LoggerFactory.getLogger(PlatformTokenVerifier.class);
    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(30);
    private static final String SYSTEM_TOKEN_TYPE = "SYSTEM";

    /** The user a verified token names: the AD domain and account the auth service signed in. */
    public record PlatformUser(String domain, String accountName) { }

    private final Map<String, String> afSettings;
    private final Duration refreshInterval;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile SecretKey key;
    private volatile Instant lastFetch = Instant.EPOCH;

    @Autowired
    public PlatformTokenVerifier(@Qualifier("af_settings") Map<String, String> afSettings) {
        this(afSettings, REFRESH_INTERVAL);
    }

    PlatformTokenVerifier(Map<String, String> afSettings, Duration refreshInterval) {
        this.afSettings = afSettings;
        this.refreshInterval = refreshInterval;
    }

    /** True when DAF can verify platform tokens at all: its system code, client secret and the auth service. */
    public boolean configured() {
        return !blank(systemCode()) && !blank(setting("af.system.clientSecret")) && !blank(setting("af.authservice.base"));
    }

    /** The user the token names, or empty when it is not a valid user token for DAF. */
    public Optional<PlatformUser> verify(String token) {
        if (blank(token) || !configured()) return Optional.empty();
        if (key == null && due()) refresh();
        if (key == null) return Optional.empty();
        try {
            return user(parse(token, key));
        } catch (SignatureException e) {
            if (due() && refresh()) {
                try {
                    return user(parse(token, key));
                } catch (JwtException | IllegalArgumentException retry) {
                    logger.debug("Platform token rejected after key refresh: {}", retry.getMessage());
                    return Optional.empty();
                }
            }
            logger.debug("Platform token rejected: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Platform token rejected: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Claims parse(String token, SecretKey signingKey) {
        JwtParserBuilder parser = Jwts.parserBuilder().setSigningKey(signingKey);
        String issuer = System.getenv("AF_PLATFORM_TOKEN_ISSUER");
        if (!blank(issuer)) parser = parser.requireIssuer(issuer.trim());
        return parser.build().parseClaimsJws(token).getBody();
    }

    private Optional<PlatformUser> user(Claims claims) {
        String systemCode = claims.get("systemCode", String.class);
        if (systemCode == null || !systemCode().equalsIgnoreCase(systemCode)) {
            logger.debug("Platform token for system {} rejected (expected {})", systemCode, systemCode());
            return Optional.empty();
        }
        if (SYSTEM_TOKEN_TYPE.equals(claims.get("tokenType", String.class))) {
            logger.debug("Platform token rejected: a system token names no user");
            return Optional.empty();
        }
        String domain = claims.get("domain", String.class);
        String accountName = claims.get("accountName", String.class);
        if (blank(accountName)) accountName = claims.getSubject();
        if (blank(domain) || blank(accountName)) return Optional.empty();
        return Optional.of(new PlatformUser(domain.trim(), accountName.trim()));
    }

    private boolean due() {
        return !Instant.now().isBefore(lastFetch.plus(refreshInterval));
    }

    /** Fetches DAF's signing secret from the auth service; true when it was fetched. */
    private synchronized boolean refresh() {
        lastFetch = Instant.now();
        String url = setting("af.authservice.base").trim().replaceAll("/+$", "") + "/api/v1/auth/bootstrap";
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "systemCode", systemCode(), "clientSecret", setting("af.system.clientSecret").trim()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode node = blank(response.body()) ? null : mapper.readTree(response.body());
            String secret = node == null ? null : node.path("jwtSigningSecret").asText(null);
            if (response.statusCode() / 100 != 2 || node == null || !"success".equals(node.path("status").asText())
                    || blank(secret)) {
                logger.warn("Platform token key for {} not issued by {} ({}): platform sign-in refused until it is",
                        systemCode(), url, node == null ? "HTTP " + response.statusCode() : node.path("reason").asText(""));
                return false;
            }
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            logger.info("Platform token key for {} fetched from {}", systemCode(), url);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            logger.warn("Platform token key for {} could not be fetched from {}: {}", systemCode(), url, e.toString());
            return false;
        }
    }

    private String systemCode() {
        return setting("af.system.caller").trim();
    }

    private String setting(String name) {
        String value = afSettings.get(name);
        return value == null ? "" : value;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
