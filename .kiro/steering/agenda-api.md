---
inclusion: fileMatch
fileMatchPattern: "api/src/main/**/*{Agenda,agenda,Graph,MeetingPapers,InvitationStyle}*"
---

# Agenda API

## Source map

Paths are relative to `api/src/main/java/za/co/rmb/approvalforum/`.

| Area | Source |
| --- | --- |
| Endpoints, role guards | `web/resources/ApprovalForumAgendaResource.java` (base path `/approvalForum/agenda`) |
| Business rules | `service/AgendaService.java`, `service/impl/AgendaServiceImpl.java`, `service/impl/AgendaTeamValidation.java`, `service/AgendaAccess.java` |
| E-mail | `service/AgendaMailService.java`, `service/impl/AgendaMailServiceImpl.java` |
| Calendar invitations | `service/AgendaCalendarService.java`, `web/graph/GraphMeetingClient.java`, `service/InvitationStyle.java`, `service/impl/MeetingPapers.java` |
| Persistence | `data/ApprovalForumAgendaRepository.java`, `data/jpa/JPAApprovalForumAgendaRepository.java`, `data/entity/ApprovalForumAg*Entity.java`, `api/src/main/resources/db/migration/V2`–`V17` |
| Request/response types | `model/agenda/` |
| Scheduled jobs | `web/quartz/ProcessAgendaCutOffRequest.java`, `web/quartz/ProcessAgendaReminderRequest.java`, `web/resources/ApprovalForumSchedulerResource.java` |
| Settings | `web/module/ApprovalForumWebResourcesModule.java` (`af_settings`) |

## Working rules

- Read the spec folder `specs/agenda-api/` before changing behaviour. `requirements.md` states the rule. The generated `api-*.md` files hold the exact declarations.
- Business-rule refusals throw `IllegalStateException`. The resource turns them into `409 {"message": …}`, and the UI shows that message, so write it as user-facing text.
- Role guards live in the resource. Item and committee scope rules live in the service (`canView`, `isOwner`, `servesAsSecretary`, `servesAsChair`, `decidesFor`). Keep a new rule in the same place as its siblings.
- An item's invitation goes to that item's participants, its attendee e-mails and the committee's standing roles: chair, alternate chair, meeting secretary, minute taker and administrator. Nothing is recorded as sent unless Graph confirmed it.
- Agenda numbers are `<mandate-level digits>.<sequence>`, assigned in slot order when an item is approved and kept dense when one leaves. There is no compile or lock action. `Compiled` and `Locked` are legacy meeting statuses only.
- After a change: run the agenda tests (`-Dtest='Agenda*Test,ApprovalForumAgendaResourceTest'`), run `python api/tools/document-agenda-api.py`, and mirror the change to `reviewmandates`.
