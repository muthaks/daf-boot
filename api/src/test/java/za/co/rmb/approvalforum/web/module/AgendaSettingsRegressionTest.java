package za.co.rmb.approvalforum.web.module;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.quartz.CronExpression;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** G17/G21: never print credential values, contact services, or change this test JVM's environment. */
class AgendaSettingsRegressionTest {
    @SuppressWarnings("unchecked")
    private static Map<String, String> defaults() throws Exception {
        Field field = ApprovalForumWebResourcesModule.class.getDeclaredField("DEFAULTS");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }

    @Test
    void G17_serviceCredentialsMustNotHaveEmbeddedNonemptyDefaults() throws Exception {
        Map<String, String> settings = defaults();
        List<String> keys = settings.keySet().stream()
                .filter(key -> key.toLowerCase(Locale.ROOT).matches(".*(?:token|secret|password|apikey)$"))
                .sorted().toList();
        assertFalse(keys.isEmpty(), "Credential keys must be inspected, not silently omitted");
        assertAll(keys.stream().map(key -> () -> assertTrue(settings.get(key) == null || settings.get(key).isBlank(),
                "G17 credential default must be empty and supplied externally: " + key)));
    }

    @Test
    void G17_testRecipientsMustBeConfiguredExplicitlyRatherThanDefaultingToAPerson() throws Exception {
        String recipient = defaults().get("af.mail.test.emails");
        assertTrue(recipient == null || recipient.isBlank(), "G17 configure a test mailbox externally; do not embed a person's address");
    }

    @TestFactory
    Stream<DynamicTest> G21_cutoffProcessingMustNotWaitSeveralDaysForANonThursdayDeadline() {
        return Stream.of("2026-11-02T10:01:00Z", "2026-11-03T10:01:00Z", "2026-11-04T10:01:00Z",
                "2026-11-05T10:01:00Z", "2026-11-06T10:01:00Z").map(value ->
                DynamicTest.dynamicTest("G21 deadline on " + value.substring(0, 10), () -> {
                    CronExpression cron = new CronExpression(defaults().get("af.scheduler.agenda.cutoff.cron.entry"));
                    cron.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));
                    Instant deadline = Instant.parse(value);
                    Date next = cron.getNextValidTimeAfter(Date.from(deadline));
                    assertNotNull(next);
                    assertFalse(next.toInstant().isAfter(deadline.plusSeconds(3600)),
                            "G21 every weekday deadline needs a cleanup run within an hour, not next Thursday");
                }));
    }

    @Test
    void G21_cutoffAndReminderSchedulesCanBeOverriddenIndependentlyThroughTheEnvironment() throws Exception {
        String cutoff = "0 0/20 * * * ?", reminder = "0 30 8 ? * MON-FRI";
        Map<String, String> configured = probe(cutoff, reminder);
        assertEquals(cutoff, configured.get("CUTOFF"));
        assertEquals(reminder, configured.get("REMINDER"));
    }

    @Test
    void G21_blankEnvironmentSettingsKeepValidDefaults() throws Exception {
        Map<String, String> configured = probe(" ", "");
        assertEquals(defaults().get("af.scheduler.agenda.cutoff.cron.entry"), configured.get("CUTOFF"));
        assertEquals(defaults().get("af.scheduler.agenda.reminder.cron.entry"), configured.get("REMINDER"));
    }

    private static Map<String, String> probe(String cutoff, String reminder) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ProcessBuilder command = new ProcessBuilder(java, "-cp", classpath, EnvironmentProbe.class.getName());
        // Do not inherit real application credentials or mutate global environment state.
        command.environment().keySet().removeIf(key -> key.startsWith("AF_"));
        command.environment().put("AF_SCHEDULER_AGENDA_CUTOFF_CRON", cutoff);
        command.environment().put("AF_SCHEDULER_AGENDA_REMINDER_CRON", reminder);
        Process process = command.redirectErrorStream(true).start();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Scheduler environment probe timed out; process output omitted");
        }
        assertEquals(0, process.exitValue(), "Scheduler environment probe failed; process output omitted");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> result = new HashMap<>();
        output.lines().filter(line -> line.startsWith("CUTOFF=") || line.startsWith("REMINDER="))
                .forEach(line -> { int equals = line.indexOf('='); result.put(line.substring(0, equals), line.substring(equals + 1)); });
        return result;
    }

    public static class EnvironmentProbe {
        public static void main(String[] args) {
            Map<String, String> settings = new ApprovalForumWebResourcesModule().getLmsProperties();
            System.out.println("CUTOFF=" + settings.get("af.scheduler.agenda.cutoff.cron.entry"));
            System.out.println("REMINDER=" + settings.get("af.scheduler.agenda.reminder.cron.entry"));
        }
    }
}
