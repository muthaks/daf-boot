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
                    "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJlODA0NmQ1Mi03NWIwLTQ4ZjQtODJjOS0zNDJmNjZkZDAwOTUiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2FwcHJvdmFsZm9ydW0iLCJzdWIiOiJTTkFQQVBQX1JSQ2FwdHVyZXJAcm1iLmNvLnphIiwiaWF0IjoxNTcxNjQzMTE3LCJleHAiOjQ3MzM4ODMxMTcsInJvbGVzIjpbIkNyZWRpdEFuYWx5c3QiXSwicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.zFCMf-xKjIu67IlzDUxSnCIUNvdljXx4hxiQMiU6aC0")
            .put("af.environment", "DEV").put("af.devusers", "jvf,kir,kea,jjx,jeb,axu,shc,ffa,ixm,dvo")
            .put("af.adgroup.ou", "OU=DAF,OU=Credit,OU=Applications,OU=Rand Merchant Bank,DC=rmb,DC=co,DC=za")
            .put("af.keycloakConfig.url", "https://rmb-rbvdvcrsk01.fnb.co.za/auth")
            .put("af.keycloakConfig.realm", "master").put("af.keycloakConfig.clientId", "lms")
            .put("af.keycloakConfig.secret", "0aab530d-6550-4c5b-939b-b9a7546ae99f")
            .put("af.applicationservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/application-2.6.0")
            .put("af.applicationservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/application-2.7.0")
            .put("af.applicationservice.token",
                    "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZmJkYjk4YS0wZjlmLTRhZjAtYjgxNS1jYTA2ZTgzZmMzZmIiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2FwcGxpY2F0aW9uIiwic3ViIjoic19lY2xjIiwiaWF0IjoxNTQ5MjYzNTY5LCJleHAiOjQ3MTE1MDM1NjksInJlZnJlc2hDb3VudCI6MCwicmVmcmVzaExpbWl0IjoxfQ.i9uWpJBgfnAsRDZ9_V6CcJZjTllPF-wsUni2d8vdpqM")
            .put("af.counterpartyservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/counterparty-2.5.0")
            .put("af.counterpartyservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/counterparty-2.7.0")
            .put("af.counterpartyservice.token",
                    "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3N2IzMWIyNy0xMjU0LTQ5YTUtYjg0NS0yNTNmZTg4NmNmYTYiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2NvdW50ZXJwYXJ0eSIsInN1YiI6InNfZWNsYyIsImlhdCI6MTU0OTUyMzk5MSwiZXhwIjo0NzExNzYzOTkxLCJyZWZyZXNoQ291bnQiOjAsInJlZnJlc2hMaW1pdCI6MX0.K_f9YylJfAJn15my7khaW4oGwqSLM5sSBMalz3wsMxo")
            .put("af.refdataservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/referencedata-2.5.0")
            .put("af.refdataservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/referencedata-2.5.1")
            .put("af.refdataservice.token",
                    "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJmZDE0NDE0Ni1mMGFiLTRiOGUtOTcyYi02YmU0OGUwYzljYjAiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL3JlZmVyZW5jZWRhdGEiLCJzdWIiOiJzX2VjbGMiLCJpYXQiOjE1NTExNjQwODMsImV4cCI6NDcxMzQwNDA4MywicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.9ZXJbiUTuDpJd0n_s1-Ctizz9FC0C3UFpCMTt5HnPcI")
            .put("af.facilityservice.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/facility-2.5.0")
            .put("af.facilityservice.ash.base", "http://rmb-rbvdvcrsk01.fnb.co.za:8580/facility-2.6.0")
            .put("af.facilityservice.token",
                    "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5NmQxNjA0Ny01Yzc5LTQ5YTAtOWQ3MS1lZjQ3YmNhNTkxMTciLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2ZhY2lsaXR5Iiwic3ViIjoic19lY2xjIiwiaWF0IjoxNTQ5NTMwMDQ4LCJleHAiOjQ3MTE3NzAwNDgsInJlZnJlc2hDb3VudCI6MCwicmVmcmVzaExpbWl0IjoxfQ.25qIm8EuiFRUJzJO8VglMAudYcktKOOG7BSB8DSBjT8")
            .put("af.lumosservice.base", "https://lumos-apisix-dev.apps.dev-01-rb.ocp.fnb.co.za")
            .put("af.lumosservice.orchestration.token",
                    "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJEQUYiLCJpc3MiOiJodHRwczovL2x1bW9zLmZpcnN0cmFuZC5jby56YSIsImF1ZCI6Im9yY2hlc3RyYXRpb24iLCJyb2xlcyI6WyJyb2xlIl0sImlhdCI6MTY3ODE3NDU4MCwiZXhwIjo0ODQwNDE0NTgwfQ.NIz-IDhLe1Sp9I7xlv3AwUUL-UocnOt3DsdA80uhxV5Bw9l9wQDzB1_1bNXxq-9TiYr0dMkmUQq4BQu2EFa2og")
            .put("af.lumosservice.workflow.token",
                    "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJEQUYiLCJpc3MiOiJodHRwczovL2x1bW9zLmZpcnN0cmFuZC5jby56YSIsImF1ZCI6Im9yY2hlc3RyYXRpb24iLCJyb2xlcyI6WyJyb2xlIl0sImlhdCI6MTY3ODE3NDU4MCwiZXhwIjo0ODQwNDE0NTgwfQ.NIz-IDhLe1Sp9I7xlv3AwUUL-UocnOt3DsdA80uhxV5Bw9l9wQDzB1_1bNXxq-9TiYr0dMkmUQq4BQu2EFa2og")
            .put("af.rulesservice.base", "http://rmb-axu1-w:8197/approvalforumdrools/api/")
            .put("af.listmanagementservice.base", "http://rmb-axu1-w:8197/listmanagement/api")
            .put("af.listmanagementservice.token","eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4OTdhODZmYy1iOTEzLTQxMzktYTk0Zi1kZGE5OWUyMjgwNjkiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2xpc3RtYW5hZ2VtZW50Iiwic3ViIjoiQVhVQHJtYi5jby56YSIsImlhdCI6MTU2NTE3NjY5MywiZXhwIjo0NzI3NDE2NjkzLCJyb2xlcyI6WyJCVENyZWRpdFVzZXIiLCJBc3NldE1hbmFnZW1lbnRVc2VyIiwiQ29tcGxpYW5jZVVzZXIiLCJBc3NldE1hbmFnZW1lbnRBZG1pbiIsIkNvbXBsaWFuY2VBZG1pbiIsIkJUQ3JlZGl0QWRtaW4iXSwicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.5PG-EiLTaJdxDgWSFd89btfZ2VNfTHU8hi0MbTcSaBw")
            .put("af.scheduler.cron.entry", "0 * 5-20 * * ?")
            .put("af.scheduler.daily.mail.cron.entry", "0 0 8 ? * MON-FRI")
            .put("af.scheduler.daily.mails.unsent.cron.entry", "0 0 11,15,19 ? * MON-FRI")
            .put("af.scheduler.cron.cleanup.entry", "0 0 5 * * ?")
