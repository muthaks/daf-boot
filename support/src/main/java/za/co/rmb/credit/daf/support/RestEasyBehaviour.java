package za.co.rmb.credit.daf.support;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.Map;

/**
 * Two things the legacy RESTEasy 3.6 runtime did to responses, which the JAX-RS resources relied on without
 * saying so. Register all three providers with each Jersey application.
 *
 * <ul>
 *   <li>An unknown JSON property: 400, {@code text/html}, the original message (resteasy-jackson2-provider's
 *       UnrecognizedPropertyExceptionHandler).</li>
 *   <li>A request body that could not be read as JSON: RESTEasy wrapped the Jackson error in a ReaderException
 *       and answered 400, {@code text/html}, with {@code exception.toString()} (class name and message) as body
 *       ({@code ExceptionHandler.handleReaderException}). Jersey's own mapper answers {@code text/plain} with the
 *       message only. The Jackson message itself is worded by the Jackson version and can differ.</li>
 *   <li>{@code MessageSanitizerContainerResponseFilter}: every 400 answer with a {@code text/html} String body had
 *       {@code / < > & " '} replaced by entities.</li>
 * </ul>
 */
public final class RestEasyBehaviour {

    /** The content type RESTEasy gave these answers (Tomcat reported it with the charset). */
    static final MediaType HTML_UTF8 = MediaType.TEXT_HTML_TYPE.withCharset("UTF-8");

    private RestEasyBehaviour() {
    }

    /** The provider classes to register with a Jersey ResourceConfig. */
    public static List<Class<?>> providers() {
        return List.of(JsonMappingErrors.class, JsonParseErrors.class, UnrecognizedPropertyErrors.class, MessageSanitizer.class);
    }

    /**
     * resteasy-jackson2-provider's UnrecognizedPropertyExceptionHandler: an unknown JSON property answered 400,
     * {@code text/html}, with the exception's original message (no class name, no location).
     */
    @Provider
    @Priority(Priorities.USER - 1000)
    public static class UnrecognizedPropertyErrors implements ExceptionMapper<UnrecognizedPropertyException> {
        @Override
        public Response toResponse(UnrecognizedPropertyException e) {
            return Response.status(Response.Status.BAD_REQUEST).type(HTML_UTF8).entity(e.getOriginalMessage()).build();
        }
    }

    static Response readerError(Exception e) {
        return Response.status(Response.Status.BAD_REQUEST).type(HTML_UTF8).entity(e.toString()).build();
    }

    @Provider
    @Priority(Priorities.USER - 1000)
    public static class JsonMappingErrors implements ExceptionMapper<JsonMappingException> {
        @Override
        public Response toResponse(JsonMappingException e) {
            return readerError(e);
        }
    }

    @Provider
    @Priority(Priorities.USER - 1000)
    public static class JsonParseErrors implements ExceptionMapper<JsonParseException> {
        @Override
        public Response toResponse(JsonParseException e) {
            return readerError(e);
        }
    }

    /** Port of RESTEasy 3.6's MessageSanitizerContainerResponseFilter. */
    @Provider
    @Priority(Priorities.ENTITY_CODER)
    public static class MessageSanitizer implements ContainerResponseFilter {

        private static final Map<Character, String> REPLACEMENTS = Map.of(
                '/', "&#x2F;", '<', "&lt;", '>', "&gt;", '&', "&amp;", '"', "&quot;", '\'', "&#x27;");

        @Override
        public void filter(ContainerRequestContext request, ContainerResponseContext response) {
            if (response.getStatus() == 400 && response.getEntity() instanceof String text && isHtml(response.getMediaType())) {
                StringBuilder sb = new StringBuilder(text.length());
                for (char c : text.toCharArray()) {
                    String replacement = REPLACEMENTS.get(c);
                    sb.append(replacement == null ? String.valueOf(c) : replacement);
                }
                response.setEntity(sb.toString());
            }
        }

        private static boolean isHtml(MediaType type) {
            return type != null && "text".equalsIgnoreCase(type.getType()) && "html".equalsIgnoreCase(type.getSubtype());
        }
    }
}
