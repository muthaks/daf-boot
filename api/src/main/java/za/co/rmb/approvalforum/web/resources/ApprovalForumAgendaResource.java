package za.co.rmb.approvalforum.web.resources;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.apache.commons.io.IOUtils;
import za.co.rmb.credit.daf.jaxrs.multipart.InputPart;
import za.co.rmb.credit.daf.jaxrs.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.agenda.AgendaCommitteeDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaConditionDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDocumentDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingRecordDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaParticipantDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaPostMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaBookingDraftDTO;
import za.co.rmb.approvalforum.model.agenda.BookAgendaItemRequest;
import za.co.rmb.approvalforum.model.agenda.MoveAgendaItemRequest;
import za.co.rmb.approvalforum.model.agenda.QuorumExceptionRequest;
import za.co.rmb.approvalforum.model.agenda.ReassignAgendaReviewerRequest;
import za.co.rmb.approvalforum.service.AgendaAccess;
import za.co.rmb.approvalforum.model.CounterpartyResponseDTO;
import za.co.rmb.approvalforum.service.AgendaService;
import za.co.rmb.approvalforum.service.ApprovalForumExternalService;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;
import za.co.rmb.approvalforum.web.casdm.CasdmRestClient;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static za.co.rmb.approvalforum.security.util.SecurityHelper.tokenValidation;

