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
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgidAgendaItemDocumentEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgnAgendaNotificationEntity;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaQuorumResultDTO;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.mailservice.model.MailModel;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Every agenda notification: who it goes to (user ids resolved to AD mail), its subject, the key
 * content of its template, its attachment, and how the send itself is logged. The mail client is a
 * mock; nothing leaves the test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendaMailServiceImplCoverageTest {

    private static final String TOKEN = "fixture-token";
    private static final String UI = "https://daf.example.test/agenda-builder";

    @Mock
    private MailServiceClient client;
    @Mock
    private ApprovalForumAuthService authService;
    @Mock
    private ApprovalForumAgendaRepository repository;

    private final Map<String, String> settings = new HashMap<>();
    private AgendaMailServiceImpl service;
    private AgendaItemDTO item;
    private AgendaMeetingDTO meeting;

    @BeforeEach
    void setUp() throws Exception {
        settings.put("af.mail.enabled", "true");
        settings.put("af.mail_environment", "PROD");
        settings.put("af.mailservice.token", TOKEN);
        settings.put("af.agenda.ui.base", UI + "  ");
        service = new AgendaMailServiceImpl(client, authService, repository, settings);

        item = new AgendaItemDTO();
        item.setAgiId(123);
        item.setCounterpartyName("Acme & Sons");
        item.setCommitteeName("CCC");
        item.setMandateLevel("L3");
        item.setMeetingDate(Timestamp.valueOf("2026-11-03 00:00:00"));
        item.setStartTime("09:00");
        item.setEndTime("09:30");
        item.setEstimatedMinutes(30);
        item.setPurposeOfSubmission("Annual review");
        item.setRatingBand("BB+");
        item.setBusinessUnit("Corporates");
        item.setNumberOfPages(18);
        item.setPrimaryAnalyst("A Analyst");
        item.setReviewAnalyst("R Reviewer");
        item.setCreditSponsor("S Sponsor");
        item.setBuHead("B Head");
        item.setChair("C Chair");
        item.setCreatedBy("booker");
        item.setPrimaryAnalystId("analyst");
        item.setReviewAnalystId("reviewer");

        meeting = new AgendaMeetingDTO();
        meeting.setAgmId(10);
        meeting.setCommitteeName("CCC");
        meeting.setMandateLevel("L3");
        meeting.setMeetingDate(Timestamp.valueOf("2026-11-03 00:00:00"));
        meeting.setHeldVia("Teams");
        meeting.setQuorumSize(2);
        meeting.setChair("C Chair");

        ad("booker", "booker@rmb.co.za");
        ad("analyst", "analyst@rmb.co.za");
        ad("reviewer", "reviewer@rmb.co.za");
        ad("sec", "secretary@rmb.co.za");
        ad("chair", "chair@rmb.co.za");
        when(repository.getDocumentsForItem(anyInt())).thenReturn(Collections.emptyList());
    }

    private void ad(String id, String mail) throws Exception {
        ActiveDirectoryUserModel user = new ActiveDirectoryUserModel();
        user.setMail(mail);
        when(authService.getUserDetailFromAd(id)).thenReturn(user);
    }

    private MailModel sent() {
        ArgumentCaptor<MailModel> captor = ArgumentCaptor.forClass(MailModel.class);
        verify(client).Send(eq(TOKEN), captor.capture());
        return captor.getValue();
    }

    private ApprovalForumAgnAgendaNotificationEntity logged() {
        ArgumentCaptor<ApprovalForumAgnAgendaNotificationEntity> captor = ArgumentCaptor.forClass(ApprovalForumAgnAgendaNotificationEntity.class);
        verify(repository).saveNotification(captor.capture());
        return captor.getValue();
    }

    private static final String ANALYSTS = "booker@rmb.co.za;analyst@rmb.co.za;reviewer@rmb.co.za";

    private void assertItemMail(MailModel mail, String type, String subject, String badge) {
        assertEquals("approvalForum@rmb.co.za", mail.getFrom());
        assertEquals("Agenda Builder - " + subject + ": Acme & Sons", mail.getSubject());
        assertTrue(mail.getBody().startsWith("<html><body style=\"background-color:white;font-family:calibri;\">"));
        assertTrue(mail.getBody().contains("Good day,"));
        assertTrue(mail.getBody().contains(">" + badge + "<"), badge);
        assertTrue(mail.getBody().contains("Acme &amp; Sons"), "user content is escaped");
        assertFalse(mail.getBody().contains("Acme & Sons"));
        assertTrue(mail.getBody().contains("href=\"" + UI + "/agenda/item/123\""), "deep link, trimmed base");
        assertTrue(mail.getBody().contains("View in Agenda Builder"));
        assertTrue(mail.getBody().contains("AGD-123"));
        assertTrue(mail.getBody().contains("CCC L3"));
        assertTrue(mail.getBody().contains("2026-11-03"));
        assertTrue(mail.getBody().contains("09:00 - 09:30 SAST"));
        assertTrue(mail.getBody().contains("30 minutes"));
        assertTrue(mail.getBody().contains("BB+"));
        assertTrue(mail.getBody().contains(">18<"));
        assertTrue(mail.getBody().contains("R Reviewer"));
        assertTrue(mail.getBody().contains("C Chair"));
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals(type, log.getNotificationType());
        assertEquals(Integer.valueOf(123), log.getAgiId());
        assertNull(log.getAgmId());
        assertEquals("Sent", log.getStatus());
        assertEquals(mail.getTo(), log.getRecipients());
        assertEquals(mail.getSubject(), log.getSubject());
        assertNull(log.getErrorMessage());
        assertNotNull(log.getSentOn());
    }

    // ---- Booking lifecycle ----

    @Test
    void capturedMailGoesToTheSecretariesAndAnalysts() {
        service.sendItemCapturedMail(item, "sec;chair;sec");
        MailModel mail = sent();
        assertEquals("secretary@rmb.co.za;chair@rmb.co.za;" + ANALYSTS, mail.getTo());
        assertItemMail(mail, "Captured", "Item captured", "PENDING PAPER");
        assertTrue(mail.getBody().contains("has been captured on the CCC agenda"));
        assertTrue(mail.getBody().contains("removed from the agenda automatically"));
        assertNull(mail.getAttachments());
    }

    @Test
    void confirmedMailAttachesTheCreditPaper() {
        paper("acme.pdf", "Credit Paper", "Accepted", true);
        paper("old.pdf", "Credit Paper", "Superseded", true);
        paper("terms.pdf", "Term Sheet", "Accepted", true);
        paper("inactive.pdf", "Credit Paper", "Accepted", false);
        service.sendItemConfirmedMail(item);
        MailModel mail = sent();
        assertEquals(ANALYSTS, mail.getTo());
        assertItemMail(mail, "Confirmed", "Item confirmed", "CONFIRMED");
        assertTrue(mail.getBody().contains("Agenda Booking Confirmed"));
        assertEquals(1, mail.getAttachments().size());
        assertEquals("acme.pdf", mail.getAttachments().get(0).getFilename());
        assertArrayEquals("%PDF".getBytes(), mail.getAttachments().get(0).getData());
    }

    private void paper(String name, String type, String status, boolean active) {
        ApprovalForumAgidAgendaItemDocumentEntity doc = new ApprovalForumAgidAgendaItemDocumentEntity();
        doc.setDocType(type);
        doc.setFileName(name);
        doc.setFileData("%PDF".getBytes());
        doc.setValidationStatus(status);
        doc.setActive(active);
        java.util.List<ApprovalForumAgidAgendaItemDocumentEntity> all = new java.util.ArrayList<>(repository.getDocumentsForItem(123));
        all.add(doc);
        when(repository.getDocumentsForItem(123)).thenReturn(all);
    }

    @Test
    void removedMailNamesTheDeadlineMiss() {
        service.sendItemRemovedMail(item);
        MailModel mail = sent();
        assertEquals(ANALYSTS, mail.getTo());
        assertItemMail(mail, "Removed", "Item removed", "REMOVED");
        assertTrue(mail.getBody().contains("agenda for 2026-11-03 - the submission deadline was missed"));
    }

    @Test
    @DisplayName("The reminder says how many days remain and the deadline, or 'the committee cut-off' when none is set")
    void reminderMail() {
        service.sendItemReminderMail(item, Timestamp.valueOf("2026-10-29 17:00:00"), 1);
        MailModel mail = sent();
        assertEquals(ANALYSTS, mail.getTo());
        assertItemMail(mail, "Reminder", "Reminder to upload credit paper", "REMINDER");
        assertTrue(mail.getBody().contains("Action Required"));
        assertTrue(mail.getBody().contains("17:00 - 1 day away"), mail.getBody());
        assertTrue(mail.getBody().contains("2026 17:00"));

        reset(client, repository);
        when(repository.getDocumentsForItem(anyInt())).thenReturn(Collections.emptyList());
        service.sendItemReminderMail(item, null, 2);
        assertTrue(sent().getBody().contains("The deadline is the committee cut-off - 2 days away"));
    }

    // ---- Agenda mails to the meeting ----

    @Test
    void preliminaryAgendaGoesToTheChairWithTheMeetingLink() {
        service.sendPreliminaryAgendaMail(meeting, "chair@rmb.co.za");
        MailModel mail = sent();
        assertEquals("chair@rmb.co.za", mail.getTo());
        assertEquals("Agenda Builder - Preliminary agenda: CCC", mail.getSubject());
        assertTrue(mail.getBody().contains(">FOR REVIEW<"));
        assertTrue(mail.getBody().contains("The preliminary CCC agenda for 2026-11-03 has been compiled"));
        assertTrue(mail.getBody().contains("href=\"" + UI + "/agenda/meeting/10\""));
        assertTrue(mail.getBody().contains("Teams"));
        assertTrue(mail.getBody().contains(">2<"), "quorum row");
        assertTrue(mail.getBody().contains("C Chair"));
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals("Preliminary", log.getNotificationType());
        assertNull(log.getAgiId());
        assertEquals(Integer.valueOf(10), log.getAgmId());
    }

    @Test
    void finalAgendaCarriesNoPackOfPapers() {
        meeting.setQuorumSize(0);
        meeting.setMandateLevel(null);
        service.sendFinalAgendaMail(meeting, "a@rmb.co.za;b@rmb.co.za");
        MailModel mail = sent();
        assertEquals("a@rmb.co.za;b@rmb.co.za", mail.getTo());
        assertEquals("Agenda Builder - Final agenda: CCC", mail.getSubject());
        assertTrue(mail.getBody().contains(">FINAL<"));
        assertTrue(mail.getBody().contains("has been locked and issued"));
        assertFalse(mail.getBody().contains("Quorum"), "empty rows are skipped");
        assertNull(mail.getAttachments());
        assertEquals("Final", logged().getNotificationType());
        verify(repository, never()).getDocumentsForItem(anyInt());
    }

    // ---- Workflow ----

    @Test
    void submittedMailGoesToTheReviewersAndAnalystsWithThePaper() {
        paper("acme.pdf", "Credit Paper", "Pending", true);
        service.sendItemSubmittedMail(item, "reviewer@rmb.co.za;secretary@rmb.co.za");
        MailModel mail = sent();
        assertEquals("reviewer@rmb.co.za;secretary@rmb.co.za;booker@rmb.co.za;analyst@rmb.co.za", mail.getTo(), "already-mailed addresses are not repeated");
        assertItemMail(mail, "Submitted", "Item submitted for review", "FOR REVIEW");
        assertTrue(mail.getBody().contains("Please review the submission and return or approve it"));
        assertEquals("acme.pdf", mail.getAttachments().get(0).getFilename());
    }

    @Test
    @DisplayName("The submitted mail resolves the reviewer and secretary ids through the directory, like every other mail")
    void submittedMailResolvesTheReviewerIds() {
        service.sendItemSubmittedMail(item, "reviewer;sec");
        MailModel mail = sent();
        assertEquals("reviewer@rmb.co.za;secretary@rmb.co.za;booker@rmb.co.za;analyst@rmb.co.za", mail.getTo());
    }

    @Test
    void reviewedMailResolvesTheApproverIds() {
        service.sendItemReviewedMail(item, "chair;;sec");
        MailModel mail = sent();
        assertEquals("chair@rmb.co.za;secretary@rmb.co.za;" + ANALYSTS, mail.getTo());
        assertItemMail(mail, "Reviewed", "Item reviewed - approval required", "AWAITING APPROVAL");
        assertTrue(mail.getBody().contains("has completed independent review"));
        assertNull(mail.getAttachments());
    }

    @Test
    void returnedMailWithoutAReasonSaysSo() {
        service.sendItemReturnedMail(item, " ");
        MailModel mail = sent();
        assertItemMail(mail, "Returned", "Item returned", "RETURNED");
        assertTrue(mail.getBody().contains("No return reason was recorded"));
        assertTrue(mail.getBody().contains("What to do next"));
    }

    @Test
    void changedMailEscapesTheChangeAndDetail() {
        service.sendItemChangedMail(item, "reviewer;sec", "meeting date", "Moved from Tue 3 Nov <09:00> to Tue 10 Nov\nReason: clash");
        MailModel mail = sent();
        assertEquals("reviewer@rmb.co.za;secretary@rmb.co.za;booker@rmb.co.za;analyst@rmb.co.za", mail.getTo());
        assertEquals("Agenda Builder - Item changed (meeting date): Acme & Sons", mail.getSubject());
        assertTrue(mail.getBody().contains(">CHANGED<"));
        assertTrue(mail.getBody().contains("has changed while it is in review: meeting date."));
        assertTrue(mail.getBody().contains("What changed"));
        assertTrue(mail.getBody().contains("&lt;09:00&gt; to Tue 10 Nov<br/>Reason: clash"));
        assertEquals("Changed", logged().getNotificationType());
    }

    @Test
    @DisplayName("The quorum exception request tells the deciders the shortfall and the analyst's reason")
    void quorumExceptionRequested() {
        item.setQuorumExceptionReason("Member on leave");
        AgendaQuorumResultDTO quorum = new AgendaQuorumResultDTO();
        quorum.setEligibleCount(1);
        quorum.setRequired(2);
        service.sendQuorumExceptionRequestedMail(item, "sec;chair", quorum);
        MailModel mail = sent();
        assertEquals("secretary@rmb.co.za;chair@rmb.co.za;" + ANALYSTS, mail.getTo());
        assertItemMail(mail, "Quorum exception requested", "Quorum exception requested", "DECISION NEEDED");
        assertTrue(mail.getBody().contains("does not have full quorum. It has 1 of the 2 eligible people the committee needs."));
        assertTrue(mail.getBody().contains("Reason given by the analyst"));
        assertTrue(mail.getBody().contains("Member on leave"));
        assertTrue(mail.getBody().contains("approve or decline the quorum exception"));

        reset(client, repository);
        when(repository.getDocumentsForItem(anyInt())).thenReturn(Collections.emptyList());
        service.sendQuorumExceptionRequestedMail(item, "sec", null);
        assertTrue(sent().getBody().contains("does not have full quorum.</div>"), "no count without a quorum result");
    }

    @Test
    void quorumExceptionDecidedApprovedWithNote() {
        service.sendQuorumExceptionDecidedMail(item, true, "  Alternate confirmed  ");
        MailModel mail = sent();
        assertEquals(ANALYSTS, mail.getTo());
        assertItemMail(mail, "Quorum exception approved", "Quorum exception approved", "APPROVED");
        assertTrue(mail.getBody().contains("Quorum Exception Approved"));
        assertTrue(mail.getBody().contains("You can submit the item for review without full quorum"));
        assertTrue(mail.getBody().contains(">Note</div>"));
        assertTrue(mail.getBody().contains("Alternate confirmed"));
    }

    @Test
    void quorumExceptionDecidedDeclinedWithAndWithoutReason() {
        service.sendQuorumExceptionDecidedMail(item, false, "Find a second CCE");
        MailModel mail = sent();
        assertItemMail(mail, "Quorum exception declined", "Quorum exception declined", "DECLINED");
        assertTrue(mail.getBody().contains("has been declined. Add the people needed for quorum"));
        assertTrue(mail.getBody().contains(">Reason</div>"));
        assertTrue(mail.getBody().contains("Find a second CCE"));

        reset(client, repository);
        when(repository.getDocumentsForItem(anyInt())).thenReturn(Collections.emptyList());
        service.sendQuorumExceptionDecidedMail(item, false, " ");
        assertFalse(sent().getBody().contains(">Reason</div>"), "no section without a note");
    }

    @Test
    @DisplayName("The approval mail gives the agenda number and slot, and attaches the paper")
    void approvedMail() {
        item.setAgendaNumber("3.2");
        paper("acme.pdf", "Credit Paper", "Accepted", true);
        service.sendItemApprovedMail(item);
        MailModel mail = sent();
        assertEquals(ANALYSTS, mail.getTo());
        assertItemMail(mail, "Approved", "Item approved", "APPROVED");
        assertTrue(mail.getBody().contains("has been approved for the CCC agenda for 2026-11-03 as item 3.2 at 09:00 - 09:30."));
        assertTrue(mail.getBody().contains("receive their own calendar invitation"));
        assertEquals("acme.pdf", mail.getAttachments().get(0).getFilename());

        reset(client, repository);
        when(repository.getDocumentsForItem(anyInt())).thenReturn(Collections.emptyList());
        item.setAgendaNumber(null);
        item.setEndTime(null);
        service.sendItemApprovedMail(item);
        MailModel bare = sent();
        assertTrue(bare.getBody().contains("agenda for 2026-11-03 at 09:00. The item"));
        assertNull(bare.getAttachments(), "no paper, no attachment list");
    }

    @Test
    void rejectedMailCarriesTheReasonWhenGiven() {
        service.sendItemRejectedMail(item, "  Outside mandate  ");
        MailModel mail = sent();
        assertEquals(ANALYSTS, mail.getTo());
        assertItemMail(mail, "Rejected", "Item rejected", "REJECTED");
        assertTrue(mail.getBody().contains("has been rejected for the CCC agenda for 2026-11-03. Reason: Outside mandate"));

        reset(client, repository);
        when(repository.getDocumentsForItem(anyInt())).thenReturn(Collections.emptyList());
        service.sendItemRejectedMail(item, null);
        assertTrue(sent().getBody().contains("agenda for 2026-11-03.</div>"));
    }

    // ---- Meeting lifecycle ----

    @Test
    void meetingCancelledMail() {
        service.sendMeetingCancelledMail(meeting, "a@rmb.co.za;b@rmb.co.za", " Public holiday ");
        MailModel mail = sent();
        assertEquals("a@rmb.co.za;b@rmb.co.za", mail.getTo());
        assertEquals("Agenda Builder - Meeting cancelled: CCC", mail.getSubject());
        assertTrue(mail.getBody().contains(">CANCELLED<"));
        assertTrue(mail.getBody().contains("The CCC meeting of 2026-11-03 has been cancelled. Reason: Public holiday"));
        assertTrue(mail.getBody().contains(UI + "/agenda/meeting/10"));
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals("Cancelled", log.getNotificationType());
        assertEquals(Integer.valueOf(10), log.getAgmId());

        reset(client, repository);
        service.sendMeetingCancelledMail(meeting, "a@rmb.co.za", null);
        assertTrue(sent().getBody().contains("has been cancelled.</div>"));
    }

    @Test
    void meetingClosedMail() {
        service.sendMeetingClosedMail(meeting, "a@rmb.co.za");
        MailModel mail = sent();
        assertEquals("Agenda Builder - Meeting closed: CCC", mail.getSubject());
        assertTrue(mail.getBody().contains(">CLOSED<"));
        assertTrue(mail.getBody().contains("Decisions, conditions and attendance are now final"));
        assertEquals("Closed", logged().getNotificationType());
    }

    // ---- Delivery rules shared by every mail ----

    @Test
    @DisplayName("Outside PROD every mail is redirected to the test inbox and its subject is tagged with the environment")
    void nonProdRedirectsToTheTestInbox() {
        settings.put("af.mail_environment", "UAT");
        settings.put("af.mail.test.emails", "tester@rmb.co.za");
        service.sendItemConfirmedMail(item);
        MailModel mail = sent();
        assertEquals("tester@rmb.co.za", mail.getTo());
        assertEquals("[UAT] Agenda Builder - Item confirmed: Acme & Sons", mail.getSubject());
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals("tester@rmb.co.za", log.getRecipients());
        assertEquals("[UAT] Agenda Builder - Item confirmed: Acme & Sons", log.getSubject());
    }

    @Test
    void noRecipientIsLoggedAndNotSent() {
        item.setCreatedBy(null);
        item.setPrimaryAnalystId(" ");
        item.setReviewAnalystId(null);
        service.sendItemConfirmedMail(item);
        verifyNoInteractions(client);
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals("NoRecipient", log.getStatus());
        assertEquals("No recipient resolved", log.getErrorMessage());
        assertEquals("Confirmed", log.getNotificationType());
    }

    @Test
    void aFailedSendIsLoggedWithItsError() {
        when(client.Send(eq(TOKEN), any())).thenThrow(new RuntimeException("mail service 503"));
        service.sendItemApprovedMail(item);
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals("Failed", log.getStatus());
        assertEquals("mail service 503", log.getErrorMessage());
        assertEquals(ANALYSTS, log.getRecipients());
    }

    @Test
    void loggingFailuresNeverBreakTheSend() {
        when(repository.saveNotification(any())).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> service.sendItemRejectedMail(item, "No"));
        verify(client).Send(eq(TOKEN), any());
    }

    @Test
    void longRecipientListsAndSubjectsAreTrimmedInTheLog() {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 120; i++) many.append("person").append(i).append("@rmb.co.za;");
        service.sendFinalAgendaMail(meeting, many.toString());
        assertTrue(many.length() > 2000);
        assertEquals(2000, logged().getRecipients().length());
        assertEquals(many.toString(), sent().getTo(), "the mail itself is not trimmed");
    }

    @Test
    void withoutAUiBaseThereIsNoLinkButton() {
        settings.put("af.agenda.ui.base", " ");
        service.sendItemConfirmedMail(item);
        assertFalse(sent().getBody().contains("View in Agenda Builder"));
        assertFalse(sent().getBody().contains("href="));
    }

    @Test
    void itemsWithoutACommitteeOrDateStillRead() {
        item.setCommitteeName(null);
        item.setMeetingDate(null);
        item.setMandateLevel(null);
        item.setStartTime(null);
        item.setEstimatedMinutes(null);
        item.setNumberOfPages(null);
        service.sendItemRemovedMail(item);
        String body = sent().getBody();
        assertTrue(body.contains("removed from the committee agenda for  - the submission deadline"));
        assertFalse(body.contains("Meeting date"), "empty rows are skipped");
        assertFalse(body.contains("Duration"));
        assertFalse(body.contains("Number of pages"));
        assertTrue(body.contains(">committee<"));
    }

    // ---- Address resolution ----

    @Test
    @DisplayName("User ids resolve to AD mail; the id itself stands in when AD has no address or fails; duplicates are dropped")
    void resolveEmails() throws Exception {
        ActiveDirectoryUserModel noMail = new ActiveDirectoryUserModel();
        when(authService.getUserDetailFromAd("nomail")).thenReturn(noMail);
        when(authService.getUserDetailFromAd("broken")).thenThrow(new RuntimeException("LDAP"));
        when(authService.getUserDetailFromAd("missing")).thenReturn(null);
        assertEquals("secretary@rmb.co.za;nomail;broken;missing;chair@rmb.co.za",
                service.resolveEmails(" sec ;nomail;broken;missing;;chair; sec"));
        assertEquals("", service.resolveEmails(null));
        assertEquals("", service.resolveEmails(" ; "));
    }

    @Test
    void analystAudienceIsDeduplicatedCaseInsensitively() throws Exception {
        item.setCreatedBy("Analyst");
        item.setPrimaryAnalystId(" analyst ");
        item.setReviewAnalystId("ANALYST");
        service.sendItemConfirmedMail(item);
        assertEquals("analyst@rmb.co.za", sent().getTo());
        verify(authService, times(1)).getUserDetailFromAd("analyst");
    }

    @Test
    void disabledMailLogsEveryType() {
        settings.put("af.mail.enabled", "false");
        service.sendMeetingClosedMail(meeting, "a@rmb.co.za");
        verifyNoInteractions(client);
        ApprovalForumAgnAgendaNotificationEntity log = logged();
        assertEquals("Disabled", log.getStatus());
        assertEquals("Closed", log.getNotificationType());
        assertEquals("Mail disabled (af.mail.enabled=false)", log.getErrorMessage());
        assertEquals(Integer.valueOf(10), log.getAgmId());
    }
}
