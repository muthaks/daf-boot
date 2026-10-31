# Agenda API notification and invitation reference

This is a generated backend contract reference for the Agenda Builder API in daf-boot. Literal declarations below are part of the specification; requirements.md and design.md explain them. It is not evidence of a live service, environment or database.

Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. No API, directory or database is accessed.


E-mails go through the DAF mail service client and are logged in `AGN_AgendaNotification` with status Sent, Failed, Disabled or NoRecipient. Calendar invitations go to Microsoft Graph as Teams/Outlook events. Subjects carry the `Agenda Builder - ` prefix; outside PROD the recipients are replaced by `af.mail.test.emails` and the subject is prefixed with `[<af.mail_environment>]`.

## E-mails

Source: `api/src/main/java/za/co/rmb/approvalforum/service/impl/AgendaMailServiceImpl.java`. SHA-256: `9602310e03c8f3317830ba6aa8a932e3936f843eea946760aaa9ebeab3477651`.

| Method | Type logged | Banner | Badge | Recipients | Subject | Paper attached | Sent by |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `sendItemCapturedMail` | Captured | Agenda Item Captured | PENDING PAPER | `join(resolveEmails(secretaryIds), analystRecipients(item))` | `SUBJECT_PREFIX + "Item captured: " + item.getCounterpartyName()` | no | `bookItem` |
| `sendItemConfirmedMail` | Confirmed | Agenda Booking Confirmed | CONFIRMED | `analystRecipients(item)` | `SUBJECT_PREFIX + "Item confirmed: " + item.getCounterpartyName()` | yes | `attachDocument`, `uploadDocument` |
| `sendItemRemovedMail` | Removed | Agenda Item Removed | REMOVED | `analystRecipients(item)` | `SUBJECT_PREFIX + "Item removed: " + item.getCounterpartyName()` | no | `removeItem` |
| `sendItemReminderMail` | Reminder | Action Required | REMINDER | `analystRecipients(item)` | `SUBJECT_PREFIX + "Reminder to upload credit paper: " + item.getCounterpartyName()` | no | `processReminders` |
| `sendPreliminaryAgendaMail` | Preliminary | Preliminary Agenda Ready | FOR REVIEW | `chairEmail` | `SUBJECT_PREFIX + "Preliminary agenda: " + meeting.getCommitteeName()` | no | not called |
| `sendFinalAgendaMail` | Final | Final Agenda Issued | FINAL | `recipientEmails` | `SUBJECT_PREFIX + "Final agenda: " + meeting.getCommitteeName()` | no | not called |
| `sendItemSubmittedMail` | Submitted | Agenda Item Submitted | FOR REVIEW | `to` | `SUBJECT_PREFIX + "Item submitted for review: " + item.getCounterpartyName()` | yes | `submitItem` |
| `sendItemReviewedMail` | Reviewed | Submission Review Complete | AWAITING APPROVAL | `join(resolveEmails(approverIds), analystRecipients(item))` | `SUBJECT_PREFIX + "Item reviewed - approval required: " + item.getCounterpartyName()` | no | `completeReview` |
| `sendItemReturnedMail` | Returned | Agenda Item Returned | RETURNED | `analystRecipients(item)` | `SUBJECT_PREFIX + "Item returned: " + item.getCounterpartyName()` | no | `returnItem` |
| `sendItemChangedMail` | Changed | Agenda Item Changed | CHANGED | `join(resolveEmails(recipientIds), analystRecipients(item))` | `SUBJECT_PREFIX + "Item changed (" + change + "): " + item.getCounterpartyName()` | no | `moveItem`, `updateRoster`, `uploadDocument` |
| `sendQuorumExceptionRequestedMail` | Quorum exception requested | Quorum Exception Requested | DECISION NEEDED | `join(resolveEmails(deciderIds), analystRecipients(item))` | `SUBJECT_PREFIX + "Quorum exception requested: " + item.getCounterpartyName()` | no | `mailExceptionRequested` |
| `sendQuorumExceptionDecidedMail` | type | Quorum Exception Approved / Quorum Exception Declined | APPROVED / DECLINED | `analystRecipients(item)` | `SUBJECT_PREFIX + type + ": " + item.getCounterpartyName()` | no | `decideQuorumException` |
| `sendItemApprovedMail` | Approved | Agenda Item Approved | APPROVED | `analystRecipients(item)` | `SUBJECT_PREFIX + "Item approved: " + item.getCounterpartyName()` | yes | `approveItem` |
| `sendItemRejectedMail` | Rejected | Agenda Item Rejected | REJECTED | `analystRecipients(item)` | `SUBJECT_PREFIX + "Item rejected: " + item.getCounterpartyName()` | no | `rejectItem` |
| `sendMeetingCancelledMail` | Cancelled | Meeting Cancelled | CANCELLED | `recipientEmails` | `SUBJECT_PREFIX + "Meeting cancelled: " + meeting.getCommitteeName()` | no | `cancelMeeting` |
| `sendMeetingClosedMail` | Closed | Meeting Record Closed | CLOSED | `recipientEmails` | `SUBJECT_PREFIX + "Meeting closed: " + meeting.getCommitteeName()` | no | `closeMeeting` |

### Mail service implementation

