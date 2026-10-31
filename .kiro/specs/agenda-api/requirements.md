# Agenda API requirements

## Scope

The server side of the Credit Committee Agenda Builder in daf-boot `api/`. It covers:

- committees and the meeting calendar
- booking, documents, quorum and the review/approval workflow
- per-item calendar invitations, the meeting lifecycle and post-meeting records
- notifications, scheduled jobs, reporting and persistence

The Agenda Builder UI (credit-platform spec folder `specs/agenda-builder/`) is the consumer. Its `ui-client-contract.md` must stay consistent with [api-endpoint-contract.md](api-endpoint-contract.md).

These criteria describe the behaviour the code implements today. Where that behaviour is questionable, the criterion says so and points to a gap (G-number) in [tasks.md](tasks.md). Do not "fix" a criterion in code without a decision on its gap. The exact declarations are in the generated `api-*.md` files. [design.md](design.md) gives the state machines.

## 1. Authentication and roles

**User story:** As an operator, I want every agenda call tied to an authenticated DAF identity and its AD-derived roles.

1.1 THE API SHALL validate the DAF bearer token (`tokenValidation`) at the start of every agenda endpoint and use the token principal as the acting user (`SYSTEM` when there is none).
1.2 THE API SHALL derive the roles CreditAnalyst, ReviewAnalyst, Approver, Chairperson and Secretary from the caller's AD groups through the DAF token; the reviewer role name SHALL come from `af.agenda.reviewer.role` (default `ReviewAnalyst`).
1.3 A role SHALL confer oversight in item visibility only while it is listed in `af.agenda.oversight.roles` (default `Secretary;Chairperson;Approver;ReviewAnalyst`).
1.4 WHEN a role or scope guard refuses a call, THE API SHALL answer 403 with `{"message": "<reason>"}` and SHALL NOT change any data.
1.5 Endpoint-level role guards SHALL be exactly those in [api-permission-matrix.md](api-permission-matrix.md).
1.6 Committee administration (update, deactivate), meeting actions (create, update, cancel, reinstate, invitations, held, close, re-open, records) and the item actions re-open and unlock SHALL be allowed only to a Secretary or Chairperson who serves that committee (section 2.2). Approve and reject SHALL be allowed to the Approver role, or to a Secretary or Chairperson who serves the item's committee. Return SHALL need view access to the item.
1.7 `POST /processCutOff` and `POST /processReminders` SHALL answer only the scheduler's user, `af.scheduler.user` (the subject of `af.token` unless `AF_SCHEDULER_USER` names it); everyone else SHALL get 403.
1.8 No credential (any setting ending in `.token`, `.secret`, `.clientSecret`, `.apikey`) SHALL have a built-in value, and no credential value SHALL be written to the log.

## 2. Item visibility and ownership

**User story:** As an analyst, I want my confidential credit paper visible only to the people who work on it.

2.1 THE owner of an item SHALL be its booking analyst (`CreatedBy`) or its named primary analyst, compared case-insensitively.
2.2 A caller SHALL be able to view an item when they are any of the following:
- its owner
- its assigned review analyst holding the reviewer role
- its committee's meeting secretary, minute taker or administrator holding the Secretary role
- its committee's chair or alternate chair holding the Chairperson role
- an Approver, once the item has been Submitted, Reviewed, Approved, Rejected or Returned
2.3 `GET /items` SHALL return only items the caller can view. `GET /items/my` SHALL return the active items the caller booked, is primary analyst on, or reviews.
2.4 Item detail, post-meeting, notifications, workflow history, participants, quorum and conditions reads SHALL require view access.
2.5 Editing the item, uploading or linking documents, asking for a quorum exception and removing the item SHALL require ownership. Removal also needs CreditAnalyst, Secretary or Chairperson.
2.6 `GET /meetings/{agmId}` SHALL return the meeting with every active item, but an item the caller cannot view SHALL be reduced to its slot: id, meeting id and date, status, agenda number, start and end time and duration. Nothing of the paper, people or outcome SHALL be returned for it.
2.7 `GET /committees/{agcId}/lastRoster` SHALL return only the roster of the committee's latest item (chair, quorum roles, overlay and additional attendees, attendee e-mails and participants), never the paper's details.
2.8 The operational report SHALL list late-item details only for items the caller can view; the counts cover every item.

