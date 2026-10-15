package za.co.rmb.credit.daf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DAF (Delegated Authority Forum) API: the legacy approvalforum JAX-RS application, run on Jersey inside
 * Spring Boot. Component scanning covers this package (the Spring wiring that replaces Guice) and the
 * legacy resources and authentication filter, which Jersey serves.
 */
@SpringBootApplication(scanBasePackages = {
        "za.co.rmb.credit.daf",
        "za.co.rmb.approvalforum.web.resources",
        "za.co.rmb.approvalforum.security.filter"})
public class DafApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DafApiApplication.class, args);
    }
}
