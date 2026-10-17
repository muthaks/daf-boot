# DAF Boot (daf-boot)

The Delegated Authority Forum backend (the legacy review mandates services) on Java 25 / Spring Boot 4.0.3: every endpoint, every scheduled job and the rules engine, with the business logic copied unchanged and parity proven against the legacy stack. Java only: the UI is the Credit platform DAF remote and Agenda Builder remote (`credit-platform` repository), built and deployed from there.

| Folder         | What                                                                                                         |
| -------------- | ------------------------------------------------------------------------------------------------------------ |
| `api/`         | `daf-api`: the legacy `approvalforum` war, 149 endpoints under `/approvalforum/api` and the 10 Quartz jobs   |
| `rules/`       | `daf-rules`: the legacy `approvalforumdrools` war on Drools 10, 3 endpoints under `/approvalforumdrools/api` |
| `docgen/`      | `daf-docgen`: the legacy `approvalforum-docgen` (PDF agendas and minutes) under `/docgen`                    |
| `soap/`        | `daf-soap`: the legacy `approvalforum-soap` (external create/cancel requests) under `/approvalforum-soap`    |
| `support/`     | Shared compatibility code: SQL Server dialect, legacy JSON and error behaviour, Guice-style circular proxies |
| `legacy-libs/` | The legacy client and model jars, rewritten from `javax` to `jakarta` at build time (Eclipse Transformer)    |
| `tools/`       | `sync-legacy.py` (copies the legacy sources), `legacy-endpoints.py` (the expected endpoint lists)            |
| `deploy/`      | PAM deploy tool, `daf` stack, DEV target, `releases/dev.env`                                                 |
| `docs/`        | [`PARITY_PLAN.md`](docs/PARITY_PLAN.md) approach, parity tests and results                                   |

## Build and run locally

| Step   | Command (repo root)                                                                                                        |
| ------ | -------------------------------------------------------------------------------------------------------------------------- |
| JDK    | Java 25 (`JAVA_HOME` pointing at a JDK 25)                                                                                 |
| Build  | `./mvnw -B install` (Nexus, VPN): every module and its parity tests                                                        |
| Run    | `java -jar <module>/target/daf-<module>.jar` with the legacy environment variables (see `env.properties.example`)          |
| Health | `/approvalforum/api/actuator/health`, `/approvalforumdrools/api/actuator/health`, `/docgen/actuator/health`, the SOAP WSDL |

## Keeping in step with legacy

| Step | Action                                                                                                         |
| ---- | -------------------------------------------------------------------------------------------------------------- |
| 1    | Check out the legacy review mandates repository                                                                |
| 2    | `python tools/sync-legacy.py <legacy root> api rules docgen soap`                                              |
| 3    | `git diff`: empty when nothing changed in legacy; otherwise only the legacy change itself                      |
| 4    | `python tools/legacy-endpoints.py <legacy root>` and `./mvnw -B install` (endpoint inventory and parity tests) |

## Build and ship

| Step   | Detail                                                                                                                                                               |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Build  | TeamCity **Daf Boot → Build** (`cat-utils` `.teamcity/DafBoot`): Maven on JDK 25, images `credit/daf-boot/{daf-api,daf-rules,daf-docgen,daf-soap}:<n>` and `:latest` |
| Server | Platform DEV server `rmb-rbvdvcsdm01`, stack dir `/opt/docker/backend/daf-boot`, host ports 14736–14739, subnet `172.31.100.96/29`                                   |
| Tags   | DEV always runs `:latest`. Pinned builds (`-b <n>` or a release manifest) are for QA/UAT/PROD                                                                        |

| Container    | Host port | Replaces (legacy)                                |
| ------------ | --------- | ------------------------------------------------ |
| `daf-api`    | 14736     | `approvalforum` war in the `daf-api` Tomcat      |
| `daf-rules`  | 14737     | `approvalforumdrools` war                        |
| `daf-docgen` | 14738     | `daf-docgen`                                     |
| `daf-soap`   | 14739     | `approvalforum-soap` war in the `daf-api` Tomcat |

| Purpose                 | Command (VPN, in this repo)                                                               |
| ----------------------- | ----------------------------------------------------------------------------------------- |
| Upgrade on DEV          | `deploy/pam-deploy.sh daf rmb-rbvdvcsdm01 -u <your AD user>`                              |
| Upload only, no restart | `deploy/pam-deploy.sh daf rmb-rbvdvcsdm01 -u <your AD user> --no-run`                     |
| PAM shell on the server | `ssh -p 4422 -l 'rmb.co.za\<your AD user>@docker@rmb-rbvdvcsdm01' passwordsafe.fnb.co.za` |
| Restart on the server   | `cd /opt/docker/backend/daf-boot && ./latest.sh`                                          |

| Runtime behaviour  | Detail                                                                                                                                       |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Configuration      | `env.properties`: the legacy containers' variables, read by the same legacy code. A set variable replaces the legacy default even when empty |
| Scheduler start    | As in legacy, the `daf-api` health check (`GET /approvalforum/api/scheduler/scheduler`, every 300 s) starts the Quartz jobs                  |
| Service URLs       | `AF_BASE`, `AF_RULESSERVICE_BASE`, `AF_DOCGENSERVICE_BASE` are set in `docker-compose.yml` (legacy ran the wars in one Tomcat)               |
| Exception messages | `-XX:-ShowCodeDetailsInExceptionMessages`: a NullPointerException message is null, as on Java 8, where the legacy code stores or returns it  |

The deploy tool has no default user: pass `-u` or export `SERVER_USER`. It shows what is deployed, asks before replacing, keeps `env.properties` and runs `latest.sh`.

## Platform pieces (cat-utils, platform team)

| Piece                  | Where                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Gateway route `/daf`   | `co-api-gateway/conf/apisix.yaml` (`daf-api` upstream, `daf` service): the remotes reach `daf-api`; DEV's `co-api-gateway/env/dev.properties` points it at this stack (`host.docker.internal:14736`), other environments still at the legacy DAF                                                                                                                                                                                                                       |
| Sign-in and AD lookups | Through the gateway to auth-service-boot (`AF_AUTHSERVICE_BASE` = `/gateway/auth`): DAF's legacy auth-service calls (`AuthenticateAgainstAD`, users, groups, domains, ping) go to `/api/v1/*` and answer in the legacy models (`AuthServiceBootClient`). Hosted in the Credit shell, `POST /auth/platformToken` gives a platform user DAF's token without Windows SSO (`docs/AUTHENTICATION.md`) |
| Credit Data            | Through the gateway to Credit Data Boot: `AF_*SERVICE_BASE` = `/gateway/creditdata/<service>` (RMB's CASDM), `AF_*SERVICE_ASH_BASE` = `/gateway/creditdata/ash/<service>` (Ashburton's); with a CREDITDATA system token from the auth service (DAF's system sign-in, as for mail; auth-service-boot lists DAF in `AUTH_SYSTEM_TOKENS_CREDITDATA`), added to every Credit Data request by `CreditDataTokenFilter`. `latest.sh` refuses to start with any of these blank |
| Mail                   | Through the gateway to mailservice-boot (`/gateway/mailservice`), with a DAF → MAILSERVICE system token from the auth service (`/gateway/auth`); auth-service-boot lists DAF in `AUTH_SYSTEM_TOKENS_MAILSERVICE`                                                                                                                                                                                                                                                       |
| Ports and subnets      | `docs/DEPLOYMENT.md`                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
