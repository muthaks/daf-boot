package za.co.rmb.credit.daf.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Settings of the MCP server (prefix {@code daf.mcp}; the environment variables are in application.yml).
 *
 * @param apiBaseUrl       daf-api's base URL, the JAX-RS application root ({@code .../approvalforum/api})
 * @param categories       tool categories served: {@code user} (the API a signed-in user works with),
 *                         {@code sign-in} (endpoints that take credentials or another system's token),
 *                         {@code system} (scheduled jobs, their triggers and the build information)
 * @param groups           tool groups served (agenda, workflow, auth, external, creditdata, documents, docstash,
 *                         email, scheduler, git); empty means every group
 * @param readOnly         serve only the tools that read (GET): nothing that changes DAF
 * @param requireToken     refuse MCP requests without an Authorization bearer (401) instead of answering the
 *                         tool call with an error
 * @param requestTimeout   per daf-api call
 * @param maxResponseChars a JSON or text answer longer than this is cut, with a note
 * @param maxDownloadBytes a download larger than this is refused instead of returned as base64
 */
@ConfigurationProperties(prefix = "daf.mcp")
public record DafMcpProperties(
        String apiBaseUrl,
        List<String> categories,
        List<String> groups,
        boolean readOnly,
        boolean requireToken,
        Duration requestTimeout,
        int maxResponseChars,
        long maxDownloadBytes) {

    public DafMcpProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            throw new IllegalArgumentException("daf.mcp.api-base-url (DAF_API_BASE) must be set");
        }
        apiBaseUrl = apiBaseUrl.replaceAll("/+$", "");
        categories = categories == null || categories.isEmpty() ? List.of("user") : List.copyOf(categories);
        groups = groups == null ? List.of() : List.copyOf(groups);
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(60) : requestTimeout;
        maxResponseChars = maxResponseChars <= 0 ? 200_000 : maxResponseChars;
        maxDownloadBytes = maxDownloadBytes <= 0 ? 10L * 1024 * 1024 : maxDownloadBytes;
    }
}
