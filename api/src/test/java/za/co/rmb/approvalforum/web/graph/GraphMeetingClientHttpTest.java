package za.co.rmb.approvalforum.web.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.rmb.approvalforum.model.agenda.AgendaMeetingDTO;
import za.co.rmb.approvalforum.service.AgendaCalendarService.Attachment;
import za.co.rmb.approvalforum.service.AgendaCalendarService.CalendarEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the actual HTTP connector against loopback only; no mail or calendar requests leave the PC. */
public class GraphMeetingClientHttpTest {
    private static final String EVENTS = "/v1.0/users/organizer@example.test/events";
    private static final String EVENT = "{\"id\":\"event-10\",\"onlineMeeting\":{\"joinUrl\":\"https://teams.example.test/10\"}}";
    private static final String TEAMS_BLOB = "<div class=\"me-email-text\">Microsoft Teams meeting<a id=\"meet_invite_block.action.join_link\">Join</a></div>";
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String base;
    private volatile boolean failPatch;
    private volatile boolean duplicateAfterCreate;
    private volatile boolean createConflict;
    private volatile boolean includeExistingId = true;
    private volatile boolean wrongTransaction;
    private volatile boolean cancelled;
    private volatile String createdTransaction;
    private volatile String createErrorCode = "ErrorDuplicateTransactionId";
    private volatile int createFailureStatus = 400;
    private volatile int recoveryStatus = 200;

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    public void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    public void createsAnInvitationWithAttendeesWhenThereAreNoPapers() {
        CalendarEvent event = client(false).createOrUpdateEvent(meeting(null), attendees(), "<p>Agenda</p>", Collections.emptyList());
        assertNotNull(event);
        assertEquals("event-10", event.getEventId());
        assertEquals("https://teams.example.test/10", event.getJoinUrl());
        assertEquals(1, matching("POST", EVENTS).size());
        assertEquals("attendee@example.test", matching("POST", EVENTS).get(0).body.path("attendees").get(0).path("emailAddress").path("address").asText());
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
    }

    @Test
    public void attachesPaperThenPatchInvitesAttendeesAndPreservesTeamsDetails() {
        CalendarEvent event = client(false).createOrUpdateEvent(meeting(null), attendees(), "<p>Agenda</p>", papers());
        assertNotNull(event, "PATCH must reach the server instead of failing in HttpURLConnection");
        assertEquals("event-10", event.getEventId());
        assertFalse(matching("POST", EVENTS).get(0).body.has("attendees"));
        Request attachment = matching("POST", EVENTS + "/event-10/attachments").get(0);
        assertEquals("credit-paper.pdf", attachment.body.path("name").asText());
        assertEquals("JVBERi10ZXN0", attachment.body.path("contentBytes").asText());
        Request patch = matching("PATCH", EVENTS + "/event-10").get(0);
        assertEquals("attendee@example.test", patch.body.path("attendees").get(0).path("emailAddress").path("address").asText());
        assertTrue(patch.body.path("body").path("content").asText().contains(TEAMS_BLOB));
        assertTrue(requests.indexOf(attachment) < requests.indexOf(patch), "The paper is attached before attendees are invited");
        assertEquals("Bearer loopback-only", patch.authorization);
    }

    @Test
    public void updatesExistingInvitationWithPatchWithoutCreatingAnotherEvent() {
        CalendarEvent event = client(false).createOrUpdateEvent(meeting("event-10"), attendees(), "<p>Updated agenda</p>", papers());
        assertNotNull(event);
        assertEquals("event-10", event.getEventId());
        assertTrue(matching("POST", EVENTS).isEmpty());
        assertEquals(1, matching("PATCH", EVENTS + "/event-10").size());
        assertTrue(matching("PATCH", EVENTS + "/event-10").get(0).body.path("body").path("content").asText().contains("Updated agenda"));
    }

    @Test
    public void failedAttendeePatchReportsFailureAndRetryKeepsCreationTransactionIdentity() {
        GraphMeetingClient client = client(false);
        AgendaMeetingDTO meeting = meeting(null);
        failPatch = true;
        duplicateAfterCreate = true;
        assertNull(client.createOrUpdateEvent(meeting, attendees(), "<p>Agenda</p>", papers()));
        assertNull(meeting.getGraphEventId(), "A failed send must not mark the meeting as invited");
        failPatch = false;
        assertNotNull(client.createOrUpdateEvent(meeting, attendees(), "<p>Agenda</p>", papers()));
        List<Request> creates = matching("POST", EVENTS);
        assertEquals(2, creates.size());
        String transactionId = creates.get(0).body.path("transactionId").asText();
        assertFalse(transactionId.isEmpty());
        assertEquals(transactionId, creates.get(1).body.path("transactionId").asText());
        assertEquals(2, matching("PATCH", EVENTS + "/event-10").size());
        assertEquals(1, requests.stream().filter(r -> r.method.equals("GET") && r.uri.contains("transactionId")).count());
    }

