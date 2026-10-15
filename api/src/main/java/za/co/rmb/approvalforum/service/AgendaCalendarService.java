package za.co.rmb.approvalforum.service;

import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;

import java.util.List;

/**
 * Creates and maintains the Teams/Outlook meeting for a committee meeting (BRS BR-013).
 * Implemented in approvalforum-web against Microsoft Graph; the service layer only sees this
 * contract so it can be stubbed and so an unconfigured environment degrades gracefully.
 */
public interface AgendaCalendarService {

    /** Result of creating or updating the calendar event. */
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

    /** A file attached to the invitation (the meeting pack). */
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

    boolean isConfigured();

    /**
     * Create the event when the meeting has none yet (graphEventId null), otherwise update it, with
     * the attachments replacing any the event already has; attendees receive them with the invitation.
     * Returns null when the integration is not configured or the call failed (already logged).
     */
    CalendarEvent createOrUpdateEvent(AgendaMeetingDTO meeting, List<String> attendeeEmails, String agendaHtml,
                                      List<Attachment> attachments);

    /** Cancel the event; no-op when there is none or the integration is not configured. */
    void cancelEvent(AgendaMeetingDTO meeting, String reason);

}