```java
/**
 * Auto-generated agenda notifications (Agenda_Builder_Spec.md section 7),
 * sent through the existing DAF mail service client.
 */
public class AgendaMailServiceImpl implements AgendaMailService {

    private static final Logger logger = LoggerFactory.getLogger(AgendaMailServiceImpl.class);

    private static final String FROM = "approvalForum@rmb.co.za";
    private static final String BODY_STYLE = "<body style=\"background-color:white;font-family:calibri;\">";

    private final MailServiceClient mailServiceClient;
    private final ApprovalForumAuthService authService;
    private final ApprovalForumAgendaRepository agendaRepository;
    private final Map<String, String> afSettings;

    @Inject
    public AgendaMailServiceImpl(MailServiceClient mailServiceClient,
                                 ApprovalForumAuthService authService,
                                 ApprovalForumAgendaRepository agendaRepository,
                                 @Named("af_settings") Map<String, String> afSettings) {
        this.mailServiceClient = mailServiceClient;
        this.authService = authService;
        this.agendaRepository = agendaRepository;
        this.afSettings = afSettings;
    }

    @Override
    public void sendItemCapturedMail(AgendaItemDTO item, String secretaryIds) {
        String content = banner("#fff6dd", "#f0dfae", "Agenda Item Captured", "PENDING PAPER", "#f47920",
                item.getCounterpartyName() + " has been captured on the " + committeeOf(item)
                        + " agenda. The credit paper still needs to be uploaded by the committee cut-off - "
                        + "if it is not uploaded in time, the item is removed from the agenda automatically.")
                + itemSections(item);
        send(item.getAgiId(), null, "Captured", join(resolveEmails(secretaryIds), analystRecipients(item)),
                SUBJECT_PREFIX + "Item captured: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendItemConfirmedMail(AgendaItemDTO item) {
        String content = banner("#e8f6ed", "#bfe6cc", "Agenda Booking Confirmed", "CONFIRMED", "#1CA345",
                "Your agenda item has been confirmed and all required participants have been notified.")
                + itemSections(item);
        send(item.getAgiId(), null, "Confirmed", analystRecipients(item),
                SUBJECT_PREFIX + "Item confirmed: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)),
                creditPaperAttachment(item.getAgiId()));
    }

    @Override
    public void sendItemRemovedMail(AgendaItemDTO item) {
        String content = banner("#fcebec", "#f0b9bb", "Agenda Item Removed", "REMOVED", "#DD3F44",
                item.getCounterpartyName() + " has been removed from the " + committeeOf(item)
                        + " agenda for " + dateOf(item) + " - the submission deadline was missed.")
                + itemSections(item);
        send(item.getAgiId(), null, "Removed", analystRecipients(item),
                SUBJECT_PREFIX + "Item removed: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendItemReminderMail(AgendaItemDTO item, java.sql.Timestamp cutOff, int daysToCutOff) {
        String daysText = daysToCutOff == 1 ? "1 day" : daysToCutOff + " days";
        String content = banner("#fff6dd", "#f0dfae", "Action Required", "REMINDER", "#f47920",
                "The credit paper for " + item.getCounterpartyName() + " on the " + committeeOf(item)
                        + " agenda has not yet been uploaded. The deadline is " + cutOffText(cutOff) + " - "
                        + daysText + " away. If the paper is not uploaded by then, the item is removed from the agenda.")
                + itemSections(item);
        send(item.getAgiId(), null, "Reminder", analystRecipients(item),
                SUBJECT_PREFIX + "Reminder to upload credit paper: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendPreliminaryAgendaMail(AgendaMeetingDTO meeting, String chairEmail) {
        String content = banner("#e6f4f4", "#b3dede", "Preliminary Agenda Ready", "FOR REVIEW", "#006c68",
                "The preliminary " + meeting.getCommitteeName() + " agenda for " + dateOf(meeting)
                        + " has been compiled. Please review and make any amendments before the cut-off.")
                + meetingSections(meeting);
        send(null, meeting.getAgmId(), "Preliminary", chairEmail,
                SUBJECT_PREFIX + "Preliminary agenda: " + meeting.getCommitteeName(), shell(content, dafLink(null, meeting.getAgmId())), null);
    }

    @Override
    public void sendFinalAgendaMail(AgendaMeetingDTO meeting, String recipientEmails) {
        String content = banner("#e8f6ed", "#bfe6cc", "Final Agenda Issued", "FINAL", "#1CA345",
                "The " + meeting.getCommitteeName() + " agenda for " + dateOf(meeting)
                        + " has been locked and issued. No further changes can be made to the agenda.")
                + meetingSections(meeting);
        // No pack of every item's paper: each item's participants hold their own paper (BR-013, NFR-001).
        send(null, meeting.getAgmId(), "Final", recipientEmails,
                SUBJECT_PREFIX + "Final agenda: " + meeting.getCommitteeName(), shell(content, dafLink(null, meeting.getAgmId())), null);
    }

    // ---- BRS alignment: review/approval workflow and meeting lifecycle ----

    @Override
    public void sendItemSubmittedMail(AgendaItemDTO item, String reviewerEmails) {
        String content = banner("#e6f4f4", "#b3dede", "Agenda Item Submitted", "FOR REVIEW", "#006c68",
                item.getCounterpartyName() + " has been submitted for review on the " + committeeOf(item)
                        + " agenda for " + dateOf(item) + ". Please review the submission and return or approve it.")
                + itemSections(item);
        String to = join(resolveEmails(reviewerEmails), analystRecipients(item));
        send(item.getAgiId(), null, "Submitted", to,
                SUBJECT_PREFIX + "Item submitted for review: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)),
                creditPaperAttachment(item.getAgiId()));
    }

    @Override
    public void sendItemReviewedMail(AgendaItemDTO item, String approverIds) {
        String content = banner("#e6f4f4", "#b3dede", "Submission Review Complete", "AWAITING APPROVAL", "#006c68",
                item.getCounterpartyName() + " has completed independent review. An authorised approver or chair must now approve or reject the agenda item.")
                + itemSections(item);
        send(item.getAgiId(), null, "Reviewed", join(resolveEmails(approverIds), analystRecipients(item)),
                SUBJECT_PREFIX + "Item reviewed - approval required: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendItemReturnedMail(AgendaItemDTO item, String reason) {
        String content = banner("#fff6dd", "#f0dfae", "Agenda Item Returned", "RETURNED", "#f47920",
                item.getCounterpartyName() + " has been returned by the reviewer and needs attention before it can be "
                        + "resubmitted.")
                + section("Reason for return", htmlText(reason == null || reason.trim().isEmpty()
                        ? "No return reason was recorded. Ask the reviewer to clarify the required corrections." : reason.trim()))
                + section("What to do next", "Make the requested corrections. If the credit paper changed, upload a new PDF version. "
                        + "Then submit the item for review again using the link below.")
                + itemSections(item);
        send(item.getAgiId(), null, "Returned", analystRecipients(item),
                SUBJECT_PREFIX + "Item returned: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendItemChangedMail(AgendaItemDTO item, String recipientIds, String change, String detail) {
        String content = banner("#e6f4f4", "#b3dede", "Agenda Item Changed", "CHANGED", "#006c68",
                item.getCounterpartyName() + " on the " + committeeOf(item) + " agenda for " + dateOf(item)
                        + " has changed while it is in review: " + htmlText(change) + ".")
                + section("What changed", htmlText(detail))
                + itemSections(item);
        send(item.getAgiId(), null, "Changed", join(resolveEmails(recipientIds), analystRecipients(item)),
                SUBJECT_PREFIX + "Item changed (" + change + "): " + item.getCounterpartyName(),
                shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendQuorumExceptionRequestedMail(AgendaItemDTO item, String deciderIds, AgendaQuorumResultDTO quorum) {
        String people = quorum == null ? "" : " It has " + quorum.getEligibleCount() + " of the " + quorum.getRequired()
                + " eligible people the committee needs.";
        String content = banner("#fff6dd", "#f0dfae", "Quorum Exception Requested", "DECISION NEEDED", "#f47920",
                item.getCounterpartyName() + " on the " + committeeOf(item) + " agenda for " + dateOf(item)
                        + " does not have full quorum." + people)
                + section("Reason given by the analyst", htmlText(item.getQuorumExceptionReason()))
                + section("What to do next", "Open the item with the link below and approve or decline the quorum exception. "
                        + "If you approve it, the analyst can submit the item for review when the paper is ready.")
                + itemSections(item);
        send(item.getAgiId(), null, "Quorum exception requested", join(resolveEmails(deciderIds), analystRecipients(item)),
                SUBJECT_PREFIX + "Quorum exception requested: " + item.getCounterpartyName(),
                shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendQuorumExceptionDecidedMail(AgendaItemDTO item, boolean approved, String note) {
        String content = (approved
                ? banner("#e8f6ed", "#bfe6cc", "Quorum Exception Approved", "APPROVED", "#1CA345",
                        "The quorum exception for " + item.getCounterpartyName() + " on the " + committeeOf(item)
                                + " agenda for " + dateOf(item) + " has been approved. You can submit the item for review "
                                + "without full quorum once the paper and details are complete.")
                : banner("#fcebec", "#f0b9bb", "Quorum Exception Declined", "DECLINED", "#DD3F44",
                        "The quorum exception for " + item.getCounterpartyName() + " on the " + committeeOf(item)
                                + " agenda for " + dateOf(item) + " has been declined. Add the people needed for quorum "
                                + "before you submit the item for review."))
                + (note == null || note.trim().isEmpty() ? "" : section(approved ? "Note" : "Reason", htmlText(note.trim())))
                + itemSections(item);
        String type = approved ? "Quorum exception approved" : "Quorum exception declined";
        send(item.getAgiId(), null, type, analystRecipients(item),
                SUBJECT_PREFIX + type + ": " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendItemApprovedMail(AgendaItemDTO item) {
        String content = banner("#e8f6ed", "#bfe6cc", "Agenda Item Approved", "APPROVED", "#1CA345",
                item.getCounterpartyName() + " has been approved for the " + committeeOf(item)
                        + " agenda for " + dateOf(item)
                        + (item.getAgendaNumber() == null ? "" : " as item " + item.getAgendaNumber())
                        + (item.getStartTime() == null ? "" : " at " + item.getStartTime() + (item.getEndTime() == null ? "" : " - " + item.getEndTime()))
                        + ". The item's participants receive their own calendar invitation when the committee secretary sends the invitations.")
                + itemSections(item);
        send(item.getAgiId(), null, "Approved", analystRecipients(item),
                SUBJECT_PREFIX + "Item approved: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)),
                creditPaperAttachment(item.getAgiId()));
    }

    @Override
    public void sendItemRejectedMail(AgendaItemDTO item, String reason) {
        String content = banner("#fcebec", "#f0b9bb", "Agenda Item Rejected", "REJECTED", "#DD3F44",
                item.getCounterpartyName() + " has been rejected for the " + committeeOf(item) + " agenda for "
                        + dateOf(item) + "." + (reason == null || reason.trim().isEmpty() ? "" : " Reason: " + reason.trim()))
                + itemSections(item);
        send(item.getAgiId(), null, "Rejected", analystRecipients(item),
                SUBJECT_PREFIX + "Item rejected: " + item.getCounterpartyName(), shell(content, dafLink(item.getAgiId(), null)), null);
    }

    @Override
    public void sendMeetingCancelledMail(AgendaMeetingDTO meeting, String recipientEmails, String reason) {
        String content = banner("#fcebec", "#f0b9bb", "Meeting Cancelled", "CANCELLED", "#DD3F44",
                "The " + meeting.getCommitteeName() + " meeting of " + dateOf(meeting) + " has been cancelled."
                        + (reason == null || reason.trim().isEmpty() ? "" : " Reason: " + reason.trim()))
                + meetingSections(meeting);
        send(null, meeting.getAgmId(), "Cancelled", recipientEmails,
                SUBJECT_PREFIX + "Meeting cancelled: " + meeting.getCommitteeName(), shell(content, dafLink(null, meeting.getAgmId())), null);
    }

    @Override
    public void sendMeetingClosedMail(AgendaMeetingDTO meeting, String recipientEmails) {
        String content = banner("#e8f6ed", "#bfe6cc", "Meeting Record Closed", "CLOSED", "#1CA345",
                "The " + meeting.getCommitteeName() + " meeting record for " + dateOf(meeting)
                        + " has been closed. Decisions, conditions and attendance are now final.")
                + meetingSections(meeting);
        send(null, meeting.getAgmId(), "Closed", recipientEmails,
                SUBJECT_PREFIX + "Meeting closed: " + meeting.getCommitteeName(), shell(content, dafLink(null, meeting.getAgmId())), null);
    }

    private String join(String a, String b) {
        Set<String> all = new LinkedHashSet<>();
        for (String part : new String[]{a, b}) {
            if (part == null) {
                continue;
            }
            for (String email : part.split(";")) {
                if (!email.trim().isEmpty()) {
                    all.add(email.trim());
                }
            }
        }
        return String.join(";", all);
    }

    // ---- Email-client-safe content (nested tables + inline styles only; no flexbox/grid because
    // Outlook renders with the Word engine). The mail service's template frames it: header,
    // subject, footer and disclaimer. ----

    private static final String FONT = "font-family:Calibri,Arial,sans-serif;";

    /** Agenda Builder is a module of DAF; its notifications carry the module's name. */
    private static final String SUBJECT_PREFIX = "Agenda Builder - ";

    /** Greeting + content + link to the item, for the mail service's template to frame. */
    private String shell(String content, String link) {
        String button = link == null ? "" :
                "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:16px;\"><tr>"
                        + "<td style=\"background-color:#fa9d1e;border-radius:6px;\">"
                        + "<a href=\"" + link + "\" style=\"display:inline-block;padding:10px 22px;" + FONT
                        + "font-size:13px;color:#0a0a0d;text-decoration:none;font-weight:bold;\">View in Agenda Builder</a>"
                        + "</td></tr></table>";
        return "<div style=\"" + FONT + "font-size:13.5px;color:#0a0a0d;padding-bottom:12px;\">Good day,</div>"
                + content + button;
    }

    /** Coloured status banner with a right-aligned badge (e.g. CONFIRMED). */
    private String banner(String bg, String border, String title, String badge, String badgeColor, String text) {
        return "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:" + bg
                + ";border:1px solid " + border + ";border-radius:6px;\"><tr>"
                + "<td style=\"padding:14px 18px;\">"
                + "<div style=\"" + FONT + "font-size:16px;font-weight:bold;color:#0a0a0d;\">" + title + "</div>"
                + "<div style=\"" + FONT + "font-size:13px;color:#4a4f55;padding-top:4px;\">" + htmlText(text) + "</div></td>"
                + "<td align=\"right\" valign=\"top\" style=\"padding:14px 18px;\">"
                + "<span style=\"" + FONT + "font-size:10px;font-weight:bold;letter-spacing:0.5px;color:" + badgeColor + ";\">"
                + badge + "</span></td>"
                + "</tr></table>";
    }

    /** Treat user-entered content as text, preserving readable paragraph breaks in email. */
    private String htmlText(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;").replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br/>");
    }

    private String section(String title, String inner) {
        return "<div style=\"" + FONT + "font-size:12px;font-weight:bold;color:#4a4f55;letter-spacing:0.6px;"
                + "text-transform:uppercase;padding:16px 0 6px 0;\">" + title + "</div>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border:1px solid #ececec;border-radius:6px;\">"
                + "<tr><td style=\"padding:10px 14px;\">" + inner + "</td></tr></table>";
    }

    /** Label/value rows; rows with empty values are skipped. */
    private String kvRows(String[][] rows) {
        StringBuilder sb = new StringBuilder("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">");
        for (String[] r : rows) {
            if (r[1] == null || r[1].trim().isEmpty()) {
                continue;
            }
            sb.append("<tr><td style=\"padding:3px 0;").append(FONT)
                    .append("font-size:13px;color:#4a4f55;width:45%;\">").append(r[0])
                    .append("</td><td style=\"padding:3px 0;").append(FONT)
                    .append("font-size:13px;color:#0a0a0d;font-weight:bold;\">").append(htmlText(r[1]))
                    .append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }

    private String itemSections(AgendaItemDTO item) {
        String time = item.getStartTime() == null ? null
                : item.getStartTime() + (item.getEndTime() == null ? "" : " - " + item.getEndTime()) + " SAST";
        return section("Meeting Details", kvRows(new String[][]{
                {"Committee", committeeOf(item) + (item.getMandateLevel() == null ? "" : " " + item.getMandateLevel())},
                {"Meeting date", dateOf(item)},
                {"Time", time},
                {"Duration", item.getEstimatedMinutes() == null ? null : item.getEstimatedMinutes() + " minutes"},
                {"Booking reference", "AGD-" + item.getAgiId()}}))
                + section("Deal Information", kvRows(new String[][]{
                {"Counterparty", item.getCounterpartyName()},
                {"Purpose of submission", item.getPurposeOfSubmission()},
                {"Rating", item.getRatingBand()},
                {"Business unit / segment", item.getBusinessUnit()},
                {"Number of pages", item.getNumberOfPages() == null ? null : String.valueOf(item.getNumberOfPages())}}))
                + section("Assigned Team", kvRows(new String[][]{
                {"Primary analyst", item.getPrimaryAnalyst()},
                {"Review analyst", item.getReviewAnalyst()},
                {"Credit sponsor", item.getCreditSponsor()},
                {"BU head", item.getBuHead()},
                {"Chairperson", item.getChair()}}));
    }

    private String meetingSections(AgendaMeetingDTO meeting) {
        return section("Meeting Details", kvRows(new String[][]{
                {"Committee", meeting.getCommitteeName()
                        + (meeting.getMandateLevel() == null ? "" : " " + meeting.getMandateLevel())},
                {"Meeting date", dateOf(meeting)},
                {"Held via", meeting.getHeldVia()},
                {"Quorum", meeting.getQuorumSize() > 0 ? String.valueOf(meeting.getQuorumSize()) : null},
                {"Chairperson", meeting.getChair()}}));
    }

    private void send(Integer agiId, Integer agmId, String type, String to, String subject, String body,
                      List<AttachmentModel> attachments) {
        if (!Boolean.parseBoolean(afSettings.get("af.mail.enabled"))) {
            logger.info("Mail disabled - skipping '{}'", subject);
            logNotification(agiId, agmId, type, to, subject, "Disabled", "Mail disabled (af.mail.enabled=false)");
            return;
        }
        if (to == null || to.trim().isEmpty()) {
            logger.warn("No recipient for '{}' - skipping", subject);
            logNotification(agiId, agmId, type, to, subject, "NoRecipient", "No recipient resolved");
            return;
        }

        String fullBody = body;

        MailModel mailModel = new MailModel();
        mailModel.setFrom(FROM);
        String actualTo;
        String finalSubject = subject;
        if (!"PROD".equalsIgnoreCase(afSettings.get("af.mail_environment"))) {
            actualTo = afSettings.get("af.mail.test.emails");
            finalSubject = "[" + afSettings.get("af.mail_environment") + "] " + subject;
        } else {
            actualTo = to;
        }
        mailModel.setTo(actualTo);
        mailModel.setSubject(finalSubject);
        mailModel.setBody("<html>" + BODY_STYLE + fullBody + "</body></html>");
        if (attachments != null && !attachments.isEmpty()) {
            mailModel.setAttachments(attachments);
        }

        try {
            mailServiceClient.Send(afSettings.get("af.mailservice.token"), mailModel);
            logger.info("Agenda mail sent - type={}, to={}, attachments={}, subject='{}'",
                    type, actualTo, (attachments == null ? 0 : attachments.size()), finalSubject);
            logNotification(agiId, agmId, type, actualTo, finalSubject, "Sent", null);
        } catch (Exception ex) {
            logger.error("Failed to send '{}': {}", finalSubject, ex.getMessage(), ex);
            logNotification(agiId, agmId, type, actualTo, finalSubject, "Failed", ex.getMessage());
        }
    }

    // Deep link back into DAF (item or meeting) appended to every mail; null when unconfigured.
    private String dafLink(Integer agiId, Integer agmId) {
        String uiBase = afSettings.get("af.agenda.ui.base");
        if (uiBase == null || uiBase.trim().isEmpty()) {
            return null;
        }
        uiBase = uiBase.trim();
        if (agiId != null) {
            return uiBase + "/agenda/item/" + agiId;
        }
        if (agmId != null) {
            return uiBase + "/agenda/meeting/" + agmId;
        }
        return null;
    }

    // The uploaded credit paper for an item, as a mail attachment (empty when none uploaded).
    private List<AttachmentModel> creditPaperAttachment(Integer agiId) {
        return attachments(MeetingPapers.creditPapers(agendaRepository, agiId));
    }

    private List<AttachmentModel> attachments(List<MeetingPapers.Paper> papers) {
        List<AttachmentModel> attachments = new ArrayList<>();
        for (MeetingPapers.Paper paper : papers) {
            AttachmentModel model = new AttachmentModel();
            model.setData(paper.getData());
            model.setFilename(paper.getFileName());
            attachments.add(model);
        }
        return attachments;
    }

    private void logNotification(Integer agiId, Integer agmId, String type, String recipients,
                                 String subject, String status, String error) {
        try {
            ApprovalForumAgnAgendaNotificationEntity n = new ApprovalForumAgnAgendaNotificationEntity();
            n.setAgiId(agiId);
            n.setAgmId(agmId);
            n.setNotificationType(type);
            n.setRecipients(trim(recipients, 2000));
            n.setSubject(trim(subject, 500));
            n.setStatus(status);
            n.setErrorMessage(trim(error, 2000));
            n.setSentOn(new Timestamp(System.currentTimeMillis()));
            agendaRepository.saveNotification(n);
        } catch (Exception ex) {
            logger.warn("Failed to record notification for '{}': {}", subject, ex.getMessage());
        }
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    /**
     * The analyst audience (booker + primary + review analyst), resolved from user id
     * to AD email so PROD mail reaches real inboxes. De-duplicated, case-insensitive.
     */
    private String analystRecipients(AgendaItemDTO item) {
        Set<String> seenIds = new LinkedHashSet<>();
        addId(seenIds, item.getCreatedBy());
        addId(seenIds, item.getPrimaryAnalystId());
        addId(seenIds, item.getReviewAnalystId());

        StringBuilder recipients = new StringBuilder();
        for (String userId : seenIds) {
            String email = resolveEmail(userId);
            if (email != null && !email.trim().isEmpty()) {
                if (recipients.length() > 0) {
                    recipients.append(";");
                }
                recipients.append(email.trim());
            }
        }
        return recipients.toString();
    }

    private void addId(Set<String> ids, String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            ids.add(userId.trim().toLowerCase());
        }
    }

    /** ';'-separated user ids to their AD mail addresses (the id itself when AD has none). */
    @Override
    public String resolveEmails(String userIds) {
        Set<String> emails = new LinkedHashSet<>();
        if (userIds != null) {
            for (String userId : userIds.split(";")) {
                String email = resolveEmail(userId);
                if (email != null && !email.trim().isEmpty()) {
                    emails.add(email.trim());
                }
            }
        }
        return String.join(";", emails);
    }

    private String resolveEmail(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            ActiveDirectoryUserModel user = authService.getUserDetailFromAd(userId.trim());
            if (user != null && user.getMail() != null && !user.getMail().trim().isEmpty()) {
                return user.getMail().trim();
            }
        } catch (Exception ex) {
            logger.warn("AD email lookup failed for {} - falling back to id: {}", userId, ex.getMessage());
        }
        return userId.trim();
    }

    private String committeeOf(AgendaItemDTO item) {
        return item.getCommitteeName() == null ? "committee" : item.getCommitteeName();
    }

    private String dateOf(AgendaItemDTO item) {
        return item.getMeetingDate() == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(item.getMeetingDate());
    }

    private String dateOf(AgendaMeetingDTO meeting) {
        return meeting.getMeetingDate() == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(meeting.getMeetingDate());
    }

    private String cutOffText(java.sql.Timestamp cutOff) {
        return cutOff == null ? "the committee cut-off" : new SimpleDateFormat("EEE dd MMM yyyy HH:mm").format(cutOff);
    }

}
```

