package za.co.rmb.credit.daf.rules;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DAF rules service: the legacy approvalforumdrools JAX-RS application, run on Jersey inside Spring Boot,
 * with the rules engine on Drools 10. Scanning covers this package and the legacy resources and
 * authentication filter, which Jersey serves.
 */
@SpringBootApplication(scanBasePackages = {
        "za.co.rmb.credit.daf.rules",
        "za.co.rmb.approvalforumdrools.web.resources",
        "za.co.rmb.approvalforumdrools.security.filter"})
public class DafRulesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DafRulesApplication.class, args);
    }
}
