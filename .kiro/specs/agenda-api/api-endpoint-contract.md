# Agenda API endpoint contract

This is a generated backend contract reference for the Agenda Builder API in daf-boot. Literal declarations below are part of the specification; requirements.md and design.md explain them. It is not evidence of a live service, environment or database.

Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. No API, directory or database is accessed.

Source: `api/src/main/java/za/co/rmb/approvalforum/web/resources/ApprovalForumAgendaResource.java`. SHA-256: `2b8f8198844673b7cca7649ae5947d1fb14b2fd07ddee9f8cc90993200f9577e`.

66 endpoints. Every endpoint first calls `tokenValidation(context)`: a request without a valid DAF bearer token is refused before the method body. Exceptions the method does not catch (for example `IllegalArgumentException` for an unknown id) are not mapped by an agenda-specific handler.

| # | Verb | Path | Method | Parameters | Guards | Responses |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | GET | `/approvalForum/agenda/users` | `getUsers` | — | authenticated | 200 OK |
| 2 | GET | `/approvalForum/agenda/lists` | `getAvailableLists` | — | authenticated | 200 OK |
| 3 | GET | `/approvalForum/agenda/counterparties` | `getAgendaCounterparties` | query `tenantId`: int (default 1) | authenticated | 200 OK, 500 with the external error as reason phrase |
| 4 | GET | `/approvalForum/agenda/casdm/application` | `getCasdmApplication` | query `inkombaId`: String | authenticated | 200 OK |
| 5 | GET | `/approvalForum/agenda/committees` | `getCommittees` | — | authenticated | 200 OK |
| 6 | PUT | `/approvalForum/agenda/committees` | `updateCommittee` | body `committee`: AgendaCommitteeDTO | role: Secretary or Chairperson; serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 7 | GET | `/approvalForum/agenda/committees/all` | `getAllCommittees` | — | role: Secretary or Chairperson | 200 OK, 403 Forbidden {message} |
| 8 | POST | `/approvalForum/agenda/committees` | `createCommittee` | body `committee`: AgendaCommitteeDTO | role: Secretary or Chairperson | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 9 | POST | `/approvalForum/agenda/committees/{agcId}/deactivate` | `deactivateCommittee` | path `agcId`: int | role: Secretary or Chairperson; serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message} |
| 10 | POST | `/approvalForum/agenda/meetings` | `createMeeting` | body `meeting`: AgendaMeetingDTO | role: Secretary or Chairperson; serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 11 | PUT | `/approvalForum/agenda/meetings/{agmId}` | `updateMeeting` | path `agmId`: int; body `meeting`: AgendaMeetingDTO | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 12 | POST | `/approvalForum/agenda/meetings/{agmId}/cancel` | `cancelMeeting` | path `agmId`: int; query `reason`: String | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 13 | POST | `/approvalForum/agenda/meetings/{agmId}/reinstate` | `reinstateMeeting` | path `agmId`: int; query `reason`: String | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 14 | GET | `/approvalForum/agenda/meetings` | `getMeetingsBetween` | query `from`: String; query `to`: String | authenticated | 200 OK |
| 15 | GET | `/approvalForum/agenda/committees/{agcId}/dates` | `getAvailableDates` | path `agcId`: int | authenticated | 200 OK |
| 16 | GET | `/approvalForum/agenda/committees/{agcId}/lastRoster` | `getLastRoster` | path `agcId`: int | authenticated | 200 OK |
| 17 | GET | `/approvalForum/agenda/committees/{agcId}/members` | `getCommitteeMembers` | path `agcId`: int | authenticated | 200 OK |
| 18 | GET | `/approvalForum/agenda/committees/{agcId}/meetings` | `getMeetings` | path `agcId`: int | authenticated | 200 OK |
| 19 | GET | `/approvalForum/agenda/meetings/{agmId}` | `getMeeting` | path `agmId`: int | authenticated | 200 OK |
| 20 | POST | `/approvalForum/agenda/meetings/{agmId}/invitation` | `sendInvitation` | path `agmId`: int | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 21 | POST | `/approvalForum/agenda/meetings/{agmId}/invitation/update` | `sendInvitationUpdate` | path `agmId`: int | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 22 | POST | `/approvalForum/agenda/meetings/{agmId}/held` | `markMeetingHeld` | path `agmId`: int; query `actualStartTime`: String; query `actualEndTime`: String; query `quorumAchieved`: Boolean | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 23 | POST | `/approvalForum/agenda/meetings/{agmId}/close` | `closeMeeting` | path `agmId`: int | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 24 | POST | `/approvalForum/agenda/meetings/{agmId}/reopen` | `reopenMeeting` | path `agmId`: int; query `reason`: String | role: Chairperson or Secretary; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 25 | GET | `/approvalForum/agenda/meetings/{agmId}/records` | `getMeetingRecords` | path `agmId`: int | authenticated | 200 OK |
| 26 | POST | `/approvalForum/agenda/meetings/{agmId}/records` | `addMeetingRecord` | path `agmId`: int; body `record`: AgendaMeetingRecordDTO | role: Secretary or Chairperson; serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 27 | POST | `/approvalForum/agenda/items` | `bookItem` | body `request`: BookAgendaItemRequest | authenticated | 200 OK, 409 Conflict {message} |
| 28 | PUT | `/approvalForum/agenda/items` | `updateItem` | body `item`: AgendaItemDTO | owner: booking analyst or named primary analyst | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 29 | GET | `/approvalForum/agenda/items` | `getAllItems` | — | authenticated | 200 OK |
| 30 | GET | `/approvalForum/agenda/items/my` | `getMyItems` | — | authenticated | 200 OK |
| 31 | GET | `/approvalForum/agenda/items/{agiId}` | `getItem` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 32 | POST | `/approvalForum/agenda/items/{agiId}/meeting` | `moveItem` | path `agiId`: int; body `request`: MoveAgendaItemRequest | owner or assigned review analyst, item before approval, meeting Draft or Compiled | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 33 | PUT | `/approvalForum/agenda/items/{agiId}/roster` | `updateRoster` | path `agiId`: int; body `roster`: AgendaItemDTO | owner or assigned review analyst, item before approval, meeting Draft or Compiled | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 34 | GET | `/approvalForum/agenda/drafts` | `getBookingDrafts` | — | authenticated | 200 OK |
| 35 | GET | `/approvalForum/agenda/drafts/{agbdId}` | `getBookingDraft` | path `agbdId`: int | authenticated | 200 OK, 404 Not Found |
| 36 | POST | `/approvalForum/agenda/drafts` | `createBookingDraft` | body `draft`: AgendaBookingDraftDTO | authenticated | 200 OK, 409 Conflict {message} |
| 37 | PUT | `/approvalForum/agenda/drafts/{agbdId}` | `updateBookingDraft` | path `agbdId`: int; body `draft`: AgendaBookingDraftDTO | authenticated | 200 OK, 409 Conflict {message}, 404 Not Found |
| 38 | DELETE | `/approvalForum/agenda/drafts/{agbdId}` | `deleteBookingDraft` | path `agbdId`: int | authenticated | 204 No Content, 404 Not Found |
| 39 | POST | `/approvalForum/agenda/items/{agiId}/document` | `attachDocument` | path `agiId`: int; query `docStashId`: String; query `fileName`: String | owner: booking analyst or named primary analyst | 200 OK, 403 Forbidden {message} |
| 40 | POST | `/approvalForum/agenda/items/{agiId}/upload` | `uploadDocument` | path `agiId`: int; body `form`: MultipartFormDataInput | owner: booking analyst or named primary analyst | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 41 | GET | `/approvalForum/agenda/documents/{agidId}/download` | `downloadDocument` | path `agidId`: int | can view the document's item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message}, 404 Not Found |
| 42 | POST | `/approvalForum/agenda/items/{agiId}/remove` | `removeItem` | path `agiId`: int; query `reason`: String | role: CreditAnalyst or Secretary or Chairperson; owner: booking analyst or named primary analyst | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 43 | GET | `/approvalForum/agenda/items/{agiId}/postMeeting` | `getPostMeeting` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 44 | GET | `/approvalForum/agenda/items/{agiId}/notifications` | `getNotifications` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 45 | POST | `/approvalForum/agenda/items/{agiId}/postMeeting` | `savePostMeeting` | path `agiId`: int; body `postMeeting`: AgendaPostMeetingDTO | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message} |
| 46 | POST | `/approvalForum/agenda/items/{agiId}/postMeeting/unlock` | `unlockPostMeeting` | path `agiId`: int | role: Chairperson or Secretary; serves the item's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message} |
| 47 | POST | `/approvalForum/agenda/items/{agiId}/submit` | `submitItem` | path `agiId`: int; query `quorumOverrideReason`: String | role: CreditAnalyst or Secretary or Chairperson; a non-blank override reason also needs Secretary or Chairperson | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 48 | POST | `/approvalForum/agenda/items/{agiId}/return` | `returnItem` | path `agiId`: int; query `reason`: String | role: ReviewAnalyst or Approver or Chairperson or Secretary; can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 49 | POST | `/approvalForum/agenda/items/{agiId}/quorum-exception` | `requestQuorumException` | path `agiId`: int; body `request`: QuorumExceptionRequest | owner: booking analyst or named primary analyst | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 50 | POST | `/approvalForum/agenda/items/{agiId}/quorum-exception/decision` | `decideQuorumException` | path `agiId`: int; body `request`: QuorumExceptionRequest | role: Secretary or Chairperson | 200 OK, 403 Forbidden {message}, 400 Bad Request {message}, 409 Conflict {message} |
| 51 | POST | `/approvalForum/agenda/items/{agiId}/reviewer` | `reassignReviewer` | path `agiId`: int; body `request`: ReassignAgendaReviewerRequest | role: Secretary or Chairperson; serves this committee as secretariat or chair; item Submitted; meeting Draft or Compiled | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 52 | POST | `/approvalForum/agenda/items/{agiId}/review` | `completeReview` | path `agiId`: int; query `comments`: String | role: ReviewAnalyst or Secretary or Chairperson; can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 53 | POST | `/approvalForum/agenda/items/{agiId}/approve` | `approveItem` | path `agiId`: int; query `comments`: String | role: Approver or Chairperson or Secretary; without the Approver role: serves the item's committee as secretariat (Secretary role) or chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 54 | POST | `/approvalForum/agenda/items/{agiId}/reject` | `rejectItem` | path `agiId`: int; query `reason`: String | role: Approver or Chairperson or Secretary; without the Approver role: serves the item's committee as secretariat (Secretary role) or chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 55 | POST | `/approvalForum/agenda/items/{agiId}/reopen` | `reopenItem` | path `agiId`: int; query `reason`: String | role: Secretary or Chairperson; serves the item's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 56 | GET | `/approvalForum/agenda/items/{agiId}/actions` | `getWorkflowActions` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 57 | POST | `/approvalForum/agenda/documents/{agidId}/validate` | `validateDocument` | path `agidId`: int; query `status`: String; query `comment`: String | role: ReviewAnalyst or Secretary or Chairperson | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 58 | GET | `/approvalForum/agenda/items/{agiId}/participants` | `getParticipants` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 59 | PUT | `/approvalForum/agenda/items/{agiId}/participants` | `saveParticipants` | path `agiId`: int; body `participants`: List<AgendaParticipantDTO> | owner before Held; committee secretary or chair at any time | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 60 | GET | `/approvalForum/agenda/items/{agiId}/quorum` | `evaluateQuorum` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 61 | GET | `/approvalForum/agenda/items/{agiId}/conditions` | `getConditions` | path `agiId`: int | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) | 200 OK, 403 Forbidden {message} |
| 62 | PUT | `/approvalForum/agenda/items/{agiId}/conditions` | `saveConditions` | path `agiId`: int; body `conditions`: List<AgendaConditionDTO> | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 63 | POST | `/approvalForum/agenda/conditions/{agcaId}/close` | `closeCondition` | path `agcaId`: int; query `status`: String; query `comments`: String | serves the condition's committee as secretariat (Secretary role) or chair (Chairperson role) | 200 OK, 403 Forbidden {message}, 409 Conflict {message} |
| 64 | GET | `/approvalForum/agenda/reports/summary` | `getReportSummary` | query `from`: String; query `to`: String | authenticated | 200 OK |
| 65 | POST | `/approvalForum/agenda/processCutOff` | `processCutOff` | — | the scheduler's own user only (`af.scheduler.user`, the subject of `af.token`) | 200 OK, 403 Forbidden {message} |
| 66 | POST | `/approvalForum/agenda/processReminders` | `processReminders` | — | the scheduler's own user only (`af.scheduler.user`, the subject of `af.token`) | 200 OK, 403 Forbidden {message} |

