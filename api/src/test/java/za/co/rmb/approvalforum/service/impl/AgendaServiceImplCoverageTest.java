package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.ListManagementList;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.AgendaAccess;
import za.co.rmb.approvalforum.service.AgendaCalendarService;
import za.co.rmb.approvalforum.service.AgendaMailService;
import za.co.rmb.approvalforum.service.ApprovalForumListManagementService;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * The parts of AgendaServiceImpl the other agenda tests do not reach: committee and meeting
 * administration, listing and visibility, DTO mapping, documents, re-open, post-meeting capture and
 * sign-off, mark as held, close / re-open of the record, participants, conditions, the schedulers,
 * agenda numbering and who an item's invitation goes to.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendaServiceImplCoverageTest {

    private static final String ANALYST = "analyst@rmb.co.za";
    private static final String REVIEWER = "reviewer@rmb.co.za";
    private static final String SECRETARY = "secretary@rmb.co.za";
    private static final String CHAIR = "chair@rmb.co.za";
    private static final AgendaAccess SECRETARY_ROLE = new AgendaAccess(true, false, false, false);
    private static final AgendaAccess CHAIR_ROLE = new AgendaAccess(false, true, false, false);
    private static final AgendaAccess REVIEWER_ROLE = new AgendaAccess(false, false, true, false);
    private static final AgendaAccess APPROVER_ROLE = new AgendaAccess(false, false, false, true);

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
    private ApprovalForumAgiAgendaItemEntity item;
    private final List<ApprovalForumAgiAgendaItemEntity> items = new ArrayList<>();
    private final List<ApprovalForumAgpAgendaParticipantEntity> participants = new ArrayList<>();
    private final List<ApprovalForumAgidAgendaItemDocumentEntity> documents = new ArrayList<>();
    private final List<ApprovalForumAgcaAgendaConditionEntity> conditions = new ArrayList<>();

    private static Timestamp day(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private static LocalDate nextWeekday(DayOfWeek day, int weeks) {
        return LocalDate.now().with(TemporalAdjusters.next(day)).plusWeeks(weeks);
    }

    /** The most recent weekday strictly before today, so a "held" meeting is in the past on any day of the week. */
    private static LocalDate lastWeekday() {
        LocalDate date = LocalDate.now().minusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) date = date.minusDays(1);
        return date;
    }

    @BeforeEach
    void setUp() {
        service = new AgendaServiceImpl(repository, mail, listManagement, calendar);

        committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1);
        committee.setActive(true);
        committee.setCommitteeName("CCC");
        committee.setMandateLevel("L3");
        committee.setQuorumSize(2);
        committee.setQuorumRoles("Chair;IndependentMember");
        committee.setMeetingDay("Tuesday");
        committee.setMeetingSecretaryId(SECRETARY);
        committee.setChairId(CHAIR);
        committee.setChair("C Chair");
        committee.setRequiredDocTypes("Credit Paper");
        committee.setDefaultDurationMinutes(180);

        meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10);
        meeting.setStatus("Draft");
        meeting.setActive(true);
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(day(nextWeekday(DayOfWeek.TUESDAY, 1)));
        meeting.setCutOffOn(Timestamp.valueOf(LocalDateTime.now().plusDays(3)));
        meeting.setStartTime("09:00");
        meeting.setEndTime("12:00");

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
        items.add(item);

        when(repository.getItem(100)).thenReturn(item);
        when(repository.getItemForUpdate(100)).thenReturn(item);
        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.getMeeting(10)).thenReturn(meeting);
        when(repository.getMeetingForUpdate(10)).thenReturn(meeting);
        when(repository.saveItem(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveMeeting(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveCommittee(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveWorkflowAction(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveDocument(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveParticipant(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveCondition(any())).thenAnswer(inv -> {
            ApprovalForumAgcaAgendaConditionEntity c = inv.getArgument(0);
            if (c.getAgcaId() == 0) c.setAgcaId(500 + conditions.size());
            return c;
        });
        when(repository.savePostMeeting(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveRecord(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.getDocumentsForItem(anyInt())).thenReturn(documents);
        when(repository.getParticipantsForItem(anyInt())).thenAnswer(inv -> new ArrayList<>(participants));
        when(repository.getWorkflowActionsForItem(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getWorkflowActionsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getRecordsForMeeting(anyInt())).thenReturn(new ArrayList<>());
        when(repository.getConditionsForItem(anyInt())).thenReturn(conditions);
        when(repository.getItemsForMeeting(10)).thenReturn(items);
        when(calendar.isConfigured()).thenReturn(false);
    }

    private ApprovalForumAgpAgendaParticipantEntity participant(int agpId, String role, String id, String email) {
        ApprovalForumAgpAgendaParticipantEntity p = new ApprovalForumAgpAgendaParticipantEntity();
        p.setAgpId(agpId);
        p.setAgiAgendaItemByAgiId(item);
        p.setPersonId(id);
        p.setPersonName(id);
        p.setEmail(email);
        p.setCommitteeRole(role);
        p.setQuorumEligible(true);
        p.setActive(true);
        participants.add(p);
        return p;
    }

    private ApprovalForumAgidAgendaItemDocumentEntity document(int agidId, String type, String status, int version) {
        ApprovalForumAgidAgendaItemDocumentEntity d = new ApprovalForumAgidAgendaItemDocumentEntity();
        d.setAgidId(agidId);
        d.setAgiAgendaItemByAgiId(item);
        d.setDocType(type);
        d.setFileName(type.toLowerCase().replace(' ', '-') + ".pdf");
        d.setFileData(new byte[] { 1 });
        d.setValidationStatus(status);
        d.setVersion(version);
        d.setActive(true);
        documents.add(d);
        return d;
    }

    private ApprovalForumAgiAgendaItemEntity otherItem(int agiId, String status, String start, String end) {
        ApprovalForumAgiAgendaItemEntity other = new ApprovalForumAgiAgendaItemEntity();
        other.setAgiId(agiId);
        other.setActive(true);
        other.setItemStatus(status);
        other.setCounterpartyName("Item " + agiId);
        other.setStartTime(start);
        other.setEndTime(end);
        other.setEstimatedMinutes(30);
        other.setAgmAgendaMeetingByAgmId(meeting);
        other.setCreatedOn(Timestamp.valueOf("2026-10-01 10:00:00"));
        when(repository.getItem(agiId)).thenReturn(other);
        when(repository.getItemForUpdate(agiId)).thenReturn(other);
        items.add(other);
        return other;
    }

    private ApprovalForumAgpmAgendaPostMeetingEntity postMeeting(ApprovalForumAgiAgendaItemEntity owner) {
        ApprovalForumAgpmAgendaPostMeetingEntity pm = new ApprovalForumAgpmAgendaPostMeetingEntity();
        pm.setAgpmId(owner.getAgiId() + 1000);
        pm.setAgiAgendaItemByAgiId(owner);
        when(repository.getPostMeetingForItem(owner.getAgiId())).thenReturn(pm);
        return pm;
    }

    private void held() {
        meeting.setStatus("Held");
        meeting.setMeetingDate(day(lastWeekday()));
    }

    private List<ApprovalForumAgwAgendaWorkflowActionEntity> actions() {
        ArgumentCaptor<ApprovalForumAgwAgendaWorkflowActionEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgwAgendaWorkflowActionEntity.class);
        verify(repository, atLeastOnce()).saveWorkflowAction(captor.capture());
        return captor.getAllValues();
    }

    private static String failure(org.junit.jupiter.api.function.Executable call) {
        return assertThrows(IllegalStateException.class, call).getMessage();
    }

    // ---- Committees ----

    @Test
    @DisplayName("Committees are listed and mapped field for field; an unknown committee reads as null")
    void committeeReadsAndMapping() {
        committee.setLevelDescription("Large corporates");
        committee.setAltChairId("alt@rmb.co.za");
        committee.setMinuteTakerId("minutes@rmb.co.za");
        committee.setAdministratorId("admin@rmb.co.za");
        committee.setApproverComposition("Chair + 1");
        committee.setPageGuidance(25);
        committee.setListName("CCC members");
        committee.setDefaultHeldVia("Teams");
        committee.setCutOffDayOfWeek("Thursday");
        committee.setCutOffTime("17:00");
        committee.setEffectiveFrom(Timestamp.valueOf("2026-01-01 00:00:00"));
        when(repository.getCommittees()).thenReturn(Collections.singletonList(committee));
        when(repository.getAllCommittees()).thenReturn(Arrays.asList(committee, committee));

        AgendaCommitteeDTO dto = service.getCommittees().get(0);
        assertEquals(1, dto.getAgcId());
        assertEquals("CCC", dto.getCommitteeName());
        assertEquals("L3", dto.getMandateLevel());
        assertEquals("Large corporates", dto.getLevelDescription());
        assertEquals(2, dto.getQuorumSize());
        assertEquals("Tuesday", dto.getMeetingDay());
        assertEquals("C Chair", dto.getChair());
        assertEquals(CHAIR, dto.getChairId());
        assertEquals("alt@rmb.co.za", dto.getAltChairId());
        assertEquals(SECRETARY, dto.getMeetingSecretaryId());
        assertEquals("minutes@rmb.co.za", dto.getMinuteTakerId());
        assertEquals("admin@rmb.co.za", dto.getAdministratorId());
        assertEquals("Chair + 1", dto.getApproverComposition());
        assertEquals(Integer.valueOf(25), dto.getPageGuidance());
        assertEquals("CCC members", dto.getListName());
        assertTrue(dto.isActive());
        assertEquals("Teams", dto.getDefaultHeldVia());
        assertEquals(Integer.valueOf(180), dto.getDefaultDurationMinutes());
        assertEquals("Thursday", dto.getCutOffDayOfWeek());
        assertEquals("17:00", dto.getCutOffTime());
        assertEquals("Credit Paper", dto.getRequiredDocTypes());
        assertEquals("Chair;IndependentMember", dto.getQuorumRoles());
        assertEquals(Timestamp.valueOf("2026-01-01 00:00:00"), dto.getEffectiveFrom());
        assertEquals(2, service.getAllCommittees().size());
        assertEquals("CCC", service.getCommittee(1).getCommitteeName());
        assertNull(service.getCommittee(99));
    }

    @Test
    @DisplayName("Updating a committee applies its people, configuration and cut-off; bad cut-off values are refused")
    void updateCommittee() {
        AgendaCommitteeDTO dto = new AgendaCommitteeDTO();
        dto.setAgcId(1);
        dto.setQuorumSize(3);
        dto.setMeetingDay("Wednesday");
        dto.setChair("New Chair");
        dto.setChairId("newchair@rmb.co.za");
        dto.setMeetingSecretary("Sec");
        dto.setMeetingSecretaryId("newsec@rmb.co.za");
        dto.setMinuteTakerId("mt@rmb.co.za");
        dto.setAdministratorId("adm@rmb.co.za");
        dto.setListName("New list");
        dto.setDefaultDurationMinutes(120);
        dto.setDefaultHeldVia("Hybrid");
        dto.setCutOffDayOfWeek(" friday ");
        dto.setCutOffTime(" 12:30 ");
        dto.setRequiredDocTypes("Credit Paper;Annexure");
        dto.setQuorumRoles("Chair");

        AgendaCommitteeDTO saved = service.updateCommittee(dto, SECRETARY);

        assertEquals(3, committee.getQuorumSize());
        assertEquals("Wednesday", committee.getMeetingDay());
        assertEquals("newchair@rmb.co.za", committee.getChairId());
        assertEquals("newsec@rmb.co.za", committee.getMeetingSecretaryId());
        assertEquals("mt@rmb.co.za", committee.getMinuteTakerId());
        assertEquals("adm@rmb.co.za", committee.getAdministratorId());
        assertEquals("New list", committee.getListName());
        assertEquals(Integer.valueOf(120), committee.getDefaultDurationMinutes());
        assertEquals("Hybrid", committee.getDefaultHeldVia());
        assertEquals("friday", committee.getCutOffDayOfWeek());
        assertEquals("12:30", committee.getCutOffTime());
        assertEquals("Credit Paper;Annexure", committee.getRequiredDocTypes());
        assertEquals("Chair", committee.getQuorumRoles());
        assertEquals("friday", saved.getCutOffDayOfWeek());
        verify(repository).saveCommittee(committee);

        dto.setCutOffTime("25:00");
        assertTrue(failure(() -> service.updateCommittee(dto, SECRETARY)).contains("Cut-off time must be HH:mm"));
        dto.setCutOffTime("12:30");
        dto.setCutOffDayOfWeek("Someday");
        assertThrows(IllegalArgumentException.class, () -> service.updateCommittee(dto, SECRETARY));
        dto.setAgcId(99);
        assertThrows(IllegalArgumentException.class, () -> service.updateCommittee(dto, SECRETARY));
    }

    @Test
    @DisplayName("Creating a committee needs a name, mandate level, valid meeting day and positive quorum; defaults fill the cut-off and papers")
    void createCommittee() {
        AgendaCommitteeDTO dto = new AgendaCommitteeDTO();
        assertTrue(failure(() -> service.createCommittee(dto, SECRETARY)).contains("Committee name"));
        dto.setCommitteeName(" ICC ");
        assertTrue(failure(() -> service.createCommittee(dto, SECRETARY)).contains("Mandate level"));
        dto.setMandateLevel(" L2 ");
        assertTrue(failure(() -> service.createCommittee(dto, SECRETARY)).contains("Meeting day"));
        dto.setMeetingDay("Funday");
        assertThrows(IllegalArgumentException.class, () -> service.createCommittee(dto, SECRETARY));
        dto.setMeetingDay("Monday");
        assertTrue(failure(() -> service.createCommittee(dto, SECRETARY)).contains("Quorum size"));
        dto.setQuorumSize(3);
        verify(repository, never()).saveCommittee(any());

        AgendaCommitteeDTO saved = service.createCommittee(dto, SECRETARY);

        ArgumentCaptor<ApprovalForumAgcAgendaCommitteeEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgcAgendaCommitteeEntity.class);
        verify(repository).saveCommittee(captor.capture());
        ApprovalForumAgcAgendaCommitteeEntity created = captor.getValue();
        assertEquals("ICC", created.getCommitteeName());
        assertEquals("L2", created.getMandateLevel());
        assertTrue(created.isActive());
        assertEquals(SECRETARY, created.getCreatedBy());
        assertNotNull(created.getCreatedOn());
        assertEquals("Thursday", created.getCutOffDayOfWeek());
        assertEquals("17:00", created.getCutOffTime());
        assertEquals("Credit Paper", created.getRequiredDocTypes());
        assertEquals("ICC", saved.getCommitteeName());

        AgendaCommitteeDTO configured = new AgendaCommitteeDTO();
        configured.setCommitteeName("X");
        configured.setMandateLevel("L1");
        configured.setMeetingDay("Monday");
        configured.setQuorumSize(1);
        configured.setCutOffDayOfWeek("Wednesday");
        configured.setCutOffTime("10:00");
        configured.setRequiredDocTypes("Term Sheet");
        AgendaCommitteeDTO second = service.createCommittee(configured, SECRETARY);
        assertEquals("Wednesday", second.getCutOffDayOfWeek());
        assertEquals("10:00", second.getCutOffTime());
        assertEquals("Term Sheet", second.getRequiredDocTypes(), "configured values are kept over the defaults");
        verify(repository, times(2)).saveCommittee(any());
    }

    @Test
    void deactivateCommitteeClosesItsEffectiveWindowOnce() {
        assertThrows(IllegalArgumentException.class, () -> service.deactivateCommittee(99, SECRETARY));
        AgendaCommitteeDTO saved = service.deactivateCommittee(1, SECRETARY);
        assertFalse(saved.isActive());
        assertNotNull(committee.getEffectiveTo());
        Timestamp first = committee.getEffectiveTo();
        committee.setActive(true);
        service.deactivateCommittee(1, SECRETARY);
        assertEquals(first, committee.getEffectiveTo(), "an existing end date is kept");
    }

    // ---- Meetings ----

    private AgendaMeetingDTO newMeeting(LocalDate date) {
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgcId(1);
        dto.setMeetingDate(Timestamp.valueOf(date.atTime(14, 30)));
        return dto;
    }

    @Test
    @DisplayName("Scheduling a meeting: unknown committee, no date, inactive or out-of-window committee, duplicate date and venue rules are refused")
    void createMeetingRefusals() {
        LocalDate date = nextWeekday(DayOfWeek.TUESDAY, 2);
        AgendaMeetingDTO unknown = newMeeting(date);
        unknown.setAgcId(99);
        assertThrows(IllegalArgumentException.class, () -> service.createMeeting(unknown, SECRETARY));
        assertTrue(failure(() -> service.createMeeting(new AgendaMeetingDTO() {{ setAgcId(1); }}, SECRETARY)).contains("Meeting date is required"));
        committee.setActive(false);
        assertTrue(failure(() -> service.createMeeting(newMeeting(date), SECRETARY)).contains("is not effective on"));
        committee.setActive(true);
        committee.setEffectiveTo(day(date.minusDays(1)));
        assertTrue(failure(() -> service.createMeeting(newMeeting(date), SECRETARY)).contains("is not effective on"));
        committee.setEffectiveTo(null);
        committee.setEffectiveFrom(day(date.plusDays(1)));
        assertTrue(failure(() -> service.createMeeting(newMeeting(date), SECRETARY)).contains("is not effective on"));
        committee.setEffectiveFrom(null);
        when(repository.findMeeting(1, day(date))).thenReturn(meeting);
        assertTrue(failure(() -> service.createMeeting(newMeeting(date), SECRETARY)).contains("already exists"));
        when(repository.findMeeting(1, day(date))).thenReturn(null);
        AgendaMeetingDTO physical = newMeeting(date);
        physical.setHeldVia("Physical");
        assertTrue(failure(() -> service.createMeeting(physical, SECRETARY)).contains("A venue is required for a Physical meeting"));
        committee.setDefaultHeldVia("In person");
        assertTrue(failure(() -> service.createMeeting(newMeeting(date), SECRETARY)).contains("venue"));
        verify(repository, never()).saveMeeting(any());
    }

    @Test
    @DisplayName("A new meeting is a Draft on the date (time dropped), with the committee's mode, cut-off, 08:00 start and duration")
    void createMeetingDefaults() {
        LocalDate date = nextWeekday(DayOfWeek.TUESDAY, 2);
        committee.setDefaultHeldVia("Teams");
        committee.setCutOffDayOfWeek("Friday");
        committee.setCutOffTime("12:00");
        AgendaMeetingDTO dto = newMeeting(date);
        dto.setTeamsJoinUrl("https://teams.example.test/x");

        AgendaMeetingDTO saved = service.createMeeting(dto, SECRETARY);

        assertEquals(day(date), saved.getMeetingDate());
        assertEquals("Draft", saved.getStatus());
        assertEquals("Teams", saved.getHeldVia());
        assertEquals("08:00", saved.getStartTime());
        assertEquals("11:00", saved.getEndTime());
        assertEquals(Timestamp.valueOf(date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)).atTime(12, 0)), saved.getCutOffOn());
        assertEquals("https://teams.example.test/x", saved.getTeamsJoinUrl());
        assertEquals(1, saved.getAgcId());
        assertEquals("CCC", saved.getCommitteeName());
        ArgumentCaptor<ApprovalForumAgmAgendaMeetingEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgmAgendaMeetingEntity.class);
        verify(repository).saveMeeting(captor.capture());
        assertEquals(SECRETARY, captor.getValue().getCreatedBy());
        assertTrue(captor.getValue().isActive());

        AgendaMeetingDTO explicit = newMeeting(date);
        explicit.setHeldVia("Hybrid");
        explicit.setVenue("Boardroom 1");
        explicit.setStartTime("10:00");
        explicit.setCutOffOn(Timestamp.valueOf("2026-10-01 09:00:00"));
        committee.setDefaultDurationMinutes(null);
        AgendaMeetingDTO hybrid = service.createMeeting(explicit, SECRETARY);
        assertEquals("Hybrid", hybrid.getHeldVia());
        assertEquals("Boardroom 1", hybrid.getVenue());
        assertEquals("14:00", hybrid.getEndTime(), "four hours when the committee has no duration");
        assertEquals(Timestamp.valueOf("2026-10-01 09:00:00"), hybrid.getCutOffOn());
    }

    @Test
    void meetingEndTimeIsCappedBeforeMidnight() {
        AgendaMeetingDTO dto = newMeeting(nextWeekday(DayOfWeek.TUESDAY, 2));
        dto.setStartTime("22:00");
        assertEquals("23:59", service.createMeeting(dto, SECRETARY).getEndTime());
    }

    @Test
    @DisplayName("Editing a meeting: unknown, held, closed or cancelled meetings are refused; a date clash with another meeting too")
    void updateMeetingRefusals() {
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgmId(99);
        assertThrows(IllegalArgumentException.class, () -> service.updateMeeting(dto, SECRETARY));
        dto.setAgmId(10);
        for (String status : new String[] { "Held", "Closed" }) {
            meeting.setStatus(status);
            assertTrue(failure(() -> service.updateMeeting(dto, SECRETARY)).contains("has been held"));
        }
        meeting.setStatus("Cancelled");
        assertTrue(failure(() -> service.updateMeeting(dto, SECRETARY)).contains("has been cancelled"));
        meeting.setStatus("Draft");
        LocalDate other = nextWeekday(DayOfWeek.TUESDAY, 2);
        ApprovalForumAgmAgendaMeetingEntity clash = new ApprovalForumAgmAgendaMeetingEntity();
        clash.setAgmId(11);
        when(repository.findMeeting(1, day(other))).thenReturn(clash);
        dto.setMeetingDate(day(other));
        assertTrue(failure(() -> service.updateMeeting(dto, SECRETARY)).contains("already exists"));
        AgendaMeetingDTO hybrid = new AgendaMeetingDTO();
        hybrid.setAgmId(10);
        hybrid.setHeldVia("Hybrid");
        assertTrue(failure(() -> service.updateMeeting(hybrid, SECRETARY)).contains("venue is required"));
        verify(repository, never()).saveMeeting(any());
    }

    @Test
    @DisplayName("Editing a meeting applies the date (with a recomputed cut-off), explicit cut-off, mode, venue and Teams link")
    void updateMeetingAppliesFields() {
        LocalDate other = nextWeekday(DayOfWeek.TUESDAY, 2);
        when(repository.findMeeting(1, day(other))).thenReturn(meeting);
        committee.setCutOffDayOfWeek("Thursday");
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgmId(10);
        dto.setMeetingDate(Timestamp.valueOf(other.atTime(9, 0)));
        dto.setHeldVia("Hybrid");
        dto.setVenue("Boardroom 3");
        dto.setTeamsJoinUrl("https://teams.example.test/y");

        AgendaMeetingDTO saved = service.updateMeeting(dto, SECRETARY);

        assertEquals(day(other), meeting.getMeetingDate());
        assertEquals(Timestamp.valueOf(other.with(TemporalAdjusters.previous(DayOfWeek.THURSDAY)).atTime(17, 0)), meeting.getCutOffOn());
        assertEquals("Hybrid", saved.getHeldVia());
        assertEquals("Boardroom 3", saved.getVenue());
        assertEquals("https://teams.example.test/y", saved.getTeamsJoinUrl());
        assertEquals("12:00", saved.getEndTime(), "09:00 plus the committee's 180 minutes");
        assertFalse(saved.isInvitationNeedsUpdate());
        assertFalse(saved.isInvitationsSent());

        AgendaMeetingDTO cutOff = new AgendaMeetingDTO();
        cutOff.setAgmId(10);
        cutOff.setCutOffOn(Timestamp.valueOf("2026-10-02 12:00:00"));
        service.updateMeeting(cutOff, SECRETARY);
        assertEquals(Timestamp.valueOf("2026-10-02 12:00:00"), meeting.getCutOffOn());
        verify(calendar, never()).createOrUpdateEvent(any());
    }

    @Test
    @DisplayName("When Graph throws during the edit's automatic update, the edit is saved and the invitation stays pending")
    void editSavedWhenAutomaticInvitationUpdateThrows() {
        committee.setMeetingSecretaryId(SECRETARY);
        item.setItemStatus("Approved");
        item.setAttendeeEmails("attendee@example.test");
        when(calendar.isConfigured()).thenReturn(true);
        when(calendar.createOrUpdateEvent(any())).thenReturn(new AgendaCalendarService.CalendarEvent("event-1", null));
        service.sendInvitation(10, SECRETARY);
        assertEquals("event-1", item.getGraphEventId());

        when(calendar.createOrUpdateEvent(any())).thenThrow(new RuntimeException("graph timeout"));
        AgendaMeetingDTO dto = new AgendaMeetingDTO();
        dto.setAgmId(10);
        dto.setVenue("Boardroom 9");
        AgendaMeetingDTO saved = service.updateMeeting(dto, SECRETARY);

        assertEquals("Boardroom 9", meeting.getVenue());
        assertEquals("Boardroom 9", saved.getVenue());
        assertTrue(saved.isInvitationNeedsUpdate());
        assertTrue(service.getMeeting(10).isInvitationNeedsUpdate());
        assertEquals("event-1", item.getGraphEventId(), "the sent invitation is kept");
        verify(repository).saveMeeting(meeting);
    }

    @Test
    @DisplayName("Cancelling: unknown, held or already cancelled meetings and a blank reason are refused")
    void cancelMeetingRefusals() {
        assertThrows(IllegalArgumentException.class, () -> service.cancelMeeting(99, "x", SECRETARY));
        meeting.setStatus("Held");
        assertTrue(failure(() -> service.cancelMeeting(10, "x", SECRETARY)).contains("has been held"));
        meeting.setStatus("Cancelled");
        assertTrue(failure(() -> service.cancelMeeting(10, "x", SECRETARY)).contains("has been cancelled"));
        meeting.setStatus("Draft");
        assertTrue(failure(() -> service.cancelMeeting(10, " ", SECRETARY)).contains("cancellation reason is required"));
        verify(repository, never()).saveMeeting(any());
    }

    @Test
    @DisplayName("Cancellation mails the distinct attendee addresses of the live items; a mail failure does not undo it")
    void cancelMeetingMailsAttendees() {
        item.setAttendeeEmails("a@example.test; b@example.test");
        otherItem(101, "Confirmed", "10:00", "10:30").setAttendeeEmails("b@example.test;c@example.test");
        otherItem(102, "Removed", "11:00", "11:30").setAttendeeEmails("gone@example.test");
        doThrow(new RuntimeException("smtp down")).when(mail).sendMeetingCancelledMail(any(), any(), any());

        AgendaMeetingDTO cancelled = service.cancelMeeting(10, "  Public holiday  ", SECRETARY);

        assertEquals("Cancelled", cancelled.getStatus());
        assertEquals("Public holiday", cancelled.getCancelReason());
        assertEquals(SECRETARY, cancelled.getCancelledBy());
        assertNotNull(cancelled.getCancelledOn());
        verify(mail).sendMeetingCancelledMail(any(), eq("a@example.test;b@example.test;c@example.test"), eq("  Public holiday  "));
        ApprovalForumAgwAgendaWorkflowActionEntity action = actions().get(0);
        assertEquals("Cancel", action.getAction());
        assertEquals("Draft", action.getFromStatus());
        assertEquals("Cancelled", action.getToStatus());
        assertEquals(Integer.valueOf(10), action.getAgmId());
        verifyNoInteractions(calendar);
    }

    @Test
    void meetingListsMapWithoutItemsAndTheMeetingDetailWithThem() {
        meeting.setVenue("Boardroom 1");
        meeting.setHeldVia("Hybrid");
        meeting.setLockedBy("legacy");
        meeting.setClosedBy("sec");
        when(repository.getMeetingsBetween(any(), any())).thenReturn(Collections.singletonList(meeting));
        when(repository.getMeetingsForCommittee(1)).thenReturn(Collections.singletonList(meeting));
        ApprovalForumAgmrAgendaMeetingRecordEntity record = new ApprovalForumAgmrAgendaMeetingRecordEntity();
        record.setAgmrId(3);
        record.setAgmAgendaMeetingByAgmId(meeting);
        record.setRecordType("Minutes");
        when(repository.getRecordsForMeeting(10)).thenReturn(Collections.singletonList(record));
        ApprovalForumAgpmAgendaPostMeetingEntity pm = postMeeting(item);
        pm.setDecision("Approved");

        AgendaMeetingDTO listed = service.getMeetingsBetween(Timestamp.valueOf("2026-01-01 00:00:00"), Timestamp.valueOf("2026-12-31 00:00:00")).get(0);
        assertNull(listed.getItems());
        assertNull(listed.getRecords());
        assertEquals("Boardroom 1", listed.getVenue());
        assertEquals("Hybrid", listed.getHeldVia());
        assertEquals("legacy", listed.getLockedBy());
        assertEquals("sec", listed.getClosedBy());
        assertEquals(2, listed.getQuorumSize());
        assertEquals("C Chair", listed.getChair());
        assertEquals("L3", listed.getMandateLevel());
        assertNull(service.getMeetingsForCommittee(1).get(0).getItems());

        AgendaMeetingDTO detail = service.getMeeting(10);
        assertEquals(1, detail.getItems().size());
        assertEquals("Approved", detail.getItems().get(0).getPostMeeting().getDecision());
        assertEquals("Minutes", detail.getRecords().get(0).getRecordType());
        assertEquals(10, detail.getRecords().get(0).getAgmId());
        assertNotNull(detail.getActions());
        assertNull(service.getMeeting(99));
    }

    @Test
    void committeeMembersComeFromTheLinkedList() {
        assertThrows(IllegalArgumentException.class, () -> service.getCommitteeMembers(99));
        assertNull(service.getCommitteeMembers(1), "no list linked");
        committee.setListName(" CCC members ");
        ListManagementList list = new ListManagementList();
        when(listManagement.getListManagementListByName("CCC members")).thenReturn(list);
        assertSame(list, service.getCommitteeMembers(1));
    }

    @Test
    @DisplayName("Available dates: scheduled open meetings in the next three weeks plus the regular day, minus cancelled ones, sorted")
    void availableMeetingDates() {
        assertTrue(service.getAvailableMeetingDates(99).isEmpty());
        LocalDate today = LocalDate.now();
        LocalDate extra = today.plusDays(10);
        LocalDate farAway = today.plusWeeks(5);
        List<ApprovalForumAgmAgendaMeetingEntity> scheduled = Arrays.asList(
                scheduled(20, "Draft", extra),
                scheduled(21, "Cancelled", today.plusDays(11)),
                scheduled(22, "Held", today.plusDays(12)),
                scheduled(23, "Closed", today.plusDays(13)),
                scheduled(24, "Draft", farAway),
                scheduled(25, "Draft", today.minusDays(1)));
        when(repository.getMeetingsForCommittee(1)).thenReturn(scheduled);
        LocalDate firstTuesday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        ApprovalForumAgmAgendaMeetingEntity cancelledRegular = new ApprovalForumAgmAgendaMeetingEntity();
        cancelledRegular.setStatus("Cancelled");
        when(repository.findMeeting(1, day(firstTuesday.plusWeeks(1)))).thenReturn(cancelledRegular);

        List<Timestamp> dates = service.getAvailableMeetingDates(1);

        List<Timestamp> expected = new ArrayList<>(Arrays.asList(day(extra), day(firstTuesday), day(firstTuesday.plusWeeks(2))));
        expected.sort(java.util.Comparator.naturalOrder());
        assertEquals(expected, dates);

        committee.setActive(false);
        assertEquals(Collections.singletonList(day(extra)), service.getAvailableMeetingDates(1), "an inactive committee has no regular days");
    }

    private ApprovalForumAgmAgendaMeetingEntity scheduled(int agmId, String status, LocalDate date) {
        ApprovalForumAgmAgendaMeetingEntity m = new ApprovalForumAgmAgendaMeetingEntity();
        m.setAgmId(agmId);
        m.setStatus(status);
        m.setMeetingDate(day(date));
        m.setAgcAgendaCommitteeByAgcId(committee);
        when(repository.getMeeting(agmId)).thenReturn(m);
        return m;
    }

    // ---- Listing, visibility and ownership ----

    @Test
    void listsCarryTheirPostMeetingOutcomeWhereOneExists() {
        ApprovalForumAgiAgendaItemEntity other = otherItem(101, "Approved", "10:00", "10:30");
        postMeeting(other).setDecisionType("Declined");
        when(repository.getAllItems()).thenReturn(items);
        when(repository.getItemsCreatedBy(ANALYST)).thenReturn(items);
        when(repository.getItemsForUser(ANALYST)).thenReturn(items);

        List<AgendaItemDTO> all = service.getAllItems();
        assertNull(all.get(0).getPostMeeting());
        assertEquals("Declined", all.get(1).getPostMeeting().getDecisionType());
        assertEquals("Declined", service.getItemsCreatedBy(ANALYST).get(1).getPostMeeting().getDecisionType());
        assertNull(service.getItemsForUser(ANALYST).get(1).getPostMeeting(), "the user list is the plain mapping");
        assertNull(all.get(0).getDocuments(), "lists leave the documents out");
        verify(repository, never()).getDocumentsForItem(anyInt());
    }

    @Test
    @DisplayName("Visibility (BR-024): owner always; reviewer, secretariat and chair only with the role and the seat; approver once submitted")
    void visibilityRules() {
        committee.setMinuteTakerId("minutes@rmb.co.za");
        committee.setAdministratorId("admin@rmb.co.za");
        committee.setAltChairId("alt@rmb.co.za");
        when(repository.getAllItems()).thenReturn(items);

        assertTrue(service.canViewItem(100, ANALYST, null), "the booker, even with no roles");
        item.setCreatedBy("booker@rmb.co.za");
        assertTrue(service.canViewItem(100, " ANALYST@rmb.co.za ", AgendaAccess.none()), "the primary analyst, case-insensitively");
        assertFalse(service.canViewItem(100, REVIEWER, AgendaAccess.none()), "the reviewer without the role");
        assertTrue(service.canViewItem(100, REVIEWER, REVIEWER_ROLE));
        assertFalse(service.canViewItem(100, "other@rmb.co.za", REVIEWER_ROLE), "the role without the assignment");
        assertTrue(service.canViewItem(100, SECRETARY, SECRETARY_ROLE));
        assertTrue(service.canViewItem(100, "minutes@rmb.co.za", SECRETARY_ROLE));
        assertTrue(service.canViewItem(100, "admin@rmb.co.za", SECRETARY_ROLE));
        assertFalse(service.canViewItem(100, SECRETARY, CHAIR_ROLE), "the wrong role for the seat");
        assertTrue(service.canViewItem(100, CHAIR, CHAIR_ROLE));
        assertTrue(service.canViewItem(100, "alt@rmb.co.za", CHAIR_ROLE));
        assertFalse(service.canViewItem(100, "someone@rmb.co.za", APPROVER_ROLE), "an approver before submission");
        for (String status : new String[] { "Submitted", "Reviewed", "Approved", "Rejected", "Returned" }) {
            item.setItemStatus(status);
            assertTrue(service.canViewItem(100, "someone@rmb.co.za", APPROVER_ROLE), status);
        }
        assertFalse(service.canViewItem(999, ANALYST, APPROVER_ROLE), "an unknown item");
        assertEquals(1, service.getVisibleItems("someone@rmb.co.za", APPROVER_ROLE).size());
        assertTrue(service.getVisibleItems("someone@rmb.co.za", AgendaAccess.none()).isEmpty());
    }

    @Test
    @DisplayName("Editing is for the owner; the roster is the owner's until the meeting is held, then the secretariat's; outcomes are always the secretariat's")
    void ownershipRules() {
        assertTrue(service.canEditItem(100, ANALYST));
        assertFalse(service.canEditItem(100, REVIEWER));
        assertFalse(service.canEditItem(999, ANALYST));

        assertTrue(service.canEditParticipants(100, ANALYST, AgendaAccess.none()));
        assertFalse(service.canEditParticipants(100, SECRETARY, AgendaAccess.none()));
        assertTrue(service.canEditParticipants(100, SECRETARY, SECRETARY_ROLE));
        held();
        assertFalse(service.canEditParticipants(100, ANALYST, AgendaAccess.none()), "after the meeting the roster is the attendance record");
        assertTrue(service.canEditParticipants(100, SECRETARY, SECRETARY_ROLE));
        assertTrue(service.canEditParticipants(100, CHAIR, CHAIR_ROLE));
        assertFalse(service.canEditParticipants(100, "other@rmb.co.za", SECRETARY_ROLE));
        assertFalse(service.canEditParticipants(999, SECRETARY, SECRETARY_ROLE));

        assertFalse(service.canCaptureOutcome(100, ANALYST, AgendaAccess.none()), "the analyst never writes the minute of their own paper");
        assertTrue(service.canCaptureOutcome(100, SECRETARY, SECRETARY_ROLE));
        assertTrue(service.canCaptureOutcome(100, CHAIR, CHAIR_ROLE));
        assertFalse(service.canCaptureOutcome(100, CHAIR, SECRETARY_ROLE));
        assertFalse(service.canCaptureOutcome(999, CHAIR, CHAIR_ROLE));
    }

    @Test
    void lastRosterAndItemDetailMapping() {
        assertNull(service.getLastRoster(1));
        assertNull(service.getItem(999));
        item.setChair("C Chair");
        item.setIndependentMember("I Member");
        when(repository.getLatestItemForCommittee(1)).thenReturn(item);
        AgendaItemDTO roster = service.getLastRoster(1);
        assertEquals("C Chair", roster.getChair());
        assertEquals("I Member", roster.getIndependentMember());
        assertNull(roster.getDocuments());

        committee.setRequiredDocTypes(null);
        document(5, "Credit Paper", "Accepted", 2).setDocStashId("stash-5");
        participant(1, "Chair", CHAIR, null);
        AgendaItemDTO detail = service.getItem(100);
        assertEquals(10, detail.getAgmId());
        assertEquals("CCC", detail.getCommitteeName());
        assertEquals("L3", detail.getMandateLevel());
        assertEquals("Draft", detail.getMeetingStatus());
        assertEquals(meeting.getMeetingDate(), detail.getMeetingDate());
        assertEquals(meeting.getCutOffOn(), detail.getCutOffOn());
        assertEquals(Collections.singletonList("Credit Paper"), detail.getRequiredDocumentTypes(), "the default paper when the committee has none");
        assertEquals(1, detail.getDocuments().size());
        assertEquals("stash-5", detail.getDocuments().get(0).getDocStashId());
        assertEquals(100, detail.getDocuments().get(0).getAgiId());
        assertEquals(2, detail.getDocuments().get(0).getVersion());
        assertEquals(1, detail.getParticipants().size());
        assertEquals(100, detail.getParticipants().get(0).getAgiId());
        assertNotNull(detail.getActions());
        assertNotNull(detail.getConditions());
        assertEquals(1, detail.getQuorum().getEligibleCount());
        assertFalse(detail.getQuorum().isMet());
        assertNull(detail.getApprovedOn());
        assertFalse(detail.isInvitationNeedsUpdate());
    }

    // ---- Documents ----

    @Test
    void attachDocumentConfirmsTheSlot() {
        assertThrows(IllegalArgumentException.class, () -> service.attachDocument(999, "s", "f.pdf", ANALYST));
        held();
        assertTrue(failure(() -> service.attachDocument(100, "s", "f.pdf", ANALYST)).contains("has been held"));
        meeting.setStatus("Draft");
        item.setItemStatus("Captured");

        AgendaItemDTO saved = service.attachDocument(100, "stash-1", "paper.pdf", ANALYST);

        assertEquals("Confirmed", saved.getItemStatus());
        ArgumentCaptor<ApprovalForumAgidAgendaItemDocumentEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgidAgendaItemDocumentEntity.class);
        verify(repository).saveDocument(captor.capture());
        assertEquals("stash-1", captor.getValue().getDocStashId());
        assertEquals("paper.pdf", captor.getValue().getFileName());
        assertEquals(ANALYST, captor.getValue().getUploadedBy());
        assertTrue(captor.getValue().isActive());
        verify(mail).sendItemConfirmedMail(any());
    }

    @Test
    @DisplayName("Upload is refused on an unknown, locked or held meeting and on a rejected or removed item")
    void uploadRefusals() {
        assertThrows(IllegalArgumentException.class, () -> service.uploadDocument(999, "Credit Paper", "p.pdf", ".pdf", new byte[1], ANALYST));
        meeting.setStatus("Locked");
        assertTrue(failure(() -> service.uploadDocument(100, "Credit Paper", "p.pdf", ".pdf", new byte[1], ANALYST)).contains("locked"));
        held();
        assertTrue(failure(() -> service.uploadDocument(100, "Credit Paper", "p.pdf", ".pdf", new byte[1], ANALYST)).contains("has been held"));
        meeting.setStatus("Draft");
        for (String status : new String[] { "Rejected", "Removed" }) {
            item.setItemStatus(status);
            assertTrue(failure(() -> service.uploadDocument(100, "Credit Paper", "p.pdf", ".pdf", new byte[1], ANALYST)).contains("documents cannot be added"));
        }
        verify(repository, never()).saveDocument(any());
    }

    @Test
    @DisplayName("A new version supersedes the previous one of the same type, is flagged mandatory for required types, and confirms a captured item")
    void uploadVersionsAndConfirms() {
        item.setItemStatus("Captured");
        ApprovalForumAgidAgendaItemDocumentEntity previous = document(5, "Credit Paper", "Rejected", 1);

        AgendaItemDTO saved = service.uploadDocument(100, "credit paper", "v2.pdf", ".pdf", new byte[] { 1, 2, 3 }, ANALYST);

        assertEquals("Superseded", previous.getValidationStatus());
        ArgumentCaptor<ApprovalForumAgidAgendaItemDocumentEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgidAgendaItemDocumentEntity.class);
        verify(repository, times(2)).saveDocument(captor.capture());
        ApprovalForumAgidAgendaItemDocumentEntity created = captor.getAllValues().get(1);
        assertEquals(2, created.getVersion());
        assertTrue(created.isMandatory());
        assertEquals("Pending", created.getValidationStatus());
        assertEquals(Long.valueOf(3), created.getFileSize());
        assertEquals(".pdf", created.getFileExtension());
        assertEquals(ANALYST, created.getUploadedBy());
        assertEquals("Captured", saved.getItemStatus(), "the superseded v1 is the only credit paper the repository reports in this fixture");
    }

    @Test
    void uploadingTheMandatoryPaperConfirmsButAnOptionalDocumentDoesNot() {
        item.setItemStatus("Captured");
        when(repository.getDocumentsForItem(100)).thenAnswer(inv -> new ArrayList<>(documents));
        doAnswer(inv -> { documents.add(inv.getArgument(0)); return inv.getArgument(0); }).when(repository).saveDocument(any());

        AgendaItemDTO optional = service.uploadDocument(100, "Term Sheet", "t.pdf", ".pdf", new byte[1], ANALYST);
        assertEquals("Captured", optional.getItemStatus());
        assertFalse(documents.get(0).isMandatory());
        verify(mail, never()).sendItemConfirmedMail(any());

        AgendaItemDTO confirmed = service.uploadDocument(100, "Credit Paper", "p.pdf", ".pdf", new byte[1], ANALYST);
        assertEquals("Confirmed", confirmed.getItemStatus());
        verify(mail).sendItemConfirmedMail(any());
        verify(repository, never()).saveWorkflowAction(any());

        item.setItemStatus("Confirmed");
        assertEquals("Confirmed", service.uploadDocument(100, "Annexure", "a.pdf", ".pdf", new byte[1], ANALYST).getItemStatus());
        verify(mail, times(1)).sendItemConfirmedMail(any());
    }

    @Test
    void validateDocumentRecordsTheDecision() {
        assertThrows(IllegalArgumentException.class, () -> service.validateDocument(99, "Accepted", "ok", REVIEWER));
        ApprovalForumAgidAgendaItemDocumentEntity doc = document(5, "Credit Paper", "Pending", 1);
        when(repository.getDocumentForValidation(5)).thenReturn(doc);
        item.setItemStatus("Submitted");
        assertTrue(failure(() -> service.validateDocument(5, "Maybe", "ok", REVIEWER)).contains("must be Pending, Accepted or Rejected"));
        assertTrue(failure(() -> service.validateDocument(5, "Rejected", " ", REVIEWER)).contains("comment is required when rejecting"));
        item.setItemStatus("Approved");
        assertTrue(failure(() -> service.validateDocument(5, "Accepted", "ok", REVIEWER)).contains("review is closed"));
        item.setItemStatus("Submitted");

        AgendaItemDocumentDTO accepted = service.validateDocument(5, "Accepted", "Figures agree", REVIEWER);

        assertEquals("Accepted", accepted.getValidationStatus());
        assertEquals(REVIEWER, accepted.getValidatedBy());
        assertEquals("Figures agree", accepted.getValidationComment());
        assertNotNull(accepted.getValidatedOn());
        assertEquals(100, accepted.getAgiId());
    }

    @Test
    void documentReadsReturnNullWhenMissing() {
        assertNull(service.getDocument(5));
        assertNull(service.getDocumentData(5));
        ApprovalForumAgidAgendaItemDocumentEntity doc = document(5, "Credit Paper", "Accepted", 1);
        doc.setFileData(new byte[] { 9, 9 });
        doc.setFileSize(2L);
        when(repository.getDocument(5)).thenReturn(doc);
        assertEquals("credit-paper.pdf", service.getDocument(5).getFileName());
        assertEquals(Long.valueOf(2), service.getDocument(5).getFileSize());
        assertArrayEquals(new byte[] { 9, 9 }, service.getDocumentData(5));
    }

    // ---- Remove, re-open, reject ----

    @Test
    @DisplayName("Removing an item that was never approved does not renumber; the analysts are mailed and a mail failure is swallowed")
    void removeItemBeforeApproval() {
        assertThrows(IllegalArgumentException.class, () -> service.removeItem(999, SECRETARY, "x"));
        held();
        assertTrue(failure(() -> service.removeItem(100, SECRETARY, "x")).contains("has been held"));
        meeting.setStatus("Draft");
        doThrow(new RuntimeException("smtp")).when(mail).sendItemRemovedMail(any());

        AgendaItemDTO removed = service.removeItem(100, SECRETARY, "Withdrawn");

        assertEquals("Removed", removed.getItemStatus());
        verify(repository, times(1)).saveItem(any());
        ApprovalForumAgwAgendaWorkflowActionEntity action = actions().get(0);
        assertEquals("Remove", action.getAction());
        assertEquals("Confirmed", action.getFromStatus());
        assertEquals("Withdrawn", action.getComments());
        verify(mail).sendItemRemovedMail(any());
        verifyNoInteractions(calendar);
    }

    @Test
    @DisplayName("Re-open: unknown, held or cancelled meeting, a blank reason, or an item that is not in the workflow are refused")
    void reopenRefusals() {
        assertThrows(IllegalArgumentException.class, () -> service.reopenItem(999, "x", SECRETARY));
        held();
        assertTrue(failure(() -> service.reopenItem(100, "x", SECRETARY)).contains("has been held"));
        meeting.setStatus("Cancelled");
        assertTrue(failure(() -> service.reopenItem(100, "x", SECRETARY)).contains("has been cancelled"));
        meeting.setStatus("Draft");
        item.setItemStatus("Submitted");
        assertTrue(failure(() -> service.reopenItem(100, " ", SECRETARY)).contains("reason is required"));
        for (String status : new String[] { "Captured", "Confirmed", "Returned", "Removed" }) {
            item.setItemStatus(status);
            assertTrue(failure(() -> service.reopenItem(100, "Fix", SECRETARY)).contains("does not need re-opening"));
        }
        verify(repository, never()).saveItem(any());
    }

    @Test
    @DisplayName("Re-open takes the item back to Confirmed with its paper, else Captured; not an override on an open meeting")
    void reopenGoesBackToTheAnalyst() {
        item.setItemStatus("Rejected");
        assertEquals("Captured", service.reopenItem(100, "Decision reversed", SECRETARY).getItemStatus(), "no paper on file");
        ApprovalForumAgwAgendaWorkflowActionEntity action = actions().get(0);
        assertEquals("Reopen", action.getAction());
        assertEquals("Rejected", action.getFromStatus());
        assertEquals("Captured", action.getToStatus());
        assertFalse(action.isOverrideFlag());
        assertNull(action.getOverrideReason());
        assertEquals("Decision reversed", action.getComments());

        document(5, "Credit Paper", "Accepted", 1);
        item.setItemStatus("Submitted");
        assertEquals("Confirmed", service.reopenItem(100, "Correction", SECRETARY).getItemStatus());
        verifyNoInteractions(calendar, mail);
    }

    @Test
    @DisplayName("Re-opening an approved item cancels its invitation, drops its number and closes the gap in the running order")
    void reopenApprovedItemRenumbers() {
        document(5, "Credit Paper", "Accepted", 1);
        item.setItemStatus("Approved");
        item.setAgendaNumber("3.1");
        item.setGraphEventId("event-item:100");
        item.setInvitationSentOn(Timestamp.valueOf("2026-10-20 10:00:00"));
        ApprovalForumAgiAgendaItemEntity later = otherItem(101, "Approved", "10:00", "10:30");
        later.setAgendaNumber("3.2");

        AgendaItemDTO reopened = service.reopenItem(100, "Chair wants changes", SECRETARY);

        assertEquals("Confirmed", reopened.getItemStatus());
        assertNull(item.getAgendaNumber());
        assertNull(item.getGraphEventId());
        assertNull(item.getInvitationSentOn());
        assertEquals("3.1", later.getAgendaNumber());
        verify(calendar).cancelEvent("event-item:100", "Chair wants changes");
        assertEquals(Arrays.asList("InvitationCancelled", "Reopen"),
                actions().stream().map(ApprovalForumAgwAgendaWorkflowActionEntity::getAction).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("Reject needs a reason and a reviewed item; it cancels any invitation and mails the analysts the reason")
    void rejectItem() {
        item.setItemStatus("Reviewed");
        assertTrue(failure(() -> service.rejectItem(100, " ", CHAIR)).contains("reason is required"));
        item.setGraphEventId("event-item:100");

        AgendaItemDTO rejected = service.rejectItem(100, "Outside mandate", CHAIR);

        assertEquals("Rejected", rejected.getItemStatus());
        assertNull(item.getGraphEventId());
        verify(calendar).cancelEvent("event-item:100", "Outside mandate");
        assertEquals(Arrays.asList("InvitationCancelled", "Reject"),
                actions().stream().map(ApprovalForumAgwAgendaWorkflowActionEntity::getAction).collect(Collectors.toList()));
        verify(mail).sendItemRejectedMail(any(), eq("Outside mandate"));
        held();
        item.setItemStatus("Reviewed");
        assertTrue(failure(() -> service.rejectItem(100, "x", CHAIR)).contains("has been held"));
    }

    @Test
    void completedReviewTellsTheChairAndSecretary() {
        item.setItemStatus("Submitted");
        document(5, "Credit Paper", "Accepted", 1);
        participant(1, "Chair", CHAIR, null);
        participant(2, "IndependentMember", "indep@rmb.co.za", null);
        service.completeReview(100, "Checked", REVIEWER);
        verify(mail).sendItemReviewedMail(any(), eq(CHAIR + ";" + SECRETARY));
        committee.setChairId(null);
        item.setItemStatus("Submitted");
        service.completeReview(100, "Checked", REVIEWER);
        verify(mail).sendItemReviewedMail(any(), eq(";" + SECRETARY));
    }

    @Test
    void workflowActionsMapActorAndTimestamp() {
        ApprovalForumAgwAgendaWorkflowActionEntity entity = new ApprovalForumAgwAgendaWorkflowActionEntity();
        entity.setAgwId(7);
        entity.setAgiId(100);
        entity.setAction("Submit");
        entity.setFromStatus("Confirmed");
        entity.setToStatus("Submitted");
        entity.setComments("c");
        entity.setOverrideFlag(true);
        entity.setOverrideReason("why");
        entity.setCreatedBy(ANALYST);
        entity.setCreatedOn(Timestamp.valueOf("2026-10-20 10:00:00"));
        when(repository.getWorkflowActionsForItem(100)).thenReturn(Collections.singletonList(entity));
        AgendaWorkflowActionDTO dto = service.getWorkflowActions(100).get(0);
        assertEquals(7, dto.getAgwId());
        assertEquals(Integer.valueOf(100), dto.getAgiId());
        assertEquals("Submit", dto.getAction());
        assertEquals("Confirmed", dto.getFromStatus());
        assertEquals("Submitted", dto.getToStatus());
        assertEquals("c", dto.getComments());
        assertTrue(dto.isOverrideFlag());
        assertEquals("why", dto.getOverrideReason());
        assertEquals(ANALYST, dto.getActionBy());
        assertEquals(Timestamp.valueOf("2026-10-20 10:00:00"), dto.getActionOn());
    }

    // ---- Schedulers ----

    @Test
    void cutOffProcessingRemovesOnlyUnconfirmedItems() {
        item.setItemStatus("Captured");
        otherItem(101, "Confirmed", "10:00", "10:30");
        when(repository.getMeetingsPastCutOff(any())).thenReturn(Collections.singletonList(meeting));
        assertEquals(1, service.processCutOffs());
        assertEquals("Removed", item.getItemStatus());
        assertEquals("Confirmed", repository.getItem(101).getItemStatus());
        ApprovalForumAgwAgendaWorkflowActionEntity action = actions().get(0);
        assertEquals("SYSTEM", action.getCreatedBy());
        assertEquals("Credit paper not uploaded by the cut-off", action.getComments());
        verify(mail).sendItemRemovedMail(any());
    }

    @Test
    @DisplayName("Reminders go out one and two days before the cut-off for captured items only; failures are not counted")
    void remindersFollowTheWindow() {
        item.setItemStatus("Captured");
        otherItem(101, "Confirmed", "10:00", "10:30");
        LocalDate today = LocalDate.now();
        ApprovalForumAgmAgendaMeetingEntity in1 = scheduled(31, "Draft", today.plusDays(8));
        in1.setCutOffOn(Timestamp.valueOf(today.plusDays(1).atTime(17, 0)));
        ApprovalForumAgmAgendaMeetingEntity in2 = scheduled(32, "Draft", today.plusDays(9));
        in2.setCutOffOn(Timestamp.valueOf(today.plusDays(2).atTime(17, 0)));
        ApprovalForumAgmAgendaMeetingEntity today0 = scheduled(33, "Draft", today.plusDays(7));
        today0.setCutOffOn(Timestamp.valueOf(today.atTime(23, 59)));
        ApprovalForumAgmAgendaMeetingEntity in3 = scheduled(34, "Draft", today.plusDays(10));
        in3.setCutOffOn(Timestamp.valueOf(today.plusDays(3).atTime(17, 0)));
        ApprovalForumAgmAgendaMeetingEntity none = scheduled(35, "Draft", today.plusDays(10));
        for (int agmId : new int[] { 31, 32, 33, 34, 35 }) when(repository.getItemsForMeeting(agmId)).thenReturn(items);
        when(repository.getMeetingsWithCutOffBetween(any(), any())).thenReturn(Arrays.asList(in1, in2, today0, in3, none));

        assertEquals(2, service.processReminders());

        verify(mail).sendItemReminderMail(any(), eq(in1.getCutOffOn()), eq(1));
        verify(mail).sendItemReminderMail(any(), eq(in2.getCutOffOn()), eq(2));
        verify(mail, times(2)).sendItemReminderMail(any(), any(), anyInt());

        doThrow(new RuntimeException("smtp")).when(mail).sendItemReminderMail(any(), any(), anyInt());
        assertEquals(0, service.processReminders());
    }

    @Test
    void notificationHistoryExplainsRejectionsAndLeavesOtherTypesAlone() {
        ApprovalForumAgnAgendaNotificationEntity rejected = new ApprovalForumAgnAgendaNotificationEntity();
        rejected.setAgnId(1);
        rejected.setAgiId(100);
        rejected.setNotificationType("Rejected");
        rejected.setSentOn(Timestamp.valueOf("2026-10-17 16:52:03"));
        rejected.setRecipients("a@x");
        rejected.setSubject("Rejected");
        rejected.setStatus("Sent");
        ApprovalForumAgnAgendaNotificationEntity captured = new ApprovalForumAgnAgendaNotificationEntity();
        captured.setNotificationType("Captured");
        ApprovalForumAgwAgendaWorkflowActionEntity reject = new ApprovalForumAgwAgendaWorkflowActionEntity();
        reject.setAgwId(4);
        reject.setActive(true);
        reject.setToStatus("Rejected");
        reject.setComments("Outside mandate");
        reject.setCreatedBy(CHAIR);
        reject.setCreatedOn(Timestamp.valueOf("2026-10-17 16:52:00"));
        when(repository.getNotificationsForItem(100)).thenReturn(Arrays.asList(rejected, captured));
        when(repository.getWorkflowActionsForItem(100)).thenReturn(Collections.singletonList(reject));

        List<AgendaNotificationDTO> history = service.getNotificationsForItem(100);

        assertTrue(history.get(0).getMessage().contains("was rejected"));
        assertEquals("Outside mandate", history.get(0).getReason());
        assertEquals(CHAIR, history.get(0).getActionBy());
        assertEquals(Integer.valueOf(4), history.get(0).getWorkflowActionId());
        assertEquals("a@x", history.get(0).getRecipients());
        assertEquals("Sent", history.get(0).getStatus());
        assertNull(history.get(1).getMessage());
        assertNull(history.get(1).getReason());
    }

    // ---- Post-meeting capture, sign-off and unlock ----

    private AgendaPostMeetingDTO capture() {
        AgendaPostMeetingDTO dto = new AgendaPostMeetingDTO();
        dto.setAgiId(100);
        dto.setDecision("Approved with conditions");
        dto.setActualMinutes(25);
        dto.setActualPages(12);
        dto.setAttendeesPresent("Chair; Member");
        dto.setMinuteTaker("M Taker");
        dto.setActualDcp(1000000.0);
        return dto;
    }

    @Test
    @DisplayName("Capture is refused for an unknown item, a meeting not yet held or not yet taken place, and a signed-off record")
    void postMeetingCaptureRefusals() {
        AgendaPostMeetingDTO dto = capture();
        dto.setAgiId(999);
        assertThrows(IllegalArgumentException.class, () -> service.savePostMeeting(dto, SECRETARY));
        dto.setAgiId(100);
        assertTrue(failure(() -> service.savePostMeeting(dto, SECRETARY)).contains("has taken place and been marked Held"));
        meeting.setStatus("Held");
        assertTrue(failure(() -> service.savePostMeeting(dto, SECRETARY)).contains("has taken place"), "Held but dated in the future");
        meeting.setMeetingDate(day(lastWeekday()));
        ApprovalForumAgpmAgendaPostMeetingEntity locked = postMeeting(item);
        locked.setLocked(true);
        assertTrue(failure(() -> service.savePostMeeting(dto, SECRETARY)).contains("is locked after sign-off"));
        item.setAgmAgendaMeetingByAgmId(null);
        assertTrue(failure(() -> service.savePostMeeting(dto, SECRETARY)).contains("not linked to a meeting"));
        verify(repository, never()).savePostMeeting(any());
    }

    @Test
    @DisplayName("A first capture creates the record; chair sign-off locks it; the decision date follows the decision type")
    void postMeetingCaptureAndSignOff() {
        held();
        AgendaPostMeetingDTO dto = capture();

        AgendaPostMeetingDTO saved = service.savePostMeeting(dto, SECRETARY);

        ArgumentCaptor<ApprovalForumAgpmAgendaPostMeetingEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgpmAgendaPostMeetingEntity.class);
        verify(repository).savePostMeeting(captor.capture());
        ApprovalForumAgpmAgendaPostMeetingEntity entity = captor.getValue();
        assertSame(item, entity.getAgiAgendaItemByAgiId());
        assertEquals(SECRETARY, entity.getCreatedBy());
        assertTrue(entity.isActive());
        assertEquals("Approved with conditions", saved.getDecision());
        assertEquals(Integer.valueOf(25), saved.getActualMinutes());
        assertEquals(Integer.valueOf(12), saved.getActualPages());
        assertEquals("Chair; Member", saved.getAttendeesPresent());
        assertEquals("M Taker", saved.getMinuteTaker());
        assertEquals(Double.valueOf(1000000.0), saved.getActualDcp());
        assertEquals(100, saved.getAgiId());
        assertFalse(saved.isLocked());
        assertNull(saved.getDecisionDate(), "no decision type, no decision date");

        when(repository.getPostMeetingForItem(100)).thenReturn(entity);
        dto.setDecisionType("Approved");
        AgendaPostMeetingDTO typed = service.savePostMeeting(dto, SECRETARY);
        assertNotNull(typed.getDecisionDate(), "derived when the decision type is first recorded");
        Timestamp derived = typed.getDecisionDate();
        service.savePostMeeting(dto, SECRETARY);
        assertEquals(derived, entity.getDecisionDate(), "kept on later saves");
        dto.setDecisionDate(Timestamp.valueOf("2026-10-20 11:00:00"));
        assertEquals(Timestamp.valueOf("2026-10-20 11:00:00"), service.savePostMeeting(dto, SECRETARY).getDecisionDate());

        dto.setChairSignOffBy("C Chair");
        dto.setChairSignOffOn(Timestamp.valueOf("2026-10-20 12:00:00"));
        AgendaPostMeetingDTO signed = service.savePostMeeting(dto, CHAIR);
        assertTrue(signed.isLocked());
        assertEquals(CHAIR, signed.getLockedBy());
        assertNotNull(signed.getLockedOn());
        verify(repository, times(5)).savePostMeeting(entity);
        assertTrue(failure(() -> service.savePostMeeting(dto, SECRETARY)).contains("locked after sign-off"));
    }

    @Test
    void unlockReopensTheSignedOffRecordAndRecordsWho() {
        assertThrows(IllegalArgumentException.class, () -> service.unlockPostMeeting(100, CHAIR));
        ApprovalForumAgpmAgendaPostMeetingEntity entity = postMeeting(item);
        entity.setLocked(true);
        entity.setLockedBy(CHAIR);
        AgendaPostMeetingDTO unlocked = service.unlockPostMeeting(100, SECRETARY);
        assertFalse(unlocked.isLocked());
        assertEquals(SECRETARY, unlocked.getLockedBy());
        assertNotNull(unlocked.getLockedOn());
        assertEquals(1100, unlocked.getAgpmId());
        verify(repository).savePostMeeting(entity);
        assertNull(service.getPostMeeting(999));
        assertEquals(100, service.getPostMeeting(100).getAgiId());
    }

    // ---- Mark as held ----

    @Test
    void markHeldRefusals() {
        assertThrows(IllegalArgumentException.class, () -> service.markMeetingHeld(99, SECRETARY));
        meeting.setStatus("Cancelled");
        assertTrue(failure(() -> service.markMeetingHeld(10, SECRETARY)).contains("has been cancelled"));
        for (String status : new String[] { "Held", "Closed" }) {
            meeting.setStatus(status);
            assertTrue(failure(() -> service.markMeetingHeld(10, SECRETARY)).contains("already been marked Held"));
        }
        verify(repository, never()).saveMeeting(any());
    }

    @Test
    @DisplayName("The sitting has ended once its end time has passed; without one its start time; an unparseable time or no time falls back to the day")
    void sittingEndedVariants() {
        meeting.setMeetingDate(day(LocalDate.now()));
        meeting.setEndTime(null);
        meeting.setStartTime("23:59");
        assertTrue(failure(() -> service.markMeetingHeld(10, SECRETARY)).contains("before its scheduled end"), "start time only, still to come");
        assertFalse(failure(() -> service.markMeetingHeld(10, SECRETARY)).contains("("), "no end time to quote");

        meeting.setMeetingDate(day(lastWeekday()));
        assertEquals("Held", service.markMeetingHeld(10, SECRETARY).getStatus(), "start time only, yesterday");

        meeting.setStatus("Draft");
        meeting.setStartTime("later");
        meeting.setEndTime("tbc");
        assertEquals("Held", service.markMeetingHeld(10, SECRETARY).getStatus(), "unparseable times fall back to the day");

        meeting.setStatus("Draft");
        meeting.setStartTime(null);
        meeting.setEndTime(null);
        assertEquals("Held", service.markMeetingHeld(10, SECRETARY).getStatus(), "no times, yesterday");

        meeting.setStatus("Compiled");
        meeting.setMeetingDate(null);
        assertEquals("Held", service.markMeetingHeld(10, SECRETARY).getStatus(), "no date cannot be waited for");
        assertEquals("Compiled", actions().get(actions().size() - 1).getFromStatus());
    }

    @Test
    void meetingTodayWithoutTimesCannotBeHeldUntilTheDayIsOver() {
        meeting.setMeetingDate(day(LocalDate.now()));
        meeting.setStartTime(null);
        meeting.setEndTime(null);
        assertTrue(failure(() -> service.markMeetingHeld(10, SECRETARY)).contains("before its scheduled end"));
        assertEquals("Draft", meeting.getStatus());
    }

    @Test
    @DisplayName("Held records the actual times and the quorum outcome: given, else derived from the approved items")
    void markHeldRecordsTimesAndQuorum() {
        meeting.setMeetingDate(day(lastWeekday()));
        assertNull(service.markMeetingHeld(10, "09:05", "11:50", null, SECRETARY).getQuorumAchieved(), "no approved item, nothing to derive");
        assertEquals("09:05", meeting.getActualStartTime());
        assertEquals("11:50", meeting.getActualEndTime());
        assertEquals("Quorum achieved: null", actions().get(0).getComments());

        meeting.setStatus("Draft");
        item.setItemStatus("Approved");
        participant(1, "Chair", CHAIR, null);
        assertEquals(Boolean.FALSE, service.markMeetingHeld(10, null, null, null, SECRETARY).getQuorumAchieved(), "one of two and no exception");
        assertEquals("09:05", meeting.getActualStartTime(), "null actual times leave the recorded ones");

        meeting.setStatus("Draft");
        item.setQuorumOverrideReason("Approved exception");
        assertEquals(Boolean.TRUE, service.markMeetingHeld(10, SECRETARY).getQuorumAchieved());

        meeting.setStatus("Draft");
        item.setQuorumOverrideReason(null);
        participant(2, "IndependentMember", "indep@rmb.co.za", null);
        assertEquals(Boolean.TRUE, service.markMeetingHeld(10, SECRETARY).getQuorumAchieved());

        meeting.setStatus("Draft");
        assertEquals(Boolean.FALSE, service.markMeetingHeld(10, null, null, Boolean.FALSE, SECRETARY).getQuorumAchieved(), "the secretary's answer wins");
    }

    // ---- Close and re-open the meeting record ----

    @Test
    @DisplayName("Close needs Held, and every approved item signed off with a decision or decision type; other items are ignored")
    void closeMeetingRules() {
        assertThrows(IllegalArgumentException.class, () -> service.closeMeeting(99, SECRETARY));
        assertTrue(failure(() -> service.closeMeeting(10, SECRETARY)).contains("must be Held"));
        held();
        item.setItemStatus("Approved");
        ApprovalForumAgpmAgendaPostMeetingEntity pm = postMeeting(item);
        pm.setChairSignOffOn(Timestamp.valueOf("2026-10-20 12:00:00"));
        assertTrue(failure(() -> service.closeMeeting(10, SECRETARY)).contains("Acme Ltd"), "signed off without a decision");
        otherItem(101, "Removed", null, null);
        otherItem(102, "Rejected", null, null);
        pm.setDecisionType("Declined");
        item.setAttendeeEmails("a@example.test");
        doThrow(new RuntimeException("smtp")).when(mail).sendMeetingClosedMail(any(), any());

        AgendaMeetingDTO closed = service.closeMeeting(10, SECRETARY);

        assertEquals("Closed", closed.getStatus());
        assertEquals(SECRETARY, closed.getClosedBy());
        assertNotNull(closed.getClosedOn());
        verify(mail).sendMeetingClosedMail(any(), eq("a@example.test"));
        ApprovalForumAgwAgendaWorkflowActionEntity action = actions().get(0);
        assertEquals("Close", action.getAction());
        assertEquals("Held", action.getFromStatus());
        assertEquals("Closed", action.getToStatus());
    }

    @Test
    void reopenClosedRecordIsAnAuditedOverride() {
        assertThrows(IllegalArgumentException.class, () -> service.reopenMeeting(99, "x", CHAIR));
        assertTrue(failure(() -> service.reopenMeeting(10, "x", CHAIR)).contains("is not closed"));
        meeting.setStatus("Closed");
        meeting.setClosedBy(SECRETARY);
        meeting.setClosedOn(Timestamp.valueOf("2026-10-20 12:00:00"));
        assertTrue(failure(() -> service.reopenMeeting(10, " ", CHAIR)).contains("correction reason is required"));
        verify(repository, never()).saveMeeting(any());

        AgendaMeetingDTO reopened = service.reopenMeeting(10, "Decision wording corrected", CHAIR);

        assertEquals("Held", reopened.getStatus());
        assertNull(reopened.getClosedBy());
        assertNull(reopened.getClosedOn());
        ApprovalForumAgwAgendaWorkflowActionEntity action = actions().get(0);
        assertEquals("Reopen", action.getAction());
        assertEquals("Closed", action.getFromStatus());
        assertEquals("Held", action.getToStatus());
        assertTrue(action.isOverrideFlag());
        assertEquals("Decision wording corrected", action.getOverrideReason());
        assertEquals(CHAIR, action.getCreatedBy());
    }

    // ---- Records ----

    @Test
    void meetingRecordsNeedTypeNameAndLocation() {
        AgendaMeetingRecordDTO dto = new AgendaMeetingRecordDTO();
        dto.setAgmId(99);
        assertThrows(IllegalArgumentException.class, () -> service.addMeetingRecord(dto, SECRETARY));
        dto.setAgmId(10);
        assertTrue(failure(() -> service.addMeetingRecord(dto, SECRETARY)).contains("Record type"));
        dto.setRecordType(" Minutes ");
        assertTrue(failure(() -> service.addMeetingRecord(dto, SECRETARY)).contains("Record name"));
        dto.setName(" CCC minutes ");
        assertTrue(failure(() -> service.addMeetingRecord(dto, SECRETARY)).contains("Record location"));
        dto.setLocation(" https://sp.example.test/minutes ");

        AgendaMeetingRecordDTO saved = service.addMeetingRecord(dto, SECRETARY);

        assertEquals("Minutes", saved.getRecordType());
        assertEquals("CCC minutes", saved.getName());
        assertEquals("https://sp.example.test/minutes", saved.getLocation());
        assertEquals(10, saved.getAgmId());
        assertEquals(SECRETARY, saved.getCreatedBy());
        assertNotNull(saved.getCreatedOn());
        ArgumentCaptor<ApprovalForumAgmrAgendaMeetingRecordEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgmrAgendaMeetingRecordEntity.class);
        verify(repository).saveRecord(captor.capture());
        assertTrue(captor.getValue().isActive());
        when(repository.getRecordsForMeeting(10)).thenReturn(Collections.singletonList(captor.getValue()));
        assertEquals("Minutes", service.getMeetingRecords(10).get(0).getRecordType());
    }

    // ---- Participants ----

    @Test
    @DisplayName("Without participant rows the legacy roster columns are read: scribes do not count towards quorum")
    void legacyRosterIsSynthesised() {
        assertThrows(IllegalArgumentException.class, () -> service.getParticipants(999));
        item.setChair("C Chair");
        item.setIndependentMember("I One; I Two");
        item.setScribe("S Scribe");
        List<AgendaParticipantDTO> roster = service.getParticipants(100);
        assertEquals(Arrays.asList("C Chair", "I One", "I Two", "S Scribe"),
                roster.stream().map(AgendaParticipantDTO::getPersonName).collect(Collectors.toList()));
        assertEquals("IndependentMember", roster.get(1).getCommitteeRole());
        assertTrue(roster.get(1).isIndependentFlag());
        assertTrue(roster.get(1).isQuorumEligible());
        assertFalse(roster.get(3).isQuorumEligible());
        assertFalse(roster.get(3).isVotingEligible());
        assertTrue(roster.get(3).isAttendanceRequired());
        assertEquals(3, service.evaluateQuorum(100).getEligibleCount());
    }

    @Test
    void quorumIgnoresRolesOutsideTheCommitteesListAndCountsPeopleOnce() {
        participant(1, "Chair", CHAIR, null);
        participant(2, "chair", " CHAIR@rmb.co.za ", null);
        participant(3, "Scribe", "scribe@rmb.co.za", null);
        AgendaQuorumResultDTO quorum = service.evaluateQuorum(100);
        assertEquals(1, quorum.getEligibleCount(), "the same person in the same role twice");
        assertEquals(Collections.singletonList("IndependentMember"), quorum.getMissingRoles());
        assertEquals(Collections.singletonList("chair"), quorum.getCountedRoles());
        committee.setQuorumRoles(null);
        assertEquals(2, service.evaluateQuorum(100).getEligibleCount(), "with no configured roles every eligible participant counts");
        item.setAgmAgendaMeetingByAgmId(null);
        AgendaQuorumResultDTO noCommittee = service.evaluateQuorum(100);
        assertEquals(0, noCommittee.getRequired());
        assertFalse(noCommittee.isMet());
    }

    private AgendaParticipantDTO participantDto(int agpId, String role, String name, Boolean attended) {
        AgendaParticipantDTO dto = new AgendaParticipantDTO();
        dto.setAgpId(agpId);
        dto.setCommitteeRole(role);
        dto.setPersonName(name);
        dto.setPersonId(name == null ? null : name.trim().toLowerCase().replace(' ', '.') + "@rmb.co.za");
        dto.setActualAttendance(attended);
        return dto;
    }

    @Test
    @DisplayName("Before the meeting the roster is replaced (blank rows dropped); a locked or cancelled meeting refuses")
    void participantsBeforeTheMeetingAreReplaced() {
        ApprovalForumAgpAgendaParticipantEntity old = participant(1, "Chair", CHAIR, null);
        List<AgendaParticipantDTO> roster = Arrays.asList(
                participantDto(0, " Chair ", " New Chair ", null),
                participantDto(0, "IndependentMember", null, null),
                participantDto(0, " ", "No Role", null),
                participantDto(0, "Scribe", "S Scribe", null));
        doAnswer(inv -> {
            ApprovalForumAgpAgendaParticipantEntity p = inv.getArgument(0);
            if (!p.isActive()) participants.remove(p); else if (!participants.contains(p)) participants.add(p);
            return p;
        }).when(repository).saveParticipant(any());

        List<AgendaParticipantDTO> saved = service.saveParticipants(100, roster, ANALYST);

        assertFalse(old.isActive());
        assertEquals(Arrays.asList("New Chair", "S Scribe"), saved.stream().map(AgendaParticipantDTO::getPersonName).collect(Collectors.toList()));
        assertEquals("Chair", saved.get(0).getCommitteeRole());
        assertEquals("new.chair@rmb.co.za", saved.get(0).getPersonId());

        meeting.setStatus("Locked");
        assertTrue(failure(() -> service.saveParticipants(100, roster, ANALYST)).contains("locked"));
        meeting.setStatus("Cancelled");
        assertTrue(failure(() -> service.saveParticipants(100, roster, ANALYST)).contains("cancelled"));
        meeting.setStatus("Closed");
        assertTrue(failure(() -> service.saveParticipants(100, roster, ANALYST)).contains("participants are final"));
    }

    @Test
    @DisplayName("After the meeting only actual attendance is captured, matched by participant id; nobody is added or removed")
    void participantsAfterTheMeetingRecordAttendance() {
        held();
        ApprovalForumAgpAgendaParticipantEntity chair = participant(1, "Chair", CHAIR, null);
        ApprovalForumAgpAgendaParticipantEntity member = participant(2, "IndependentMember", "indep@rmb.co.za", null);
        List<AgendaParticipantDTO> attendance = Arrays.asList(
                participantDto(1, "Chair", "C Chair", Boolean.TRUE),
                participantDto(0, "Guest", "G Guest", Boolean.TRUE),
                participantDto(9, "Chair", "Nobody", Boolean.FALSE));

        List<AgendaParticipantDTO> saved = service.saveParticipants(100, attendance, SECRETARY);

        assertEquals(Boolean.TRUE, chair.getActualAttendance());
        assertNull(member.getActualAttendance());
        assertTrue(chair.isActive());
        assertEquals(2, saved.size());
        verify(repository, times(1)).saveParticipant(chair);
        verify(repository, never()).saveParticipant(member);
    }

    // ---- Conditions ----

    private AgendaConditionDTO condition(int agcaId, String description, String status) {
        AgendaConditionDTO dto = new AgendaConditionDTO();
        dto.setAgcaId(agcaId);
        dto.setDescription(description);
        dto.setStatus(status);
        dto.setOwnerName("Owner");
        dto.setDueDate(Timestamp.valueOf("2026-12-01 00:00:00"));
        return dto;
    }

    private ApprovalForumAgcaAgendaConditionEntity existingCondition(int agcaId, String description, String status) {
        ApprovalForumAgcaAgendaConditionEntity e = new ApprovalForumAgcaAgendaConditionEntity();
        e.setAgcaId(agcaId);
        e.setAgiAgendaItemByAgiId(item);
        e.setDescription(description);
        e.setStatus(status);
        e.setConditionType("Condition");
        e.setActive(true);
        conditions.add(e);
        return e;
    }

    @Test
    @DisplayName("Conditions are saved after Held and before Closed: new ones open, edited ones kept, closed ones untouched, omitted ones dropped")
    void saveConditions() {
        List<AgendaConditionDTO> incoming = Arrays.asList(condition(0, " Provide audited financials ", null));
        assertTrue(failure(() -> service.saveConditions(100, incoming, SECRETARY)).contains("marked Held"));
        meeting.setStatus("Closed");
        meeting.setMeetingDate(day(lastWeekday()));
        assertTrue(failure(() -> service.saveConditions(100, incoming, SECRETARY)).contains("has taken place"));
        held();
        ApprovalForumAgcaAgendaConditionEntity open = existingCondition(1, "Old wording", "Open");
        ApprovalForumAgcaAgendaConditionEntity closed = existingCondition(2, "Done", "Closed");
        ApprovalForumAgcaAgendaConditionEntity dropped = existingCondition(3, "Gone", "Open");
        AgendaConditionDTO edit = condition(1, "New wording", "Closed");
        AgendaConditionDTO reopenAttempt = condition(2, "Changed", "Open");
        AgendaConditionDTO fresh = condition(0, " Provide audited financials ", "InProgress");
        AgendaConditionDTO blank = condition(0, " ", null);

        service.saveConditions(100, Arrays.asList(edit, reopenAttempt, fresh, blank), SECRETARY);

        assertEquals("New wording", open.getDescription());
        assertEquals("Open", open.getStatus(), "a Closed status is only reached through closeCondition");
        assertEquals("Done", closed.getDescription(), "closed conditions are not edited here");
        assertTrue(closed.isActive());
        assertFalse(dropped.isActive());
        ArgumentCaptor<ApprovalForumAgcaAgendaConditionEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgcaAgendaConditionEntity.class);
        verify(repository, times(3)).saveCondition(captor.capture());
        ApprovalForumAgcaAgendaConditionEntity created = captor.getAllValues().stream().filter(c -> c != open && c != dropped).findFirst().get();
        assertEquals("Provide audited financials", created.getDescription());
        assertEquals("InProgress", created.getStatus());
        assertEquals("Condition", created.getConditionType());
        assertEquals(SECRETARY, created.getCreatedBy());
        assertEquals("Owner", created.getOwnerName());
        assertTrue(created.isActive());
    }

    @Test
    void closeConditionNeedsAFinalStatusAndComments() {
        assertThrows(IllegalArgumentException.class, () -> service.closeCondition(9, "Closed", "done", SECRETARY));
        ApprovalForumAgcaAgendaConditionEntity e = existingCondition(1, "Provide financials", "Open");
        when(repository.getCondition(1)).thenReturn(e);
        assertTrue(failure(() -> service.closeCondition(1, "Open", "x", SECRETARY)).contains("Closed or Waived"));
        assertTrue(failure(() -> service.closeCondition(1, "Waived", " ", SECRETARY)).contains("Closure comments are required"));
        assertEquals("Open", e.getStatus());

        AgendaConditionDTO closed = service.closeCondition(1, "Waived", "  Not needed after restructure  ", SECRETARY);

        assertEquals("Waived", closed.getStatus());
        assertEquals("Not needed after restructure", closed.getClosureComments());
        assertEquals(SECRETARY, closed.getClosedBy());
        assertNotNull(closed.getClosedOn());
        assertEquals(100, closed.getAgiId());
        assertEquals("Provide financials", service.getConditions(100).get(0).getDescription());
    }

    // ---- Reporting ----

    @Test
    @DisplayName("The report also counts papers outstanding, decisions, attendance and quorum outcomes, and files blanks as Unspecified")
    void reportSummaryAggregates() {
        item.setItemStatus("Approved");
        item.setBusinessUnit(null);
        document(5, "Credit Paper", "Accepted", 1);
        ApprovalForumAgpAgendaParticipantEntity chair = participant(1, "Chair", CHAIR, null);
        chair.setPlannedAttendance("Accepted");
        chair.setActualAttendance(Boolean.TRUE);
        ApprovalForumAgpAgendaParticipantEntity member = participant(2, "IndependentMember", "indep@rmb.co.za", null);
        member.setPlannedAttendance("Declined");
        ApprovalForumAgpmAgendaPostMeetingEntity pm = postMeeting(item);
        pm.setActualMinutes(45);
        pm.setDecision("Approved");
        ApprovalForumAgiAgendaItemEntity noPaper = otherItem(101, "Approved", "10:00", "10:30");
        when(repository.getDocumentsForItem(101)).thenReturn(Collections.emptyList());
        when(repository.getParticipantsForItem(101)).thenReturn(Collections.emptyList());
        ApprovalForumAgpmAgendaPostMeetingEntity declined = postMeeting(noPaper);
        declined.setDecisionType("Declined");
        ApprovalForumAgiAgendaItemEntity removed = otherItem(102, "Removed", null, null);
        when(repository.getDocumentsForItem(102)).thenReturn(Collections.emptyList());
        when(repository.getParticipantsForItem(102)).thenReturn(Collections.emptyList());
        ApprovalForumAgmAgendaMeetingEntity other = scheduled(20, "Held", LocalDate.now().minusDays(7));
        when(repository.getMeetingsBetween(any(), any())).thenReturn(Arrays.asList(meeting, other));
        when(repository.getItemsBetween(any(), any())).thenReturn(Arrays.asList(item, noPaper, removed));

        AgendaReportSummaryDTO report = service.getReportSummary(Timestamp.valueOf("2026-01-01 00:00:00"), Timestamp.valueOf("2026-12-31 00:00:00"));

        assertEquals(2, report.getMeetings());
        assertEquals(Integer.valueOf(1), report.getMeetingsByStatus().get("Draft"));
        assertEquals(Integer.valueOf(1), report.getMeetingsByStatus().get("Held"));
        assertEquals(3, report.getItems());
        assertEquals(Integer.valueOf(2), report.getItemsByStatus().get("Approved"));
        assertEquals(Integer.valueOf(3), report.getItemsByCommittee().get("CCC L3"));
        assertEquals(Integer.valueOf(3), report.getItemsByBusinessUnit().get("Unspecified"));
        assertEquals(Integer.valueOf(1), report.getItemsByPurpose().get("Annual review"));
        assertEquals(1, report.getOutstandingMandatoryPapers(), "the removed item does not count, the approved one without a paper does");
        assertEquals(90, report.getPlannedMinutes());
        assertEquals(45, report.getActualMinutes());
        assertEquals(Integer.valueOf(1), report.getDecisions().get("Approved"));
        assertEquals(Integer.valueOf(1), report.getDecisions().get("Declined"));
        assertEquals(2, report.getInvited());
        assertEquals(1, report.getPlannedAttending());
        assertEquals(1, report.getActuallyAttended());
        assertEquals(1, report.getQuorumMet());
        assertEquals(1, report.getQuorumNotMet());
        assertEquals(0, report.getLateItems());
        assertEquals(Timestamp.valueOf("2026-01-01 00:00:00"), report.getFrom());
    }

    // ---- Agenda numbering and invitations ----

    @Test
    @DisplayName("Numbers follow the mandate level digit and the slot order: by start time, then capture time, unslotted last")
    void renumberingFollowsSlotOrder() {
        committee.setMandateLevel("Level 2");
        document(5, "Credit Paper", "Accepted", 1);
        item.setItemStatus("Approved");
        item.setStartTime("10:00");
        item.setEndTime("10:30");
        item.setCreatedOn(Timestamp.valueOf("2026-10-01 09:00:00"));
        ApprovalForumAgiAgendaItemEntity early = otherItem(101, "Approved", "09:00", "09:30");
        ApprovalForumAgiAgendaItemEntity unslottedLate = otherItem(102, "Approved", null, null);
        unslottedLate.setCreatedOn(Timestamp.valueOf("2026-10-02 09:00:00"));
        ApprovalForumAgiAgendaItemEntity unslottedEarly = otherItem(103, "Approved", null, null);
        unslottedEarly.setCreatedOn(Timestamp.valueOf("2026-10-01 08:00:00"));
        otherItem(104, "Submitted", "08:00", "08:30");
        ApprovalForumAgiAgendaItemEntity leaving = otherItem(105, "Approved", "09:30", "10:00");
        leaving.setAgendaNumber("2.2");

        service.reopenItem(105, "Chair wants changes", SECRETARY);

        assertEquals("2.1", early.getAgendaNumber());
        assertEquals("2.2", item.getAgendaNumber());
        assertEquals("2.3", unslottedEarly.getAgendaNumber());
        assertEquals("2.4", unslottedLate.getAgendaNumber());
        assertNull(leaving.getAgendaNumber());
        assertNull(repository.getItem(104).getAgendaNumber(), "items not yet approved stay unnumbered");

        committee.setMandateLevel(null);
        leaving.setItemStatus("Approved");
        service.reopenItem(105, "Again", SECRETARY);
        assertEquals("1.1", early.getAgendaNumber(), "no mandate level means group 1");
    }

    @Test
    @DisplayName("An item's invitation goes to its participants (email, else id), its attendee addresses and all five standing roles, resolved and de-duplicated")
    void itemAttendeesIncludeTheStandingRoles() {
        item.setItemStatus("Approved");
        item.setAttendeeEmails("team@example.test; Chair@rmb.co.za");
        participant(1, "Chair", "chair.id", "member@example.test");
        participant(2, "IndependentMember", "indep.id", null);
        committee.setChairId(CHAIR);
        committee.setAltChairId("ALT@rmb.co.za");
        committee.setMeetingSecretaryId("sec.id");
        committee.setMinuteTakerId("minutes.id");
        committee.setAdministratorId(" admin.id ");
        when(mail.resolveEmails("indep.id;sec.id;minutes.id;admin.id")).thenReturn("indep@rmb.co.za;sec@rmb.co.za;minutes@rmb.co.za;admin@rmb.co.za");
        when(calendar.isConfigured()).thenReturn(true);
        when(calendar.createOrUpdateEvent(any())).thenReturn(new AgendaCalendarService.CalendarEvent("event-1", null));

        service.sendInvitation(10, SECRETARY);

        ArgumentCaptor<AgendaCalendarService.CalendarEventRequest> captor = ArgumentCaptor.forClass(AgendaCalendarService.CalendarEventRequest.class);
        verify(calendar).createOrUpdateEvent(captor.capture());
        assertEquals(Arrays.asList("member@example.test", "team@example.test", "chair@rmb.co.za", "alt@rmb.co.za",
                "indep@rmb.co.za", "sec@rmb.co.za", "minutes@rmb.co.za", "admin@rmb.co.za"), captor.getValue().getAttendees());
        assertEquals("CCC L3 - Acme Ltd", captor.getValue().getSubject());
        assertEquals("09:00", captor.getValue().getStartTime());
        assertEquals("09:30", captor.getValue().getEndTime());
    }

    @Test
    void unresolvedIdsAreInvitedAsTheyAreWhenTheDirectoryFails() {
        item.setItemStatus("Approved");
        item.setAttendeeEmails("team@example.test");
        committee.setChairId(null);
        committee.setMeetingSecretaryId("SEC.ID");
        when(mail.resolveEmails(any())).thenThrow(new RuntimeException("AD down"));
        when(calendar.isConfigured()).thenReturn(true);
        when(calendar.createOrUpdateEvent(any())).thenReturn(new AgendaCalendarService.CalendarEvent("event-1", null));
        service.sendInvitation(10, SECRETARY);
        ArgumentCaptor<AgendaCalendarService.CalendarEventRequest> captor = ArgumentCaptor.forClass(AgendaCalendarService.CalendarEventRequest.class);
        verify(calendar).createOrUpdateEvent(captor.capture());
        assertEquals(Arrays.asList("team@example.test", "sec.id"), captor.getValue().getAttendees());
    }

    @Test
    void itemWithoutSlotIsInvitedForTheMeetingStartAndItsDuration() {
        item.setItemStatus("Approved");
        item.setStartTime(null);
        item.setEndTime(null);
        item.setEstimatedMinutes(45);
        item.setAttendeeEmails("team@example.test");
        when(calendar.isConfigured()).thenReturn(true);
        when(calendar.createOrUpdateEvent(any())).thenReturn(new AgendaCalendarService.CalendarEvent("event-1", "https://teams.example.test/1"));
        AgendaMeetingDTO sent = service.sendInvitation(10, SECRETARY);
        ArgumentCaptor<AgendaCalendarService.CalendarEventRequest> captor = ArgumentCaptor.forClass(AgendaCalendarService.CalendarEventRequest.class);
        verify(calendar).createOrUpdateEvent(captor.capture());
        assertEquals("09:00", captor.getValue().getStartTime());
        assertEquals("09:45", captor.getValue().getEndTime());
        assertEquals("https://teams.example.test/1", item.getTeamsJoinUrl());
        assertEquals("3 participant(s) invited for the meeting.", sent.getInvitationResults().get(0).getMessage(),
                "the team address plus the committee's chair and secretary");
        assertEquals("Acme Ltd", sent.getInvitationResults().get(0).getLabel());
    }

    // ---- Booking drafts list ----

    @Test
    void draftListLeavesThePayloadOutAndNamesTheCommittee() {
        ApprovalForumAgbdAgendaBookingDraftEntity draft = new ApprovalForumAgbdAgendaBookingDraftEntity();
        draft.setAgbdId(7);
        draft.setAgcId(1);
        draft.setOwnerId(ANALYST);
        draft.setCounterpartyName("Acme");
        draft.setStep(2);
        draft.setPayload("{\"step\":2}");
        draft.setActive(true);
        ApprovalForumAgbdAgendaBookingDraftEntity orphan = new ApprovalForumAgbdAgendaBookingDraftEntity();
        orphan.setAgbdId(8);
        orphan.setAgcId(99);
        when(repository.getBookingDrafts(ANALYST)).thenReturn(Arrays.asList(draft, orphan));
        List<AgendaBookingDraftDTO> drafts = service.getBookingDrafts(ANALYST);
        assertEquals(7, drafts.get(0).getAgbdId());
        assertEquals("CCC - L3", drafts.get(0).getCommitteeName());
        assertEquals("Acme", drafts.get(0).getCounterpartyName());
        assertEquals(2, drafts.get(0).getStep());
        assertNull(drafts.get(0).getPayload(), "the list is light; the payload comes with one draft");
        assertNull(drafts.get(1).getCommitteeName());
        when(repository.getBookingDraft(7)).thenReturn(draft);
        assertEquals("{\"step\":2}", service.getBookingDraft(7, ANALYST).getPayload());
    }
}
