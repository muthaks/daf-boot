package za.co.rmb.approvalforumdrools.web.resources.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import za.co.rmb.approvalforumdrools.engine.ApprovalForumDroolsEngine;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesRequestDTO;
import za.co.rmb.credit.daf.support.LegacyObjectMapper;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The rules engine on Drools 10 answers exactly as the legacy rules service on Drools 7.8.
 *
 * <p>{@code golden/rules-golden.json.gz} holds 24,648 requests generated from RuleData.drl (every meeting rule,
 * WatchList, bank, Annual Review, every fit, every country group alone and in pairs, movement changed or not)
 * and the legacy service's answers (rules/tools/rules-golden.py). Each request is replayed through the legacy
 * resource code ({@code processApprovalForumRules}, which maps the request, runs the engine and maps the
 * response) with the engine built from the same DRL, serialised with the legacy JSON mapper, and compared.</p>
 */
class RulesGoldenTest {

    private static final ObjectMapper MAPPER = LegacyObjectMapper.create();
    private static ApprovalForumDroolsResourceUtil resource;

    @BeforeAll
    static void engineFromTheLegacyRules() throws Exception {
        String rules = String.join("\n", Files.readAllLines(
                Path.of(RulesGoldenTest.class.getResource("/approvalforum/RuleData.drl").toURI()))) + "\n";
        resource = new ApprovalForumDroolsResourceUtil() { };
        Field engine = ApprovalForumDroolsResourceUtil.class.getDeclaredField("approvalForumDroolsEngine");
        engine.setAccessible(true);
        engine.set(resource, new ApprovalForumDroolsEngine(rules));
    }

    @Test
    void everyGoldenRequestGetsTheLegacyAnswer() throws Exception {
        JsonNode golden;
        try (InputStream in = new GZIPInputStream(RulesGoldenTest.class.getResourceAsStream("/golden/rules-golden.json.gz"))) {
            golden = MAPPER.readTree(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        List<String> mismatches = new ArrayList<>();
        int checked = 0;
        for (JsonNode record : golden.get("records")) {
            ApprovalForumRulesRequestDTO request = MAPPER.treeToValue(record.get("request"), ApprovalForumRulesRequestDTO.class);
            int status;
            JsonNode answer;
            try {
                Response response = resource.processApprovalForumRules(request);
                status = response.getStatus();
                answer = MAPPER.readTree(MAPPER.writeValueAsString(response.getEntity()));
            } catch (WebApplicationException e) {
                status = e.getResponse().getStatus();
                answer = null;
            }
            checked++;
            boolean sameStatus = status == record.get("status").asInt();
            boolean sameBody = answer == null ? record.get("response").isNull() || !sameStatus
                    : answer.equals(record.get("response"));
            if (!sameStatus || !sameBody) {
                mismatches.add(record.get("request") + "\n  legacy " + record.get("status") + " " + record.get("response")
                        + "\n  boot   " + status + " " + answer);
            }
        }
        if (!mismatches.isEmpty()) {
            fail(mismatches.size() + " of " + checked + " answers differ, first:\n" + String.join("\n", mismatches.subList(0, Math.min(5, mismatches.size()))));
        }
        assertTrue(checked > 20000, "golden set unexpectedly small: " + checked);
    }
}
