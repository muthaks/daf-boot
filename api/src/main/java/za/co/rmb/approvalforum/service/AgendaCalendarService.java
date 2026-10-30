package za.co.rmb.approvalforum.service;

import java.sql.Timestamp;
import java.util.List;

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
