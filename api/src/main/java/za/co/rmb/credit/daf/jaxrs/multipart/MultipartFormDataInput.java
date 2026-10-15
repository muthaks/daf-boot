package za.co.rmb.credit.daf.jaxrs.multipart;

import java.util.List;
import java.util.Map;

/**
 * The part of RESTEasy's {@code MultipartFormDataInput} the legacy upload resources use, so they read
 * {@code multipart/form-data} on Jersey without a code change. Read by {@link MultipartFormDataInputReader}.
 */
public interface MultipartFormDataInput {

    /** Form fields by name, each with its parts in the order they were sent. */
    Map<String, List<InputPart>> getFormDataMap();
}
