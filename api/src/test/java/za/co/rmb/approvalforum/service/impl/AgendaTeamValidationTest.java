package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.Test;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.BookAgendaItemRequest;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.service.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AgendaTeamValidationTest {
    private static final List<BiConsumer<AgendaItemDTO, String>> IDS = List.of(
        AgendaItemDTO::setPrimaryAnalystId, AgendaItemDTO::setReviewAnalystId,
        AgendaItemDTO::setCreditSponsorId, AgendaItemDTO::setBuHeadId);

    private AgendaItemDTO valid() {
        AgendaItemDTO dto = new AgendaItemDTO();
        dto.setPrimaryAnalyst("Same display name"); dto.setReviewAnalyst("Same display name");
        dto.setCreditSponsor("Same display name"); dto.setBuHead("Same display name");
        for (int i = 0; i < IDS.size(); i++) IDS.get(i).accept(dto, "person" + i + "@example.test");
        return dto;
    }
    @Test void rejectsAllFiveRestrictedPairsOfAccountableRoles() {
        for (int i = 0; i < IDS.size(); i++) for (int j = i + 1; j < IDS.size(); j++) {
            if (i == 2 && j == 3) continue;
            AgendaItemDTO dto = valid(); IDS.get(i).accept(dto, "RMB\\Alice"); IDS.get(j).accept(dto, " ALICE@rmb.co.za ");
            assertTrue(assertThrows(IllegalStateException.class,
                () -> AgendaTeamValidation.validate(dto, "booker")).getMessage().contains("different people"));
        }
    }
    @Test void sponsorAndBuHeadMayShareAnAccountIncludingAliases() {
        AgendaItemDTO dto = valid(); dto.setCreditSponsorId("RMB\\Sponsor"); dto.setBuHeadId(" SPONSOR@rmb.co.za ");
        assertDoesNotThrow(() -> AgendaTeamValidation.validate(dto, "booker"));
    }
    @Test void permitsDifferentAccountsWithIdenticalDisplayNames() {
        assertDoesNotThrow(() -> AgendaTeamValidation.validate(valid(), "booker"));
    }
    @Test void keepsExternalDomainsDistinct() {
        AgendaItemDTO dto = valid(); dto.setPrimaryAnalystId("alice@one.test"); dto.setReviewAnalystId("alice@two.test");
        assertDoesNotThrow(() -> AgendaTeamValidation.validate(dto, "booker"));
    }
    @Test void unresolvedImportedAssignmentsRequireDirectorySelection() {
        AgendaItemDTO dto = valid(); dto.setReviewAnalystId(null);
        assertTrue(assertThrows(IllegalStateException.class,
            () -> AgendaTeamValidation.validate(dto, "booker")).getMessage().contains("directory"));
    }
    @Test void bookingAnalystCannotBeTheirOwnReviewer() {
        AgendaItemDTO dto = valid(); dto.setReviewAnalystId("alice@rmb.co.za");
        assertTrue(assertThrows(IllegalStateException.class,
            () -> AgendaTeamValidation.validate(dto, "RMB\\ALICE")).getMessage().contains("cannot review their own"));
    }
    @Test void optionalRolesCanBeCleared() {
        AgendaItemDTO dto = valid(); dto.setCreditSponsor(null); dto.setCreditSponsorId(null); dto.setBuHead(null); dto.setBuHeadId(null);
        assertDoesNotThrow(() -> AgendaTeamValidation.validate(dto, "booker"));
    }
    @Test void reviewerIsRequiredEvenWhenThePrimaryAnalystIsVerified() {
        for (String name : new String[] { null, "", "   " }) {
            AgendaItemDTO dto = valid(); dto.setReviewAnalyst(name); dto.setReviewAnalystId(null);
            assertTrue(assertThrows(IllegalStateException.class,
                () -> AgendaTeamValidation.validate(dto, "booker")).getMessage().contains("independent review analyst"));
        }
    }
    @Test void aReviewerIdWithoutANameCannotBypassTheRequirement() {
        AgendaItemDTO dto = valid(); dto.setReviewAnalyst(null);
        assertThrows(IllegalStateException.class, () -> AgendaTeamValidation.validate(dto, "booker"));
    }
    private final ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);
    private final AgendaMailService mail = mock(AgendaMailService.class);
    private final AgendaServiceImpl service = new AgendaServiceImpl(repository, mail,
        mock(ApprovalForumListManagementService.class), mock(AgendaCalendarService.class));

    private ApprovalForumAgiAgendaItemEntity item(String status) {
        ApprovalForumAgcAgendaCommitteeEntity committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1); committee.setActive(true);
        ApprovalForumAgmAgendaMeetingEntity meeting = new ApprovalForumAgmAgendaMeetingEntity();
        meeting.setAgmId(10); meeting.setActive(true); meeting.setStatus("Draft");
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(Timestamp.valueOf(LocalDateTime.now().plusDays(7)));
        ApprovalForumAgiAgendaItemEntity item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(100); item.setActive(true); item.setItemStatus(status); item.setCreatedBy("booker@rmb.co.za");
        item.setPrimaryAnalyst("Primary Analyst"); item.setPrimaryAnalystId("analyst@rmb.co.za");
        item.setReviewAnalyst("Reviewer"); item.setReviewAnalystId("reviewer@rmb.co.za");
        item.setAgmAgendaMeetingByAgmId(meeting);
        when(repository.getItem(100)).thenReturn(item);
        lenient().when(repository.getItemForUpdate(100)).thenReturn(item);
        return item;
    }
    private void noWrites() {
        verify(repository, never()).saveItem(any()); verify(repository, never()).saveMeeting(any());
        verify(repository, never()).saveWorkflowAction(any()); verifyNoInteractions(mail);
    }
    @Test void bookingRejectsDuplicatesBeforeCreatingAMeeting() {
        ApprovalForumAgcAgendaCommitteeEntity committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setActive(true); when(repository.getCommittee(1)).thenReturn(committee);
        AgendaItemDTO dto = valid(); dto.setReviewAnalystId(dto.getPrimaryAnalystId());
        BookAgendaItemRequest request = new BookAgendaItemRequest(); request.setAgcId(1); request.setItem(dto);
        request.setMeetingDate(Timestamp.valueOf(LocalDateTime.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))));
        assertTrue(assertThrows(IllegalStateException.class,
            () -> service.bookItem(request, "booker")).getMessage().contains("different people")); noWrites();
        verify(repository, never()).findMeeting(anyInt(), any());
    }
    @Test void bookingRejectsSaturdayAndSundayBeforeMeetingLookupOrWrites() {
        ApprovalForumAgcAgendaCommitteeEntity committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setActive(true); when(repository.getCommittee(1)).thenReturn(committee);
        for (java.time.DayOfWeek day : List.of(java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY)) {
            BookAgendaItemRequest request = new BookAgendaItemRequest(); request.setAgcId(1); request.setItem(valid());
            request.setMeetingDate(Timestamp.valueOf(LocalDateTime.now().with(java.time.temporal.TemporalAdjusters.next(day))));
            assertTrue(assertThrows(IllegalStateException.class, () -> service.bookItem(request, "booker")).getMessage().contains("Weekends"));
        }
        verify(repository, never()).findMeeting(anyInt(), any()); noWrites();
    }
    @Test void updateRejectsDuplicatesWithoutMutatingStoredItem() {
        ApprovalForumAgiAgendaItemEntity item = item("Confirmed"); AgendaItemDTO dto = valid(); dto.setAgiId(100);
        dto.setReviewAnalystId(dto.getPrimaryAnalystId());
        assertThrows(IllegalStateException.class, () -> service.updateItem(dto, "booker@rmb.co.za"));
        assertEquals("Primary Analyst", item.getPrimaryAnalyst()); noWrites();
    }
    @Test void bookingWithoutReviewerFailsBeforeMeetingLookupOrWrites() {
        ApprovalForumAgcAgendaCommitteeEntity committee = new ApprovalForumAgcAgendaCommitteeEntity();
        when(repository.getCommittee(1)).thenReturn(committee);
        AgendaItemDTO dto = valid(); dto.setReviewAnalyst(null); dto.setReviewAnalystId(null);
        BookAgendaItemRequest request = new BookAgendaItemRequest(); request.setAgcId(1); request.setItem(dto);
        request.setMeetingDate(Timestamp.valueOf(LocalDateTime.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))));
        assertTrue(assertThrows(IllegalStateException.class, () -> service.bookItem(request, "booker")).getMessage().contains("independent review analyst"));
        verify(repository, never()).findMeeting(anyInt(), any()); noWrites();
    }
    @Test void reviewerCannotBeClearedWhenUpdatingAnExistingItem() {
        item("Confirmed"); AgendaItemDTO dto = valid(); dto.setAgiId(100); dto.setReviewAnalyst(null); dto.setReviewAnalystId(null);
        assertThrows(IllegalStateException.class, () -> service.updateItem(dto, "booker@rmb.co.za")); noWrites();
    }
    @Test void legacyItemsWithoutAReviewerCannotBeSubmitted() {
        ApprovalForumAgiAgendaItemEntity item = item("Confirmed"); item.setReviewAnalyst(null); item.setReviewAnalystId(null);
        assertThrows(IllegalStateException.class, () -> service.submitItem(100, null, "booker@rmb.co.za"));
        assertEquals("Confirmed", item.getItemStatus()); noWrites();
    }
    @Test void submissionRejectsLegacyDuplicatesBeforeChangingStatus() {
        ApprovalForumAgiAgendaItemEntity item = item("Confirmed"); item.setReviewAnalyst("Reviewer"); item.setReviewAnalystId("RMB\\ANALYST");
        assertTrue(assertThrows(IllegalStateException.class,
            () -> service.submitItem(100, null, "booker")).getMessage().contains("different people"));
        assertEquals("Confirmed", item.getItemStatus()); noWrites();
    }
    @Test void authorsCannotApproveReturnOrRejectTheirOwnItemsEvenUsingAliases() {
        ApprovalForumAgiAgendaItemEntity item = item("Submitted");
        for (String actor : List.of("RMB\\BOOKER", "RMB\\ANALYST")) {
            assertThrows(IllegalStateException.class, () -> service.completeReview(100, "Approved", actor));
            assertThrows(IllegalStateException.class, () -> service.approveItem(100, "Approved", actor));
            assertThrows(IllegalStateException.class, () -> service.returnItem(100, "Fix this", actor));
            assertThrows(IllegalStateException.class, () -> service.rejectItem(100, "Rejected", actor));
        }
        assertEquals("Submitted", item.getItemStatus()); noWrites();
    }
    @Test void authorsCannotValidateTheirOwnDocuments() {
        ApprovalForumAgiAgendaItemEntity item = item("Submitted");
        ApprovalForumAgidAgendaItemDocumentEntity document = new ApprovalForumAgidAgendaItemDocumentEntity();
        document.setAgidId(5); document.setAgiAgendaItemByAgiId(item); when(repository.getDocumentForValidation(5)).thenReturn(document);
        assertThrows(IllegalStateException.class, () -> service.validateDocument(5, "Accepted", "Checked", "RMB\\ANALYST"));
        verify(repository, never()).saveDocument(any()); noWrites();
    }
    @Test void independentReviewersPassTheOwnershipGuard() {
        ApprovalForumAgiAgendaItemEntity item = item("Submitted");
        assertDoesNotThrow(() -> AgendaTeamValidation.requireIndependentReviewer(item, "independent@rmb.co.za"));
    }
}
