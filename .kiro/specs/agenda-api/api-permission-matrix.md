# Agenda API permission matrix

This is a generated backend contract reference for the Agenda Builder API in daf-boot. Literal declarations below are part of the specification; requirements.md and design.md explain them. It is not evidence of a live service, environment or database.

Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. No API, directory or database is accessed.

Source: `api/src/main/java/za/co/rmb/approvalforum/web/resources/ApprovalForumAgendaResource.java`. SHA-256: `2b8f8198844673b7cca7649ae5947d1fb14b2fd07ddee9f8cc90993200f9577e`.

Roles come from the caller's AD groups (`ApprovalForumAuthResource.getAFGroups`) through the DAF token. `ReviewAnalyst` is the configured `af.agenda.reviewer.role`. A role only confers oversight in `access()` while it is listed in `af.agenda.oversight.roles`; the role guards below (`forbiddenUnless`) use `hasAnyRole` directly. "Any" means any authenticated caller. A scope guard narrows a role to the item or committee.

| Verb | Path | CreditAnalyst | ReviewAnalyst | Approver | Chairperson | Secretary | Scope |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/approvalForum/agenda/users` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/lists` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/counterparties` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/casdm/application` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/committees` | Any | Any | Any | Any | Any | — |
| PUT | `/approvalForum/agenda/committees` | No | No | No | Yes | Yes | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| GET | `/approvalForum/agenda/committees/all` | No | No | No | Yes | Yes | — |
| POST | `/approvalForum/agenda/committees` | No | No | No | Yes | Yes | — |
| POST | `/approvalForum/agenda/committees/{agcId}/deactivate` | No | No | No | Yes | Yes | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings` | No | No | No | Yes | Yes | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| PUT | `/approvalForum/agenda/meetings/{agmId}` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings/{agmId}/cancel` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings/{agmId}/reinstate` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| GET | `/approvalForum/agenda/meetings` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/committees/{agcId}/dates` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/committees/{agcId}/lastRoster` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/committees/{agcId}/members` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/committees/{agcId}/meetings` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/meetings/{agmId}` | Any | Any | Any | Any | Any | — |
| POST | `/approvalForum/agenda/meetings/{agmId}/invitation` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings/{agmId}/invitation/update` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings/{agmId}/held` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings/{agmId}/close` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/meetings/{agmId}/reopen` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| GET | `/approvalForum/agenda/meetings/{agmId}/records` | Any | Any | Any | Any | Any | — |
| POST | `/approvalForum/agenda/meetings/{agmId}/records` | No | No | No | Yes | Yes | serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/items` | Any | Any | Any | Any | Any | — |
| PUT | `/approvalForum/agenda/items` | Any | Any | Any | Any | Any | owner: booking analyst or named primary analyst |
| GET | `/approvalForum/agenda/items` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/items/my` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/items/{agiId}` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| POST | `/approvalForum/agenda/items/{agiId}/meeting` | Any | Any | Any | Any | Any | owner or assigned review analyst, item before approval, meeting Draft or Compiled |
| PUT | `/approvalForum/agenda/items/{agiId}/roster` | Any | Any | Any | Any | Any | owner or assigned review analyst, item before approval, meeting Draft or Compiled |
| GET | `/approvalForum/agenda/drafts` | Any | Any | Any | Any | Any | — |
| GET | `/approvalForum/agenda/drafts/{agbdId}` | Any | Any | Any | Any | Any | — |
| POST | `/approvalForum/agenda/drafts` | Any | Any | Any | Any | Any | — |
| PUT | `/approvalForum/agenda/drafts/{agbdId}` | Any | Any | Any | Any | Any | — |
| DELETE | `/approvalForum/agenda/drafts/{agbdId}` | Any | Any | Any | Any | Any | — |
| POST | `/approvalForum/agenda/items/{agiId}/document` | Any | Any | Any | Any | Any | owner: booking analyst or named primary analyst |
| POST | `/approvalForum/agenda/items/{agiId}/upload` | Any | Any | Any | Any | Any | owner: booking analyst or named primary analyst |
| GET | `/approvalForum/agenda/documents/{agidId}/download` | Any | Any | Any | Any | Any | can view the document's item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| POST | `/approvalForum/agenda/items/{agiId}/remove` | Yes | No | No | Yes | Yes | owner: booking analyst or named primary analyst |
| GET | `/approvalForum/agenda/items/{agiId}/postMeeting` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| GET | `/approvalForum/agenda/items/{agiId}/notifications` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| POST | `/approvalForum/agenda/items/{agiId}/postMeeting` | Any | Any | Any | Any | Any | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/items/{agiId}/postMeeting/unlock` | No | No | No | Yes | Yes | serves the item's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/items/{agiId}/submit` | Yes | No | No | Yes | Yes | a non-blank override reason also needs Secretary or Chairperson |
| POST | `/approvalForum/agenda/items/{agiId}/return` | No | Yes | Yes | Yes | Yes | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| POST | `/approvalForum/agenda/items/{agiId}/quorum-exception` | Any | Any | Any | Any | Any | owner: booking analyst or named primary analyst |
| POST | `/approvalForum/agenda/items/{agiId}/quorum-exception/decision` | No | No | No | Yes | Yes | — |
| POST | `/approvalForum/agenda/items/{agiId}/reviewer` | No | No | No | Yes | Yes | serves this committee as secretariat or chair; item Submitted; meeting Draft or Compiled |
| POST | `/approvalForum/agenda/items/{agiId}/review` | No | Yes | No | Yes | Yes | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| POST | `/approvalForum/agenda/items/{agiId}/approve` | No | No | Yes | Yes | Yes | without the Approver role: serves the item's committee as secretariat (Secretary role) or chair (Chairperson role) |
| POST | `/approvalForum/agenda/items/{agiId}/reject` | No | No | Yes | Yes | Yes | without the Approver role: serves the item's committee as secretariat (Secretary role) or chair (Chairperson role) |
| POST | `/approvalForum/agenda/items/{agiId}/reopen` | No | No | No | Yes | Yes | serves the item's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| GET | `/approvalForum/agenda/items/{agiId}/actions` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| POST | `/approvalForum/agenda/documents/{agidId}/validate` | No | Yes | No | Yes | Yes | — |
| GET | `/approvalForum/agenda/items/{agiId}/participants` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| PUT | `/approvalForum/agenda/items/{agiId}/participants` | Any | Any | Any | Any | Any | owner before Held; committee secretary or chair at any time |
| GET | `/approvalForum/agenda/items/{agiId}/quorum` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| GET | `/approvalForum/agenda/items/{agiId}/conditions` | Any | Any | Any | Any | Any | can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted) |
| PUT | `/approvalForum/agenda/items/{agiId}/conditions` | Any | Any | Any | Any | Any | serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role) |
| POST | `/approvalForum/agenda/conditions/{agcaId}/close` | Any | Any | Any | Any | Any | serves the condition's committee as secretariat (Secretary role) or chair (Chairperson role) |
| GET | `/approvalForum/agenda/reports/summary` | Any | Any | Any | Any | Any | — |
| POST | `/approvalForum/agenda/processCutOff` | Any | Any | Any | Any | Any | the scheduler's own user only (`af.scheduler.user`, the subject of `af.token`) |
| POST | `/approvalForum/agenda/processReminders` | Any | Any | Any | Any | Any | the scheduler's own user only (`af.scheduler.user`, the subject of `af.token`) |

