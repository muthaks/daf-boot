package za.co.rmb.credit.daf.support;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

/**
 * The JSON mapper of the legacy RESTEasy provider.
 *
 * <p>Legacy JSON came from {@code ResteasyJackson2Provider} (a {@code JacksonJaxbJsonProvider}): a plain
 * {@code new ObjectMapper()} with no modules, reading Jackson annotations first and JAXB annotations second.
 * The model classes are JAXB-generated, so their property names and adapters come from the JAXB annotations,
 * and {@code java.util.Date} is written as epoch milliseconds. Spring Boot's default mapper differs on all of
 * these, so the services give Jersey this one.</p>
 */
public final class LegacyObjectMapper {

    private LegacyObjectMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(AnnotationIntrospector.pair(
                new JacksonAnnotationIntrospector(),
                new JakartaXmlBindAnnotationIntrospector(mapper.getTypeFactory())));
        return mapper;
    }
}
