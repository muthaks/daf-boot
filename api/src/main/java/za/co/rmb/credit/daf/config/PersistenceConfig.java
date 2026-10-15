package za.co.rmb.credit.daf.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;

import za.co.rmb.credit.daf.support.LegacyPersistenceUnit;
import za.co.rmb.credit.daf.support.LegacyProcedureNullParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The legacy persistence unit ({@code ApprovalForumPersistence}) and Guice-persist behaviour on Spring.
 *
 * <ul>
 *   <li>Managed classes: the {@code <class>} entries of the legacy persistence.xml (copied to
 *       {@code legacy/persistence.xml}), including the three model classes it maps, plus every annotated
 *       class in the jar that held that persistence.xml (approvalforum-jdbc, packages
 *       {@code za.co.rmb.approvalforum.data}): the unit did not set {@code exclude-unlisted-classes}, so
 *       Hibernate detected those too (ApprovalForumRRulesEntity is only reachable that way).</li>
 *   <li>{@link EntityManager}: injected into the repositories' constructors as in Guice; Spring provides
 *       the transaction-bound shared proxy.</li>
 *   <li>One EntityManager per request: Guice-persist's {@code PersistFilter} on {@code /*} becomes
 *       {@link OpenEntityManagerInViewFilter} on {@code /*} (Spring Boot's open-in-view only covers Spring MVC).</li>
 *   <li>Flyway before JPA: the legacy {@code FlywayMigrationInitializer} ran as an eager singleton; the
 *       entity manager factory now depends on it.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class PersistenceConfig {

    static final String FLYWAY_INITIALIZER = "za.co.rmb.approvalforum.data.flyway.FlywayMigrationInitializer";

    static final String JDBC_MODULE_PACKAGE = "za.co.rmb.approvalforum.data";

    @Bean
    PersistenceManagedTypes persistenceManagedTypes(ResourceLoader resourceLoader) throws IOException {
        return LegacyPersistenceUnit.managedTypes(resourceLoader, JDBC_MODULE_PACKAGE);
    }

    @Bean
    FilterRegistrationBean<OpenEntityManagerInViewFilter> persistFilter() {
        FilterRegistrationBean<OpenEntityManagerInViewFilter> registration =
                new FilterRegistrationBean<>(new OpenEntityManagerInViewFilter());
        registration.addUrlPatterns("/*");
        registration.setName("persistFilter");
        return registration;
    }

    @Bean
    static BeanPostProcessor legacyProcedureNullParameters() {
        return new LegacyProcedureNullParameters();
    }

    @Bean
    static BeanFactoryPostProcessor flywayBeforeJpa() {
        return beanFactory -> {
            for (String name : beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false)) {
                BeanDefinition definition = beanFactory.getBeanDefinition(name.startsWith("&") ? name.substring(1) : name);
                List<String> dependsOn = new ArrayList<>();
                if (definition.getDependsOn() != null) {
                    dependsOn.addAll(List.of(definition.getDependsOn()));
                }
                dependsOn.add(FLYWAY_INITIALIZER);
                definition.setDependsOn(dependsOn.toArray(String[]::new));
            }
        };
    }

}
