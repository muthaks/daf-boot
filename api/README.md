# api

`daf-api`: the legacy `approvalforum` war (model, jdbc, service, security, web) on Java 25 / Spring Boot 4.0.3.

| Part                                   | Detail                                                                                                        |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `src/main/java/za/co/rmb/approvalforum` | The legacy sources, copied by `../tools/sync-legacy.py` (`javax` → `jakarta`, Guice annotations → Spring/JSR-330); do not edit, re-sync |
| `src/legacy-overrides/java`            | Legacy classes that could not be copied as they are (same name and package), e.g. the Quartz `SchedulerJobFactory` on Spring |
| `src/main/java/za/co/rmb/credit/daf`   | The Guice modules as Spring configuration (`LegacyBindings`, `LegacyModulesConfig`), Jersey, JSON, persistence |
| URLs                                   | `/approvalforum/api/...` as before; JAX-RS on Jersey 4 instead of RESTEasy                                       |
| Settings                               | `af_settings`: legacy defaults, overridden by the legacy `AF_*` variables; JWT settings in `application.properties` |
| Database                               | `DB_URL`, `DB_USER`, `DB_PASSWORD`; Flyway migrations V1–V13 run by the legacy `FlywayMigrationInitializer`     |

| Test or tool                    | Proves                                                                                                   |
| ------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `LegacyEndpointInventoryTest`   | The Jersey resource model serves exactly the 149 legacy verbs and paths (`src/test/resources/legacy/endpoints.txt`) |
| `SchedulerParityTest`           | The scheduler registers the 10 legacy jobs with the same names, groups, triggers and cron settings         |
| `tools/parity-compare.py`       | Every GET answers the same status and body as the legacy stack                                            |
| `tools/parity-write.py`         | Scenarios of writes answer the same and leave the same rows (table checksums) as the legacy stack          |
