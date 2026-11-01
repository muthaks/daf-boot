package za.co.rmb.credit.daf.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

/** What the server tests share: the stand-in's address and an MCP client over Streamable HTTP. */
final class McpTestSupport {

    private McpTestSupport() { }

    /** daf-api is the stand-in in the same application: its address is only known once the server runs. */
    @TestConfiguration
    static class StandInBaseUrl {
        @Bean
        @Primary
        DafApiBaseUrl standInDafApiBaseUrl(Environment environment) {
            return () -> "http://localhost:" + environment.getProperty("local.server.port") + "/approvalforum/api";
        }
    }

    static McpSyncClient client(int port, String bearer) {
        HttpRequest.Builder request = HttpRequest.newBuilder();
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .requestBuilder(request)
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .initializationTimeout(Duration.ofSeconds(30))
                .build();
        client.initialize();
        return client;
    }

    static String text(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    static McpSchema.CallToolResult call(McpSyncClient client, String tool, Map<String, Object> arguments) {
        return client.callTool(new McpSchema.CallToolRequest(tool, arguments));
    }
}
