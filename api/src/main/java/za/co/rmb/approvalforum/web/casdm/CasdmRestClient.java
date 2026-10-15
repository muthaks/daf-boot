package za.co.rmb.approvalforum.web.casdm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.agenda.AgendaCasdmApplicationDTO;

import javax.net.ssl.HostnameVerifier;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Calls the CASDM REST API directly (registered calling system "DAF").
 *
 * Two-step auth per the CASDM access guide: exchange the API key for a short-lived
 * bearer token (cached ~14 min), then call the data endpoint with it. Configured via
 * af.casdmrest.base / af.casdmrest.apikey (env AF_CASDMREST_BASE / AF_CASDMREST_APIKEY);
 * when unset the client reports notConfigured() so the agenda flow degrades gracefully.
 */
@Singleton
public class CasdmRestClient {

    private static final Logger logger = LoggerFactory.getLogger(CasdmRestClient.class);

    private static final long TOKEN_TTL_MS = 14 * 60 * 1000L; // token lasts 15 min; refresh at 14

    // Fail fast rather than hanging the UI request when CASDM is slow/unreachable.
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 15000;

    private final Map<String, String> afSettings;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String cachedToken;
    private volatile long cachedTokenExpiryMs;

    @Inject
    public CasdmRestClient(@Named("af_settings") Map<String, String> afSettings) {
        this.afSettings = afSettings;
    }

    public AgendaCasdmApplicationDTO getLatestApplicationByInkomba(String creditInkombaId) {
        AgendaCasdmApplicationDTO result = new AgendaCasdmApplicationDTO();

        String base = afSettings.get("af.casdmrest.base");
        String apiKey = afSettings.get("af.casdmrest.apikey");
        logger.info("CASDM check start: inkombaId={}, base={}, apiKeyPresent={} (length {})",
                creditInkombaId, base, apiKey != null && !apiKey.trim().isEmpty(),
                apiKey == null ? 0 : apiKey.trim().length());
        if (base == null || base.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("CASDM check ABORT: not configured - af.casdmrest.base='{}', apikey blank={}. "
                    + "Env AF_CASDMREST_BASE/AF_CASDMREST_APIKEY not applied at startup?",
                    base, apiKey == null || apiKey.trim().isEmpty());
            result.setConfigured(false);
            return result;
        }
        if (creditInkombaId == null || creditInkombaId.trim().isEmpty()) {
            return result;
        }

        try {
            String token = getToken(base, apiKey);
            if (token == null) {
                logger.warn("CASDM check ABORT: token exchange failed (see error above) - reporting not configured");
                result.setConfigured(false);
                return result;
            }
            logger.info("CASDM check: token OK, calling {}/credit-application/latest-for-inkomba?creditInkombaId={}",
                    base, creditInkombaId.trim());

            Client client = newTimedClient();
            try {
                Response response = client.target(base + "/credit-application/latest-for-inkomba")
                        .queryParam("creditInkombaId", creditInkombaId.trim())
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .get();

                if (response.getStatus() != 200) {
                    logger.warn("CASDM latest-for-inkomba returned {} for {}", response.getStatus(), creditInkombaId);
                    return result;
                }

                String body = response.readEntity(String.class);
                if (body == null || body.trim().isEmpty() || "null".equalsIgnoreCase(body.trim())) {
                    logger.info("CASDM check: 200 but empty body - no application on CASDM for {}", creditInkombaId);
                    return result; // no application on CASDM
                }
                logger.info("CASDM check: application found for {} ({} bytes)", creditInkombaId, body.length());

                JsonNode node = mapper.readTree(body);
                result.setFound(true);
                if (node.hasNonNull("id")) {
                    result.setApplicationId(node.get("id").asInt());
                }
                String code = node.path("status").path("statusType").path("code").asText(null);
                result.setStatus(code);
                result.setInSubApproval("SubApproval".equals(code));
                result.setInSubReview("SubReview".equals(code));
                result.setApproved("Approved".equals(code));

                // Prepopulation data: the same application detail CASDM serves DAF capture -
                // analysts, rating, country, purpose - so the booking form can prefill.
                JsonNode main = node.path("mainInfo");
                result.setPurposeOfSubmission(main.path("purposeOfSubmission").asText(null));
                result.setMeetingName(main.path("meetingName").path("name").asText(null));
                result.setCreditAnalyst(main.path("creditAnalystTransaction").path("displayName").asText(null));
                result.setCreditAnalystEmail(main.path("creditAnalystTransaction").path("emailAddress").asText(null));
                result.setReviewAnalyst(main.path("creditAnalystReview").path("displayName").asText(null));
                result.setReviewAnalystEmail(main.path("creditAnalystReview").path("emailAddress").asText(null));
                result.setCountryOfRisk(node.path("pbe").path("countryOfDomicile").path("name").asText(null));

                JsonNode deal = node.path("firstDeal");
                String rating = deal.path("proposedFRSymbolRating").path("name").asText(null);
                if (rating == null) {
                    rating = deal.path("existingFRSymbolRating").path("name").asText(null);
                }
                result.setProposedRating(rating);
            } finally {
                client.close();
            }
        } catch (Throwable ex) {
            logger.error("CASDM latest-for-inkomba call failed for {}: {}", creditInkombaId, ex.toString(), ex);
        }
        return result;
    }

