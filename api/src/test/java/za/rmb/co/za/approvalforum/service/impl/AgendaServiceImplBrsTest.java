package za.rmb.co.za.approvalforum.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.agenda.AgendaConditionDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaQuorumResultDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaReportSummaryDTO;
import za.co.rmb.approvalforum.model.agenda.BookAgendaItemRequest;
import za.co.rmb.approvalforum.service.AgendaCalendarService;
import za.co.rmb.approvalforum.service.AgendaAccess;
import za.co.rmb.approvalforum.model.agenda.ReassignAgendaReviewerRequest;
import za.co.rmb.approvalforum.service.AgendaMailService;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;
import za.co.rmb.approvalforum.service.impl.AgendaServiceImpl;

import java.lang.reflect.Constructor;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BRS alignment rules in AgendaServiceImpl (CASDM-1352): late detection (RULE-003), the review /
 * approval state machine (BR-010), role-based quorum (RULE-005), readiness (RULE-002), committee
 * cut-off configuration, close / re-open (RULE-015) and reporting aggregates.
 */
@ExtendWith(MockitoExtension.class)
public class AgendaServiceImplBrsTest {

    @Mock
    private ApprovalForumAgendaRepository repository;
    @Mock
    private AgendaMailService mailService;
    @Mock
    private ApprovalForumListManagementService listManagementService;
    @Mock
    private AgendaCalendarService calendarService;

    private AgendaServiceImpl service;

    private ApprovalForumAgcAgendaCommitteeEntity committee;
    private ApprovalForumAgmAgendaMeetingEntity meeting;
    private ApprovalForumAgiAgendaItemEntity item;

    @BeforeEach
    void setUp() throws Exception {
        // The constructor is package-private (Guice-injected); the test lives in another package.
        Constructor<AgendaServiceImpl> ctor = AgendaServiceImpl.class.getDeclaredConstructor(
                ApprovalForumAgendaRepository.class, AgendaMailService.class,
                ApprovalForumListManagementService.class, AgendaCalendarService.class);
        ctor.setAccessible(true);
        service = ctor.newInstance(repository, mailService, listManagementService, calendarService);

        committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1);
        committee.setActive(true);
        committee.setCommitteeName("Corporates Credit Committee (CCC)");
        committee.setMandateLevel("L3");
        committee.setQuorumSize(2);
        committee.setMeetingDay("Tuesday");
        committee.setCutOffDayOfWeek("Thursday");
        committee.setCutOffTime("17:00");
        committee.setRequiredDocTypes("Credit Paper");
        committee.setQuorumRoles("Chair;IndependentMember;PortfolioCce1");

        meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10);
        meeting.setActive(true);
        meeting.setStatus("Draft");
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(Timestamp.valueOf(LocalDateTime.now().plusDays(7)));
        meeting.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().plusDays(2)));

        item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(100);
        item.setActive(true);
        item.setItemStatus("Confirmed");
        item.setAgmAgendaMeetingByAgmId(meeting);
        item.setCounterpartyName("Acme Ltd");
        item.setPurposeOfSubmission("Annual review");
        item.setBusinessUnit("Corporates");
        item.setPrimaryAnalyst("A Analyst");
        item.setPrimaryAnalystId("analyst@rmb.co.za");
        item.setReviewAnalyst("Review Analyst");
        item.setReviewAnalystId("reviewer@rmb.co.za");
        item.setEstimatedMinutes(30);

        lenient().when(repository.getItem(100)).thenReturn(item);
        lenient().when(repository.getItemForUpdate(100)).thenReturn(item);
        lenient().when(repository.saveItem(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveMeeting(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.saveWorkflowAction(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(creditPaper()));
        lenient().when(repository.getParticipantsForItem(100)).thenReturn(participants("Chair", "IndependentMember"));
        lenient().when(repository.getWorkflowActionsForItem(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(repository.getConditionsForItem(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(repository.getMeeting(10)).thenReturn(meeting);
        lenient().when(repository.getMeetingForUpdate(10)).thenReturn(meeting);
        lenient().when(repository.getItemsForMeeting(10)).thenReturn(Collections.singletonList(item));
        lenient().when(repository.getRecordsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(repository.getWorkflowActionsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        lenient().when(calendarService.isConfigured()).thenReturn(false);
    }

    private ApprovalForumAgidAgendaItemDocumentEntity creditPaper() {
        ApprovalForumAgidAgendaItemDocumentEntity doc = new ApprovalForumAgidAgendaItemDocumentEntity();
        doc.setAgidId(5);
        doc.setAgiAgendaItemByAgiId(item);
        doc.setDocType("Credit Paper");
        doc.setActive(true);
        doc.setValidationStatus("Pending");
        doc.setVersion(1);
        return doc;
    }

    private List<ApprovalForumAgpAgendaParticipantEntity> participants(String... roles) {
        List<ApprovalForumAgpAgendaParticipantEntity> list = new ArrayList<>();
        int i = 1;
        for (String role : roles) {
            ApprovalForumAgpAgendaParticipantEntity p = new ApprovalForumAgpAgendaParticipantEntity();
            p.setAgpId(i);
            p.setAgiAgendaItemByAgiId(item);
            p.setPersonName("Person " + i++);
            p.setCommitteeRole(role);
            p.setQuorumEligible(true);
            p.setActive(true);
            list.add(p);
        }
        return list;
    }

    // ---- RULE-003: late submission is system-determined and needs a reason ----

    // ---- Booking: the committee's meeting secretary is notified ----

    private BookAgendaItemRequest bookingRequest() {
        AgendaItemDTO dto = new AgendaItemDTO();
        dto.setCounterpartyName("Acme Ltd");
        dto.setPurposeOfSubmission("Annual review");
        dto.setEstimatedMinutes(30);
        dto.setStartTime("09:00");
        dto.setReviewAnalyst("Review Analyst");
        dto.setReviewAnalystId("reviewer@rmb.co.za");
        BookAgendaItemRequest request = new BookAgendaItemRequest();
        request.setAgcId(1);
        request.setMeetingDate(Timestamp.valueOf(LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY)).atStartOfDay()));
        request.setItem(dto);
        return request;
    }

    @Test
    @DisplayName("Booking without quorum: this committee's secretary records the exception; anyone else's reason is a request")
    void bookingRecordsOrRequestsTheQuorumException() {
        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.findMeeting(eq(1), any())).thenReturn(meeting);
        committee.setMeetingSecretaryId("secretary@rmb.co.za");
        BookAgendaItemRequest request = bookingRequest();
        request.getItem().setQuorumOverrideReason("  Alternate committee member pending  ");
        AgendaItemDTO saved = service.bookItem(request, "secretary@rmb.co.za", new AgendaAccess(true, false, false, false));
        assertEquals("Alternate committee member pending", saved.getQuorumOverrideReason());
        assertEquals("Approved", saved.getQuorumExceptionStatus());
        assertEquals("Captured", saved.getItemStatus());

        BookAgendaItemRequest asked = bookingRequest();
        asked.getItem().setQuorumOverrideReason("Alternate committee member pending");
        AgendaItemDTO requested = service.bookItem(asked, "secretary@rmb.co.za", AgendaAccess.none());
        assertNull(requested.getQuorumOverrideReason(), "without the role it is only a request");
        assertEquals("Requested", requested.getQuorumExceptionStatus());
    }

    @Test
    @DisplayName("Booking an item notifies the committee's meeting secretary")
    void bookingNotifiesSecretary() {
        committee.setMeetingSecretaryId(" secretary@rmb.co.za ");
        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.findMeeting(eq(1), any())).thenReturn(meeting);
        service.bookItem(bookingRequest(), "analyst@rmb.co.za");
        verify(mailService).sendItemCapturedMail(any(AgendaItemDTO.class), eq("secretary@rmb.co.za"));
    }

    @Test
    @DisplayName("Booking on a committee without a secretary notifies only the analysts")
    void bookingWithoutSecretary() {
        committee.setMeetingSecretaryId(null);
        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.findMeeting(eq(1), any())).thenReturn(meeting);
        service.bookItem(bookingRequest(), "analyst@rmb.co.za");
        verify(mailService).sendItemCapturedMail(any(AgendaItemDTO.class), eq(""));
    }

    @Test
    @DisplayName("An item cannot be booked on a past date")
    void bookingInThePastIsRefused() {
        when(repository.getCommittee(1)).thenReturn(committee);
        BookAgendaItemRequest request = bookingRequest();
        request.setMeetingDate(Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay()));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.bookItem(request, "analyst@rmb.co.za"));
        assertTrue(ex.getMessage().contains("past"));
        verify(repository, never()).saveItem(any());
    }

    @Test
    @DisplayName("A meeting cannot be created on a past date; today is allowed")
    void creatingMeetingInThePastIsRefused() {
        when(repository.getCommittee(1)).thenReturn(committee);
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgcId(1);
        dto.setMeetingDate(Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay()));
        assertThrows(IllegalStateException.class, () -> service.createMeeting(dto, "secretary"));
        verify(repository, never()).saveMeeting(any());

        dto.setMeetingDate(Timestamp.valueOf(LocalDate.now().atStartOfDay()));
        service.createMeeting(dto, "secretary");
        verify(repository).saveMeeting(any());
    }

    @Test
    @DisplayName("A meeting cannot be moved into the past, but a past meeting keeps its date when edited")
    void movingMeetingIntoThePastIsRefused() {
        AgendaMeetingDTO move = new AgendaMeetingDTO();
        move.setAgmId(10);
        move.setMeetingDate(Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay()));
        assertThrows(IllegalStateException.class, () -> service.updateMeeting(move, "secretary"));
        verify(repository, never()).saveMeeting(any());

        Timestamp yesterday = Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay());
        meeting.setMeetingDate(yesterday);
        AgendaMeetingDTO edit = new AgendaMeetingDTO();
        edit.setAgmId(10);
        edit.setMeetingDate(yesterday);
        edit.setVenue("Boardroom 3");
        service.updateMeeting(edit, "secretary");
        assertEquals("Boardroom 3", meeting.getVenue());
    }

    @Test
    @DisplayName("A meeting's end time is its start time plus the committee's duration, never entered")
    void meetingEndTimeIsCalculated() {
        committee.setDefaultDurationMinutes(180);
        when(repository.getCommittee(1)).thenReturn(committee);
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgcId(1);
        dto.setMeetingDate(Timestamp.valueOf(LocalDate.now().plusDays(7).atStartOfDay()));
        dto.setStartTime("09:00");
        dto.setEndTime("09:15");
        assertEquals("12:00", service.createMeeting(dto, "secretary").getEndTime());

        AgendaMeetingDTO edit = new AgendaMeetingDTO();
        edit.setAgmId(10);
        edit.setStartTime("10:30");
        edit.setEndTime("11:00");
        service.updateMeeting(edit, "secretary");
        assertEquals("13:30", meeting.getEndTime());

        committee.setDefaultDurationMinutes(null);
        AgendaMeetingDTO venue = new AgendaMeetingDTO();
        venue.setAgmId(10);
        venue.setVenue("Boardroom 3");
        service.updateMeeting(venue, "secretary");
        assertEquals("14:30", meeting.getEndTime());
    }

    @Test
    @DisplayName("Submitting after the cut-off without a reason is refused")
    void submitAfterCutOffNeedsReason() {
        meeting.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
        item.setLateSubmissionReason(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.submitItem(100, null, "abc"));
        assertTrue(ex.getMessage().contains("late submission reason is required"));
    }

    @Test
    @DisplayName("Submitting after the cut-off with a reason flags the item late")
    void submitAfterCutOffWithReasonIsLate() {
        meeting.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
        item.setLateSubmissionReason("Board date moved");
        AgendaItemDTO dto = service.submitItem(100, null, "abc");
        assertTrue(dto.isLateSubmission());
        assertEquals("Submitted", dto.getItemStatus());
    }

    @Test
    @DisplayName("Submitting before the cut-off is not late even if the analyst ticked the flag")
    void submitBeforeCutOffNotLate() {
        item.setLateSubmission(true);
        AgendaItemDTO dto = service.submitItem(100, null, "abc");
        assertFalse(dto.isLateSubmission());
    }

    // ---- BR-010: workflow state machine ----

    @Test
    @DisplayName("Submit records a workflow action and notifies reviewers")
    void submitRecordsActionAndMails() {
        service.submitItem(100, null, "abc");
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository).saveWorkflowAction(captor.capture());
        assertEquals("Submit", captor.getValue().getAction());
        assertEquals("Confirmed", captor.getValue().getFromStatus());
        assertEquals("Submitted", captor.getValue().getToStatus());
        assertEquals("abc", captor.getValue().getCreatedBy());
        verify(mailService).sendItemSubmittedMail(any(AgendaItemDTO.class), any());
    }

    @Test
    @DisplayName("Only a submitted item can be approved, returned or rejected")
    void onlySubmittedCanBeReviewed() {
        assertThrows(IllegalStateException.class, () -> service.approveItem(100, null, "chair"));
        assertThrows(IllegalStateException.class, () -> service.returnItem(100, "fix", "rev"));
        assertThrows(IllegalStateException.class, () -> service.rejectItem(100, "no", "chair"));
    }

    @Test
    @DisplayName("Return requires a reason and sends the item back to the analyst")
    void returnNeedsReason() {
        item.setItemStatus("Submitted");
        assertThrows(IllegalStateException.class, () -> service.returnItem(100, " ", "rev"));
        AgendaItemDTO dto = service.returnItem(100, "Pages exceed guidance", "rev");
        assertEquals("Returned", dto.getItemStatus());
        verify(mailService).sendItemReturnedMail(any(AgendaItemDTO.class), eq("Pages exceed guidance"));
    }

    @Test
    @DisplayName("Approve moves a submitted item onto the agenda and signs off a late item")
    void approveMovesToApproved() {
        item.setItemStatus("Reviewed");
        item.setLateSubmission(true);
        ApprovalForumAgidAgendaItemDocumentEntity accepted = creditPaper();
        accepted.setValidationStatus("Accepted");
        when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(accepted));
        AgendaItemDTO dto = service.approveItem(100, "ok", "chair");
        assertEquals("Approved", dto.getItemStatus());
        assertEquals("chair", dto.getLateApprovedBy());
        assertNotNull(dto.getLateApprovedOn());
    }

    @Test
    @DisplayName("Submitted / approved items are read-only for the analyst")
    void submittedItemsAreReadOnly() {
        item.setItemStatus("Submitted");
        AgendaItemDTO dto = new AgendaItemDTO();
        dto.setAgiId(100);
        assertThrows(IllegalStateException.class, () -> service.updateItem(dto, "abc"));
    }

    @Test
    @DisplayName("Re-opening an approved item on a locked agenda is recorded as an override (RULE-009)")
    void reopenOnLockedAgendaIsOverride() {
        item.setItemStatus("Approved");
        meeting.setStatus("Locked");
        AgendaItemDTO dto = service.reopenItem(100, "Chair requested change", "sec");
        assertEquals("Confirmed", dto.getItemStatus());
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository).saveWorkflowAction(captor.capture());
        assertTrue(captor.getValue().isOverrideFlag());
        assertEquals("Chair requested change", captor.getValue().getOverrideReason());
    }

    @Test
    @DisplayName("A decided document version cannot be reversed or reset to Pending")
    void documentDecisionsAreFinalForTheVersion() {
        for (String decided : Arrays.asList("Accepted", "Rejected")) {
            ApprovalForumAgidAgendaItemDocumentEntity document = creditPaper();
            document.setValidationStatus(decided);
            document.setValidatedBy("original-reviewer");
            when(repository.getDocumentForValidation(5)).thenReturn(document);
            for (String next : Arrays.asList("Accepted", "Rejected", "Pending")) {
                if (decided.equals(next)) continue;
                assertThrows(IllegalStateException.class, () -> service.validateDocument(5, next, "Change", "reviewer"));
            }
            assertEquals(decided, document.getValidationStatus());
            assertEquals("original-reviewer", document.getValidatedBy());
        }
        verify(repository, never()).saveDocument(any());
    }

    @Test
    @DisplayName("Repeating the same document decision preserves the original audit details without another write")
    void documentDecisionRetriesAreIdempotent() {
        for (String decided : Arrays.asList("Accepted", "Rejected")) {
            ApprovalForumAgidAgendaItemDocumentEntity document = creditPaper();
            document.setValidationStatus(decided);
            document.setValidatedBy("original-reviewer");
            document.setValidationComment("Original reason");
            Timestamp recorded = Timestamp.valueOf("2026-10-10 14:00:00");
            document.setValidatedOn(recorded);
            when(repository.getDocumentForValidation(5)).thenReturn(document);
            service.validateDocument(5, decided, "New comment", "another-reviewer");
            assertEquals("original-reviewer", document.getValidatedBy());
            assertEquals("Original reason", document.getValidationComment());
            assertEquals(recorded, document.getValidatedOn());
        }
        verify(repository, never()).saveDocument(any());
    }

    @Test
    @DisplayName("A fresh Pending version can be reviewed, but a superseded version cannot")
    void onlyFreshDocumentVersionsCanBeReviewed() {
        ApprovalForumAgidAgendaItemDocumentEntity document = creditPaper();
        document.setVersion(2);
        when(repository.getDocumentForValidation(5)).thenReturn(document);
        when(repository.saveDocument(any())).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("Accepted", service.validateDocument(5, "Accepted", "Checked", "reviewer").getValidationStatus());
        document.setValidationStatus("Superseded");
        assertThrows(IllegalStateException.class, () -> service.validateDocument(5, "Accepted", "Checked", "reviewer"));
        verify(repository, times(1)).saveDocument(any());
    }

    @Test
    @DisplayName("Accepted required documents close Return to Analyst without saving or sending notifications")
    void acceptedDocumentReviewCannotBeReturned() {
        item.setItemStatus("Reviewed");
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.returnItem(100, "Change", "reviewer"));
        assertTrue(failure.getMessage().contains("Only a submitted"));
        assertEquals("Reviewed", item.getItemStatus());
        verify(repository, never()).saveItem(any());
        verify(repository, never()).saveWorkflowAction(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Approval waits for all configured required document types, rather than one accepted attachment")
    void approvalRequiresEveryConfiguredDocumentType() {
        item.setItemStatus("Reviewed");
        committee.setRequiredDocTypes("Credit Paper;Annexure");
        ApprovalForumAgidAgendaItemDocumentEntity accepted = creditPaper();
        accepted.setValidationStatus("Accepted");
        ApprovalForumAgidAgendaItemDocumentEntity annexure = creditPaper();
        annexure.setAgidId(6); annexure.setDocType("Annexure");
        when(repository.getDocumentsForItem(100)).thenReturn(Arrays.asList(accepted, annexure));
        assertThrows(IllegalStateException.class, () -> service.approveItem(100, "ok", "chair"));
        assertEquals("Reviewed", item.getItemStatus());
        verify(repository, never()).saveItem(any());
        annexure.setValidationStatus("Accepted");
        AgendaItemDTO approved = service.approveItem(100, "ok", "chair");
        assertEquals("Approved", approved.getItemStatus());
        assertEquals(Arrays.asList("Credit Paper", "Annexure"), approved.getRequiredDocumentTypes());
    }

    @Test
    @DisplayName("A pending required annexure leaves Return to Analyst available after the credit paper is accepted")
    void incompleteRequiredReviewCanStillReturnForCorrection() {
        item.setItemStatus("Submitted");
        committee.setRequiredDocTypes("Credit Paper;Annexure");
        ApprovalForumAgidAgendaItemDocumentEntity accepted = creditPaper(); accepted.setValidationStatus("Accepted");
        ApprovalForumAgidAgendaItemDocumentEntity annexure = creditPaper(); annexure.setDocType("Annexure");
        when(repository.getDocumentsForItem(100)).thenReturn(Arrays.asList(accepted, annexure));
        assertEquals("Returned", service.returnItem(100, "Annexure needs correction", "reviewer").getItemStatus());
    }

    @Test
    @DisplayName("An accepted optional attachment does not complete mandatory document review")
    void optionalAcceptanceCannotCompleteDocumentReview() {
        item.setItemStatus("Submitted");
        ApprovalForumAgidAgendaItemDocumentEntity optional = creditPaper();
        optional.setDocType("Supporting Document"); optional.setValidationStatus("Accepted");
        when(repository.getDocumentsForItem(100)).thenReturn(Arrays.asList(creditPaper(), optional));
        assertThrows(IllegalStateException.class, () -> service.approveItem(100, "ok", "chair"));
        assertEquals("Returned", service.returnItem(100, "Credit paper incomplete", "reviewer").getItemStatus());
    }

    @Test
    @DisplayName("Accepting documents leaves the whole submitted item reviewable and returnable")
    void documentAcceptanceDoesNotCompleteSubmissionReview() {
        item.setItemStatus("Submitted");
        ApprovalForumAgidAgendaItemDocumentEntity accepted = creditPaper(); accepted.setValidationStatus("Accepted");
        when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(accepted));
        assertThrows(IllegalStateException.class, () -> service.approveItem(100, "not reviewed", "chair"));
        assertEquals("Returned", service.returnItem(100, "Correct the deal details", "reviewer").getItemStatus());
    }

    @Test
    @DisplayName("Resubmission starts a fresh whole-item review even when the existing document remains accepted")
    void resubmissionRequiresFreshIndependentReview() {
        item.setItemStatus("Submitted");
        ApprovalForumAgidAgendaItemDocumentEntity accepted = creditPaper(); accepted.setValidationStatus("Accepted");
        when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(accepted));
        assertEquals("Reviewed", service.completeReview(100, "Whole submission checked", "reviewer").getItemStatus());
        assertThrows(IllegalStateException.class, () -> service.completeReview(100, "duplicate", "reviewer"));
        assertEquals("Confirmed", service.reopenItem(100, "Correction requested", "secretary").getItemStatus());
        assertEquals("Submitted", service.submitItem(100, null, "analyst").getItemStatus());
        assertThrows(IllegalStateException.class, () -> service.approveItem(100, "premature", "chair"));
        assertEquals("Reviewed", service.completeReview(100, "Rechecked amended item", "reviewer").getItemStatus());
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> actions = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository, atLeast(2)).saveWorkflowAction(actions.capture());
        assertEquals(2, actions.getAllValues().stream().filter(action -> "Review".equals(action.getAction())).count());
        verify(mailService, times(2)).sendItemReviewedMail(any(), any());
    }

    @Test
    @DisplayName("Whole-item review cannot finish before all mandatory document checks are accepted")
    void completingReviewRequiresAcceptedMandatoryDocuments() {
        item.setItemStatus("Submitted");
        assertThrows(IllegalStateException.class, () -> service.completeReview(100, "ok", "reviewer"));
        verify(repository, never()).saveItem(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Notification history exposes the reason from its own return event, even when email delivery is disabled")
    void notificationHistoryIncludesItsOwnRecordedReason() {
        ApprovalForumAgnAgendaNotificationEntity notification = new ApprovalForumAgnAgendaNotificationEntity();
        notification.setAgnId(1); notification.setAgiId(100); notification.setNotificationType("Returned");
        notification.setSentOn(Timestamp.valueOf("2026-10-17 16:52:03")); notification.setStatus("Disabled");
        ApprovalForumAgwAgendaWorkflowActionEntity old = new ApprovalForumAgwAgendaWorkflowActionEntity();
        old.setActive(true); old.setToStatus("Returned"); old.setComments("Old reason"); old.setCreatedOn(Timestamp.valueOf("2026-10-10 14:28:00"));
        ApprovalForumAgwAgendaWorkflowActionEntity current = new ApprovalForumAgwAgendaWorkflowActionEntity();
        current.setAgwId(9); current.setActive(true); current.setToStatus("Returned"); current.setCreatedBy("reviewer");
        current.setComments("Correct the proposed rating"); current.setCreatedOn(Timestamp.valueOf("2026-10-17 16:52:02"));
        when(repository.getNotificationsForItem(100)).thenReturn(Collections.singletonList(notification));
        when(repository.getWorkflowActionsForItem(100)).thenReturn(Arrays.asList(old, current));
        za.co.rmb.approvalforum.model.agenda.AgendaNotificationDTO result = service.getNotificationsForItem(100).get(0);
        assertEquals("Correct the proposed rating", result.getReason()); assertEquals("reviewer", result.getActionBy());
        assertEquals(Integer.valueOf(9), result.getWorkflowActionId()); assertEquals("Disabled", result.getStatus());
        assertTrue(result.getMessage().contains("submit the item for review again"));
    }

    @Test
    @DisplayName("A historical notification without a matching audit event never borrows another return's reason")
    void notificationHistoryDoesNotInventReasons() {
        ApprovalForumAgnAgendaNotificationEntity notification = new ApprovalForumAgnAgendaNotificationEntity();
        notification.setAgiId(100); notification.setNotificationType("Returned"); notification.setSentOn(Timestamp.valueOf("2026-10-17 16:52:03"));
        ApprovalForumAgwAgendaWorkflowActionEntity old = new ApprovalForumAgwAgendaWorkflowActionEntity();
        old.setActive(true); old.setToStatus("Returned"); old.setComments("Old reason"); old.setCreatedOn(Timestamp.valueOf("2026-10-10 14:28:00"));
        when(repository.getNotificationsForItem(100)).thenReturn(Collections.singletonList(notification));
        when(repository.getWorkflowActionsForItem(100)).thenReturn(Collections.singletonList(old));
        assertNull(service.getNotificationsForItem(100).get(0).getReason());
    }

    @Test
    @DisplayName("Approved date comes from the most recent approval audit, not creation or meeting date")
    void approvalDateUsesLatestRecordedDecision() {
        item.setItemStatus("Approved");
        ApprovalForumAgwAgendaWorkflowActionEntity older = new ApprovalForumAgwAgendaWorkflowActionEntity();
        older.setActive(true); older.setAction("Approve"); older.setCreatedOn(Timestamp.valueOf("2026-10-10 14:00:00"));
        ApprovalForumAgwAgendaWorkflowActionEntity latest = new ApprovalForumAgwAgendaWorkflowActionEntity();
        latest.setActive(true); latest.setAction("Approve"); latest.setCreatedOn(Timestamp.valueOf("2026-10-17 16:00:00"));
        when(repository.getWorkflowActionsForItem(100)).thenReturn(Arrays.asList(latest, older));
        assertEquals(latest.getCreatedOn(), service.getItem(100).getApprovedOn());
    }

    @Test
    @DisplayName("After whole-item review completes even optional pending document checks are closed")
    void reviewedSubmissionClosesPendingDocumentActions() {
        item.setItemStatus("Reviewed");
        ApprovalForumAgidAgendaItemDocumentEntity optional = creditPaper(); optional.setDocType("Supporting Document");
        when(repository.getDocumentForValidation(5)).thenReturn(optional);
        assertThrows(IllegalStateException.class, () -> service.validateDocument(5, "Accepted", "", "reviewer"));
        assertEquals("Pending", optional.getValidationStatus()); verify(repository, never()).saveDocument(any());
    }

    // ---- RULE-001 / RULE-002: readiness ----

    @Test
    @DisplayName("Submit is refused while mandatory fields are missing")
    void submitNeedsMandatoryFields() {
        item.setPurposeOfSubmission(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.submitItem(100, null, "abc"));
        assertTrue(ex.getMessage().contains("purpose of submission"));
    }

    @Test
    @DisplayName("Submit is refused while a mandatory paper is missing or superseded")
    void submitNeedsMandatoryPapers() {
        ApprovalForumAgidAgendaItemDocumentEntity old = creditPaper();
        old.setValidationStatus("Superseded");
        when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(old));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.submitItem(100, null, "abc"));
        assertTrue(ex.getMessage().contains("Credit Paper"));
    }

    // ---- RULE-005: role-based quorum ----

    @Test
    @DisplayName("Quorum counts only eligible participants in the committee's quorum roles")
    void quorumCountsEligibleRolesOnly() {
        List<ApprovalForumAgpAgendaParticipantEntity> list = participants("Chair", "Scribe", "Member", "PortfolioCce1");
        list.get(3).setQuorumEligible(false);
        when(repository.getParticipantsForItem(100)).thenReturn(list);
        AgendaQuorumResultDTO q = service.evaluateQuorum(100);
        assertEquals(1, q.getEligibleCount());
        assertEquals(2, q.getRequired());
        assertFalse(q.isMet());
        assertTrue(q.getMissingRoles().contains("IndependentMember"));
        assertTrue(q.getMissingRoles().contains("PortfolioCce1"));
    }

    @Test
    @DisplayName("Quorum falls back to the legacy roster columns when no participant rows exist")
    void quorumUsesLegacyRoster() {
        when(repository.getParticipantsForItem(100)).thenReturn(new ArrayList<>());
        item.setChair("C Chair");
        item.setIndependentMember("I Member; J Member");
        AgendaQuorumResultDTO q = service.evaluateQuorum(100);
        assertEquals(3, q.getEligibleCount());
        assertTrue(q.isMet());
    }

    @Test
    @DisplayName("Submit without quorum needs an override reason, which is recorded")
    void submitWithoutQuorumNeedsOverride() {
        when(repository.getParticipantsForItem(100)).thenReturn(participants("Chair"));
        assertThrows(IllegalStateException.class, () -> service.submitItem(100, null, "abc"));
        committee.setMeetingSecretaryId("sec");
        AgendaItemDTO dto = service.submitItem(100, "Chair accepted reduced quorum", "sec",
                new za.co.rmb.approvalforum.service.AgendaAccess(true, false, false, false));
        assertEquals("Chair accepted reduced quorum", dto.getQuorumOverrideReason());
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository, times(2)).saveWorkflowAction(captor.capture());
        assertEquals("QuorumExceptionApproved", captor.getAllValues().get(0).getAction());
        assertEquals("Submit", captor.getValue().getAction());
        assertTrue(captor.getValue().isOverrideFlag());
    }

    private ReassignAgendaReviewerRequest reassignment(String newId) {
        ReassignAgendaReviewerRequest request = new ReassignAgendaReviewerRequest();
        request.setReviewerId(newId);
        request.setReviewerName("New reviewer");
        request.setExpectedReviewerId(item.getReviewAnalystId());
        request.setReason("Original reviewer is unavailable");
        return request;
    }

    private AgendaAccess secretariat() { return new AgendaAccess(true, false, false, false); }

    private void pendingReassignment() {
        committee.setMeetingSecretaryId("sec@example.test");
        item.setItemStatus("Submitted");
    }

    @Test
    void reviewerReassignmentPreservesSubmissionAndDocumentDecisionsAndMovesVisibility() {
        pendingReassignment();
        Timestamp submittedOn = Timestamp.valueOf("2026-10-17 17:54:00");
        item.setSubmittedOn(submittedOn);
        item.setSubmittedBy("booker@example.test");
        ApprovalForumAgidAgendaItemDocumentEntity document = creditPaper();
        document.setValidationStatus("Accepted");
        document.setValidatedBy("previous-reviewer@example.test");
        when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(document));
        when(repository.getAllItems()).thenReturn(Collections.singletonList(item));
        ReassignAgendaReviewerRequest request = reassignment("new-reviewer@example.test");
        String oldId = item.getReviewAnalystId();
        AgendaItemDTO saved = service.reassignReviewer(100, request, "sec@example.test", secretariat());
        assertEquals("Submitted", saved.getItemStatus());
        assertEquals(submittedOn, saved.getSubmittedOn());
        assertEquals("booker@example.test", saved.getSubmittedBy());
        assertEquals("new-reviewer@example.test", saved.getReviewAnalystId());
        assertEquals("Accepted", document.getValidationStatus());
        assertEquals("previous-reviewer@example.test", document.getValidatedBy());
        AgendaAccess reviewer = new AgendaAccess(false, false, true, false);
        assertTrue(service.getVisibleItems(oldId, reviewer).isEmpty());
        assertEquals(1, service.getVisibleItems("new-reviewer@example.test", reviewer).size());
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> audit = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository).saveWorkflowAction(audit.capture());
        assertEquals("ReassignReviewer", audit.getValue().getAction());
        assertTrue(audit.getValue().getComments().contains(oldId));
        assertTrue(audit.getValue().getComments().contains(request.getReviewerId()));
        assertEquals(request.getReason(), audit.getValue().getOverrideReason());
        verify(repository).getItemForUpdate(100);
        verify(repository, never()).saveDocument(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void reviewerReassignmentRequiresCommitteeScopeNotJustRole() {
        pendingReassignment();
        for (AgendaAccess roles : Arrays.asList(AgendaAccess.none(), new AgendaAccess(false,false,true,false), new AgendaAccess(false,false,false,true))) {
            assertFalse(service.canReassignReviewer(100, "sec@example.test", roles));
            assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, reassignment("new@example.test"), "sec@example.test", roles));
        }
        assertFalse(service.canReassignReviewer(100, "other-secretary@example.test", secretariat()));
        assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, reassignment("new@example.test"), "other-secretary@example.test", secretariat()));
        verify(repository, never()).saveItem(any());
    }

    @Test
    void committeeChairMayReassignPendingReview() {
        pendingReassignment();
        committee.setChairId("chair@example.test");
        assertEquals("new@example.test", service.reassignReviewer(100, reassignment("new@example.test"), "chair@example.test", new AgendaAccess(false,true,false,false)).getReviewAnalystId());
    }

    @Test
    void reviewerReassignmentRequiresPendingSubmissionAndOpenMeeting() {
        pendingReassignment();
        for (String status : Arrays.asList("Captured","Confirmed","Returned","Reviewed","Approved","Rejected","Removed")) {
            item.setItemStatus(status);
            assertFalse(service.canReassignReviewer(100, "sec@example.test", secretariat()));
            assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, reassignment("new@example.test"), "sec@example.test", secretariat()));
        }
        item.setItemStatus("Submitted");
        for (String status : Arrays.asList("Locked","Held","Closed","Cancelled")) {
            meeting.setStatus(status);
            assertFalse(service.canReassignReviewer(100, "sec@example.test", secretariat()));
            assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, reassignment("new@example.test"), "sec@example.test", secretariat()));
        }
        verify(repository, never()).saveItem(any());
    }

    @Test
    void reviewerReassignmentRejectsAuthorPrimarySponsorAndBuHead() {
        pendingReassignment();
        item.setCreatedBy("booker@example.test");
        item.setCreditSponsor("Sponsor"); item.setCreditSponsorId("sponsor@example.test");
        item.setBuHead("BU head"); item.setBuHeadId("bu@example.test");
        String original = item.getReviewAnalystId();
        for (String id : Arrays.asList(item.getCreatedBy(),item.getPrimaryAnalystId(),item.getCreditSponsorId(),item.getBuHeadId())) {
            assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, reassignment(id), "sec@example.test", secretariat()));
            assertEquals(original, item.getReviewAnalystId());
        }
        verify(repository, never()).saveItem(any());
    }

    @Test
    void reassignmentRequiresReasonAndDirectoryIdentity() {
        pendingReassignment();
        ReassignAgendaReviewerRequest request = reassignment("new@example.test"); request.setReason(" ");
        assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, request, "sec@example.test", secretariat()));
        request.setReason("Unavailable"); request.setReviewerId(" ");
        assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, request, "sec@example.test", secretariat()));
        verify(repository, never()).saveItem(any());
    }

    @Test
    void staleReviewerReassignmentIsRejectedWithoutWrites() {
        pendingReassignment();
        ReassignAgendaReviewerRequest request = reassignment("new@example.test"); request.setExpectedReviewerId("outdated@example.test");
        assertThrows(IllegalStateException.class, () -> service.reassignReviewer(100, request, "sec@example.test", secretariat()));
        verify(repository, never()).saveItem(any());
    }

    @Test
    void identicalReassignmentRetryDoesNotDuplicateHistory() {
        pendingReassignment();
        ReassignAgendaReviewerRequest request = reassignment("new@example.test");
        service.reassignReviewer(100, request, "sec@example.test", secretariat());
        service.reassignReviewer(100, request, "sec@example.test", secretariat());
        verify(repository, times(1)).saveItem(any());
        verify(repository, times(1)).saveWorkflowAction(any());
    }

    // ---- Committee cut-off configuration ----

    @Test
    @DisplayName("Cut-off uses the committee's configured day and time, falling back to Thursday 17:00")
    void cutOffFollowsCommitteeConfig() {
        Timestamp meetingDate = Timestamp.valueOf(LocalDateTime.of(2026, 9, 22, 0, 0)); // Tuesday
        committee.setCutOffDayOfWeek("Friday");
        committee.setCutOffTime("12:30");
        Timestamp cutOff = service.cutOffFor(committee, meetingDate);
        assertEquals(LocalDateTime.of(2026, 9, 18, 12, 30), cutOff.toLocalDateTime());
        committee.setCutOffDayOfWeek(null);
        assertEquals(LocalDateTime.of(2026, 9, 17, 17, 0), service.cutOffFor(committee, meetingDate).toLocalDateTime());
    }

    // ---- Compile only approved items ----

    @Test
    @DisplayName("Compile refuses while items still await review")
    void compileRefusesPendingReview() {
        item.setItemStatus("Submitted");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.compileAgenda(10, "sec"));
        assertTrue(ex.getMessage().contains("await review"));
    }

    @Test
    @DisplayName("Compile numbers approved items only and leaves confirmed ones unnumbered")
    void compileNumbersApprovedOnly() {
        ApprovalForumAgiAgendaItemEntity approved = new ApprovalForumAgiAgendaItemEntity();
        approved.setAgiId(101);
        approved.setActive(true);
        approved.setItemStatus("Approved");
        approved.setEstimatedMinutes(30);
        approved.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
        approved.setAgmAgendaMeetingByAgmId(meeting);
        item.setCreatedOn(Timestamp.valueOf(LocalDateTime.now().minusMinutes(5)));
        when(repository.getItemsForMeeting(10)).thenReturn(Arrays.asList(item, approved));
        service.compileAgenda(10, "sec");
        assertEquals("3.1", approved.getAgendaNumber());
        assertNull(item.getAgendaNumber());
        assertEquals("Compiled", meeting.getStatus());
    }

    @Test
    @DisplayName("Repeating compile preserves the agenda without notifying attendees")
    void repeatedCompileIsHarmless() {
        item.setItemStatus("Approved");
        item.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
        service.compileAgenda(10, "sec");
        String agendaNumber = item.getAgendaNumber();
        String startTime = item.getStartTime();
        String endTime = item.getEndTime();
        AgendaMeetingDTO repeated = service.compileAgenda(10, "sec");
        assertEquals("Compiled", repeated.getStatus());
        assertEquals(agendaNumber, item.getAgendaNumber());
        assertEquals(startTime, item.getStartTime());
        assertEquals(endTime, item.getEndTime());
        verify(repository, times(1)).saveItem(item);
        verify(repository, times(1)).saveMeeting(meeting);
        verifyNoInteractions(mailService);
    }

    private void readyInvitation() {
        item.setAttendeeEmails("attendee@example.test");
        when(calendarService.isConfigured()).thenReturn(true);
        lenient().when(calendarService.createOrUpdateEvent(any(), any(), any(), any()))
                .thenReturn(new AgendaCalendarService.CalendarEvent("event-10", "https://teams.example.test/10"));
    }

    private void assertInvitationFailure() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.sendInvitation(10, "sec"));
        assertTrue(error.getMessage().contains("could not be sent"));
        assertEquals("Draft", meeting.getStatus());
        assertNull(meeting.getGraphEventId());
        assertNull(meeting.getInvitationSentOn());
        verify(repository, never()).saveMeeting(any());
        verify(repository, never()).saveWorkflowAction(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void lockRequiresInvitationAndDoesNotSendOne() {
        meeting.setStatus("Compiled");
        assertTrue(assertThrows(IllegalStateException.class, () -> service.lockMeeting(10, "sec")).getMessage().contains("Send the calendar invitation"));
        assertEquals("Compiled", meeting.getStatus());
        verifyNoInteractions(calendarService, mailService);
        verify(repository, never()).saveMeeting(any());
    }

    @Test
    void lockingWithInvitationDoesNotNotifyAttendees() {
        meeting.setStatus("Compiled");
        meeting.setGraphEventId("existing-event");
        assertEquals("Locked", service.lockMeeting(10, "sec").getStatus());
        verifyNoInteractions(calendarService, mailService);
        verify(repository).saveWorkflowAction(any());
    }

    @Test
    void invitationUnavailableDoesNotSaveSuccess() {
        item.setAttendeeEmails("attendee@example.test");
        assertInvitationFailure();
    }

    @Test
    void invitationNullResultDoesNotSaveSuccess() {
        readyInvitation();
        when(calendarService.createOrUpdateEvent(any(), any(), any(), any())).thenReturn(null);
        assertInvitationFailure();
    }

    @Test
    void invitationBlankIdDoesNotSaveSuccess() {
        readyInvitation();
        when(calendarService.createOrUpdateEvent(any(), any(), any(), any())).thenReturn(new AgendaCalendarService.CalendarEvent(" ", null));
        assertInvitationFailure();
    }

    @Test
    void invitationExceptionDoesNotSaveSuccess() {
        readyInvitation();
        when(calendarService.createOrUpdateEvent(any(), any(), any(), any())).thenThrow(new RuntimeException("test timeout"));
        assertInvitationFailure();
    }

    @Test
    void sendInvitationPreservesDraftAndRepeatedRequestIsHarmless() {
        readyInvitation();
        AgendaMeetingDTO sent = service.sendInvitation(10, "sec");
        assertEquals("Draft", sent.getStatus());
        assertEquals("event-10", sent.getGraphEventId());
        assertEquals("sec", sent.getInvitationSentBy());
        assertNotNull(sent.getInvitationSentOn());
        assertFalse(sent.isInvitationNeedsUpdate());
        assertFalse(service.getMeeting(10).isInvitationNeedsUpdate());
        service.sendInvitation(10, "sec");
        verify(calendarService, times(1)).createOrUpdateEvent(any(), any(), any(), any());
        verify(repository, times(1)).saveMeeting(meeting);
        verify(repository, times(1)).saveWorkflowAction(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void invitationWithoutAttendeesIsRejected() {
        assertTrue(assertThrows(IllegalStateException.class, () -> service.sendInvitation(10, "sec")).getMessage().contains("Add attendees"));
        verifyNoInteractions(calendarService);
    }

    @Test
    void reopenKeepsInvitationAndUpdateClearsPendingChanges() {
        readyInvitation();
        service.sendInvitation(10, "sec");
        meeting.setStatus("Compiled");
        service.lockMeeting(10, "sec");
        AgendaMeetingDTO reopened = service.setMeetingStatus(10, "Draft", "sec");
        assertEquals("event-10", reopened.getGraphEventId());
        assertTrue(reopened.isInvitationNeedsUpdate());
        verify(calendarService, never()).cancelEvent(any(), any());
        verify(calendarService, times(1)).createOrUpdateEvent(any(), any(), any(), any());
        AgendaMeetingDTO updated = service.sendInvitationUpdate(10, "sec");
        assertEquals("Draft", updated.getStatus());
        assertFalse(updated.isInvitationNeedsUpdate());
        assertFalse(service.getMeeting(10).isInvitationNeedsUpdate());
        service.sendInvitationUpdate(10, "sec");
        verify(calendarService, times(2)).createOrUpdateEvent(any(), any(), any(), any());
        verifyNoInteractions(mailService);
    }

    @Test
    void meetingChangesRemainUnsentUntilExplicitUpdate() {
        readyInvitation();
        service.sendInvitation(10, "sec");
        AgendaMeetingDTO edit = new AgendaMeetingDTO();
        edit.setAgmId(10);
        edit.setStartTime("09:00");
        edit.setEndTime("10:00");
        service.updateMeeting(edit, "sec");
        assertTrue(service.getMeeting(10).isInvitationNeedsUpdate());
        verify(calendarService, times(1)).createOrUpdateEvent(any(), any(), any(), any());
        service.sendInvitationUpdate(10, "sec");
        ArgumentCaptor<AgendaMeetingDTO> captured = ArgumentCaptor.forClass(AgendaMeetingDTO.class);
        verify(calendarService, times(2)).createOrUpdateEvent(captured.capture(), any(), any(), any());
        assertEquals("event-10", captured.getAllValues().get(1).getGraphEventId());
        assertEquals("09:00", captured.getAllValues().get(1).getStartTime());
        assertFalse(service.getMeeting(10).isInvitationNeedsUpdate());
    }

    @Test
    void agendaAndPaperVersionChangesAreDetected() {
        readyInvitation();
        item.setItemStatus("Approved");
        service.sendInvitation(10, "sec");
        item.setCounterpartyName("Updated deal");
        assertTrue(service.getMeeting(10).isInvitationNeedsUpdate());
        service.sendInvitationUpdate(10, "sec");
        assertFalse(service.getMeeting(10).isInvitationNeedsUpdate());
        ApprovalForumAgidAgendaItemDocumentEntity newPaper = creditPaper();
        newPaper.setAgidId(6);
        newPaper.setVersion(2);
        when(repository.getDocumentsForItem(100)).thenReturn(Collections.singletonList(newPaper));
        assertTrue(service.getMeeting(10).isInvitationNeedsUpdate());
    }

    @Test
    void attendeeChangesNeedAnExplicitUpdate() {
        readyInvitation();
        service.sendInvitation(10, "sec");
        item.setAttendeeEmails("attendee@example.test;another@example.test");
        assertTrue(service.getMeeting(10).isInvitationNeedsUpdate());
        verify(calendarService, times(1)).createOrUpdateEvent(any(), any(), any(), any());
        service.sendInvitationUpdate(10, "sec");
        assertFalse(service.getMeeting(10).isInvitationNeedsUpdate());
    }

    @Test
    void unnumberedItemsChangingQueryOrderDoNotProduceFalseUnsentChanges() {
        readyInvitation();
        item.setItemStatus("Approved");
        ApprovalForumAgiAgendaItemEntity second = new ApprovalForumAgiAgendaItemEntity();
        second.setAgiId(101);
        second.setActive(true);
        second.setItemStatus("Approved");
        second.setCounterpartyName("Second item");
        second.setAgmAgendaMeetingByAgmId(meeting);
        when(repository.getItemsForMeeting(10)).thenReturn(Arrays.asList(item, second));
        service.sendInvitation(10, "sec");
        when(repository.getItemsForMeeting(10)).thenReturn(Arrays.asList(second, item));
        assertFalse(service.getMeeting(10).isInvitationNeedsUpdate());
    }

    @Test
    void failedUpdateRetainsExistingInvitationAndPendingChanges() {
        readyInvitation();
        service.sendInvitation(10, "sec");
        meeting.setInvitationReopened(true);
        Timestamp sentOn = meeting.getInvitationSentOn();
        when(calendarService.createOrUpdateEvent(any(), any(), any(), any())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> service.sendInvitationUpdate(10, "sec"));
        assertEquals("event-10", meeting.getGraphEventId());
        assertEquals(sentOn, meeting.getInvitationSentOn());
        assertTrue(service.getMeeting(10).isInvitationNeedsUpdate());
        verify(repository, times(1)).saveMeeting(meeting);
        when(calendarService.createOrUpdateEvent(any(), any(), any(), any())).thenReturn(new AgendaCalendarService.CalendarEvent("event-10", null));
        assertFalse(service.sendInvitationUpdate(10, "sec").isInvitationNeedsUpdate());
    }

    @Test
    void updateRequiresExistingInvitation() {
        assertThrows(IllegalStateException.class, () -> service.sendInvitationUpdate(10, "sec"));
        verifyNoInteractions(calendarService);
    }

    @Test
    void heldAndCancelledMeetingsCannotSendInvitations() {
        for (String status : Arrays.asList("Held", "Closed", "Cancelled")) {
            meeting.setStatus(status);
            assertThrows(IllegalStateException.class, () -> service.sendInvitation(10, "sec"));
            assertThrows(IllegalStateException.class, () -> service.sendInvitationUpdate(10, "sec"));
        }
        verifyNoInteractions(calendarService);
    }

    @Test
    void genericStatusCannotBypassLockRequirements() {
        assertThrows(IllegalStateException.class, () -> service.setMeetingStatus(10, "Locked", "sec"));
        assertEquals("Draft", meeting.getStatus());
    }

    @Test
    void cancellationFailureKeepsMeetingAndInvitationActive() {
        meeting.setStatus("Locked");
        meeting.setGraphEventId("event-10");
        doThrow(new RuntimeException("test cancellation failure")).when(calendarService).cancelEvent(any(), any());
        assertThrows(IllegalStateException.class, () -> service.cancelMeeting(10, "No longer needed", "sec"));
        assertEquals("Locked", meeting.getStatus());
        verify(repository, never()).saveMeeting(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void cancelMeetingCancelsInvitationAndRecordsReason() {
        meeting.setGraphEventId("event-10");
        AgendaMeetingDTO cancelled = service.cancelMeeting(10, "No longer needed", "sec");
        assertEquals("Cancelled", cancelled.getStatus());
        verify(calendarService).cancelEvent(any(), eq("No longer needed"));
        verify(mailService).sendMeetingCancelledMail(any(), any(), eq("No longer needed"));
    }

    @Test
    void reinstatementRestoresEditingPreservesItemsAndAuditsReasonWithoutSending() {
        meeting.setStatus("Cancelled");
        Timestamp cancelledOn = Timestamp.valueOf("2026-10-17 18:30:00");
        meeting.setCancelledOn(cancelledOn);
        meeting.setCancelledBy("original-secretary");
        meeting.setCancelReason("Original cancellation");
        meeting.setGraphEventId("cancelled-event");
        meeting.setTeamsJoinUrl("https://teams.example.test/cancelled");
        meeting.setInvitationSnapshot("old-snapshot");
        meeting.setInvitationSentOn(cancelledOn);
        meeting.setInvitationSentBy("sender");
        meeting.setInvitationReopened(true);
        meeting.setLockedBy("chair");
        meeting.setLockedOn(cancelledOn);
        item.setItemStatus("Approved");
        item.setAgendaNumber("3.1");
        AgendaMeetingDTO restored = service.reinstateMeeting(10, "  Committee is available again  ", "sec");
        assertEquals("Draft", restored.getStatus());
        assertNull(restored.getGraphEventId());
        assertNull(restored.getTeamsJoinUrl());
        assertNull(restored.getInvitationSentOn());
        assertNull(restored.getInvitationSentBy());
        assertNull(meeting.getInvitationSnapshot());
        assertFalse(meeting.isInvitationReopened());
        assertNull(restored.getLockedOn());
        assertNull(restored.getLockedBy());
        assertEquals(cancelledOn, restored.getCancelledOn());
        assertEquals("original-secretary", restored.getCancelledBy());
        assertEquals("Original cancellation", restored.getCancelReason());
        assertEquals("Approved", item.getItemStatus());
        assertEquals("3.1", item.getAgendaNumber());
        verify(repository).getMeetingForUpdate(10);
        verify(repository, never()).saveItem(any());
        verify(repository, never()).saveDocument(any());
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> audit = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository).saveWorkflowAction(audit.capture());
        assertEquals("Reinstate", audit.getValue().getAction());
        assertEquals("Cancelled", audit.getValue().getFromStatus());
        assertEquals("Draft", audit.getValue().getToStatus());
        assertEquals("Committee is available again", audit.getValue().getComments());
        assertEquals("sec", audit.getValue().getCreatedBy());
        assertNotNull(audit.getValue().getCreatedOn());
        verifyNoInteractions(calendarService, mailService);
    }

    @Test
    void reinstatementNeedsAReasonAndRejectsOversizedReasonsWithoutMutation() {
        meeting.setStatus("Cancelled");
        assertThrows(IllegalStateException.class, () -> service.reinstateMeeting(10, " ", "sec"));
        assertThrows(IllegalStateException.class, () -> service.reinstateMeeting(10, String.join("", Collections.nCopies(1001, "x")), "sec"));
        assertEquals("Cancelled", meeting.getStatus());
        verify(repository, never()).saveMeeting(any());
        verify(repository, never()).saveWorkflowAction(any());
        verifyNoInteractions(calendarService, mailService);
    }

    @Test
    void onlyCancelledMeetingsCanBeReinstated() {
        for (String status : Arrays.asList("Draft", "Compiled", "Locked", "Held", "Closed")) {
            meeting.setStatus(status);
            assertThrows(IllegalStateException.class, () -> service.reinstateMeeting(10, "New plans", "sec"));
            assertEquals(status, meeting.getStatus());
        }
        verify(repository, never()).saveMeeting(any());
        verify(repository, never()).saveWorkflowAction(any());
        verifyNoInteractions(calendarService, mailService);
    }

    @Test
    void legacyCancelledMeetingGetsADurableNewInvitationCycle() {
        meeting.setStatus("Cancelled");
        assertNull(meeting.getCancelledOn());
        AgendaMeetingDTO restored = service.reinstateMeeting(10, "Resume the sitting", "sec");
        assertNotNull(restored.getCancelledOn());
        assertEquals(restored.getCancelledOn(), meeting.getCancelledOn());
        verifyNoInteractions(calendarService, mailService);
    }

    @Test
    void repeatedReinstatementDoesNotResetOrResendAnything() {
        meeting.setStatus("Cancelled");
        AgendaMeetingDTO restored = service.reinstateMeeting(10, "Resume the sitting", "sec");
        Timestamp cycle = restored.getCancelledOn();
        assertThrows(IllegalStateException.class, () -> service.reinstateMeeting(10, "Resume the sitting", "sec"));
        assertEquals("Draft", meeting.getStatus());
        assertEquals(cycle, meeting.getCancelledOn());
        verify(repository, times(1)).saveMeeting(any());
        verify(repository, times(1)).saveWorkflowAction(any());
        verifyNoInteractions(calendarService, mailService);
    }

    @Test
    void reinstatedMeetingCreatesANewInvitationOnlyOnExplicitSend() {
        readyInvitation();
        meeting.setStatus("Cancelled");
        meeting.setCancelledOn(Timestamp.valueOf("2026-10-17 18:30:00"));
        meeting.setGraphEventId("cancelled-event");
        service.reinstateMeeting(10, "Resume the sitting", "sec");
        verify(calendarService, never()).createOrUpdateEvent(any(), any(), any(), any());
        when(calendarService.createOrUpdateEvent(any(), any(), any(), any())).thenReturn(new AgendaCalendarService.CalendarEvent("new-event", "https://teams.example.test/new"));
        AgendaMeetingDTO sent = service.sendInvitation(10, "sec");
        ArgumentCaptor<AgendaMeetingDTO> request = ArgumentCaptor.forClass(AgendaMeetingDTO.class);
        verify(calendarService).createOrUpdateEvent(request.capture(), any(), any(), any());
        assertNull(request.getValue().getGraphEventId());
        assertEquals(meeting.getCancelledOn(), request.getValue().getCancelledOn());
        assertEquals("new-event", sent.getGraphEventId());
        assertEquals("Draft", sent.getStatus());
        assertEquals("https://teams.example.test/new", sent.getTeamsJoinUrl());
        verify(calendarService, never()).cancelEvent(any(), any());
        verifyNoInteractions(mailService);
    }

    @Test
    void unknownMeetingCannotBeReinstated() {
        assertThrows(IllegalArgumentException.class, () -> service.reinstateMeeting(999, "Resume the sitting", "sec"));
        verify(repository, never()).saveMeeting(any());
        verifyNoInteractions(calendarService, mailService);
    }

    // ---- Close / re-open (RULE-015) ----

    @Test
    @DisplayName("Close refuses while an approved item has no chair sign-off")
    void closeNeedsSignOff() {
        meeting.setStatus("Held");
        item.setItemStatus("Approved");
        when(repository.getPostMeetingForItem(100)).thenReturn(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.closeMeeting(10, "sec"));
        assertTrue(ex.getMessage().contains("Acme Ltd"));
    }

    @Test
    @DisplayName("Close succeeds once every approved item is decided and signed off, and re-open is audited")
    void closeThenReopen() {
        meeting.setStatus("Held");
        item.setItemStatus("Approved");
        ApprovalForumAgpmAgendaPostMeetingEntity pm = new ApprovalForumAgpmAgendaPostMeetingEntity();
        pm.setAgiAgendaItemByAgiId(item);
        pm.setDecision("Approved");
        pm.setChairSignOffOn(Timestamp.valueOf(LocalDateTime.now()));
        when(repository.getPostMeetingForItem(100)).thenReturn(pm);
        AgendaMeetingDTO closed = service.closeMeeting(10, "sec");
        assertEquals("Closed", closed.getStatus());
        assertEquals("sec", closed.getClosedBy());
        assertThrows(IllegalStateException.class, () -> service.reopenMeeting(10, " ", "rev"));
        AgendaMeetingDTO reopened = service.reopenMeeting(10, "Decision wording corrected", "rev");
        assertEquals("Held", reopened.getStatus());
        verify(repository, times(2)).saveWorkflowAction(any());
    }

    @Test
    @DisplayName("Conditions can only be captured once the meeting is held")
    void conditionsNeedHeldMeeting() {
        AgendaConditionDTO c = new AgendaConditionDTO();
        c.setDescription("Provide audited financials");
        assertThrows(IllegalStateException.class, () -> service.saveConditions(100, Collections.singletonList(c), "sec"));
    }

    @Test
    @DisplayName("Report aggregates late items, status counts and open conditions")
    void reportSummary() {
        item.setLateSubmission(true);
        ApprovalForumAgcaAgendaConditionEntity cond = new ApprovalForumAgcaAgendaConditionEntity();
        cond.setAgiAgendaItemByAgiId(item);
        cond.setStatus("Open");
        cond.setDueDate(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
        when(repository.getMeetingsBetween(any(), any())).thenReturn(Collections.singletonList(meeting));
        when(repository.getItemsBetween(any(), any())).thenReturn(Collections.singletonList(item));
        when(repository.getConditionsForItem(100)).thenReturn(Collections.singletonList(cond));
        AgendaReportSummaryDTO r = service.getReportSummary(Timestamp.valueOf(LocalDateTime.now().minusDays(30)),
                Timestamp.valueOf(LocalDateTime.now().plusDays(30)));
        assertEquals(1, r.getMeetings());
        assertEquals(1, r.getItems());
        assertEquals(1, r.getLateItems());
        assertEquals(Integer.valueOf(1), r.getItemsByStatus().get("Confirmed"));
        assertEquals(1, r.getOpenConditions());
        assertEquals(1, r.getOverdueConditions());
    }

}
