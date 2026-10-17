package za.co.rmb.approvalforum.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.*;
import za.co.rmb.approvalforum.model.agenda.*;
import za.co.rmb.approvalforum.service.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class AgendaBookingSlotTest {
    private final ApprovalForumAgendaRepository repository = mock(ApprovalForumAgendaRepository.class);
    private final AgendaMailService mail = mock(AgendaMailService.class);
    private final AgendaServiceImpl service = new AgendaServiceImpl(repository, mail,
        mock(ApprovalForumListManagementService.class), mock(AgendaCalendarService.class));
    private ApprovalForumAgmAgendaMeetingEntity meeting;
    @BeforeEach void setup() {
        ApprovalForumAgcAgendaCommitteeEntity committee = new ApprovalForumAgcAgendaCommitteeEntity();
        committee.setAgcId(1); committee.setActive(true); committee.setQuorumSize(2);
        meeting = new ApprovalForumAgmAgendaMeetingEntity(); meeting.setAgmId(10); meeting.setStatus("Draft");
        meeting.setAgcAgendaCommitteeByAgcId(committee);
        meeting.setMeetingDate(Timestamp.valueOf(LocalDateTime.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY))));
        when(repository.getCommittee(1)).thenReturn(committee);
        when(repository.findMeeting(eq(1), any())).thenReturn(meeting);
        when(repository.saveItem(any())).thenAnswer(inv -> { ApprovalForumAgiAgendaItemEntity item = inv.getArgument(0); item.setAgiId(123); return item; });
    }
    private BookAgendaItemRequest request(String start, Integer duration, String forgedEnd) {
        AgendaItemDTO dto = new AgendaItemDTO(); dto.setCounterpartyName("Slot test"); dto.setPurposeOfSubmission("Annual review");
        dto.setPrimaryAnalyst("Analyst"); dto.setPrimaryAnalystId("analyst@example.test");
        dto.setReviewAnalyst("Reviewer"); dto.setReviewAnalystId("reviewer@example.test");
        dto.setStartTime(start); dto.setEstimatedMinutes(duration); dto.setEndTime(forgedEnd);
        BookAgendaItemRequest request = new BookAgendaItemRequest(); request.setAgcId(1); request.setMeetingDate(meeting.getMeetingDate()); request.setItem(dto);
        return request;
    }
    private void existing(String start, String end, Integer duration, String status) {
        ApprovalForumAgiAgendaItemEntity other = new ApprovalForumAgiAgendaItemEntity(); other.setAgiId(99);
        other.setStartTime(start); other.setEndTime(end); other.setEstimatedMinutes(duration); other.setItemStatus(status);
        other.setCounterpartyName("Existing paper"); other.setCreatedBy("another-analyst@example.test");
        when(repository.getItemsForMeeting(10)).thenReturn(List.of(other));
    }
    private void rejected(BookAgendaItemRequest request, String message) {
        assertTrue(assertThrows(IllegalStateException.class, () -> service.bookItem(request, "booker")).getMessage().contains(message));
        verify(repository, never()).saveItem(any()); verify(repository, never()).saveMeeting(any()); verifyNoInteractions(mail);
    }
    @Test void startTimeIsRequiredAndMustBeValid() {
        for (String value : new String[] { null, "", "24:00", "09:99", "bad" }) rejected(request(value, 30, "10:00"), "start time");
    }
    @Test void durationMustBePositive() {
        for (Integer value : new Integer[] { null, 0, -1 }) rejected(request("09:00", value, null), "duration");
    }
    @Test void serverCalculatesEndAndIgnoresSubmittedEnd() {
        AgendaItemDTO saved = service.bookItem(request("09:00", 60, "09:01"), "booker");
        assertEquals("10:00", saved.getEndTime()); assertEquals("09:00", saved.getStartTime());
    }
    @Test void endMayBeOmittedBecauseTheServerDerivesIt() {
        assertEquals("09:15", service.bookItem(request("09:00", 15, null), "booker").getEndTime());
    }
    @Test void midnightOverflowIsRejectedBeforeMeetingLookup() {
        rejected(request("23:55", 15, "23:59"), "midnight"); verify(repository, never()).findMeeting(anyInt(), any());
    }
    @Test void allOverlapShapesAreRejectedIncludingOtherAnalysts() {
        existing("09:00", "09:30", 30, "Confirmed");
        for (String start : List.of("08:45", "09:00", "09:15")) rejected(request(start, 30, null), "overlaps");
        rejected(request("08:45", 60, null), "overlaps");
    }
    @Test void aForgedShortEndCannotBypassOverlap() {
        existing("09:15", "10:00", 45, "Confirmed"); rejected(request("09:00", 30, "09:05"), "overlaps");
    }
    @Test void adjacentSlotsAreAllowedAtBothBoundaries() {
        existing("09:00", "09:30", 30, "Confirmed");
        assertEquals("09:00", service.bookItem(request("08:30", 30, null), "booker").getEndTime());
        assertEquals("10:00", service.bookItem(request("09:30", 30, null), "booker").getEndTime());
    }
    @Test void legacyMissingEndIsDerivedFromItsDuration() {
        existing("09:00", null, 60, "Confirmed"); rejected(request("09:30", 30, null), "overlaps");
    }
    @Test void removedItemsDoNotReserveTime() {
        existing("09:00", "10:00", 60, "Removed");
        assertEquals("09:30", service.bookItem(request("09:00", 30, null), "booker").getEndTime());
    }
    @Test void locksTheCommitteeBeforeReadingAvailabilityAndWritingTheReservation() {
        service.bookItem(request("09:00", 30, null), "booker");
        org.mockito.InOrder order = inOrder(repository);
        order.verify(repository).lockCommitteeBookings(1);
        order.verify(repository).findMeeting(eq(1), any());
        order.verify(repository).getItemsForMeeting(10);
        order.verify(repository).saveItem(any());
    }
    @Test void lateReasonIsCheckedBeforeCreatingTheMeetingOrSavingAnItem() {
        when(repository.findMeeting(eq(1), any())).thenReturn(null);
        BookAgendaItemRequest request = request("09:00", 30, null);
        java.time.LocalDate meetingDay = java.time.LocalDate.now();
        while (meetingDay.getDayOfWeek() == DayOfWeek.SATURDAY || meetingDay.getDayOfWeek() == DayOfWeek.SUNDAY)
            meetingDay = meetingDay.plusDays(1);
        request.setMeetingDate(Timestamp.valueOf(meetingDay.atStartOfDay()));
        rejected(request, "late submission reason");
    }
    @Test void updatesCannotUseAForgedEndToBypassOverlap() {
        ApprovalForumAgiAgendaItemEntity item = new ApprovalForumAgiAgendaItemEntity();
        item.setAgiId(123); item.setItemStatus("Confirmed"); item.setAgmAgendaMeetingByAgmId(meeting);
        when(repository.getItem(123)).thenReturn(item);
        existing("09:30", "10:00", 30, "Confirmed");
        AgendaItemDTO dto = request("09:00", 60, "09:01").getItem(); dto.setAgiId(123);
        assertTrue(assertThrows(IllegalStateException.class, () -> service.updateItem(dto, "booker")).getMessage().contains("overlaps"));
        verify(repository, never()).saveItem(any());
    }
}
