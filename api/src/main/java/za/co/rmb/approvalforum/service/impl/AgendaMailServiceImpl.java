package za.co.rmb.approvalforum.service.impl;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.data.ApprovalForumAgendaRepository;
import za.co.rmb.approvalforum.data.entity.ApprovalForumAgnAgendaNotificationEntity;
import za.co.rmb.approvalforum.model.agenda.AgendaItemDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.model.agenda.AgendaQuorumResultDTO;
import za.co.rmb.approvalforum.service.AgendaMailService;
import za.co.rmb.approvalforum.service.ApprovalForumAuthService;
import za.co.rmb.credit.authservice.model.ActiveDirectoryUserModel;
import za.co.rmb.credit.mailservice.model.AttachmentModel;
import za.co.rmb.credit.mailservice.model.MailModel;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        send(null, meeting.getAgmId(), "Final", recipientEmails,
                SUBJECT_PREFIX + "Final agenda: " + meeting.getCommitteeName(), shell(content, dafLink(null, meeting.getAgmId())),
                meetingPack(meeting));
    }

    // ---- BRS alignment: review/approval workflow and meeting lifecycle ----

    @Override
    public void sendItemSubmittedMail(AgendaItemDTO item, String reviewerEmails) {
        String content = banner("#e6f4f4", "#b3dede", "Agenda Item Submitted", "FOR REVIEW", "#006c68",
                item.getCounterpartyName() + " has been submitted for review on the " + committeeOf(item)
                        + " agenda for " + dateOf(item) + ". Please review the submission and return or approve it.")
                + itemSections(item);
        String to = join(reviewerEmails, analystRecipients(item));
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
                        + " agenda for " + dateOf(item) + " and will be included when the agenda is compiled.")
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

    /** The pack distributed with the final agenda: the current credit paper of every approved item. */
    private List<AttachmentModel> meetingPack(AgendaMeetingDTO meeting) {
        return attachments(MeetingPapers.meetingPack(agendaRepository, meeting));
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
    private String resolveEmails(String userIds) {
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
