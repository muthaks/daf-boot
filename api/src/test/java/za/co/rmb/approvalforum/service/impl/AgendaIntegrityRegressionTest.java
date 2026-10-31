package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real service rules for G1/G2/G3/G6/G9, using only an in-memory mocked repository. */
class AgendaIntegrityRegressionTest {
    private static final String CALLER = "committee.chair@rmb.co.za";
    private static final String OWNER = "credit.analyst@rmb.co.za";
    private static final AgendaAccess CHAIR = new AgendaAccess(false, true, false, false);
    private ApprovalForumAgendaRepository repository;
    private AgendaServiceImpl service;
    private ApprovalForumAgcAgendaCommitteeEntity committee;
    private ApprovalForumAgmAgendaMeetingEntity meeting;
    private ApprovalForumAgiAgendaItemEntity item;
    private ApprovalForumAgcaAgendaConditionEntity condition;

    @BeforeEach
    void setUp() {
        repository = mock(ApprovalForumAgendaRepository.class);
        AgendaMailService mail = mock(AgendaMailService.class);
        when(mail.resolveEmails(anyString())).thenAnswer(call -> call.getArgument(0));
        service = new AgendaServiceImpl(repository, mail, mock(ApprovalForumListManagementService.class), mock(AgendaCalendarService.class));
        committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1); committee.setActive(true); committee.setCommitteeName("CCC");
        committee.setChairId(CALLER); committee.setMeetingSecretaryId("committee.secretary@rmb.co.za");
        meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10); meeting.setActive(true); meeting.setStatus("Held");
        meeting.setMeetingDate(Timestamp.valueOf("2026-10-27 00:00:00"));
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(100); item.setActive(true); item.setItemStatus("Approved");
        item.setCreatedBy(OWNER); item.setPrimaryAnalystId(OWNER);
        item.setCounterpartyName("Protected Holdings"); item.setReviewAnalystId("review.analyst@rmb.co.za");
        item.setAgmAgendaMeetingByAgmId(meeting); item.setStartTime("09:00"); item.setEndTime("09:30"); item.setEstimatedMinutes(30);
        condition = new ApprovalForumAgcaAgendaConditionEntity();
        condition.setAgcaId(500); condition.setActive(true); condition.setStatus("Open"); condition.setAgiAgendaItemByAgiId(item);
        when(repository.getItem(100)).thenReturn(item);
        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.getMeeting(10)).thenReturn(meeting);
        when(repository.getItemsForMeeting(10)).thenReturn(List.of(item));
        when(repository.getCondition(500)).thenReturn(condition);
        when(repository.saveCondition(any())).thenAnswer(call -> call.getArgument(0));
        when(repository.savePostMeeting(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void G1_documentAccessUsesItsOwningItemRatherThanTheDocumentIdAlone() {
        ApprovalForumAgidAgendaItemDocumentEntity document = new ApprovalForumAgidAgendaItemDocumentEntity();
        document.setAgiAgendaItemByAgiId(item);
        when(repository.getDocument(50)).thenReturn(document);
        assertFalse(service.canViewDocument(50, "unrelated.user@rmb.co.za", AgendaAccess.none()));
        assertTrue(service.canViewDocument(50, OWNER, AgendaAccess.none()));
        assertFalse(service.canViewDocument(999, OWNER, AgendaAccess.none()));
    }

    @Test
    void G2_G6_conditionScopeRequiresTheActualCommitteeAndTheRelevantRole() {
        assertTrue(service.canCloseCondition(500, CALLER, CHAIR));
        assertFalse(service.canCloseCondition(500, "other.chair@rmb.co.za", CHAIR));
        assertFalse(service.canCloseCondition(500, CALLER, AgendaAccess.none()));
        assertFalse(service.canCloseCondition(999, CALLER, CHAIR));
    }

    @TestFactory
    Stream<DynamicTest> G2_conditionsCannotBeClosedOrWaivedOnClosedMeetings() {
        return Stream.of("Closed", "Waived").map(status -> DynamicTest.dynamicTest("G2 closed meeting: " + status, () -> {
            meeting.setStatus("Closed");
            clearInvocations(repository);
            assertThrows(IllegalStateException.class, () -> service.closeCondition(500, status, "Evidence checked", CALLER));
            verify(repository, never()).saveCondition(any());
            assertEquals("Open", condition.getStatus());
        }));
    }

    @TestFactory
    Stream<DynamicTest> G2_authorizedClosureOnHeldMeetingsRetainsItsAudit() {
        return Stream.of("Closed", "Waived").map(status -> DynamicTest.dynamicTest("G2 held meeting: " + status, () -> {
            condition.setStatus("Open");
            AgendaConditionDTO saved = service.closeCondition(500, status, "Evidence checked", CALLER);
            assertEquals(status, saved.getStatus());
            assertEquals(CALLER, saved.getClosedBy());
            assertNotNull(saved.getClosedOn());
        }));
    }

    @Test
    void G3_unrelatedReaderGetsOnlyBusySlotDataAndNoConfidentialItemDetails() {
        AgendaMeetingDTO result = service.getMeetingFor(10, "unrelated.user@rmb.co.za", AgendaAccess.none());
        for (AgendaItemDTO visible : result.getItems()) {
            assertNull(visible.getCounterpartyName(), "G3 counterparty must not leak through meeting detail");
            assertNull(visible.getPrimaryAnalystId());
            assertNull(visible.getReviewAnalystId());
            assertNull(visible.getPostMeeting());
            assertTrue(visible.getDocuments() == null || visible.getDocuments().isEmpty());
        }
    }

    @Test
    void G3_ownerStillGetsFullDetailsThroughTheScopedMeetingRead() {
        AgendaMeetingDTO result = service.getMeetingFor(10, OWNER, AgendaAccess.none());
        assertEquals(1, result.getItems().size());
        assertEquals("Protected Holdings", result.getItems().get(0).getCounterpartyName());
    }

    @Test
    void G6_globalChairRoleDoesNotConferAnotherCommitteesAdministrationRights() {
        assertTrue(service.decidesForCommittee(1, CALLER, CHAIR));
        assertTrue(service.decidesForMeeting(10, CALLER, CHAIR));
        assertFalse(service.decidesForCommittee(1, "other.chair@rmb.co.za", CHAIR));
        assertFalse(service.decidesForMeeting(10, "other.chair@rmb.co.za", CHAIR));
        assertFalse(service.decidesForMeeting(10, CALLER, AgendaAccess.none()));
    }

    @TestFactory
    Stream<DynamicTest> G9_signerAndTimestampComeFromTheServerNotTheRequest() {
        return Stream.of(false, true).map(chair -> DynamicTest.dynamicTest("G9 " + (chair ? "chair" : "analyst") + " sign-off", () -> {
            AgendaPostMeetingDTO payload = new AgendaPostMeetingDTO(); payload.setAgiId(100);
            Timestamp forgedTime = Timestamp.valueOf("2000-01-01 00:00:00");
            if (chair) { payload.setChairSignOffBy("forged.person@rmb.co.za"); payload.setChairSignOffOn(forgedTime); }
            else { payload.setAnalystSignOffBy("forged.person@rmb.co.za"); payload.setAnalystSignOffOn(forgedTime); }
            Instant before = Instant.now().minusSeconds(1);
            AgendaPostMeetingDTO saved = service.savePostMeeting(payload, CALLER);
            String signer = chair ? saved.getChairSignOffBy() : saved.getAnalystSignOffBy();
            Timestamp signedOn = chair ? saved.getChairSignOffOn() : saved.getAnalystSignOffOn();
            assertEquals(CALLER, signer, "G9 signer must be the authenticated caller");
            assertNotNull(signedOn);
            assertFalse(signedOn.toInstant().isBefore(before), "G9 caller must not backdate sign-off");
            assertFalse(signedOn.toInstant().isAfter(Instant.now().plusSeconds(1)), "G9 signer time must be server time");
        }));
    }

    @Test
    void G9_anExistingSignatureCannotBeReplacedByAnotherCallersPayload() {
        ApprovalForumAgpmAgendaPostMeetingEntity existing = new ApprovalForumAgpmAgendaPostMeetingEntity();
        existing.setAgiAgendaItemByAgiId(item);
        existing.setAnalystSignOffBy(OWNER);
        Timestamp original = Timestamp.valueOf("2026-10-27 10:00:00");
        existing.setAnalystSignOffOn(original);
        when(repository.getPostMeetingForItem(100)).thenReturn(existing);
        AgendaPostMeetingDTO payload = new AgendaPostMeetingDTO(); payload.setAgiId(100);
        payload.setAnalystSignOffBy("forged.person@rmb.co.za");
        payload.setAnalystSignOffOn(Timestamp.valueOf("2000-01-01 00:00:00"));
        AgendaPostMeetingDTO saved = service.savePostMeeting(payload, CALLER);
        assertEquals(OWNER, saved.getAnalystSignOffBy());
        assertEquals(original, saved.getAnalystSignOffOn());
    }
}