### Mail service contract

Source: `api/src/main/java/za/co/rmb/approvalforum/service/AgendaMailService.java`. SHA-256: `7d1590613132aa12f2adfca778ad1b02f0e2ddf9913d54264ab29c02b8010a70`.

```java
public interface AgendaMailService {

    /** The analysts on the item and the committee's meeting secretary (user ids, ';'-separated). */
    void sendItemCapturedMail(AgendaItemDTO item, String secretaryIds);

    void sendItemConfirmedMail(AgendaItemDTO item);

    void sendItemRemovedMail(AgendaItemDTO item);

    void sendItemReminderMail(AgendaItemDTO item, java.sql.Timestamp cutOff, int daysToCutOff);

    void sendPreliminaryAgendaMail(AgendaMeetingDTO meeting, String chairEmail);

    void sendFinalAgendaMail(AgendaMeetingDTO meeting, String recipientEmails);

    // ---- BRS alignment: workflow and meeting lifecycle notifications ----

    void sendItemSubmittedMail(AgendaItemDTO item, String reviewerEmails);

    void sendItemReviewedMail(AgendaItemDTO item, String approverIds);

    void sendItemReturnedMail(AgendaItemDTO item, String reason);

    void sendItemApprovedMail(AgendaItemDTO item);

    void sendItemRejectedMail(AgendaItemDTO item, String reason);

    /**
     * An item in review changed: its meeting date, its quorum roster or a new document. `recipientIds`
     * are user ids (the reviewer and the committee secretary); the analysts are copied.
     */
    void sendItemChangedMail(AgendaItemDTO item, String recipientIds, String change, String detail);

    /** The analyst asks for a quorum exception: to the committee's secretary and chair (user ids), analysts copied. */
    void sendQuorumExceptionRequestedMail(AgendaItemDTO item, String deciderIds, AgendaQuorumResultDTO quorum);

    /** The secretary or chair approved or declined the quorum exception: to the analysts. */
    void sendQuorumExceptionDecidedMail(AgendaItemDTO item, boolean approved, String note);

    /** ';'-separated user ids to their AD mail addresses (the id itself when AD has none). */
    String resolveEmails(String userIds);

    void sendMeetingCancelledMail(AgendaMeetingDTO meeting, String recipientEmails, String reason);

    void sendMeetingClosedMail(AgendaMeetingDTO meeting, String recipientEmails);

}
```

