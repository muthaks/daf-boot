# DAF Committee Agendas (daf-boot)

The DAF Credit Committee Agenda Builder as a Credit platform remote: Angular 22 on the Credit design system (`co-ds`), hosted in the Credit shell or standalone. The backend is the existing DAF (`approvalforum`) API, unchanged, reached through the Credit API gateway. `api/` is reserved for the Java 25 / Spring Boot 4 migration of that backend.

| Folder    | What                                                                                                    |
| --------- | ------------------------------------------------------------------------------------------------------- |
| `web/`    | Angular 22 Native Federation remote, name `daf`, dev port 4408. Agenda list, booking, item, meeting, dashboard |
| `api/`    | Placeholder for the DAF backend migration (Java 25, Spring Boot 4). Empty today                          |
| `e2e/`    | Playwright smoke tests (standalone and hosted), fixtures only, no real DAF                               |
| `deploy/` | PAM deploy tool, `daf-remote` stack, DEV target, `releases/dev.env`                                      |
| `docs/`   | [`AUTHENTICATION.md`](docs/AUTHENTICATION.md) sign-in and permissions, [`MIGRATION.md`](docs/MIGRATION.md) what moved from DAF and what did not |

## In one table

| Piece            | How it works                                                                                                                                                                          |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend          | DAF `ApprovalForumAgendaResource` and `ApprovalForumAuthResource`, unchanged. The gateway route `/daf/<path>` rewrites to `<DAF server>/approvalforum/api/<path>`                        |
| Sign-in          | DAF's own: SSO servers from `/auth/ssoServers`, Windows user detail from the SSO server, `/auth/tenant` + `/auth/ssoToken` issue DAF's role token. Hosted: silent, one sign-in for the user |
| Permissions      | DAF roles in the token (`creditanalyst` books, `chairperson` manages) mapped to `agenda.view`, `agenda.book`, `agenda.manage` in `core/models/permissions.ts`                          |
| Runtime config   | `assets/config.json` (`apiUrl`) rendered by the container from `DAF_API_URL`. Nothing environment-specific in the image                                                                |
| Design system    | `co-ds` from Nexus (`"co-ds": "^1.0.0"`, `web/.npmrc`). No custom CSS in the remote; co-ds components only                                                                              |
| Shell            | Optional. Registry entry `daf` in the shell (`cat-utils` `co-platform/shell`), category Credit, no `authType` because DAF is not an auth-service audience                              |

## Run locally

```bash
cd web
npm ci --legacy-peer-deps      # co-ds from Nexus (VPN)
npm start                      # http://localhost:4408, standalone with its own login
# hosted: the Credit shell (cat-utils co-platform/shell) on :4400 lists it under Credit

cd e2e && npm install && npm test
```

`web/src/assets/config.json` points at the local gateway (`http://localhost:19080/daf`), whose `/daf` route must reach a DAF instance (`cat-utils` `co-api-gateway`, `DAF_API_HOST`).

## Build and ship

| Step    | Detail                                                                                                                                                  |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Build   | TeamCity **Daf Boot → Build** (`cat-utils` `.teamcity/DafBoot`): `npm ci` + `ng build`, image `credit/daf-boot/daf-remote:<n>` and `:latest`             |
| Trigger | Any commit here except `e2e/`                                                                                                                           |
| Server  | Platform DEV server `rmb-rbvdvcsdm01` (`credit-systems-dev.rmb.co.za`), stack dir `/opt/docker/daf-remote`, host port 14708, subnet `172.31.100.96/29`   |
| Tags    | DEV always runs `:latest`. Pinned builds (`-b <n>` or a release manifest) are for QA/UAT/PROD                                                            |

| Purpose                       | Command (VPN, in this repo)                                              |
| ----------------------------- | ------------------------------------------------------------------------ |
| Upgrade on DEV                | `deploy/pam-deploy.sh daf-remote rmb-rbvdvcsdm01 -u <your AD user>`      |
| Upload only, no restart       | `deploy/pam-deploy.sh daf-remote rmb-rbvdvcsdm01 -u <your AD user> --no-run` |
| PAM shell on the server       | `ssh -p 4422 -l 'rmb.co.za\<your AD user>@docker@rmb-rbvdvcsdm01' passwordsafe.fnb.co.za` |
| Restart on the server         | `cd /opt/docker/daf-remote && ./latest.sh`                                |

The deploy tool has no default user: pass `-u` or export `SERVER_USER`. It shows what is deployed, asks before replacing, keeps `env.properties` and runs `latest.sh`.

### Platform pieces (cat-utils, platform team)

| Piece                      | Where                                                                  |
| -------------------------- | ---------------------------------------------------------------------- |
| Gateway route `/daf`       | `co-api-gateway/conf/apisix.yaml` (`daf-api` upstream, `daf` service); `DAF_API_HOST` / `DAF_API_PORT` in its `env.properties` |
| nginx location             | `docker-base/nginx/platform/credit-platform.conf` `/platform/remotes/daf/` → 14708 |
| Shell registry entry       | `co-platform/shell/src/app/core/federation/remote-registry.ts` (`daf`)  |
| Ports and subnets          | `docs/DEPLOYMENT.md`                                                   |

### Verify

| URL                                                                    | Expect                              |
| ---------------------------------------------------------------------- | ----------------------------------- |
| `https://credit-systems-dev.rmb.co.za/platform/remotes/daf/remoteEntry.json` | 200                           |
| `https://credit-systems-dev.rmb.co.za/gateway/daf/auth/ping`           | 200 (gateway → DAF)                 |
| `https://credit-systems-dev.rmb.co.za/platform/`                       | Committee Agendas tile opens the app |
| `https://credit-systems-dev.rmb.co.za/platform/remotes/daf/`           | Standalone login (SSO or credentials) |
