# Agenda API tasks

Scope is the Agenda Builder backend in daf-boot `api/` and its legacy mirror. Check off a task only with evidence. A gap is not fixed by editing this file. Each one needs a decision, a code change with tests, a regenerated reference and the legacy mirror.

## Verification

- [x] 1. Generate the exact references from source.
  - `python api/tools/document-agenda-api.py` writes the seven `api-*.md` files; `--check` reports drift.
  - Evidence: 66 endpoints, 19 request/response types, 11 agenda tables, V2–V17 migrations.
  - Requirements: all.

- [x] 2. Unit-test the resource, service, mail and Graph layers.
  - Evidence: `./mvnw -o -pl api test` on JDK 25 — 20 classes, 376 tests, 0 failures, 0 errors, 0 skipped (2026-10-31, including the resource, service, scope, mail, papers, access and settings test classes). Surefire fails the build on any failure.
  - Requirements: 1–19.

- [ ] 3. Run the agenda flow against a real database through the API.
  - Book, upload, submit, review, approve, send invitations, move the meeting, mark held, capture, sign off, close, cancel and reinstate on the local stack.
  - Requirements: 4, 5, 8–13, 18.

- [ ] 4. Verify Graph, AD and mail in a non-production environment.
  - One invitation per approved item; standing roles present; update on meeting move; cancellation on reject/re-open/remove/cancel; notification log statuses.
  - Requirements: 11, 12, 14.

- [ ] 5. Keep the consumer contract aligned.
  - Compare [api-endpoint-contract.md](api-endpoint-contract.md) with the credit-platform spec `specs/agenda-builder/ui-client-contract.md` after every endpoint change.
  - Requirements: 1, 19.

## Gaps found while documenting (decide, then fix with tests)

Each gap names the evidence in the code. Severity: **S** security, **D** data or behaviour, **W** wording, **P** performance.