## 1. GET /approvalForum/agenda/users

DAF user directory for people pickers (same source as capture)

- Method: `getUsers`; transactional: no
- Produces: application/json
- Calls: `approvalForumListManagementService.getAllUsers`

```java
    @GET
    @Path("/users")
    @Produces("application/json")
    @ApiOperation(value = "DAF user directory for people pickers (same source as capture)",
            authorizations = @Authorization(value = "Bearer"))
    public Response getUsers() {
        logger.info("getUsers()");
        tokenValidation(context);
        return Response.ok(approvalForumListManagementService.getAllUsers()).build();
    }
```

## 2. GET /approvalForum/agenda/lists

Names of the List Management lists mapped into DAF - the options a committee can be linked to for its member list

- Method: `getAvailableLists`; transactional: no
- Produces: application/json
- Calls: `approvalForumListManagementService.getAllListNames`

```java
    @GET
    @Path("/lists")
    @Produces("application/json")
    @ApiOperation(value = "Names of the List Management lists mapped into DAF - the options a "
            + "committee can be linked to for its member list",
            authorizations = @Authorization(value = "Bearer"))
    public Response getAvailableLists() {
        logger.info("getAvailableLists()");
        tokenValidation(context);
        return Response.ok(approvalForumListManagementService.getAllListNames()).build();
    }
```

## 3. GET /approvalForum/agenda/counterparties

Counterparty list for agenda booking - same creditdata feed as capture but defaulting to all active application statuses; narrow via af.agenda.application.statuses

- Method: `getAgendaCounterparties`; transactional: no
- Produces: application/json
- Calls: `approvalForumExternalService.getCounterparties`

```java
    @GET
    @Path("/counterparties")
    @Produces("application/json")
    @ApiOperation(value = "Counterparty list for agenda booking - same creditdata feed as capture but "
            + "defaulting to all active application statuses; narrow via af.agenda.application.statuses",
            authorizations = @Authorization(value = "Bearer"))
    public Response getAgendaCounterparties(@DefaultValue("1") @QueryParam("tenantId") int tenantId) {
        logger.info("getAgendaCounterparties()");
        tokenValidation(context);
        String statuses = afSettings.get("af.agenda.application.statuses");
        if (statuses == null || statuses.trim().isEmpty()) {
            statuses = AGENDA_DEFAULT_STATUSES;
        }
        try {
            return Response.ok(new CounterpartyResponseDTO().withCounterpartyDetail(
                    approvalForumExternalService.getCounterparties(true, tenantId,
                            Arrays.asList(statuses.split(";"))))).build();
        } catch (ExternalResourceException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()).build();
        }
    }
```