    @Test
    public void configuredOutboundProxyCarriesTokenCreateAndPatchRequests() {
        CalendarEvent event = client(true).createOrUpdateEvent(meeting(null), attendees(), "<p>Agenda</p>", papers());
        assertNotNull(event, "Proxy configuration must apply to the selected connector");
        assertTrue(requests.stream().anyMatch(r -> r.uri.startsWith("http://auth.invalid/")));
        Request patch = matching("PATCH", EVENTS + "/event-10").get(0);
        assertTrue(patch.uri.startsWith("http://graph.invalid/"));
    }

    @Test
    public void duplicateConflictWithoutPapersResumesWithCurrentAttendees() {
        createConflict = true;
        createFailureStatus = 409;
        CalendarEvent event = client(false).createOrUpdateEvent(meeting(null), attendees(), "<p>Current agenda</p>", Collections.emptyList());
        assertNotNull(event);
        assertEquals("event-10", event.getEventId());
        assertEquals(1, matching("POST", EVENTS).size());
        Request patch = matching("PATCH", EVENTS + "/event-10").get(0);
        assertTrue(patch.body.path("body").path("content").asText().contains("Current agenda"));
        assertEquals("attendee@example.test", patch.body.path("attendees").get(0).path("emailAddress").path("address").asText());
    }

    @Test
    public void duplicateWithoutExistingIdRemainsFailedWithoutUpdatingAnything() {
        createConflict = true;
        includeExistingId = false;
        assertNull(client(false).createOrUpdateEvent(meeting(null), attendees(), "Agenda", papers()));
        assertTrue(matching("GET", EVENTS + "/event-10").isEmpty());
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
    }

    @Test
    public void unrelatedCreateErrorNeverAttemptsRecovery() {
        createConflict = true;
        createErrorCode = "ErrorInvalidRequest";
        assertNull(client(false).createOrUpdateEvent(meeting(null), attendees(), "Agenda", papers()));
        assertTrue(matching("GET", EVENTS + "/event-10").isEmpty());
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
    }

    @Test
    public void permissionFailureNeverAttemptsRecoveryEvenWithDuplicateCode() {
        createConflict = true;
        createFailureStatus = 403;
        assertNull(client(false).createOrUpdateEvent(meeting(null), attendees(), "Agenda", papers()));
        assertTrue(matching("GET", EVENTS + "/event-10").isEmpty());
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
    }

    @Test
    public void duplicateBelongingToAnotherTransactionIsNeverModified() {
        createConflict = true;
        wrongTransaction = true;
        assertNull(client(false).createOrUpdateEvent(meeting(null), attendees(), "Agenda", papers()));
        assertEquals(1, matching("GET", EVENTS + "/event-10").size());
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
        assertTrue(matching("POST", EVENTS + "/event-10/attachments").isEmpty());
    }

    @Test
    public void cancelledDuplicateIsNeverResent() {
        createConflict = true;
        cancelled = true;
        assertNull(client(false).createOrUpdateEvent(meeting(null), attendees(), "Agenda", papers()));
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
        assertTrue(matching("POST", EVENTS + "/event-10/attachments").isEmpty());
    }

    @Test
    public void unreadableDuplicateRemainsFailedWithoutCreatingAnotherEvent() {
        createConflict = true;
        recoveryStatus = 404;
        assertNull(client(false).createOrUpdateEvent(meeting(null), attendees(), "Agenda", papers()));
        assertEquals(1, matching("POST", EVENTS).size());
        assertTrue(matching("PATCH", EVENTS + "/event-10").isEmpty());
        assertTrue(matching("POST", EVENTS + "/event-10/attachments").isEmpty());
    }

    @Test
    public void reinstatedMeetingUsesFreshTransactionWhileItsRetriesRemainStable() {
        GraphMeetingClient client = client(false);
        AgendaMeetingDTO meeting = meeting(null);
        assertNotNull(client.createOrUpdateEvent(meeting, attendees(), "Agenda", Collections.emptyList()));
        meeting.setCancelledOn(java.sql.Timestamp.valueOf("2026-10-17 18:30:00"));
        assertNotNull(client.createOrUpdateEvent(meeting, attendees(), "Restored agenda", Collections.emptyList()));
        createConflict = true;
        assertNotNull(client.createOrUpdateEvent(meeting, attendees(), "Restored agenda", Collections.emptyList()));
        List<Request> creates = matching("POST", EVENTS);
        assertEquals(3, creates.size());
        assertNotEquals(creates.get(0).body.path("transactionId").asText(), creates.get(1).body.path("transactionId").asText());
        assertEquals(creates.get(1).body.path("transactionId").asText(), creates.get(2).body.path("transactionId").asText());
    }

