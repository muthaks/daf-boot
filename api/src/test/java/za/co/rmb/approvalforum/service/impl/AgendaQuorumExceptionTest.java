package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.*;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * A quorum exception is asked for by the item's analyst and decided by the committee's secretary or
 * chair, separately from submission: the analyst still submits, and an approved exception lets the
 * item through without full quorum.
 */
class AgendaQuorumExceptionTest {
    private static final String ANALYST = "analyst@rmb.co.za";
    private static final String REVIEWER = "reviewer@rmb.co.za";
    private static final String SECRETARY = "secretary@rmb.co.za";
    private static final String CHAIR = "chair@rmb.co.za";
    private static final AgendaAccess SECRETARY_ROLE = new AgendaAccess(true, false, false, false);
    private static final AgendaAccess CHAIR_ROLE = new AgendaAccess(false, true, false, false);

    private final ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);
    private final AgendaMailService mail = mock(AgendaMailService.class);
    private final AgendaServiceImpl service = new AgendaServiceImpl(repository, mail,
            mock(ApprovalForumListManagementService.class), mock(AgendaCalendarService.class));

    private ApprovalForumAgcAgendaCommitteeEntity committee;
    private ApprovalForumAgmAgendaMeetingEntity meeting;
    private ApprovalForumAgiAgendaItemEntity item;
    private final List<ApprovalForumAgpAgendaParticipantEntity> participants = new ArrayList<>();
    private final List<ApprovalForumAgidAgendaItemDocumentEntity> documents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1);
        committee.setActive(true);
        committee.setCommitteeName("CCC");
        committee.setMandateLevel("L3");
        committee.setQuorumSize(2);
        committee.setQuorumRoles("Chair;IndependentMember");
        committee.setMeetingSecretaryId(SECRETARY);
        committee.setChairId(CHAIR);
        committee.setRequiredDocTypes("Credit Paper");

        meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10);
        meeting.setStatus("Draft");
        meeting.setActive(true);
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(Timestamp.valueOf(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY)).plusWeeks(1).atStartOfDay()));
        meeting.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().plusDays(3)));

        item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(100);
        item.setActive(true);
        item.setItemStatus("Confirmed");
        item.setAgmAgendaMeetingByAgmId(meeting);
        item.setCounterpartyName("Acme Ltd");
        item.setPurposeOfSubmission("Annual review");
        item.setBusinessUnit("Corporates");
        item.setCreatedBy(ANALYST);
        item.setPrimaryAnalyst("A Analyst");
        item.setPrimaryAnalystId(ANALYST);
        item.setReviewAnalyst("R Reviewer");
        item.setReviewAnalystId(REVIEWER);
        item.setEstimatedMinutes(30);
        item.setStartTime("09:00");
        item.setEndTime("09:30");

        participants.add(participant("Chair", CHAIR));
        documents.add(document());

        lenient().when(repository.getItem(100)).thenReturn(item);
        lenient().when(repository.getItemForUpdate(100)).thenReturn(item);
        lenient().when(repository.getCommittee(1)).thenReturn(committee);
        lenient().when(repository.saveItem(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveWorkflowAction(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.getDocumentsForItem(anyInt())).thenReturn(documents);
        lenient().when(repository.getParticipantsForItem(anyInt())).thenAnswer(inv -> new ArrayList<>(participants));
        lenient().when(repository.saveParticipant(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.getWorkflowActionsForItem(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(repository.getItemsForMeeting(anyInt())).thenReturn(new ArrayList<>());
    }

    private ApprovalForumAgpAgendaParticipantEntity participant(String role, String id) {
        ApprovalForumAgpAgendaParticipantEntity p = new ApprovalForumAgpAgendaParticipantEntity();
        p.setAgiAgendaItemByAgiId(item);
        p.setPersonId(id);
        p.setPersonName(id);
        p.setCommitteeRole(role);
        p.setQuorumEligible(true);
        p.setActive(true);
        return p;
    }

    private ApprovalForumAgidAgendaItemDocumentEntity document() {
        ApprovalForumAgidAgendaItemDocumentEntity d = new ApprovalForumAgidAgendaItemDocumentEntity();
        d.setAgiAgendaItemByAgiId(item);
        d.setDocType("Credit Paper");
        d.setValidationStatus("Pending");
        d.setVersion(1);
        d.setActive(true);
        return d;
    }

    private List<ApprovalForumAgwAgendaWorkflowActionEntity> recordedActions() {
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> captor =
                ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository, atLeastOnce()).saveWorkflowAction(captor.capture());
        return captor.getAllValues();
    }

    private String conflict(org.junit.jupiter.api.function.Executable call) {
        return assertThrows(IllegalStateException.class, call).getMessage();
    }

    // ---- The analyst asks ----

    @Test
    @DisplayName("The analyst asks: Requested with the reason, audited, and the secretary and chair are emailed")
    void analystRequestsAnException() {
        assertTrue(service.canRequestQuorumException(100, ANALYST));

        AgendaItemDTO saved = service.requestQuorumException(100, "  Member on leave  ", ANALYST);

        assertEquals("Requested", saved.getQuorumExceptionStatus());
        assertEquals("Member on leave", saved.getQuorumExceptionReason());
        assertEquals(ANALYST, saved.getQuorumExceptionRequestedBy());
        assertNotNull(saved.getQuorumExceptionRequestedOn());
        assertNull(saved.getQuorumOverrideReason(), "a request is not yet an exception");
        assertEquals("Confirmed", saved.getItemStatus(), "asking does not change the item's status");
        ApprovalForumAgwAgendaWorkflowActionEntity action = recordedActions().get(0);
        assertEquals("QuorumExceptionRequested", action.getAction());
        assertEquals("Member on leave", action.getComments());
        verify(mail).sendQuorumExceptionRequestedMail(any(), eq(SECRETARY + ";" + CHAIR), any());
        assertFalse(service.canRequestQuorumException(100, ANALYST), "one request at a time");
    }

    @Test
    @DisplayName("Only the analyst, before submission, without quorum, with a reason, and not twice")
    void requestIsRefusedOutsideItsRules() {
        assertFalse(service.canRequestQuorumException(100, REVIEWER));
        assertTrue(conflict(() -> service.requestQuorumException(100, "Leave", REVIEWER)).contains("analyst"));
        assertTrue(conflict(() -> service.requestQuorumException(100, "  ", ANALYST)).contains("reason"));
        assertTrue(conflict(() -> service.requestQuorumException(100, "x".repeat(1001), ANALYST)).contains("1000"));

        item.setItemStatus("Submitted");
        assertFalse(service.canRequestQuorumException(100, ANALYST));
        assertTrue(conflict(() -> service.requestQuorumException(100, "Leave", ANALYST)).contains("before the item is submitted"));

        item.setItemStatus("Confirmed");
        meeting.setStatus("Locked");
        assertTrue(conflict(() -> service.requestQuorumException(100, "Leave", ANALYST)).contains("no longer open"));

        meeting.setStatus("Draft");
        participants.add(participant("IndependentMember", "independent@rmb.co.za"));
        assertFalse(service.canRequestQuorumException(100, ANALYST));
        assertTrue(conflict(() -> service.requestQuorumException(100, "Leave", ANALYST)).contains("Quorum is met"));
        verifyNoInteractions(mail);
    }

    // ---- The secretary or chair decides ----

    @Test
    @DisplayName("The committee's secretary approves: the reason becomes the override and the analysts are emailed")
    void secretaryApproves() {
        service.requestQuorumException(100, "Member on leave", ANALYST);
        assertTrue(service.canDecideQuorumException(100, SECRETARY, SECRETARY_ROLE));
        assertTrue(service.canDecideQuorumException(100, CHAIR, CHAIR_ROLE));

        AgendaItemDTO saved = service.decideQuorumException(100, true, null, SECRETARY, SECRETARY_ROLE);

        assertEquals("Approved", saved.getQuorumExceptionStatus());
        assertEquals("Member on leave", saved.getQuorumOverrideReason());
        assertEquals(ANALYST, saved.getQuorumExceptionRequestedBy(), "who asked is kept");
        assertEquals(SECRETARY, saved.getQuorumExceptionDecidedBy());
        ApprovalForumAgwAgendaWorkflowActionEntity action = recordedActions().get(1);
        assertEquals("QuorumExceptionApproved", action.getAction());
        assertTrue(action.isOverrideFlag());
        assertEquals("Member on leave", action.getOverrideReason());
        verify(mail).sendQuorumExceptionDecidedMail(any(), eq(true), isNull());
        assertFalse(service.canDecideQuorumException(100, SECRETARY, SECRETARY_ROLE), "already decided");
        assertFalse(service.canRequestQuorumException(100, ANALYST), "already approved");
    }

    @Test
    @DisplayName("A decline needs a reason; the analyst may then ask again")
    void chairDeclines() {
        service.requestQuorumException(100, "Member on leave", ANALYST);
        assertTrue(conflict(() -> service.decideQuorumException(100, false, " ", CHAIR, CHAIR_ROLE)).contains("why"));

        AgendaItemDTO saved = service.decideQuorumException(100, false, "Find a second portfolio CCE", CHAIR, CHAIR_ROLE);

        assertEquals("Declined", saved.getQuorumExceptionStatus());
        assertEquals("Find a second portfolio CCE", saved.getQuorumExceptionDecisionNote());
        assertNull(saved.getQuorumOverrideReason());
        verify(mail).sendQuorumExceptionDecidedMail(any(), eq(false), eq("Find a second portfolio CCE"));
        assertTrue(service.canRequestQuorumException(100, ANALYST));
    }

    @Test
    @DisplayName("Another committee's secretary, the role without serving, or no pending request cannot decide")
    void onlyThisCommitteesSecretaryOrChairDecides() {
        assertTrue(conflict(() -> service.decideQuorumException(100, true, null, SECRETARY, SECRETARY_ROLE)).contains("no quorum exception request"));
        service.requestQuorumException(100, "Member on leave", ANALYST);
        assertFalse(service.canDecideQuorumException(100, "other.secretary@rmb.co.za", SECRETARY_ROLE));
        assertFalse(service.canDecideQuorumException(100, SECRETARY, AgendaAccess.none()));
        assertFalse(service.canDecideQuorumException(100, ANALYST, AgendaAccess.none()));
        assertTrue(conflict(() -> service.decideQuorumException(100, true, null, "other.secretary@rmb.co.za", SECRETARY_ROLE))
                .contains("secretary or chair"));
    }

    // ---- Submission ----

    @Test
    @DisplayName("Without quorum the analyst cannot submit until the exception is approved, then can")
    void approvedExceptionLetsTheAnalystSubmit() {
        String waiting = conflict(() -> service.submitItem(100, null, ANALYST));
        assertTrue(waiting.contains("ask the committee secretary or chair"), waiting);

        service.requestQuorumException(100, "Member on leave", ANALYST);
        assertTrue(conflict(() -> service.submitItem(100, null, ANALYST)).contains("still waiting"));

        service.decideQuorumException(100, true, null, CHAIR, CHAIR_ROLE);
        AgendaItemDTO submitted = service.submitItem(100, null, ANALYST);

        assertEquals("Submitted", submitted.getItemStatus());
        assertEquals("Member on leave", submitted.getQuorumOverrideReason(), "the approved exception travels with the submission");
    }

    @Test
    @DisplayName("The secretary or chair can still submit with their own reason, which records an approved exception")
    void secretarySubmitsWithOwnReason() {
        AgendaItemDTO submitted = service.submitItem(100, "Chair agreed at short notice", SECRETARY, SECRETARY_ROLE);
        assertEquals("Submitted", submitted.getItemStatus());
        assertEquals("Approved", item.getQuorumExceptionStatus());
        assertEquals(SECRETARY, item.getQuorumExceptionDecidedBy());
        assertEquals("Chair agreed at short notice", item.getQuorumOverrideReason());
    }

    @Test
    @DisplayName("A secretary or chair who does not serve this committee cannot approve an exception by submitting")
    void otherCommitteeSecretaryCannotSubmitWithReason() {
        assertTrue(conflict(() -> service.submitItem(100, "Not my committee", "other.secretary@rmb.co.za", SECRETARY_ROLE))
                .contains("Only this committee's secretary or chair"));
        assertTrue(conflict(() -> service.submitItem(100, "No role", SECRETARY, AgendaAccess.none()))
                .contains("Only this committee's secretary or chair"));
        assertNull(item.getQuorumExceptionStatus());
        assertEquals("Confirmed", item.getItemStatus());
    }

    // ---- Booking ----

    private BookAgendaItemRequest booking(String reason) {
        AgendaItemDTO dto = new AgendaItemDTO();
        dto.setCounterpartyName("Acme Ltd");
        dto.setPurposeOfSubmission("Annual review");
        dto.setPrimaryAnalyst("A Analyst");
        dto.setPrimaryAnalystId(ANALYST);
        dto.setReviewAnalyst("R Reviewer");
        dto.setReviewAnalystId(REVIEWER);
        dto.setStartTime("09:00");
        dto.setEstimatedMinutes(30);
        dto.setQuorumOverrideReason(reason);
        BookAgendaItemRequest request = new BookAgendaItemRequest();
        request.setAgcId(1);
        request.setMeetingDate(meeting.getMeetingDate());
        request.setItem(dto);
        return request;
    }

    private void bookingSetUp() {
        when(repository.findMeeting(eq(1), any())).thenReturn(meeting);
        when(repository.saveItem(any())).thenAnswer(inv -> {
            ApprovalForumAgiAgendaItemEntity saved = inv.getArgument(0);
            if (saved.getAgiId() == 0) saved.setAgiId(123);
            return saved;
        });
    }

    @Test
    @DisplayName("An analyst booking without quorum, with a reason, books the item and asks for the exception")
    void analystBookingAsksForTheException() {
        bookingSetUp();
        AgendaItemDTO saved = service.bookItem(booking("Member on leave"), ANALYST, AgendaAccess.none());

        assertEquals("Captured", saved.getItemStatus());
        assertEquals("Requested", saved.getQuorumExceptionStatus());
        assertNull(saved.getQuorumOverrideReason(), "an analyst's reason is a request, not an exception");
        verify(mail).sendQuorumExceptionRequestedMail(any(), eq(SECRETARY + ";" + CHAIR), any());
    }

    @Test
    @DisplayName("This committee's secretary booking without quorum approves the exception as they book")
    void secretaryBookingApprovesTheException() {
        bookingSetUp();
        AgendaItemDTO saved = service.bookItem(booking("Member on leave"), SECRETARY, SECRETARY_ROLE);

        assertEquals("Approved", saved.getQuorumExceptionStatus());
        assertEquals("Member on leave", saved.getQuorumOverrideReason());
        verify(mail, never()).sendQuorumExceptionRequestedMail(any(), any(), any());
    }

    @Test
    @DisplayName("Booking without quorum and without a reason is allowed; with quorum a reason is ignored")
    void bookingWithoutReasonOrWithQuorum() {
        bookingSetUp();
        assertNull(service.bookItem(booking(null), ANALYST, AgendaAccess.none()).getQuorumExceptionStatus());

        participants.add(participant("IndependentMember", "independent@rmb.co.za"));
        AgendaItemDTO met = service.bookItem(booking("Not needed"), ANALYST, AgendaAccess.none());
        assertNull(met.getQuorumExceptionStatus());
        assertNull(met.getQuorumOverrideReason());
        verify(mail, never()).sendQuorumExceptionRequestedMail(any(), any(), any());
    }
}
