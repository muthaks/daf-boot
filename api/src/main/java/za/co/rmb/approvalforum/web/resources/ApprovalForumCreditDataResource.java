package za.co.rmb.approvalforum.web.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

/**
 * Credit Data reference data for the Agenda Builder's classification fields: the countries list, the industries
 * the counterparties are classified in, and a counterparty's own country of risk and industry. Read from Credit
 * Data Boot through the gateway ({@code af.refdataservice.base}, {@code af.counterpartyservice.base}); the
 * Credit Data token is added to each request by the Credit Data token filter.
 */
@Api(tags = "creditdata")
@Path("/approvalForum/creditdata")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumCreditDataResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumCreditDataResource.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long INDUSTRIES_TTL_MS = TimeUnit.HOURS.toMillis(6);

    /** The industry list is derived from every counterparty's classification, so it is kept for a while. */
    private static volatile List<String> industries;
    private static volatile long industriesAt;

    @Context
    private SecurityContext context;

    private final Map<String, String> afSettings;

    @Inject
    public ApprovalForumCreditDataResource(@Named("af_settings") Map<String, String> afSettings) {
        this.afSettings = afSettings;
    }

    @GET
    @Path("/countries")
    @Produces("application/json")
    @ApiOperation(value = "Countries (Credit Data reference data), enabled ones, by name",
            authorizations = @Authorization(value = "Bearer"))
    public Response countries() {
        tokenValidation(context);
        JsonNode rows = get("af.refdataservice", "/api/countries");
        if (rows == null) return unavailable("countries");
        List<Map<String, String>> out = new ArrayList<>();
        for (JsonNode row : rows) {
            if (row.path("disabled").asBoolean(false) || row.path("name").asText("").isBlank()) continue;
            Map<String, String> country = new LinkedHashMap<>();
            country.put("code", row.path("code").asText(""));
            country.put("name", row.path("name").asText());
            out.add(country);
        }
        out.sort(Comparator.comparing(c -> c.get("name")));
        return Response.ok(out).build();
    }

    @GET
    @Path("/industries")
    @Produces("application/json")
    @ApiOperation(value = "Industries the counterparties are classified in (Credit Data), by name",
            authorizations = @Authorization(value = "Bearer"))
    public Response industries() {
        tokenValidation(context);
        List<String> cached = industries;
        if (cached != null && System.currentTimeMillis() - industriesAt < INDUSTRIES_TTL_MS) {
            return Response.ok(cached).build();
        }
        JsonNode rows = get("af.counterpartyservice", "/api/sectorindustry");
        if (rows == null) return cached != null ? Response.ok(cached).build() : unavailable("industries");
        TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (JsonNode row : rows) {
            String name = row.path("industry_name").asText("").trim();
            if (!name.isEmpty()) names.add(name);
        }
        industries = List.copyOf(names);
        industriesAt = System.currentTimeMillis();
        return Response.ok(industries).build();
    }

    @GET
    @Path("/counterparty/classification")
    @Produces("application/json")
    @ApiOperation(value = "A counterparty's main country of risk and industry (Credit Data)",
            authorizations = @Authorization(value = "Bearer"))
    public Response classification(@QueryParam("inkombaId") String inkombaId) {
        tokenValidation(context);
        Map<String, String> out = new LinkedHashMap<>();
        if (inkombaId == null || inkombaId.isBlank()) return Response.ok(out).build();
        String id = URLEncoder.encode(inkombaId.trim(), StandardCharsets.UTF_8);
        JsonNode country = largest(get("af.counterpartyservice", "/api/countryofrisk/" + id), "risk_percentage");
        if (country != null) out.put("countryOfRisk", country.path("country_name").asText(""));
        JsonNode industry = largest(get("af.counterpartyservice", "/api/sectorindustry?creditInkombaId=" + id), "percentage");
        if (industry != null) {
            out.put("industry", industry.path("industry_name").asText(""));
            out.put("sector", industry.path("sector_name").asText(""));
        }
        return Response.ok(out).build();
    }

    /** The row with the largest share (country of risk %, industry %), or null. */
    private static JsonNode largest(JsonNode rows, String share) {
        JsonNode best = null;
        if (rows == null) return null;
        for (JsonNode row : rows) {
            if (best == null || row.path(share).asDouble(0) > best.path(share).asDouble(0)) best = row;
        }
        return best;
    }

    /**
     * GET on a Credit Data service: {@code <service>.base} + path (the gateway maps the service's previous
     * paths onto Credit Data Boot). The service's own token is sent unless the Credit Data token filter
     * replaces it. Null when the call fails or the answer is not a JSON array.
     */
    private JsonNode get(String service, String path) {
        String base = afSettings.get(service + ".base");
        if (base == null || base.isBlank()) return null;
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        try {
            Invocation.Builder request = client.target(base.replaceAll("/+$", "") + path).request(MediaType.APPLICATION_JSON);
            String token = afSettings.get(service + ".token");
            if (token != null && !token.isBlank()) request.header("Authorization", "Bearer " + token);
            Response response = request.get();
            String text = response.readEntity(String.class);
            if (response.getStatus() != 200) {
                logger.warn("Credit Data {} answered {}", path.replaceAll("\\?.*", ""), response.getStatus());
                return null;
            }
            JsonNode node = MAPPER.readTree(text);
            return node != null && node.isArray() ? node : null;
        } catch (Exception e) {
            logger.warn("Credit Data {} failed: {}", path.replaceAll("\\?.*", ""), e.getMessage());
            return null;
        } finally {
            client.close();
        }
    }

    private static Response unavailable(String what) {
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(Map.of("message", "Credit Data " + what + " are not available right now."))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