## Guard helpers

Exact declarations used by the endpoints above.

```java
    /** Business-rule violations (state machine, double-booked slots) surface as 409 + message. */
    private Response conflict(IllegalStateException e) {
        logger.warn("Agenda business rule rejected the request: {}", e.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .entity(java.util.Collections.singletonMap("message", e.getMessage())).build();
    }
```
```java
    // ---- Role-based access (BR-024, RULE-008). DAF roles come from the AD groups mapped in
    // ApprovalForumAuthResource.getAFGroups: CreditAnalyst, ReviewAnalyst, Approver, Chairperson
    // and Secretary (DL_/FL_/G_DAF_<env>_Secretary - the committee secretariat). ----

    static final String ROLE_SECRETARY = "Secretary";
```
```java
    static final String ROLE_CHAIRPERSON = "Chairperson";
```
```java
    static final String ROLE_APPROVER = "Approver";
```
```java
    static final String ROLE_CREDIT_ANALYST = "CreditAnalyst";
```
```java
    private boolean hasAnyRole(String... roles) {
        if (context == null) {
            return false;
        }
        for (String role : roles) {
            if (role != null && !role.trim().isEmpty() && context.isUserInRole(role.trim())) {
                return true;
            }
        }
        return false;
    }
```
```java
    private String reviewerRole() {
        String role = afSettings.get("af.agenda.reviewer.role");
        return role == null || role.trim().isEmpty() ? "ReviewAnalyst" : role.trim();
    }
```
```java
    /**
     * The DAF roles the caller holds, read from the request's security context. Those roles are
     * derived purely from the caller's AD group membership in
     * {@code ApprovalForumAuthResource.getAFGroups} - nothing here is a static user list. The role
     * *names* are configurable (`af.agenda.reviewer.role`, `af.agenda.oversight.roles`) so an
     * environment can rename the AD-group-derived role without a code change.
     *
     * <p>Holding a role is not by itself permission to open an item: the agenda service scopes a
     * secretary or chair to the committee they actually serve (AGC chair / secretary / minute
     * taker / administrator) and an approver to items that have been submitted. Ownership is what
     * grants the right to change an item.</p>
     */
    private AgendaAccess access() {
        java.util.List<String> configured = java.util.Arrays.asList(oversightRoles());
        return new AgendaAccess(
                isOversightRole(configured, ROLE_SECRETARY) && hasAnyRole(ROLE_SECRETARY),
                isOversightRole(configured, ROLE_CHAIRPERSON) && hasAnyRole(ROLE_CHAIRPERSON),
                isOversightRole(configured, reviewerRole()) && hasAnyRole(reviewerRole()),
                isOversightRole(configured, ROLE_APPROVER) && hasAnyRole(ROLE_APPROVER));
    }
```
```java
    /** A role only confers oversight while it is listed in `af.agenda.oversight.roles`. */
    private boolean isOversightRole(java.util.List<String> configured, String role) {
        for (String candidate : configured) {
            if (candidate != null && candidate.trim().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
```
```java
    /**
     * The oversight roles come from configuration (`af.agenda.oversight.roles`, semicolon or comma
     * separated), like the reviewer role does, so an environment can change which AD-group-derived
     * DAF roles may check other analysts' items without a code change.
     */
    private String[] oversightRoles() {
        String configured = afSettings.get("af.agenda.oversight.roles");
        if (configured == null || configured.trim().isEmpty()) {
            return new String[] { ROLE_SECRETARY, ROLE_CHAIRPERSON, ROLE_APPROVER, reviewerRole() };
        }
        return configured.split("[;,]");
    }
```
```java
    private Response forbidden(String what, int agiId, String message) {
        logger.warn("Agenda: {} refused for {} on agenda item {}", what, userName(), agiId);
        return Response.status(Response.Status.FORBIDDEN)
                .entity(java.util.Collections.singletonMap("message", message)).build();
    }
```
```java
    /** 403 unless the caller owns this item - the booking analyst or the named primary analyst. */
    private Response forbiddenUnlessOwner(String what, int agiId) {
        if (agendaService.canEditItem(agiId, userName())) {
            return null;
        }
        return forbidden(what, agiId, "Only the analyst who booked agenda item " + agiId
                + ", or the primary analyst named on it, can change it.");
    }
```
```java
    /** 403 unless the caller may open this item at all (owner, assigned reviewer, secretariat, approver). */
    private Response forbiddenUnlessCanView(String what, int agiId) {
        if (agendaService.canViewItem(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Agenda item " + agiId + " belongs to another analyst.");
    }
```
```java
    /**
     * 403 unless the caller may change the participant roster - the owner until the meeting is
     * held (the planned roster the quorum check at submission reads), then the secretary or chair.
     */
    private Response forbiddenUnlessCanEditParticipants(String what, int agiId) {
        if (agendaService.canEditParticipants(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Agenda item " + agiId + " - once the meeting has been held "
                + "only the secretary or chair of its committee can change who attended.");
    }
```
```java
    /**
     * 403 unless the caller is the secretary or chair of this item's committee. The meeting record
     * - decisions, attendance, conditions - belongs to the secretariat; the analyst who owns the
     * item and the review analyst can read it but not change it.
     */
    private Response forbiddenUnlessCanCapture(String what, int agiId) {
        if (agendaService.canCaptureOutcome(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Only the secretary or chair of the committee for agenda item "
                + agiId + " can record what happened at the meeting.");
    }
```
```java
    /** 403 unless the caller is the secretary or chair serving the committee this meeting belongs to. */
    private Response forbiddenUnlessDecidesForMeeting(String what, int agmId) {
        if (agendaService.decidesForMeeting(agmId, userName(), access())) {
            return null;
        }
        logger.warn("Agenda: {} refused for {} on meeting {}", what, userName(), agmId);
        return Response.status(Response.Status.FORBIDDEN).entity(java.util.Collections.singletonMap("message",
                "Only the secretary or chair of this meeting's committee may " + what + ".")).build();
    }
```
```java
    /** 403 unless the caller is the secretary or chair serving this committee. */
    private Response forbiddenUnlessDecidesForCommittee(String what, int agcId) {
        if (agendaService.decidesForCommittee(agcId, userName(), access())) {
            return null;
        }
        logger.warn("Agenda: {} refused for {} on committee {}", what, userName(), agcId);
        return Response.status(Response.Status.FORBIDDEN).entity(java.util.Collections.singletonMap("message",
                "Only the secretary or chair of this committee may " + what + ".")).build();
    }
```
```java
    /** 403 unless the caller is the secretary or chair serving the committee this item sits on. */
    private Response forbiddenUnlessDecidesForItem(String what, int agiId) {
        if (agendaService.canCaptureOutcome(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Only the secretary or chair of the committee for agenda item " + agiId
                + " may " + what + ".");
    }
```
```java
    /** 403 unless the caller may open the item this document belongs to. */
    private Response forbiddenUnlessCanViewDocument(String what, int agidId) {
        if (agendaService.canViewDocument(agidId, userName(), access())) {
            return null;
        }
        logger.warn("Agenda: {} refused for {} on document {}", what, userName(), agidId);
        return Response.status(Response.Status.FORBIDDEN).entity(java.util.Collections.singletonMap("message",
                "Agenda document " + agidId + " belongs to another analyst's item.")).build();
    }
```
```java
    /** 403 unless the caller is the scheduler's own user (`af.scheduler.user`: the subject of `af.token`). */
    private Response forbiddenUnlessScheduler(String what) {
        String scheduler = afSettings.get("af.scheduler.user");
        if (scheduler != null && !scheduler.trim().isEmpty() && scheduler.trim().equalsIgnoreCase(userName())) {
            return null;
        }
        logger.warn("Agenda: {} refused for {} - scheduler only", what, userName());
        return Response.status(Response.Status.FORBIDDEN).entity(java.util.Collections.singletonMap("message",
                "Only the agenda scheduler may " + what + ".")).build();
    }
```
```java
    /** 403 with the same message shape as the business-rule 409, or null when allowed. */
    private Response forbiddenUnless(String what, String... roles) {
        if (hasAnyRole(roles)) {
            return null;
        }
        logger.warn("Agenda: {} refused for {} - requires one of {}", what, userName(), String.join("/", roles));
        return Response.status(Response.Status.FORBIDDEN)
                .entity(java.util.Collections.singletonMap("message",
                        "Only " + String.join(" or ", roles) + " may " + what)).build();
    }
```
```java
    /** ISO dates (yyyy-MM-dd) to an inclusive timestamp range; defaults to the last and next 90 days. */
    private java.sql.Timestamp[] dateRange(String from, String to) {
        java.time.LocalDate start = from == null || from.trim().isEmpty()
                ? java.time.LocalDate.now().minusDays(90) : java.time.LocalDate.parse(from.trim());
        java.time.LocalDate end = to == null || to.trim().isEmpty()
                ? java.time.LocalDate.now().plusDays(90) : java.time.LocalDate.parse(to.trim());
        return new java.sql.Timestamp[]{
                java.sql.Timestamp.valueOf(start.atStartOfDay()),
                java.sql.Timestamp.valueOf(end.atTime(23, 59, 59))};
    }
```
```java
    /** The item as the page shows it after a change: with what this caller may still do. */
    private AgendaItemDTO withPermissions(AgendaItemDTO item) {
        if (item != null) {
            item.setCanReassignReviewer(agendaService.canReassignReviewer(item.getAgiId(), userName(), access()));
            item.setCanChangeSchedule(agendaService.canChangeSchedule(item.getAgiId(), userName(), access()));
            item.setCanRequestQuorumException(agendaService.canRequestQuorumException(item.getAgiId(), userName()));
            item.setCanDecideQuorumException(agendaService.canDecideQuorumException(item.getAgiId(), userName(), access()));
        }
        return item;
    }
```
```java
    /** 403 unless the caller may move this item or change its roster (owner or assigned reviewer, before approval). */
    private Response forbiddenUnlessCanChangeSchedule(String what, int agiId) {
        if (agendaService.canChangeSchedule(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Only the analyst who owns agenda item " + agiId + ", or its review analyst, can "
                + what + " - and only before the item is approved, while its meeting is open.");
    }
```
```java
    private Response draftNotFound(IllegalArgumentException e) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(java.util.Collections.singletonMap("message", e.getMessage())).build();
    }
```
```java
    private String userName() {
        if (context != null && context.getUserPrincipal() != null) {
            return context.getUserPrincipal().getName();
        }
        return "SYSTEM";
    }
```