//            .put("af.scheduler.cron.cleanup.adu.entry", "0 0 5 * * ?")
            .put("af.scheduler.agenda.cutoff.cron.entry", "0 15 17 ? * THU")
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
            .put("af.docstashservice.token", "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5MzMwNDViYS04MjNjLTQxYTMtYjBlNy0wYWY3MGU2MjExMjYiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2RvY3N0YXNoIiwic3ViIjoiU05BUEFQUF9BcHByb3ZlcjFAcm1iLmNvLnphIiwiaWF0IjoxNTYwMTY0MTM5LCJleHAiOjQ3MjI0MDQxMzksInJvbGVzIjpbImRvY3N0YXNoX21lZXRpbmdfaW5mb191cGxvYWRlciIsImRvY3N0YXNoX3JlcG9ydGluZ191cGxvYWRlciIsImRvY3N0YXNoX2NvcnJlc3BvbmRlbmNlX3VwbG9hZGVyIiwiZG9jc3Rhc2hfY3JlZGl0X2FwcF9jb21wb25lbnRfdXBsb2FkZXIiLCJkb2NzdGFzaF9wb2xpY2llc19hbmRfZnJhbWV3b3Jrc191cGxvYWRlciIsImRvY3N0YXNoX2FsbG9jYXRpb25zX3VwbG9hZGVyIiwiZG9jc3Rhc2hfbGVnYWxfZG9jX3VwbG9hZGVyIiwiZG9jc3Rhc2hfY3JlZGl0X2Fzc2Vzc21lbnRfdXBsb2FkZXIiXSwicmVmcmVzaENvdW50IjowLCJyZWZyZXNoTGltaXQiOjF9.fES7T1rpZtxoetV4oVJMevxwKVXRcZPtadyJ1pfn9ms")
            .put("af.docstashservice.base", "https://rmb-rbvdvcrsk01.fnb.co.za:9080/docstash/api")
            .put("af.sso.servers", "http://rmb-rbvtsrska01:8080/auth/,http://rmb-rbvtsrskd01/auth/,http://rmb-rbvutrska01:8080/auth/,http://rmb-rbvutrskd01/auth/")
            .put("af.mailservice.token", "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI3NWRjZjM2Yy01ODhlLTQzZGYtODI5My01MzMxZmYzYWY3ZjYiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL21haWwiLCJzdWIiOiJTbmFwQXBwIiwiaWF0IjoxNTM0MzA1OTA0LCJleHAiOjQ2OTY1NDU5MDQsInJlZnJlc2hDb3VudCI6MCwicmVmcmVzaExpbWl0IjoxfQ.KL8wNrxzVfwwAHq95j5Igef-oNZChGJ0zAS-iyC1W98")
            .put("af.lumos.mailservice.token","eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtYWlsc2VydmljZSIsImlzcyI6Imh0dHBzOi8vbHVtb3MuZmlyc3RyYW5kLmNvLnphIiwiYXVkIjoibWFpbHNlcnZpY2UiLCJqdGkiOiI3NjkwOTJiYy04MzdjLTRlY2MtYjRmYS1kNGRkMTUxZDVlYmYiLCJyb2xlcyI6WyJpc3N1ZXIiXSwiaWF0IjoxNzI5Njg3Njk3LCJleHAiOjMzMzUyMDg3Njk3fQ.BNaDYPEi6FL2KyStwkstRgv9UE56JZkA1lIyrV6EzkUcF2W0KX57zC1JnOxkl5-DvKt4HYbAWA4J2OwjoTCLkA")
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
            .put("af.mail.test.emails", "shelley.speirs@rmb.co.za")
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
            .put("af.rulesservice.token", "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjMjg3MGU2OC1lYTkwLTRkYTEtOWMzYS04NWMyMDFmM2Q3ZTEiLCJpc3MiOiJodHRwOi8vY3JlZGl0LnJtYi5jby56YSIsImF1ZCI6Imh0dHA6Ly9jcmVkaXQucm1iLmNvLnphL2FwcHJvdmFsZm9ydW1kcm9vbHMiLCJzdWIiOiJEQUZfU3lzdGVtQHJtYi5jby56YSIsImlhdCI6MTU4ODU2Nzc0OCwiZXhwIjo0NzUwODA3NzQ4LCJyb2xlcyI6W10sInJlZnJlc2hDb3VudCI6MCwicmVmcmVzaExpbWl0IjoxfQ.nDwy1j2ZhmKx9eRy6yFyqFv-f7OVVqreNvSX0ZCOxLw")
            .put("af.lumosservice.refdata.token",
                    "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJEQUYiLCJpc3MiOiJodHRwczovL2x1bW9zLmZpcnN0cmFuZC5jby56YSIsImF1ZCI6InJlZmVyZW5jZS1kYXRhIiwicm9sZXMiOlsicm9sZSJdLCJpYXQiOjE3NTQ2NDY3NTksImV4cCI6NDkxNjg4Njc1OX0.zjxpy5NeUVu9f0e4lORRughfb4jDwGYwFwl78AqrAUEL2bCMqNQhVdhLds-kSqrm3y6HD3LYAAD5tkw6DHTWNw")
            .put("af.lumosservice.partymodel.token",
                    "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJBdXRoIERFViIsImlzcyI6Imh0dHBzOi8vbHVtb3MuZmlyc3RyYW5kLmNvLnphIiwiYXVkIjoicGFydHltb2RlbCIsInJvbGVzIjpbImlzc3VlciJdLCJpYXQiOjE2NDQ5OTU5ODIsImV4cCI6NDgwNzIzNTk4Mn0.5kcPSXgFchI-fkd0baL6RG7NUu5RKVrtTEM__fvQ-3b-3w_DNUNdNoIjOJ13sQ87zXJSQnBc1kWw6MIhJVtG0w")
            .put("af.lumosservice.lumosworkflow.token",
                    "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJEQUYiLCJpc3MiOiJodHRwczovL2x1bW9zLmZpcnN0cmFuZC5jby56YSIsImF1ZCI6IndvcmtmbG93Iiwicm9sZXMiOlsicm9sZSJdLCJpYXQiOjE3NTQ2NDY3MDMsImV4cCI6NDkxNjg4NjcwM30.CG83uke_9i6arVuT0weXITmrVzivxTd2aD9IMWjg3cSFPoytrj5c9hEdUBVSAuwCKfnB-a46xmMrft91dyo7GA")
            .put("af.lumosservice.lumosworkflow.decision.token",
                    "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJTX1NWQ19MVU1PU19NVUxDVF9RQUBybWIuY28uemEiLCJpc3MiOiJodHRwczovL2x1bW9zLmZpcnN0cmFuZC5jby56YSIsImF1ZCI6IndvcmtmbG93Iiwicm9sZXMiOlsiUHJ1ZGVudGlhbENhbGN1bGF0aW9uc3xDcmVhdGUiLCJDb2xsYXRlcmFsIFBvb2x8VXBkYXRlIiwiRmFjaWxpdHkgUER8RGVsZXRlIiwiQ29sbGF0ZXJhbCBQb29sfERlbGV0ZSIsIk9ibGlnb3J8VXBkYXRlIiwiT2JsaWdvcnxEZWxldGUiLCJSZWNvdXJzZUVudGl0eXxDcmVhdGUiLCJGYWNpbGl0eSBQRHxVcGRhdGUiLCJBcHByb3ZhbCBXb3JrZmxvd3xBcHByb3ZlIiwiQm9ycm93ZXJ8VXBkYXRlIiwiQ2xpZW50fENyZWF0ZSIsIkFwcHJvdmFsQ2FsY3VsYXRpb25zfFVwZGF0ZSIsIkNvbGxhdGVyYWxQcm92aWRlcnxDcmVhdGUiLCJBZG1pbnxWaWV3IiwiQ2xpZW50IExHRHxVcGRhdGUiLCJBcHByb3ZhbENhbGN1bGF0aW9uc3xEZWxldGUiLCJCb3Jyb3dlcnxEZWxldGUiLCJDcmVkaXRTdW1tYXJ5fENyZWF0ZSIsIkFwcHJvdmFsIFdvcmtmbG93fFJldmlldyBTdWJtaXQiLCJDbGllbnQgTEdEfERlbGV0ZSIsIkZhY2lsaXR5fENyZWF0ZSIsIkNvbmRpdGlvbnN8Q3JlYXRlIiwiQXBwcm92YWwgV29ya2Zsb3d8Q2FwdHVyZSBEZWNpc2lvbiIsIkNsaWVudCBQRHxDcmVhdGUiLCJMaW1pdHxDcmVhdGUiLCJGaW5hbmNpYWwgU3ByZWFkfENyZWF0ZSIsIkNyZWRpdFN1bW1hcnl8VXBkYXRlIiwiQ29uZGl0aW9uc3xEZWxldGUiLCJEaXNjYXJkfEFsbCIsIkNvbGxhdGVyYWx8RGVsZXRlIiwiRmFjaWxpdHkgUER8Q3JlYXRlIiwiTGltaXR8VXBkYXRlIiwiUmVjb3Vyc2VFbnRpdHl8RGVsZXRlIiwiQ29uZGl0aW9uc3xEb3dubG9hZCIsIkRpZ2l0YWwgQXBwcm92YWwgRm9ydW18Q2FwdHVyZSIsIkFwcHJvdmFsIFdvcmtmbG93fENyZWRpdCBTdWJtaXQiLCJDbGllbnR8RGVsZXRlIiwiRmFjaWxpdHkgTEdEfFVwZGF0ZSIsIkNvbGxhdGVyYWxQcm92aWRlcnxVcGRhdGUiLCJDb2xsYXRlcmFsIFBvb2x8TGluayIsIkJvcnJvd2VyfENyZWF0ZSIsIkZhY2lsaXR5IExHRHxEZWxldGUiLCJDb2xsYXRlcmFsfENyZWF0ZSIsIkNsaWVudHxVcGRhdGUiLCJGYWNpbGl0eXxVcGRhdGUiLCJGYWNpbGl0eXxEZWxldGUiLCJSZWZlcmVuY2UgRGF0YXxWaWV3IiwiQ2xpZW50IFBEfERlbGV0ZSIsIlBydWRlbnRpYWxDYWxjdWxhdGlvbnN8VXBkYXRlIiwiRmluYW5jaWFsIFNwcmVhZHxEZWxldGUiLCJPYmxpZ29yfENyZWF0ZSIsIkZpbmFuY2lhbCBTcHJlYWR8VXBkYXRlIiwiQ29sbGF0ZXJhbCBQb29sfENyZWF0ZSIsIkNsaWVudCBQRHxVcGRhdGUiLCJQcnVkZW50aWFsQ2FsY3VsYXRpb25zfERlbGV0ZSIsIkNsaWVudCBMR0R8Q3JlYXRlIiwiQXBwcm92YWxDYWxjdWxhdGlvbnN8Q3JlYXRlIiwiQ29sbGF0ZXJhbCBQb29sfFVubGluayIsIkNyZWRpdCBCZWhhdmlvciBDaGVja2xpc3R8VXBkYXRlIiwiQ29sbGF0ZXJhbHxVcGRhdGUiLCJEaWdpdGFsIEFwcHJvdmFsIEZvcnVtfEFwcHJvdmUiLCJSZWNvdXJzZUVudGl0eXxVcGRhdGUiLCJGYWNpbGl0eSBMR0R8Q3JlYXRlIiwiQ29uZGl0aW9uc3xVcGRhdGUiLCJMaW1pdHxEZWxldGUiLCJMdW1vc3xWaWV3IiwiQ29sbGF0ZXJhbFByb3ZpZGVyfERlbGV0ZSIsIiBDcmVkaXQgQmVoYXZpb3IgQ2hlY2tsaXN0fENyZWF0ZSJdLCJpYXQiOjE3NTY3NDkyOTgsImV4cCI6MTc2MTU3NjU5M30.KkF_ubDDPEBlT0dOc5MStlIfmI885kJom8D-xf3sCK0kMGK2OTMpf0C3CcuvPH86z3yodVxu_3MIAreG48YhXw")
            .put("af.lumosservice.lumosworkflow.client.token",
            "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJvcmNoZXN0cmF0aW9uIiwiaXNzIjoiaHR0cHM6Ly9sdW1vcy5maXJzdHJhbmQuY28uemEiLCJhdWQiOiJ3b3JrZmxvdyIsInJvbGVzIjpbInJvbGUiXSwiaWF0IjoxNjY2ODc0NDYzLCJleHAiOjQ4MjkxMTQ0NjN9.9oMd9mmc9PP39VPi2hWuk4-ahIasc9jyaBMYBkmLOIR1pms08Ge-8hjMr09IS0aG8bheA1bPL6nl5YhBgijduQ")
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
        logger.info(propKey + ": " + propValue);
    }

    
    @Singleton
    public AuthServiceClient getAuthServiceClient(Client client) {
        return new AuthServiceBootClient(getLmsProperties().get("af.authservice.base"));
    }

}
