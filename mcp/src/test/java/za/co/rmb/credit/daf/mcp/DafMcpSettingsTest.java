package za.co.rmb.credit.daf.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Read-only mode, a group filter, the sign-in category and no token requirement at the connection. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "daf.mcp.api-base-url=http://placeholder/approvalforum/api",
        "daf.mcp.read-only=true",
        "daf.mcp.groups=agenda,auth",
        "daf.mcp.categories=user,sign-in",
        "daf.mcp.require-token=false"})
@Import({StandInDafApi.class, McpTestSupport.StandInBaseUrl.class})
class DafMcpSettingsTest {

    @LocalServerPort
    int port;

    @Test
    void servesOnlyTheReadToolsOfTheChosenGroups() {
        McpSyncClient client = McpTestSupport.client(port, null);
        try {
            Set<McpSchema.Tool> tools = Set.copyOf(client.listTools().tools());
            Set<String> names = tools.stream().map(McpSchema.Tool::name).collect(Collectors.toSet());
            assertTrue(names.contains("agenda_get_committees"));
            assertTrue(names.contains("auth_users"));
            assertTrue(names.contains("auth_keycloak_config"), "sign-in category requested");
            assertFalse(names.contains("agenda_cancel_meeting"), "writes are not served in read-only mode");
            assertFalse(names.contains("auth_login"), "a POST, even in the sign-in category");
            assertFalse(names.contains("workflow_list_analyst"), "workflow group not requested");
            assertTrue(tools.stream().allMatch(t -> Boolean.TRUE.equals(t.annotations().readOnlyHint())));

            McpSchema.CallToolResult result = McpTestSupport.call(client, "agenda_get_committees", Map.of());
            assertEquals(Boolean.TRUE, result.isError(), "no bearer on the connection");
            assertTrue(McpTestSupport.text(result).contains("No DAF token"));
        } finally {
            client.closeGracefully();
        }
    }
}
