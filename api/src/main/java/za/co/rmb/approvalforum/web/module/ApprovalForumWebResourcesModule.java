package za.co.rmb.approvalforum.web.module;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import za.co.rmb.approvalforum.security.filter.AuthenticationFilter;
import za.co.rmb.approvalforum.web.resources.*;
import za.co.rmb.approvalforum.web.auth.AuthServiceBootClient;
import za.co.rmb.approvalforum.web.auth.CreditDataTokenFilter;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;

public class ApprovalForumWebResourcesModule {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumWebResourcesModule.class);

    private static final Map<String, String> DEFAULTS = ImmutableMap.<String, String>builder()
            .put("af.authservice.base", "https://rmb-rbvdvcrsk01.fnb.co.za/authservice-1.1.0")
            .put("af.base", "http://localhost:8197/approvalforum/api")
            .put("af.mail.base", "https://crsk-daf-dev.rmb.co.za")
            .put("af.token",
                    "")
            .put("af.environment", "DEV").put("af.devusers", "jvf,kir,kea,jjx,jeb,axu,shc,ffa,ixm,dvo")
            .put("af.adgroup.ou", "OU=DAF,OU=Credit,OU=Applications,OU=Rand Merchant Bank,DC=rmb,DC=co,DC=za")
            .put("af.keycloakConfig.url", "https://rmb-rbvdvcrsk01.fnb.co.za/auth")
            .put("af.keycloakConfig.realm", "master").put("af.keycloakConfig.clientId", "lms")
            .put("af.keycloakConfig.secret", "")
            .put("af.applicationservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/application-2.6.0")
            .put("af.applicationservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/application-2.7.0")
            .put("af.applicationservice.token",
                    "")
            .put("af.counterpartyservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/counterparty-2.5.0")
            .put("af.counterpartyservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/counterparty-2.7.0")
            .put("af.counterpartyservice.token",
                    "")
            .put("af.refdataservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/referencedata-2.5.0")
            .put("af.refdataservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/referencedata-2.5.1")
            .put("af.refdataservice.token",
                    "")
            .put("af.facilityservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/facility-2.5.0")
            .put("af.facilityservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/facility-2.6.0")
            .put("af.facilityservice.token",
                    "")
            .put("af.lumosservice.base", "https://lumos-apisix-dev.apps.dev-01-rb.ocp.fnb.co.za")
            .put("af.lumosservice.orchestration.token",
                    "")
            .put("af.lumosservice.workflow.token",
                    "")
            .put("af.rulesservice.base", "http://rmb-axu1-w:8197/approvalforumdrools/api/")
            .put("af.listmanagementservice.base", "http://rmb-axu1-w:8197/listmanagement/api")
            .put("af.listmanagementservice.token","")
            .put("af.scheduler.cron.entry", "0 * 5-20 * * ?")
            .put("af.scheduler.daily.mail.cron.entry", "0 0 8 ? * MON-FRI")
            .put("af.scheduler.daily.mails.unsent.cron.entry", "0 0 11,15,19 ? * MON-FRI")
            .put("af.scheduler.cron.cleanup.entry", "0 0 5 * * ?")
