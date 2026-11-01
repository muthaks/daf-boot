package za.co.rmb.credit.daf.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static za.co.rmb.credit.daf.mcp.McpTestSupport.call;
import static za.co.rmb.credit.daf.mcp.McpTestSupport.text;

/**
 * The server over Streamable HTTP, as an agent uses it: tools listed from the catalog, each call forwarded to
 * daf-api (the stand-in) with the connection's bearer, the answer returned as the tool's text.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"daf.mcp.api-base-url=http://placeholder/approvalforum/api"})
@Import({StandInDafApi.class, McpTestSupport.StandInBaseUrl.class})
class DafMcpServerTest {

    private static final String BEARER = "Bearer daf-token-of-the-user";

    @LocalServerPort
    int port;

    @Autowired
    DafToolSpecifications specifications;

    @Autowired
    JsonMapper mapper;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        StandInDafApi.SEEN.clear();
        client = McpTestSupport.client(port, BEARER);
    }

    @AfterEach
    void disconnect() {
        client.closeGracefully();
    }

    @Test
    void listsEveryUserToolOfTheCatalog() {
        McpSchema.ListToolsResult tools = client.listTools();
        Set<String> listed = tools.tools().stream().map(McpSchema.Tool::name).collect(Collectors.toSet());
        Set<String> served = specifications.served().stream().map(ToolCatalog.Tool::name).collect(Collectors.toSet());
        assertEquals(served, listed);
        assertTrue(listed.size() > 100, "user tools: " + listed.size());
        assertTrue(listed.contains("agenda_get_committees"));
        assertTrue(listed.contains("workflow_list_analyst"));
        assertFalse(listed.contains("scheduler_start_scheduler"), "system tools are not served by default");
        assertFalse(listed.contains("auth_login"), "sign-in tools are not served by default");

        McpSchema.Tool committees = tools.tools().stream().filter(t -> t.name().equals("agenda_get_committees")).findFirst().orElseThrow();
        assertTrue(committees.description().endsWith("[GET /approvalForum/agenda/committees]"));
        assertEquals(Boolean.TRUE, committees.annotations().readOnlyHint());

        McpSchema.Tool cancel = tools.tools().stream().filter(t -> t.name().equals("agenda_cancel_meeting")).findFirst().orElseThrow();
        assertEquals(Boolean.FALSE, cancel.annotations().readOnlyHint());
        assertEquals(List.of("agmId"), cancel.inputSchema().get("required"));
    }

    @Test
    void forwardsTheBearerAndReturnsTheJsonAnswer() {
        McpSchema.CallToolResult result = call(client, "agenda_get_committees", Map.of());

        assertOk(result);
        StandInDafApi.Seen seen = StandInDafApi.last();
        assertEquals("GET", seen.method());
        assertEquals("/approvalForum/agenda/committees", seen.path());
        assertEquals(BEARER, seen.headers().get("authorization"));
        JsonNode answer = mapper.readTree(text(result));
        assertEquals(2, answer.size());
        assertEquals("REIB", answer.get(1).get("name").asString());
    }

    @Test
    void substitutesPathParametersAndEncodesQueryParameters() {
        McpSchema.CallToolResult result = call(client, "agenda_cancel_meeting",
                Map.of("agmId", 7, "reason", "chair away & quorum lost"));

        assertOk(result);
        StandInDafApi.Seen seen = StandInDafApi.last();
        assertEquals("POST", seen.method());
        assertEquals("/approvalForum/agenda/meetings/7/cancel", seen.path());
        assertEquals("reason=chair%20away%20%26%20quorum%20lost", seen.query());
        assertTrue(text(result).contains("Cancelled"));
    }

    @Test
    void sendsTheBodyArgumentAsJson() {
        Map<String, Object> body = Map.of("externalRequest", false,
                "data", Map.of("recommendation", "Approve", "tenantId", 1));
        McpSchema.CallToolResult result = call(client, "workflow_create_wip_request",
                Map.of("tenantId", 2, "body", body));

        assertOk(result);
        StandInDafApi.Seen seen = StandInDafApi.last();
        assertEquals("POST", seen.method());
        assertEquals("/approvalForum/request/wip", seen.path());
        assertEquals("tenantId=2", seen.query());
        assertTrue(seen.headers().get("content-type").startsWith("application/json"));
        JsonNode sent = mapper.readTree(seen.body());
        assertEquals("Approve", sent.get("data").get("recommendation").asString());
        assertEquals(42, mapper.readTree(text(result)).get("workflowId").asInt());
    }

    @Test
    void aMissingBodyIsAnErrorBeforeDafIsCalled() {
        McpSchema.CallToolResult result = call(client, "workflow_create_wip_request", Map.of());

        assertEquals(Boolean.TRUE, result.isError());
        assertTrue(text(result).contains("body"), text(result));
        assertTrue(StandInDafApi.SEEN.isEmpty());
    }

    @Test
    void aMissingPathParameterIsAnErrorBeforeDafIsCalled() {
        McpSchema.CallToolResult result = call(client, "agenda_get_item", new HashMap<>());

        assertEquals(Boolean.TRUE, result.isError());
        assertTrue(text(result).contains("agiId"));
        assertTrue(StandInDafApi.SEEN.isEmpty());
    }

    @Test
    void dafsRefusalComesBackAsAnErrorWithItsMessage() {
        McpSchema.CallToolResult result = call(client, "agenda_get_item", Map.of("agiId", 9));

        assertEquals(Boolean.TRUE, result.isError());
        assertTrue(text(result).contains("HTTP 403"));
        assertTrue(text(result).contains("Only the owner may open this item"));
    }

    @Test
    void noContentAnswersAreReported() {
        McpSchema.CallToolResult result = call(client, "agenda_delete_booking_draft", Map.of("agbdId", 3));

        assertOk(result);
        assertEquals("HTTP 204 (no content)", text(result));
        assertEquals("DELETE", StandInDafApi.last().method());
    }

    @Test
    void textAnswersAreReturnedAsTheyAre() {
        McpSchema.CallToolResult result = call(client, "auth_ping", Map.of());

        assertOk(result);
        assertEquals("pong", text(result));
    }

    @Test
    void downloadsComeBackAsBase64WithTheFileName() {
        McpSchema.CallToolResult result = call(client, "documents_download", Map.of("documentId", "D-1"));

        assertOk(result);
        assertEquals("documentId=D-1", StandInDafApi.last().query());
        JsonNode file = mapper.readTree(text(result));
        assertEquals("paper.pdf", file.get("fileName").asString());
        assertEquals("%PDF-1.4 stand-in",
                new String(Base64.getDecoder().decode(file.get("contentBase64").asString()), StandardCharsets.UTF_8));
    }

    @Test
    void uploadsSendTheFileAndTheFormFields() {
        String content = Base64.getEncoder().encodeToString("paper bytes".getBytes(StandardCharsets.UTF_8));
        McpSchema.CallToolResult result = call(client, "agenda_upload_document",
                Map.of("agiId", 5, "fileName", "credit-paper.pdf", "fileContentBase64", content, "docType", "Credit Paper"));

        assertOk(result);
        StandInDafApi.Seen seen = StandInDafApi.last();
        assertEquals("/approvalForum/agenda/items/5/upload", seen.path());
        assertTrue(seen.headers().get("content-type").startsWith("multipart/form-data"));
        assertEquals("multipart credit-paper.pdf paper bytes", seen.body());
        JsonNode answer = mapper.readTree(text(result));
        assertEquals("Credit Paper", answer.get("docType").asString());
        assertEquals(11, answer.get("size").asInt());
    }

    @Test
    void anUnknownToolIsRefusedByTheServer() {
        McpError refused = assertThrows(McpError.class, () -> call(client, "agenda_no_such_tool", Map.of()));
        assertTrue(refused.getMessage().contains("Unknown tool"), refused.getMessage());
    }

    private static void assertOk(McpSchema.CallToolResult result) {
        assertNotEquals(Boolean.TRUE, result.isError(), () -> text(result));
    }

    @Test
    void aConnectionWithoutABearerIsRefusedWithA401() {
        RestClient plain = RestClient.create();
        var response = plain.post().uri("http://localhost:" + port + "/mcp")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}")
                .exchange((req, res) -> Map.of("status", res.getStatusCode(),
                        "authenticate", String.valueOf(res.getHeaders().getFirst("WWW-Authenticate")),
                        "body", new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        assertEquals(HttpStatus.UNAUTHORIZED, response.get("status"));
        assertEquals("Bearer realm=\"daf\"", response.get("authenticate"));
        assertNotNull(response.get("body"));
        assertTrue(response.get("body").toString().contains("DAF token is required"));
    }
}
