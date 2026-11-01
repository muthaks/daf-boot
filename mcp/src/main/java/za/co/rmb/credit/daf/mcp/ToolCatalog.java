package za.co.rmb.credit.daf.mcp;

import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tool catalog {@code daf-tools.json}: one entry per daf-api endpoint, generated from the resource sources
 * by {@code mcp/tools/generate-tool-catalog.py}. The catalog is the contract between the generator and the
 * server; the server never derives anything from the API at run time.
 */
public final class ToolCatalog {

    public static final String RESOURCE = "daf-tools.json";

    /** A request parameter: path, query or header. */
    public record Param(String name, String type, String defaultValue, boolean required) { }

    /** The form parts of a multipart endpoint: the file part and the plain fields. */
    public record Multipart(String filePart, List<String> fields) { }

    /** One endpoint of daf-api as a tool. */
    public record Tool(
            String name,
            String group,
            String category,
            String description,
            String method,
            String path,
            boolean readOnly,
            List<Param> pathParams,
            List<Param> queryParams,
            List<Param> headerParams,
            Map<String, Object> body,
            Multipart multipart,
            boolean binary,
            boolean permitAll,
            String resource) {

        public String endpoint() {
            return method + " " + path;
        }
    }

    private final List<Tool> tools;

    public ToolCatalog(List<Tool> tools) {
        this.tools = List.copyOf(tools);
    }

    public List<Tool> tools() {
        return tools;
    }

    public static ToolCatalog load(JsonMapper mapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            return parse(mapper.readTree(in));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    @SuppressWarnings("unchecked")
    static ToolCatalog parse(JsonNode root) {
        List<Tool> tools = new ArrayList<>();
        for (JsonNode node : root.get("tools")) {
            JsonNode multipart = node.get("multipart");
            JsonNode body = node.get("body");
            tools.add(new Tool(
                    node.get("name").asString(),
                    node.get("group").asString(),
                    node.get("category").asString(),
                    node.get("description").asString(),
                    node.get("method").asString(),
                    node.get("path").asString(),
                    node.get("readOnly").asBoolean(),
                    params(node.get("pathParams")),
                    params(node.get("queryParams")),
                    params(node.get("headerParams")),
                    body == null || body.isNull() ? null : (Map<String, Object>) toJava(body),
                    multipart == null || multipart.isNull() ? null
                            : new Multipart(multipart.get("filePart").asString(), strings(multipart.get("fields"))),
                    node.get("binary").asBoolean(),
                    node.get("permitAll").asBoolean(),
                    node.get("resource").asString()));
        }
        return new ToolCatalog(tools);
    }

    private static List<Param> params(JsonNode array) {
        List<Param> out = new ArrayList<>();
        if (array != null) {
            for (JsonNode p : array) {
                JsonNode def = p.get("default");
                JsonNode type = p.get("type");
                JsonNode required = p.get("required");
                out.add(new Param(p.get("name").asString(),
                        type == null || type.isNull() ? "string" : type.asString(),
                        def == null || def.isNull() ? null : def.asString(),
                        required != null && required.asBoolean()));
            }
        }
        return List.copyOf(out);
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        for (JsonNode s : array) {
            out.add(s.asString());
        }
        return List.copyOf(out);
    }

    /** A JSON tree as plain Java maps, lists and values (the shape the MCP schema builder takes). */
    static Object toJava(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.properties().forEach(e -> map.put(e.getKey(), toJava(e.getValue())));
            return map;
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(n -> list.add(toJava(n)));
            return list;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isNull()) {
            return null;
        }
        return node.asString();
    }
}