## Calendar invitations

The invitation content (`itemEvent`, `itemHtml`, `itemSnapshot`, `itemAttendees`, `standingIds`) is in the business rules reference. The calendar contract and its Graph implementation follow.

Source: `api/src/main/java/za/co/rmb/approvalforum/service/AgendaCalendarService.java`. SHA-256: `ae4d7acae27daefed3270519fe0fcf584c4b2e81ab481738f2bcb4c4ba450118`.

```java
/**
 * Creates and maintains the Teams/Outlook events for a committee meeting (BRS BR-013): one event per
 * approved agenda item, inviting only that item's participants for its slot with its paper, and one
 * event for the meeting's standing roles (chair, alternate chair, secretary, minute taker and
 * administrator) carrying the agenda without papers. Implemented in the web layer against Microsoft
 * Graph; the service layer only sees this contract so it can be stubbed. Sending an invitation
 * requires a successful calendar result.
 */
public interface AgendaCalendarService {

    /** Result of creating or updating a calendar event. */
    class CalendarEvent {
        private final String eventId;
        private final String joinUrl;

        public CalendarEvent(String eventId, String joinUrl) {
            this.eventId = eventId;
            this.joinUrl = joinUrl;
        }

        public String getEventId() { return eventId; }
        public String getJoinUrl() { return joinUrl; }
    }

    /** A file attached to an invitation (an item's credit paper). */
    class Attachment {
        private final String name;
        private final byte[] data;

        public Attachment(String name, byte[] data) {
            this.name = name;
            this.data = data;
        }

        public String getName() { return name; }
        public byte[] getData() { return data; }
    }

    /**
     * Everything one calendar event needs. {@code key} identifies the event stably across retries
     * ("meeting:10" or "item:55"); with {@code cancelledOn} it changes after a reinstatement so Graph
     * cannot reuse a cancelled event. {@code existingEventId} null creates, otherwise updates.
     */
    class CalendarEventRequest {
        private String key;
        private String existingEventId;
        private String subject;
        private Timestamp date;
        private String startTime;
        private String endTime;
        private String venue;
        private String heldVia;
        private String link;
        private Timestamp cancelledOn;
        private List<String> attendees;
        private String html;
        private List<Attachment> attachments;

        public String getKey() { return key; }
        public CalendarEventRequest setKey(String value) { key = value; return this; }
        public String getExistingEventId() { return existingEventId; }
        public CalendarEventRequest setExistingEventId(String value) { existingEventId = value; return this; }
        public String getSubject() { return subject; }
        public CalendarEventRequest setSubject(String value) { subject = value; return this; }
        public Timestamp getDate() { return date; }
        public CalendarEventRequest setDate(Timestamp value) { date = value; return this; }
        public String getStartTime() { return startTime; }
        public CalendarEventRequest setStartTime(String value) { startTime = value; return this; }
        public String getEndTime() { return endTime; }
        public CalendarEventRequest setEndTime(String value) { endTime = value; return this; }
        public String getVenue() { return venue; }
        public CalendarEventRequest setVenue(String value) { venue = value; return this; }
        public String getHeldVia() { return heldVia; }
        public CalendarEventRequest setHeldVia(String value) { heldVia = value; return this; }
        /** The page in Agenda Builder the invitation links to (the item, or the meeting). */
        public String getLink() { return link; }
        public CalendarEventRequest setLink(String value) { link = value; return this; }
        public Timestamp getCancelledOn() { return cancelledOn; }
        public CalendarEventRequest setCancelledOn(Timestamp value) { cancelledOn = value; return this; }
        public List<String> getAttendees() { return attendees; }
        public CalendarEventRequest setAttendees(List<String> value) { attendees = value; return this; }
        public String getHtml() { return html; }
        public CalendarEventRequest setHtml(String value) { html = value; return this; }
        public List<Attachment> getAttachments() { return attachments; }
        public CalendarEventRequest setAttachments(List<Attachment> value) { attachments = value; return this; }
    }

    boolean isConfigured();

    /**
     * Create the event when the request names none yet, otherwise update it, with the attachments
     * replacing any the event already has; attendees receive them with the invitation. Returns null
     * when the integration is not configured or the call failed (already logged).
     */
    CalendarEvent createOrUpdateEvent(CalendarEventRequest request);

    /** Cancel the event; no-op when the id is blank. Throw on failure when an event exists. */
    void cancelEvent(String eventId, String reason);

}
```

