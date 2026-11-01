package za.co.rmb.credit.daf.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a tool call into the daf-api request the catalog entry describes and the response into the tool's
 * answer. The caller's bearer token is forwarded as received: DAF decides what the user may do.
 */
public class DafApiExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DafApiExecutor.class);

    /** Tool arguments the multipart endpoints take, beside the form fields. */
    static final String ARG_FILE_NAME = "fileName";
    static final String ARG_FILE_CONTENT = "fileContentBase64";
    /** Tool argument that carries a request body. */
    static final String ARG_BODY = "body";

    /** A tool's answer: text for the agent, or an error. */
    public record Outcome(String text, boolean error) {
        static Outcome ok(String text) {
            return new Outcome(text, false);
        }

        static Outcome failed(String text) {
            return new Outcome(text, true);
        }
    }

    private final RestClient restClient;
    private final DafApiBaseUrl baseUrl;
    private final JsonMapper mapper;
    private final DafMcpProperties properties;

    public DafApiExecutor(RestClient restClient, DafApiBaseUrl baseUrl, JsonMapper mapper, DafMcpProperties properties) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.properties = properties;
    }

    public Outcome call(ToolCatalog.Tool tool, Map<String, Object> arguments, String bearer) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        if ((bearer == null || bearer.isBlank()) && !tool.permitAll()) {
            return Outcome.failed("No DAF token: connect to this MCP server with the header "
                    + "\"Authorization: Bearer <DAF token>\" (the token the DAF remote obtains at sign-in).");
        }
        for (ToolCatalog.Param p : tool.pathParams()) {
            if (args.get(p.name()) == null) {
                return Outcome.failed("Missing argument \"" + p.name() + "\" (path parameter of " + tool.endpoint() + ").");
            }
        }
        URI uri;
        try {
            uri = uri(tool, args);
        } catch (IllegalArgumentException e) {
            return Outcome.failed("Invalid arguments for " + tool.endpoint() + ": " + e.getMessage());
        }

        RestClient.RequestBodySpec request = restClient.method(HttpMethod.valueOf(tool.method())).uri(uri);
        if (bearer != null && !bearer.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        for (ToolCatalog.Param p : tool.headerParams()) {
            Object value = args.get(p.name());
            if (value != null) {
                request.header(p.name(), String.valueOf(value));
            } else if (p.required()) {
                return Outcome.failed("Missing argument \"" + p.name() + "\" (header of " + tool.endpoint() + ").");
            }
        }
        request.accept(tool.binary() ? MediaType.APPLICATION_OCTET_STREAM : MediaType.APPLICATION_JSON);

        if (tool.multipart() != null) {
            Outcome invalid = multipart(tool, args, request);
            if (invalid != null) {
                return invalid;
            }
        } else if (tool.body() != null) {
            Object body = args.get(ARG_BODY);
            if (body == null && !"GET".equals(tool.method())) {
                return Outcome.failed("Missing argument \"" + ARG_BODY + "\" (request body of " + tool.endpoint() + ").");
            }
            if (body != null) {
                String json = body instanceof String s && "string".equals(tool.body().get("type"))
                        ? s : mapper.writeValueAsString(body);
                request.contentType(MediaType.APPLICATION_JSON).body(json);
            }
        }

        logger.info("{} {} {}", tool.name(), tool.method(), uri.getPath());
        try {
            return request.exchange((req, response) -> answer(tool, response.getStatusCode(), response.getHeaders(),
                    response.getBody().readAllBytes()));
        } catch (RuntimeException e) {
            logger.warn("{} {} failed: {}", tool.name(), uri, e.toString());
            return Outcome.failed("DAF did not answer " + tool.endpoint() + ": " + e.getMessage());
        }
    }

    private URI uri(ToolCatalog.Tool tool, Map<String, Object> args) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl.get() + tool.path());
        Map<String, Object> variables = new LinkedHashMap<>();
        for (ToolCatalog.Param p : tool.pathParams()) {
            variables.put(p.name(), String.valueOf(args.get(p.name())));
        }
        int i = 0;
        for (ToolCatalog.Param p : tool.queryParams()) {
            Object value = args.get(p.name());
            if (value != null) {
                String variable = "q" + (i++);
                builder.queryParam(p.name(), "{" + variable + "}");
                variables.put(variable, String.valueOf(value));
            }
        }
        return builder.build().expand(variables).encode().toUri();
    }

    private Outcome multipart(ToolCatalog.Tool tool, Map<String, Object> args, RestClient.RequestBodySpec request) {
        Object name = args.get(ARG_FILE_NAME);
        Object content = args.get(ARG_FILE_CONTENT);
        if (name == null || content == null) {
            return Outcome.failed("Missing argument \"" + ARG_FILE_NAME + "\" or \"" + ARG_FILE_CONTENT
                    + "\" (the file of " + tool.endpoint() + ").");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(String.valueOf(content));
        } catch (IllegalArgumentException e) {
            return Outcome.failed(ARG_FILE_CONTENT + " is not valid base64.");
        }
        String fileName = String.valueOf(name);
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part(tool.multipart().filePart(), new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        for (String field : tool.multipart().fields()) {
            Object value = args.get(field);
            if (value != null) {
                body.part(field, String.valueOf(value));
            }
        }
        request.contentType(MediaType.MULTIPART_FORM_DATA).body(body.build());
        return null;
    }

    private Outcome answer(ToolCatalog.Tool tool, HttpStatusCode status, HttpHeaders headers, byte[] bytes) {
        if (status.isError()) {
            String body = new String(bytes, StandardCharsets.UTF_8);
            return Outcome.failed("DAF answered HTTP " + status.value() + " for " + tool.endpoint()
                    + (body.isBlank() ? "" : ": " + cut(body)));
        }
        if (tool.binary()) {
            if (bytes.length > properties.maxDownloadBytes()) {
                return Outcome.failed("The file is " + bytes.length + " bytes, more than this server returns ("
                        + properties.maxDownloadBytes() + "). Download it through the DAF UI.");
            }
            ContentDisposition disposition = headers.getContentDisposition();
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("fileName", disposition.getFilename());
            file.put("contentType", headers.getContentType() == null ? null : headers.getContentType().toString());
            file.put("size", bytes.length);
            file.put("contentBase64", Base64.getEncoder().encodeToString(bytes));
            return Outcome.ok(mapper.writeValueAsString(file));
        }
        if (bytes.length == 0) {
            return Outcome.ok("HTTP " + status.value() + " (no content)");
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        try {
            JsonNode json = mapper.readTree(text);
            return Outcome.ok(cut(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json)));
        } catch (JacksonException e) {
            return Outcome.ok(cut(text));
        }
    }

    private String cut(String text) {
        int max = properties.maxResponseChars();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "\n... (" + (text.length() - max) + " more characters not shown)";
    }
}
