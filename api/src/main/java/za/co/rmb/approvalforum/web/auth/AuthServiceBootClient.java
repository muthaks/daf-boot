package za.co.rmb.approvalforum.web.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.credit.authservice.model.ActiveDirectoryDomainModel;
import za.co.rmb.credit.authservice.model.ActiveDirectoryGroupModel;
import za.co.rmb.credit.authservice.model.ActiveDirectoryLoginResultModel;
import za.co.rmb.credit.authservice.model.ActiveDirectoryServerModel;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.authservice.model.GroupRequestModel;
import za.co.rmb.credit.authservice.model.PingModel;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * DAF's auth service client over auth-service-boot. DAF makes the calls of the legacy auth service
 * (/api/auth/*); auth-service-boot serves the same operations under /api/v1/*, reached through the API
 * gateway ({@code af.authservice.base} = the gateway's /auth). Answers come back in the legacy models,
 * and a failed call answers as the legacy client did (ping false, no user or sign-in result, empty
 * lists), so DAF's sign-in, role and user lookups are unchanged.
 */
public class AuthServiceBootClient extends AuthServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceBootClient.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;

    /** auth-service-boot answers with more fields than the legacy models hold. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String base;

    public AuthServiceBootClient(String base) {
        super(base);
        this.base = base == null ? "" : base.replaceAll("/+$", "");
    }

    @Override
    public PingModel Ping() {
        JsonNode body = call("GET", "/api/v1/auth/ping", null, null);
        return new PingModel(body != null && body.path("success").asBoolean(false));
    }

    /**
     * A refused sign-in is an answer (status "failure"), not an error; the result always carries a
     * model, as DAF reads its fields without a null check.
     */
    @Override
    public ActiveDirectoryLoginResultModel AuthenticateAgainstAD(String domain, String username, String password) {
        JsonNode body = call("POST", "/api/v1/auth/authenticate", null, builder -> builder
                .header("domain", domain)
                .header("username", username)
                .header("password", password));
        if (body == null) return null;
        ObjectNode model = body.path("user").isObject() ? ((ObjectNode) body.get("user")).deepCopy() : MAPPER.createObjectNode();
        JsonNode groups = body.path("groups").isArray() ? body.get("groups") : model.path("groups");
        model.set("groups", groups.isArray() ? groups : MAPPER.createArrayNode());
        ObjectNode result = MAPPER.createObjectNode();
        result.put("status", body.path("status").asText("failure"));
        result.put("reason", body.path("reason").asText(""));
        result.set("model", model);
        return convert(result, ActiveDirectoryLoginResultModel.class);
    }

    @Override
    public ActiveDirectoryUserModel User(String domain, String username) {
        return convert(call("GET", "/api/v1/users?domain=" + q(domain) + "&username=" + q(username), null, null),
                ActiveDirectoryUserModel.class);
    }

    @Override
    public ActiveDirectoryUserModel User(String userPrincipalName) {
        return convert(call("GET", "/api/v1/users/" + q(userPrincipalName), null, null), ActiveDirectoryUserModel.class);
    }

    @Override
    public List<ActiveDirectoryUserModel> Users(String domain, String role) {
        return list(call("GET", "/api/v1/users/search?domain=" + q(domain) + "&role=" + q(role) + "&displayName=", null, null),
                new TypeReference<>() {});
    }

    @Override
    public List<ActiveDirectoryUserModel> UsersWithDisplayName(String domain, String displayName) {
        return list(call("GET", "/api/v1/users/search?domain=" + q(domain) + "&role=&displayName=" + q(displayName), null, null),
                new TypeReference<>() {});
    }

    @Override
    public List<ActiveDirectoryGroupModel> GroupsFromName(String domain, String name) {
        return list(call("GET", "/api/v1/groups?domain=" + q(domain) + "&name=" + q(name), null, null),
                new TypeReference<>() {});
    }

    @Override
    public List<ActiveDirectoryGroupModel> GetGroups(GroupRequestModel request) {
        return list(call("POST", "/api/v1/groups", request, null), new TypeReference<>() {});
    }

    @Override
    public List<ActiveDirectoryDomainModel> GetDomains(boolean onlyEnabled) {
        return list(call("GET", "/api/v1/domains?onlyEnabled=" + onlyEnabled, null, null), new TypeReference<>() {});
    }

    @Override
    public List<ActiveDirectoryServerModel> GetServers(String domain) {
        return list(call("GET", "/api/v1/domains/" + q(domain) + "/servers", null, null), new TypeReference<>() {});
    }

    /** The parsed answer of a successful call (sign-in also on 401: its body is the refusal), else null. */
    private JsonNode call(String method, String path, Object body, Function<Invocation.Builder, Invocation.Builder> headers) {
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
        try {
            WebTarget target = client.target(base + path);
            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
            if (headers != null) builder = headers.apply(builder);
            Response response = "POST".equals(method)
                    ? builder.post(Entity.entity(body == null ? "" : MAPPER.writeValueAsString(body), MediaType.APPLICATION_JSON))
                    : builder.get();
            String text = response.readEntity(String.class);
            boolean signIn = path.startsWith("/api/v1/auth/authenticate");
            if (response.getStatus() >= 300 && !(signIn && response.getStatus() == 401)) {
                logger.error("auth-service {} {} answered {}", method, path.replaceAll("\\?.*", ""), response.getStatus());
                return null;
            }
            return text == null || text.isBlank() ? null : MAPPER.readTree(text);
        } catch (Exception e) {
            logger.error("auth-service {} {} failed: {}", method, path.replaceAll("\\?.*", ""), e.getMessage());
            return null;
        } finally {
            client.close();
        }
    }

    private static <T> T convert(JsonNode node, Class<T> type) {
        if (node == null || !node.isObject()) return null;
        try {
            return MAPPER.treeToValue(node, type);
        } catch (Exception e) {
            logger.error("auth-service answer is not a {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private static <T> List<T> list(JsonNode node, TypeReference<List<T>> type) {
        if (!(node instanceof ArrayNode)) return new ArrayList<>();
        try {
            return MAPPER.convertValue(node, type);
        } catch (Exception e) {
            logger.error("auth-service list answer could not be read: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private static String q(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