## 3. Committees and calendar administration

**User story:** As a committee secretary or chair, I want to maintain committees and their meeting calendar.

3.1 Creating, updating and deactivating committees and listing all committees SHALL require Secretary or Chairperson.
3.2 Creating a committee SHALL require a name, a mandate level, a valid weekday meeting day and a quorum size above zero. It SHALL default the cut-off to Thursday 17:00 and the required document types to `Credit Paper`.
3.3 A cut-off day SHALL be a weekday name and a cut-off time SHALL be `HH:mm`; an invalid value SHALL be refused with 409.
3.4 Updating a committee SHALL replace these fields with the values sent:
- quorum size and meeting day
- chair and alternate chair, with their ids
- meeting secretary, minute taker and administrator, with their ids
- approver composition, page guidance and list name
- effective window, default meeting mode and default duration

Cut-off day, cut-off time, required document types and quorum roles SHALL change only when sent.
3.5 Deactivating a committee SHALL mark it inactive and set its effective end to now when none is set.
3.6 Available meeting dates for a committee SHALL be the union of two sets, sorted:
- its scheduled meetings within today plus three weeks that are not Cancelled, Held or Closed
- its regular meeting weekday for the next three weeks, skipping Cancelled dates and dates outside the committee's effective window
3.7 Committee members SHALL come from the List Management list named on the committee, or null when none is linked.

## 4. Meetings

**User story:** As a secretary, I want to schedule and maintain meetings without double-booking a committee.

4.1 A meeting SHALL belong to one committee and one calendar date. A second meeting for the same committee and date SHALL be refused.
4.2 A meeting date SHALL NOT be in the past (today is allowed) and SHALL fall inside the committee's effective window.
4.3 A new meeting SHALL be created as follows:
- status Draft
- mode from the request, else the committee default
- start 08:00 unless given
- cut-off from the committee rule unless given
- end time computed per 4.4

A booking or move onto a date with no meeting SHALL create one the same way.
4.4 THE scheduled end time SHALL be the start time plus the committee's default duration. When the committee has none it SHALL be 240 minutes. It SHALL be capped at 23:59, and SHALL NOT be entered by the caller.
4.5 A venue SHALL be required when the mode contains "physical", "hybrid" or "person".
4.6 Updating a meeting SHALL be refused once it is Held, Closed or Cancelled. A date change SHALL recompute the cut-off. A cut-off sent explicitly SHALL override it.
4.7 WHEN a meeting edit leaves sent invitations out of date, THE API SHALL attempt the invitation update itself. A failed update SHALL leave the edit saved and the update pending.
4.8 THE committee cut-off SHALL be the last configured cut-off weekday strictly before the meeting date at the configured time. The default is Thursday 17:00.

## 5. Booking

**User story:** As a credit analyst, I want to reserve a discussion slot on a committee meeting.

5.1 Booking SHALL require a known committee and a meeting date that is today or later and not a Saturday or Sunday.
5.2 Booking SHALL validate the team (section 6) and derive the slot from a valid `HH:mm` start and a positive duration. The end SHALL be start plus duration, and a slot ending after 23:59 SHALL be refused.
5.3 Booking, moving and editing a slot SHALL take a pessimistic write lock on the committee row before checking availability. Competing bookings for the same committee are then serialised.
5.4 A slot SHALL NOT overlap another active, non-Removed item on the same meeting. The other item's interval is its start to the later of its end time and start plus duration. Adjacent slots are allowed.
5.5 Booking SHALL be refused on a Held, Closed, Cancelled or legacy Locked meeting.
5.6 WHEN the cut-off has passed, THE booking SHALL require a late-submission reason. The item SHALL be marked late with that reason. Lateness SHALL be decided by the server.
5.7 A booked item SHALL start in status Captured. Its participants SHALL be stored when sent.
5.8 WHEN a quorum override reason is sent and quorum is not met, THE booking SHALL handle it by who is booking:
- the committee's secretariat member or chair (holding the matching role) approves the exception at once
- anyone else's reason becomes a pending quorum exception request
5.9 WHEN booked from a draft owned by the caller, THE draft SHALL be deactivated.
5.10 THE committee's meeting secretary and the analysts SHALL be mailed that the item was captured.

