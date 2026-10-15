package za.co.rmb.credit.daf.support;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.MethodParameter;

/**
 * Guice's circular proxies, for the legacy services' constructor injection.
 *
 * <p>The legacy services inject each other through {@code @Inject} constructors (for example
 * ApprovalForumServiceImpl takes ApprovalForumMailService, whose implementation injects
 * ApprovalForumService). Guice resolves such a cycle by injecting a proxy for the interface. Spring cannot
 * break a cycle that runs through a constructor, so an interface-typed constructor parameter of a legacy
 * class, whose type is a legacy interface, is injected as a lazy proxy to the same singleton bean. Calls
 * reach the same object as before; only the moment it is looked up moves to the first call.</p>
 */
public final class GuiceCircularProxies extends ContextAnnotationAutowireCandidateResolver {

    private final String legacyPackage;

    private GuiceCircularProxies(String legacyPackage) {
        this.legacyPackage = legacyPackage;
    }

    /** Installs the resolver for classes and interfaces under {@code legacyPackage} (e.g. "za.co.rmb.approvalforum."). */
    public static BeanFactoryPostProcessor installer(String legacyPackage) {
        return beanFactory -> ((DefaultListableBeanFactory) beanFactory)
                .setAutowireCandidateResolver(new GuiceCircularProxies(legacyPackage));
    }

    @Override
    protected boolean isLazy(DependencyDescriptor descriptor) {
        if (super.isLazy(descriptor)) {
            return true;
        }
        MethodParameter parameter = descriptor.getMethodParameter();
        if (parameter == null || parameter.getConstructor() == null) {
            return false;
        }
        Class<?> type = parameter.getParameterType();
        return type.isInterface()
                && type.getName().startsWith(legacyPackage)
                && parameter.getDeclaringClass().getName().startsWith(legacyPackage);
    }
}