    @Test
    public void aLaterCancellationAndReinstatementCreatesAnotherFreshTransaction() {
        GraphMeetingClient client = client(false);
        AgendaMeetingDTO meeting = meeting(null);
        meeting.setCancelledOn(java.sql.Timestamp.valueOf("2026-10-17 18:30:00"));
        assertNotNull(client.createOrUpdateEvent(meeting, attendees(), "First reinstatement", Collections.emptyList()));
        meeting.setCancelledOn(java.sql.Timestamp.valueOf("2026-10-18 08:00:00"));
        assertNotNull(client.createOrUpdateEvent(meeting, attendees(), "Second reinstatement", Collections.emptyList()));
        List<Request> creates = matching("POST", EVENTS);
        assertNotEquals(creates.get(0).body.path("transactionId").asText(), creates.get(1).body.path("transactionId").asText());
    }

    private GraphMeetingClient client(boolean proxy) {
        Map<String, String> settings = new HashMap<>();
        settings.put("af.graph.tenantId", "test-tenant");
        settings.put("af.graph.clientId", "test-client");
        settings.put("af.graph.clientSecret", "test-secret");
        settings.put("af.graph.organizer", "organizer@example.test");
        settings.put("af.agenda.ui.base", "https://agenda.example.test");
        if (proxy) {
            settings.put("af.graph.proxy.host", "127.0.0.1");
            settings.put("af.graph.proxy.port", String.valueOf(server.getAddress().getPort()));
        }
        return new GraphMeetingClient(settings, (proxy ? "http://graph.invalid" : base) + "/v1.0", (proxy ? "http://auth.invalid" : base) + "/");
    }

    private AgendaMeetingDTO meeting(String eventId) {
        AgendaMeetingDTO meeting = new AgendaMeetingDTO();
        meeting.setAgmId(10);
        meeting.setCommitteeName("Fixture committee");
        meeting.setMandateLevel("L3");
        meeting.setMeetingDate(java.sql.Timestamp.valueOf("2026-10-27 00:00:00"));
        meeting.setStartTime("08:00");
        meeting.setEndTime("09:00");
        meeting.setHeldVia("MS Teams");
        meeting.setGraphEventId(eventId);
        return meeting;
    }

    private List<String> attendees() { return Arrays.asList("attendee@example.test"); }
    private List<Attachment> papers() { return Arrays.asList(new Attachment("credit-paper.pdf", "%PDF-test".getBytes(StandardCharsets.UTF_8))); }
    private List<Request> matching(String method, String path) {
        List<Request> result = new ArrayList<>();
        for (Request request : requests) if (method.equals(request.method) && path.equals(request.path)) result.add(request);
        return result;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String text = readBody(exchange.getRequestBody());
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        requests.add(new Request(method, path, exchange.getRequestURI().toString(), text.startsWith("{") ? mapper.readTree(text) : mapper.createObjectNode(), exchange.getRequestHeaders().getFirst("Authorization")));
        int status = 200;
        String response;
        if (path.equals("/test-tenant/oauth2/v2.0/token") && method.equals("POST")) response = "{\"access_token\":\"loopback-only\",\"expires_in\":3600}";
        else if (path.equals(EVENTS) && method.equals("POST")) {
            createdTransaction = mapper.readTree(text).path("transactionId").asText();
            if (createConflict || (duplicateAfterCreate && matching("POST", EVENTS).size() > 1)) {
                status = createFailureStatus;
                JsonNode error = mapper.createObjectNode().put("code", createErrorCode).put("message", "The TransactionId has already been used to create an event.");
                if (includeExistingId) ((com.fasterxml.jackson.databind.node.ObjectNode) error).put("@event.existingEventId", "event-10");
                response = mapper.createObjectNode().set("error", error).toString();
            } else { status = 201; response = EVENT; }
        }
        else if (path.equals(EVENTS + "/event-10/attachments") && method.equals("GET")) response = "{\"value\":[]}";
        else if (path.equals(EVENTS + "/event-10/attachments") && method.equals("POST")) { status = 201; response = "{\"id\":\"paper-1\"}"; }
        else if (path.equals(EVENTS + "/event-10") && method.equals("GET")) {
            if (exchange.getRequestURI().getQuery().contains("transactionId")) {
                status = recoveryStatus;
                response = mapper.createObjectNode().put("id", "event-10").put("transactionId", wrongTransaction ? "another-transaction" : createdTransaction).put("isCancelled", cancelled).toString();
            } else response = mapper.createObjectNode().set("body", mapper.createObjectNode().put("content", "<html><body>Previous agenda" + TEAMS_BLOB + "</body></html>")).toString();
        }
        else if (path.equals(EVENTS + "/event-10") && method.equals("PATCH")) { status = failPatch ? 500 : 200; response = failPatch ? "{\"error\":\"fixture failure\"}" : EVENT; }
        else { status = 404; response = "{}"; }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String readBody(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static class Request {
        final String method, path, uri, authorization;
        final JsonNode body;
        Request(String method, String path, String uri, JsonNode body, String authorization) {
            this.method = method; this.path = path; this.uri = uri; this.body = body; this.authorization = authorization;
        }
    }
}
