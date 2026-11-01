package za.co.rmb.credit.daf.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The MCP tools: one {@link McpServerFeatures.SyncToolSpecification} per catalog entry the settings let
 * through (category, group, read-only). The input schema is built from the entry's parameters and body;
 * the call handler hands the arguments and the connection's bearer to {@link DafApiExecutor}.
 */
public class DafToolSpecifications {

    private static final Logger logger = LoggerFactory.getLogger(DafToolSpecifications.class);

    private final ToolCatalog catalog;
    private final DafMcpProperties properties;
    private final DafApiExecutor executor;

    public DafToolSpecifications(ToolCatalog catalog, DafMcpProperties properties, DafApiExecutor executor) {
        this.catalog = catalog;
        this.properties = properties;
        this.executor = executor;
    }

    /** The catalog entries the settings serve. */
    public List<ToolCatalog.Tool> served() {
        List<ToolCatalog.Tool> out = new ArrayList<>();
        for (ToolCatalog.Tool tool : catalog.tools()) {
            if (!properties.categories().contains(tool.category())) {
                continue;
            }
            if (!properties.groups().isEmpty() && !properties.groups().contains(tool.group())) {
                continue;
            }
            if (properties.readOnly() && !tool.readOnly()) {
                continue;
            }
            out.add(tool);
        }
        return out;
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        List<McpServerFeatures.SyncToolSpecification> specs = new ArrayList<>();
        for (ToolCatalog.Tool tool : served()) {
            specs.add(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(McpSchema.Tool.builder()
                            .name(tool.name())
                            .description(description(tool))
                            .inputSchema(inputSchema(tool))
                            .annotations(annotations(tool))
                            .build())
                    .callHandler((exchange, request) -> {
                        Object bearer = exchange.transportContext().get(McpTransportConfig.AUTHORIZATION);
                        DafApiExecutor.Outcome outcome = executor.call(tool, request.arguments(),
                                bearer == null ? null : bearer.toString());
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(outcome.text())
                                .isError(outcome.error())
                                .build();
                    })
                    .build());
        }
        logger.info("Serving {} of {} DAF tools (categories {}, groups {}, read only {})", specs.size(),
                catalog.tools().size(), properties.categories(),
                properties.groups().isEmpty() ? "all" : properties.groups(), properties.readOnly());
        return specs;
    }

    static String description(ToolCatalog.Tool tool) {
        return tool.description() + " [" + tool.endpoint() + "]";
    }

    static McpSchema.ToolAnnotations annotations(ToolCatalog.Tool tool) {
        boolean idempotent = switch (tool.method()) {
            case "GET", "PUT", "DELETE" -> true;
            default -> false;
        };
        return McpSchema.ToolAnnotations.builder()
                .readOnlyHint(tool.readOnly())
                .destructiveHint("DELETE".equals(tool.method()))
                .idempotentHint(idempotent)
                .openWorldHint(false)
                .build();
    }

    /** The tool's input: path parameters (required), query and header parameters, the body or the file. */
    static Map<String, Object> inputSchema(ToolCatalog.Tool tool) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (ToolCatalog.Param p : tool.pathParams()) {
            properties.put(p.name(), Map.of("type", p.type(), "description", "Path parameter"));
            required.add(p.name());
        }
        for (ToolCatalog.Param p : tool.queryParams()) {
            String description = "Query parameter" + (p.defaultValue() == null ? "" : ", default " + p.defaultValue());
            properties.put(p.name(), Map.of("type", p.type(), "description", description));
            if (p.required() && p.defaultValue() == null) {
                required.add(p.name());
            }
        }
        for (ToolCatalog.Param p : tool.headerParams()) {
            properties.put(p.name(), Map.of("type", "string", "description", "Request header"));
            if (p.required()) {
                required.add(p.name());
            }
        }
        if (tool.multipart() != null) {
            properties.put(DafApiExecutor.ARG_FILE_NAME, Map.of("type", "string", "description", "Name of the file, with its extension"));
            properties.put(DafApiExecutor.ARG_FILE_CONTENT, Map.of("type", "string", "description", "The file's content, base64"));
            required.add(DafApiExecutor.ARG_FILE_NAME);
            required.add(DafApiExecutor.ARG_FILE_CONTENT);
            for (String field : tool.multipart().fields()) {
                properties.put(field, Map.of("type", "string", "description", "Form field"));
            }
        } else if (tool.body() != null) {
            properties.put(DafApiExecutor.ARG_BODY, tool.body());
            required.add(DafApiExecutor.ARG_BODY);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        schema.put("additionalProperties", false);
        return schema;
    }
}
