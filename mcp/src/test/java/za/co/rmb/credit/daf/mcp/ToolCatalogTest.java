package za.co.rmb.credit.daf.mcp;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catalog covers every endpoint daf-api serves (the legacy inventory plus the additions, the lists the API
 * module's own inventory test checks against) and nothing else; names are unique; schemas are well formed.
 */
class ToolCatalogTest {

    private static final Path LEGACY = Path.of("..", "api", "src", "test", "resources", "legacy");

    private final ToolCatalog catalog = ToolCatalog.load(JsonMapper.builder().build());

    @Test
    void coversExactlyTheEndpointsDafApiServes() throws IOException {
        Set<String> expected = new TreeSet<>();
        for (String file : List.of("endpoints.txt", "additions.txt")) {
            for (String line : Files.readAllLines(LEGACY.resolve(file))) {
                if (!line.isBlank() && !line.trim().startsWith("#")) {
                    expected.add(line.trim());
                }
            }
        }
        Set<String> cataloged = catalog.tools().stream().map(ToolCatalog.Tool::endpoint).collect(Collectors.toCollection(TreeSet::new));
        assertEquals(expected, cataloged);
    }

    @Test
    void namesAreUniqueAndEveryToolHasASchemaAndADescription() {
        Set<String> names = new HashSet<>();
        for (ToolCatalog.Tool tool : catalog.tools()) {
            assertTrue(names.add(tool.name()), "duplicate tool name " + tool.name());
            assertTrue(tool.name().matches("[a-z][a-z0-9_]*"), tool.name());
            assertTrue(tool.description().length() > 3, tool.name() + " has no description");
            assertTrue(Set.of("user", "sign-in", "system").contains(tool.category()), tool.name());
            Map<String, Object> schema = DafToolSpecifications.inputSchema(tool);
            assertEquals("object", schema.get("type"));
            assertNotNull(schema.get("properties"));
            assertEquals(tool.method().equals("GET"), tool.readOnly(), tool.name());
        }
    }

    @Test
    void bodiesDescribeTheirFields() {
        ToolCatalog.Tool book = catalog.tools().stream().filter(t -> t.name().equals("agenda_book_item")).findFirst().orElseThrow();
        assertNotNull(book.body());
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) book.body().get("properties");
        assertTrue(properties.containsKey("agcId"));
        assertTrue(properties.containsKey("item"));

        ToolCatalog.Tool upload = catalog.tools().stream().filter(t -> t.name().equals("documents_upload")).findFirst().orElseThrow();
        assertNotNull(upload.multipart());
        assertEquals("file", upload.multipart().filePart());
        assertTrue(upload.multipart().fields().contains("docType"));

        ToolCatalog.Tool download = catalog.tools().stream().filter(t -> t.name().equals("documents_download")).findFirst().orElseThrow();
        assertTrue(download.binary());
    }
}