## 6. Team independence

**User story:** As a reviewer, I want separation of duties enforced by the server.

6.1 A review analyst SHALL be named, and every named primary analyst, review analyst, credit sponsor and BU head SHALL carry a directory account id.
6.2 Account comparison SHALL ignore case, an `RMB\` prefix and an `@rmb.co.za` suffix.
6.3 The primary analyst, review analyst, credit sponsor and BU head SHALL be different accounts. The one exception: the credit sponsor and BU head may be the same account.
6.4 The review analyst SHALL NOT be the booking analyst.
6.5 Returning, completing review, approving, rejecting and validating a document SHALL be refused to the item's booking analyst and primary analyst.

## 7. Quorum and quorum exceptions

**User story:** As a secretary, I want quorum evaluated by role and exceptions decided by the committee.

7.1 Quorum SHALL count distinct quorum-eligible participants (by person id, else name) whose role is one of the committee's quorum roles, or any role when none is configured. Quorum SHALL be met when that count reaches the committee's quorum size and the size is above zero.
7.2 Configured quorum roles with nobody assigned SHALL be reported as missing.
7.3 An item without participant rows SHALL be evaluated from its legacy roster columns (Chair, IndependentMember, PortfolioCce1/2, BusinessRep1, SeniorBusinessRep, LecRep, EqMemberFrmExec, Scribe). Scribe counts for neither quorum nor voting.
7.4 The owner MAY ask for an exception when all of these hold:
- the item is active and Captured, Confirmed or Returned
- the meeting is Draft or Compiled
- no exception is Requested or Approved
- quorum is not met

The reason SHALL be non-blank and at most 1,000 characters.
7.5 A Requested exception SHALL be decided only by the committee's secretariat member or chair holding the matching role, while the item is Captured, Confirmed, Returned, Submitted or Reviewed. A decline SHALL need a note; an approval note is optional; notes SHALL be at most 1,000 characters. A missing `approved` flag SHALL be answered with 400.
7.6 An approval SHALL make the requested reason the item's quorum override. A decline SHALL clear the override. A declined exception MAY be requested again.
7.7 The deciders (secretary, chair, alternate chair) SHALL be mailed a request. The analysts SHALL be mailed the decision.

## 8. Documents

**User story:** As an analyst and a reviewer, I want versioned papers with recorded decisions.

8.1 Upload SHALL take a multipart `file` part and an optional `docType` part (default `Credit Paper`). It SHALL be refused on Locked, Held or Closed meetings and on Rejected or Removed items.
8.2 A new upload of a type SHALL be the next version of that type. Earlier versions SHALL become Superseded. The new version SHALL be Pending and marked mandatory when its type is a committee required type.
8.3 WHEN an upload lands on a Submitted or Reviewed item, a Reviewed item SHALL return to Submitted. The change SHALL be recorded, and the reviewer and secretary mailed.
8.4 WHEN a Captured item has a current (not Superseded or Rejected) version of every required type, it SHALL become Confirmed and the analysts SHALL be mailed with the paper attached.
8.5 Linking a docstash document (`/items/{agiId}/document`) SHALL set the item to Confirmed and mail the analysts. It checks only that the meeting is not Held or Closed (G7).
8.6 A document decision SHALL be Pending, Accepted or Rejected. A rejection SHALL need a comment.
8.7 Accepted or Rejected versions SHALL NOT be decided again. Repeating the same decision SHALL return the recorded decision unchanged. Superseded versions SHALL NOT be decided.
8.8 Documents SHALL NOT be decided while the item is Reviewed, Approved, Rejected or Removed.
8.9 Download SHALL require view access to the document's item (2.2), then stream the stored bytes with the stored filename, or 404 when absent.

## 9. Review and approval workflow

**User story:** As an analyst, reviewer and approver, I want a controlled path from booking to the agenda.

9.1 Submit SHALL be allowed from Captured, Confirmed or Returned, by CreditAnalyst, Secretary or Chairperson. It SHALL be refused on Locked, Held or Cancelled meetings.
9.2 Submit SHALL require all of these:
- a valid team
- counterparty, purpose, business unit, primary analyst and a positive duration
- a current version of every required document type
- a late reason when late
- quorum met, or an approved exception
9.3 A submit with an override reason SHALL be accepted only from the committee's secretariat member or chair, who thereby approves the exception. At the endpoint this also needs the Secretary or Chairperson role.
9.4 Submit SHALL record the submitter and time, set Submitted, and mail the review analyst, the meeting secretary and the analysts with the paper. The review analyst and secretary ids SHALL be resolved to mail addresses through the directory, like every other notification.
9.5 Return SHALL be allowed only from Submitted, with a reason, by the reviewer role, Approver, Chairperson or Secretary with view access to the item, before the meeting is held. It SHALL set Returned and mail the analysts.
9.6 Complete review SHALL be allowed only from Submitted, by the reviewer role, Secretary or Chairperson with view access, on a meeting not Held or Cancelled. It SHALL require:
- a valid team
- an accepted version of every required type
- no Pending document
- quorum met or an override

It SHALL set Reviewed and mail the chair, the meeting secretary and the analysts.
9.7 Approve SHALL be allowed only from Reviewed, by an Approver, or by a Chairperson or Secretary who serves the item's committee, on a meeting not Held or Cancelled. It SHALL re-check the team, the accepted required documents and quorum or override. It SHALL then:
- set Approved
- stamp the late approval when late and not yet stamped
- renumber the agenda (section 10)
- mail the analysts with the paper
9.8 Reject SHALL be allowed to the same people as approve (9.7), only from Reviewed, with a reason. It SHALL cancel the item's invitation first and set Rejected.
9.9 Re-open SHALL be allowed from Submitted, Reviewed, Approved or Rejected, with a reason, by a Secretary or Chairperson who serves the item's committee, on a meeting not Held or Cancelled. It SHALL:
- cancel the item's invitation
- set Confirmed when every required type has a current version, else Captured
- clear the number and renumber when the item was Approved
- mark the action an override when the meeting is legacy Locked
9.10 Remove SHALL be refused once the meeting is Held. Otherwise it SHALL:
- cancel the item's invitation
- set Removed
- renumber when the item was Approved
- mail the analysts

It takes an optional reason and has no status precondition (G18).
9.11 Edit SHALL be refused on Submitted, Reviewed, Approved, Rejected or Removed items and on Locked, Held or Closed meetings. It SHALL re-validate the team, slot and lateness.
9.12 Reviewer reassignment SHALL be allowed only on an active Submitted item on a Draft or Compiled meeting, by the committee's secretariat member or chair. It SHALL require:
- a directory reviewer id and name, each at most 255 characters
- a reason of at most 1,000 characters
- the expected current reviewer, to detect concurrent change

Picking the same reviewer SHALL return the item unchanged when the expected reviewer is stale. Otherwise it SHALL be refused. The new team SHALL pass section 6.
9.13 Moving an item and changing its roster SHALL be allowed to the owner or the assigned review analyst while all of these hold:
- the item is Captured, Confirmed, Submitted, Returned or Reviewed
- the meeting is Draft or Compiled
- the item is active

A move SHALL keep the duration, follow 5.1–5.6, refuse an unchanged slot and clear the number when the meeting changes. A roster change SHALL never drop an already-listed attendee e-mail. Both SHALL be recorded and, once submitted, mailed to the reviewer and secretary.

## 10. Agenda numbering

10.1 Approved items of a meeting SHALL be numbered `<digits of the committee mandate level, or 1>.<sequence>`. The sequence follows start time, then capture time, then id; unslotted items go last.
10.2 Numbers SHALL be assigned when an item is approved and recomputed densely when an approved item is re-opened or removed. Items not approved SHALL stay unnumbered.

## 11. Calendar invitations

**User story:** As a secretary, I want one invitation per approved item, sent once and kept up to date.

11.1 Send and Send update SHALL require Secretary or Chairperson and SHALL be refused on Held, Closed or Cancelled meetings. Send update SHALL be refused when nothing was ever sent.
11.2 A repeated call when every invitable approved item already holds a current invitation SHALL return the meeting without contacting anyone.
11.3 Each approved item SHALL get its own event:
- subject `<committee name> <level> - [<number> ]<counterparty>`
- its slot; without booked times, the meeting start and the item's minutes
- the meeting venue and mode, as a Teams meeting when the mode is blank or contains "teams", "hybrid" or "virtual"
- its current credit paper attached
- a link to `/agenda/item/{agiId}`

The body SHALL describe only that item.
11.4 THE attendees of an item's event SHALL be all of these, de-duplicated and lower-cased:
- its participants' e-mails or ids
- its attendee e-mails
- the committee's standing roles: chair, alternate chair, meeting secretary, minute taker and administrator

User ids SHALL be resolved to AD mail addresses, falling back to the id.
11.5 An item SHALL be re-sent only when a hash of its event content, attendees and current credit-paper versions differs from the hash recorded at its last send.
11.6 An item SHALL be recorded as sent (event id, Teams URL, hash, time, sender) only when Graph returns an event id. Each item's outcome SHALL be returned as Sent, Updated, Unchanged, Skipped (no attendees) or Failed.
11.7 WHEN no approved item has attendees, THE call SHALL be refused. WHEN Graph is not configured, or every attempted item failed, THE call SHALL be refused with the generic invitation failure and nothing recorded.
11.8 An item leaving the agenda (reject, re-open, remove) SHALL have its event cancelled first. A cancellation failure SHALL refuse the action and leave the item unchanged.
11.9 Graph events SHALL be created with a stable transaction id derived from the UI base, client id and event key. An earlier partial create SHALL be recovered rather than duplicated.

## 12. Meeting lifecycle and records

**User story:** As a secretary, I want the sitting recorded from held to closed, with cancellation and correction.

12.1 Cancel SHALL require Secretary or Chairperson and a reason, and SHALL be refused once Held, Closed or Cancelled. Every item event and any legacy meeting event SHALL be cancelled first. Any failure SHALL keep the meeting active and refuse with 409.
12.2 A cancelled meeting SHALL record who, when and why. The attendee e-mails captured on its items SHALL be mailed (G12).
12.3 Reinstate SHALL be allowed only on a Cancelled meeting, by Secretary or Chairperson, with a reason of at most 1,000 characters. It SHALL:
- restore Draft
- clear lock, event, Teams link and invitation state on the meeting and its items
- keep the cancellation record, items, papers, approvals and numbers
- send nothing
12.4 Mark as held SHALL be refused on Cancelled, Held or Closed meetings, and before the sitting has ended. The sitting ends at the meeting date plus its end time, else its start time, else the end of that day.
12.5 Mark as held SHALL record the actual start and end times as sent (G20) and the quorum flag as sent. When none is sent, quorum SHALL be derived: true when every approved item met quorum or carries an override, false otherwise, null without approved items.
12.6 Close SHALL be allowed only on a Held meeting. Every approved item SHALL need a post-meeting record with a chair sign-off and a decision or decision type. It SHALL record who and when and mail the captured attendee e-mails.
12.7 Re-open SHALL be allowed only on a Closed meeting, by Chairperson or Secretary, with a reason. It SHALL restore Held and record an override.
12.8 A meeting record SHALL need a type, a name and a location (URL or repository reference). Adding one needs a Secretary or Chairperson who serves the committee (1.6). It does not check the meeting status (G23).

## 13. Post-meeting capture, attendance and conditions

13.1 Post-meeting capture SHALL be writable only by the committee's secretariat member or chair, only when the meeting is Held and its date has passed.
13.2 A sign-off SHALL be recorded as the caller and the server's time the first time the request carries a sign-off time for that role; the names and times sent SHALL be ignored, and a recorded sign-off SHALL never be overwritten or withdrawn by a later save. A record SHALL lock when the chair sign-off is recorded. A locked record SHALL refuse edits until unlocked. Unlock needs a Chairperson or Secretary who serves the committee; it still overwrites the lock audit and writes no action (G8).
13.3 The decision date SHALL default to now when a decision type is first captured without one.
13.4 Before the meeting is Held, saving participants SHALL replace the roster (owner, or the committee's secretariat or chair). Rows without a name or role SHALL be skipped. This SHALL be refused on Locked or Cancelled meetings.
13.5 Once Held, saving participants SHALL change only the actual attendance of existing rows. Once Closed, it SHALL be refused.
13.6 Conditions SHALL be saved only by the committee's secretariat or chair, on a Held meeting that is not Closed. Saving SHALL behave as follows:
- new rows are Open, with type `Condition` by default
- Closed and Waived rows are left unchanged
- rows not sent are deactivated
13.7 Closing a condition SHALL be allowed only to a Secretary or Chairperson who serves the item's committee, SHALL need status Closed or Waived and closure comments, and SHALL be refused once the meeting is Closed.

## 14. Notifications

14.1 Every agenda e-mail SHALL go through the DAF mail service from `approvalForum@rmb.co.za` with the subject prefix `Agenda Builder - `. Each SHALL be logged in `AGN_AgendaNotification` with status Sent, Failed, Disabled or NoRecipient.
14.2 WHEN `af.mail.enabled` is not true, THE mail SHALL be logged Disabled and not sent.
14.3 Outside `af.mail_environment=PROD`, recipients SHALL be replaced by `af.mail.test.emails` and the subject prefixed with `[<environment>]`.
14.4 Analyst recipients SHALL be the booking, primary and review analysts, resolved to AD mail. The resolution SHALL fall back to the id.
14.5 Each e-mail's trigger, recipients, subject, banner and attachment SHALL be as listed in [api-notification-reference.md](api-notification-reference.md). A mail failure SHALL NOT fail the business action.
14.6 Returned and rejected notifications SHALL show the next step. They SHALL show the matching workflow reason and actor only when a decision of that type was recorded within two minutes before the notification.

## 15. Scheduled jobs

15.1 The cut-off job SHALL remove, as `SYSTEM`, every Captured item on an active Draft (or legacy Preliminary) meeting whose cut-off has passed, with the reason "Credit paper not uploaded by the cut-off".
15.2 The reminder job SHALL mail the analysts of Captured items on such meetings whose cut-off date is one or two days away.
15.3 The jobs SHALL run on `af.scheduler.agenda.cutoff.cron.entry` (default hourly, so each committee's own cut-off day and time is honoured; `AF_SCHEDULER_AGENDA_CUTOFF_CRON`) and `af.scheduler.agenda.reminder.cron.entry` (default weekdays 09:00; `AF_SCHEDULER_AGENDA_REMINDER_CRON`). They call this API with `af.token`, which SHALL come from `AF_TOKEN` (1.8).

## 16. Drafts

16.1 Drafts SHALL be private to their owner; another user's or an inactive draft SHALL be answered 404.
16.2 A draft SHALL need a non-blank payload of at most 2,000,000 characters. Its counterparty name SHALL be trimmed to 500 characters. Its step SHALL never be below 0.
16.3 Deleting a draft SHALL deactivate it.

## 17. Reporting

17.1 The summary for a date range (default the last and next 90 days) SHALL report:
- meetings by status
- items by status, committee, business unit and purpose
- late items and outstanding mandatory papers
- planned and actual minutes, and decisions
- invited, planned-attending and attended counts
- quorum met and not met for approved items
- open and overdue conditions

## 18. Audit and persistence

18.1 Every workflow, schedule, exception, invitation, meeting and roster action SHALL write a workflow action with:
- the item and/or meeting
- the action, from and to statuses, and comment
- override flag and reason
- actor and time
18.2 Agenda tables SHALL be created and changed only by Flyway migrations V2 onwards ([api-schema-reference.md](api-schema-reference.md)). Audited entities SHALL keep Envers history.
18.3 Rows SHALL be deactivated (`Active = 0`) rather than deleted. Queries SHALL read active rows.

## 19. Errors

19.1 Business-rule refusals SHALL answer 409 with `{"message": …}` written for the user.
19.2 Unknown drafts SHALL answer 404 with a message. A missing download SHALL answer 404.
19.3 An unknown item, meeting, committee or condition id outside the draft endpoints SHALL surface as an unhandled server error (G13).
