package za.co.rmb.credit.daf.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.stereotype.Component;
import za.co.rmb.approvalforumdrools.security.filter.AuthenticationFilter;
import za.co.rmb.approvalforumdrools.web.resources.ApprovalForumDroolsResource;
import za.co.rmb.approvalforumdrools.web.resources.GitResource;
import za.co.rmb.credit.daf.support.GuiceCircularProxies;
import za.co.rmb.credit.daf.support.LegacyObjectMapper;
import za.co.rmb.credit.daf.support.LegacyPersistenceUnit;
import za.co.rmb.credit.daf.support.LegacyProcedureNullParameters;
import za.co.rmb.credit.daf.support.RestEasyBehaviour;

import java.io.IOException;

/**
 * Spring wiring of the rules service, in place of its Guice modules and web.xml: the Guice bindings, the
 * persistence unit {@code ApprovalDroolsForumPersistence} with Guice-persist's per-request EntityManager, and
 * the JSON mapper of the legacy RESTEasy provider.
 */
@Configuration(proxyBeanMethods = false)
public class RulesConfig {

    static final String JDBC_MODULE_PACKAGE = "za.co.rmb.approvalforumdrools.data";

    @Bean
    static RulesBindings rulesBindings() {
        return new RulesBindings();
    }

    @Bean
    static BeanFactoryPostProcessor guiceCircularProxies() {
        return GuiceCircularProxies.installer("za.co.rmb.approvalforumdrools.");
    }

    @Bean
    static BeanPostProcessor legacyProcedureNullParameters() {
        return new LegacyProcedureNullParameters();
    }

    @Bean
    @Primary
    ObjectMapper legacyObjectMapper() {
        return LegacyObjectMapper.create();
    }

    @Bean
    PersistenceManagedTypes persistenceManagedTypes(ResourceLoader resourceLoader) throws IOException {
        return LegacyPersistenceUnit.managedTypes(resourceLoader, JDBC_MODULE_PACKAGE);
    }

    /** ApprovalForumDroolsWebModule: filter("/*").through(PersistFilter.class) */
    @Bean
    FilterRegistrationBean<OpenEntityManagerInViewFilter> persistFilter() {
        FilterRegistrationBean<OpenEntityManagerInViewFilter> registration =
                new FilterRegistrationBean<>(new OpenEntityManagerInViewFilter());
        registration.addUrlPatterns("/*");
        registration.setName("persistFilter");
        return registration;
    }

    /** web.xml (/api) and ApprovalForumDroolsWebResourcesModule; URLs stay /approvalforumdrools/api/... */
    @Component
    @ApplicationPath("/api")
    public static class RulesJerseyConfig extends ResourceConfig {

        public RulesJerseyConfig() {
            register(ApprovalForumDroolsResource.class);
            register(GitResource.class);
            register(AuthenticationFilter.class);
            RestEasyBehaviour.providers().forEach(this::register);
            // RESTEasy answered an error status without an entity with an empty body, not an HTML page
            property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        }
    }
}