## 4. GET /approvalForum/agenda/casdm/application

Check CASDM directly for the latest credit application of a counterparty

- Method: `getCasdmApplication`; transactional: no
- Produces: application/json
- Calls: `casdmRestClient.getLatestApplicationByInkomba`

```java
    @GET
    @Path("/casdm/application")
    @Produces("application/json")
    @ApiOperation(value = "Check CASDM directly for the latest credit application of a counterparty",
            authorizations = @Authorization(value = "Bearer"))
    public Response getCasdmApplication(@QueryParam("inkombaId") String inkombaId) {
        logger.info("getCasdmApplication({})", inkombaId);
        tokenValidation(context);
        return Response.ok(casdmRestClient.getLatestApplicationByInkomba(inkombaId)).build();
    }
```

## 5. GET /approvalForum/agenda/committees

List agenda committees and levels

- Method: `getCommittees`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getCommittees`

```java
    @GET
    @Path("/committees")
    @Produces("application/json")
    @ApiOperation(value = "List agenda committees and levels", authorizations = @Authorization(value = "Bearer"))
    public Response getCommittees() {
        logger.info("getCommittees()");
        tokenValidation(context);
        return Response.ok(agendaService.getCommittees()).build();
    }
```

## 6. PUT /approvalForum/agenda/committees

Update committee information (secretary/chair only)

- Method: `updateCommittee`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.updateCommittee`

```java
    @PUT
    @Path("/committees")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Update committee information (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response updateCommittee(AgendaCommitteeDTO committee) {
        logger.info("updateCommittee({})", committee.getAgcId());
        tokenValidation(context);
        Response denied = forbiddenUnless("update committee configuration", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForCommittee("update committee configuration", committee.getAgcId());
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.updateCommittee(committee, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 7. GET /approvalForum/agenda/committees/all

All committees including inactive ones (administration)

- Method: `getAllCommittees`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getAllCommittees`

```java
    // ---- BRS alignment: committee & calendar administration (BR-001, BR-002) - secretary or chair ----

    @GET
    @Path("/committees/all")
    @Produces("application/json")
    @ApiOperation(value = "All committees including inactive ones (administration)", authorizations = @Authorization(value = "Bearer"))
    public Response getAllCommittees() {
        logger.info("getAllCommittees()");
        tokenValidation(context);
        Response denied = forbiddenUnless("administer committees", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        return Response.ok(agendaService.getAllCommittees()).build();
    }
```

## 8. POST /approvalForum/agenda/committees

Create a committee / mandate level (secretary/chair only)

