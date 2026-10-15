# DAF on Java 25: like-for-like plan and results

Target: every endpoint, every scheduled job and the rules engine of the legacy DAF (review mandates) running on Java 25 / Spring Boot 4.0.3, with the business logic copied unchanged and parity proven by tests.

## Source of truth

| Item                 | Value                                                                                         |
| -------------------- | --------------------------------------------------------------------------------------------- |
| Legacy repository    | `reviewmandates`, the newest branch, plus the fix only on master (`getActiveDirectoryUser` uses `setMaxResults(1)`) |
| Main API             | `approvalforum-model`, `-jdbc`, `-service`, `-security`, `-web` (war `/approvalforum`, JAX-RS on RESTEasy under `/api`) |
| Rules service        | `approvalforumrules/approvalforumdrools` (Drools 7.8.0, war `/approvalforumdrools`, `/api`)   |
| Document generation  | `approvalforum-docgen` (Spring Boot 2.1, flying-saucer/iText PDF)                             |
| SOAP service         | `approvalforum-soap` (Spring Boot 2.2, Spring WS, XWSS)                                       |
| Not Java, unchanged  | `approvalforum-socketio` (Node socket.io), `approvalforum-proxy` (nginx config of the old UI), `approvalforum-ui` (replaced by the platform remotes) |

## Inventory

| What                   | Legacy                                                                                                   | Count |
| ---------------------- | -------------------------------------------------------------------------------------------------------- | ----- |
| API endpoints          | `ApprovalForumResource` 57, `ApprovalForumAgendaResource` 56, `ApprovalForumExternalResource` 13, `ApprovalForumAuthResource` 12, `ApprovalForumDocumentsResource` 4, `ApprovalForumDocstashResource` 3, `ApprovalForumEmailReceivedResource` 2, `ApprovalForumSchedulerResource` 1, `GitResource` 1 | 149 |
| Rules endpoints        | `ApprovalForumDroolsResource` 2, `GitResource` 1                                                          | 3     |
| Docgen endpoints       | Agenda and minutes PDFs, ping                                                                             | all   |
| SOAP operations        | `createExternalRequest`, `cancelExternalRequest`, WSDL and XSD                                            | 2     |
| Scheduled jobs         | DailyApproverMail, DailyCapturerMail, ResendDailyMails, ExternalRequests, DocStashRequests, UnsentMailRequests, CleanupRequests, EmailReceived, AgendaCutOff, AgendaReminder (MailRequests and ADUCleanup are commented out in legacy and stay off) | 10 |
| Scheduler start        | `GET /approvalforum/api/scheduler/scheduler`, called by the container `HEALTHCHECK` every 300 s           | 1     |
| Tenant handling        | 326 lines across 20 classes (`tenantId` on requests, workflows, external requests, meeting names; Ashburton = tenant 2, Broader Africa > 2; SOAP forwards tenant 2 to the Ashburton endpoints) | kept verbatim |
| Guice wiring           | 12 service bindings, 5 repositories, 4 security bindings, scheduler provider, 9 resources + `AuthenticationFilter`, 19 client providers, `af_settings` (defaults + env overrides) | 1:1 Spring config |

## Approach

| Decision                          | Why                                                                                                       |
| --------------------------------- | --------------------------------------------------------------------------------------------------------- |
| Keep JAX-RS, run it on Jersey 4   | `spring-boot-starter-jersey` (Jersey 4, `jakarta.ws.rs`). Resources, `@QueryParam` defaults, body binding, `SecurityContext`, filters and exception mapping keep their semantics |
| Keep the legacy packages          | `za.co.rmb.approvalforum*` copied as is, so a diff against legacy shows only the mechanical rewrites      |
| Copy by script, not by hand       | `tools/sync-legacy.py` copies the sources and applies only `javax` → `jakarta`, Guice → Spring/JSR-330 annotations and the patches listed in the script; re-running it changes nothing |
| Legacy jars transformed, not copied | `legacy-libs/` rewrites the exact client and model jars to `jakarta` with the Eclipse Transformer        |
| Guice modules → Spring config     | Same bindings and scopes (unscoped → request scope, `@Singleton` → lazy singleton, circular proxies as Guice), same `af_settings` keys, defaults and overrides |
| Platform behaviour restored       | Hibernate 7 kept to the legacy SQL (`TOP` paging, null procedure parameters fail), RESTEasy JSON error answers, Spring 5.2 REST client errors, Java 8 NPE messages |
| Drools 7.8 → Drools 10            | `RuleData.drl` unchanged; one internal API (`KnowledgeHelper` → `RuleContext`) in a logging helper        |
| SOAP security XWSS → WSS4J        | XWSS has no Jakarta version; the same policy (UsernameToken, plain password, no nonce) on WSS4J            |
| Separate deployables kept         | `daf-api`, `daf-rules`, `daf-docgen`, `daf-soap` as separate containers                                    |

## Modules in daf-boot