## Item and committee scope rules

Source: `api/src/main/java/za/co/rmb/approvalforum/service/impl/AgendaServiceImpl.java`. SHA-256: `c8e9a8d36aed03cbdc9cb1debb73a5bcf452fb2b4084188e67e4702a6fe2af59`.

```java
    private boolean decidesFor(ApprovalForumAgcAgendaCommitteeEntity committee, String user, AgendaAccess access) {
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        return (roles.isSecretary() && servesAsSecretary(committee, user))
                || (roles.isChairperson() && servesAsChair(committee, user));
    }
```
```java
    @Override
    public boolean canRequestQuorumException(int agiId, String user) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        return item != null && item.isActive() && isOwner(item, user)
                && EXCEPTION_REQUEST_STATUSES.contains(item.getItemStatus()) && meetingOpen(item)
                && !EXCEPTION_REQUESTED.equals(item.getQuorumExceptionStatus())
                && !EXCEPTION_APPROVED.equals(item.getQuorumExceptionStatus())
                && !evaluateQuorum(item).isMet();
    }
```
```java
    @Override
    public boolean canDecideQuorumException(int agiId, String user, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        return item != null && item.isActive() && EXCEPTION_REQUESTED.equals(item.getQuorumExceptionStatus())
                && EXCEPTION_DECISION_STATUSES.contains(item.getItemStatus())
                && decidesFor(committeeOf(item), user, access);
    }
```
```java
    // ---- Agenda item visibility (BR-024, R13) -------------------------------------------------
    // An agenda item is a confidential credit paper, so the default is no access. Exactly four
    // things open one up, and only the first grants the right to change it:
    //   owner      - the analyst who booked the item, or the primary analyst named on it
    //   reviewer   - the review analyst assigned to that item, read-only
    //   secretariat- the secretary / chair of the committee the item sits on, read-only
    //   approver   - read-only, and only once the item has been submitted for approval
    // Non-owners still act through the workflow endpoints (submit, return, approve, reject,
    // compile, lock), which carry their own role checks; what they cannot do is edit the paper.

    private static boolean sameUser(String a, String b) {
        return a != null && b != null && !a.trim().isEmpty() && a.trim().equalsIgnoreCase(b.trim());
    }
```
```java
    /** The owner rule: who booked it, or who is named as the primary analyst on it. */
    private boolean isOwner(ApprovalForumAgiAgendaItemEntity item, String userId) {
        return item != null
                && (sameUser(item.getCreatedBy(), userId) || sameUser(item.getPrimaryAnalystId(), userId));
    }
```
```java
    /** Submitted at least once, so an approver has a reason to look at it. */
    private boolean hasBeenSubmitted(ApprovalForumAgiAgendaItemEntity item) {
        String status = item == null ? null : item.getItemStatus();
        return STATUS_SUBMITTED.equals(status) || STATUS_REVIEWED.equals(status) || STATUS_APPROVED.equals(status)
                || STATUS_REJECTED.equals(status) || STATUS_RETURNED.equals(status);
    }
```
```java
    /** The secretariat of the committee this item sits on: secretary, minute taker or administrator. */
    private boolean servesAsSecretary(ApprovalForumAgcAgendaCommitteeEntity c, String userId) {
        return c != null && (sameUser(c.getMeetingSecretaryId(), userId)
                || sameUser(c.getMinuteTakerId(), userId)
                || sameUser(c.getAdministratorId(), userId));
    }
```
```java
    /** The chair of the committee this item sits on, or its alternate chair. */
    private boolean servesAsChair(ApprovalForumAgcAgendaCommitteeEntity c, String userId) {
        return c != null && (sameUser(c.getChairId(), userId) || sameUser(c.getAltChairId(), userId));
    }
```
```java
    private boolean canView(ApprovalForumAgiAgendaItemEntity item, String userId, AgendaAccess access) {
        if (item == null) {
            return false;
        }
        if (isOwner(item, userId)) {
            return true;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        if (roles.isReviewer() && sameUser(item.getReviewAnalystId(), userId)) {
            return true;
        }
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        if (roles.isSecretary() && servesAsSecretary(committee, userId)) {
            return true;
        }
        if (roles.isChairperson() && servesAsChair(committee, userId)) {
            return true;
        }
        return roles.isApprover() && hasBeenSubmitted(item);
    }
```
```java
    @Override
    public List<AgendaItemDTO> getVisibleItems(String userId, AgendaAccess access) {
        return agendaRepository.getAllItems().stream()
                .filter(i -> canView(i, userId, access))
                .map(i -> toItemDTO(i, false))
                .collect(Collectors.toList());
    }
```
```java
    @Override
    public boolean canViewItem(int agiId, String userId, AgendaAccess access) {
        return canView(agendaRepository.getItem(agiId), userId, access);
    }
```
```java
    @Override
    public boolean canEditItem(int agiId, String userId) {
        return isOwner(agendaRepository.getItem(agiId), userId);
    }
```
```java
    /**
     * The participant roster has two lives. Before the meeting is Held it is the *planned* roster
     * the analyst builds to pass the quorum check at submission, so the owner may set it. Once the
     * meeting is Held it becomes the record of who actually attended, which is the secretariat's to
     * keep - from that point only the secretary or chair of the committee may change it.
     */
    @Override
    public boolean canEditParticipants(int agiId, String userId, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            return false;
        }
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        boolean held = meeting != null && MEETING_HELD.equals(meeting.getStatus());
        if (!held && isOwner(item, userId)) {
            return true;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        return (roles.isSecretary() && servesAsSecretary(committee, userId))
                || (roles.isChairperson() && servesAsChair(committee, userId));
    }
```
```java
    /**
     * The meeting date and the quorum roster are logistics, not the paper: they may still change after
     * submission, until the item is approved. The owning analyst and the review analyst assigned to the
     * item may change them while the meeting is open. The item keeps its status; each change is recorded
     * in the item's audit trail and, once submitted, mailed to the reviewer and secretary.
     */
    @Override
    public boolean canChangeSchedule(int agiId, String userId, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null || !item.isActive() || !SCHEDULE_OPEN_STATUSES.contains(item.getItemStatus())) {
            return false;
        }
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        if (meeting == null || !(MEETING_DRAFT.equals(meeting.getStatus()) || MEETING_COMPILED.equals(meeting.getStatus()))) {
            return false;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        return isOwner(item, userId) || (roles.isReviewer() && sameUser(item.getReviewAnalystId(), userId));
    }
```
```java
    /**
     * What happened at the meeting - decisions, attendance and conditions - is the secretariat's
     * record, not the analyst's. Only the secretary or chair of that committee may write it. The
     * owning analyst and the review analyst can read it (see {@link #canViewItem}) but never
     * change it, so the minute of a meeting cannot be edited by the person whose paper it was.
     */
    @Override
    public boolean canCaptureOutcome(int agiId, String userId, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null) {
            return false;
        }
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        return (roles.isSecretary() && servesAsSecretary(committee, userId))
                || (roles.isChairperson() && servesAsChair(committee, userId));
    }
```
```java
    @Override
    public boolean canReassignReviewer(int agiId, String user, AgendaAccess access) {
        ApprovalForumAgiAgendaItemEntity item = agendaRepository.getItem(agiId);
        if (item == null || !item.isActive() || !STATUS_SUBMITTED.equals(item.getItemStatus())) return false;
        ApprovalForumAgmAgendaMeetingEntity meeting = item.getAgmAgendaMeetingByAgmId();
        if (meeting == null || (!MEETING_DRAFT.equals(meeting.getStatus()) && !MEETING_COMPILED.equals(meeting.getStatus()))) return false;
        AgendaAccess roles = access == null ? AgendaAccess.none() : access;
        ApprovalForumAgcAgendaCommitteeEntity committee = committeeOf(item);
        return (roles.isSecretary() && servesAsSecretary(committee, user))
                || (roles.isChairperson() && servesAsChair(committee, user));
    }
```

