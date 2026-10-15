package za.co.rmb.approvalforum.web.resources;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import za.co.rmb.approvalforum.web.module.ApprovalForumWebResourcesModule;
import za.co.rmb.approvalforum.web.quartz.SchedulerJobFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The scheduler registers the legacy jobs exactly, and the Spring job factory builds each of them.
 *
 * <p>{@code GET /scheduler/scheduler} (called by the container health check, as in legacy) starts Quartz and
 * schedules the jobs. The expectation below is the legacy ApprovalForumSchedulerResource: ten jobs (MailRequests
 * and ADUCleanup are commented out there), their names, groups, trigger names and af_settings cron keys, with the
 * legacy default cron expressions. A recording Scheduler stands in for Quartz so no job runs.</p>
 */
class SchedulerParityTest {

    /** job class, job group, trigger name, af_settings key, legacy default cron */
    private static final String[][] LEGACY_JOBS = {
            {"ProcessDailyApproverMailRequest", "ProcessDailyApproverMailRequestSchedulerGroup", "ProcessDailyApproverMailRequestTrigger", "af.scheduler.daily.mail.cron.entry", "0 0 8 ? * MON-FRI"},
            {"ProcessDailyCapturerMailRequest", "ProcessDailyCapturerMailRequestSchedulerGroup", "ProcessDailyCapturerMailRequestTrigger", "af.scheduler.daily.mail.cron.entry", "0 0 8 ? * MON-FRI"},
            {"ProcessResendDailyMailsRequest", "ProcessResendDailyMailsRequestSchedulerGroup", "ProcessResendDailyMailsRequestTrigger", "af.scheduler.daily.mails.unsent.cron.entry", "0 0 11,15,19 ? * MON-FRI"},
            {"ProcessExternalRequests", "ProcessExternalRequestSchedulerGroup", "ProcessExternalRequestsTrigger", "af.scheduler.cron.entry", "0 * 5-20 * * ?"},
            {"ProcessDocStashRequests", "ProcessDocStashRequestSchedulerGroup", "ProcessDocStashRequestsTrigger", "af.scheduler.cron.entry", "0 * 5-20 * * ?"},
            {"ProcessUnsentMailRequests", "ProcessUnsentMailRequestSchedulerGroup", "ProcessUnsentMailRequestsTrigger", "af.scheduler.cron.entry", "0 * 5-20 * * ?"},
            {"ProcessCleanupRequests", "ProcessCleanupRequestSchedulerGroup", "ProcessCleanupRequestsTrigger", "af.scheduler.cron.cleanup.entry", "0 0 5 * * ?"},
            {"ProcessEmailReceivedRequest", "ProcessEmailReceivedRequestSchedulerGroup", "ProcessEmailReceivedRequestTrigger", "af.scheduler.cron.email-received.entry", "* /5 * * * ?"},
            {"ProcessAgendaCutOffRequest", "ProcessAgendaCutOffRequestSchedulerGroup", "ProcessAgendaCutOffRequestTrigger", "af.scheduler.agenda.cutoff.cron.entry", "0 15 17 ? * THU"},
            {"ProcessAgendaReminderRequest", "ProcessAgendaReminderRequestSchedulerGroup", "ProcessAgendaReminderRequestTrigger", "af.scheduler.agenda.reminder.cron.entry", "0 0 9 ? * MON-FRI"},
    };
    private static final String QUARTZ_PACKAGE = "za.co.rmb.approvalforum.web.quartz.";

    @Test
    void schedulesTheLegacyJobs() throws Exception {
        Map<String, String> afSettings = new ApprovalForumWebResourcesModule().getLmsProperties();
        List<Object[]> scheduled = new ArrayList<>();
        Scheduler recording = (Scheduler) Proxy.newProxyInstance(Scheduler.class.getClassLoader(), new Class<?>[]{Scheduler.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isStarted" -> false;
                    case "scheduleJob" -> {
                        scheduled.add(args);
                        yield new Date();
                    }
                    default -> null;
                });
        ApprovalForumSchedulerResource resource = new ApprovalForumSchedulerResource(afSettings);
        Field scheduler = ApprovalForumSchedulerResource.class.getDeclaredField("scheduler");
        scheduler.setAccessible(true);
        scheduler.set(resource, recording);

        assertEquals(200, resource.startScheduler().getStatus());

        assertEquals(LEGACY_JOBS.length, scheduled.size());
        for (int i = 0; i < LEGACY_JOBS.length; i++) {
            String[] expected = LEGACY_JOBS[i];
            JobDetail job = (JobDetail) scheduled.get(i)[0];
            CronTrigger trigger = (CronTrigger) scheduled.get(i)[1];
            assertEquals(QUARTZ_PACKAGE + expected[0], job.getJobClass().getName());
            assertEquals(expected[0], job.getKey().getName());
            assertEquals(expected[1], job.getKey().getGroup());
            assertEquals(expected[2], trigger.getKey().getName());
            assertEquals(expected[1], trigger.getKey().getGroup());
            assertEquals(job.getKey(), trigger.getJobKey());
            assertEquals(expected[4], afSettings.get(expected[3]));
            assertEquals(expected[4], trigger.getCronExpression());
        }
    }

    @Test
    void jobFactoryBuildsEveryJobAsGuiceDid() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("af_settings", Map.class, () -> new ApprovalForumWebResourcesModule().getLmsProperties());
            context.registerBean(SchedulerJobFactory.class);
            context.refresh();
            SchedulerJobFactory factory = context.getBean(SchedulerJobFactory.class);
            for (String[] legacy : LEGACY_JOBS) {
                @SuppressWarnings("unchecked")
                Class<? extends Job> type = (Class<? extends Job>) Class.forName(QUARTZ_PACKAGE + legacy[0]);
                Job first = factory.newJob(bundle(type), null);
                assertNotNull(first);
                assertEquals(type, first.getClass());
                Field settings = type.getDeclaredField("afSettings");
                settings.setAccessible(true);
                assertNotNull(settings.get(first), legacy[0] + " has no af_settings");
                // the legacy jobs are @Singleton: Guice returned the same instance for every firing
                assertSame(first, factory.newJob(bundle(type), null));
            }
        }
    }

    private static TriggerFiredBundle bundle(Class<? extends Job> type) throws Exception {
        JobDetailImpl job = new JobDetailImpl();
        job.setName(type.getSimpleName());
        job.setJobClass(type);
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setName(type.getSimpleName() + "Trigger");
        trigger.setCronExpression("0 0 5 * * ?");
        return new TriggerFiredBundle(job, trigger, null, false, new Date(), new Date(), null, null);
    }
}
