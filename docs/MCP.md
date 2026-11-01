# DAF MCP server (daf-mcp)

The DAF API as tools for agents. `daf-mcp` is an MCP (Model Context Protocol) server over Streamable HTTP: every `daf-api` endpoint is a tool, and a tool call becomes that endpoint's request, sent with the caller's own DAF token. DAF decides what the user may do, exactly as for the UI. The server holds no credentials and changes nothing in `daf-api`.

## 1. How it fits

| Piece         | Detail                                                                                                                                            |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Module        | `mcp/` (`daf-mcp`): Spring Boot, Spring AI MCP server starter (WebMVC), MCP Java SDK; its own jar and image, same build as the other modules       |
| Endpoint      | `POST /mcp` (and `GET /mcp` for the server's notification stream), Streamable HTTP; protocol handled by the SDK                                   |
| Container     | `daf-mcp` in the `daf` stack, host port 14740, next to `daf-api`; `DAF_API_BASE` points at `daf-api` inside the stack                             |
| Gateway       | `/gateway/mcp/daf` -> `daf-mcp` `/mcp` (cat-utils `co-api-gateway`, `DAF_MCP_HOST` / `DAF_MCP_PORT`); the platform nginx fronts the gateway        |
| Credential    | `Authorization: Bearer <DAF token>` on the MCP connection: the role token the DAF remote obtains at sign-in, forwarded as is on every tool call    |
| Health        | `GET /actuator/health` on the container (the stack's `latest.sh` verifies it)                                                                     |

## 2. Tools

| Group        | Prefix        | DAF resource                                                             | Tools |
| ------------ | ------------- | ------------------------------------------------------------------------ | ----- |
| Agenda       | `agenda_`     | the committee Agenda Builder (`/approvalForum/agenda`)                   | 68    |
| Workflow     | `workflow_`   | credit application requests, decisions, lists, chat (`/approvalForum`)   | 66    |
| External     | `external_`   | counterparties, facilities and applications from CASDM                   | 13    |
| Credit Data  | `creditdata_` | countries, industries, counterparty classification                       | 3     |
| Documents    | `documents_`  | request documents: list, upload, download, archive                       | 4     |
| Auth         | `auth_`       | ping, domains, users, groups, SSO servers; the sign-in endpoints         | 13    |
| Docstash, email, scheduler, git | `docstash_`, `email_`, `scheduler_`, `git_` | jobs and build information      | 8     |

| Category  | What                                                                                              | Served by default |
| --------- | ------------------------------------------------------------------------------------------------- | ----------------- |
| `user`    | the API a signed-in user works with (what the DAF remotes and the Swagger-documented API expose)  | yes               |
| `sign-in` | `auth_login`, `auth_sso_token`, `auth_sso_tenant`, `auth_role_token`, `auth_keycloak_config`, `auth_platform_token`: take credentials or another system's token | no |
| `system`  | scheduled jobs and their triggers, the scheduler probe, mail jobs, build information             | no                |

| Tool input                | Detail                                                                                                        |
| ------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Path parameters           | required arguments, named as in the endpoint (`agiId`, `workflowId`, …)                                       |
| Query parameters          | optional arguments; the endpoint's default is in the description                                              |
| Request body              | the `body` argument, an object whose fields are the DTO's (nested objects described to four levels)           |
| File upload               | `fileName` and `fileContentBase64`, plus the form fields (`docType`, …)                                       |
| Download                  | answered as `{fileName, contentType, size, contentBase64}`, up to `DAF_MCP_MAX_DOWNLOAD_BYTES`                |
| Description               | the endpoint's Swagger description, ending with the endpoint it calls, for example `[GET /approvalForum/agenda/committees]` |
| Annotations               | `readOnlyHint` on GET tools, `destructiveHint` on DELETE tools                                                |

| Answer                      | Detail                                                                                       |
| --------------------------- | -------------------------------------------------------------------------------------------- |
| 2xx with JSON               | the JSON, pretty-printed, cut at `DAF_MCP_MAX_RESPONSE_CHARS` with a note                    |
| 2xx without a body          | `HTTP 204 (no content)`                                                                      |
| 4xx / 5xx                   | a tool error: `DAF answered HTTP 403 for POST …: {"message": …}`                             |
| No bearer on the connection | 401 at the connection when `DAF_MCP_REQUIRE_TOKEN=true` (default); otherwise a tool error    |
| Missing or invalid argument | a tool error before DAF is called (the SDK validates arguments against the tool's schema)    |

## 3. Settings (env.properties)

| Variable                     | Default                                  | Meaning                                                                       |
| ---------------------------- | ---------------------------------------- | ----------------------------------------------------------------------------- |
| `MCP_PORT`                   | 14740                                    | host port of the container                                                    |
| `DAF_API_BASE`               | `http://daf-api:8080/approvalforum/api`  | daf-api's JAX-RS root (set in docker-compose.yml)                             |
| `DAF_MCP_CATEGORIES`         | `user`                                   | categories served, comma-separated: `user`, `sign-in`, `system`               |
| `DAF_MCP_GROUPS`             | blank = all                              | groups served, comma-separated (`agenda,workflow,…`)                          |
| `DAF_MCP_READ_ONLY`          | `false`                                  | `true` serves only GET tools                                                  |
| `DAF_MCP_REQUIRE_TOKEN`      | `true`                                   | refuse connections without a bearer (401)                                     |
| `DAF_MCP_ENDPOINT`           | `/mcp`                                   | the MCP endpoint path                                                         |
| `DAF_MCP_KEEP_ALIVE`         | `30s`                                    | keep-alive pings on the notification stream                                   |
| `DAF_MCP_REQUEST_TIMEOUT`    | `60s`                                    | per daf-api call                                                              |
| `DAF_MCP_MAX_RESPONSE_CHARS` | 200000                                   | longer answers are cut                                                        |
| `DAF_MCP_MAX_DOWNLOAD_BYTES` | 10485760                                 | larger downloads are refused                                                  |
| `MAX_HTTP_HEADER_SIZE`       | 65536                                    | the DAF token carries the user's roles (same as daf-api)                      |

## 4. Getting a DAF token

| Where the user is                | How                                                                                                                                             |
| -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Signed in to the DAF remote      | the token in the tab's session storage (`daf.session.token`); it is the same token the remote sends on every API call                            |
| Credit platform (shell) session  | `POST /gateway/auth/api/v1/auth/token/exchange {systemCode: DAF}` with the platform token, then `POST /gateway/daf/auth/platformToken` with the exchanged token as bearer: answers `{Token, tenantId, domain, displayName}` |
| Credentials, no browser          | `POST /gateway/daf/auth/login` with headers `domain`, `username`, `password`: answers `{Token, Roles, …}` (the legacy sign-in; also served as the `auth_login` tool when `DAF_MCP_CATEGORIES` includes `sign-in`) |

Tokens expire as DAF's own tokens do; reconnect with a new one.

## 5. Connecting an agent

| Client       | Configuration                                                                                                                                                              |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Kiro         | `.kiro/settings/mcp.json`: `{"mcpServers": {"daf": {"url": "https://credit-systems-dev.rmb.co.za/gateway/mcp/daf", "headers": {"Authorization": "Bearer ${DAF_TOKEN}"}}}}` |
| Inspector    | `npx @modelcontextprotocol/inspector --server-url http://localhost:14740/mcp --transport http` (add the header in the UI)                                                   |
| Any client   | Streamable HTTP, the URL above, the bearer header; stdio-only clients use the `mcp-remote` bridge                                                                           |

## 6. Build, test and run

| Task                | Command (repository root)                                                                                                 |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Build and test      | `./mvnw -B -pl mcp test` (JDK 25). The tests start the server, a stand-in daf-api and an MCP client over Streamable HTTP  |
| Jar                 | `./mvnw -B -pl mcp -DskipTests package` -> `mcp/target/daf-mcp.jar`                                                       |
| Run locally         | `DAF_API_BASE=http://localhost:14736/approvalforum/api java -jar mcp/target/daf-mcp.jar` (port 8080)                      |
| Image               | TeamCity Daf Boot build: `credit/daf-boot/daf-mcp:<n>`; the `daf` stack starts it with the other containers               |
| Smoke test          | `npx @modelcontextprotocol/inspector --cli http://localhost:14740/mcp --transport http --header "Authorization: Bearer <token>" --method tools/list` |

## 7. The tool catalog

| Item        | Detail                                                                                                                                                     |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| File        | `mcp/src/main/resources/daf-tools.json`: one entry per endpoint (name, group, category, description, verb, path, parameters, body schema, upload, download) |
| Generator   | `python mcp/tools/generate-tool-catalog.py` reads the resource classes (paths, verbs, parameters, Swagger descriptions) and the DTOs (api sources, legacy model jar through `javap`); `--check` exits 1 when the catalog is out of date |
| Test        | `ToolCatalogTest` fails when the catalog does not cover exactly the endpoints daf-api serves (the api module's legacy inventory and additions)             |
| After a change to a resource | regenerate the catalog, run `--check`, run the mcp tests                                                                                  |