| Module    | Artifact     | Replaces                         | Context path              |
| --------- | ------------ | -------------------------------- | ------------------------- |
| `api/`    | `daf-api`    | `approvalforum-web` war          | `/approvalforum` + `/api` |
| `rules/`  | `daf-rules`  | `approvalforumdrools-web` war    | `/approvalforumdrools` + `/api` |
| `docgen/` | `daf-docgen` | `approvalforum-docgen`           | `/docgen`                 |
| `soap/`   | `daf-soap`   | `approvalforum-soap` war         | `/approvalforum-soap`     |

## Phases

| # | Phase                    | Status | Evidence                                                                                          |
| - | ------------------------ | ------ | ------------------------------------------------------------------------------------------------- |
| 1 | API re-sync              | done   | 161 classes copied by script; re-sync reproduces every file                                        |
| 2 | API wiring               | done   | Starts against `wc_approvalforum`; Flyway state unchanged                                          |
| 3 | Scheduler                | done   | `SchedulerParityTest`; health check starts the 10 jobs in the container                            |
| 4 | Rules on Drools 10       | done   | `RulesGoldenTest` (24,648 legacy answers), HTTP replay identical                                   |
| 5 | Docgen and SOAP          | done   | Docgen PDFs byte-identical after masking; SOAP side by side (below)                                |
| 6 | Parity tests             | done   | Below                                                                                              |
| 7 | Build and deploy         | done   | Root pom builds all modules; four Dockerfiles; compose stack verified with `latest.sh`; TeamCity builds four images |

## Parity tests

| Test or tool                               | Kind            | Proves                                                                                   |
| ------------------------------------------ | --------------- | ---------------------------------------------------------------------------------------- |
| `LegacyEndpointInventoryTest`              | unit (build)    | The Jersey model serves exactly the 149 legacy verbs and paths                            |
| `RulesEndpointInventoryTest`               | unit (build)    | The rules service serves exactly the 3 legacy endpoints                                   |
| `SchedulerParityTest`                      | unit (build)    | Same 10 jobs, names, groups, triggers, cron keys and defaults; jobs built and injected     |
| `RulesGoldenTest`                          | unit (build)    | 24,648 rule inputs give the legacy answers on Drools 10                                   |
| `ExternalRequestParityTest`                | unit (build)    | SOAP forwards each operation and tenant to the legacy endpoint with the same request; same faults |
| `tools/sync-legacy.py` re-run              | check           | The copied sources equal legacy after the mechanical rewrites                              |
| `api/tools/parity-compare.py`              | side by side    | Every GET: same status, media type and body                                                |
| `api/tools/parity-write.py` + scenarios    | side by side    | Writes: same answers and the same rows (table checksums) on two copies of one database     |
| `rules/tools/rules-golden.py`              | recorder        | Records the legacy rules answers the golden test replays                                   |
| `docgen/tools/docgen-parity.py`            | side by side    | Same PDFs (byte for byte after masking clock, document id and font subset prefixes)        |
| `soap/tools/soap-parity.py`                | side by side    | Same WSDL/XSD, forwarded requests and SOAP answers, both services pointed at one recording stub |

## Results

| Area                   | Result                                                                                      |
| ---------------------- | ------------------------------------------------------------------------------------------- |
| GET endpoints          | 67 of 67 identical (side-effect GETs exercised in the write scenarios)                        |
| Agenda writes          | 47 of 47 steps identical, 10 tables identical                                                 |
| Workflow writes        | 35 of 35 steps identical, 24 tables identical                                                 |
| Remaining writes       | 30 of 30 steps identical, 18 tables identical                                                 |
| Endpoints exercised    | 149 of 149                                                                                     |
| Scheduler              | Same job registration responses and logs; started by the health check in the container         |
| Rules                  | 24,648 of 24,648 identical; reload error and 401 identical                                    |
| Docgen                 | 5 of 5 identical                                                                              |
| SOAP                   | 36 of 46 identical, 10 differ in the fault string only (same status, fault code and forwarded request), 0 other differences |

## Known differences

| Difference                                         | Where                       | Why                                                                                     |
| -------------------------------------------------- | --------------------------- | --------------------------------------------------------------------------------------- |
| JSON parse error message wording                    | API 400 answers             | Jackson 2.8 → 2.20 changed its exception messages; status and media type identical        |
| Container error page banner                         | API/rules error pages       | Tomcat 9 → Tomcat 11 HTML                                                                 |
| Swagger / Springfox documentation endpoints dropped | API, docgen                 | No Jakarta version of the libraries; not business endpoints                              |
| SOAP security fault string                          | SOAP, wrong or no UsernameToken | XWSS → WSS4J wording; same `Client` fault                                             |
| SOAP fault string when the API answers 200 with a JSON body | SOAP                 | Class name `jakarta.xml.ws.Response` and Spring 6 message; same `Server` fault. The API answers these calls with no body, so this does not occur |
| `Double.toString` for some values                   | Anywhere a double becomes text | Java 19 changed the shortest representation of some doubles; no case found in the parity runs |
