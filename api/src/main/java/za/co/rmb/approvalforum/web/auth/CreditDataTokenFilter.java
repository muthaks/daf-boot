package za.co.rmb.approvalforum.web.auth;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Credit Data Boot accepts auth-service tokens for its system (CREDITDATA). DAF calls the Credit Data
 * services from several clients, some built once at start-up with a fixed token; this filter, applied to
 * every JAX-RS client (see {@link CreditDataTokenDiscoverable}), gives each request addressed to one of
 * DAF's Credit Data bases ({@code af.*service.base}, {@code af.*service.ash.base}) a current system token
 * for {@code af.creditdata.audience} (default CREDITDATA) instead.
 *
 * Without DAF's system-token settings, or when the auth service issues no token, the request keeps the
 * token its client set ({@code af.*service.token}).
 */
public class CreditDataTokenFilter implements ClientRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(CreditDataTokenFilter.class);
    private static final String DEFAULT_AUDIENCE = "CREDITDATA";
    private static final List<String> SERVICES = List.of("applicationservice", "counterpartyservice", "facilityservice", "refdataservice");

    private static volatile List<String> bases = List.of();
    private static volatile SystemTokenSource tokens;
    private static volatile String audience = DEFAULT_AUDIENCE;

    /** Takes the Credit Data bases and DAF's system-token settings from DAF's settings. */
    public static void configure(Map<String, String> afSettings) {
        bases = SERVICES.stream()
                .flatMap(service -> Stream.of(afSettings.get("af." + service + ".base"), afSettings.get("af." + service + ".ash.base")))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(base -> !base.isEmpty())
                // Several clients drop the base's "/api" before adding their paths.
                .map(base -> base.replaceAll("/+$", "").replaceAll("/api$", ""))
                .distinct()
                .toList();
        SystemTokenSource source = new SystemTokenSource(afSettings);
        tokens = source.isConfigured() ? source : null;
        String configured = afSettings.get("af.creditdata.audience");
        audience = configured == null || configured.isBlank() ? DEFAULT_AUDIENCE : configured.trim();
        logger.info("Credit Data system token for {}: {}", audience, tokens != null ? "from the auth service" : "not configured, client tokens kept");
    }

    @Override
    public void filter(ClientRequestContext request) {
        SystemTokenSource source = tokens;
        if (source == null || !isCreditData(request.getUri().toString())) return;
        String token = source.token(audience);
        if (token != null) {
            request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }

    static boolean isCreditData(String uri) {
        for (String base : bases) {
            if (uri.equals(base) || uri.startsWith(base + "/") || uri.startsWith(base + "?")) return true;
        }
        return false;
    }
}
