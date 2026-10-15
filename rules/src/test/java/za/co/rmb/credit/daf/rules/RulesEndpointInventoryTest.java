package za.co.rmb.credit.daf.rules;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * daf-rules serves every endpoint of the legacy approvalforumdrools-web application, and no other.
 * {@code legacy/endpoints.txt} is generated from the legacy resources (tools/legacy-endpoints.py).
 */
class RulesEndpointInventoryTest {

    @Test
    void servesExactlyTheLegacyEndpoints() throws IOException {
        TreeSet<String> expected = new TreeSet<>();
        try (InputStream in = getClass().getResourceAsStream("/legacy/endpoints.txt")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank()) {
                    expected.add(line.trim());
                }
            }
        }
        TreeSet<String> served = new TreeSet<>();
        for (Class<?> type : new RulesConfig.RulesJerseyConfig().getClasses()) {
            if (type.isAnnotationPresent(jakarta.ws.rs.Path.class)) {
                collect(Resource.from(type), "", served);
            }
        }
        assertEquals(String.join("\n", expected), String.join("\n", served));
    }

    private static void collect(Resource resource, String parent, TreeSet<String> out) {
        String joined = (parent + "/" + (resource.getPath() == null ? "" : resource.getPath()))
                .replaceAll("\\{\\s*(\\w+)\\s*(:[^}]*)?}", "{$1}");
        StringBuilder path = new StringBuilder();
        for (String part : joined.split("/")) {
            if (!part.isEmpty()) {
                path.append('/').append(part);
            }
        }
        for (ResourceMethod method : resource.getResourceMethods()) {
            out.add(method.getHttpMethod() + " " + path);
        }
        for (Resource child : resource.getChildResources()) {
            collect(child, path.toString(), out);
        }
    }
}
