package za.co.rmb.credit.daf.mcp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A stand-in daf-api, served by the test's own Boot application under {@code /approvalforum/api}: records
 * every request it gets and answers a few endpoints the way daf-api does (JSON, a download, a refusal).
 */
@RestController
@RequestMapping("/approvalforum/api")
public class StandInDafApi {

    /** One request as the stand-in saw it. */
    public record Seen(String method, String path, String query, Map<String, String> headers, String body) { }

    public static final List<Seen> SEEN = new ArrayList<>();

    public static Seen last() {
        return SEEN.get(SEEN.size() - 1);
    }

    @PostMapping(path = "/approvalForum/agenda/items/{agiId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(HttpServletRequest request, @RequestPart("file") MultipartFile file,
                                      @RequestParam(value = "docType", required = false) String docType) throws IOException {
        record(request, "multipart " + file.getOriginalFilename() + " " + new String(file.getBytes(), StandardCharsets.UTF_8));
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("fileName", file.getOriginalFilename());
        answer.put("size", file.getSize());
        answer.put("docType", docType);
        return answer;
    }

    @RequestMapping("/**")
    public ResponseEntity<?> any(HttpServletRequest request) throws IOException {
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Seen seen = record(request, body);
        String key = seen.method() + " " + seen.path();
        return switch (key) {
            case "GET /approvalForum/agenda/committees" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("[{\"agcId\":1,\"name\":\"Credit Committee\"},{\"agcId\":2,\"name\":\"REIB\"}]");
            case "POST /approvalForum/agenda/meetings/7/cancel" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"agmId\":7,\"status\":\"Cancelled\"}");
            case "POST /approvalForum/request/wip" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"workflowId\":42,\"echo\":" + body + "}");
            case "GET /approvalForum/agenda/items/9" -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"Only the owner may open this item\"}");
            case "GET /documents/download" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"paper.pdf\"")
                    .body("%PDF-1.4 stand-in".getBytes(StandardCharsets.UTF_8));
            case "DELETE /approvalForum/agenda/drafts/3" -> ResponseEntity.noContent().build();
            case "GET /auth/ping" -> ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("pong");
            default -> ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        };
    }

    private static Seen record(HttpServletRequest request, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(n -> headers.put(n.toLowerCase(), request.getHeader(n)));
        String path = request.getRequestURI().substring("/approvalforum/api".length());
        Seen seen = new Seen(request.getMethod(), path, request.getQueryString(), headers, body);
        SEEN.add(seen);
        return seen;
    }
}
