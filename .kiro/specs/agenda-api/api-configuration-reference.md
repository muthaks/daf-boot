# Agenda API configuration reference

This is a generated backend contract reference for the Agenda Builder API in daf-boot. Literal declarations below are part of the specification; requirements.md and design.md explain them. It is not evidence of a live service, environment or database.

Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. No API, directory or database is accessed.

Source: `api/src/main/java/za/co/rmb/approvalforum/web/module/ApprovalForumWebResourcesModule.java`. SHA-256: `7712a5d280a873159a712a3bbf77315c1db4ebc98410a552da0c041d3a75df53`.

Settings reach the code through the `af_settings` map: a built-in default, replaced by the environment variable where one is mapped. Values of credentials are never reproduced here.

| Setting | Default | Environment variable | Read by |
| --- | --- | --- | --- |
| `af.agenda.application.statuses` | blank | `AF_AGENDA_APPLICATION_STATUSES` | ApprovalForumAgendaResource.getAgendaCounterparties |
| `af.agenda.oversight.roles` | `Secretary;Chairperson;Approver;ReviewAnalyst` | `AF_AGENDA_OVERSIGHT_ROLES` | ApprovalForumAgendaResource.oversightRoles |
| `af.agenda.reviewer.role` | `ReviewAnalyst` | none | ApprovalForumAgendaResource.reviewerRole |
| `af.agenda.ui.base` | `https://crsk-daf-dev.rmb.co.za` | `AF_AGENDA_UI_BASE` | AgendaMailServiceImpl.dafLink, GraphMeetingClient.agendaLink, GraphMeetingClient.createOrUpdateEvent |
| `af.base` | `http://localhost:8197/approvalforum/api` | `AF_BASE` | ProcessAgendaCutOffRequest.execute, ProcessAgendaReminderRequest.execute |
| `af.graph.clientId` | blank | `AF_GRAPH_CLIENT_ID` | GraphMeetingClient.createOrUpdateEvent, GraphMeetingClient.getToken, GraphMeetingClient.isConfigured |
| `af.graph.clientSecret` | blank | `AF_GRAPH_CLIENT_SECRET` | GraphMeetingClient.getToken, GraphMeetingClient.isConfigured |
| `af.graph.organizer` | blank | `AF_GRAPH_ORGANIZER` | GraphMeetingClient.cancelEvent, GraphMeetingClient.createOrUpdateEvent, GraphMeetingClient.isConfigured |
| `af.graph.proxy.host` | blank | `AF_GRAPH_PROXY_HOST` | GraphMeetingClient.newClient |
| `af.graph.proxy.port` | `8080` | `AF_GRAPH_PROXY_PORT` | GraphMeetingClient.newClient |
| `af.graph.tenantId` | blank | `AF_GRAPH_TENANT_ID` | GraphMeetingClient.getToken, GraphMeetingClient.isConfigured |
| `af.mail.enabled` | `true` | `AF_MAIL_ENABLED` | AgendaMailServiceImpl.send |
| `af.mail.test.emails` | blank | `AF_MAIL_TEST_EMAILS` | AgendaMailServiceImpl.send |
| `af.mail_environment` | `DEV` | `AF_MAIL_ENV` | AgendaMailServiceImpl.send |
| `af.mailservice.token` | blank | `AF_MAILSERVICE_TOKEN` | AgendaMailServiceImpl.send |
| `af.scheduler.agenda.cutoff.cron.entry` | `0 5 * * * ?` | `AF_SCHEDULER_AGENDA_CUTOFF_CRON` | ApprovalForumSchedulerResource.startScheduler |
| `af.scheduler.agenda.reminder.cron.entry` | `0 0 9 ? * MON-FRI` | `AF_SCHEDULER_AGENDA_REMINDER_CRON` | ApprovalForumSchedulerResource.startScheduler |
| `af.scheduler.user` | not set | none | ApprovalForumAgendaResource.forbiddenUnlessScheduler |
| `af.token` | blank | `AF_TOKEN` | ProcessAgendaCutOffRequest.getClient, ProcessAgendaReminderRequest.getClient |

## Scheduled jobs

Source: `api/src/main/java/za/co/rmb/approvalforum/web/resources/ApprovalForumSchedulerResource.java`. SHA-256: `c07664057f20338162044406ab0d9e3cf1e263cbea5624e843bed7a1e13c8257`.

```java
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
```

Source: `api/src/main/java/za/co/rmb/approvalforum/web/quartz/ProcessAgendaCutOffRequest.java`. SHA-256: `6ed5898d12d985c5837bc3dfea49d2b55460f317f81b935a1260dc89863eb8c7`.

```java
@Singleton
@DisallowConcurrentExecution
public class ProcessAgendaCutOffRequest implements Job {

    private Map<String, String> afSettings;

    @Inject
    public ProcessAgendaCutOffRequest(@Named("af_settings") Map<String, String> afSettings){
        this.afSettings = afSettings;
    }

    public void execute(JobExecutionContext jobContext) throws JobExecutionException{
        Client client = getClient();
        try {
            client.target(afSettings.get("af.base") + "/approvalForum/agenda/processCutOff")
                    .request()
                    .post(Entity.text(""));
        } catch (Exception ex) {
            throw new JobExecutionException(ex);
        }
        finally {
            client.close();
        }
    }

    private Client getClient(){
        Client client = ClientBuilder.newClient();
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.token"));
        client.register(feature);
        return client;
    }

}
```

Source: `api/src/main/java/za/co/rmb/approvalforum/web/quartz/ProcessAgendaReminderRequest.java`. SHA-256: `73866dd2439af775a719df3950fcf02e6bce3edd67978d93ffa822dd1fc0feda`.

```java
@Singleton
@DisallowConcurrentExecution
public class ProcessAgendaReminderRequest implements Job {

    private Map<String, String> afSettings;

    @Inject
    public ProcessAgendaReminderRequest(@Named("af_settings") Map<String, String> afSettings){
        this.afSettings = afSettings;
    }

    public void execute(JobExecutionContext jobContext) throws JobExecutionException{
        Client client = getClient();
        try {
            client.target(afSettings.get("af.base") + "/approvalForum/agenda/processReminders")
                    .request()
                    .post(Entity.text(""));
        } catch (Exception ex) {
            throw new JobExecutionException(ex);
        }
        finally {
            client.close();
        }
    }

    private Client getClient(){
        Client client = ClientBuilder.newClient();
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.token"));
        client.register(feature);
        return client;
    }

}
```
