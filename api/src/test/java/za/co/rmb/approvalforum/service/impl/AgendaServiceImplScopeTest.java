package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaPostMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaReportSummaryDTO;
import za.co.rmb.approvalforum.service.AgendaAccess;
import za.co.rmb.approvalforum.service.AgendaCalendarService;
import za.co.rmb.approvalforum.service.AgendaMailService;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The item- and committee-scope rules the resource relies on: who serves which committee, what a
 * caller sees of a meeting they may not fully open, documents and conditions following their item,
 * sign-offs recorded as the caller, and the shared roster carrying no paper details.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendaServiceImplScopeTest {

    private static final String ANALYST = "analyst@rmb.co.za";
    private static final String OTHER_ANALYST = "other@rmb.co.za";
    private static final String REVIEWER = "reviewer@rmb.co.za";
    private static final String SECRETARY = "secretary@rmb.co.za";
    private static final String OTHER_SECRETARY = "other-secretary@rmb.co.za";
    private static final String CHAIR = "chair@rmb.co.za";
    private static final AgendaAccess SECRETARY_ROLE = new AgendaAccess(true, false, false, false);
    private static final AgendaAccess CHAIR_ROLE = new AgendaAccess(false, true, false, false);
    private static final AgendaAccess APPROVER_ROLE = new AgendaAccess(false, false, false, true);
    private static final AgendaAccess NO_ROLE = AgendaAccess.none();

    @Mock
    private ApprovalForumAgendaRepository repository;
    @Mock
    private AgendaMailService mail;
    @Mock
    private ApprovalForumListManagementService listManagement;
    @Mock
    private AgendaCalendarService calendar;

    private AgendaServiceImpl service;
    private ApprovalForumAgcAgendaCommitteeEntity committee;
    private ApprovalForumAgmAgendaMeetingEntity meeting;
    private ApprovalForumAgiAgendaItemEntity mine;
    private ApprovalForumAgiAgendaItemEntity theirs;
    private final List<ApprovalForumAgiAgendaItemEntity> items = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new AgendaServiceImpl(repository, mail, listManagement, calendar);

        committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1);
        committee.setActive(true);
        committee.setCommitteeName("CCC");
        committee.setMandateLevel("L3");
        committee.setQuorumRoles("Chair");
        committee.setMeetingSecretaryId(SECRETARY);
        committee.setChairId(CHAIR);

        meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10);
        meeting.setStatus("Draft");
        meeting.setActive(true);
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(Timestamp.valueOf(LocalDate.now().plusDays(7).atStartOfDay()));

        mine = item(100, ANALYST, "Acme Ltd", "09:00", "09:30");
        theirs = item(101, OTHER_ANALYST, "Globex Corp", "10:00", "10:45");

        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.getMeeting(10)).thenReturn(meeting);
        when(repository.getItemsForMeeting(10)).thenReturn(items);
        when(repository.getRecordsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getWorkflowActionsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getWorkflowActionsForItem(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getDocumentsForItem(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getParticipantsForItem(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getConditionsForItem(anyInt())).thenReturn(new ArrayList<>());
        when(repository.savePostMeeting(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveCondition(any())).thenAnswer(inv -> inv.getArgument(0));
        when(calendar.isConfigured()).thenReturn(false);
    }

    private ApprovalForumAgiAgendaItemEntity item(int agiId, String owner, String counterparty, String start, String end) {
        ApprovalForumAgiAgendaItemEntity item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(agiId);
        item.setActive(true);
        item.setItemStatus("Confirmed");
        item.setAgmAgendaMeetingByAgmId(meeting);
        item.setCounterpartyName(counterparty);
        item.setPurposeOfSubmission("Annual review");
        item.setCreatedBy(owner);
        item.setPrimaryAnalystId(owner);
        item.setPrimaryAnalyst(owner);
        item.setReviewAnalystId(REVIEWER);
        item.setEstimatedMinutes(30);
        item.setStartTime(start);
        item.setEndTime(end);
        item.setChair("C Chair");
        item.setAttendeeEmails("guest@rmb.co.za");
        items.add(item);
        when(repository.getItem(agiId)).thenReturn(item);
        return item;
    }

    private ApprovalForumAgpmAgendaPostMeetingEntity heldWithRecord() {
        meeting.setStatus("Held");
        meeting.setMeetingDate(Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay()));
        ApprovalForumAgpmAgendaPostMeetingEntity record = new ApprovalForumAgpmAgendaPostMeetingEntity();
        record.setAgpmId(1100);
        record.setAgiAgendaItemByAgiId(mine);
        when(repository.getPostMeetingForItem(100)).thenReturn(record);
        return record;
    }

    // ---- Who serves which committee ----

    @Test
    @DisplayName("A role only counts for the committee the caller serves; an unknown meeting or committee never matches")
    void decidesForMeetingAndCommittee() {
        assertTrue(service.decidesForMeeting(10, SECRETARY, SECRETARY_ROLE));
        assertTrue(service.decidesForMeeting(10, CHAIR, CHAIR_ROLE));
        assertTrue(service.decidesForCommittee(1, SECRETARY, SECRETARY_ROLE));
        assertFalse(service.decidesForMeeting(10, OTHER_SECRETARY, SECRETARY_ROLE), "a secretary of another committee");
        assertFalse(service.decidesForMeeting(10, SECRETARY, CHAIR_ROLE), "the right person with the wrong role");
        assertFalse(service.decidesForMeeting(10, SECRETARY, NO_ROLE), "the role must be held");
        assertFalse(service.decidesForCommittee(1, CHAIR, APPROVER_ROLE), "an approver is not committee staff");
        assertFalse(service.decidesForMeeting(999, SECRETARY, SECRETARY_ROLE));
        assertFalse(service.decidesForCommittee(999, SECRETARY, SECRETARY_ROLE));
    }

    // ---- Meeting detail as the caller may see it ----

    @Test
    @DisplayName("Meeting detail keeps the caller's own items whole and reduces the others to their slot")
    void meetingDetailReducesOtherAnalystsItemsToTheirSlot() {
        AgendaMeetingDTO forAnalyst = service.getMeetingFor(10, ANALYST, NO_ROLE);

        assertEquals(2, forAnalyst.getItems().size(), "every slot is still there for clash checking");
        AgendaItemDTO own = forAnalyst.getItems().get(0);
        assertEquals("Acme Ltd", own.getCounterpartyName());
        assertEquals(ANALYST, own.getPrimaryAnalyst());
        AgendaItemDTO slot = forAnalyst.getItems().get(1);
        assertEquals(101, slot.getAgiId());
        assertEquals("10:00", slot.getStartTime());
        assertEquals("10:45", slot.getEndTime());
        assertEquals(Integer.valueOf(30), slot.getEstimatedMinutes());
        assertEquals("Confirmed", slot.getItemStatus());
        assertEquals(10, slot.getAgmId());
        assertNull(slot.getCounterpartyName(), "nothing of the paper");
        assertNull(slot.getPrimaryAnalyst());
        assertNull(slot.getAttendeeEmails());
        assertNull(slot.getChair());
        assertNull(slot.getPostMeeting());

        AgendaMeetingDTO forSecretary = service.getMeetingFor(10, SECRETARY, SECRETARY_ROLE);
        assertEquals("Globex Corp", forSecretary.getItems().get(1).getCounterpartyName(), "the committee's secretary sees everything");

        AgendaMeetingDTO forOtherSecretary = service.getMeetingFor(10, OTHER_SECRETARY, SECRETARY_ROLE);
        assertNull(forOtherSecretary.getItems().get(0).getCounterpartyName());
        assertNull(forOtherSecretary.getItems().get(1).getCounterpartyName());

        assertNull(service.getMeetingFor(999, ANALYST, NO_ROLE));
        assertEquals("Globex Corp", service.getMeeting(10).getItems().get(1).getCounterpartyName(), "the unscoped read is unchanged");
    }

    // ---- Documents and conditions follow their item ----

    @Test
    void documentVisibilityFollowsTheItem() {
        ApprovalForumAgidAgendaItemDocumentEntity document = new ApprovalForumAgidAgendaItemDocumentEntity();
        document.setAgidId(5);
        document.setAgiAgendaItemByAgiId(theirs);
        when(repository.getDocument(5)).thenReturn(document);

        assertTrue(service.canViewDocument(5, OTHER_ANALYST, NO_ROLE), "the owner");
        assertTrue(service.canViewDocument(5, SECRETARY, SECRETARY_ROLE), "the committee's secretary");
        assertFalse(service.canViewDocument(5, ANALYST, NO_ROLE), "another analyst");
        assertFalse(service.canViewDocument(5, OTHER_SECRETARY, SECRETARY_ROLE), "another committee's secretary");
        assertFalse(service.canViewDocument(5, "someone", APPROVER_ROLE), "an approver before submission");
        theirs.setItemStatus("Submitted");
        assertTrue(service.canViewDocument(5, "someone", APPROVER_ROLE), "an approver once submitted");
        assertFalse(service.canViewDocument(6, SECRETARY, SECRETARY_ROLE), "unknown document");
    }

    @Test
    void conditionClosureBelongsToTheItemsSecretariat() {
        ApprovalForumAgcaAgendaConditionEntity condition = new ApprovalForumAgcaAgendaConditionEntity();
        condition.setAgcaId(7);
        condition.setAgiAgendaItemByAgiId(mine);
        condition.setStatus("Open");
        condition.setActive(true);
        when(repository.getCondition(7)).thenReturn(condition);

        assertTrue(service.canCloseCondition(7, SECRETARY, SECRETARY_ROLE));
        assertTrue(service.canCloseCondition(7, CHAIR, CHAIR_ROLE));
        assertFalse(service.canCloseCondition(7, ANALYST, NO_ROLE), "the item's own analyst");
        assertFalse(service.canCloseCondition(7, OTHER_SECRETARY, SECRETARY_ROLE));
        assertFalse(service.canCloseCondition(8, SECRETARY, SECRETARY_ROLE), "unknown condition");

        meeting.setStatus("Closed");
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> service.closeCondition(7, "Closed", "Evidence received", SECRETARY));
        assertTrue(refused.getMessage().contains("Meeting is closed"));
        assertEquals("Open", condition.getStatus());
    }

    // ---- Sign-offs are recorded as the caller ----

    @Test
    @DisplayName("A sign-off is recorded as the caller at the server's time; the names and times sent are ignored and a sign-off is never overwritten")
    void signOffIdentityComesFromTheServer() {
        ApprovalForumAgpmAgendaPostMeetingEntity record = heldWithRecord();
        AgendaPostMeetingDTO dto = new AgendaPostMeetingDTO();
        dto.setAgiId(100);
        dto.setDecision("Approved");
        dto.setAnalystSignOffBy("Somebody Else");
        dto.setAnalystSignOffOn(Timestamp.valueOf("2020-01-01 00:00:00"));

        AgendaPostMeetingDTO saved = service.savePostMeeting(dto, ANALYST);
        assertEquals(ANALYST, saved.getAnalystSignOffBy());
        assertNotNull(saved.getAnalystSignOffOn());
        assertTrue(saved.getAnalystSignOffOn().after(Timestamp.valueOf("2026-01-01 00:00:00")), "the server's time, not the request's");
        assertNull(saved.getChairSignOffBy());
        assertFalse(saved.isLocked());
        Timestamp first = saved.getAnalystSignOffOn();

        dto.setAnalystSignOffBy("Another Name");
        dto.setAnalystSignOffOn(Timestamp.valueOf("2021-01-01 00:00:00"));
        AgendaPostMeetingDTO again = service.savePostMeeting(dto, SECRETARY);
        assertEquals(ANALYST, again.getAnalystSignOffBy(), "a recorded sign-off keeps its signer");
        assertEquals(first, again.getAnalystSignOffOn());

        dto.setAnalystSignOffOn(null);
        dto.setChairSignOffBy("Forged Chair");
        dto.setChairSignOffOn(Timestamp.valueOf("2021-01-01 00:00:00"));
        AgendaPostMeetingDTO signed = service.savePostMeeting(dto, CHAIR);
        assertEquals(ANALYST, signed.getAnalystSignOffBy(), "leaving the field out does not withdraw a sign-off");
        assertEquals(CHAIR, signed.getChairSignOffBy());
        assertTrue(signed.getChairSignOffOn().after(Timestamp.valueOf("2026-01-01 00:00:00")));
        assertTrue(signed.isLocked());
        assertEquals(CHAIR, record.getLockedBy());

        IllegalStateException locked = assertThrows(IllegalStateException.class, () -> service.savePostMeeting(dto, SECRETARY));
        assertTrue(locked.getMessage().contains("the chairperson or secretary must unlock it"));
    }

    // ---- The shared roster carries no paper details ----

    @Test
    void lastRosterSharesOnlyThePeople() {
        theirs.setLecRep("L Rep");
        theirs.setScribe("S Scribe");
        when(repository.getLatestItemForCommittee(1)).thenReturn(theirs);
        ApprovalForumAgpAgendaParticipantEntity participant = new ApprovalForumAgpAgendaParticipantEntity();
        participant.setAgpId(1);
        participant.setAgiAgendaItemByAgiId(theirs);
        participant.setPersonId(CHAIR);
        participant.setCommitteeRole("Chair");
        participant.setActive(true);
        when(repository.getParticipantsForItem(101)).thenReturn(Collections.singletonList(participant));

        AgendaItemDTO roster = service.getLastRoster(1);

        assertEquals("C Chair", roster.getChair());
        assertEquals("L Rep", roster.getLecRep());
        assertEquals("S Scribe", roster.getScribe());
        assertEquals("guest@rmb.co.za", roster.getAttendeeEmails());
        assertEquals(1, roster.getParticipants().size());
        assertEquals(CHAIR, roster.getParticipants().get(0).getPersonId());
        assertEquals(0, roster.getAgiId());
        assertNull(roster.getCounterpartyName());
        assertNull(roster.getPrimaryAnalyst());
        assertNull(roster.getPurposeOfSubmission());
        assertNull(roster.getMeetingDate());
        assertNull(roster.getItemStatus());
    }

    // ---- Report: late-item details only for items the caller may open ----

    @Test
    void reportListsLateItemDetailsOnlyForVisibleItems() {
        mine.setLateSubmission(true);
        theirs.setLateSubmission(true);
        when(repository.getMeetingsBetween(any(), any())).thenReturn(Collections.singletonList(meeting));
        when(repository.getItemsBetween(any(), any())).thenReturn(Arrays.asList(mine, theirs));
        Timestamp from = Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 0, 0));
        Timestamp to = Timestamp.valueOf(LocalDateTime.of(2026, 12, 31, 0, 0));

        AgendaReportSummaryDTO forAnalyst = service.getReportSummary(from, to, ANALYST, NO_ROLE);
        assertEquals(2, forAnalyst.getLateItems(), "the count covers everything");
        assertEquals(1, forAnalyst.getLateItemDetails().size());
        assertEquals("Acme Ltd", forAnalyst.getLateItemDetails().get(0).getCounterpartyName());

        AgendaReportSummaryDTO forSecretary = service.getReportSummary(from, to, SECRETARY, SECRETARY_ROLE);
        assertEquals(2, forSecretary.getLateItemDetails().size());

        AgendaReportSummaryDTO anonymous = service.getReportSummary(from, to);
        assertEquals(2, anonymous.getLateItems());
        assertTrue(anonymous.getLateItemDetails().isEmpty(), "without a caller no details are listed");
        verify(repository, org.mockito.Mockito.times(3)).getItemsBetween(from, to);
    }
}
