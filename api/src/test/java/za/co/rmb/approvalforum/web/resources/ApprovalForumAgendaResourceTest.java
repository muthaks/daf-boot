package za.co.rmb.approvalforum.web.resources;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Transactional;
import za.co.rmb.approvalforum.model.CounterpartyModel;
import za.co.rmb.approvalforum.model.CounterpartyResponseDTO;
import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.UsersResponse;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.AgendaAccess;
import za.co.rmb.approvalforum.service.AgendaService;
import za.co.rmb.approvalforum.service.ApprovalForumExternalService;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;
import za.co.rmb.approvalforum.service.exception.ExternalResourceException;
import za.co.rmb.approvalforum.web.casdm.CasdmRestClient;
import za.co.rmb.credit.daf.jaxrs.multipart.InputPart;
import za.co.rmb.credit.daf.jaxrs.multipart.MultipartFormDataInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * The agenda REST layer on its own: every endpoint is called directly with a mocked service, so these
 * checks cover delegation, argument mapping, the HTTP status and body shape, the role gates (the DAF
 * roles the caller holds come from the security context) and the ownership / visibility gates.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalForumAgendaResourceTest {

    private static final String USER = "analyst@example.test";
    private static final String SECRETARY = "Secretary";
    private static final String CHAIR = "Chairperson";
    private static final String APPROVER = "Approver";
    private static final String ANALYST = "CreditAnalyst";
    private static final String REVIEWER = "ReviewAnalyst";

    @Mock
    private AgendaService agendaService;
    @Mock
    private ApprovalForumListManagementService listManagementService;
    @Mock
    private CasdmRestClient casdmRestClient;
    @Mock
    private ApprovalForumExternalService externalService;
    @Mock
    private SecurityContext context;
    @Mock
    private Principal principal;

    private final Map<String, String> settings = new HashMap<>();
    private final Set<String> roles = new HashSet<>();
    private ApprovalForumAgendaResource resource;

    @BeforeEach
    void setUp() {
        resource = new ApprovalForumAgendaResource();
        inject("context", context);
        inject("agendaService", agendaService);
        inject("approvalForumListManagementService", listManagementService);
        inject("casdmRestClient", casdmRestClient);
        inject("approvalForumExternalService", externalService);
        inject("afSettings", settings);
        when(context.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(USER);
        when(context.isUserInRole(anyString())).thenAnswer(inv -> roles.contains(inv.<String>getArgument(0)));
    }

    /** The resource is container-injected through private fields; the test wires them the same way. */
    private void inject(String field, Object value) {
        try {
            Field f = ApprovalForumAgendaResource.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(resource, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void roles(String... held) {
        roles.clear();
        roles.addAll(Arrays.asList(held));
    }

    private static String message(Response response) {
        assertTrue(response.getEntity() instanceof Map, "error responses carry a {message} body");
        return String.valueOf(((Map<?, ?>) response.getEntity()).get("message"));
    }

    private static void assertForbidden(Response response, String... words) {
        assertEquals(403, response.getStatus());
        for (String word : words) {
            assertTrue(message(response).contains(word), message(response));
        }
    }

    private static void assertConflict(Response response, String text) {
        assertEquals(409, response.getStatus());
        assertEquals(text, message(response));
    }

    private static AgendaItemDTO item(int agiId) {
        AgendaItemDTO item = new AgendaItemDTO();
        item.setAgiId(agiId);
        return item;
    }

    private static AgendaMeetingDTO meeting(int agmId) {
        AgendaMeetingDTO meeting = new AgendaMeetingDTO();
        meeting.setAgmId(agmId);
        return meeting;
    }

    // ---- Token validation and the caller's identity ----

    @Test
    @DisplayName("Every endpoint refuses a request without a security context or principal")
    void endpointsNeedAToken() {
        inject("context", null);
        assertThrows(NotAuthorizedException.class, () -> resource.getCommittees());
        inject("context", context);
        when(context.getUserPrincipal()).thenReturn(null);
        assertThrows(NotAuthorizedException.class, () -> resource.getCommittees());
        assertThrows(NotAuthorizedException.class, () -> resource.getItem(1));
        assertThrows(NotAuthorizedException.class, () -> resource.approveItem(1, "ok"));
        verifyNoInteractions(agendaService);
    }

    // ---- Lookups delegated straight through ----

    @Test
    void directoryAndListLookupsDelegate() {
        UsersResponse users = new UsersResponse();
        when(listManagementService.getAllUsers()).thenReturn(users);
        when(listManagementService.getAllListNames()).thenReturn(Arrays.asList("CCC", "ICC"));
        assertSame(users, resource.getUsers().getEntity());
        assertEquals(Arrays.asList("CCC", "ICC"), resource.getAvailableLists().getEntity());
    }

    @Test
    void casdmLookupDelegatesTheInkombaId() {
        AgendaCasdmApplicationDTO application = new AgendaCasdmApplicationDTO();
        when(casdmRestClient.getLatestApplicationByInkomba("INK-1")).thenReturn(application);
        Response response = resource.getCasdmApplication("INK-1");
        assertEquals(200, response.getStatus());
        assertSame(application, response.getEntity());
    }

    @Test
    @DisplayName("Counterparties default to every active application status and narrow through af.agenda.application.statuses")
    void counterpartiesUseDefaultOrConfiguredStatuses() throws Exception {
        CounterpartyModel model = new CounterpartyModel();
        when(externalService.getCounterparties(eq(true), eq(1), any())).thenReturn(Collections.singletonList(model));
        Response response = resource.getAgendaCounterparties(1);
        assertEquals(200, response.getStatus());
        assertEquals(Collections.singletonList(model), ((CounterpartyResponseDTO) response.getEntity()).getCounterpartyDetail());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> statuses = ArgumentCaptor.forClass(List.class);
        verify(externalService).getCounterparties(eq(true), eq(1), statuses.capture());
        assertEquals(Arrays.asList("Work in Progress", "Submitted for Review", "CA Referred Back", "CA Reviewed",
                "Conditional Review", "Submitted for Approval", "Approved", "Conditionally Approved"), statuses.getValue());

        settings.put("af.agenda.application.statuses", "Approved;Conditionally Approved");
        when(externalService.getCounterparties(eq(true), eq(2), any())).thenReturn(Collections.emptyList());
        resource.getAgendaCounterparties(2);
        verify(externalService).getCounterparties(true, 2, Arrays.asList("Approved", "Conditionally Approved"));
    }

    @Test
    void counterpartyFeedFailureIsA500WithTheReason() throws Exception {
        when(externalService.getCounterparties(eq(true), anyInt(), any())).thenThrow(new ExternalResourceException("creditdata down"));
        Response response = resource.getAgendaCounterparties(1);
        assertEquals(500, response.getStatus());
        assertEquals("creditdata down", response.getStatusInfo().getReasonPhrase());
    }

    // ---- Committees ----

    @Test
    void committeeReadsAreOpenToEveryAuthenticatedUser() {
        List<AgendaCommitteeDTO> committees = Collections.singletonList(new AgendaCommitteeDTO());
        when(agendaService.getCommittees()).thenReturn(committees);
        when(agendaService.getAvailableMeetingDates(1)).thenReturn(Collections.singletonList(Timestamp.valueOf("2026-11-03 00:00:00")));
        AgendaItemDTO roster = item(9);
        when(agendaService.getLastRoster(1)).thenReturn(roster);
        ListManagementList list = new ListManagementList();
        when(agendaService.getCommitteeMembers(1)).thenReturn(list);
        List<AgendaMeetingDTO> meetings = Collections.singletonList(meeting(10));
        when(agendaService.getMeetingsForCommittee(1)).thenReturn(meetings);

        assertSame(committees, resource.getCommittees().getEntity());
        assertEquals(Collections.singletonList(Timestamp.valueOf("2026-11-03 00:00:00")), resource.getAvailableDates(1).getEntity());
        assertSame(roster, resource.getLastRoster(1).getEntity());
        assertSame(list, resource.getCommitteeMembers(1).getEntity());
        assertSame(meetings, resource.getMeetings(1).getEntity());
    }

    @Test
    @DisplayName("Committee administration needs Secretary or Chairperson; each role alone is enough")
    void committeeAdministrationIsSecretariatOnly() {
        AgendaCommitteeDTO committee = new AgendaCommitteeDTO();
        committee.setAgcId(1);
        committee.setCommitteeName("CCC");
        roles(ANALYST, APPROVER, REVIEWER);
        assertForbidden(resource.updateCommittee(committee), "Secretary or Chairperson", "update committee configuration");
        assertForbidden(resource.getAllCommittees(), "administer committees");
        assertForbidden(resource.createCommittee(committee), "create committees");
        assertForbidden(resource.deactivateCommittee(1), "deactivate committees");
        verifyNoInteractions(agendaService);

        roles(SECRETARY);
        when(agendaService.decidesForCommittee(eq(1), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.updateCommittee(committee), "Only the secretary or chair of this committee may update committee configuration.");
        assertForbidden(resource.deactivateCommittee(1), "Only the secretary or chair of this committee may deactivate committees.");
        verify(agendaService, never()).updateCommittee(any(), any());
        verify(agendaService, never()).deactivateCommittee(anyInt(), any());

        when(agendaService.decidesForCommittee(eq(1), eq(USER), any())).thenReturn(true);
        for (String role : new String[] { SECRETARY, CHAIR }) {
            roles(role);
            when(agendaService.updateCommittee(committee, USER)).thenReturn(committee);
            when(agendaService.getAllCommittees()).thenReturn(Collections.singletonList(committee));
            when(agendaService.createCommittee(committee, USER)).thenReturn(committee);
            when(agendaService.deactivateCommittee(1, USER)).thenReturn(committee);
            assertSame(committee, resource.updateCommittee(committee).getEntity());
            assertEquals(Collections.singletonList(committee), resource.getAllCommittees().getEntity());
            assertSame(committee, resource.createCommittee(committee).getEntity());
            assertSame(committee, resource.deactivateCommittee(1).getEntity());
        }
    }

    @Test
    void committeeRuleViolationsAre409() {
        roles(SECRETARY);
        when(agendaService.decidesForCommittee(anyInt(), eq(USER), any())).thenReturn(true);
        AgendaCommitteeDTO committee = new AgendaCommitteeDTO();
        when(agendaService.updateCommittee(any(), eq(USER))).thenThrow(new IllegalStateException("Cut-off time must be HH:mm"));
        when(agendaService.createCommittee(any(), eq(USER))).thenThrow(new IllegalStateException("Committee name is required"));
        assertConflict(resource.updateCommittee(committee), "Cut-off time must be HH:mm");
        assertConflict(resource.createCommittee(committee), "Committee name is required");
    }

    // ---- Meetings ----

    @Test
    void meetingAdministrationIsSecretariatOnly() {
        roles(ANALYST, APPROVER, REVIEWER);
        AgendaMeetingDTO meeting = meeting(0);
        meeting.setAgcId(1);
        assertForbidden(resource.createMeeting(meeting), "schedule meetings");
        assertForbidden(resource.updateMeeting(10, meeting), "update meetings");
        assertForbidden(resource.cancelMeeting(10, "No quorum"), "cancel meetings");
        assertForbidden(resource.reinstateMeeting(10, "Back on"), "reinstate meetings");
        assertForbidden(resource.sendInvitation(10), "send the calendar invitation");
        assertForbidden(resource.sendInvitationUpdate(10), "send the updated calendar invitation");
        assertForbidden(resource.markMeetingHeld(10, null, null, null), "mark the meeting held");
        assertForbidden(resource.closeMeeting(10), "close the meeting record");
        assertForbidden(resource.reopenMeeting(10, "Fix"), "re-open a closed meeting record");
        assertForbidden(resource.addMeetingRecord(10, new AgendaMeetingRecordDTO()), "attach meeting records");
        verifyNoInteractions(agendaService);
    }

    @Test
    @DisplayName("Meeting edits map the path id onto the body and delegate with the caller's name")
    void meetingEditsDelegate() {
        roles(CHAIR);
        when(agendaService.decidesForCommittee(eq(1), eq(USER), any())).thenReturn(true);
        when(agendaService.decidesForMeeting(eq(10), eq(USER), any())).thenReturn(true);
        AgendaMeetingDTO body = meeting(0);
        body.setAgcId(1);
        AgendaMeetingDTO saved = meeting(10);
        when(agendaService.createMeeting(body, USER)).thenReturn(saved);
        when(agendaService.updateMeeting(body, USER)).thenReturn(saved);
        when(agendaService.cancelMeeting(10, "No quorum", USER)).thenReturn(saved);
        when(agendaService.reinstateMeeting(10, "Back on", USER)).thenReturn(saved);
        when(agendaService.sendInvitation(10, USER)).thenReturn(saved);
        when(agendaService.sendInvitationUpdate(10, USER)).thenReturn(saved);
        when(agendaService.markMeetingHeld(10, "09:05", "11:40", Boolean.TRUE, USER)).thenReturn(saved);
        when(agendaService.closeMeeting(10, USER)).thenReturn(saved);
        when(agendaService.reopenMeeting(10, "Fix", USER)).thenReturn(saved);
        when(agendaService.getMeetingFor(eq(10), eq(USER), any())).thenReturn(saved);

        assertSame(saved, resource.createMeeting(body).getEntity());
        assertSame(saved, resource.updateMeeting(10, body).getEntity());
        assertEquals(10, body.getAgmId(), "the path id wins over whatever the body carried");
        assertSame(saved, resource.cancelMeeting(10, "No quorum").getEntity());
        assertSame(saved, resource.reinstateMeeting(10, "Back on").getEntity());
        assertSame(saved, resource.sendInvitation(10).getEntity());
        assertSame(saved, resource.sendInvitationUpdate(10).getEntity());
        assertSame(saved, resource.markMeetingHeld(10, "09:05", "11:40", Boolean.TRUE).getEntity());
        assertSame(saved, resource.closeMeeting(10).getEntity());
        assertSame(saved, resource.reopenMeeting(10, "Fix").getEntity());
        assertSame(saved, resource.getMeeting(10).getEntity());
    }

    @Test
    @DisplayName("A secretary or chair acts only on the meetings of the committee they serve")
    void meetingActionsAreScopedToTheCallersCommittee() {
        roles(SECRETARY, CHAIR);
        AgendaMeetingDTO body = meeting(0);
        body.setAgcId(1);
        when(agendaService.decidesForCommittee(eq(1), eq(USER), any())).thenReturn(false);
        when(agendaService.decidesForMeeting(eq(10), eq(USER), any())).thenReturn(false);
        String committee = "Only the secretary or chair of this committee may schedule meetings.";
        String meeting = "Only the secretary or chair of this meeting's committee may ";
        assertForbidden(resource.createMeeting(body), committee);
        assertForbidden(resource.updateMeeting(10, body), meeting + "update meetings.");
        assertForbidden(resource.cancelMeeting(10, "x"), meeting + "cancel meetings.");
        assertForbidden(resource.reinstateMeeting(10, "x"), meeting + "reinstate meetings.");
        assertForbidden(resource.sendInvitation(10), meeting + "send the calendar invitation.");
        assertForbidden(resource.sendInvitationUpdate(10), meeting + "send the updated calendar invitation.");
        assertForbidden(resource.markMeetingHeld(10, null, null, null), meeting + "mark the meeting held.");
        assertForbidden(resource.closeMeeting(10), meeting + "close the meeting record.");
        assertForbidden(resource.reopenMeeting(10, "x"), meeting + "re-open a closed meeting record.");
        assertForbidden(resource.addMeetingRecord(10, new AgendaMeetingRecordDTO()), meeting + "attach meeting records.");
        verify(agendaService, never()).createMeeting(any(), any());
        verify(agendaService, never()).updateMeeting(any(), any());
        verify(agendaService, never()).cancelMeeting(anyInt(), any(), any());
        verify(agendaService, never()).reinstateMeeting(anyInt(), any(), any());
        verify(agendaService, never()).sendInvitation(anyInt(), any());
        verify(agendaService, never()).sendInvitationUpdate(anyInt(), any());
        verify(agendaService, never()).markMeetingHeld(anyInt(), any(), any(), any(), any());
        verify(agendaService, never()).closeMeeting(anyInt(), any());
        verify(agendaService, never()).reopenMeeting(anyInt(), any(), any());
        verify(agendaService, never()).addMeetingRecord(any(), any());
    }

    @Test
    @DisplayName("Meeting detail is answered for the caller: the service decides which items they may open")
    void meetingDetailIsFilteredForTheCaller() {
        roles(ANALYST);
        AgendaMeetingDTO visible = meeting(10);
        when(agendaService.getMeetingFor(eq(10), eq(USER), access(false, false, false, false))).thenReturn(visible);
        assertSame(visible, resource.getMeeting(10).getEntity());
        verify(agendaService, never()).getMeeting(anyInt());
    }

    @Test
    void meetingRuleViolationsAre409() {
        roles(SECRETARY);
        when(agendaService.decidesForCommittee(anyInt(), eq(USER), any())).thenReturn(true);
        when(agendaService.decidesForMeeting(eq(10), eq(USER), any())).thenReturn(true);
        when(agendaService.createMeeting(any(), eq(USER))).thenThrow(new IllegalStateException("Meeting date is required"));
        when(agendaService.updateMeeting(any(), eq(USER))).thenThrow(new IllegalStateException("A meeting cannot be in the past"));
        when(agendaService.cancelMeeting(10, "x", USER)).thenThrow(new IllegalStateException("has been held"));
        when(agendaService.reinstateMeeting(10, "x", USER)).thenThrow(new IllegalStateException("Only a cancelled meeting"));
        when(agendaService.sendInvitation(10, USER)).thenThrow(new IllegalStateException("Add attendees"));
        when(agendaService.sendInvitationUpdate(10, USER)).thenThrow(new IllegalStateException("Send the invitations before"));
        when(agendaService.markMeetingHeld(10, null, null, null, USER)).thenThrow(new IllegalStateException("before its scheduled end"));
        when(agendaService.closeMeeting(10, USER)).thenThrow(new IllegalStateException("must be Held"));
        when(agendaService.reopenMeeting(10, "x", USER)).thenThrow(new IllegalStateException("is not closed"));
        when(agendaService.addMeetingRecord(any(), eq(USER))).thenThrow(new IllegalStateException("Record type is required"));
        assertConflict(resource.createMeeting(meeting(0)), "Meeting date is required");
        assertConflict(resource.updateMeeting(10, meeting(0)), "A meeting cannot be in the past");
        assertConflict(resource.cancelMeeting(10, "x"), "has been held");
        assertConflict(resource.reinstateMeeting(10, "x"), "Only a cancelled meeting");
        assertConflict(resource.sendInvitation(10), "Add attendees");
        assertConflict(resource.sendInvitationUpdate(10), "Send the invitations before");
        assertConflict(resource.markMeetingHeld(10, null, null, null), "before its scheduled end");
        assertConflict(resource.closeMeeting(10), "must be Held");
        assertConflict(resource.reopenMeeting(10, "x"), "is not closed");
        assertConflict(resource.addMeetingRecord(10, new AgendaMeetingRecordDTO()), "Record type is required");
    }

    @Test
    @DisplayName("The calendar range parses ISO dates inclusively and defaults to 90 days either side of today")
    void meetingsBetweenParsesTheRange() {
        when(agendaService.getMeetingsBetween(any(), any())).thenReturn(Collections.emptyList());
        resource.getMeetingsBetween("2026-10-01", "2026-10-31");
        verify(agendaService).getMeetingsBetween(Timestamp.valueOf("2026-10-01 00:00:00"), Timestamp.valueOf("2026-10-31 23:59:59"));

        LocalDate today = LocalDate.now();
        resource.getMeetingsBetween(null, " ");
        verify(agendaService).getMeetingsBetween(Timestamp.valueOf(today.minusDays(90).atStartOfDay()),
                Timestamp.valueOf(today.plusDays(90).atTime(23, 59, 59)));

        roles(APPROVER);
        when(agendaService.getReportSummary(any(), any(), eq(USER), any())).thenReturn(new AgendaReportSummaryDTO());
        resource.getReportSummary(" 2026-01-01 ", "2026-01-31");
        verify(agendaService).getReportSummary(eq(Timestamp.valueOf("2026-01-01 00:00:00")), eq(Timestamp.valueOf("2026-01-31 23:59:59")),
                eq(USER), access(false, false, false, true));
    }

    @Test
    void malformedDatesAreRejectedBeforeTheServiceIsCalled() {
        assertThrows(java.time.format.DateTimeParseException.class, () -> resource.getMeetingsBetween("31/10/2026", null));
        verifyNoInteractions(agendaService);
    }

    @Test
    void meetingRecordsReadForAnyoneAndAttachForTheSecretariat() {
        List<AgendaMeetingRecordDTO> records = Collections.singletonList(new AgendaMeetingRecordDTO());
        when(agendaService.getMeetingRecords(10)).thenReturn(records);
        assertSame(records, resource.getMeetingRecords(10).getEntity());

        roles(SECRETARY);
        when(agendaService.decidesForMeeting(eq(10), eq(USER), any())).thenReturn(true);
        AgendaMeetingRecordDTO record = new AgendaMeetingRecordDTO();
        record.setAgmId(99);
        when(agendaService.addMeetingRecord(record, USER)).thenReturn(record);
        assertSame(record, resource.addMeetingRecord(10, record).getEntity());
        assertEquals(10, record.getAgmId(), "the record belongs to the meeting in the path");
    }

    // ---- Items: booking, editing, listing ----

    @Test
    @DisplayName("Booking passes the caller's roles to the service and decorates the answer with what the caller may still do")
    void bookingDelegatesWithAccessAndPermissions() {
        roles(SECRETARY, CHAIR);
        BookAgendaItemRequest request = new BookAgendaItemRequest();
        request.setAgcId(1);
        AgendaItemDTO booked = item(100);
        when(agendaService.bookItem(eq(request), eq(USER), any())).thenReturn(booked);
        when(agendaService.canReassignReviewer(eq(100), eq(USER), access(true, true, false, false))).thenReturn(true);
        when(agendaService.canChangeSchedule(eq(100), eq(USER), any())).thenReturn(true);
        when(agendaService.canRequestQuorumException(100, USER)).thenReturn(false);
        when(agendaService.canDecideQuorumException(eq(100), eq(USER), any())).thenReturn(true);

        Response response = resource.bookItem(request);

        assertEquals(200, response.getStatus());
        assertSame(booked, response.getEntity());
        ArgumentCaptor<AgendaAccess> access = ArgumentCaptor.forClass(AgendaAccess.class);
        verify(agendaService).bookItem(eq(request), eq(USER), access.capture());
        assertTrue(access.getValue().isSecretary());
        assertTrue(access.getValue().isChairperson());
        assertFalse(access.getValue().isReviewer());
        assertFalse(access.getValue().isApprover());
        assertTrue(booked.isCanChangeSchedule());
        assertTrue(booked.isCanDecideQuorumException());
        assertFalse(booked.isCanRequestQuorumException());
    }

    /** Matches an AgendaAccess by its four flags (the class has no equals). */
    private static AgendaAccess access(boolean secretary, boolean chair, boolean reviewer, boolean approver) {
        return org.mockito.ArgumentMatchers.argThat(a -> a != null && a.isSecretary() == secretary
                && a.isChairperson() == chair && a.isReviewer() == reviewer && a.isApprover() == approver);
    }

    @Test
    void bookingRuleViolationsAre409() {
        when(agendaService.bookItem(any(), eq(USER), any())).thenThrow(new IllegalStateException("Weekends are unavailable"));
        assertConflict(resource.bookItem(new BookAgendaItemRequest()), "Weekends are unavailable");
    }

    @Test
    @DisplayName("Only the owner may edit, attach to, upload to, or ask a quorum exception for an item")
    void ownerOnlyEndpointsRefuseNonOwners() {
        when(agendaService.canEditItem(100, USER)).thenReturn(false);
        roles(ANALYST, SECRETARY, CHAIR);
        assertForbidden(resource.updateItem(item(100)), "Only the analyst who booked agenda item 100");
        assertForbidden(resource.attachDocument(100, "stash-1", "paper.pdf"), "primary analyst named on it");
        assertForbidden(resource.uploadDocument(100, mock(MultipartFormDataInput.class)), "agenda item 100");
        assertForbidden(resource.requestQuorumException(100, new QuorumExceptionRequest()), "agenda item 100");
        assertForbidden(resource.removeItem(100, "Withdrawn"), "agenda item 100");
        verify(agendaService, never()).updateItem(any(), any());
        verify(agendaService, never()).attachDocument(anyInt(), any(), any(), any());
        verify(agendaService, never()).uploadDocument(anyInt(), any(), any(), any(), any(), any());
        verify(agendaService, never()).requestQuorumException(anyInt(), any(), any());
        verify(agendaService, never()).removeItem(anyInt(), any(), any());
    }

    @Test
    void ownerEditsDelegate() {
        when(agendaService.canEditItem(100, USER)).thenReturn(true);
        AgendaItemDTO body = item(100);
        AgendaItemDTO saved = item(100);
        when(agendaService.updateItem(body, USER)).thenReturn(saved);
        when(agendaService.attachDocument(100, "stash-1", "paper.pdf", USER)).thenReturn(saved);
        assertSame(saved, resource.updateItem(body).getEntity());
        assertSame(saved, resource.attachDocument(100, "stash-1", "paper.pdf").getEntity());

        when(agendaService.updateItem(body, USER)).thenThrow(new IllegalStateException("is Submitted"));
        assertConflict(resource.updateItem(body), "is Submitted");
    }

    @Test
    @DisplayName("Upload reads the file name from Content-Disposition, the bytes, the document type, and derives the extension")
    void uploadParsesTheMultipartForm() throws Exception {
        when(agendaService.canEditItem(100, USER)).thenReturn(true);
        byte[] bytes = "%PDF-1.7".getBytes();
        MultipartFormDataInput form = form("form-data; name=\"file\"; filename=\"Acme Paper.pdf\"", bytes, "Term Sheet");
        AgendaItemDTO saved = item(100);
        when(agendaService.uploadDocument(100, "Term Sheet", "Acme Paper.pdf", ".pdf", bytes, USER)).thenReturn(saved);
        when(agendaService.canChangeSchedule(eq(100), eq(USER), any())).thenReturn(true);

        Response response = resource.uploadDocument(100, form);

        assertEquals(200, response.getStatus());
        assertSame(saved, response.getEntity());
        assertTrue(saved.isCanChangeSchedule(), "the answer carries the caller's permissions");
    }

    @Test
    void uploadDefaultsToACreditPaperWithoutADocTypeOrExtension() throws Exception {
        when(agendaService.canEditItem(100, USER)).thenReturn(true);
        byte[] bytes = new byte[] { 1, 2 };
        MultipartFormDataInput form = form("form-data; name=\"file\"; filename=\"paper\"", bytes, null);
        when(agendaService.uploadDocument(100, "Credit Paper", "paper", "", bytes, USER)).thenReturn(item(100));
        assertEquals(200, resource.uploadDocument(100, form).getStatus());
        verify(agendaService).uploadDocument(100, "Credit Paper", "paper", "", bytes, USER);
    }

    @Test
    void uploadRuleViolationsAre409() throws Exception {
        when(agendaService.canEditItem(100, USER)).thenReturn(true);
        MultipartFormDataInput form = form("form-data; name=\"file\"; filename=\"p.pdf\"", new byte[] { 1 }, null);
        when(agendaService.uploadDocument(anyInt(), any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("is Rejected - documents cannot be added"));
        assertConflict(resource.uploadDocument(100, form), "is Rejected - documents cannot be added");
    }

    private MultipartFormDataInput form(String disposition, byte[] bytes, String docType) throws Exception {
        InputPart file = mock(InputPart.class);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("Content-Disposition", disposition);
        when(file.getHeaders()).thenReturn(headers);
        when(file.getBody(eq(InputStream.class), isNull())).thenReturn(new ByteArrayInputStream(bytes));
        Map<String, List<InputPart>> parts = new HashMap<>();
        parts.put("file", Collections.singletonList(file));
        if (docType != null) {
            InputPart type = mock(InputPart.class);
            when(type.getBody(eq(String.class), isNull())).thenReturn(docType);
            parts.put("docType", Collections.singletonList(type));
        }
        MultipartFormDataInput form = mock(MultipartFormDataInput.class);
        when(form.getFormDataMap()).thenReturn(parts);
        return form;
    }

    @Test
    @DisplayName("Download streams the stored bytes with the file name, or 404 when the document or its data is missing")
    void downloadStreamsTheDocument() throws Exception {
        AgendaItemDocumentDTO document = new AgendaItemDocumentDTO();
        document.setFileName("acme.pdf");
        when(agendaService.canViewDocument(eq(5), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.downloadDocument(5), "Agenda document 5 belongs to another analyst's item.");
        verify(agendaService, never()).getDocumentData(anyInt());

        when(agendaService.canViewDocument(anyInt(), eq(USER), any())).thenReturn(true);
        when(agendaService.getDocument(5)).thenReturn(document);
        when(agendaService.getDocumentData(5)).thenReturn(new byte[] { 7, 8, 9 });

        Response response = resource.downloadDocument(5);

        assertEquals(200, response.getStatus());
        assertEquals("attachment; filename=\"acme.pdf\"", response.getHeaderString("content-disposition"));
        assertEquals("3", response.getHeaderString("Content-Length"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        assertArrayEquals(new byte[] { 7, 8, 9 }, out.toByteArray());

        when(agendaService.getDocumentData(5)).thenReturn(null);
        assertEquals(404, resource.downloadDocument(5).getStatus());
        when(agendaService.getDocument(6)).thenReturn(null);
        when(agendaService.getDocumentData(6)).thenReturn(new byte[] { 1 });
        assertEquals(404, resource.downloadDocument(6).getStatus());
    }

    @Test
    @DisplayName("The item list is the caller's visible items under their roles; 'my' items are the ones for the user")
    void itemListsDelegate() {
        roles(APPROVER);
        List<AgendaItemDTO> visible = Collections.singletonList(item(1));
        when(agendaService.getVisibleItems(eq(USER), any())).thenReturn(visible);
        assertSame(visible, resource.getAllItems().getEntity());
        verify(agendaService).getVisibleItems(eq(USER), access(false, false, false, true));

        List<AgendaItemDTO> mine = Collections.singletonList(item(2));
        when(agendaService.getItemsForUser(USER)).thenReturn(mine);
        assertSame(mine, resource.getMyItems().getEntity());
    }

    @Test
    @DisplayName("Oversight only counts while the role is listed in af.agenda.oversight.roles; the reviewer role name is configurable")
    void accessFollowsTheConfiguredRoles() {
        roles(SECRETARY, CHAIR, REVIEWER, APPROVER);
        when(agendaService.getVisibleItems(eq(USER), any())).thenReturn(Collections.emptyList());
        resource.getAllItems();
        verify(agendaService).getVisibleItems(eq(USER), access(true, true, true, true));

        settings.put("af.agenda.oversight.roles", "secretary, Chairperson");
        resource.getAllItems();
        verify(agendaService).getVisibleItems(eq(USER), access(true, true, false, false));

        settings.remove("af.agenda.oversight.roles");
        settings.put("af.agenda.reviewer.role", " Checker ");
        resource.getAllItems();
        verify(agendaService).getVisibleItems(eq(USER), access(true, true, false, true));
        roles("Checker");
        resource.getAllItems();
        verify(agendaService).getVisibleItems(eq(USER), access(false, false, true, false));
    }

    @Test
    @DisplayName("Opening an item needs visibility; the answer carries what the caller may still do")
    void itemDetailIsGatedByVisibility() {
        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.getItem(100), "Agenda item 100 belongs to another analyst");
        verify(agendaService, never()).getItem(anyInt());

        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO item = item(100);
        when(agendaService.getItem(100)).thenReturn(item);
        when(agendaService.canReassignReviewer(eq(100), eq(USER), any())).thenReturn(true);
        when(agendaService.canChangeSchedule(eq(100), eq(USER), any())).thenReturn(false);
        when(agendaService.canRequestQuorumException(100, USER)).thenReturn(true);
        when(agendaService.canDecideQuorumException(eq(100), eq(USER), any())).thenReturn(false);
        Response response = resource.getItem(100);
        assertSame(item, response.getEntity());
        assertTrue(item.isCanReassignReviewer());
        assertFalse(item.isCanChangeSchedule());
        assertTrue(item.isCanRequestQuorumException());
        assertFalse(item.isCanDecideQuorumException());
    }

    @Test
    void readOnlyItemViewsAreGatedByVisibility() {
        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.getPostMeeting(100), "another analyst");
        assertForbidden(resource.getNotifications(100), "another analyst");
        assertForbidden(resource.getWorkflowActions(100), "another analyst");
        assertForbidden(resource.getParticipants(100), "another analyst");
        assertForbidden(resource.evaluateQuorum(100), "another analyst");
        assertForbidden(resource.getConditions(100), "another analyst");
        verify(agendaService, never()).getPostMeeting(anyInt());
        verify(agendaService, never()).getNotificationsForItem(anyInt());
        verify(agendaService, never()).getWorkflowActions(anyInt());
        verify(agendaService, never()).getParticipants(anyInt());
        verify(agendaService, never()).evaluateQuorum(anyInt());
        verify(agendaService, never()).getConditions(anyInt());

        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(true);
        AgendaPostMeetingDTO post = new AgendaPostMeetingDTO();
        List<AgendaNotificationDTO> notifications = Collections.singletonList(new AgendaNotificationDTO());
        List<AgendaWorkflowActionDTO> actions = Collections.singletonList(new AgendaWorkflowActionDTO());
        List<AgendaParticipantDTO> participants = Collections.singletonList(new AgendaParticipantDTO());
        AgendaQuorumResultDTO quorum = new AgendaQuorumResultDTO();
        List<AgendaConditionDTO> conditions = Collections.singletonList(new AgendaConditionDTO());
        when(agendaService.getPostMeeting(100)).thenReturn(post);
        when(agendaService.getNotificationsForItem(100)).thenReturn(notifications);
        when(agendaService.getWorkflowActions(100)).thenReturn(actions);
        when(agendaService.getParticipants(100)).thenReturn(participants);
        when(agendaService.evaluateQuorum(100)).thenReturn(quorum);
        when(agendaService.getConditions(100)).thenReturn(conditions);
        assertSame(post, resource.getPostMeeting(100).getEntity());
        assertSame(notifications, resource.getNotifications(100).getEntity());
        assertSame(actions, resource.getWorkflowActions(100).getEntity());
        assertSame(participants, resource.getParticipants(100).getEntity());
        assertSame(quorum, resource.evaluateQuorum(100).getEntity());
        assertSame(conditions, resource.getConditions(100).getEntity());
    }

    // ---- Schedule changes: move and roster ----

    @Test
    void moveAndRosterNeedTheScheduleRight() {
        when(agendaService.canChangeSchedule(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.moveItem(100, new MoveAgendaItemRequest()), "change the meeting date", "only before the item is approved");
        assertForbidden(resource.updateRoster(100, item(100)), "change the quorum roster");
        verify(agendaService, never()).moveItem(anyInt(), any(), any());
        verify(agendaService, never()).updateRoster(anyInt(), any(), any());

        when(agendaService.canChangeSchedule(eq(100), eq(USER), any())).thenReturn(true);
        MoveAgendaItemRequest move = new MoveAgendaItemRequest();
        AgendaItemDTO roster = item(100);
        AgendaItemDTO saved = item(100);
        when(agendaService.moveItem(100, move, USER)).thenReturn(saved);
        when(agendaService.updateRoster(100, roster, USER)).thenReturn(saved);
        assertSame(saved, resource.moveItem(100, move).getEntity());
        assertSame(saved, resource.updateRoster(100, roster).getEntity());
        assertTrue(saved.isCanChangeSchedule(), "decorated with the caller's permissions");

        when(agendaService.moveItem(100, move, USER)).thenThrow(new IllegalStateException("overlaps"));
        when(agendaService.updateRoster(100, roster, USER)).thenThrow(new IllegalStateException("The roster is required"));
        assertConflict(resource.moveItem(100, move), "overlaps");
        assertConflict(resource.updateRoster(100, roster), "The roster is required");
    }

    // ---- Booking drafts ----

    @Test
    @DisplayName("Drafts: create forces a new id, update takes the path id, a stranger's draft is 404, an empty draft is 409")
    void bookingDraftsDelegate() {
        List<AgendaBookingDraftDTO> drafts = Collections.singletonList(new AgendaBookingDraftDTO());
        when(agendaService.getBookingDrafts(USER)).thenReturn(drafts);
        assertSame(drafts, resource.getBookingDrafts().getEntity());

        AgendaBookingDraftDTO draft = new AgendaBookingDraftDTO();
        draft.setAgbdId(55);
        draft.setPayload("{}");
        when(agendaService.saveBookingDraft(draft, USER)).thenReturn(draft);
        assertSame(draft, resource.createBookingDraft(draft).getEntity());
        assertEquals(0, draft.getAgbdId(), "a POST always creates");
        assertSame(draft, resource.updateBookingDraft(7, draft).getEntity());
        assertEquals(7, draft.getAgbdId(), "a PUT updates the draft in the path");

        when(agendaService.getBookingDraft(7, USER)).thenReturn(draft);
        assertSame(draft, resource.getBookingDraft(7).getEntity());
        assertEquals(204, resource.deleteBookingDraft(7).getStatus());
        verify(agendaService).deleteBookingDraft(7, USER);

        when(agendaService.getBookingDraft(8, USER)).thenThrow(new IllegalArgumentException("Booking draft 8 was not found"));
        doThrow(new IllegalArgumentException("Booking draft 8 was not found")).when(agendaService).deleteBookingDraft(8, USER);
        Response missing = resource.getBookingDraft(8);
        assertEquals(404, missing.getStatus());
        assertEquals("Booking draft 8 was not found", message(missing));
        assertEquals(404, resource.deleteBookingDraft(8).getStatus());

        when(agendaService.saveBookingDraft(any(), eq(USER))).thenThrow(new IllegalStateException("There is nothing to save in this draft"));
        assertConflict(resource.createBookingDraft(new AgendaBookingDraftDTO()), "There is nothing to save in this draft");
        assertConflict(resource.updateBookingDraft(7, null), "There is nothing to save in this draft");
        assertEquals(409, resource.createBookingDraft(null).getStatus(), "a null body is passed through and refused by the service");

        when(agendaService.saveBookingDraft(any(), eq(USER))).thenThrow(new IllegalArgumentException("Booking draft 9 was not found"));
        assertEquals(404, resource.updateBookingDraft(9, new AgendaBookingDraftDTO()).getStatus());
    }

    // ---- Removal ----

    @Test
    @DisplayName("Remove needs a removing role and ownership, then delegates with the reason")
    void removeNeedsRoleAndOwnership() {
        roles(APPROVER, REVIEWER);
        assertForbidden(resource.removeItem(100, "Withdrawn"), "CreditAnalyst or Secretary or Chairperson", "remove agenda items");
        verify(agendaService, never()).canEditItem(anyInt(), any());

        AgendaItemDTO removed = item(100);
        when(agendaService.canEditItem(100, USER)).thenReturn(true);
        when(agendaService.removeItem(100, USER, "Withdrawn")).thenReturn(removed);
        for (String role : new String[] { ANALYST, SECRETARY, CHAIR }) {
            roles(role);
            assertSame(removed, resource.removeItem(100, "Withdrawn").getEntity());
        }
        when(agendaService.removeItem(100, USER, "Withdrawn")).thenThrow(new IllegalStateException("has been held"));
        assertConflict(resource.removeItem(100, "Withdrawn"), "has been held");
    }

    // ---- Post-meeting capture ----

    @Test
    void postMeetingCaptureBelongsToTheSecretariatOfTheCommittee() {
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.savePostMeeting(100, new AgendaPostMeetingDTO()), "Only the secretary or chair of the committee for agenda item 100");
        assertForbidden(resource.saveConditions(100, Collections.emptyList()), "record what happened at the meeting");
        verify(agendaService, never()).savePostMeeting(any(), any());
        verify(agendaService, never()).saveConditions(anyInt(), any(), any());

        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(true);
        AgendaPostMeetingDTO body = new AgendaPostMeetingDTO();
        body.setAgiId(1);
        when(agendaService.savePostMeeting(body, USER)).thenReturn(body);
        assertSame(body, resource.savePostMeeting(100, body).getEntity());
        assertEquals(100, body.getAgiId(), "the path id wins");
        List<AgendaConditionDTO> conditions = Collections.singletonList(new AgendaConditionDTO());
        when(agendaService.saveConditions(100, conditions, USER)).thenReturn(conditions);
        assertSame(conditions, resource.saveConditions(100, conditions).getEntity());
        when(agendaService.saveConditions(100, conditions, USER)).thenThrow(new IllegalStateException("Meeting is closed"));
        assertConflict(resource.saveConditions(100, conditions), "Meeting is closed");
    }

    @Test
    @DisplayName("Unlocking a signed-off record is for the chairperson or secretary, not a reviewer or approver")
    void unlockIsChairOrSecretary() {
        roles(REVIEWER, APPROVER, ANALYST);
        assertForbidden(resource.unlockPostMeeting(100), "Chairperson or Secretary", "unlock a signed-off post-meeting record");
        verify(agendaService, never()).unlockPostMeeting(anyInt(), any());
        roles(CHAIR);
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.unlockPostMeeting(100), "Only the secretary or chair of the committee for agenda item 100 may unlock");
        verify(agendaService, never()).unlockPostMeeting(anyInt(), any());
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(true);
        AgendaPostMeetingDTO unlocked = new AgendaPostMeetingDTO();
        when(agendaService.unlockPostMeeting(100, USER)).thenReturn(unlocked);
        for (String role : new String[] { CHAIR, SECRETARY }) {
            roles(role);
            assertSame(unlocked, resource.unlockPostMeeting(100).getEntity());
        }
    }

    @Test
    @DisplayName("Closing a condition is for the secretary or chair of its item's committee; rule refusals are 409")
    void closeConditionIsScopedAndMaps409() {
        when(agendaService.canCloseCondition(eq(5), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.closeCondition(5, "Closed", "Evidence received"), "can close or waive it");
        verify(agendaService, never()).closeCondition(anyInt(), any(), any(), any());
        when(agendaService.canCloseCondition(eq(5), eq(USER), any())).thenReturn(true);
        AgendaConditionDTO closed = new AgendaConditionDTO();
        when(agendaService.closeCondition(5, "Closed", "Evidence received", USER)).thenReturn(closed);
        assertSame(closed, resource.closeCondition(5, "Closed", "Evidence received").getEntity());
        when(agendaService.closeCondition(5, "Open", null, USER)).thenThrow(new IllegalStateException("A condition can only be closed as Closed or Waived"));
        assertConflict(resource.closeCondition(5, "Open", null), "A condition can only be closed as Closed or Waived");
    }

    // ---- Workflow: submit, return, review, approve, reject, re-open ----

    @Test
    @DisplayName("Submit: analyst, secretary or chair; a quorum override reason is refused unless the caller is secretary or chair")
    void submitRolesAndOverride() {
        roles(APPROVER, REVIEWER);
        assertForbidden(resource.submitItem(100, null), "CreditAnalyst or Secretary or Chairperson", "submit agenda items");

        roles(ANALYST);
        assertForbidden(resource.submitItem(100, "Chair agreed"), "Only Secretary or Chairperson may override quorum");
        verify(agendaService, never()).submitItem(anyInt(), any(), any(), any());

        AgendaItemDTO submitted = item(100);
        when(agendaService.submitItem(eq(100), isNull(), eq(USER), any())).thenReturn(submitted);
        when(agendaService.submitItem(eq(100), eq("  "), eq(USER), any())).thenReturn(submitted);
        assertSame(submitted, resource.submitItem(100, null).getEntity());
        assertSame(submitted, resource.submitItem(100, "  ").getEntity(), "a blank reason is not an override");
        verify(agendaService).submitItem(eq(100), isNull(), eq(USER), access(false, false, false, false));

        roles(SECRETARY);
        when(agendaService.submitItem(eq(100), eq("Chair agreed"), eq(USER), any())).thenReturn(submitted);
        assertSame(submitted, resource.submitItem(100, "Chair agreed").getEntity());
        verify(agendaService).submitItem(eq(100), eq("Chair agreed"), eq(USER), access(true, false, false, false));

        roles(CHAIR);
        when(agendaService.submitItem(eq(100), eq("Chair agreed"), eq(USER), any())).thenThrow(new IllegalStateException("quorum not met"));
        assertConflict(resource.submitItem(100, "Chair agreed"), "quorum not met");
    }

    @Test
    @DisplayName("Return: reviewer, approver, chair or secretary")
    void returnRoles() {
        roles(ANALYST);
        assertForbidden(resource.returnItem(100, "Fix"), "ReviewAnalyst or Approver or Chairperson or Secretary");
        roles(REVIEWER);
        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.returnItem(100, "Fix"), "belongs to another analyst");
        verify(agendaService, never()).returnItem(anyInt(), any(), any());
        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO returned = item(100);
        when(agendaService.returnItem(100, "Fix", USER)).thenReturn(returned);
        for (String role : new String[] { REVIEWER, APPROVER, CHAIR, SECRETARY }) {
            roles(role);
            assertSame(returned, resource.returnItem(100, "Fix").getEntity());
        }
        when(agendaService.returnItem(100, "Fix", USER)).thenThrow(new IllegalStateException("Only a submitted agenda item can be returned"));
        assertConflict(resource.returnItem(100, "Fix"), "Only a submitted agenda item can be returned");
    }

    @Test
    @DisplayName("Complete review: reviewer, secretary or chair, and only on an item the caller may open")
    void completeReviewRolesAndVisibility() {
        roles(ANALYST, APPROVER);
        assertForbidden(resource.completeReview(100, "ok"), "ReviewAnalyst or Secretary or Chairperson", "complete submission review");
        roles(REVIEWER);
        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.completeReview(100, "ok"), "belongs to another analyst");
        verify(agendaService, never()).completeReview(anyInt(), any(), any());

        when(agendaService.canViewItem(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO reviewed = item(100);
        when(agendaService.completeReview(100, "ok", USER)).thenReturn(reviewed);
        for (String role : new String[] { REVIEWER, SECRETARY, CHAIR }) {
            roles(role);
            assertSame(reviewed, resource.completeReview(100, "ok").getEntity());
        }
        when(agendaService.completeReview(100, "ok", USER)).thenThrow(new IllegalStateException("Accept all required document versions"));
        assertConflict(resource.completeReview(100, "ok"), "Accept all required document versions");
    }

    @Test
    @DisplayName("Approve: an Approver, or the chair or secretary of the item's own committee; never the analyst or reviewer")
    void approveRoles() {
        roles(ANALYST, REVIEWER);
        assertForbidden(resource.approveItem(100, "ok"), "Only Approver or Chairperson or Secretary may approve agenda items");
        roles(CHAIR, SECRETARY);
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.approveItem(100, "ok"), "Only the secretary or chair of the committee for agenda item 100 may approve");
        verify(agendaService, never()).approveItem(anyInt(), any(), any());
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO approved = item(100);
        when(agendaService.approveItem(100, "ok", USER)).thenReturn(approved);
        for (String role : new String[] { APPROVER, CHAIR, SECRETARY }) {
            roles(role);
            assertSame(approved, resource.approveItem(100, "ok").getEntity());
        }
        verify(agendaService, times(3)).approveItem(100, "ok", USER);
        when(agendaService.approveItem(100, "ok", USER)).thenThrow(new IllegalStateException("Complete the independent submission review"));
        assertConflict(resource.approveItem(100, "ok"), "Complete the independent submission review");
    }

    @Test
    @DisplayName("Reject: the same people as approve - an Approver, or the chair or secretary of the item's committee")
    void rejectRoles() {
        roles(REVIEWER, ANALYST);
        assertForbidden(resource.rejectItem(100, "No"), "Only Approver or Chairperson or Secretary may reject agenda items");
        roles(SECRETARY);
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.rejectItem(100, "No"), "Only the secretary or chair of the committee for agenda item 100 may reject");
        verify(agendaService, never()).rejectItem(anyInt(), any(), any());
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO rejected = item(100);
        when(agendaService.rejectItem(100, "No", USER)).thenReturn(rejected);
        for (String role : new String[] { APPROVER, CHAIR, SECRETARY }) {
            roles(role);
            assertSame(rejected, resource.rejectItem(100, "No").getEntity());
        }
        when(agendaService.rejectItem(100, "No", USER)).thenThrow(new IllegalStateException("Only a reviewed agenda item can be rejected"));
        assertConflict(resource.rejectItem(100, "No"), "Only a reviewed agenda item can be rejected");
    }

    @Test
    @DisplayName("Re-open: Secretary or Chairperson")
    void reopenRoles() {
        roles(APPROVER, REVIEWER, ANALYST);
        assertForbidden(resource.reopenItem(100, "Correction"), "Secretary or Chairperson", "re-open agenda items");
        roles(SECRETARY);
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.reopenItem(100, "Correction"), "Only the secretary or chair of the committee for agenda item 100 may re-open");
        verify(agendaService, never()).reopenItem(anyInt(), any(), any());
        when(agendaService.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO reopened = item(100);
        when(agendaService.reopenItem(100, "Correction", USER)).thenReturn(reopened);
        for (String role : new String[] { SECRETARY, CHAIR }) {
            roles(role);
            assertSame(reopened, resource.reopenItem(100, "Correction").getEntity());
        }
        when(agendaService.reopenItem(100, "Correction", USER)).thenThrow(new IllegalStateException("does not need re-opening"));
        assertConflict(resource.reopenItem(100, "Correction"), "does not need re-opening");
    }

    // ---- Quorum exceptions ----

    @Test
    void quorumExceptionRequestDelegatesTheReasonForTheOwner() {
        when(agendaService.canEditItem(100, USER)).thenReturn(true);
        QuorumExceptionRequest request = new QuorumExceptionRequest();
        request.setReason("Member on leave");
        AgendaItemDTO saved = item(100);
        when(agendaService.requestQuorumException(100, "Member on leave", USER)).thenReturn(saved);
        when(agendaService.canRequestQuorumException(100, USER)).thenReturn(false);
        assertSame(saved, resource.requestQuorumException(100, request).getEntity());
        assertFalse(saved.isCanRequestQuorumException());

        when(agendaService.requestQuorumException(100, null, USER)).thenThrow(new IllegalStateException("Give a reason for the quorum exception"));
        assertConflict(resource.requestQuorumException(100, null), "Give a reason for the quorum exception");
    }

    @Test
    @DisplayName("Deciding needs Secretary or Chairperson and an explicit approved flag (400 without)")
    void quorumExceptionDecision() {
        roles(APPROVER);
        QuorumExceptionRequest request = new QuorumExceptionRequest();
        request.setApproved(Boolean.TRUE);
        assertForbidden(resource.decideQuorumException(100, request), "decide quorum exceptions");

        roles(SECRETARY);
        Response missing = resource.decideQuorumException(100, null);
        assertEquals(400, missing.getStatus());
        assertEquals("Say whether the quorum exception is approved", message(missing));
        assertEquals(400, resource.decideQuorumException(100, new QuorumExceptionRequest()).getStatus());
        verify(agendaService, never()).decideQuorumException(anyInt(), anyBoolean(), any(), any(), any());

        request.setApproved(Boolean.FALSE);
        request.setNote("Find another member");
        AgendaItemDTO decided = item(100);
        when(agendaService.decideQuorumException(eq(100), eq(false), eq("Find another member"), eq(USER), any())).thenReturn(decided);
        assertSame(decided, resource.decideQuorumException(100, request).getEntity());
        verify(agendaService).decideQuorumException(eq(100), eq(false), eq("Find another member"), eq(USER), access(true, false, false, false));

        roles(CHAIR);
        when(agendaService.decideQuorumException(eq(100), eq(false), eq("Find another member"), eq(USER), any()))
                .thenThrow(new IllegalStateException("There is no quorum exception request waiting on this item"));
        assertConflict(resource.decideQuorumException(100, request), "There is no quorum exception request waiting on this item");
    }

    // ---- Reviewer reassignment ----

    @Test
    void reassignReviewerNeedsRoleThenCommitteeScope() {
        ReassignAgendaReviewerRequest request = new ReassignAgendaReviewerRequest();
        roles(REVIEWER, APPROVER);
        assertForbidden(resource.reassignReviewer(100, request), "reassign reviewers");

        roles(SECRETARY);
        when(agendaService.canReassignReviewer(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.reassignReviewer(100, request), "Only this committee's secretary or chair may reassign");
        verify(agendaService, never()).reassignReviewer(anyInt(), any(), any(), any());

        when(agendaService.canReassignReviewer(eq(100), eq(USER), any())).thenReturn(true);
        AgendaItemDTO item = item(100);
        when(agendaService.reassignReviewer(eq(100), eq(request), eq(USER), any())).thenReturn(item);
        assertSame(item, resource.reassignReviewer(100, request).getEntity());
        assertTrue(item.isCanReassignReviewer());

        when(agendaService.reassignReviewer(eq(100), eq(request), eq(USER), any())).thenThrow(new IllegalStateException("The assigned reviewer has changed"));
        assertConflict(resource.reassignReviewer(100, request), "The assigned reviewer has changed");
    }

    // ---- Documents and participants ----

    @Test
    void validateDocumentRoles() {
        roles(ANALYST, APPROVER);
        assertForbidden(resource.validateDocument(5, "Accepted", "ok"), "ReviewAnalyst or Secretary or Chairperson", "validate documents");
        AgendaItemDocumentDTO document = new AgendaItemDocumentDTO();
        when(agendaService.validateDocument(5, "Accepted", "ok", USER)).thenReturn(document);
        for (String role : new String[] { REVIEWER, SECRETARY, CHAIR }) {
            roles(role);
            assertSame(document, resource.validateDocument(5, "Accepted", "ok").getEntity());
        }
        when(agendaService.validateDocument(5, "Accepted", "ok", USER)).thenThrow(new IllegalStateException("already been accepted"));
        assertConflict(resource.validateDocument(5, "Accepted", "ok"), "already been accepted");
    }

    @Test
    void participantsAreGatedBySeparateRosterRight() {
        List<AgendaParticipantDTO> participants = Collections.singletonList(new AgendaParticipantDTO());
        when(agendaService.canEditParticipants(eq(100), eq(USER), any())).thenReturn(false);
        assertForbidden(resource.saveParticipants(100, participants), "only the secretary or chair of its committee can change who attended");
        verify(agendaService, never()).saveParticipants(anyInt(), any(), any());

        when(agendaService.canEditParticipants(eq(100), eq(USER), any())).thenReturn(true);
        when(agendaService.saveParticipants(100, participants, USER)).thenReturn(participants);
        assertSame(participants, resource.saveParticipants(100, participants).getEntity());
        when(agendaService.saveParticipants(100, participants, USER)).thenThrow(new IllegalStateException("participants are final"));
        assertConflict(resource.saveParticipants(100, participants), "participants are final");
    }

    // ---- Schedulers ----

    @Test
    @DisplayName("The job endpoints answer only the scheduler's own user (af.scheduler.user); anyone else is refused")
    void schedulerEndpointsReturnTheCounts() {
        roles(SECRETARY, CHAIR, APPROVER);
        assertForbidden(resource.processCutOff(), "Only the agenda scheduler may process agenda cut-offs.");
        assertForbidden(resource.processReminders(), "Only the agenda scheduler may send agenda reminders.");
        settings.put("af.scheduler.user", "Scheduler@rmb.co.za");
        assertForbidden(resource.processCutOff(), "Only the agenda scheduler may process agenda cut-offs.");
        verifyNoInteractions(agendaService);
        when(principal.getName()).thenReturn("scheduler@rmb.co.za");
        roles();
        when(agendaService.processCutOffs()).thenReturn(3);
        when(agendaService.processReminders()).thenReturn(5);
        assertEquals(3, resource.processCutOff().getEntity());
        assertEquals(5, resource.processReminders().getEntity());
    }

    // ---- The published contract, read from the annotations ----

    @Test
    void resourceIsMountedUnderTheAgendaPath() {
        assertEquals("/approvalForum/agenda", ApprovalForumAgendaResource.class.getAnnotation(Path.class).value());
    }

    @Test
    @DisplayName("Each endpoint has the HTTP method and path it is published under; writes are transactional")
    void endpointAnnotations() throws Exception {
        expect("getUsers", GET.class, "/users", false);
        expect("getAvailableLists", GET.class, "/lists", false);
        expect("getAgendaCounterparties", GET.class, "/counterparties", false, int.class);
        expect("getCasdmApplication", GET.class, "/casdm/application", false, String.class);
        expect("getCommittees", GET.class, "/committees", false);
        expect("updateCommittee", PUT.class, "/committees", true, AgendaCommitteeDTO.class);
        expect("getAllCommittees", GET.class, "/committees/all", false);
        expect("createCommittee", POST.class, "/committees", true, AgendaCommitteeDTO.class);
        expect("deactivateCommittee", POST.class, "/committees/{agcId}/deactivate", true, int.class);
        expect("createMeeting", POST.class, "/meetings", true, AgendaMeetingDTO.class);
        expect("updateMeeting", PUT.class, "/meetings/{agmId}", true, int.class, AgendaMeetingDTO.class);
        expect("cancelMeeting", POST.class, "/meetings/{agmId}/cancel", true, int.class, String.class);
        expect("reinstateMeeting", POST.class, "/meetings/{agmId}/reinstate", true, int.class, String.class);
        expect("getMeetingsBetween", GET.class, "/meetings", false, String.class, String.class);
        expect("getAvailableDates", GET.class, "/committees/{agcId}/dates", false, int.class);
        expect("getLastRoster", GET.class, "/committees/{agcId}/lastRoster", false, int.class);
        expect("getCommitteeMembers", GET.class, "/committees/{agcId}/members", false, int.class);
        expect("getMeetings", GET.class, "/committees/{agcId}/meetings", false, int.class);
        expect("getMeeting", GET.class, "/meetings/{agmId}", false, int.class);
        expect("sendInvitation", POST.class, "/meetings/{agmId}/invitation", true, int.class);
        expect("sendInvitationUpdate", POST.class, "/meetings/{agmId}/invitation/update", true, int.class);
        expect("markMeetingHeld", POST.class, "/meetings/{agmId}/held", true, int.class, String.class, String.class, Boolean.class);
        expect("closeMeeting", POST.class, "/meetings/{agmId}/close", true, int.class);
        expect("reopenMeeting", POST.class, "/meetings/{agmId}/reopen", true, int.class, String.class);
        expect("getMeetingRecords", GET.class, "/meetings/{agmId}/records", false, int.class);
        expect("addMeetingRecord", POST.class, "/meetings/{agmId}/records", true, int.class, AgendaMeetingRecordDTO.class);
        expect("bookItem", POST.class, "/items", true, BookAgendaItemRequest.class);
        expect("updateItem", PUT.class, "/items", true, AgendaItemDTO.class);
        expect("getAllItems", GET.class, "/items", false);
        expect("getMyItems", GET.class, "/items/my", false);
        expect("getItem", GET.class, "/items/{agiId}", false, int.class);
        expect("moveItem", POST.class, "/items/{agiId}/meeting", true, int.class, MoveAgendaItemRequest.class);
        expect("updateRoster", PUT.class, "/items/{agiId}/roster", true, int.class, AgendaItemDTO.class);
        expect("getBookingDrafts", GET.class, "/drafts", false);
        expect("getBookingDraft", GET.class, "/drafts/{agbdId}", false, int.class);
        expect("createBookingDraft", POST.class, "/drafts", true, AgendaBookingDraftDTO.class);
        expect("updateBookingDraft", PUT.class, "/drafts/{agbdId}", true, int.class, AgendaBookingDraftDTO.class);
        expect("deleteBookingDraft", DELETE.class, "/drafts/{agbdId}", true, int.class);
        expect("attachDocument", POST.class, "/items/{agiId}/document", true, int.class, String.class, String.class);
        expect("uploadDocument", POST.class, "/items/{agiId}/upload", true, int.class, MultipartFormDataInput.class);
        expect("downloadDocument", GET.class, "/documents/{agidId}/download", false, int.class);
        expect("removeItem", POST.class, "/items/{agiId}/remove", true, int.class, String.class);
        expect("getPostMeeting", GET.class, "/items/{agiId}/postMeeting", false, int.class);
        expect("getNotifications", GET.class, "/items/{agiId}/notifications", false, int.class);
        expect("savePostMeeting", POST.class, "/items/{agiId}/postMeeting", true, int.class, AgendaPostMeetingDTO.class);
        expect("unlockPostMeeting", POST.class, "/items/{agiId}/postMeeting/unlock", true, int.class);
        expect("submitItem", POST.class, "/items/{agiId}/submit", true, int.class, String.class);
        expect("returnItem", POST.class, "/items/{agiId}/return", true, int.class, String.class);
        expect("requestQuorumException", POST.class, "/items/{agiId}/quorum-exception", true, int.class, QuorumExceptionRequest.class);
        expect("decideQuorumException", POST.class, "/items/{agiId}/quorum-exception/decision", true, int.class, QuorumExceptionRequest.class);
        expect("reassignReviewer", POST.class, "/items/{agiId}/reviewer", true, int.class, ReassignAgendaReviewerRequest.class);
        expect("completeReview", POST.class, "/items/{agiId}/review", true, int.class, String.class);
        expect("approveItem", POST.class, "/items/{agiId}/approve", true, int.class, String.class);
        expect("rejectItem", POST.class, "/items/{agiId}/reject", true, int.class, String.class);
        expect("reopenItem", POST.class, "/items/{agiId}/reopen", true, int.class, String.class);
        expect("getWorkflowActions", GET.class, "/items/{agiId}/actions", false, int.class);
        expect("validateDocument", POST.class, "/documents/{agidId}/validate", true, int.class, String.class, String.class);
        expect("getParticipants", GET.class, "/items/{agiId}/participants", false, int.class);
        expect("saveParticipants", PUT.class, "/items/{agiId}/participants", true, int.class, List.class);
        expect("evaluateQuorum", GET.class, "/items/{agiId}/quorum", false, int.class);
        expect("getConditions", GET.class, "/items/{agiId}/conditions", false, int.class);
        expect("saveConditions", PUT.class, "/items/{agiId}/conditions", true, int.class, List.class);
        expect("closeCondition", POST.class, "/conditions/{agcaId}/close", true, int.class, String.class, String.class);
        expect("getReportSummary", GET.class, "/reports/summary", false, String.class, String.class);
        expect("processCutOff", POST.class, "/processCutOff", true);
        expect("processReminders", POST.class, "/processReminders", true);
    }

    private static void expect(String name, Class<? extends Annotation> httpMethod, String path, boolean transactional,
                               Class<?>... parameters) throws NoSuchMethodException {
        Method method = ApprovalForumAgendaResource.class.getMethod(name, parameters);
        assertNotNull(method.getAnnotation(httpMethod), name + " is " + httpMethod.getSimpleName());
        assertEquals(path, method.getAnnotation(Path.class).value(), name);
        assertEquals(transactional, method.isAnnotationPresent(Transactional.class), name + " transactional");
        assertEquals(Response.class, method.getReturnType(), name);
    }

    @Test
    @DisplayName("The agenda lock and meeting status endpoints were removed on purpose and have not come back")
    void noLockOrStatusEndpoints() {
        for (Method method : ApprovalForumAgendaResource.class.getMethods()) {
            Path path = method.getAnnotation(Path.class);
            if (path == null) continue;
            assertFalse(path.value().endsWith("/lock"), path.value());
            assertFalse(path.value().endsWith("/status"), path.value());
            assertFalse(path.value().endsWith("/compile"), path.value());
        }
        assertEquals(66, Arrays.stream(ApprovalForumAgendaResource.class.getMethods())
                .filter(m -> m.isAnnotationPresent(Path.class)).count(), "every published endpoint is covered above");
    }

    @Test
    void roleNamesMatchTheDafGroupsMappedByAuth() {
        assertEquals("Secretary", ApprovalForumAgendaResource.ROLE_SECRETARY);
        assertEquals("Chairperson", ApprovalForumAgendaResource.ROLE_CHAIRPERSON);
        assertEquals("Approver", ApprovalForumAgendaResource.ROLE_APPROVER);
        assertEquals("CreditAnalyst", ApprovalForumAgendaResource.ROLE_CREDIT_ANALYST);
    }
}
