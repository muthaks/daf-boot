# Migrating the DAF Agenda Builder to the platform

Source: `reviewmandates` `approvalforum-ui/src/app/core/agenda/` (Angular 8, ng-bootstrap, 2,600 lines of custom SCSS). Target: this repository, Angular 22 remote on `co-ds`, no custom CSS.

## What moved

| DAF (Angular 8)                              | daf-boot (Angular 22)                                          | Status |
| -------------------------------------------- | -------------------------------------------------------------- | ------ |
| `services/agenda.service.ts`                 | `core/services/agenda.service.ts` (typed, same endpoints)      | done   |
| `SessionService` roles + `RoleGuard`          | `core/models/permissions.ts`, `PermissionsService`, directives | done   |
| SSO / role-token sign-in (`login`, `AuthCloakGuard`) | `core/services/auth.service.ts`, `SessionService`, login page | done   |
| `agenda-list.component`                      | `features/agenda/agenda-list.component`                        | done   |
| `agenda-dashboard.component`                 | `features/agenda/agenda-dashboard.component`                   | done   |
| `agenda-new.component`                       | `features/agenda/agenda-new.component`                         | done   |
| `agenda-existing.component`                  | `features/agenda/agenda-item.component`                        | done   |
| `agenda-meeting.component`                   | `features/agenda/agenda-meeting.component`                     | done   |
| Nav menu entries, routes                     | `remote/remote.navigation.ts`, `features/agenda/agenda.routes.ts` | done |

## BRS v0.1 alignment (DAF commit `caf8b602`, 2026-09-15)

DAF changed the Agenda Builder while the first port was in progress. This port brings every UI change across.

| DAF change                                                        | daf-boot                                                                  |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------- |
| 26 new endpoints in `agenda.service.ts`                           | `core/services/agenda.service.ts`, same paths, verbs and parameters       |
| New DTOs: condition, meeting record, participant, quorum, report, workflow action | `core/models/agenda.models.ts`                                |
| Item workflow: submit, return, approve, reject, re-open, history  | `agenda-item.component` Workflow card and tab                             |
| Document version, mandatory flag, validation                      | `agenda-item.component` Documents tab                                     |
| Participants, attendance, role quorum with override               | `agenda-item.component`; `agenda-new.component` sends the participants    |
| Conditions and actions                                            | `agenda-item.component` Conditions tab                                    |
| Decision type and date                                            | `agenda-item.component` Post-meeting tab                                  |
| Meeting cancel, close, re-open, held with actuals, records        | `agenda-meeting.component`                                                |
| Operational report with Excel export                              | `agenda-dashboard.component`                                              |
| Committee Administration page                                     | `agenda-admin.component`, route `agenda/admin`                            |
| Secretary role; module open to all five DAF roles                 | `core/models/permissions.ts` (see `AUTHENTICATION.md` §3)                 |
| New item statuses Submitted, Returned, Approved, Rejected         | Status chips and colours in list, dashboard, item and meeting pages       |

Every `window.prompt()` and `confirm()` in DAF is a co-ds dialog or `CoConfirmService` here. DAF's backend-only changes (V13 migration, Graph Teams and Outlook events, service tests) need nothing in the remote.

## What stays in DAF for now

| Piece                                              | Reason                                                              |
| -------------------------------------------------- | ------------------------------------------------------------------- |
| `approvalforum-web` REST API, services, Flyway     | Backend unchanged by design; `api/` here is the future home         |
| Capture, review, approve, chair modules (Angular 8)| Out of scope; migrate module by module the same way                 |
| DAF's own login page and nav                       | Only needed while the Angular 8 app still serves the other modules  |

## Rules applied

| Rule                                   | Source                                                         |
| -------------------------------------- | -------------------------------------------------------------- |
| No custom CSS; compose co-ds only      | `cat-utils` `co-platform/docs/REMOTE-STANDARDS.md` §4          |
| Five-file `remote/` contract           | REMOTE-STANDARDS §3                                            |
| Fail-soft session guard                | REMOTE-STANDARDS §3                                            |
| co-ds from Nexus, not a file link      | `cat-utils` `design-system/docs/publishing.md`                 |
| Named permissions, signals             | ccirm remote (the template for this repo)                      |

## Next steps

| Step                                             | Owner         |
| ------------------------------------------------ | ------------- |
| Deploy the rmb-rbvdvcrsk02 nginx with its new `/approvalforum` location (DAF DEV, `daf-api` on 40200) so the gateway can reach DAF over 443 | platform team |
| Register `DAF` in the auth service and accept its tokens in `approvalforum-security` (drops steps 2 to 4 of the sign-in) | DAF + platform |
| Raise the `@PermitAll` weaknesses on DAF's `ssoToken` / `roleToken` | DAF team |
| Backend migration into `api/` (Java 25, Spring Boot 4), endpoint-compatible with `/approvalForum/agenda` | DAF team |
