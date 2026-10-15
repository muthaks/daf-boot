package za.co.rmb.credit.daf.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The managed classes of a legacy persistence unit, as Hibernate 5 saw them.
 *
 * <p>The legacy units were RESOURCE_LOCAL and did not set {@code exclude-unlisted-classes}, so Hibernate managed
 * the {@code <class>} entries of persistence.xml plus every annotated class in the jar that held it. The
 * persistence.xml is copied to {@code legacy/persistence.xml}; the jar's packages are passed as
 * {@code persistenceUnitRootPackage}.</p>
 */
public final class LegacyPersistenceUnit {

    private LegacyPersistenceUnit() {
    }

    public static PersistenceManagedTypes managedTypes(ResourceLoader resourceLoader, String persistenceUnitRootPackage)
            throws IOException {
        Set<String> names = new LinkedHashSet<>(listedClasses());
        names.addAll(new PersistenceManagedTypesScanner(resourceLoader).scan(persistenceUnitRootPackage).getManagedClassNames());
        return PersistenceManagedTypes.of(List.copyOf(names), List.of());
    }

    /** The {@code <class>} entries of {@code legacy/persistence.xml}, in order. */
    public static List<String> listedClasses() throws IOException {
        try (InputStream in = new ClassPathResource("legacy/persistence.xml").getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(in);
            NodeList classes = document.getElementsByTagNameNS("*", "class");
            List<String> names = new ArrayList<>();
            for (int i = 0; i < classes.getLength(); i++) {
                names.add(classes.item(i).getTextContent().trim());
            }
            return names;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("legacy/persistence.xml could not be read", e);
        }
    }
}