Source: `api/src/main/java/za/co/rmb/approvalforum/service/AgendaAccess.java`. SHA-256: `1f7db0c18ff85c75be19390f25bddeeb4ce14ca30c4b7d1a51c573c92d84e9b8`.

```java
package za.co.rmb.approvalforum.service;

/**
 * The DAF roles the caller holds, passed from the web layer into the agenda service so the
 * visibility rules (BR-024) live in one place and cannot be bypassed by calling a different
 * endpoint.
 *
 * <p>The roles themselves come from the AD groups mapped in
 * {@code ApprovalForumAuthResource.getAFGroups}. This type carries only the answer to "does the
 * caller hold this role", never the AD group names, so the policy stays independent of the
 * environment-specific group naming.</p>
 *
 * <p>Holding a role is never sufficient on its own: an oversight role is additionally scoped to
 * the committee the caller serves, or - for an approver - to items that have been submitted.
 * Ownership is what grants the right to change an item.</p>
 */
public class AgendaAccess {

    private final boolean secretary;
    private final boolean chairperson;
    private final boolean reviewer;
    private final boolean approver;

    public AgendaAccess(boolean secretary, boolean chairperson, boolean reviewer, boolean approver) {
        this.secretary = secretary;
        this.chairperson = chairperson;
        this.reviewer = reviewer;
        this.approver = approver;
    }

    /** No oversight at all - an analyst sees only what they own. */
    public static AgendaAccess none() {
        return new AgendaAccess(false, false, false, false);
    }

    public boolean isSecretary() {
        return secretary;
    }

    public boolean isChairperson() {
        return chairperson;
    }

    public boolean isReviewer() {
        return reviewer;
    }

    public boolean isApprover() {
        return approver;
    }

    /** True when the caller holds at least one role that can check another analyst's item. */
    public boolean hasAnyOversight() {
        return secretary || chairperson || reviewer || approver;
    }

    @Override
    public String toString() {
        return "AgendaAccess{secretary=" + secretary + ", chairperson=" + chairperson
                + ", reviewer=" + reviewer + ", approver=" + approver + '}';
    }
}
```
