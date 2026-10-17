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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Changes allowed after submission: the meeting date and the quorum roster may change until
 * the item is approved, by the owning analyst or its review analyst, keeping the item's status; a document
 * uploaded during or after review has to be reviewed; and the booking drafts.
 */
class AgendaScheduleChangeTest {
    private static final String ANALYST = "analyst@rmb.co.za";
    private static final String REVIEWER = "reviewer@rmb.co.za";
    private static final AgendaAccess REVIEWER_ROLE = new AgendaAccess(false, false, true, false);

    private final ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);
    private final AgendaMailService mail = mock(AgendaMailService.class);
    private final AgendaServiceImpl service = new AgendaServiceImpl(repository, mail,
            mock(ApprovalForumListManagementService.class), mock(AgendaCalendarService.class));

    private ApprovalForumAgcAgendaCommitteeEntity committee;
    private ApprovalForumAgmAgendaMeetingEntity meeting;
    private ApprovalForumAgiAgendaItemEntity item;
    private final List<ApprovalForumAgpAgendaParticipantEntity> participants = new ArrayList<>();
    private final List<ApprovalForumAgidAgendaItemDocumentEntity> documents = new ArrayList<>();

    private static Timestamp nextWeekday(DayOfWeek day, int weeks) {
        return Timestamp.valueOf(LocalDate.now().with(TemporalAdjusters.next(day)).plusWeeks(weeks).atStartOfDay());
    }

    @BeforeEach
    void setUp() {
        committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1);
        committee.setActive(true);
        committee.setCommitteeName("CCC");
        committee.setMandateLevel("L3");
        committee.setQuorumSize(2);
        committee.setQuorumRoles("Chair;IndependentMember");
        committee.setMeetingSecretaryId("secretary@rmb.co.za");
        committee.setRequiredDocTypes("Credit Paper");

        meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10);
        meeting.setStatus("Draft");
        meeting.setActive(true);
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(nextWeekday(DayOfWeek.TUESDAY, 1));
        meeting.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().plusDays(3)));

        item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(100);
        item.setActive(true);
        item.setItemStatus("Submitted");
        item.setAgmAgendaMeetingByAgmId(meeting);
        item.setCounterpartyName("Acme Ltd");
        item.setCreatedBy(ANALYST);
        item.setPrimaryAnalyst("A Analyst");
        item.setPrimaryAnalystId(ANALYST);
        item.setReviewAnalyst("R Reviewer");
        item.setReviewAnalystId(REVIEWER);
        item.setEstimatedMinutes(30);
        item.setStartTime("09:00");
        item.setEndTime("09:30");
        item.setAgendaNumber("3");

        participants.add(participant("Chair", "chair@rmb.co.za"));
        participants.add(participant("IndependentMember", "independent@rmb.co.za"));

        lenient().when(repository.getItem(100)).thenReturn(item);
        lenient().when(repository.getItemForUpdate(100)).thenReturn(item);
        lenient().when(repository.getCommittee(1)).thenReturn(committee);
        lenient().when(repository.saveItem(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveMeeting(any())).thenAnswer(inv -> {
            ApprovalForumAgmAgendaMeetingEntity m = inv.getArgument(0);
            if (m.getAgmId() == 0) m.setAgmId(11);
            return m;
        });
        lenient().when(repository.saveWorkflowAction(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveDocument(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.getDocumentsForItem(100)).thenReturn(documents);
        lenient().when(repository.getParticipantsForItem(100)).thenAnswer(inv -> new ArrayList<>(participants));
        lenient().when(repository.saveParticipant(any())).thenAnswer(inv -> {
            ApprovalForumAgpAgendaParticipantEntity p = inv.getArgument(0);
            if (p.isActive()) participants.add(p); else participants.remove(p);
            return p;
        });
        lenient().when(repository.getWorkflowActionsForItem(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(repository.getItemsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(repository.saveBookingDraft(any())).thenAnswer(inv -> {
            ApprovalForumAgbdAgendaBookingDraftEntity d = inv.getArgument(0);
            if (d.getAgbdId() == 0) d.setAgbdId(7);
            return d;
        });
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

    private ApprovalForumAgidAgendaItemDocumentEntity document(String type, String status, int version) {
        ApprovalForumAgidAgendaItemDocumentEntity d = new ApprovalForumAgidAgendaItemDocumentEntity();
        d.setAgiAgendaItemByAgiId(item);
        d.setDocType(type);
        d.setValidationStatus(status);
        d.setVersion(version);
        d.setActive(true);
        return d;
    }

    private MoveAgendaItemRequest move(Timestamp date, String start) {
        MoveAgendaItemRequest request = new MoveAgendaItemRequest();
        request.setMeetingDate(date);
        request.setStartTime(start);
        request.setReason("Chair unavailable");
        return request;
    }

    private List<ApprovalForumAgwAgendaWorkflowActionEntity> recordedActions() {
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> captor =
                ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository, atLeastOnce()).saveWorkflowAction(captor.capture());
        return captor.getAllValues();
    }

    // ---- Who may change the date and roster ----

    @Test
    @DisplayName("The owning analyst and the assigned review analyst may change the date and roster until approval")
    void ownerAndAssignedReviewerMayChangeSchedule() {
        for (String status : new String[] { "Captured", "Confirmed", "Submitted", "Returned", "Reviewed" }) {
            item.setItemStatus(status);
            assertTrue(service.canChangeSchedule(100, ANALYST, AgendaAccess.none()), status);
            assertTrue(service.canChangeSchedule(100, REVIEWER, REVIEWER_ROLE), status);
        }
        for (String status : new String[] { "Approved", "Rejected", "Removed" }) {
            item.setItemStatus(status);
            assertFalse(service.canChangeSchedule(100, ANALYST, AgendaAccess.none()), status);
            assertFalse(service.canChangeSchedule(100, REVIEWER, REVIEWER_ROLE), status);
        }
    }

    @Test
    @DisplayName("Another reviewer, the assigned reviewer without the role, or a locked meeting may not")
    void othersAndClosedMeetingsMayNotChangeSchedule() {
        assertFalse(service.canChangeSchedule(100, "other.reviewer@rmb.co.za", REVIEWER_ROLE));
        assertFalse(service.canChangeSchedule(100, REVIEWER, AgendaAccess.none()));
        for (String status : new String[] { "Locked", "Held", "Closed", "Cancelled" }) {
            meeting.setStatus(status);
            assertFalse(service.canChangeSchedule(100, ANALYST, AgendaAccess.none()), status);
        }
    }

    // ---- Moving to another meeting date ----

    @Test
    @DisplayName("A submitted item moves to another date: same status, new meeting, numbered again, audited and mailed")
    void submittedItemMovesToAnotherDate() {
        Timestamp later = nextWeekday(DayOfWeek.TUESDAY, 2);
        when(repository.findMeeting(eq(1), eq(later))).thenReturn(null);

        AgendaItemDTO moved = service.moveItem(100, move(later, "10:00"), REVIEWER);

        assertEquals("Submitted", moved.getItemStatus());
        assertEquals("10:00", moved.getStartTime());
        assertEquals("10:30", moved.getEndTime());
        assertEquals(11, item.getAgmAgendaMeetingByAgmId().getAgmId(), "a meeting is created for the new date");
        assertEquals(later, item.getAgmAgendaMeetingByAgmId().getMeetingDate());
        assertNull(item.getAgendaNumber(), "the running order number belonged to the old meeting");
        ApprovalForumAgwAgendaWorkflowActionEntity action = recordedActions().get(0);
        assertEquals("Reschedule", action.getAction());
        assertEquals("Submitted", action.getFromStatus());
        assertEquals("Submitted", action.getToStatus());
        assertTrue(action.getComments().contains("10:00-10:30"), action.getComments());
        assertTrue(action.getComments().contains("Reason: Chair unavailable"), action.getComments());
        verify(mail).sendItemChangedMail(any(), eq(REVIEWER + ";secretary@rmb.co.za"), eq("meeting date"), anyString());
    }

    @Test
    @DisplayName("Moving onto an existing meeting checks its slots and keeps the duration")
    void moveChecksTheNewMeetingsSlots() {
        Timestamp later = nextWeekday(DayOfWeek.TUESDAY, 2);
        ApprovalForumAgmAgendaMeetingEntity other = new ApprovalForumAgmAgendaMeetingEntity();
        other.setAgmId(20);
        other.setStatus("Compiled");
        other.setAgcAgendaCommitteeByAgcId(committee);
        other.setMeetingDate(later);
        other.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().plusDays(5)));
        when(repository.findMeeting(eq(1), eq(later))).thenReturn(other);
        ApprovalForumAgiAgendaItemEntity booked = new ApprovalForumAgiAgendaItemEntity();
        booked.setAgiId(200);
        booked.setItemStatus("Confirmed");
        booked.setStartTime("09:00");
        booked.setEndTime("10:00");
        booked.setCounterpartyName("Booked Co");
        when(repository.getItemsForMeeting(20)).thenReturn(List.of(booked));

        IllegalStateException clash = assertThrows(IllegalStateException.class, () -> service.moveItem(100, move(later, "09:30"), ANALYST));
        assertTrue(clash.getMessage().contains("overlaps"), clash.getMessage());
        assertSame(meeting, item.getAgmAgendaMeetingByAgmId(), "a refused move changes nothing");

        service.moveItem(100, move(later, "10:00"), ANALYST);
        assertSame(other, item.getAgmAgendaMeetingByAgmId());
        verify(repository, never()).saveMeeting(any());
    }

    @Test
    @DisplayName("A move is refused for an approved item, into the past, onto a weekend, a locked meeting, or to the same slot")
    void moveRefusals() {
        Timestamp later = nextWeekday(DayOfWeek.TUESDAY, 2);
        item.setItemStatus("Approved");
        assertThrows(IllegalStateException.class, () -> service.moveItem(100, move(later, null), ANALYST));
        item.setItemStatus("Reviewed");
        assertThrows(IllegalStateException.class, () -> service.moveItem(100,
                move(Timestamp.valueOf(LocalDate.now().minusDays(7).atStartOfDay()), null), ANALYST));
        assertThrows(IllegalStateException.class, () -> service.moveItem(100, move(nextWeekday(DayOfWeek.SATURDAY, 1), null), ANALYST));
        ApprovalForumAgmAgendaMeetingEntity locked = new ApprovalForumAgmAgendaMeetingEntity();
        locked.setAgmId(30);
        locked.setStatus("Locked");
        when(repository.findMeeting(eq(1), eq(later))).thenReturn(locked);
        assertThrows(IllegalStateException.class, () -> service.moveItem(100, move(later, null), ANALYST));
        when(repository.findMeeting(eq(1), eq(meeting.getMeetingDate()))).thenReturn(meeting);
        IllegalStateException same = assertThrows(IllegalStateException.class,
                () -> service.moveItem(100, move(meeting.getMeetingDate(), "09:00"), ANALYST));
        assertTrue(same.getMessage().contains("already booked"), same.getMessage());
        verify(repository, never()).saveItem(any());
    }

    @Test
    @DisplayName("Moving to a meeting whose paper deadline has passed needs a late submission reason")
    void moveAfterTheNewCutOffNeedsALateReason() {
        Timestamp soon = nextWeekday(DayOfWeek.MONDAY, 0);
        ApprovalForumAgmAgendaMeetingEntity late = new ApprovalForumAgmAgendaMeetingEntity();
        late.setAgmId(40);
        late.setStatus("Draft");
        late.setMeetingDate(soon);
        late.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
        when(repository.findMeeting(eq(1), eq(soon))).thenReturn(late);
        assertThrows(IllegalStateException.class, () -> service.moveItem(100, move(soon, null), ANALYST));
        MoveAgendaItemRequest withReason = move(soon, null);
        withReason.setLateSubmissionReason("Deal timing");
        service.moveItem(100, withReason, ANALYST);
        assertTrue(item.isLateSubmission());
        assertEquals("Deal timing", item.getLateSubmissionReason());
    }

    @Test
    @DisplayName("Before submission a move is audited but nobody is mailed")
    void captureStageMoveIsNotMailed() {
        item.setItemStatus("Confirmed");
        Timestamp later = nextWeekday(DayOfWeek.TUESDAY, 2);
        service.moveItem(100, move(later, null), ANALYST);
        assertEquals("09:00", item.getStartTime(), "the start time stays when none is given");
        verifyNoInteractions(mail);
    }

    // ---- Changing the roster ----

    private AgendaItemDTO roster(String... rolesAndIds) {
        AgendaItemDTO dto = new AgendaItemDTO();
        List<AgendaParticipantDTO> list = new ArrayList<>();
        for (int i = 0; i < rolesAndIds.length; i += 2) {
            AgendaParticipantDTO p = new AgendaParticipantDTO();
            p.setCommitteeRole(rolesAndIds[i]);
            p.setPersonId(rolesAndIds[i + 1]);
            p.setPersonName(rolesAndIds[i + 1]);
            p.setQuorumEligible(true);
            list.add(p);
        }
        dto.setParticipants(list);
        dto.setChair("New Chair");
        dto.setAttendeeEmails("newchair@rmb.co.za");
        return dto;
    }

    @Test
    @DisplayName("A submitted item's roster changes: status kept, quorum re-counted, audited and mailed")
    void submittedRosterChanges() {
        item.setAttendeeEmails("chair@rmb.co.za");
        AgendaItemDTO saved = service.updateRoster(100,
                roster("Chair", "newchair@rmb.co.za", "IndependentMember", "independent@rmb.co.za"), ANALYST);
        assertEquals("Submitted", saved.getItemStatus());
        assertEquals("New Chair", item.getChair());
        assertEquals("chair@rmb.co.za;newchair@rmb.co.za", item.getAttendeeEmails(), "no invitee is dropped");
        assertTrue(saved.getQuorum().isMet());
        ApprovalForumAgwAgendaWorkflowActionEntity action = recordedActions().get(0);
        assertEquals("RosterChanged", action.getAction());
        assertTrue(action.getComments().contains("quorum 2 of 2"), action.getComments());
        verify(mail).sendItemChangedMail(any(), anyString(), eq("quorum roster"), anyString());
    }

    @Test
    @DisplayName("A roster that no longer meets quorum blocks review and approval until it does")
    void quorumIsCheckedAgainBeforeReviewAndApproval() {
        documents.add(document("Credit Paper", "Accepted", 1));
        service.updateRoster(100, roster("Chair", "newchair@rmb.co.za"), ANALYST);
        assertTrue(recordedActions().get(0).getComments().contains("quorum is not met"));
        IllegalStateException review = assertThrows(IllegalStateException.class, () -> service.completeReview(100, "ok", REVIEWER));
        assertTrue(review.getMessage().contains("Quorum is not met"), review.getMessage());
        item.setItemStatus("Reviewed");
        assertThrows(IllegalStateException.class, () -> service.approveItem(100, "ok", "chair@rmb.co.za"));
        item.setQuorumOverrideReason("Approved exception");
        assertEquals("Approved", service.approveItem(100, "ok", "chair@rmb.co.za").getItemStatus());
    }

    @Test
    @DisplayName("The roster of an approved item, or on a locked meeting, cannot change")
    void rosterRefusals() {
        item.setItemStatus("Approved");
        assertThrows(IllegalStateException.class, () -> service.updateRoster(100, roster("Chair", "x@rmb.co.za"), ANALYST));
        item.setItemStatus("Submitted");
        meeting.setStatus("Locked");
        assertThrows(IllegalStateException.class, () -> service.updateRoster(100, roster("Chair", "x@rmb.co.za"), ANALYST));
        verify(repository, never()).saveItem(any());
    }

    // ---- Documents added during review ----

    @Test
    @DisplayName("A document uploaded after the review sends the item back to the reviewer")
    void documentAfterReviewReopensTheReview() {
        item.setItemStatus("Reviewed");
        documents.add(document("Credit Paper", "Accepted", 1));
        AgendaItemDTO saved = service.uploadDocument(100, "Term Sheet", "terms.pdf", ".pdf", new byte[] { 1 }, ANALYST);
        assertEquals("Submitted", saved.getItemStatus());
        ApprovalForumAgwAgendaWorkflowActionEntity action = recordedActions().get(0);
        assertEquals("DocumentAdded", action.getAction());
        assertEquals("Reviewed", action.getFromStatus());
        assertEquals("Submitted", action.getToStatus());
        verify(mail).sendItemChangedMail(any(), eq(REVIEWER + ";secretary@rmb.co.za"), eq("new document"), anyString());
    }

    @Test
    @DisplayName("The review cannot complete while a document added to the item is still pending")
    void reviewWaitsForEveryNewDocument() {
        documents.add(document("Credit Paper", "Accepted", 1));
        documents.add(document("Term Sheet", "Pending", 1));
        IllegalStateException pending = assertThrows(IllegalStateException.class, () -> service.completeReview(100, "ok", REVIEWER));
        assertTrue(pending.getMessage().contains("Term Sheet v1"), pending.getMessage());
        documents.get(1).setValidationStatus("Accepted");
        assertEquals("Reviewed", service.completeReview(100, "ok", REVIEWER).getItemStatus());
    }

    @Test
    @DisplayName("A document uploaded while the item is being captured changes nothing in the workflow")
    void documentBeforeSubmissionIsNotAReviewEvent() {
        item.setItemStatus("Confirmed");
        documents.add(document("Credit Paper", "Pending", 1));
        assertEquals("Confirmed", service.uploadDocument(100, "Term Sheet", "t.pdf", ".pdf", new byte[] { 1 }, ANALYST).getItemStatus());
        verify(repository, never()).saveWorkflowAction(any());
        verifyNoInteractions(mail);
    }

    // ---- Booking drafts ----

    @Test
    @DisplayName("A draft is saved, updated and deleted only by its owner")
    void draftsBelongToTheirOwner() {
        AgendaBookingDraftDTO dto = new AgendaBookingDraftDTO();
        dto.setAgcId(1);
        dto.setCounterpartyName("  Acme Ltd  ");
        dto.setStep(2);
        dto.setPayload("{\"step\":2}");
        AgendaBookingDraftDTO saved = service.saveBookingDraft(dto, ANALYST);
        assertEquals(7, saved.getAgbdId());
        assertEquals("Acme Ltd", saved.getCounterpartyName());
        assertEquals("CCC - L3", saved.getCommitteeName());

        ApprovalForumAgbdAgendaBookingDraftEntity stored = new ApprovalForumAgbdAgendaBookingDraftEntity();
        stored.setAgbdId(7);
        stored.setOwnerId(ANALYST);
        stored.setActive(true);
        stored.setPayload("{\"step\":2}");
        when(repository.getBookingDraft(7)).thenReturn(stored);

        assertThrows(IllegalArgumentException.class, () -> service.getBookingDraft(7, "someone.else@rmb.co.za"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteBookingDraft(7, "someone.else@rmb.co.za"));
        dto.setAgbdId(7);
        dto.setPayload("{\"step\":3}");
        dto.setStep(3);
        assertEquals("{\"step\":3}", service.saveBookingDraft(dto, ANALYST).getPayload());
        assertNotNull(stored.getUpdatedOn());
        service.deleteBookingDraft(7, ANALYST);
        assertFalse(stored.isActive());
        assertThrows(IllegalArgumentException.class, () -> service.getBookingDraft(7, ANALYST));
    }

    @Test
    @DisplayName("An empty draft is refused; booking from a draft discards it")
    void emptyDraftsAreRefusedAndBookingDiscardsTheDraft() {
        assertThrows(IllegalStateException.class, () -> service.saveBookingDraft(new AgendaBookingDraftDTO(), ANALYST));

        ApprovalForumAgbdAgendaBookingDraftEntity stored = new ApprovalForumAgbdAgendaBookingDraftEntity();
        stored.setAgbdId(7);
        stored.setOwnerId(ANALYST);
        stored.setActive(true);
        when(repository.getBookingDraft(7)).thenReturn(stored);
        when(repository.findMeeting(eq(1), any())).thenReturn(meeting);
        AgendaItemDTO dto = new AgendaItemDTO();
        dto.setCounterpartyName("Acme Ltd");
        dto.setEstimatedMinutes(30);
        dto.setStartTime("11:00");
        dto.setReviewAnalyst("R Reviewer");
        dto.setReviewAnalystId(REVIEWER);
        BookAgendaItemRequest request = new BookAgendaItemRequest();
        request.setAgcId(1);
        request.setMeetingDate(meeting.getMeetingDate());
        request.setItem(dto);
        request.setDraftId(7);
        service.bookItem(request, ANALYST);
        assertFalse(stored.isActive());
    }
}
