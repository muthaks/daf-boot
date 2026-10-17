package za.co.rmb.credit.daf.platform;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verification of auth-service tokens for DAF, against a stand-in for the auth service's bootstrap endpoint. */
class PlatformTokenVerifierTest {
    private static final String SECRET = "daf-signing-secret-of-at-least-sixty-four-bytes-for-hs512-0123456789";
    private static final String ROTATED = "daf-rotated-signing-secret-also-at-least-sixty-four-bytes-long-987654";

    private HttpServer authService;
    private final AtomicReference<String> currentSecret = new AtomicReference<>(SECRET);
    private final AtomicReference<String> expectedClientSecret = new AtomicReference<>("client-secret");
    private final AtomicInteger bootstraps = new AtomicInteger();
    private Map<String, String> settings;

    @BeforeEach
    void startAuthService() throws Exception {
        authService = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        authService.createContext("/auth/api/v1/auth/bootstrap", exchange -> {
            bootstraps.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean valid = body.contains("\"systemCode\":\"DAF\"")
                    && body.contains("\"clientSecret\":\"" + expectedClientSecret.get() + "\"");
            String answer = valid
                    ? "{\"status\":\"success\",\"jwtSigningSecret\":\"" + currentSecret.get() + "\"}"
                    : "{\"status\":\"failure\",\"reason\":\"Invalid client secret\"}";
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(valid ? 200 : 401, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        authService.start();
        settings = new HashMap<>();
        settings.put("af.system.caller", "DAF");
        settings.put("af.system.clientSecret", "client-secret");
        settings.put("af.authservice.base", "http://127.0.0.1:" + authService.getAddress().getPort() + "/auth/");
    }

    @AfterEach
    void stopAuthService() {
        authService.stop(0);
    }

    private PlatformTokenVerifier verifier(Duration refreshInterval) {
        return new PlatformTokenVerifier(settings, refreshInterval);
    }

    private static String token(String secret, Map<String, Object> claims, String subject, Instant expires) {
        return Jwts.builder()
                .setClaims(new HashMap<>(claims))
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expires))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static Map<String, Object> userClaims(String systemCode) {
        return Map.of("systemCode", systemCode, "domain", "RMB", "accountName", "superuser",
                "displayName", "Super User", "groups", List.of("G_CREDIT_X"));
    }

    private static Instant inAnHour() {
        return Instant.now().plusSeconds(3600);
    }

    @Test
    void acceptsAUserTokenIssuedForDaf() {
        Optional<PlatformTokenVerifier.PlatformUser> user =
                verifier(Duration.ofSeconds(30)).verify(token(SECRET, userClaims("DAF"), "superuser", inAnHour()));
        assertEquals(Optional.of(new PlatformTokenVerifier.PlatformUser("RMB", "superuser")), user);
    }

    @Test
    void usesTheSubjectWhenTheTokenHasNoAccountName() {
        Map<String, Object> claims = Map.of("systemCode", "DAF", "domain", "RMB");
        assertEquals(Optional.of(new PlatformTokenVerifier.PlatformUser("RMB", "jdoe")),
                verifier(Duration.ofSeconds(30)).verify(token(SECRET, claims, "jdoe", inAnHour())));
    }

    @Test
    void refusesATokenForAnotherSystem() {
        assertFalse(verifier(Duration.ofSeconds(30))
                .verify(token(SECRET, userClaims("CREDIT-PLATFORM"), "superuser", inAnHour())).isPresent());
    }

    @Test
    void refusesASystemToken() {
        Map<String, Object> claims = Map.of("systemCode", "DAF", "tokenType", "SYSTEM", "callerSystem", "MAILSERVICE");
        assertFalse(verifier(Duration.ofSeconds(30)).verify(token(SECRET, claims, "system:MAILSERVICE", inAnHour())).isPresent());
    }

    @Test
    void refusesAnExpiredToken() {
        assertFalse(verifier(Duration.ofSeconds(30))
                .verify(token(SECRET, userClaims("DAF"), "superuser", Instant.now().minusSeconds(60))).isPresent());
    }

    @Test
    void refusesATokenSignedWithAnotherSecret() {
        assertFalse(verifier(Duration.ofSeconds(30))
                .verify(token(ROTATED, userClaims("DAF"), "superuser", inAnHour())).isPresent());
    }

    @Test
    void refusesGarbageAndBlankTokens() {
        PlatformTokenVerifier verifier = verifier(Duration.ofSeconds(30));
        assertFalse(verifier.verify("not.a.token").isPresent());
        assertFalse(verifier.verify("").isPresent());
        assertFalse(verifier.verify(null).isPresent());
    }

    @Test
    void picksUpARotatedSigningSecret() {
        PlatformTokenVerifier verifier = verifier(Duration.ZERO);
        assertTrue(verifier.verify(token(SECRET, userClaims("DAF"), "superuser", inAnHour())).isPresent());
        currentSecret.set(ROTATED);
        assertTrue(verifier.verify(token(ROTATED, userClaims("DAF"), "superuser", inAnHour())).isPresent());
        assertEquals(2, bootstraps.get());
    }

    @Test
    void fetchesTheKeyOnceAndReusesIt() {
        PlatformTokenVerifier verifier = verifier(Duration.ofSeconds(30));
        verifier.verify(token(SECRET, userClaims("DAF"), "superuser", inAnHour()));
        verifier.verify(token(SECRET, userClaims("DAF"), "superuser", inAnHour()));
        assertEquals(1, bootstraps.get());
    }

    @Test
    void refusesEverythingWhenTheAuthServiceRefusesTheClientSecret() {
        expectedClientSecret.set("another-secret");
        assertFalse(verifier(Duration.ofSeconds(30))
                .verify(token(SECRET, userClaims("DAF"), "superuser", inAnHour())).isPresent());
    }

    @Test
    void isNotConfiguredWithoutTheSystemSignIn() {
        settings.remove("af.system.clientSecret");
        PlatformTokenVerifier verifier = verifier(Duration.ofSeconds(30));
        assertFalse(verifier.configured());
        assertFalse(verifier.verify(token(SECRET, userClaims("DAF"), "superuser", inAnHour())).isPresent());
        assertEquals(0, bootstraps.get());
    }
}