### Invitation HTML styling

Source: `api/src/main/java/za/co/rmb/approvalforum/service/InvitationStyle.java`. SHA-256: `0846b0554e1832289e69968ff3784e2e48afeabc2c32f9660ec43de9300f2556`.

```java
/**
 * The meeting invitation in the co-ds design system. Outlook keeps only inline styles, so these are
 * the resolved co-ds token values the mail service's co-ds email theme uses (named beside each),
 * and the invitation uses that email's inset details and accent.
 */
public final class InvitationStyle {

    // --co-font-primary
    public static final String FONT = "'RMB Sans', 'Segoe UI', -apple-system, BlinkMacSystemFont, Arial, sans-serif";
    // --co-amber
    public static final String ACCENT = "#fa9d1e";
    // --co-bg-app
    public static final String PAGE_BG = "#fafafa";
    // --co-bg-surface
    public static final String CARD_BG = "#ffffff";
    // --co-text-primary
    public static final String TEXT = "#0a0a0d";
    // --co-text-muted
    public static final String TEXT_MUTED = "#4a4f55";
    // --co-border-subtle
    public static final String BORDER = "#ececec";

    private InvitationStyle() {
    }

    /** Text made safe for HTML; null becomes empty. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * The invitation body: the content in a bordered card and, when there is one, a button to the
     * meeting in Agenda Builder. Outlook shows the title, time and Teams details itself, so the card
     * carries no header. Padding sits on table cells, the only place Outlook keeps it.
     */
    public static String invitation(String content, String link) {
        StringBuilder sb = new StringBuilder()
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"640\" style=\"")
                .append("max-width:640px;width:100%;border-spacing:0;background:").append(CARD_BG)
                .append(";border:1px solid ").append(BORDER).append(";border-top:3px solid ").append(ACCENT)
                .append(";font-family:").append(FONT).append(";\">")
                .append("<tr><td style=\"padding:16px 20px 20px;font-family:").append(FONT)
                .append(";font-size:13px;line-height:1.5;color:").append(TEXT).append(";\">")
                .append(content);
        if (link != null) {
            sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td height=\"16\" ")
                    .append("style=\"height:16px;line-height:16px;font-size:0;\">&nbsp;</td></tr><tr>")
                    .append("<td style=\"background:").append(ACCENT).append(";padding:9px 20px;\">")
                    .append("<a href=\"").append(escape(link)).append("\" style=\"font-family:").append(FONT)
                    .append(";font-size:13px;font-weight:600;color:").append(TEXT)
                    .append(";text-decoration:none;\">View in Agenda Builder</a></td></tr></table>");
        }
        return sb.append("</td></tr></table><br>").toString();
    }

    /**
     * Label and value pairs, two to a row, inset on the page background as the email's details block.
     * Pairs without a value are left out; a pair with a third element ("wide") takes the whole row.
     */
    public static String details(String[][] pairs) {
        StringBuilder sb = new StringBuilder("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"")
                .append("border-collapse:collapse;table-layout:fixed;background:").append(PAGE_BG).append(";border:1px solid ")
                .append(BORDER).append(";font-size:12px;line-height:1.4;\">");
        int column = 0;
        for (String[] pair : pairs) {
            if (pair[1] == null || pair[1].trim().isEmpty()) {
                continue;
            }
            boolean wide = pair.length > 2;
            if (wide && column == 1) {
                sb.append("<td style=\"width:50%;\"></td></tr>");
                column = 0;
            }
            if (column == 0) {
                sb.append("<tr>");
            }
            sb.append(wide ? "<td colspan=\"2\" valign=\"top\" style=\"padding:6px 12px;\">"
                    : "<td valign=\"top\" style=\"width:50%;padding:6px 12px;\">")
                    .append("<span style=\"color:").append(TEXT_MUTED).append(";\">").append(escape(pair[0])).append("</span><br>")
                    .append("<span style=\"color:").append(TEXT).append(";font-weight:600;\">").append(escape(pair[1])).append("</span></td>");
            column = wide ? 0 : (column + 1) % 2;
            if (column == 0) {
                sb.append("</tr>");
            }
        }
        if (column == 1) {
            sb.append("<td style=\"width:50%;\"></td></tr>");
        }
        return sb.append("</table>").toString();
    }

    /** An agenda item: its number and counterparty, with the time beside it, then its details. */
    public static String item(String title, String aside, String content) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\"><tr>"
                + "<td height=\"16\" style=\"height:16px;line-height:16px;font-size:0;\">&nbsp;</td></tr><tr>"
                + "<td style=\"border-left:3px solid " + ACCENT + ";padding:2px 0 2px 10px;font-family:" + FONT
                + ";font-size:14px;line-height:1.4;font-weight:600;color:" + TEXT + ";\">" + escape(title)
                + (aside == null ? "" : "<span style=\"font-size:12px;font-weight:400;color:" + TEXT_MUTED + ";\"> &nbsp;"
                + escape(aside) + "</span>")
                + "</td></tr><tr><td height=\"8\" style=\"height:8px;line-height:8px;font-size:0;\">&nbsp;</td></tr></table>"
                + content;
    }

    /** A muted note under the agenda. */
    public static String note(String text) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\"><tr>"
                + "<td style=\"padding-top:14px;font-family:" + FONT + ";font-size:12px;color:" + TEXT_MUTED + ";\">"
                + escape(text) + "</td></tr></table>";
    }
}
```

