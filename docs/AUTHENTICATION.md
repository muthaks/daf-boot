# Authentication and permissions

How the Committee Agendas remote signs a user in to DAF and decides what to show. The DAF backend is unchanged; everything here is what DAF's own Angular 8 UI already does, moved into the remote.

## 1. Platform sign-in

DAF's `AuthenticationFilter` accepts only DAF's own role token, so the remotes always send that. Hosted in the Credit shell they get it from the platform sign-in, without Windows SSO or a second prompt:

| # | Call                                                               | Who          | Notes                                                                                                                        |
| - | ------------------------------------------------------------------ | ------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| 1 | `POST /gateway/auth/api/v1/auth/token/exchange {systemCode: DAF}`  | shell        | The platform token exchanged for a token for the `DAF` system (shell registry API entry `daf-platform-token`)                 |
| 2 | `POST /daf/auth/platformToken` with that token as bearer           | remote → DAF | DAF verifies it (signature with DAF's signing secret, expiry, `systemCode` DAF, user not system token)                       |
| 3 | AD lookup of the user by domain and account                        | DAF          | Through the auth service, as for every DAF AD call; nothing about the user is taken from the request                         |
| 4 | DAF's own `/auth/tenant` and `/auth/ssoToken` logic                | DAF          | Same tenant and roles as a Windows SSO sign-in; answers `{Token, tenantId, domain, displayName}`, or 401 `{reason}`          |

| DAF setting                         | Use                                                                                                                       |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `AF_SYSTEM_CALLER`                  | DAF's system code on the auth service (`DAF`); only tokens issued for it are accepted                                    |
| `AF_SYSTEM_BOOTSTRAP_CLIENT_SECRET` | DAF's bootstrap client secret: exchanged at `<AF_AUTHSERVICE_BASE>/api/v1/auth/bootstrap` for DAF's JWT signing secret, fetched again after a rotation |
| `AF_AUTHSERVICE_BASE`               | The auth service through the gateway                                                                                      |
| `AF_PLATFORM_TOKEN_ISSUER`          | Optional: the `iss` the auth service stamps (`JWT_ISSUER`); blank = not checked                                           |
| `MAX_HTTP_HEADER_SIZE`              | Default 65536: the exchanged token carries the user's groups (DAF has no group scope on the auth service)                |

When the platform sign-in is refused (DAF unreachable, settings missing, user not in AD) the hosted remote falls back to the Windows SSO sequence below.

## 2. Sign-in sequence

| # | Call                                                        | Who     | Notes                                                                                   |
| - | ----------------------------------------------------------- | ------- | --------------------------------------------------------------------------------------- |
| 1 | `GET /daf/auth/ssoServers`                                  | DAF     | `af.sso.servers`, the SSO server list DAF itself uses                                   |
| 2 | `GET <sso server>userdetail` (with credentials)             | SSO     | Windows session → `WindowsAuthDetail` (userPrincipalName, groups, displayName, hostDomain) |
| 2b| `POST <sso server>loginpost {domain, username, password}`   | SSO     | Standalone fallback when Windows SSO is unavailable; answers `{isValid, userDetail}`      |
| 3 | `POST /daf/auth/tenant` (the detail)                        | DAF     | `{tenantId}`; RMB or Ashburton                                                            |
| 4 | `POST /daf/auth/ssoToken` (the detail)                      | DAF     | `{Token}`: DAF's role token, claims `sub` (UPN) and `roles`                                |
| 5 | `Authorization: Bearer <Token>` on `/daf/approvalForum/...` | remote  | Attached by `DafHttpClient` in both modes                                                 |

| Mode       | Where sign-in happens                                                                                                  |
| ---------- | ---------------------------------------------------------------------------------------------------------------------- |
| Standalone | `features/auth/login.component` on the co-ds login page: silent SSO once per tab, otherwise credentials via `loginpost` |
| Hosted     | `dafSessionGuard` → `SessionService.ensure()` runs the platform sign-in (section 1), otherwise steps 1 to 4 silently. On failure pages render an error state pointing to the standalone sign-in; the shell viewport is never blank |

Token and user live in `sessionStorage` (`daf.session.token`, `daf.session.user`) for the tab. `daf.sso_failed` stops silent SSO after a failure or sign-out.

## 3. Roles to permissions

DAF puts its role strings in the token. The remote never compares them in components; `core/models/permissions.ts` derives named permissions and the directives `*dafCan` / `[dafDisableUnless]` and `getRemoteNavigation` read them.

| Permission                    | DAF roles (contains, case-insensitive)                              | Used for                                                                      |
| ----------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `agenda.view`                 | CreditAnalyst, Chairperson, Secretary, ReviewAnalyst, Approver      | Open the module: lists, dashboard, item and meeting pages                     |
| `agenda.book`                 | CreditAnalyst                                                       | Book agenda item                                                              |
| `agenda.submit`               | CreditAnalyst, Secretary, Chairperson                               | Submit an item for review                                                     |
| `agenda.review`               | ReviewAnalyst, Approver, Chairperson, Secretary                     | Return to analyst, reject                                                     |
| `agenda.approve`              | Approver, Chairperson                                               | Approve a submitted item                                                      |
| `agenda.reopen`               | Secretary, Chairperson                                              | Re-open an item; submit with a quorum override                                |
| `agenda.validateDocuments`    | ReviewAnalyst, Secretary, Chairperson                               | Accept, reject or waive credit papers                                         |
| `agenda.manage`               | Secretary, Chairperson                                              | Compile, lock, mark held, close a meeting                                     |
| `agenda.unlockPostMeeting`    | ReviewAnalyst (DAF `af.agenda.reviewer.role`)                       | Unlock a signed-off post-meeting record                                       |
| `agenda.reopenClosedMeeting`  | ReviewAnalyst                                                       | Audited correction of a closed meeting record                                 |
| `agenda.cancelMeeting`        | Secretary                                                           | Cancel a meeting                                                              |
| `agenda.admin`                | Secretary                                                           | Committee administration: committees, cut-off rules, quorum roles, calendar   |

Each rule mirrors the role helper in DAF's own agenda pages (DAF commit `caf8b602`, BRS v0.1). Status conditions, such as an item being Submitted, stay in the pages.

DAF enforces roles on every call; the UI rules only decide what is shown.

## 4. Known weaknesses in DAF (pre-existing, not introduced here)

| Endpoint                | Issue                                                                     |
| ----------------------- | ------------------------------------------------------------------------- |
| `POST /auth/ssoToken`   | `@PermitAll`; trusts the posted groups when `hostDomain` is `rmb.co.za`   |
| `POST /auth/roleToken`  | `@PermitAll`; decodes the posted JWT without verifying its signature      |

DAF's own UI uses the same endpoints. Worth a ticket for the DAF team, independent of this remote.

## 5. Troubleshooting

| Symptom                                                        | Meaning                                                                                      |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| Hosted: "DAF did not accept your platform sign-in, and Windows SSO could not sign you in" | `POST /daf/auth/platformToken` was refused (see its `reason` and the daf-api log) and no SSO server answered `userdetail`. Open the standalone URL and use credentials |
| `platformToken` 401 "not configured"                           | `AF_SYSTEM_CALLER`, `AF_SYSTEM_BOOTSTRAP_CLIENT_SECRET` or `AF_AUTHSERVICE_BASE` blank on daf-api                             |
| `platformToken` 401 "A platform token for DAF is required"     | No exchanged token, a token for another system, or the signing secret could not be fetched (daf-api log: "Platform token key") |
| `platformToken` 400 from Tomcat                                | Header larger than `MAX_HTTP_HEADER_SIZE`                                                                                     |
| 401 on every agenda call                                       | DAF token missing or expired; sign in again                                                  |
| 403 / "no agenda role"                                         | Token has none of the five DAF roles; DAF AD groups (`OU=DAF`) decide                          |
| Gateway 502 on `/gateway/daf/...`                              | `DAF_API_HOST` wrong or DAF down; `GET /gateway/daf/auth/ping` should answer 200              |
| CORS error in the browser                                      | Remote origin missing from the gateway's `CORS_ALLOW_ORIGINS`                                |
