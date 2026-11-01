package za.co.rmb.credit.daf.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * Wires the server: the Streamable HTTP transport that keeps each request's {@code Authorization} header in
 * the transport context (so a tool call can forward it), the catalog, the executor and the tools.
 */
@Configuration
public class McpTransportConfig {

    /** Transport context key under which the request's Authorization header value is kept. */
    public static final String AUTHORIZATION = "authorization";

    /**
     * Same as the Spring AI auto-configured provider, plus the context extractor. The transport puts the
     * extracted context on every tool call of that request ({@code exchange.transportContext()}).
     */
    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper,
            McpServerStreamableHttpProperties serverProperties) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .mcpEndpoint(serverProperties.getMcpEndpoint())
                .keepAliveInterval(serverProperties.getKeepAliveInterval())
                .disallowDelete(serverProperties.isDisallowDelete())
                .contextExtractor(request -> {
                    String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
                    return authorization == null ? McpTransportContext.EMPTY
                            : McpTransportContext.create(Map.of(AUTHORIZATION, authorization));
                })
                .build();
    }

    @Bean
    public ToolCatalog toolCatalog(JsonMapper jsonMapper) {
        return ToolCatalog.load(jsonMapper);
    }

    @Bean
    public DafApiBaseUrl dafApiBaseUrl(DafMcpProperties properties) {
        return properties::apiBaseUrl;
    }

    @Bean
    public DafApiExecutor dafApiExecutor(RestClient.Builder restClientBuilder, DafApiBaseUrl baseUrl,
                                         JsonMapper jsonMapper, DafMcpProperties properties) {
        return new DafApiExecutor(restClientBuilder.build(), baseUrl, jsonMapper, properties);
    }

    @Bean
    public DafToolSpecifications dafToolSpecifications(ToolCatalog catalog, DafMcpProperties properties,
                                                       DafApiExecutor executor) {
        return new DafToolSpecifications(catalog, properties, executor);
    }

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> dafTools(DafToolSpecifications specifications) {
        return specifications.specifications();
    }
}
