package za.co.rmb.credit.daf.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Refuses MCP requests that carry no bearer token (401, {@code WWW-Authenticate: Bearer}), when
 * {@code daf.mcp.require-token} is on. The token itself is not checked here: daf-api verifies it on every
 * tool call. This only makes a missing token visible at connection time instead of on the first tool call.
 */
@Component
public class BearerRequiredFilter extends OncePerRequestFilter {

    private final DafMcpProperties properties;
    private final String endpoint;

    public BearerRequiredFilter(DafMcpProperties properties, McpServerStreamableHttpProperties serverProperties) {
        this.properties = properties;
        this.endpoint = serverProperties.getMcpEndpoint();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.requireToken() || !endpoint.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"daf\"");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"message\":\"A DAF token is required: Authorization: Bearer <DAF token>\"}"
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
    }
}
