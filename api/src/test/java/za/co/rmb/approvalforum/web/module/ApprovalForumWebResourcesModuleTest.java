package za.co.rmb.approvalforum.web.module;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The settings module keeps credentials out of the source and out of the log, and names the scheduler's user. */
class ApprovalForumWebResourcesModuleTest {

    @SuppressWarnings("unchecked")
    private static Map<String, String> defaults() throws Exception {
        Field field = ApprovalForumWebResourcesModule.class.getDeclaredField("DEFAULTS");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }

    @Test
    @DisplayName("No credential has a built-in value: every token, secret and API key must come from the environment")
    void defaultsCarryNoCredentials() throws Exception {
        Map<String, String> defaults = defaults();
        int credentials = 0;
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (ApprovalForumWebResourcesModule.isCredential(entry.getKey())) {
                credentials++;
                assertEquals("", entry.getValue(), entry.getKey() + " must not have a built-in value");
            }
            assertFalse(entry.getValue().startsWith("eyJ"), entry.getKey() + " looks like a JWT");
        }
        assertTrue(credentials >= 15, "the credential keys are still recognised: " + credentials);
        assertTrue(ApprovalForumWebResourcesModule.isCredential("af.token"));
        assertTrue(ApprovalForumWebResourcesModule.isCredential("af.keycloakConfig.secret"));
        assertTrue(ApprovalForumWebResourcesModule.isCredential("af.graph.clientSecret"));
        assertTrue(ApprovalForumWebResourcesModule.isCredential("af.casdmrest.apikey"));
        assertFalse(ApprovalForumWebResourcesModule.isCredential("af.corp.credit.secretary.mail"));
        assertFalse(ApprovalForumWebResourcesModule.isCredential("af.base"));
    }

    @Test
    @DisplayName("No person's mailbox is a built-in test recipient")
    void defaultsCarryNoPersonalTestMailbox() throws Exception {
        assertEquals("", defaults().get("af.mail.test.emails"));
    }

    @Test
    @DisplayName("The cut-off job runs hourly so every committee's own cut-off day and time is honoured")
    void cutOffJobRunsHourly() throws Exception {
        assertEquals("0 5 * * * ?", defaults().get("af.scheduler.agenda.cutoff.cron.entry"));
        assertEquals("0 0 9 ? * MON-FRI", defaults().get("af.scheduler.agenda.reminder.cron.entry"));
    }

    @Test
    void tokenSubjectReadsTheSubjectClaimAndToleratesAnythingElse() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"jobs@rmb.co.za\",\"roles\":[\"CreditAnalyst\"]}".getBytes(StandardCharsets.UTF_8));
        assertEquals("jobs@rmb.co.za", ApprovalForumWebResourcesModule.tokenSubject("header." + payload + ".signature"));
        assertEquals("jobs@rmb.co.za", ApprovalForumWebResourcesModule.tokenSubject("  header." + payload + "  "));
        assertNull(ApprovalForumWebResourcesModule.tokenSubject(null));
        assertNull(ApprovalForumWebResourcesModule.tokenSubject(" "));
        assertNull(ApprovalForumWebResourcesModule.tokenSubject("not-a-token"));
        assertNull(ApprovalForumWebResourcesModule.tokenSubject("a.!!!.c"), "undecodable payload");
        String noSubject = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"iss\":\"x\"}".getBytes(StandardCharsets.UTF_8));
        assertNull(ApprovalForumWebResourcesModule.tokenSubject("h." + noSubject + ".s"));
    }
}
