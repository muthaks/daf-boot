package za.co.rmb.credit.daf.rules;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

/**
 * The legacy Guice bindings of the rules service, registered as Spring beans by class, so Spring calls their
 * {@code @Inject} constructors and fills their {@code @Inject} fields as Guice did.
 *
 * <p>Scopes follow Guice: the unscoped bindings (token services, rules service, repository) get one instance per
 * request, as RESTEasy-Guice built them per request inside the request's Guice-persist unit of work; the engine is
 * {@code @Singleton} and, as in Guice's default stage, created on first use, which is when it loads the rules
 * from R_Rules.</p>
 */
public class RulesBindings implements BeanDefinitionRegistryPostProcessor {

    /** ApprovalForumDroolsSecurityModule */
    static final List<String> SECURITY = List.of(
            "za.co.rmb.approvalforumdrools.security.service.impl.AuthenticationTokenSettingsImpl",
            "za.co.rmb.approvalforumdrools.security.service.impl.AuthenticationTokenIssuerImpl",
            "za.co.rmb.approvalforumdrools.security.service.impl.AuthenticationTokenParserImpl",
            "za.co.rmb.approvalforumdrools.security.service.impl.AuthenticationTokenServiceImpl");

    /** JPAApprovalForumDroolsEntityModule, ApprovalForumDroolsServiceModule */
    static final List<String> SERVICES = List.of(
            "za.co.rmb.approvalforumdrools.data.jpa.JPAApprovalForumDroolsDataRepository",
            "za.co.rmb.approvalforumdrools.service.impl.ApprovalForumDroolsServiceImpl");

    /** ApprovalForumDroolsEngineModule: bind(ApprovalForumDroolsEngine.class), a @Singleton class */
    static final String ENGINE = "za.co.rmb.approvalforumdrools.engine.ApprovalForumDroolsEngine";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        for (List<String> group : List.of(SECURITY, SERVICES)) {
            for (String name : group) {
                RootBeanDefinition definition = new RootBeanDefinition(ClassUtils.resolveClassName(name, loader));
                definition.setScope(WebApplicationContext.SCOPE_REQUEST);
                BeanDefinitionHolder proxy = ScopedProxyUtils.createScopedProxy(
                        new BeanDefinitionHolder(definition, name), registry, true);
                registry.registerBeanDefinition(proxy.getBeanName(), proxy.getBeanDefinition());
            }
        }
        RootBeanDefinition engine = new RootBeanDefinition(ClassUtils.resolveClassName(ENGINE, loader));
        engine.setLazyInit(true);
        registry.registerBeanDefinition(ENGINE, engine);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing: bindings are complete after registration
    }
}
