package za.co.rmb.approvalforum.web.auth;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;

/**
 * Registers {@link CreditDataTokenFilter} on every JAX-RS client Jersey builds, including those the
 * legacy client libraries create, so no Credit Data call is missed.
 */
public class CreditDataTokenDiscoverable implements ForcedAutoDiscoverable {
    @Override
    public void configure(FeatureContext context) {
        if (context.getConfiguration().getRuntimeType() == RuntimeType.CLIENT
                && !context.getConfiguration().isRegistered(CreditDataTokenFilter.class)) {
            context.register(CreditDataTokenFilter.class);
        }
    }
}