- Method: `createCommittee`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.createCommittee`

```java
    @POST
    @Path("/committees")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Create a committee / mandate level (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response createCommittee(AgendaCommitteeDTO committee) {
        logger.info("createCommittee({})", committee.getCommitteeName());
        tokenValidation(context);
        Response denied = forbiddenUnless("create committees", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.createCommittee(committee, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 9. POST /approvalForum/agenda/committees/{agcId}/deactivate

Deactivate a committee (secretary/chair only)

- Method: `deactivateCommittee`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.deactivateCommittee`

```java
    @POST
    @Path("/committees/{agcId}/deactivate")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Deactivate a committee (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response deactivateCommittee(@PathParam("agcId") int agcId) {
        logger.info("deactivateCommittee({})", agcId);
        tokenValidation(context);
        Response denied = forbiddenUnless("deactivate committees", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForCommittee("deactivate committees", agcId);
        if (denied != null) return denied;
        return Response.ok(agendaService.deactivateCommittee(agcId, userName())).build();
    }
```

## 10. POST /approvalForum/agenda/meetings

Schedule a committee meeting (secretary/chair only)

- Method: `createMeeting`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.createMeeting`

```java
    @POST
    @Path("/meetings")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Schedule a committee meeting (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response createMeeting(AgendaMeetingDTO meeting) {
        logger.info("createMeeting(committee {})", meeting.getAgcId());
        tokenValidation(context);
        Response denied = forbiddenUnless("schedule meetings", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForCommittee("schedule meetings", meeting.getAgcId());
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.createMeeting(meeting, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 11. PUT /approvalForum/agenda/meetings/{agmId}

Update meeting date, time, mode, venue or Teams details (secretary/chair only)

- Method: `updateMeeting`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.updateMeeting`

```java
    @PUT
    @Path("/meetings/{agmId}")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Update meeting date, time, mode, venue or Teams details (secretary/chair only)",
            authorizations = @Authorization(value = "Bearer"))
    public Response updateMeeting(@PathParam("agmId") int agmId, AgendaMeetingDTO meeting) {
        logger.info("updateMeeting({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("update meetings", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("update meetings", agmId);
        if (denied != null) return denied;
        meeting.setAgmId(agmId);
        try {
            return Response.ok(agendaService.updateMeeting(meeting, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 12. POST /approvalForum/agenda/meetings/{agmId}/cancel

Cancel a meeting (secretary/chair only)

- Method: `cancelMeeting`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.cancelMeeting`

```java
    @POST
    @Path("/meetings/{agmId}/cancel")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Cancel a meeting (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response cancelMeeting(@PathParam("agmId") int agmId, @QueryParam("reason") String reason) {
        logger.info("cancelMeeting({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("cancel meetings", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("cancel meetings", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.cancelMeeting(agmId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 13. POST /approvalForum/agenda/meetings/{agmId}/reinstate

Reinstate a cancelled meeting (secretary/chair only)

- Method: `reinstateMeeting`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.reinstateMeeting`

```java
    @POST
    @Path("/meetings/{agmId}/reinstate")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Reinstate a cancelled meeting (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response reinstateMeeting(@PathParam("agmId") int agmId, @QueryParam("reason") String reason) {
        tokenValidation(context);
        Response denied = forbiddenUnless("reinstate meetings", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("reinstate meetings", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.reinstateMeeting(agmId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 14. GET /approvalForum/agenda/meetings

Meetings across committees between two dates (calendar view)

- Method: `getMeetingsBetween`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getMeetingsBetween`

```java
    @GET
    @Path("/meetings")
    @Produces("application/json")
    @ApiOperation(value = "Meetings across committees between two dates (calendar view)",
            authorizations = @Authorization(value = "Bearer"))
    public Response getMeetingsBetween(@QueryParam("from") String from, @QueryParam("to") String to) {
        logger.info("getMeetingsBetween({}, {})", from, to);
        tokenValidation(context);
        java.sql.Timestamp[] range = dateRange(from, to);
        return Response.ok(agendaService.getMeetingsBetween(range[0], range[1])).build();
    }
```

## 15. GET /approvalForum/agenda/committees/{agcId}/dates

Available meeting dates for a committee (next 3 weeks)

- Method: `getAvailableDates`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getAvailableMeetingDates`

```java
    @GET
    @Path("/committees/{agcId}/dates")
    @Produces("application/json")
    @ApiOperation(value = "Available meeting dates for a committee (next 3 weeks)", authorizations = @Authorization(value = "Bearer"))
    public Response getAvailableDates(@PathParam("agcId") int agcId) {
        logger.info("getAvailableDates({})", agcId);
        tokenValidation(context);
        return Response.ok(agendaService.getAvailableMeetingDates(agcId)).build();
    }
```

## 16. GET /approvalForum/agenda/committees/{agcId}/lastRoster

Most recent item's roster for a committee, to pre-fill a new item

- Method: `getLastRoster`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getLastRoster`

```java
    @GET
    @Path("/committees/{agcId}/lastRoster")
    @Produces("application/json")
    @ApiOperation(value = "Most recent item's roster for a committee, to pre-fill a new item",
            authorizations = @Authorization(value = "Bearer"))
    public Response getLastRoster(@PathParam("agcId") int agcId) {
        logger.info("getLastRoster({})", agcId);
        tokenValidation(context);
        return Response.ok(agendaService.getLastRoster(agcId)).build();
    }
```

## 17. GET /approvalForum/agenda/committees/{agcId}/members

Members of the List Management list linked to a committee

- Method: `getCommitteeMembers`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getCommitteeMembers`

```java
    @GET
    @Path("/committees/{agcId}/members")
    @Produces("application/json")
    @ApiOperation(value = "Members of the List Management list linked to a committee",
            authorizations = @Authorization(value = "Bearer"))
    public Response getCommitteeMembers(@PathParam("agcId") int agcId) {
        logger.info("getCommitteeMembers({})", agcId);
        tokenValidation(context);
        return Response.ok(agendaService.getCommitteeMembers(agcId)).build();
    }
```

## 18. GET /approvalForum/agenda/committees/{agcId}/meetings

Meetings for a committee

- Method: `getMeetings`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getMeetingsForCommittee`

```java
    @GET
    @Path("/committees/{agcId}/meetings")
    @Produces("application/json")
    @ApiOperation(value = "Meetings for a committee", authorizations = @Authorization(value = "Bearer"))
    public Response getMeetings(@PathParam("agcId") int agcId) {
        logger.info("getMeetings({})", agcId);
        tokenValidation(context);
        return Response.ok(agendaService.getMeetingsForCommittee(agcId)).build();
    }
```

## 19. GET /approvalForum/agenda/meetings/{agmId}

Meeting with its agenda items

- Method: `getMeeting`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getMeetingFor`

```java
    @GET
    @Path("/meetings/{agmId}")
    @Produces("application/json")
    @ApiOperation(value = "Meeting with its agenda items", authorizations = @Authorization(value = "Bearer"))
    public Response getMeeting(@PathParam("agmId") int agmId) {
        logger.info("getMeeting({})", agmId);
        tokenValidation(context);
        return Response.ok(agendaService.getMeetingFor(agmId, userName(), access())).build();
    }
```

## 20. POST /approvalForum/agenda/meetings/{agmId}/invitation

- Method: `sendInvitation`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.sendInvitation`

```java
    @POST
    @Path("/meetings/{agmId}/invitation")
    @Produces("application/json")
    @Transactional
    public Response sendInvitation(@PathParam("agmId") int agmId) {
        tokenValidation(context);
        Response denied = forbiddenUnless("send the calendar invitation", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("send the calendar invitation", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.sendInvitation(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 21. POST /approvalForum/agenda/meetings/{agmId}/invitation/update

- Method: `sendInvitationUpdate`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.sendInvitationUpdate`

```java
    @POST
    @Path("/meetings/{agmId}/invitation/update")
    @Produces("application/json")
    @Transactional
    public Response sendInvitationUpdate(@PathParam("agmId") int agmId) {
        tokenValidation(context);
        Response denied = forbiddenUnless("send the updated calendar invitation", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("send the updated calendar invitation", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.sendInvitationUpdate(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 22. POST /approvalForum/agenda/meetings/{agmId}/held

Mark the meeting as held once its scheduled end has passed (opens post-meeting capture)

- Method: `markMeetingHeld`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.markMeetingHeld`

```java
    @POST
    @Path("/meetings/{agmId}/held")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Mark the meeting as held once its scheduled end has passed (opens post-meeting capture)",
            authorizations = @Authorization(value = "Bearer"))
    public Response markMeetingHeld(@PathParam("agmId") int agmId,
                                    @QueryParam("actualStartTime") String actualStartTime,
                                    @QueryParam("actualEndTime") String actualEndTime,
                                    @QueryParam("quorumAchieved") Boolean quorumAchieved) {
        logger.info("markMeetingHeld({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("mark the meeting held", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("mark the meeting held", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.markMeetingHeld(agmId, actualStartTime, actualEndTime, quorumAchieved, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 23. POST /approvalForum/agenda/meetings/{agmId}/close

Close the meeting record once every item has a decision and chair sign-off (Secretary/Chair)

- Method: `closeMeeting`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.closeMeeting`

```java
    @POST
    @Path("/meetings/{agmId}/close")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Close the meeting record once every item has a decision and chair sign-off (Secretary/Chair)",
            authorizations = @Authorization(value = "Bearer"))
    public Response closeMeeting(@PathParam("agmId") int agmId) {
        logger.info("closeMeeting({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("close the meeting record", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("close the meeting record", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.closeMeeting(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 24. POST /approvalForum/agenda/meetings/{agmId}/reopen

Re-open a closed meeting record through an audited correction (reviewers only)

- Method: `reopenMeeting`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.reopenMeeting`

```java
    @POST
    @Path("/meetings/{agmId}/reopen")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Re-open a closed meeting record through an audited correction (reviewers only)",
            authorizations = @Authorization(value = "Bearer"))
    public Response reopenMeeting(@PathParam("agmId") int agmId, @QueryParam("reason") String reason) {
        logger.info("reopenMeeting({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("re-open a closed meeting record", ROLE_CHAIRPERSON, ROLE_SECRETARY);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("re-open a closed meeting record", agmId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.reopenMeeting(agmId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 25. GET /approvalForum/agenda/meetings/{agmId}/records

Minutes / transcript / recording / pack references for a meeting

- Method: `getMeetingRecords`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getMeetingRecords`

```java
    @GET
    @Path("/meetings/{agmId}/records")
    @Produces("application/json")
    @ApiOperation(value = "Minutes / transcript / recording / pack references for a meeting",
            authorizations = @Authorization(value = "Bearer"))
    public Response getMeetingRecords(@PathParam("agmId") int agmId) {
        logger.info("getMeetingRecords({})", agmId);
        tokenValidation(context);
        return Response.ok(agendaService.getMeetingRecords(agmId)).build();
    }
```

## 26. POST /approvalForum/agenda/meetings/{agmId}/records

Attach a governance record reference to a meeting (Secretary/Chair)

- Method: `addMeetingRecord`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.addMeetingRecord`

```java
    @POST
    @Path("/meetings/{agmId}/records")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Attach a governance record reference to a meeting (Secretary/Chair)",
            authorizations = @Authorization(value = "Bearer"))
    public Response addMeetingRecord(@PathParam("agmId") int agmId, AgendaMeetingRecordDTO record) {
        logger.info("addMeetingRecord({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("attach meeting records", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForMeeting("attach meeting records", agmId);
        if (denied != null) return denied;
        record.setAgmId(agmId);
        try {
            return Response.ok(agendaService.addMeetingRecord(record, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 27. POST /approvalForum/agenda/items

Book an agenda item

- Method: `bookItem`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.bookItem`
- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.

```java
    @POST
    @Path("/items")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Book an agenda item", authorizations = @Authorization(value = "Bearer"))
    public Response bookItem(BookAgendaItemRequest request) {
        logger.info("bookItem(committee {})", request.getAgcId());
        tokenValidation(context);
        try {
            return Response.ok(withPermissions(agendaService.bookItem(request, userName(), access()))).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 28. PUT /approvalForum/agenda/items

Update an agenda item

- Method: `updateItem`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.updateItem`

```java
    @PUT
    @Path("/items")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Update an agenda item", authorizations = @Authorization(value = "Bearer"))
    public Response updateItem(AgendaItemDTO item) {
        logger.info("updateItem({})", item.getAgiId());
        tokenValidation(context);
        Response denied = forbiddenUnlessOwner("update agenda items", item.getAgiId());
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.updateItem(item, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 29. GET /approvalForum/agenda/items

All active agenda items

- Method: `getAllItems`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getVisibleItems`

```java
    @GET
    @Path("/items")
    @Produces("application/json")
    @ApiOperation(value = "All active agenda items", authorizations = @Authorization(value = "Bearer"))
    public Response getAllItems() {
        logger.info("getAllItems()");
        tokenValidation(context);
        // Owner, assigned reviewer, the committee's secretariat, or an approver once the item has
        // been submitted - the agenda service applies the same rule the detail endpoint does.
        return Response.ok(agendaService.getVisibleItems(userName(), access())).build();
    }
```

## 30. GET /approvalForum/agenda/items/my

Agenda items for the current user

- Method: `getMyItems`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getItemsForUser`

```java
    @GET
    @Path("/items/my")
    @Produces("application/json")
    @ApiOperation(value = "Agenda items for the current user", authorizations = @Authorization(value = "Bearer"))
    public Response getMyItems() {
        logger.info("getMyItems()");
        tokenValidation(context);
        return Response.ok(agendaService.getItemsForUser(userName())).build();
    }
```

## 31. GET /approvalForum/agenda/items/{agiId}

Agenda item detail

- Method: `getItem`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getItem`, `AgendaService.canReassignReviewer`, `AgendaService.canChangeSchedule`, `AgendaService.canRequestQuorumException`, `AgendaService.canDecideQuorumException`

```java
    @GET
    @Path("/items/{agiId}")
    @Produces("application/json")
    @ApiOperation(value = "Agenda item detail", authorizations = @Authorization(value = "Bearer"))
    public Response getItem(@PathParam("agiId") int agiId) {
        logger.info("getItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("open agenda item", agiId);
        if (denied != null) return denied;
        AgendaItemDTO item = agendaService.getItem(agiId);
        item.setCanReassignReviewer(agendaService.canReassignReviewer(agiId, userName(), access()));
        item.setCanChangeSchedule(agendaService.canChangeSchedule(agiId, userName(), access()));
        item.setCanRequestQuorumException(agendaService.canRequestQuorumException(agiId, userName()));
        item.setCanDecideQuorumException(agendaService.canDecideQuorumException(agiId, userName(), access()));
        return Response.ok(item).build();
    }
```

## 32. POST /approvalForum/agenda/items/{agiId}/meeting

Move an item to another meeting date (or start time) of its committee, before approval

- Method: `moveItem`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.moveItem`
- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.

```java
    @POST
    @Path("/items/{agiId}/meeting")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Move an item to another meeting date (or start time) of its committee, before approval",
            authorizations = @Authorization(value = "Bearer"))
    public Response moveItem(@PathParam("agiId") int agiId, MoveAgendaItemRequest request) {
        logger.info("moveItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanChangeSchedule("change the meeting date", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(withPermissions(agendaService.moveItem(agiId, request, userName()))).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 33. PUT /approvalForum/agenda/items/{agiId}/roster

Replace the quorum roster and attendees of an item, before approval

- Method: `updateRoster`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.updateRoster`
- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.

```java
    @PUT
    @Path("/items/{agiId}/roster")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Replace the quorum roster and attendees of an item, before approval",
            authorizations = @Authorization(value = "Bearer"))
    public Response updateRoster(@PathParam("agiId") int agiId, AgendaItemDTO roster) {
        logger.info("updateRoster({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanChangeSchedule("change the quorum roster", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(withPermissions(agendaService.updateRoster(agiId, roster, userName()))).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 34. GET /approvalForum/agenda/drafts

The caller's saved booking drafts

- Method: `getBookingDrafts`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getBookingDrafts`

```java
    // ---- Booking drafts: the caller's own, saved part-way through booking ----

    @GET
    @Path("/drafts")
    @Produces("application/json")
    @ApiOperation(value = "The caller's saved booking drafts", authorizations = @Authorization(value = "Bearer"))
    public Response getBookingDrafts() {
        logger.info("getBookingDrafts()");
        tokenValidation(context);
        return Response.ok(agendaService.getBookingDrafts(userName())).build();
    }
```

## 35. GET /approvalForum/agenda/drafts/{agbdId}

One of the caller's booking drafts, with its saved state

- Method: `getBookingDraft`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getBookingDraft`

```java
    @GET
    @Path("/drafts/{agbdId}")
    @Produces("application/json")
    @ApiOperation(value = "One of the caller's booking drafts, with its saved state", authorizations = @Authorization(value = "Bearer"))
    public Response getBookingDraft(@PathParam("agbdId") int agbdId) {
        logger.info("getBookingDraft({})", agbdId);
        tokenValidation(context);
        try {
            return Response.ok(agendaService.getBookingDraft(agbdId, userName())).build();
        } catch (IllegalArgumentException e) {
            return draftNotFound(e);
        }
    }
```

## 36. POST /approvalForum/agenda/drafts

Save a new booking draft

- Method: `createBookingDraft`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.saveBookingDraft`

```java
    @POST
    @Path("/drafts")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Save a new booking draft", authorizations = @Authorization(value = "Bearer"))
    public Response createBookingDraft(AgendaBookingDraftDTO draft) {
        logger.info("createBookingDraft()");
        tokenValidation(context);
        if (draft != null) draft.setAgbdId(0);
        try {
            return Response.ok(agendaService.saveBookingDraft(draft, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 37. PUT /approvalForum/agenda/drafts/{agbdId}

Update one of the caller's booking drafts

- Method: `updateBookingDraft`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.saveBookingDraft`

```java
    @PUT
    @Path("/drafts/{agbdId}")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Update one of the caller's booking drafts", authorizations = @Authorization(value = "Bearer"))
    public Response updateBookingDraft(@PathParam("agbdId") int agbdId, AgendaBookingDraftDTO draft) {
        logger.info("updateBookingDraft({})", agbdId);
        tokenValidation(context);
        if (draft == null) draft = new AgendaBookingDraftDTO();
        draft.setAgbdId(agbdId);
        try {
            return Response.ok(agendaService.saveBookingDraft(draft, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        } catch (IllegalArgumentException e) {
            return draftNotFound(e);
        }
    }
```

## 38. DELETE /approvalForum/agenda/drafts/{agbdId}

Discard one of the caller's booking drafts

- Method: `deleteBookingDraft`; transactional: yes
- Calls: `AgendaService.deleteBookingDraft`

```java
    @DELETE
    @Path("/drafts/{agbdId}")
    @Transactional
    @ApiOperation(value = "Discard one of the caller's booking drafts", authorizations = @Authorization(value = "Bearer"))
    public Response deleteBookingDraft(@PathParam("agbdId") int agbdId) {
        logger.info("deleteBookingDraft({})", agbdId);
        tokenValidation(context);
        try {
            agendaService.deleteBookingDraft(agbdId, userName());
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return draftNotFound(e);
        }
    }
```

## 39. POST /approvalForum/agenda/items/{agiId}/document

Link an uploaded credit paper (docstash) to the item and confirm it

- Method: `attachDocument`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.attachDocument`

```java
    @POST
    @Path("/items/{agiId}/document")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Link an uploaded credit paper (docstash) to the item and confirm it",
            authorizations = @Authorization(value = "Bearer"))
    public Response attachDocument(@PathParam("agiId") int agiId,
                                   @QueryParam("docStashId") String docStashId,
                                   @QueryParam("fileName") String fileName) {
        logger.info("attachDocument({}, {})", agiId, docStashId);
        tokenValidation(context);
        Response denied = forbiddenUnlessOwner("attach documents to agenda items", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.attachDocument(agiId, docStashId, fileName, userName())).build();
    }
```

## 40. POST /approvalForum/agenda/items/{agiId}/upload

Upload the credit paper for an agenda item and confirm the slot

- Method: `uploadDocument`; transactional: yes
- Consumes: multipart/form-data
- Produces: application/json
- Calls: `AgendaService.uploadDocument`
- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.

```java
    @POST
    @Path("/items/{agiId}/upload")
    @Produces("application/json")
    @Consumes("multipart/form-data")
    @Transactional
    @ApiOperation(value = "Upload the credit paper for an agenda item and confirm the slot",
            authorizations = @Authorization(value = "Bearer"))
    public Response uploadDocument(@PathParam("agiId") int agiId, MultipartFormDataInput form) {
        logger.info("uploadDocument({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessOwner("upload documents to agenda items", agiId);
        if (denied != null) return denied;

        String fileName = "unknown";
        String docType = "Credit Paper";
        byte[] bytes = new byte[0];

        Map<String, List<InputPart>> uploadForm = form.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");
        for (InputPart inputPart : inputParts) {
            try {
                MultivaluedMap<String, String> header = inputPart.getHeaders();
                fileName = getFileName(header);
                InputStream inputStream = inputPart.getBody(InputStream.class, null);
                bytes = IOUtils.toByteArray(inputStream);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        try {
            if (uploadForm.get("docType") != null) {
                docType = uploadForm.get("docType").get(0).getBody(String.class, null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        String fileExtension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
        try {
            return Response.ok(withPermissions(agendaService.uploadDocument(agiId, docType, fileName, fileExtension, bytes, userName()))).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 41. GET /approvalForum/agenda/documents/{agidId}/download

Download an agenda item credit paper

- Method: `downloadDocument`; transactional: no
- Produces: application/octet-stream
- Calls: `AgendaService.getDocument`, `AgendaService.getDocumentData`

```java
    @GET
    @Path("/documents/{agidId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Download an agenda item credit paper", authorizations = @Authorization(value = "Bearer"))
    public Response downloadDocument(@PathParam("agidId") int agidId) {
        logger.info("downloadDocument({})", agidId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanViewDocument("download agenda documents", agidId);
        if (denied != null) return denied;

        AgendaItemDocumentDTO document = agendaService.getDocument(agidId);
        byte[] data = agendaService.getDocumentData(agidId);
        if (document == null || data == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        StreamingOutput stream = output -> output.write(data);
        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                .header("content-disposition", "attachment; filename=\"" + document.getFileName() + "\"")
                .header("Content-Length", data.length).build();
    }
```

## 42. POST /approvalForum/agenda/items/{agiId}/remove

Remove an agenda item

- Method: `removeItem`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.removeItem`

```java
    @POST
    @Path("/items/{agiId}/remove")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Remove an agenda item", authorizations = @Authorization(value = "Bearer"))
    public Response removeItem(@PathParam("agiId") int agiId, @QueryParam("reason") String reason) {
        logger.info("removeItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("remove agenda items", ROLE_CREDIT_ANALYST, ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        Response notOwner = forbiddenUnlessOwner("remove agenda items", agiId);
        if (notOwner != null) return notOwner;
        try {
            return Response.ok(agendaService.removeItem(agiId, userName(), reason)).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 43. GET /approvalForum/agenda/items/{agiId}/postMeeting

Post-meeting capture for an item

- Method: `getPostMeeting`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getPostMeeting`

```java
    @GET
    @Path("/items/{agiId}/postMeeting")
    @Produces("application/json")
    @ApiOperation(value = "Post-meeting capture for an item", authorizations = @Authorization(value = "Bearer"))
    public Response getPostMeeting(@PathParam("agiId") int agiId) {
        logger.info("getPostMeeting({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("open post-meeting capture", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.getPostMeeting(agiId)).build();
    }
```

## 44. GET /approvalForum/agenda/items/{agiId}/notifications

Notification (e-mail) log for an item, with send status

- Method: `getNotifications`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getNotificationsForItem`

```java
    @GET
    @Path("/items/{agiId}/notifications")
    @Produces("application/json")
    @ApiOperation(value = "Notification (e-mail) log for an item, with send status",
            authorizations = @Authorization(value = "Bearer"))
    public Response getNotifications(@PathParam("agiId") int agiId) {
        logger.info("getNotifications({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("read agenda item notifications", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.getNotificationsForItem(agiId)).build();
    }
```

## 45. POST /approvalForum/agenda/items/{agiId}/postMeeting

Save post-meeting capture for an item

- Method: `savePostMeeting`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.savePostMeeting`

```java
    @POST
    @Path("/items/{agiId}/postMeeting")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Save post-meeting capture for an item", authorizations = @Authorization(value = "Bearer"))
    public Response savePostMeeting(@PathParam("agiId") int agiId, AgendaPostMeetingDTO postMeeting) {
        logger.info("savePostMeeting({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanCapture("record post-meeting outcomes", agiId);
        if (denied != null) return denied;
        postMeeting.setAgiId(agiId);
        return Response.ok(agendaService.savePostMeeting(postMeeting, userName())).build();
    }
```

## 46. POST /approvalForum/agenda/items/{agiId}/postMeeting/unlock

Unlock a signed-off post-meeting record (chairperson or secretary)

- Method: `unlockPostMeeting`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.unlockPostMeeting`

```java
    @POST
    @Path("/items/{agiId}/postMeeting/unlock")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Unlock a signed-off post-meeting record (chairperson or secretary)",
            authorizations = @Authorization(value = "Bearer"))
    public Response unlockPostMeeting(@PathParam("agiId") int agiId) {
        logger.info("unlockPostMeeting({})", agiId);
        tokenValidation(context);
        // The chair signed the decision off, so the chair (or the committee secretary) unlocks it for a
        // correction; the unlock is audited. A reviewer outranks neither and cannot.
        Response denied = forbiddenUnless("unlock a signed-off post-meeting record", ROLE_CHAIRPERSON, ROLE_SECRETARY);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForItem("unlock a signed-off post-meeting record", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.unlockPostMeeting(agiId, userName())).build();
    }
```

## 47. POST /approvalForum/agenda/items/{agiId}/submit

Submit an agenda item for review (mandatory fields, papers and quorum are checked)

- Method: `submitItem`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.submitItem`

```java
    // ---- BRS alignment: review & approval workflow (BR-010) ----

    @POST
    @Path("/items/{agiId}/submit")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Submit an agenda item for review (mandatory fields, papers and quorum are checked)",
            authorizations = @Authorization(value = "Bearer"))
    public Response submitItem(@PathParam("agiId") int agiId, @QueryParam("quorumOverrideReason") String quorumOverrideReason) {
        logger.info("submitItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("submit agenda items", ROLE_CREDIT_ANALYST, ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        // A quorum override is an authorised exception (RULE-005) - secretariat or chair only.
        if (quorumOverrideReason != null && !quorumOverrideReason.trim().isEmpty()
                && !hasAnyRole(ROLE_SECRETARY, ROLE_CHAIRPERSON)) {
            return forbiddenUnless("override quorum", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        }
        try {
            return Response.ok(agendaService.submitItem(agiId, quorumOverrideReason, userName(), access())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 48. POST /approvalForum/agenda/items/{agiId}/return

Return a submitted item to the analyst with a reason (reviewers)

- Method: `returnItem`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.returnItem`

```java
    @POST
    @Path("/items/{agiId}/return")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Return a submitted item to the analyst with a reason (reviewers)",
            authorizations = @Authorization(value = "Bearer"))
    public Response returnItem(@PathParam("agiId") int agiId, @QueryParam("reason") String reason) {
        logger.info("returnItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("return agenda items", reviewerRole(), ROLE_APPROVER, ROLE_CHAIRPERSON, ROLE_SECRETARY);
        if (denied != null) return denied;
        denied = forbiddenUnlessCanView("return", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.returnItem(agiId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 49. POST /approvalForum/agenda/items/{agiId}/quorum-exception

The item's analyst asks the committee's secretary and chair for a quorum exception

- Method: `requestQuorumException`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.requestQuorumException`
- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.

```java
    @POST
    @Path("/items/{agiId}/quorum-exception")
    @Consumes("application/json")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "The item's analyst asks the committee's secretary and chair for a quorum exception",
            authorizations = @Authorization(value = "Bearer"))
    public Response requestQuorumException(@PathParam("agiId") int agiId, QuorumExceptionRequest request) {
        logger.info("requestQuorumException({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessOwner("ask for a quorum exception on agenda items", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(withPermissions(agendaService.requestQuorumException(agiId,
                    request == null ? null : request.getReason(), userName()))).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 50. POST /approvalForum/agenda/items/{agiId}/quorum-exception/decision

The committee's secretary or chair approves or declines a requested quorum exception

- Method: `decideQuorumException`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.decideQuorumException`
- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.

```java
    @POST
    @Path("/items/{agiId}/quorum-exception/decision")
    @Consumes("application/json")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "The committee's secretary or chair approves or declines a requested quorum exception",
            authorizations = @Authorization(value = "Bearer"))
    public Response decideQuorumException(@PathParam("agiId") int agiId, QuorumExceptionRequest request) {
        logger.info("decideQuorumException({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("decide quorum exceptions", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        if (request == null || request.getApproved() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(java.util.Collections.singletonMap("message", "Say whether the quorum exception is approved")).build();
        }
        try {
            return Response.ok(withPermissions(agendaService.decideQuorumException(agiId, request.getApproved(),
                    request.getNote(), userName(), access()))).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 51. POST /approvalForum/agenda/items/{agiId}/reviewer

- Method: `reassignReviewer`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.canReassignReviewer`, `AgendaService.reassignReviewer`

```java
    @POST
    @Path("/items/{agiId}/reviewer")
    @Consumes("application/json")
    @Produces("application/json")
    @Transactional
    public Response reassignReviewer(@PathParam("agiId") int agiId, ReassignAgendaReviewerRequest request) {
        tokenValidation(context);
        Response denied = forbiddenUnless("reassign reviewers", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        if (!agendaService.canReassignReviewer(agiId, userName(), access())) {
            return forbidden("reassign reviewer", agiId, "Only this committee's secretary or chair may reassign an item awaiting review on an open agenda.");
        }
        try {
            AgendaItemDTO item = agendaService.reassignReviewer(agiId, request, userName(), access());
            item.setCanReassignReviewer(agendaService.canReassignReviewer(agiId, userName(), access()));
            return Response.ok(item).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 52. POST /approvalForum/agenda/items/{agiId}/review

Complete independent review of the whole submission (reviewer/Secretary/Chair)

- Method: `completeReview`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.completeReview`

```java
    @POST
    @Path("/items/{agiId}/review")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Complete independent review of the whole submission (reviewer/Secretary/Chair)",
            authorizations = @Authorization(value = "Bearer"))
    public Response completeReview(@PathParam("agiId") int agiId, @QueryParam("comments") String comments) {
        tokenValidation(context);
        Response denied = forbiddenUnless("complete submission review", reviewerRole(), ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessCanView("review", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.completeReview(agiId, comments, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 53. POST /approvalForum/agenda/items/{agiId}/approve

Approve a submitted item onto the agenda (Approver, or the committee's chair or secretary)

- Method: `approveItem`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.approveItem`

```java
    @POST
    @Path("/items/{agiId}/approve")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Approve a submitted item onto the agenda (Approver, or the committee's chair or secretary)",
            authorizations = @Authorization(value = "Bearer"))
    public Response approveItem(@PathParam("agiId") int agiId, @QueryParam("comments") String comments) {
        logger.info("approveItem({})", agiId);
        tokenValidation(context);
        // The approver is whoever holds the chairperson or secretary role (or the Approver role, where granted).
        Response denied = forbiddenUnless("approve agenda items", ROLE_APPROVER, ROLE_CHAIRPERSON, ROLE_SECRETARY);
        if (denied != null) return denied;
        if (!hasAnyRole(ROLE_APPROVER)) {
            denied = forbiddenUnlessDecidesForItem("approve agenda items", agiId);
            if (denied != null) return denied;
        }
        try {
            return Response.ok(agendaService.approveItem(agiId, comments, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 54. POST /approvalForum/agenda/items/{agiId}/reject

Reject a submitted item with a reason (Approver, or the committee's chair or secretary)

- Method: `rejectItem`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.rejectItem`

```java
    @POST
    @Path("/items/{agiId}/reject")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Reject a submitted item with a reason (Approver, or the committee's chair or secretary)",
            authorizations = @Authorization(value = "Bearer"))
    public Response rejectItem(@PathParam("agiId") int agiId, @QueryParam("reason") String reason) {
        logger.info("rejectItem({})", agiId);
        tokenValidation(context);
        // The same people who may approve may reject: the approver role, or the committee's chair or secretary.
        Response denied = forbiddenUnless("reject agenda items", ROLE_APPROVER, ROLE_CHAIRPERSON, ROLE_SECRETARY);
        if (denied != null) return denied;
        if (!hasAnyRole(ROLE_APPROVER)) {
            denied = forbiddenUnlessDecidesForItem("reject agenda items", agiId);
            if (denied != null) return denied;
        }
        try {
            return Response.ok(agendaService.rejectItem(agiId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 55. POST /approvalForum/agenda/items/{agiId}/reopen

Take an item back to the analyst; on a locked agenda this is an audited override (Secretary/Chair)

- Method: `reopenItem`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.reopenItem`

```java
    @POST
    @Path("/items/{agiId}/reopen")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Take an item back to the analyst; on a locked agenda this is an audited override (Secretary/Chair)",
            authorizations = @Authorization(value = "Bearer"))
    public Response reopenItem(@PathParam("agiId") int agiId, @QueryParam("reason") String reason) {
        logger.info("reopenItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("re-open agenda items", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        denied = forbiddenUnlessDecidesForItem("re-open agenda items", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.reopenItem(agiId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 56. GET /approvalForum/agenda/items/{agiId}/actions

Workflow history (actor, action, timestamp, reason) for an item

- Method: `getWorkflowActions`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getWorkflowActions`

```java
    @GET
    @Path("/items/{agiId}/actions")
    @Produces("application/json")
    @ApiOperation(value = "Workflow history (actor, action, timestamp, reason) for an item",
            authorizations = @Authorization(value = "Bearer"))
    public Response getWorkflowActions(@PathParam("agiId") int agiId) {
        logger.info("getWorkflowActions({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("read the agenda item audit trail", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.getWorkflowActions(agiId)).build();
    }
```

## 57. POST /approvalForum/agenda/documents/{agidId}/validate

Accept or reject an uploaded document (reviewers/secretariat)

- Method: `validateDocument`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.validateDocument`

```java
    // ---- documents (section 6.5) ----

    @POST
    @Path("/documents/{agidId}/validate")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Accept or reject an uploaded document (reviewers/secretariat)",
            authorizations = @Authorization(value = "Bearer"))
    public Response validateDocument(@PathParam("agidId") int agidId, @QueryParam("status") String status,
                                     @QueryParam("comment") String comment) {
        logger.info("validateDocument({}, {})", agidId, status);
        tokenValidation(context);
        Response denied = forbiddenUnless("validate documents", reviewerRole(), ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.validateDocument(agidId, status, comment, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 58. GET /approvalForum/agenda/items/{agiId}/participants

Committee roles and people assigned to an item

- Method: `getParticipants`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getParticipants`

```java
    // ---- participants & quorum (section 6.4) ----

    @GET
    @Path("/items/{agiId}/participants")
    @Produces("application/json")
    @ApiOperation(value = "Committee roles and people assigned to an item", authorizations = @Authorization(value = "Bearer"))
    public Response getParticipants(@PathParam("agiId") int agiId) {
        logger.info("getParticipants({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("read agenda item participants", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.getParticipants(agiId)).build();
    }
```

## 59. PUT /approvalForum/agenda/items/{agiId}/participants

Replace the roster before the meeting, or capture actual attendance after it

- Method: `saveParticipants`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.saveParticipants`

```java
    @PUT
    @Path("/items/{agiId}/participants")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Replace the roster before the meeting, or capture actual attendance after it",
            authorizations = @Authorization(value = "Bearer"))
    public Response saveParticipants(@PathParam("agiId") int agiId, List<AgendaParticipantDTO> participants) {
        logger.info("saveParticipants({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanEditParticipants("change agenda item participants", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.saveParticipants(agiId, participants, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 60. GET /approvalForum/agenda/items/{agiId}/quorum

Role-based quorum evaluation for an item

- Method: `evaluateQuorum`; transactional: no
- Produces: application/json
- Calls: `AgendaService.evaluateQuorum`

```java
    @GET
    @Path("/items/{agiId}/quorum")
    @Produces("application/json")
    @ApiOperation(value = "Role-based quorum evaluation for an item", authorizations = @Authorization(value = "Bearer"))
    public Response evaluateQuorum(@PathParam("agiId") int agiId) {
        logger.info("evaluateQuorum({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("evaluate quorum", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.evaluateQuorum(agiId)).build();
    }
```

## 61. GET /approvalForum/agenda/items/{agiId}/conditions

Conditions, actions and follow-ups for an item

- Method: `getConditions`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getConditions`

```java
    // ---- conditions & actions (section 6.8) ----

    @GET
    @Path("/items/{agiId}/conditions")
    @Produces("application/json")
    @ApiOperation(value = "Conditions, actions and follow-ups for an item", authorizations = @Authorization(value = "Bearer"))
    public Response getConditions(@PathParam("agiId") int agiId) {
        logger.info("getConditions({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanView("read agenda item conditions", agiId);
        if (denied != null) return denied;
        return Response.ok(agendaService.getConditions(agiId)).build();
    }
```

## 62. PUT /approvalForum/agenda/items/{agiId}/conditions

Save the conditions captured at the meeting (after Held)

- Method: `saveConditions`; transactional: yes
- Consumes: application/json
- Produces: application/json
- Calls: `AgendaService.saveConditions`

```java
    @PUT
    @Path("/items/{agiId}/conditions")
    @Produces("application/json")
    @Consumes("application/json")
    @Transactional
    @ApiOperation(value = "Save the conditions captured at the meeting (after Held)", authorizations = @Authorization(value = "Bearer"))
    public Response saveConditions(@PathParam("agiId") int agiId, List<AgendaConditionDTO> conditions) {
        logger.info("saveConditions({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnlessCanCapture("change agenda item conditions", agiId);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.saveConditions(agiId, conditions, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 63. POST /approvalForum/agenda/conditions/{agcaId}/close

Close or waive a condition with closure comments

- Method: `closeCondition`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.canCloseCondition`, `AgendaService.closeCondition`

```java
    @POST
    @Path("/conditions/{agcaId}/close")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Close or waive a condition with closure comments", authorizations = @Authorization(value = "Bearer"))
    public Response closeCondition(@PathParam("agcaId") int agcaId, @QueryParam("status") String status,
                                   @QueryParam("comments") String comments) {
        logger.info("closeCondition({}, {})", agcaId, status);
        tokenValidation(context);
        if (!agendaService.canCloseCondition(agcaId, userName(), access())) {
            logger.warn("Agenda: close condition refused for {} on condition {}", userName(), agcaId);
            return Response.status(Response.Status.FORBIDDEN).entity(java.util.Collections.singletonMap("message",
                    "Only the secretary or chair of the committee for this condition's agenda item can close or waive it.")).build();
        }
        try {
            return Response.ok(agendaService.closeCondition(agcaId, status, comments, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }
```

## 64. GET /approvalForum/agenda/reports/summary

Operational reporting aggregates for a date range (yyyy-MM-dd)

- Method: `getReportSummary`; transactional: no
- Produces: application/json
- Calls: `AgendaService.getReportSummary`

```java
    // ---- reporting (section 10) ----

    @GET
    @Path("/reports/summary")
    @Produces("application/json")
    @ApiOperation(value = "Operational reporting aggregates for a date range (yyyy-MM-dd)",
            authorizations = @Authorization(value = "Bearer"))
    public Response getReportSummary(@QueryParam("from") String from, @QueryParam("to") String to) {
        logger.info("getReportSummary({}, {})", from, to);
        tokenValidation(context);
        java.sql.Timestamp[] range = dateRange(from, to);
        return Response.ok(agendaService.getReportSummary(range[0], range[1], userName(), access())).build();
    }
```

## 65. POST /approvalForum/agenda/processCutOff

Remove unconfirmed items on meetings past their cut-off (scheduler)

- Method: `processCutOff`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.processCutOffs`

```java
    @POST
    @Path("/processCutOff")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Remove unconfirmed items on meetings past their cut-off (scheduler)",
            authorizations = @Authorization(value = "Bearer"))
    public Response processCutOff() {
        logger.info("processCutOff()");
        tokenValidation(context);
        Response denied = forbiddenUnlessScheduler("process agenda cut-offs");
        if (denied != null) return denied;
        return Response.ok(agendaService.processCutOffs()).build();
    }
```

## 66. POST /approvalForum/agenda/processReminders

Send upload reminders for outstanding credit papers approaching cut-off (scheduler)

- Method: `processReminders`; transactional: yes
- Produces: application/json
- Calls: `AgendaService.processReminders`

```java
    @POST
    @Path("/processReminders")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Send upload reminders for outstanding credit papers approaching cut-off (scheduler)",
            authorizations = @Authorization(value = "Bearer"))
    public Response processReminders() {
        logger.info("processReminders()");
        tokenValidation(context);
        Response denied = forbiddenUnlessScheduler("send agenda reminders");
        if (denied != null) return denied;
        return Response.ok(agendaService.processReminders()).build();
    }
```
