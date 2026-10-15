package za.co.rmb.credit.daf.config;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * daf-api serves every endpoint of the legacy approvalforum-web application, the deliberate additions listed
 * in {@code legacy/additions.txt}, and no other.
 *
 * <p>{@code legacy/endpoints.txt} is generated from the legacy resources (tools/legacy-endpoints.py). The served
 * endpoints come from Jersey's own resource model of the classes {@link JerseyConfig} registers.</p>
 */
class LegacyEndpointInventoryTest {

    @Test
    void servesExactlyTheLegacyEndpointsAndTheAdditions() throws IOException {
        assertEquals(expected("/legacy/endpoints.txt", "/legacy/additions.txt"), served(new JerseyConfig().getClasses()));
    }

    static String expected(String... resources) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String resource : resources) {
            try (InputStream in = LegacyEndpointInventoryTest.class.getResourceAsStream(resource)) {
                for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                    if (!line.isBlank() && !line.trim().startsWith("#")) {
                        lines.add(line.trim());
                    }
                }
            }
        }
        return String.join("\n", new TreeSet<>(lines));
    }

    static String served(Iterable<Class<?>> classes) {
        TreeSet<String> endpoints = new TreeSet<>();
        for (Class<?> type : classes) {
            if (type.isAnnotationPresent(jakarta.ws.rs.Path.class)) {
                collect(Resource.from(type), "", endpoints);
            }
        }
        return String.join("\n", endpoints);
    }

    private static void collect(Resource resource, String parent, TreeSet<String> out) {
        String path = join(parent, resource.getPath());
        for (ResourceMethod method : resource.getResourceMethods()) {
            out.add(method.getHttpMethod() + " " + path);
        }
        for (Resource child : resource.getChildResources()) {
            collect(child, path, out);
        }
    }

    private static String join(String parent, String path) {
        String joined = (parent == null ? "" : parent) + "/" + (path == null ? "" : path);
        joined = joined.replaceAll("\\{\\s*(\\w+)\\s*(:[^}]*)?}", "{$1}");
        StringBuilder sb = new StringBuilder();
        for (String part : joined.split("/")) {
            if (!part.isEmpty()) {
                sb.append('/').append(part);
            }
        }
        return sb.toString();
    }
}
