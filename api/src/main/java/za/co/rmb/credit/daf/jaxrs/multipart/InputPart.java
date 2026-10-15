package za.co.rmb.credit.daf.jaxrs.multipart;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.lang.reflect.Type;

/** One part of a {@link MultipartFormDataInput}: the subset of RESTEasy's {@code InputPart} DAF calls. */
public interface InputPart {

    MultivaluedMap<String, String> getHeaders();

    /** The part's content type, with RESTEasy 3.6's defaults applied (see {@link MultipartFormDataInputReader}). */
    MediaType getMediaType();

    <T> T getBody(Class<T> type, Type genericType) throws IOException;

    String getBodyAsString() throws IOException;
}
