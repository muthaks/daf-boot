package za.co.rmb.credit.daf.mcp;

/** Where daf-api is: its JAX-RS application root, without a trailing slash. */
@FunctionalInterface
public interface DafApiBaseUrl {
    String get();
}