### Microsoft Graph client

Source: `api/src/main/java/za/co/rmb/approvalforum/web/graph/GraphMeetingClient.java`. SHA-256: `1ce0064f5e2bb435e1afdb69d5b64938d7ef7c38633fe61e2c0414e180684871`.

```java
/**
 * Creates the Teams/Outlook events for a committee meeting through Microsoft Graph (BRS BR-013): one
 * per approved agenda item for that item's participants and paper, and one for the meeting's standing
 * roles. The service layer describes each event as a {@link CalendarEventRequest}.
 *
 * Same shape as {@link za.co.rmb.approvalforum.web.casdm.CasdmRestClient}: a short-lived token is
 * obtained first (OAuth2 client-credentials against Entra ID, cached ~50 min), then the Graph
 * endpoint is called with it. Each event is created on the organiser mailbox's calendar
 * ({@code af.graph.organizer}) as an online Teams meeting with its attachments and its
 * attendees invited, so Outlook sends the invitations and tracks responses. Configured via af.graph.tenantId / clientId /
 * clientSecret / organizer (env AF_GRAPH_TENANT_ID / AF_GRAPH_CLIENT_ID / AF_GRAPH_CLIENT_SECRET /
 * AF_GRAPH_ORGANIZER) and an optional outbound proxy (af.graph.proxy.host / port); when unset the
 * client reports not configured and invitation sending fails.
 *
 * Entra app registration requirement: application permission Calendars.ReadWrite (admin consent).
 */
@Singleton
public class GraphMeetingClient implements AgendaCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GraphMeetingClient.class);

    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final String LOGIN_BASE = "https://login.microsoftonline.com/";
    private static final long TOKEN_TTL_MS = 50 * 60 * 1000L;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final String TIME_ZONE = "South Africa Standard Time";
    /** Graph takes a file attachment in one request below 3 MB; larger ones go through an upload session. */
    private static final int LARGE_ATTACHMENT_BYTES = 3 * 1024 * 1024;
    /** Upload ranges are kept under 4 MB, as Graph recommends. */
    private static final int UPLOAD_CHUNK_BYTES = 10 * 320 * 1024;

    private final Map<String, String> afSettings;
    private final String graphBase;
    private final String loginBase;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile String cachedToken;
    private volatile long cachedTokenExpiryMs;

    @Inject
    public GraphMeetingClient(@Named("af_settings") Map<String, String> afSettings) {
        this(afSettings, GRAPH_BASE, LOGIN_BASE);
    }

    /** Package-private endpoints let regression tests exercise real HTTP against a loopback server. */
    GraphMeetingClient(Map<String, String> afSettings, String graphBase, String loginBase) {
        this.afSettings = afSettings;
        this.graphBase = graphBase;
        this.loginBase = loginBase;
    }

    @Override
    public boolean isConfigured() {
        return !blank(setting("af.graph.tenantId")) && !blank(setting("af.graph.clientId"))
                && !blank(setting("af.graph.clientSecret")) && !blank(setting("af.graph.organizer"));
    }

    @Override
    public CalendarEvent createOrUpdateEvent(CalendarEventRequest request) {
        String key = request.getKey();
        if (!isConfigured()) {
            logger.info("Graph calendar not configured (af.graph.*) - no Teams/Outlook meeting for {}", key);
            return null;
        }
        String token = getToken();
        if (token == null) {
            return null;
        }
        String organizer = setting("af.graph.organizer").trim();
        String content = InvitationStyle.invitation(request.getHtml() == null ? "" : request.getHtml(), agendaLink(request.getLink()));
        List<Attachment> attachments = request.getAttachments();
        ObjectNode body = eventBody(request, content);
        boolean withAttachments = attachments != null && !attachments.isEmpty();
        Client client = newClient();
        try {
            String eventId = request.getExistingEventId();
            if (blank(eventId)) {
                // With papers to attach the event is created without attendees, the papers are added,
                // and the update below invites the attendees: one invitation, papers included.
                ObjectNode create = body.deepCopy();
                String identity = setting("af.agenda.ui.base") + ":" + setting("af.graph.clientId") + ":" + key;
                // Reinstatement needs a fresh event, while every retry within that cycle stays stable.
                if (request.getCancelledOn() != null) identity += ":" + request.getCancelledOn().toInstant().toString();
                create.put("transactionId", java.util.UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
                if (withAttachments) {
                    create.remove("attendees");
                }
                logger.info("Graph: creating Teams meeting for {} on {}", key, organizer);
                Response response = client.target(graphBase + "/users/" + organizer + "/events")
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .post(Entity.entity(mapper.writeValueAsString(create), MediaType.APPLICATION_JSON));
                String text = response.readEntity(String.class);
                int status = response.getStatus();
                response.close();
                if (status < 200 || status >= 300) {
                    // An earlier create can succeed before its attendee PATCH fails. Graph returns
                    // the existing event on a duplicate transaction; verify ownership before resuming.
                    eventId = recoverCreatedEventId(client, organizer, token, status, text, create.path("transactionId").asText());
                    if (blank(eventId)) {
                        logger.warn("Graph event call returned {}: {}", status, abbreviate(text));
                        return null;
                    }
                    logger.info("Graph: resuming existing event for {}", key);
                } else {
                    JsonNode created = mapper.readTree(text);
                    if (!withAttachments) {
                        return calendarEvent(created, null);
                    }
                    eventId = created.path("id").asText(null);
                }
                if (blank(eventId)) return null;
            }
            String eventUrl = graphBase + "/users/" + organizer + "/events/" + eventId;
            if (attachments != null) {
                // Graph: attachments the organiser adds reach the attendees with the next update.
                replaceAttachments(client, eventUrl, token, attachments);
            }
            logger.info("Graph: updating Teams meeting {} for {}", eventId, key);
            // Graph: replacing the body of an online meeting without its meeting blob turns the
            // meeting off, so the Teams block Exchange added to the current body is kept.
            String blob = meetingBlob(currentBody(client, eventUrl, token));
            if (!blob.isEmpty()) {
                ((ObjectNode) body.get("body")).put("content", content + blob);
            }
            Response response = client.target(eventUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .method("PATCH", Entity.entity(mapper.writeValueAsString(body), MediaType.APPLICATION_JSON));
            String text = response.readEntity(String.class);
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                logger.warn("Graph event call returned {}: {}", response.getStatus(), abbreviate(text));
                return null;
            }
            return calendarEvent(mapper.readTree(text), eventId);
        } catch (Throwable ex) {
            logger.error("Graph event call failed for {}: {}", key, ex.toString(), ex);
            return null;
        } finally {
            client.close();
        }
    }

    /** Recover only the event Graph identifies, on the same organiser mailbox and transaction. */
    private String recoverCreatedEventId(Client client, String organizer, String token, int status, String text,
                                         String transactionId) throws IOException {
        if (status != 400 && status != 409) return null;
        JsonNode error = mapper.readTree(text).path("error");
        if (!"ErrorDuplicateTransactionId".equals(error.path("code").asText())) return null;
        String eventId = error.path("@event.existingEventId").asText(null);
        if (blank(eventId)) return null;
        try (Response existing = client.target(graphBase + "/users/" + organizer + "/events/" + eventId)
                .queryParam("$select", "id,transactionId,isCancelled")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token).get()) {
            if (existing.getStatus() < 200 || existing.getStatus() >= 300) return null;
            JsonNode event = mapper.readTree(existing.readEntity(String.class));
            if (!eventId.equals(event.path("id").asText())
                    || !transactionId.equals(event.path("transactionId").asText())
                    || event.path("isCancelled").asBoolean(false)) {
                logger.warn("Graph: duplicate transaction event could not be verified");
                return null;
            }
            return eventId;
        }
    }

    private CalendarEvent calendarEvent(JsonNode node, String knownId) {
        String id = node.path("id").asText(null);
        String joinUrl = node.path("onlineMeeting").path("joinUrl").asText(null);
        if (blank(joinUrl)) {
            joinUrl = node.path("webLink").asText(null);
        }
        logger.info("Graph: event {} ready (join url present: {})", id, !blank(joinUrl));
        return new CalendarEvent(id == null ? knownId : id, joinUrl);
    }

    /** The page in Agenda Builder (af.agenda.ui.base + path), or null when the base or the path is not set. */
    private String agendaLink(String path) {
        String base = setting("af.agenda.ui.base").trim();
        return base.isEmpty() || blank(path) ? null : base + path;
    }

    /** Removes the event's attachments and adds the given ones (a file that fails is logged and skipped). */
    private void replaceAttachments(Client client, String eventUrl, String token, List<Attachment> attachments) {
        try {
            Response list = client.target(eventUrl + "/attachments").queryParam("$select", "id,name")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .get();
            String text = list.readEntity(String.class);
            if (list.getStatus() >= 200 && list.getStatus() < 300) {
                for (JsonNode existing : mapper.readTree(text).path("value")) {
                    Response deleted = client.target(eventUrl + "/attachments/" + existing.path("id").asText())
                            .request()
                            .header("Authorization", "Bearer " + token)
                            .delete();
                    deleted.close();
                    if (deleted.getStatus() < 200 || deleted.getStatus() >= 300) {
                        logger.warn("Graph: could not remove attachment {} ({})", existing.path("name").asText(), deleted.getStatus());
                    }
                }
            } else {
                logger.warn("Graph attachment list returned {}: {}", list.getStatus(), abbreviate(text));
            }
        } catch (Throwable ex) {
            logger.warn("Graph attachment list failed: {}", ex.toString());
        }
        for (Attachment attachment : attachments) {
            if (attachment.getData() == null || blank(attachment.getName())) {
                continue;
            }
            try {
                if (attachment.getData().length < LARGE_ATTACHMENT_BYTES) {
                    addAttachment(client, eventUrl, token, attachment);
                } else {
                    uploadAttachment(client, eventUrl, token, attachment);
                }
            } catch (Throwable ex) {
                logger.warn("Graph: could not attach {}: {}", attachment.getName(), ex.toString());
            }
        }
    }

    /** A file under 3 MB: one POST with its contents. */
    private void addAttachment(Client client, String eventUrl, String token, Attachment attachment) throws IOException {
        ObjectNode file = mapper.createObjectNode();
        file.put("@odata.type", "#microsoft.graph.fileAttachment");
        file.put("name", attachment.getName());
        file.put("contentBytes", Base64.getEncoder().encodeToString(attachment.getData()));
        Response response = client.target(eventUrl + "/attachments")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(mapper.writeValueAsString(file), MediaType.APPLICATION_JSON));
        String text = response.readEntity(String.class);
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            logger.warn("Graph: attaching {} returned {}: {}", attachment.getName(), response.getStatus(), abbreviate(text));
        } else {
            logger.info("Graph: attached {}", attachment.getName());
        }
    }

    /** A file of 3 MB or more: an upload session, then the bytes in ranges (the upload URL carries its own auth). */
    private void uploadAttachment(Client client, String eventUrl, String token, Attachment attachment) throws IOException {
        byte[] data = attachment.getData();
        ObjectNode session = mapper.createObjectNode();
        ObjectNode item = session.putObject("AttachmentItem");
        item.put("attachmentType", "file");
        item.put("name", attachment.getName());
        item.put("size", data.length);
        Response created = client.target(eventUrl + "/attachments/createUploadSession")
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(mapper.writeValueAsString(session), MediaType.APPLICATION_JSON));
        String text = created.readEntity(String.class);
        if (created.getStatus() < 200 || created.getStatus() >= 300) {
            logger.warn("Graph: upload session for {} returned {}: {}", attachment.getName(), created.getStatus(), abbreviate(text));
            return;
        }
        String uploadUrl = mapper.readTree(text).path("uploadUrl").asText();
        for (int from = 0; from < data.length; from += UPLOAD_CHUNK_BYTES) {
            int to = Math.min(data.length, from + UPLOAD_CHUNK_BYTES);
            Response put = client.target(uploadUrl)
                    .request()
                    .header("Content-Range", "bytes " + from + "-" + (to - 1) + "/" + data.length)
                    .put(Entity.entity(Arrays.copyOfRange(data, from, to), MediaType.APPLICATION_OCTET_STREAM));
            String reply = put.readEntity(String.class);
            if (put.getStatus() < 200 || put.getStatus() >= 300) {
                logger.warn("Graph: upload of {} stopped at byte {} ({}): {}", attachment.getName(), from, put.getStatus(), abbreviate(reply));
                return;
            }
        }
        logger.info("Graph: attached {} ({} bytes, upload session)", attachment.getName(), data.length);
    }

    @Override
    public void cancelEvent(String eventId, String reason) {
        if (blank(eventId)) return;
        if (!isConfigured()) throw new IllegalStateException("Calendar service is unavailable");
        String token = getToken();
        if (token == null) throw new IllegalStateException("Calendar access failed");
        String organizer = setting("af.graph.organizer").trim();
        Client client = newClient();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("comment", reason == null ? "Meeting cancelled" : reason);
            Response response = client.target(graphBase + "/users/" + organizer + "/events/" + eventId + "/cancel")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.entity(mapper.writeValueAsString(body), MediaType.APPLICATION_JSON));
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new IllegalStateException("Calendar cancellation returned " + response.getStatus());
            } else {
                logger.info("Graph: event {} cancelled", eventId);
            }
        } catch (Exception ex) {
            logger.error("Graph cancel failed for event {}: {}", eventId, ex.toString(), ex);
            throw new IllegalStateException("Calendar invitation cancellation failed", ex);
        } finally {
            client.close();
        }
    }

    /** The event's current HTML body, or "" when it cannot be read. */
    private String currentBody(Client client, String eventUrl, String token) {
        try {
            Response response = client.target(eventUrl).queryParam("$select", "body")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .get();
            String text = response.readEntity(String.class);
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                logger.warn("Graph event read returned {}: {}", response.getStatus(), abbreviate(text));
                return "";
            }
            return mapper.readTree(text).path("body").path("content").asText("");
        } catch (Throwable ex) {
            logger.warn("Graph event read failed: {}", ex.toString());
            return "";
        }
    }

    /**
     * The Teams meeting block Exchange appends to an online meeting's body: from the block that holds
     * the join link up to the end of the body, or "" when the body has none.
     */
    static String meetingBlob(String html) {
        if (html == null) {
            return "";
        }
        int join = html.indexOf("meet_invite_block.action.join_link");
        if (join < 0) {
            return "";
        }
        int separator = html.lastIndexOf("________________", join);
        int start = html.lastIndexOf("<div class=\"me-email-text\"", separator < 0 ? join : separator);
        if (start < 0) {
            start = html.lastIndexOf("<div", separator < 0 ? join : separator);
        }
        if (start < 0) {
            return "";
        }
        int end = html.toLowerCase().lastIndexOf("</body>");
        return html.substring(start, end > start ? end : html.length());
    }

    private ObjectNode eventBody(CalendarEventRequest request, String agendaHtml) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(request.getDate());
        String start = blank(request.getStartTime()) ? "08:00" : request.getStartTime().trim();
        String end = blank(request.getEndTime()) ? plusHours(start, 4) : request.getEndTime().trim();
        List<String> attendeeEmails = request.getAttendees();

        ObjectNode body = mapper.createObjectNode();
        body.put("subject", request.getSubject() == null ? "" : request.getSubject());
        ObjectNode content = body.putObject("body");
        content.put("contentType", "HTML");
        content.put("content", agendaHtml == null ? "" : agendaHtml);
        ObjectNode startNode = body.putObject("start");
        startNode.put("dateTime", date + "T" + start + ":00");
        startNode.put("timeZone", TIME_ZONE);
        ObjectNode endNode = body.putObject("end");
        endNode.put("dateTime", date + "T" + end + ":00");
        endNode.put("timeZone", TIME_ZONE);
        if (!blank(request.getVenue())) {
            body.putObject("location").put("displayName", request.getVenue());
        }
        String mode = request.getHeldVia() == null ? "" : request.getHeldVia().toLowerCase();
        boolean online = mode.isEmpty() || mode.contains("teams") || mode.contains("hybrid") || mode.contains("virtual");
        body.put("isOnlineMeeting", online);
        if (online) {
            body.put("onlineMeetingProvider", "teamsForBusiness");
        }
        body.put("allowNewTimeProposals", false);
        ArrayNode attendees = body.putArray("attendees");
        if (attendeeEmails != null) {
            for (String email : attendeeEmails) {
                if (blank(email)) {
                    continue;
                }
                ObjectNode attendee = attendees.addObject();
                attendee.putObject("emailAddress").put("address", email.trim());
                attendee.put("type", "required");
            }
        }
        return body;
    }

    private String plusHours(String hhmm, int hours) {
        try {
            String[] parts = hhmm.split(":");
            int h = Math.min(23, Integer.parseInt(parts[0]) + hours);
            return String.format("%02d:%s", h, parts[1]);
        } catch (Exception ex) {
            return "12:00";
        }
    }

    private synchronized String getToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < cachedTokenExpiryMs) {
            return cachedToken;
        }
        String tenant = setting("af.graph.tenantId").trim();
        Client client = newClient();
        try {
            logger.info("Graph token exchange: POST {}{}/oauth2/v2.0/token", loginBase, tenant);
            Form form = new Form()
                    .param("client_id", setting("af.graph.clientId").trim())
                    .param("client_secret", setting("af.graph.clientSecret").trim())
                    .param("scope", GRAPH_SCOPE)
                    .param("grant_type", "client_credentials");
            Response response = client.target(loginBase + tenant + "/oauth2/v2.0/token")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.form(form));
            String text = response.readEntity(String.class);
            if (response.getStatus() != 200) {
                logger.warn("Graph token endpoint returned {}: {}", response.getStatus(), abbreviate(text));
                return null;
            }
            JsonNode node = mapper.readTree(text);
            String token = node.path("access_token").asText(null);
            if (blank(token)) {
                return null;
            }
            long ttl = node.has("expires_in") ? (node.get("expires_in").asLong() - 120) * 1000L : TOKEN_TTL_MS;
            cachedToken = token;
            cachedTokenExpiryMs = now + Math.max(60_000L, Math.min(ttl, TOKEN_TTL_MS));
            logger.info("Graph token exchange: 200 OK");
            return cachedToken;
        } catch (Throwable ex) {
            logger.error("Graph token exchange failed: {}", ex.toString(), ex);
            return null;
        } finally {
            client.close();
        }
    }

    private Client newClient() {
        // Select a transport that supports PATCH, without reflective HttpURLConnection workarounds.
        org.glassfish.jersey.client.ClientConfig config = new org.glassfish.jersey.client.ClientConfig()
                .connectorProvider(new org.glassfish.jersey.apache5.connector.Apache5ConnectorProvider());
        config.property(org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
        config.property(org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT, READ_TIMEOUT_MS);
        ClientBuilder builder = new org.glassfish.jersey.client.JerseyClientBuilder().withConfig(config);
        String proxyHost = setting("af.graph.proxy.host");
        if (!blank(proxyHost)) {
            int port = 8080;
            try {
                port = Integer.parseInt(setting("af.graph.proxy.port").trim());
            } catch (Exception ignored) {
                // default port
            }
            builder.property(org.glassfish.jersey.client.ClientProperties.PROXY_URI, "http://" + proxyHost.trim() + ":" + port);
        }
        return builder.build();
    }

    private String setting(String key) {
        String value = afSettings.get(key);
        return value == null ? "" : value;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 500 ? text.substring(0, 500) + "..." : text;
    }

}
```