    private Client newTimedClient() {
        int connectTimeout = intSetting("af.casdmrest.connectTimeoutMs", DEFAULT_CONNECT_TIMEOUT_MS);
        int readTimeout = intSetting("af.casdmrest.readTimeoutMs", DEFAULT_READ_TIMEOUT_MS);
        ClientBuilder builder = ClientBuilder.newBuilder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS);
        HostnameVerifier verifier = acceptedCertNamesVerifier();
        if (verifier != null) {
            builder.hostnameVerifier(verifier);
        }
        return builder.build();
    }

    /**
     * The CASDM dev server presents a certificate issued for a different alias than the one DAF
     * dials (e.g. cert for casdm-ash-dev while the working route is casdm-dev), which strict TLS
     * hostname verification rightly rejects. Rather than disabling verification, this verifier
     * accepts the connection ONLY when the certificate's SAN list contains either the dialed host
     * or one of the names explicitly allowed via af.casdmrest.acceptedCertNames
     * (env AF_CASDMREST_ACCEPTED_CERT_NAMES, comma-separated). The certificate chain itself is
     * still fully validated against the JVM truststore. Leave the setting blank to enforce
     * standard hostname verification (recommended once the CASDM cert carries the right SANs).
     */
    private HostnameVerifier acceptedCertNamesVerifier() {
        String configured = afSettings.get("af.casdmrest.acceptedCertNames");
        if (configured == null || configured.trim().isEmpty()) {
            return null;
        }
        final Set<String> accepted = new HashSet<>();
        for (String name : configured.split(",")) {
            if (!name.trim().isEmpty()) {
                accepted.add(name.trim().toLowerCase(Locale.ROOT));
            }
        }
        return (hostname, session) -> {
            try {
                X509Certificate cert = (X509Certificate) session.getPeerCertificates()[0];
                java.util.Collection<List<?>> sans = cert.getSubjectAlternativeNames();
                if (sans == null) {
                    return false;
                }
                String host = hostname == null ? "" : hostname.toLowerCase(Locale.ROOT);
                for (List<?> san : sans) {
                    if (san.size() < 2 || !Integer.valueOf(2).equals(san.get(0))) {
                        continue; // only dNSName entries
                    }
                    String sanName = String.valueOf(san.get(1)).toLowerCase(Locale.ROOT);
                    if (sanName.equals(host)) {
                        return true; // normal match - cert actually covers the dialed name
                    }
                    if (accepted.contains(sanName)) {
                        logger.warn("CASDM TLS: accepting cert issued for '{}' on host '{}' via "
                                + "af.casdmrest.acceptedCertNames exception", sanName, hostname);
                        return true;
                    }
                }
            } catch (Exception ex) {
                logger.error("CASDM TLS: hostname verification failed: {}", ex.toString());
            }
            return false;
        };
    }

    private int intSetting(String key, int fallback) {
        String value = afSettings.get(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private synchronized String getToken(String base, String apiKey) {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < cachedTokenExpiryMs) {
            return cachedToken;
        }

        logger.info("CASDM token exchange: POST {}/auth/token", base);
        Client client = newTimedClient();
        try {
            Response response = client.target(base + "/auth/token")
                    .request(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .post(Entity.entity("{}", MediaType.APPLICATION_JSON));

            if (response.getStatus() != 200) {
                logger.warn("CASDM /auth/token returned {} (key rejected or wrong URL)", response.getStatus());
                return null;
            }
            logger.info("CASDM token exchange: 200 OK");
            JsonNode node = mapper.readTree(response.readEntity(String.class));
            String token = node.path("token").asText(null);
            if (token == null || token.isEmpty()) {
                return null;
            }
            long ttl = node.has("expiresInSeconds") ? (node.get("expiresInSeconds").asLong() - 60) * 1000L : TOKEN_TTL_MS;
            cachedToken = token;
            cachedTokenExpiryMs = now + Math.min(ttl, TOKEN_TTL_MS);
            return cachedToken;
        } catch (Throwable ex) {
            logger.error("CASDM token exchange failed: {}", ex.toString(), ex);
            return null;
        } finally {
            client.close();
        }
    }

}
