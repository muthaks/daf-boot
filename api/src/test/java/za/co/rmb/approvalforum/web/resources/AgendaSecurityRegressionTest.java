package za.co.rmb.approvalforum.web.resources;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.AgendaService;

import java.lang.reflect.Field;
import java.security.Principal;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Intended access rules for G1/G2/G3/G5/G6. No API, database, directory or calendar is contacted. */
class AgendaSecurityRegressionTest {
    private static final String USER = "committee.secretary@rmb.co.za";
    private static final Set<String> MUTATIONS = Set.of("updateCommittee", "deactivateCommittee", "createMeeting",
            "updateMeeting", "sendInvitation", "sendInvitationUpdate", "markMeetingHeld", "closeMeeting",
            "reopenMeeting", "cancelMeeting", "reinstateMeeting", "addMeetingRecord", "reopenItem",
            "unlockPostMeeting", "approveItem", "rejectItem", "closeCondition");
    private final Set<String> roles = new HashSet<>();
    private AgendaService service;
    private ApprovalForumAgendaResource resource;

    @BeforeEach
    void setUp() throws Exception {
        service = mock(AgendaService.class);
        SecurityContext context = mock(SecurityContext.class);
        Principal principal = () -> USER;
        when(context.getUserPrincipal()).thenReturn(principal);
        when(context.isUserInRole(anyString())).thenAnswer(call -> roles.contains(call.getArgument(0)));
        resource = new ApprovalForumAgendaResource();
        inject("context", context);
        inject("agendaService", service);
        inject("afSettings", new HashMap<String, String>());
    }

    private void inject(String name, Object value) throws Exception {
        Field field = ApprovalForumAgendaResource.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(resource, value);
    }

    private void scope(String role, boolean allowed) {
        roles.clear();
        roles.add(role);
        when(service.decidesForCommittee(eq(1), eq(USER), any())).thenReturn(allowed);
        when(service.decidesForMeeting(eq(10), eq(USER), any())).thenReturn(allowed);
        when(service.canCaptureOutcome(eq(100), eq(USER), any())).thenReturn(allowed);
        clearInvocations(service);
    }

    @Test
    void G1_unrelatedAuthenticatedUserCannotReadDocumentBytes() {
        roles.add("CreditAnalyst");
        when(service.canViewDocument(eq(50), eq(USER), any())).thenReturn(false);
        assertEquals(403, resource.downloadDocument(50).getStatus());
        verify(service, never()).getDocument(50);
        verify(service, never()).getDocumentData(50);
    }

    @Test
    void G1_authorizedDocumentReaderStillReceivesTheDocument() {
        when(service.canViewDocument(eq(50), eq(USER), any())).thenReturn(true);
        AgendaItemDocumentDTO document = new AgendaItemDocumentDTO();
        document.setAgiId(100);
        document.setFileName("credit-paper.pdf");
        when(service.getDocument(50)).thenReturn(document);
        when(service.getDocumentData(50)).thenReturn(new byte[]{1, 2, 3});
        assertEquals(200, resource.downloadDocument(50).getStatus());
        verify(service).getDocumentData(50);
    }

    @org.junit.jupiter.api.TestFactory
    Stream<DynamicTest> G2_conditionClosureRequiresAuthorizationForBothOutcomes() {
        return Stream.of("Closed", "Waived").flatMap(status -> Stream.of(false, true).map(allowed ->
                DynamicTest.dynamicTest("G2 " + status + (allowed ? " authorized" : " unrelated caller"), () -> {
                    when(service.canCloseCondition(eq(500), eq(USER), any())).thenReturn(allowed);
                    clearInvocations(service);
                    Response response = resource.closeCondition(500, status, "Evidence checked");
                    assertEquals(allowed ? 200 : 403, response.getStatus());
                    if (allowed) verify(service).closeCondition(500, status, "Evidence checked", USER);
                    else verify(service, never()).closeCondition(anyInt(), anyString(), anyString(), anyString());
                })));
    }

    @Test
    void G3_meetingReadUsesTheCallerScopedServiceInsteadOfTheUnfilteredService() {
        roles.add("CreditAnalyst");
        AgendaMeetingDTO filtered = new AgendaMeetingDTO();
        filtered.setAgmId(10);
        when(service.getMeetingFor(eq(10), eq(USER), any())).thenReturn(filtered);
        assertSame(filtered, resource.getMeeting(10).getEntity());
        verify(service).getMeetingFor(eq(10), eq(USER), any());
        verify(service, never()).getMeeting(10);
    }

    private record Action(String name, Supplier<Response> call) {}

    @org.junit.jupiter.api.TestFactory
    Stream<DynamicTest> G5_G6_committeeActionsDenyOtherCommitteesAndAllowTheServingSecretariat() {
        AgendaCommitteeDTO committee = new AgendaCommitteeDTO(); committee.setAgcId(1);
        AgendaMeetingDTO meeting = new AgendaMeetingDTO(); meeting.setAgmId(10); meeting.setAgcId(1);
        List<Action> actions = List.of(
                new Action("update committee", () -> resource.updateCommittee(committee)),
                new Action("deactivate committee", () -> resource.deactivateCommittee(1)),
                new Action("create meeting", () -> resource.createMeeting(meeting)),
                new Action("update meeting", () -> resource.updateMeeting(10, meeting)),
                new Action("send invitations", () -> resource.sendInvitation(10)),
                new Action("update invitations", () -> resource.sendInvitationUpdate(10)),
                new Action("mark held", () -> resource.markMeetingHeld(10, "09:00", "10:00", true)),
                new Action("close meeting", () -> resource.closeMeeting(10)),
                new Action("reopen meeting", () -> resource.reopenMeeting(10, "Correct the record")),
                new Action("cancel meeting", () -> resource.cancelMeeting(10, "No longer needed")),
                new Action("reinstate meeting", () -> resource.reinstateMeeting(10, "Meeting restored")),
                new Action("attach meeting record", () -> resource.addMeetingRecord(10, new AgendaMeetingRecordDTO())),
                new Action("reopen item", () -> resource.reopenItem(100, "Correct the submission")),
                new Action("unlock post-meeting record", () -> resource.unlockPostMeeting(100)),
                new Action("G5 approve item", () -> resource.approveItem(100, "Reviewed")),
                new Action("G5 reject item", () -> resource.rejectItem(100, "Insufficient information")));
        return actions.stream().flatMap(action -> Stream.of("Secretary", "Chairperson").flatMap(role -> Stream.of(false, true).map(allowed ->
                DynamicTest.dynamicTest("G6 " + action.name + " / " + role + (allowed ? " serving committee" : " foreign committee"), () -> {
                    scope(role, allowed);
                    Response response = action.call.get();
                    boolean mutated = mockingDetails(service).getInvocations().stream()
                            .anyMatch(call -> MUTATIONS.contains(call.getMethod().getName()));
                    if (allowed) {
                        assertTrue(response.getStatus() >= 200 && response.getStatus() < 300, "Serving secretariat must retain this action");
                        assertTrue(mutated, "The allowed action must reach the service");
                    } else {
                        assertEquals(403, response.getStatus(), "A global role must not confer access to another committee");
                        assertFalse(mutated, "Denied requests must not mutate records or send invitations");
                    }
                }))));
    }
}