| # | Sev | Gap | Evidence | Proposed decision |
| --- | --- | --- | --- | --- |
| G1 | S | ~~Any authenticated caller can download any agenda document by id~~ | fixed 2026-10-31: `forbiddenUnlessCanViewDocument` | Done: download needs view access to the document's item |
| G2 | S | ~~Any authenticated caller can close or waive any condition~~ | fixed 2026-10-31: `canCloseCondition`; refused once the meeting is Closed | Done |
| G3 | S | ~~Meeting detail returns every item to any authenticated caller~~ | fixed 2026-10-31: `getMeetingFor` reduces items the caller cannot view to their slot; `lastRoster` returns the roster only; the report lists late-item details only for visible items | Done. Meeting lists, records and members still need only authentication (meeting-level data) |
| G4 | S | ~~`processCutOff` and `processReminders` open to any authenticated caller~~ | fixed 2026-10-31: `forbiddenUnlessScheduler` (`af.scheduler.user`, the subject of `af.token`) | Done |
| G5 | D | ~~A secretary can approve but not reject~~ | fixed 2026-10-31: reject takes the same guard as approve | Done |
| G6 | S | ~~Committee, meeting and workflow actions check the role, not the committee the caller serves~~ | fixed 2026-10-31: `decidesForMeeting` / `decidesForCommittee` / `canCaptureOutcome` on every meeting, committee, re-open, unlock, approve and reject call (approve/reject: unless Approver role); return needs `canView` | Done. Creating a committee and listing all committees stay role-only (no committee to scope to) |
| G7 | D | Linking a docstash document sets the item to Confirmed whatever its status (it can turn Approved back to Confirmed) and ignores Locked/Cancelled/Rejected | `attachDocument` | Apply the same guards and version rules as `uploadDocument`, or retire the endpoint |
| G8 | D | Unlocking a signed-off record is not committee-scoped. It overwrites `LockedBy/LockedOn` with the unlocker and writes no workflow action. The lock message still says "a reviewer must unlock". | `unlockPostMeeting`, `savePostMeeting` | Scope to `decidesFor`, keep the sign-off audit, record an `Unlock` action, fix the message |
| G9 | S | ~~Sign-off identities are taken from the client~~ | fixed 2026-10-31: the signer and time come from the caller and the server clock; never overwritten | Done |
| G10 | W | Outdated or inaccurate wording, listed below this table | see list | Correct the wording; no behaviour change |
| G11 | W | `sendPreliminaryAgendaMail` and `sendFinalAgendaMail` are never called (compile/lock era) | notification reference "not called" | Remove with the legacy mirror |
| G12 | D | Cancellation and closure e-mails go only to attendee e-mails captured on items, not to participants or standing roles | `collectAttendeeEmails` | Use the same audience as the item invitations |
| G13 | D | An unknown item, meeting, committee or condition id throws `IllegalArgumentException`, which no handler maps (server error), except on drafts. `GET /meetings/{id}` answers success with no body for an unknown id. | service `requireItem` etc.; no `ExceptionMapper` | Map to 404 `{"message"}` |
| G14 | D | An upload without a `file` part fails with a null pointer | `uploadDocument` reads `uploadForm.get("file")` without a check | Answer 400 "Choose a file" |
| G15 | P | Every item in a list response computes its invitation hash, which resolves attendees through AD | `toItemDTO` → `itemSnapshot` → `itemAttendees` → `resolveAddresses` | Compute `invitationNeedsUpdate` only for item detail and meeting detail, or cache AD lookups per request |
| G16 | D | Bulk condition save accepts any status text except Closed/Waived | `saveConditions` | Restrict to Open and the documented in-progress states |
| G17 | S | ~~`DEFAULTS` hard-codes service tokens, secrets and a person's e-mail~~ | fixed 2026-10-31: every `.token` / `.secret` / `.apikey` default is blank and must come from the environment; `af.mail.test.emails` is blank; credentials are never logged | Done in source. **Deployment:** set `AF_TOKEN`, `AF_LISTMANAGEMENTSERVICE_TOKEN`, `AF_DOCSTASHSERVICE_TOKEN`, `AF_RULESSERVICE_TOKEN`, `AF_keycloakConfig_Secret` and `AF_MAIL_TEST_EMAILS` in each server's `env.properties` before deploying; rotate the values that were in the repository |
| G18 | D | Remove has no status precondition (Rejected or already Removed items can be removed again) and no required reason | `removeItem` | Refuse Removed; require a reason for user removals |
| G19 | D | Available dates still offer legacy Compiled/Locked meetings, and booking a Locked one is refused | `getAvailableMeetingDates` excludes only Cancelled/Held/Closed | Exclude Locked |
| G20 | D | Actual start and end times on Mark as held are stored without format or order checks | `markMeetingHeld` | Validate `HH:mm` and end after start, as the UI does |
| G21 | D | ~~The cut-off job runs only Thursday 17:15 with no override~~ | fixed 2026-10-31: hourly by default; `AF_SCHEDULER_AGENDA_CUTOFF_CRON` and `AF_SCHEDULER_AGENDA_REMINDER_CRON` | Done |
| G22 | D | ~~The submitted e-mail addresses reviewer ids unresolved~~ | fixed 2026-10-31: `resolveEmails` | Done |
| G23 | D | Meeting records can be added to a meeting in any state, including Cancelled or Closed. The UI requires a held, unclosed meeting. | `addMeetingRecord` | Require Held and not Closed |

G10 wording to correct:

- The removed e-mail always says the submission deadline was missed, including after a manual removal.
- The confirmed e-mail says "all required participants have been notified", but nobody else is notified at that point.
- The invitation body says the "chairperson and secretary" are on every item's invitation, but all five standing roles are.
- The `AgendaCalendarService` and `AgendaInvitationResultDTO` Javadoc still describe a separate standing-roles event.
- The submitted e-mail asks the reviewer to "return or approve".
- A `moveItem` comment says items are numbered "when compiled".
- The `reopenMeeting` API description says "reviewers only".

- [x] 6. Security gaps G1–G6, G9, G17, G21 and G22 fixed with tests on 2026-10-31, references regenerated, mirrored to `reviewmandates`; recorded in requirements 1.6–1.8, 2.6–2.8, 8.9, 9.4–9.9, 13.2, 13.7, 15.3.
- [ ] 7. Decide G7, G8, G10–G16, G18–G20, G23 with the business owners; implement with tests, regenerate and mirror.
- [ ] 8. Set the credentials in every environment's `env.properties` (see G17) and rotate the values that were in the repository.

## Regression tests for the reviewed gaps

See [gap-regression-coverage.md](gap-regression-coverage.md) for the executable mapping of G1, G2, G3, G5, G6, G9, G17, G21 and G22. The four dedicated regression classes contain 93 normal JUnit cases, including permitted and denied access, no-mutation-on-denial checks, signature integrity, recipient resolution, safe defaults and isolated scheduler environment overrides. All 93 passed; the full API suite passed 421 tests with no failures, errors or skipped tests on JDK 25. These are unit/regression results for the working tree, not deployment or credential-rotation evidence.
