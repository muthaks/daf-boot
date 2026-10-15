package za.co.rmb.credit.daf.jaxrs.multipart;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code multipart/form-data} into {@link MultipartFormDataInput} on top of Jersey's multipart support.
 *
 * <p>Part bodies are decoded the way RESTEasy 3.6 decoded them: a part without a {@code Content-Type}
 * is {@code text/plain; charset=US-ASCII}, and a {@code Content-Type} without a charset gets
 * {@code charset=US-ASCII}. The body is then handed to the registered {@link MessageBodyReader} for the
 * requested type, as RESTEasy did.</p>
 */
@Provider
@Consumes(MediaType.MULTIPART_FORM_DATA)
public class MultipartFormDataInputReader implements MessageBodyReader<MultipartFormDataInput> {

    private static final String US_ASCII = "us-ascii";
    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

    @Context
    private Providers providers;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MultipartFormDataInput.class.equals(type);
    }

    @Override
    public MultipartFormDataInput readFrom(Class<MultipartFormDataInput> type, Type genericType, Annotation[] annotations,
                                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                           InputStream entityStream) throws IOException, WebApplicationException {
        MessageBodyReader<FormDataMultiPart> reader =
                providers.getMessageBodyReader(FormDataMultiPart.class, FormDataMultiPart.class, annotations, mediaType);
        FormDataMultiPart multiPart = reader.readFrom(FormDataMultiPart.class, FormDataMultiPart.class, annotations,
                mediaType, httpHeaders, entityStream);
        Map<String, List<InputPart>> fields = new LinkedHashMap<>();
        for (Map.Entry<String, List<FormDataBodyPart>> field : multiPart.getFields().entrySet()) {
            List<InputPart> parts = new ArrayList<>();
            for (FormDataBodyPart part : field.getValue()) {
                parts.add(new Part(part, providers));
            }
            fields.put(field.getKey(), parts);
        }
        Map<String, List<InputPart>> view = Collections.unmodifiableMap(fields);
        return () -> view;
    }

    private static final class Part implements InputPart {

        private final FormDataBodyPart part;
        private final Providers providers;
        private final MediaType contentType;

        Part(FormDataBodyPart part, Providers providers) {
            this.part = part;
            this.providers = providers;
            String header = part.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            MediaType declared = header == null ? MediaType.TEXT_PLAIN_TYPE : MediaType.valueOf(header);
            if (declared.getParameters().containsKey(MediaType.CHARSET_PARAMETER)) {
                contentType = declared;
            } else {
                Map<String, String> parameters = new HashMap<>(declared.getParameters());
                parameters.put(MediaType.CHARSET_PARAMETER, US_ASCII);
                contentType = new MediaType(declared.getType(), declared.getSubtype(), parameters);
            }
        }

        @Override
        public MultivaluedMap<String, String> getHeaders() {
            return part.getHeaders();
        }

        @Override
        public MediaType getMediaType() {
            return contentType;
        }

        @Override
        public <T> T getBody(Class<T> type, Type genericType) throws IOException {
            Type generic = genericType == null ? type : genericType;
            MessageBodyReader<T> reader = providers.getMessageBodyReader(type, generic, NO_ANNOTATIONS, contentType);
            if (reader == null) {
                throw new IOException("No MessageBodyReader for " + type.getName() + " and " + contentType);
            }
            return reader.readFrom(type, generic, NO_ANNOTATIONS, contentType, part.getHeaders(), body());
        }

        @Override
        public String getBodyAsString() throws IOException {
            return getBody(String.class, null);
        }

        private InputStream body() {
            Object entity = part.getEntity();
            if (entity instanceof BodyPartEntity bodyPartEntity) {
                return bodyPartEntity.getInputStream();
            }
            return new ByteArrayInputStream(part.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
