package za.co.rmb.approvalforum.web.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.service.AgendaCalendarService;
import za.co.rmb.approvalforum.service.InvitationStyle;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Creates the Teams/Outlook meeting for a committee meeting through Microsoft Graph (BRS BR-013).
 *
 * Same shape as {@link za.co.rmb.approvalforum.web.casdm.CasdmRestClient}: a short-lived token is
 * obtained first (OAuth2 client-credentials against Entra ID, cached ~50 min), then the Graph
 * endpoint is called with it. The event is created on the organiser mailbox's calendar
 * ({@code af.graph.organizer}) as an online Teams meeting with the meeting pack attached and the
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
    public CalendarEvent createOrUpdateEvent(AgendaMeetingDTO meeting, List<String> attendeeEmails, String agendaHtml,
                                             List<Attachment> attachments) {
        if (!isConfigured()) {
            logger.info("Graph calendar not configured (af.graph.*) - no Teams/Outlook meeting for meeting {}", meeting.getAgmId());
            return null;
        }
        String token = getToken();
        if (token == null) {
            return null;
        }
        String organizer = setting("af.graph.organizer").trim();
        String content = InvitationStyle.invitation(agendaHtml == null ? "" : agendaHtml, agendaLink(meeting));
        ObjectNode body = eventBody(meeting, attendeeEmails, content);
        boolean withAttachments = attachments != null && !attachments.isEmpty();
        Client client = newClient();
        try {
            String eventId = meeting.getGraphEventId();
            if (blank(eventId)) {
                // With papers to attach the event is created without attendees, the papers are added,
                // and the update below invites the attendees: one invitation, papers included.
                ObjectNode create = body.deepCopy();
                String identity = setting("af.agenda.ui.base") + ":" + setting("af.graph.clientId") + ":" + meeting.getAgmId();
                // Reinstatement needs a fresh event, while every retry within that cycle stays stable.
                if (meeting.getCancelledOn() != null) identity += ":" + meeting.getCancelledOn().toInstant().toString();
                create.put("transactionId", java.util.UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
                if (withAttachments) {
                    create.remove("attendees");
                }
                logger.info("Graph: creating Teams meeting for agenda meeting {} on {}", meeting.getAgmId(), organizer);
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
                    logger.info("Graph: resuming existing event for agenda meeting {}", meeting.getAgmId());
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
            logger.info("Graph: updating Teams meeting {} for agenda meeting {}", eventId, meeting.getAgmId());
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
            logger.error("Graph event call failed for meeting {}: {}", meeting.getAgmId(), ex.toString(), ex);
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

    /** The meeting in Agenda Builder (af.agenda.ui.base), or null when the base is not set. */
    private String agendaLink(AgendaMeetingDTO meeting) {
        String base = setting("af.agenda.ui.base").trim();
        return base.isEmpty() ? null : base + "/agenda/meeting/" + meeting.getAgmId();
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
    public void cancelEvent(AgendaMeetingDTO meeting, String reason) {
        if (blank(meeting.getGraphEventId())) return;
        if (!isConfigured()) throw new IllegalStateException("Calendar service is unavailable");
        String token = getToken();
        if (token == null) throw new IllegalStateException("Calendar access failed");
        String organizer = setting("af.graph.organizer").trim();
        Client client = newClient();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("comment", reason == null ? "Meeting cancelled" : reason);
            Response response = client.target(graphBase + "/users/" + organizer + "/events/" + meeting.getGraphEventId() + "/cancel")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.entity(mapper.writeValueAsString(body), MediaType.APPLICATION_JSON));
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new IllegalStateException("Calendar cancellation returned " + response.getStatus());
            } else {
                logger.info("Graph: event {} cancelled", meeting.getGraphEventId());
            }
        } catch (Exception ex) {
            logger.error("Graph cancel failed for meeting {}: {}", meeting.getAgmId(), ex.toString(), ex);
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

    private ObjectNode eventBody(AgendaMeetingDTO meeting, List<String> attendeeEmails, String agendaHtml) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(meeting.getMeetingDate());
        String start = blank(meeting.getStartTime()) ? "08:00" : meeting.getStartTime().trim();
        String end = blank(meeting.getEndTime()) ? plusHours(start, 4) : meeting.getEndTime().trim();

        ObjectNode body = mapper.createObjectNode();
        body.put("subject", subject(meeting));
        ObjectNode content = body.putObject("body");
        content.put("contentType", "HTML");
        content.put("content", agendaHtml == null ? "" : agendaHtml);
        ObjectNode startNode = body.putObject("start");
        startNode.put("dateTime", date + "T" + start + ":00");
        startNode.put("timeZone", TIME_ZONE);
        ObjectNode endNode = body.putObject("end");
        endNode.put("dateTime", date + "T" + end + ":00");
        endNode.put("timeZone", TIME_ZONE);
        if (!blank(meeting.getVenue())) {
            body.putObject("location").put("displayName", meeting.getVenue());
        }
        String mode = meeting.getHeldVia() == null ? "" : meeting.getHeldVia().toLowerCase();
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

    private String subject(AgendaMeetingDTO meeting) {
        return meeting.getCommitteeName()
                + (blank(meeting.getMandateLevel()) ? "" : " " + meeting.getMandateLevel()) + " - Credit Committee";
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