//            .put("af.scheduler.cron.cleanup.adu.entry", "0 0 5 * * ?")
            // Hourly: each committee has its own cut-off day and time, and the job is idempotent.
            .put("af.scheduler.agenda.cutoff.cron.entry", "0 5 * * * ?")
            .put("af.scheduler.agenda.reminder.cron.entry", "0 0 9 ? * MON-FRI")
            .put("af.agenda.reviewer.role", "ReviewAnalyst")
            // DAF roles (from the AD groups mapped in getAFGroups) that may check agenda items
            // booked by another analyst, read-only; semicolon or comma separated, per environment.
            .put("af.agenda.oversight.roles", "Secretary;Chairperson;Approver;ReviewAnalyst")
            .put("af.agenda.ui.base", "https://crsk-daf-dev.rmb.co.za")
            .put("af.casdmrest.base", "https://casdm-dev/rest")
            .put("af.casdmrest.apikey", "")
            // Dev CASDM presents the ashburton cert on the casdm-dev route; accept those SANs
            // explicitly (chain still validated). Blank this in PROD via env once certs are right.
            .put("af.casdmrest.acceptedCertNames", "casdm-ash-dev,casdm-ash-dev.rmb.co.za")
            // Agenda booking counterparty list statuses; blank = ALL active application statuses
            // (see ApprovalForumAgendaResource.AGENDA_DEFAULT_STATUSES). Set to narrow the agenda
            // list only - capture keeps using af.application.statuses regardless.
            .put("af.agenda.application.statuses", "")
            // Microsoft Graph (Teams/Outlook meeting creation on agenda lock - BR-013). Blank = disabled;
            // the Entra app registration needs application permission Calendars.ReadWrite.
            .put("af.graph.tenantId", "")
            .put("af.graph.clientId", "")
            .put("af.graph.clientSecret", "")
            .put("af.graph.organizer", "")
            .put("af.graph.proxy.host", "")
            .put("af.graph.proxy.port", "8080")
            .put("af.scheduler.cron.email-received.entry", "* /5 * * * ?")
            .put("af.docstashservice.token", "")
            .put("af.docstashservice.base", "https://rmb-rbvdvcrsk01.fnb.co.za:9080/docstash/api")
            .put("af.sso.servers", "http://rmb-rbvtsrska01:8080/auth/,http://rmb-rbvtsrskd01/auth/,http://rmb-rbvutrska01:8080/auth/,http://rmb-rbvutrskd01/auth/")
            .put("af.mailservice.token", "")
            .put("af.lumos.mailservice.token","")
            .put("af.mailservice.base", "https://rmb-rbvdvcrsk01.fnb.co.za:8280/mailservice")
            .put("af.lumos.mailservice.base", "https://rmb-rbvdvcrsk02.fnb.co.za/lumos-mailservice")
            .put("af.lumos.mailservice.read.limit", "10")
            .put("af.mailservice.test.dl", "DLRMBBTCreditAnalytics@rmb.co.za")
            .put("af.mailservice.approval.mailto", "approvalForum_dev@rmb.co.za")
            .put("af.corp.credit.secretary.mail", "DLRMBBTCreditAnalytics@rmb.co.za")
            .put("af.corp.credit.specialist.mail", "DLRMBBTCreditAnalytics@rmb.co.za")
            .put("af.firstescalation.hours", "48")
            .put("af.secondescalation.hours", "72")
            .put("af.mail.attachment.zip", "false")
            .put("af.mail.enabled", "true")
            .put("af.mail.test.emails", "")
            .put("af.mail.attachment.limit", "10000000")
            .put("af.application.statuses", "Submitted for Approval")
            .put("af.facility.statuses", "Approved;Submitted for Approval")
            .put("af.socketio_url", "http://localhost:3000")
            .put("af.socketio.token", "")
            .put("af.docgenservice.token", "")
            .put("af.docgenservice.base", "")
            .put("af.docstash.base", "https://docstash-dev.rmb.co.za:9080/docstash-ui/")
            .put("af.mail_environment", "DEV")
            .put("af.mailservice.reviewanalyst.test", "review_analyst_test@rmb.co.za")
            .put("af.support.mail.dl", "DLRMBBTCreditAnalytics@rmb.co.za")
            .put("af.holidayservice.url", "https://rmb-rbvdvcrsk02.fnb.co.za/holiday-service/api/")
            .put("af.rulesservice.token", "")
            .put("af.lumosservice.refdata.token",
                    "")
            .put("af.lumosservice.partymodel.token",
                    "")
            .put("af.lumosservice.lumosworkflow.token",
                    "")
            .put("af.lumosservice.lumosworkflow.decision.token",
                    "")
            .put("af.lumosservice.lumosworkflow.client.token",
            "")
            .build();
    @Singleton
    @Named("af_settings")
    public Map<String, String> getLmsProperties() {

        Map<String, String> props = new HashMap<>(DEFAULTS);

        try {
            // Get environment variables
            String authServiceBase = env("AF_AUTHSERVICE_BASE");
            String afBase = env("AF_BASE");
            String afMailBase = env("AF_MAIL_BASE");
            String afMailEnabled = env("AF_MAIL_ENABLED");
            String afMailTestEmails = env("AF_MAIL_TEST_EMAILS");
            String afToken = env("AF_TOKEN");
            String environment = env("AF_ENV");
            String devUsers = env("AF_DEV_USERS");
            String afAdroleOu = env("AF_ADROLE_OU");
            String afKeycloakConfigUrl = env("AF_keycloakConfig_URL");
            String afKeycloakConfigRealm = env("AF_keycloakConfig_REALM");
            String afKeycloakConfigClientId = env("AF_keycloakConfig_ClientId");
            String afKeycloakConfigSecret = env("AF_keycloakConfig_Secret");
            String rulesServiceBase = env("AF_RULESSERVICE_BASE");
            String rulesServiceToken = env("AF_RULESSERVICE_TOKEN");
            String listManagementServiceBase = env("AF_LISTMANAGEMENTSERVICE_BASE");
            String listManagementServiceToken = env("AF_LISTMANAGEMENTSERVICE_TOKEN");

            String mailServiceBase = env("AF_MAILSERVICE_BASE");
            String lumosMailServiceBase = env("AF_LUMOS_MAILSERVICE_BASE");
            String mailServiceToken = env("AF_MAILSERVICE_TOKEN");
            String lumosMailServiceToken = env("AF_LUMOS_MAILSERVICE_TOKEN");
            String lumosMailServiceReadLimit = env("AF_LUMOS_MAILSERVICE_READ_LIMIT");

            String mailServiceTestDl = env("AF_MAILSERVICE_TEST_DL");
            String mailServiceApprovalMailto = env("AF_MAILSERVICE_APPROVAL_MAILTO");

            String applicationServiceBase = env("AF_APPLICATIONSERVICE_BASE");
            String applicationServiceToken = env("AF_APPLICATIONSERVICE_TOKEN");

            String ashApplicationServiceBase = env("AF_APPLICATIONSERVICE_ASH_BASE");

            String counterpartyServiceBase = env("AF_COUNTERPARTYSERVICE_BASE");
            String counterpartyServiceToken = env("AF_COUNTERPARTYSERVICE_TOKEN");

            String ashCounterpartyServiceBase = env("AF_COUNTERPARTYSERVICE_ASH_BASE");

            String facilityServiceBase = env("AF_FACILITYSERVICE_BASE");
            String facilityServiceToken = env("AF_FACILITYSERVICE_TOKEN");

            String ashFacilityServiceBase = env("AF_FACILITYSERVICE_ASH_BASE");

            String refDataServiceBase = env("AF_REFDATASERVICE_BASE");
            String refDataServiceToken = env("AF_REFDATASERVICE_TOKEN");

            String ashRefDataServiceBase = env("AF_REFDATASERVICE_ASH_BASE");

            String lumosServiceBase = env("AF_LUMOSSERVICE_BASE");
            String lumosServiceOrchestrationToken = env("AF_LUMOSSERVICE_ORCHESTRATION_TOKEN");
            String lumosServiceWorkflowToken = env("AF_LUMOSSERVICE_WORKFLOW_TOKEN");

            String schedulerCron = env("AF_SCHEDULER_CRON");
            String schedulerCleanupCron = env("AF_SCHDULER_CLEANUP_CRON");
//            String schedulerCleanupAduCron = env("AF_SCHDULER_ADU_CLEANUP_CRON");
            String schedulerEmailReceivedCron = env("AF_SCHEDULER_EMAIL_RECEIVED_CRON");

            String schedulerDailyEmailCron = env("AF_SCHEDULER_DAILY_EMAIL_CRON");
            String schedulerDailyUnsentEmailsCron = env("AF_SCHEDULER_DAILY_UNSENT_EMAILS_CRON");

            String docStashServiceBase = env("AF_DOCSTASHSERVICE_BASE");
            String docStashServiceToken = env("AF_DOCSTASHSERVICE_TOKEN");

            String af_sso_servers = env("AF_SSO_SERVERS");

            String afCorpCreditSecretaryMail = env("AF_CORP_CREDIT_SECRETARY_MAIL");
            String afCorpCreditSpecialistMail = env("AF_CORP_CREDIT_SPECIALIST_MAIL");

            String afFirstEscalationHours = env("AF_FIRST_ESCALATION_HOURS");
            String afSecondEscalationHours = env("AF_SECOND_ESCALATION_HOURS");

            String afMailAttachmentZip = env("AF_MAIL_ATTACHMENT_ZIP");
            String afMailAttachmentLimit = env("AF_MAIL_ATTACHMENT_LIMIT");

            String afApplicationStatuses = env("AF_APPLICATION_STATUSES");

            String afFacilityStatuses = env("AF_FACILITY_STATUSES");

            String afSocketIoUrl = env("AF_SOCKETIO_URL");
            String afSocketIoToken = env("AF_SOCKETIO_TOKEN");

            String afDocGenServiceBase = env("AF_DOCGENSERVICE_BASE");
            String afDocGenToken = env("AF_DOCGENSERVICE_TOKEN");

            String afDocstashBase = env("AF_DOCSTASH_BASE");

            String mail_environment = env("AF_MAIL_ENV");

            String reviewAnalystTestMail = env("AF_MAILSERVICE_REVIEW_ANALYST_MAIL_TEST");

            String afSupportMailDl = env("AF_SUPPORT_MAIL_DL");

            String afHolidayServiceUrl = env("AF_HOLIDAYSERVICE_URL");

            String lumosServiceRefdataToken = env("AF_LUMOSSERVICE_REFDATA_TOKEN");

            String lumosServicePartymodelToken = env("AF_LUMOSSERVICE_PARTYMODEL_TOKEN");

            String lumosServiceLumosWorkflowToken = env("AF_LUMOSSERVICE_LUMOSWORKFLOW_TOKEN");

            String lumosServiceLumosWorkflowDecisionToken = env("AF_LUMOSSERVICE_WORKFLOWDECISION_TOKEN");

            //af.lumosservice.lumosapproval.token
            String lumosServiceLumosWorkflowApprovalToken = env("AF_LUMOSSERVICE_WORKFLOWAPPROVAL_TOKEN");

            String casdmRestBase = env("AF_CASDMREST_BASE");
            String casdmRestApiKey = env("AF_CASDMREST_APIKEY");
            String casdmRestAcceptedCertNames = env("AF_CASDMREST_ACCEPTED_CERT_NAMES");
            String agendaUiBase = env("AF_AGENDA_UI_BASE");
            String agendaApplicationStatuses = env("AF_AGENDA_APPLICATION_STATUSES");
            String agendaOversightRoles = env("AF_AGENDA_OVERSIGHT_ROLES");

            // Add environment variables to system properties
            if (casdmRestBase != null)
                setProps(props, "af.casdmrest.base", casdmRestBase);
            if (casdmRestApiKey != null)
                setProps(props, "af.casdmrest.apikey", casdmRestApiKey);
            if (casdmRestAcceptedCertNames != null)
                setProps(props, "af.casdmrest.acceptedCertNames", casdmRestAcceptedCertNames);
            if (agendaApplicationStatuses != null)
                setProps(props, "af.agenda.application.statuses", agendaApplicationStatuses);
            if (agendaUiBase != null)
                setProps(props, "af.agenda.ui.base", agendaUiBase);
            String agendaCutOffCron = env("AF_SCHEDULER_AGENDA_CUTOFF_CRON");
            String agendaReminderCron = env("AF_SCHEDULER_AGENDA_REMINDER_CRON");
            if (agendaCutOffCron != null) setProps(props, "af.scheduler.agenda.cutoff.cron.entry", agendaCutOffCron);
            if (agendaReminderCron != null) setProps(props, "af.scheduler.agenda.reminder.cron.entry", agendaReminderCron);
            if (agendaOversightRoles != null)
                setProps(props, "af.agenda.oversight.roles", agendaOversightRoles);
            if (authServiceBase != null)
                setProps(props, "af.authservice.base", authServiceBase);

            if (afBase != null)
                setProps(props, "af.base", afBase);

            if (afMailEnabled != null)
                setProps(props, "af.mail.enabled", afMailEnabled);

            if(afMailTestEmails != null)
                setProps(props, "af.mail.test.emails", afMailTestEmails);

            if (afMailBase != null)
                setProps(props, "af.mail.base", afMailBase);

            if (afToken != null)
                setProps(props, "af.token", afToken);

            if (environment != null)
                setProps(props, "af.environment", environment);

            if (mail_environment != null)
                setProps(props, "af.mail_environment", mail_environment);

            if (devUsers != null)
                setProps(props, "af.devusers", devUsers);

            if (afAdroleOu != null)
                setProps(props, "af.adgroup.ou", afAdroleOu);

            if (afKeycloakConfigUrl != null)
                setProps(props, "af.keycloakConfig.url", afKeycloakConfigUrl);
            if (afKeycloakConfigRealm != null)
                setProps(props, "af.keycloakConfig.realm", afKeycloakConfigRealm);
            if (afKeycloakConfigClientId != null)
                setProps(props, "af.keycloakConfig.clientid", afKeycloakConfigClientId);
            if (afKeycloakConfigSecret != null)
                setProps(props, "af.keycloakConfig.secret", afKeycloakConfigSecret);

            if (rulesServiceBase != null)
                setProps(props, "af.rulesservice.base", rulesServiceBase);

            if(rulesServiceToken != null)
                setProps(props, "af.rulesservice.token", rulesServiceToken);

            if (counterpartyServiceBase != null)
                setProps(props, "af.counterpartyservice.base", counterpartyServiceBase);

            if (ashCounterpartyServiceBase != null)
                setProps(props, "af.counterpartyservice.ash.base", ashCounterpartyServiceBase);

            if (counterpartyServiceToken != null)
                setProps(props, "af.counterpartyservice.token", counterpartyServiceToken);


            if (ashApplicationServiceBase != null)
                setProps(props, "af.applicationservice.ash.base", ashApplicationServiceBase);

            if (applicationServiceBase != null)
                setProps(props, "af.applicationservice.base", applicationServiceBase);
            if (applicationServiceToken != null)
                setProps(props, "af.applicationservice.token", applicationServiceToken);

            if (refDataServiceBase != null)
                setProps(props, "af.refdataservice.base", refDataServiceBase);
            if (refDataServiceToken != null)
                setProps(props, "af.refdataservice.token", refDataServiceToken);
            if(ashRefDataServiceBase != null)
                setProps(props, "af.refdataservice.ash.base", ashRefDataServiceBase);

            if (facilityServiceBase != null)
                setProps(props, "af.facilityservice.base", facilityServiceBase);

            if (ashFacilityServiceBase != null)
                setProps(props, "af.facilityservice.ash.base", ashFacilityServiceBase);

            if (facilityServiceToken != null)
                setProps(props, "af.facilityservice.token", facilityServiceToken);

            if (listManagementServiceBase != null)
                setProps(props, "af.listmanagementservice.base", listManagementServiceBase);
            if (listManagementServiceToken != null)
                setProps(props, "af.listmanagementservice.token", listManagementServiceToken);

            if (lumosServiceBase != null)
                setProps(props, "af.lumosservice.base", lumosServiceBase);
            if (lumosServiceOrchestrationToken != null)
                setProps(props, "af.lumosservice.orchestration.token", lumosServiceOrchestrationToken);
            if (lumosServiceWorkflowToken != null)
                setProps(props, "af.lumosservice.workflow.token", lumosServiceWorkflowToken);

            if (schedulerCron != null) setProps(props, "af.scheduler.cron.entry", schedulerCron);
            if (schedulerCleanupCron != null) setProps(props, "af.scheduler.cron.cleanup.entry", schedulerCleanupCron);

//            if (schedulerCleanupAduCron != null) setProps(props, "af.scheduler.cron.cleanup.adu.entry", schedulerCleanupAduCron);

            if (schedulerEmailReceivedCron != null) setProps(props, "af.scheduler.cron.email-received.entry", schedulerEmailReceivedCron);
            if (schedulerDailyEmailCron != null) {
                setProps(props, "af.scheduler.daily.mail.cron.entry", schedulerDailyEmailCron);
            }

            if (schedulerDailyUnsentEmailsCron != null) {
                setProps(props, "af.scheduler.daily.mails.unsent.cron.entry", schedulerDailyUnsentEmailsCron);
            }

            if(docStashServiceBase != null) setProps(props, "af.docstashservice.base", docStashServiceBase);
            if(docStashServiceToken != null) setProps(props, "af.docstashservice.token", docStashServiceToken);

            if (af_sso_servers != null)
                setProps(props, "af.sso.servers", af_sso_servers);

            if(mailServiceBase != null) setProps(props, "af.mailservice.base", mailServiceBase);
            if(lumosMailServiceBase != null) setProps(props,"af.lumos.mailservice.base",lumosMailServiceBase);
            if(mailServiceToken != null) setProps(props, "af.mailservice.token", mailServiceToken);
            // DAF's sign-in to the auth service as a system: it gets DAF its Mail Service and Credit Data tokens.
            // AF_SYSTEM_*; the earlier names (AF_SYSTEM_CLIENT_SECRET, AF_MAILSERVICE_*) are still read when these are not set.
            String systemTokenUrl = firstEnv("AF_SYSTEM_TOKEN_URL", "AF_MAILSERVICE_SYSTEM_TOKEN_URL");
            String systemCaller = firstEnv("AF_SYSTEM_CALLER", "AF_MAILSERVICE_CALLER_SYSTEM");
            String systemClientSecret = firstEnv("AF_SYSTEM_BOOTSTRAP_CLIENT_SECRET", "AF_SYSTEM_CLIENT_SECRET", "AF_MAILSERVICE_CLIENT_SECRET");
            String mailAudience = env("AF_MAILSERVICE_AUDIENCE");
            if (systemTokenUrl != null) setProps(props, "af.system.tokenUrl", systemTokenUrl);
            if (systemCaller != null) setProps(props, "af.system.caller", systemCaller);
            if (systemClientSecret != null) props.put("af.system.clientSecret", systemClientSecret); // never logged
            if (mailAudience != null) setProps(props, "af.mailservice.audience", mailAudience);
            if(lumosMailServiceToken != null) setProps(props,"af.lumos.mailservice.token",lumosMailServiceToken);
            if(lumosMailServiceReadLimit !=null)
                setProps(props,"af.lumos.mailservice.read.limit",lumosMailServiceReadLimit);

            if(mailServiceTestDl != null) setProps(props, "af.mailservice.test.dl", mailServiceTestDl);
            if(mailServiceApprovalMailto != null)
                setProps(props,"af.mailservice.approval.mailto",mailServiceApprovalMailto);
            if(afCorpCreditSecretaryMail != null) setProps(props, "af.corp.credit.secretary.mail", afCorpCreditSecretaryMail);
            if(afCorpCreditSpecialistMail != null) setProps(props, "af.corp.credit.specialist.mail", afCorpCreditSpecialistMail);

            if(afFirstEscalationHours != null) setProps(props, "af.firstescalation.hours", afFirstEscalationHours);
            if(afSecondEscalationHours != null) setProps(props, "af.secondescalation.hours", afSecondEscalationHours);

            if(afMailAttachmentZip != null) setProps(props, "af.mail.attachment.zip", afMailAttachmentZip);
            if(afMailAttachmentLimit != null) setProps(props, "af.mail.attachment.limit", afMailAttachmentLimit);

            if(afApplicationStatuses != null) setProps(props, "af.application.statuses", afApplicationStatuses);

            if(afFacilityStatuses != null) setProps(props, "af.facility.statuses", afFacilityStatuses);

            if(afSocketIoUrl != null) setProps(props, "af.socketio.url", afSocketIoUrl);
            if(afSocketIoToken != null) setProps(props, "af.socketio.token", afSocketIoToken);

            if(afDocGenServiceBase != null) setProps(props, "af.docgenservice.base", afDocGenServiceBase);
            if(afDocGenToken != null) setProps(props, "af.docgenservice.token", afDocGenToken);

            if(afDocstashBase != null) setProps(props, "af.docstash.base", afDocstashBase);

            if (reviewAnalystTestMail != null)
                setProps(props, "af.mailservice.reviewanalyst.test", reviewAnalystTestMail);

            if (afSupportMailDl != null)
                setProps(props, "af.support.mail.dl", afSupportMailDl);

            if (afHolidayServiceUrl != null)
                setProps(props, "af.holidayservice.url", afHolidayServiceUrl);
            /*
            broader africa token configuration starts
            * */
            if (lumosServiceRefdataToken != null)
                setProps(props, "af.lumosservice.refdata.token", lumosServiceRefdataToken);
            if (lumosServicePartymodelToken != null)
                setProps(props, "af.lumosservice.partymodel.token", lumosServicePartymodelToken);
            if (lumosServiceLumosWorkflowToken != null)
                setProps(props, "af.lumosservice.lumosworkflow.token", lumosServiceLumosWorkflowToken);
               // setProps(props, "af.lumosservice.lumosworkflow.client.token", lumosServiceLumosWorkflowToken);
            if (lumosServiceLumosWorkflowDecisionToken != null)
                setProps(props, "af.lumosservice.lumosworkflow.decision.token", lumosServiceLumosWorkflowDecisionToken);
            if(lumosServiceLumosWorkflowApprovalToken!=null)
                setProps(props, "af.lumosservice.lumosapproval.token", lumosServiceLumosWorkflowApprovalToken);

            // Microsoft Graph (agenda Teams/Outlook meetings)
            String graphTenantId = env("AF_GRAPH_TENANT_ID");
            String graphClientId = env("AF_GRAPH_CLIENT_ID");
            String graphClientSecret = env("AF_GRAPH_CLIENT_SECRET");
            String graphOrganizer = env("AF_GRAPH_ORGANIZER");
            String graphProxyHost = env("AF_GRAPH_PROXY_HOST");
            String graphProxyPort = env("AF_GRAPH_PROXY_PORT");
            if (graphTenantId != null) setProps(props, "af.graph.tenantId", graphTenantId);
            if (graphClientId != null) setProps(props, "af.graph.clientId", graphClientId);
            if (graphClientSecret != null) props.put("af.graph.clientSecret", graphClientSecret); // never logged
            if (graphOrganizer != null) setProps(props, "af.graph.organizer", graphOrganizer);
            if (graphProxyHost != null) setProps(props, "af.graph.proxy.host", graphProxyHost);
            if (graphProxyPort != null) setProps(props, "af.graph.proxy.port", graphProxyPort);


            String creditDataAudience = env("AF_CREDITDATA_AUDIENCE");
            if (creditDataAudience != null) setProps(props, "af.creditdata.audience", creditDataAudience);

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        // The agenda job endpoints accept only the user the scheduler signs in as: the subject of
        // af.token unless AF_SCHEDULER_USER names it.
        String schedulerUser = env("AF_SCHEDULER_USER");
        String schedulerSubject = schedulerUser != null ? schedulerUser : tokenSubject(props.get("af.token"));
        if (schedulerSubject != null) {
            setProps(props, "af.scheduler.user", schedulerSubject);
        } else {
            logger.warn("AF_TOKEN is not set: the agenda cut-off and reminder jobs cannot sign in");
        }

        Map<String, String> settings = Collections.unmodifiableMap(props);
        CreditDataTokenFilter.configure(settings);
        return settings;

    }

    /**
     * An environment setting, or null when it is not set or blank: an empty line in env.properties keeps the
     * default rather than replacing it with nothing.
     */
    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    /** The first of these environment settings that is set (a setting renamed, its earlier name still read). */
    private static String firstEnv(String... names) {
        for (String name : names) {
            String value = env(name);
            if (value != null) return value;
        }
        return null;
    }

    private void setProps(Map<String, String> props, String propKey, String propValue) {
        props.put(propKey, propValue);
        logger.info(propKey + ": " + (isCredential(propKey) ? "<set>" : propValue));
    }

    /** Credentials are never written to the log. */
    static boolean isCredential(String key) {
        String k = key.toLowerCase(java.util.Locale.ROOT);
        return k.endsWith(".token") || k.endsWith(".secret") || k.endsWith(".clientsecret")
                || k.endsWith(".apikey") || k.endsWith(".password") || k.endsWith(".pass");
    }

    /** The subject of a JWT, read without verification (it is our own token), or null. */
    static String tokenSubject(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
            return m.find() ? m.group(1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    
    @Singleton
    public AuthServiceClient getAuthServiceClient(Client client) {
        return new AuthServiceBootClient(getLmsProperties().get("af.authservice.base"));
    }

}