@Api(tags = "agenda")
@Path("/approvalForum/agenda")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumAgendaResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumAgendaResource.class);

    @Context
    private SecurityContext context;

    @Inject
    private AgendaService agendaService;

    @Inject
    private ApprovalForumListManagementService approvalForumListManagementService;

    @Inject
    private CasdmRestClient casdmRestClient;

    @Inject
    private ApprovalForumExternalService approvalForumExternalService;

    @Inject
    @Named("af_settings")
    private Map<String, String> afSettings;

    /** Business-rule violations (state machine, double-booked slots) surface as 409 + message. */
    private Response conflict(IllegalStateException e) {
        logger.warn("Agenda business rule rejected the request: {}", e.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .entity(java.util.Collections.singletonMap("message", e.getMessage())).build();
    }

    // ---- Role-based access (BR-024, RULE-008). DAF roles come from the AD groups mapped in
    // ApprovalForumAuthResource.getAFGroups: CreditAnalyst, ReviewAnalyst, Approver, Chairperson
    // and Secretary (DL_/FL_/G_DAF_<env>_Secretary - the committee secretariat). ----

    static final String ROLE_SECRETARY = "Secretary";
    static final String ROLE_CHAIRPERSON = "Chairperson";
    static final String ROLE_APPROVER = "Approver";
    static final String ROLE_CREDIT_ANALYST = "CreditAnalyst";

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

    private String reviewerRole() {
        String role = afSettings.get("af.agenda.reviewer.role");
        return role == null || role.trim().isEmpty() ? "ReviewAnalyst" : role.trim();
    }

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

    /** A role only confers oversight while it is listed in `af.agenda.oversight.roles`. */
    private boolean isOversightRole(java.util.List<String> configured, String role) {
        for (String candidate : configured) {
            if (candidate != null && candidate.trim().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

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

    private Response forbidden(String what, int agiId, String message) {
        logger.warn("Agenda: {} refused for {} on agenda item {}", what, userName(), agiId);
        return Response.status(Response.Status.FORBIDDEN)
                .entity(java.util.Collections.singletonMap("message", message)).build();
    }

    /** 403 unless the caller owns this item - the booking analyst or the named primary analyst. */
    private Response forbiddenUnlessOwner(String what, int agiId) {
        if (agendaService.canEditItem(agiId, userName())) {
            return null;
        }
        return forbidden(what, agiId, "Only the analyst who booked agenda item " + agiId
                + ", or the primary analyst named on it, can change it.");
    }

    /** 403 unless the caller may open this item at all (owner, assigned reviewer, secretariat, approver). */
    private Response forbiddenUnlessCanView(String what, int agiId) {
        if (agendaService.canViewItem(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Agenda item " + agiId + " belongs to another analyst.");
    }

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

    // Committee slots are booked ahead of submission, so unlike capture (which only offers
    // counterparties with an application at Submitted for Approval - the state it can process),
    // the agenda defaults to every ACTIVE application status. Terminal/dead states (Withdrawn,
    // Declined, Archived, Deleted, Managed, Changes Made, PreviouslyApproved) are excluded.
    // Display names as seeded in CASDM's credit_application_status_type reference data.
    private static final String AGENDA_DEFAULT_STATUSES =
            "Work in Progress;Submitted for Review;CA Referred Back;CA Reviewed;Conditional Review;"
            + "Submitted for Approval;Approved;Conditionally Approved";

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

    @GET
    @Path("/committees")
    @Produces("application/json")
    @ApiOperation(value = "List agenda committees and levels", authorizations = @Authorization(value = "Bearer"))
    public Response getCommittees() {
        logger.info("getCommittees()");
        tokenValidation(context);
        return Response.ok(agendaService.getCommittees()).build();
    }

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
        try {
            return Response.ok(agendaService.updateCommittee(committee, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        return Response.ok(agendaService.deactivateCommittee(agcId, userName())).build();
    }

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
        try {
            return Response.ok(agendaService.createMeeting(meeting, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        meeting.setAgmId(agmId);
        try {
            return Response.ok(agendaService.updateMeeting(meeting, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        try {
            return Response.ok(agendaService.cancelMeeting(agmId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/reinstate")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Reinstate a cancelled meeting (secretary/chair only)", authorizations = @Authorization(value = "Bearer"))
    public Response reinstateMeeting(@PathParam("agmId") int agmId, @QueryParam("reason") String reason) {
        tokenValidation(context);
        Response denied = forbiddenUnless("reinstate meetings", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.reinstateMeeting(agmId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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

    @GET
    @Path("/committees/{agcId}/dates")
    @Produces("application/json")
    @ApiOperation(value = "Available meeting dates for a committee (next 3 weeks)", authorizations = @Authorization(value = "Bearer"))
    public Response getAvailableDates(@PathParam("agcId") int agcId) {
        logger.info("getAvailableDates({})", agcId);
        tokenValidation(context);
        return Response.ok(agendaService.getAvailableMeetingDates(agcId)).build();
    }

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

    @GET
    @Path("/committees/{agcId}/meetings")
    @Produces("application/json")
    @ApiOperation(value = "Meetings for a committee", authorizations = @Authorization(value = "Bearer"))
    public Response getMeetings(@PathParam("agcId") int agcId) {
        logger.info("getMeetings({})", agcId);
        tokenValidation(context);
        return Response.ok(agendaService.getMeetingsForCommittee(agcId)).build();
    }

    @GET
    @Path("/meetings/{agmId}")
    @Produces("application/json")
    @ApiOperation(value = "Meeting with its agenda items", authorizations = @Authorization(value = "Bearer"))
    public Response getMeeting(@PathParam("agmId") int agmId) {
        logger.info("getMeeting({})", agmId);
        tokenValidation(context);
        return Response.ok(agendaService.getMeeting(agmId)).build();
    }

    @POST
    @Path("/meetings/{agmId}/compile")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Compile the agenda (assign item numbers)", authorizations = @Authorization(value = "Bearer"))
    public Response compileAgenda(@PathParam("agmId") int agmId) {
        logger.info("compileAgenda({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("compile the agenda", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.compileAgenda(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/reschedule")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Auto-adjust discussion slots back-to-back to clear time clashes",
            authorizations = @Authorization(value = "Bearer"))
    public Response rescheduleAgenda(@PathParam("agmId") int agmId) {
        logger.info("rescheduleAgenda({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("re-sequence the agenda", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.rescheduleAgenda(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/invitation")
    @Produces("application/json")
    @Transactional
    public Response sendInvitation(@PathParam("agmId") int agmId) {
        tokenValidation(context);
        Response denied = forbiddenUnless("send the calendar invitation", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.sendInvitation(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/invitation/update")
    @Produces("application/json")
    @Transactional
    public Response sendInvitationUpdate(@PathParam("agmId") int agmId) {
        tokenValidation(context);
        Response denied = forbiddenUnless("send the updated calendar invitation", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.sendInvitationUpdate(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/lock")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Lock the meeting agenda for editing", authorizations = @Authorization(value = "Bearer"))
    public Response lockMeeting(@PathParam("agmId") int agmId) {
        logger.info("lockMeeting({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("lock the agenda", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.lockMeeting(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/status")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Set meeting status", authorizations = @Authorization(value = "Bearer"))
    public Response setMeetingStatus(@PathParam("agmId") int agmId, @QueryParam("status") String status) {
        logger.info("setMeetingStatus({}, {})", agmId, status);
        tokenValidation(context);
        Response denied = forbiddenUnless("change the meeting status", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.setMeetingStatus(agmId, status, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/held")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Mark the meeting as held (unlocks post-meeting capture)",
            authorizations = @Authorization(value = "Bearer"))
    public Response markMeetingHeld(@PathParam("agmId") int agmId,
                                    @QueryParam("actualStartTime") String actualStartTime,
                                    @QueryParam("actualEndTime") String actualEndTime,
                                    @QueryParam("quorumAchieved") Boolean quorumAchieved) {
        logger.info("markMeetingHeld({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("mark the meeting held", ROLE_SECRETARY, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.markMeetingHeld(agmId, actualStartTime, actualEndTime, quorumAchieved, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        try {
            return Response.ok(agendaService.closeMeeting(agmId, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/meetings/{agmId}/reopen")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Re-open a closed meeting record through an audited correction (reviewers only)",
            authorizations = @Authorization(value = "Bearer"))
    public Response reopenMeeting(@PathParam("agmId") int agmId, @QueryParam("reason") String reason) {
        logger.info("reopenMeeting({})", agmId);
        tokenValidation(context);
        Response denied = forbiddenUnless("re-open a closed meeting record", reviewerRole());
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.reopenMeeting(agmId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        record.setAgmId(agmId);
        try {
            return Response.ok(agendaService.addMeetingRecord(record, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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

    @GET
    @Path("/items/my")
    @Produces("application/json")
    @ApiOperation(value = "Agenda items for the current user", authorizations = @Authorization(value = "Bearer"))
    public Response getMyItems() {
        logger.info("getMyItems()");
        tokenValidation(context);
        return Response.ok(agendaService.getItemsForUser(userName())).build();
    }

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

    /** 403 unless the caller may move this item or change its roster (owner or assigned reviewer, before approval). */
    private Response forbiddenUnlessCanChangeSchedule(String what, int agiId) {
        if (agendaService.canChangeSchedule(agiId, userName(), access())) {
            return null;
        }
        return forbidden(what, agiId, "Only the analyst who owns agenda item " + agiId + ", or its review analyst, can "
                + what + " - and only before the item is approved, while its meeting is open.");
    }

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

    private Response draftNotFound(IllegalArgumentException e) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(java.util.Collections.singletonMap("message", e.getMessage())).build();
    }

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

    @GET
    @Path("/documents/{agidId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Download an agenda item credit paper", authorizations = @Authorization(value = "Bearer"))
    public Response downloadDocument(@PathParam("agidId") int agidId) {
        logger.info("downloadDocument({})", agidId);
        tokenValidation(context);

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

    @POST
    @Path("/items/{agiId}/postMeeting/unlock")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Unlock a signed-off post-meeting record (reviewers only)",
            authorizations = @Authorization(value = "Bearer"))
    public Response unlockPostMeeting(@PathParam("agiId") int agiId) {
        logger.info("unlockPostMeeting({})", agiId);
        tokenValidation(context);
        String reviewerRole = afSettings.get("af.agenda.reviewer.role");
        if (reviewerRole == null || reviewerRole.trim().isEmpty() || !context.isUserInRole(reviewerRole.trim())) {
            return Response.status(Response.Status.FORBIDDEN.getStatusCode(),
                    "Only reviewers may unlock a signed-off post-meeting record").build();
        }
        return Response.ok(agendaService.unlockPostMeeting(agiId, userName())).build();
    }

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
        try {
            return Response.ok(agendaService.returnItem(agiId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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

    @POST
    @Path("/items/{agiId}/approve")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Approve a submitted item onto the agenda (Approver/Chair)",
            authorizations = @Authorization(value = "Bearer"))
    public Response approveItem(@PathParam("agiId") int agiId, @QueryParam("comments") String comments) {
        logger.info("approveItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("approve agenda items", ROLE_APPROVER, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.approveItem(agiId, comments, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

    @POST
    @Path("/items/{agiId}/reject")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Reject a submitted item with a reason (Approver/Chair)",
            authorizations = @Authorization(value = "Bearer"))
    public Response rejectItem(@PathParam("agiId") int agiId, @QueryParam("reason") String reason) {
        logger.info("rejectItem({})", agiId);
        tokenValidation(context);
        Response denied = forbiddenUnless("reject agenda items", ROLE_APPROVER, ROLE_CHAIRPERSON);
        if (denied != null) return denied;
        try {
            return Response.ok(agendaService.rejectItem(agiId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        try {
            return Response.ok(agendaService.reopenItem(agiId, reason, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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

    @POST
    @Path("/conditions/{agcaId}/close")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Close or waive a condition with closure comments", authorizations = @Authorization(value = "Bearer"))
    public Response closeCondition(@PathParam("agcaId") int agcaId, @QueryParam("status") String status,
                                   @QueryParam("comments") String comments) {
        logger.info("closeCondition({}, {})", agcaId, status);
        tokenValidation(context);
        try {
            return Response.ok(agendaService.closeCondition(agcaId, status, comments, userName())).build();
        } catch (IllegalStateException e) {
            return conflict(e);
        }
    }

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
        return Response.ok(agendaService.getReportSummary(range[0], range[1])).build();
    }

    @POST
    @Path("/processCutOff")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Remove unconfirmed items on meetings past their cut-off (scheduler)",
            authorizations = @Authorization(value = "Bearer"))
    public Response processCutOff() {
        logger.info("processCutOff()");
        tokenValidation(context);
        return Response.ok(agendaService.processCutOffs()).build();
    }

    @POST
    @Path("/processReminders")
    @Produces("application/json")
    @Transactional
    @ApiOperation(value = "Send upload reminders for outstanding credit papers approaching cut-off (scheduler)",
            authorizations = @Authorization(value = "Bearer"))
    public Response processReminders() {
        logger.info("processReminders()");
        tokenValidation(context);
        return Response.ok(agendaService.processReminders()).build();
    }

    private String userName() {
        if (context != null && context.getUserPrincipal() != null) {
            return context.getUserPrincipal().getName();
        }
        return "SYSTEM";
    }

    private String getFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                return name[1].trim().replaceAll("\"", "");
            }
        }
        return "unknown";
    }

}
