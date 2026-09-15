# Authentication and permissions

How the Committee Agendas remote signs a user in to DAF and decides what to show. The DAF backend is unchanged; everything here is what DAF's own Angular 8 UI already does, moved into the remote.

## 1. Why not the platform token

| Fact                                                     | Consequence                                                                                            |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| DAF's `AuthenticationFilter` parses only DAF's own role token (HMAC, DAF secret) or an SSO token           | An auth-service token is rejected; the shell's token exchange cannot be used                            |
| DAF issues its role token from a Windows user description (`WindowsAuthDetail`)                            | Any client that can obtain that description from an SSO server can obtain a DAF token                   |
| The platform shell already performs Windows SSO against the same SSO servers                              | The user signs in once; the remote repeats the silent SSO step for DAF without a second prompt          |

Onboarding DAF to the auth service (a `DAF` system code, ConnectX style) is the later step; when it happens the shell exchanges a DAF token and steps 2 to 4 below disappear. The agenda calls do not change.

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
| Hosted     | `dafSessionGuard` → `SessionService.ensure()` runs steps 1 to 4 silently. On failure pages render an error state pointing to the standalone sign-in; the shell viewport is never blank |

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
| Hosted: "DAF could not sign you in with Windows SSO"           | No SSO server answered `userdetail` for this browser. Open the standalone URL and use credentials |
| 401 on every agenda call                                       | DAF token missing or expired; sign in again                                                  |
| 403 / "no agenda role"                                         | Token has none of the five DAF roles; DAF AD groups (`OU=DAF`) decide                          |
| Gateway 502 on `/gateway/daf/...`                              | `DAF_API_HOST` wrong or DAF down; `GET /gateway/daf/auth/ping` should answer 200              |
| CORS error in the browser                                      | Remote origin missing from the gateway's `CORS_ALLOW_ORIGINS`                                |
