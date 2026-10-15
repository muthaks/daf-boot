package za.co.rmb.credit.daf.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * The legacy Guice {@code bind(Interface).to(Implementation)} bindings, registered as Spring beans.
 *
 * <p>Each implementation is registered by class, so Spring calls its {@code @Inject} constructor and fills
 * its {@code @Inject} fields (with {@code @Named} as qualifier) the way Guice did. Classes are listed by
 * name because several are package-private. The source of each group is the Guice module named above it.</p>
 *
 * <p>Scopes follow Guice. The services, repositories and token services were unscoped: RESTEasy-Guice built a
 * new resource for every request and with it a new graph of services and repositories, inside that request's
 * Guice-persist unit of work. Some depend on that (the audit repository takes its Envers reader from the
 * request's session when it is built; the workflow service keeps per-call state in fields). They are request
 * scoped here, behind a class proxy, so each request again gets its own instances, and the proxies resolve
 * the services' mutual references as Guice's did. Classes annotated {@code @Singleton} stay singletons and are
 * created on first use, as in Guice's default (development) stage; FlywayMigrationModule bound its
 * initializer as an eager singleton.</p>
 */
public class LegacyBindings implements BeanDefinitionRegistryPostProcessor {

    /** ApprovalForumServiceModule */
    static final List<String> SERVICES = List.of(
            "za.co.rmb.approvalforum.service.impl.ApprovalForumServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumExternalServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumRulesServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumListManagementServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumAuthServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumDocumentsServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumDocstashServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumMailServiceImpl",
            "za.co.rmb.approvalforum.service.impl.LumosRestServiceImpl",
            "za.co.rmb.approvalforum.service.impl.ApprovalForumEmailReceivedImpl",
            "za.co.rmb.approvalforum.service.impl.AgendaServiceImpl",
            "za.co.rmb.approvalforum.service.impl.AgendaMailServiceImpl");

    /** JPAApprovalForumEntityModule */
    static final List<String> REPOSITORIES = List.of(
            "za.co.rmb.approvalforum.data.jpa.JPAApprovalForumDataRepository",
            "za.co.rmb.approvalforum.data.jpa.JPAApprovalForumDataAuditRepository",
            "za.co.rmb.approvalforum.data.jpa.JPAApprovalForumAuthRepositoryImpl",
            "za.co.rmb.approvalforum.data.jpa.JPAApprovalForumDocumentsRepository",
            "za.co.rmb.approvalforum.data.jpa.JPAApprovalForumAgendaRepository");

    /** ApprovalForumSecurityModule */
    static final List<String> SECURITY = List.of(
            "za.co.rmb.approvalforum.security.service.impl.AuthenticationTokenSettingsImpl",
            "za.co.rmb.approvalforum.security.service.impl.AuthenticationTokenIssuerImpl",
            "za.co.rmb.approvalforum.security.service.impl.AuthenticationTokenParserImpl",
            "za.co.rmb.approvalforum.security.service.impl.AuthenticationTokenServiceImpl");

    /**
     * ApprovalForumWebResourcesModule (AgendaCalendarService -> GraphMeetingClient), SchedulerModule's job
     * factory, FlywayMigrationModule (eager in Guice, a singleton here), and the one concrete class Guice
     * created just in time (CasdmRestClient).
     */
    static final List<String> WEB = List.of(
            "za.co.rmb.approvalforum.web.graph.GraphMeetingClient",
            "za.co.rmb.approvalforum.web.quartz.SchedulerJobFactory",
            "za.co.rmb.approvalforum.data.flyway.FlywayMigrationInitializer",
            "za.co.rmb.approvalforum.web.casdm.CasdmRestClient");

    /** ApprovalForumPropertiesModule: bound but never injected by legacy code, so never created. Lazy here. */
    static final List<String> LAZY = List.of(
            "za.co.rmb.approvalforum.data.dao.ApprovalForumProperties",
            "za.co.rmb.approvalforum.data.ApprovalForumPropertiesProvider");

    /** Unscoped in Guice: one instance per request. */
    static final List<List<String>> PER_REQUEST = List.of(SERVICES, REPOSITORIES, SECURITY);

    /** Eager singletons in Guice (asEagerSingleton). */
    static final List<String> EAGER = List.of("za.co.rmb.approvalforum.data.flyway.FlywayMigrationInitializer");

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        for (List<String> group : PER_REQUEST) {
            for (String name : group) {
                RootBeanDefinition definition = new RootBeanDefinition(ClassUtils.resolveClassName(name, loader));
                definition.setScope(WebApplicationContext.SCOPE_REQUEST);
                BeanDefinitionHolder proxy = ScopedProxyUtils.createScopedProxy(
                        new BeanDefinitionHolder(definition, name), registry, true);
                registry.registerBeanDefinition(proxy.getBeanName(), proxy.getBeanDefinition());
            }
        }
        for (String name : WEB) {
            RootBeanDefinition definition = new RootBeanDefinition(ClassUtils.resolveClassName(name, loader));
            definition.setLazyInit(!EAGER.contains(name));
            registry.registerBeanDefinition(name, definition);
        }
        for (String name : LAZY) {
            RootBeanDefinition definition = new RootBeanDefinition(ClassUtils.resolveClassName(name, loader));
            definition.setLazyInit(true);
            registry.registerBeanDefinition(name, definition);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing: bindings are complete after registration
    }
}
