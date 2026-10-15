package za.co.rmb.approvalforum.web.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.web.quartz.*;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Api(tags = "scheduler")
@Path("/scheduler/")
@org.springframework.stereotype.Component
@org.springframework.context.annotation.Scope("prototype")
public class ApprovalForumSchedulerResource {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumSchedulerResource.class);

    @Inject
    private Scheduler scheduler;

    private Map<String, String> afSettings;

    @Inject
    public ApprovalForumSchedulerResource(@Named("af_settings") Map<String, String> afSettings){
        this.afSettings = afSettings;
    }

    @GET
    @Path("/scheduler")
    @Produces("application/json")
    @ApiOperation(value = "Initialises the scheduler", authorizations = @Authorization(value = "Bearer"))
    public Response startScheduler() {

        try {
            logger.info("Scheduler started:{}",scheduler.isStarted());

            if (!scheduler.isStarted()) {
                logger.info("startScheduler()");
                scheduler.start();

                logger.info("SCHEDULER STARTED");

                JobDetail processDailyApproverMailRequestJob = JobBuilder.newJob(ProcessDailyApproverMailRequest.class)
                        .withIdentity("ProcessDailyApproverMailRequest", "ProcessDailyApproverMailRequestSchedulerGroup").build();

                CronTrigger processDailyApproverMailRequestCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessDailyApproverMailRequestTrigger", "ProcessDailyApproverMailRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.daily.mail.cron.entry")))
                        .forJob("ProcessDailyApproverMailRequest", "ProcessDailyApproverMailRequestSchedulerGroup").build();

                scheduler.scheduleJob(processDailyApproverMailRequestJob, processDailyApproverMailRequestCronTrigger);
                logger.info("Daily Approver Escalation Emails Cron Scheduled");

                JobDetail processDailyCapturerMailRequestJob = JobBuilder.newJob(ProcessDailyCapturerMailRequest.class)
                        .withIdentity("ProcessDailyCapturerMailRequest", "ProcessDailyCapturerMailRequestSchedulerGroup").build();

                CronTrigger processDailyCapturerMailRequestCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessDailyCapturerMailRequestTrigger", "ProcessDailyCapturerMailRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.daily.mail.cron.entry")))
                        .forJob("ProcessDailyCapturerMailRequest", "ProcessDailyCapturerMailRequestSchedulerGroup").build();

                scheduler.scheduleJob(processDailyCapturerMailRequestJob, processDailyCapturerMailRequestCronTrigger);
                logger.info("Daily Capturer Escalation Emails Cron Scheduled");

                JobDetail processResendDailyMailsRequestJob = JobBuilder.newJob(ProcessResendDailyMailsRequest.class)
                        .withIdentity("ProcessResendDailyMailsRequest", "ProcessResendDailyMailsRequestSchedulerGroup").build();

                CronTrigger processResendDailyMailsRequestCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessResendDailyMailsRequestTrigger", "ProcessResendDailyMailsRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.daily.mails.unsent.cron.entry")))
                        .forJob("ProcessResendDailyMailsRequest", "ProcessResendDailyMailsRequestSchedulerGroup").build();

                scheduler.scheduleJob(processResendDailyMailsRequestJob, processResendDailyMailsRequestCronTrigger);
                logger.info("Daily Unsent Emails Cron Scheduled");

                JobDetail processExternalRequestsJob = JobBuilder.newJob(ProcessExternalRequests.class)
                        .withIdentity("ProcessExternalRequests", "ProcessExternalRequestSchedulerGroup").build();

                CronTrigger processExternalRequestsCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessExternalRequestsTrigger", "ProcessExternalRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.cron.entry")))
                        .forJob("ProcessExternalRequests", "ProcessExternalRequestSchedulerGroup").build();

                scheduler.scheduleJob(processExternalRequestsJob, processExternalRequestsCronTrigger);

                JobDetail processDocStashRequestsJob = JobBuilder.newJob(ProcessDocStashRequests.class)
                        .withIdentity("ProcessDocStashRequests", "ProcessDocStashRequestSchedulerGroup").build();

                CronTrigger processDocStashRequestsCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessDocStashRequestsTrigger", "ProcessDocStashRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.cron.entry")))
                        .forJob("ProcessDocStashRequests", "ProcessDocStashRequestSchedulerGroup").build();

                scheduler.scheduleJob(processDocStashRequestsJob, processDocStashRequestsCronTrigger);

//                JobDetail processMailRequestsJob = JobBuilder.newJob(ProcessMailRequests.class)
//                        .withIdentity("ProcessMailRequests", "ProcessMailRequestSchedulerGroup").build();
//
//                CronTrigger processMailRequestsCronTrigger = TriggerBuilder.newTrigger()
//                        .withIdentity("ProcessMailRequestsTrigger", "ProcessMailRequestSchedulerGroup")
//                        .withSchedule(CronScheduleBuilder
//                                .cronSchedule(afSettings.get("af.scheduler.cron.entry")))
//                        .forJob("ProcessMailRequests", "ProcessMailRequestSchedulerGroup").build();
//
//                scheduler.scheduleJob(processMailRequestsJob, processMailRequestsCronTrigger);
//
                JobDetail processUnsentMailRequestsJob = JobBuilder.newJob(ProcessUnsentMailRequests.class)
                        .withIdentity("ProcessUnsentMailRequests", "ProcessUnsentMailRequestSchedulerGroup").build();

                CronTrigger processUnsentMailRequestsCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessUnsentMailRequestsTrigger", "ProcessUnsentMailRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.cron.entry")))
                        .forJob("ProcessUnsentMailRequests", "ProcessUnsentMailRequestSchedulerGroup").build();

                scheduler.scheduleJob(processUnsentMailRequestsJob, processUnsentMailRequestsCronTrigger);

                JobDetail processCleanupRequestsJob = JobBuilder.newJob(ProcessCleanupRequests.class)
                        .withIdentity("ProcessCleanupRequests", "ProcessCleanupRequestSchedulerGroup").build();

                CronTrigger processCleanupRequestsCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessCleanupRequestsTrigger", "ProcessCleanupRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.cron.cleanup.entry")))
                        .forJob("ProcessCleanupRequests", "ProcessCleanupRequestSchedulerGroup").build();

                scheduler.scheduleJob(processCleanupRequestsJob, processCleanupRequestsCronTrigger);

                //cleanup job for ActiveDirectoryUser
                /*JobDetail processADUCleanupRequestsJob = JobBuilder.newJob(ProcessADUCleanupRequests.class)
                        .withIdentity("ProcessADUCleanupRequests", "ProcessADUCleanupRequestSchedulerGroup").build();

                CronTrigger processADUCleanupRequestsCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessADUCleanupRequestsTrigger", "ProcessADUCleanupRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.cron.adu.cleanup.entry")))
                        .forJob("ProcessADUCleanupRequests", "ProcessADUCleanupRequestSchedulerGroup").build();

                scheduler.scheduleJob(processADUCleanupRequestsJob, processADUCleanupRequestsCronTrigger);*/

                //Process Received Emails
                logger.info("Received Emails Cron Scheduled");
                JobDetail processEmailReceivedRequestJob = JobBuilder.newJob(ProcessEmailReceivedRequest.class)
                        .withIdentity("ProcessEmailReceivedRequest", "ProcessEmailReceivedRequestSchedulerGroup").build();

                CronTrigger processEmailReceivedRequestCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessEmailReceivedRequestTrigger", "ProcessEmailReceivedRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.cron.email-received.entry")))
                        .forJob("ProcessEmailReceivedRequest", "ProcessEmailReceivedRequestSchedulerGroup").build();

                scheduler.scheduleJob(processEmailReceivedRequestJob, processEmailReceivedRequestCronTrigger);

                //Process Agenda Cut-Off (auto-remove unconfirmed agenda items)
                JobDetail processAgendaCutOffRequestJob = JobBuilder.newJob(ProcessAgendaCutOffRequest.class)
                        .withIdentity("ProcessAgendaCutOffRequest", "ProcessAgendaCutOffRequestSchedulerGroup").build();

                CronTrigger processAgendaCutOffRequestCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessAgendaCutOffRequestTrigger", "ProcessAgendaCutOffRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.agenda.cutoff.cron.entry")))
                        .forJob("ProcessAgendaCutOffRequest", "ProcessAgendaCutOffRequestSchedulerGroup").build();

                scheduler.scheduleJob(processAgendaCutOffRequestJob, processAgendaCutOffRequestCronTrigger);
                logger.info("Agenda Cut-Off Cron Scheduled");

                //Process Agenda Reminders (upload reminders at T-2 and T-1 before cut-off)
                JobDetail processAgendaReminderRequestJob = JobBuilder.newJob(ProcessAgendaReminderRequest.class)
                        .withIdentity("ProcessAgendaReminderRequest", "ProcessAgendaReminderRequestSchedulerGroup").build();

                CronTrigger processAgendaReminderRequestCronTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("ProcessAgendaReminderRequestTrigger", "ProcessAgendaReminderRequestSchedulerGroup")
                        .withSchedule(CronScheduleBuilder
                                .cronSchedule(afSettings.get("af.scheduler.agenda.reminder.cron.entry")))
                        .forJob("ProcessAgendaReminderRequest", "ProcessAgendaReminderRequestSchedulerGroup").build();

                scheduler.scheduleJob(processAgendaReminderRequestJob, processAgendaReminderRequestCronTrigger);
                logger.info("Agenda Reminder Cron Scheduled");

            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return Response.ok().build();
    }

}
