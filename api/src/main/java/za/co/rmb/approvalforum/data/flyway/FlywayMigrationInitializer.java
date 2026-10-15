package za.co.rmb.approvalforum.data.flyway;

import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import javax.sql.DataSource;

/**
 * Runs Flyway migrations at application startup (bound as an eager singleton).
 * Existing environments are baselined at version 1 so the V1 baseline script
 * is skipped there; fresh databases are built entirely from db/migration.
 */
@Singleton
public class FlywayMigrationInitializer {

    private static final Logger logger = LoggerFactory.getLogger(FlywayMigrationInitializer.class);

    @Inject
    FlywayMigrationInitializer(final DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        MigrateResult result = flyway.migrate();
        logger.info("Flyway migration complete: {} migration(s) applied, schema version {}",
                result.migrationsExecuted, result.targetSchemaVersion);
    }

}
