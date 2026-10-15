package za.co.rmb.approvalforum.web.module;

import jakarta.inject.Singleton;
import jakarta.inject.Named;
import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import jakarta.ws.rs.client.ClientBuilder;
import za.co.rmb.approvalforumrules.client.ApprovalForumDroolsClientImpl;
import za.co.rmb.approvalforumrules.model.ApprovalForumRulesClient;
import za.co.rmb.credit.application.client.ApplicationService;
import za.co.rmb.credit.application.client.http.ApplicationServiceImpl;
import za.co.rmb.credit.facility.client.FacilityService;
import za.co.rmb.credit.facility.client.http.FacilityServiceImpl;
import za.co.rmb.credit.lookupservice.client.CounterpartyService;
import za.co.rmb.credit.lookupservice.client.PESearch;
import za.co.rmb.credit.lookupservice.client.http.CounterpartyServiceImpl;
import za.co.rmb.credit.lookupservice.client.http.HttpPESearch;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;
import za.co.rmb.credit.referencedata.client.RefDataService;
import za.co.rmb.credit.referencedata.client.RefDataServiceImpl;
import za.co.rmb.credit.security.client.BearerTokenAuth;
import za.co.rmb.credit.security.client.InvocationAuth;
import za.co.rmb.docstash.client.DocStashService;
import za.co.rmb.docstash.client.http.DocStashServiceImpl;
import za.co.rmb.listmanagement.client.ListManagementService;
import za.co.rmb.listmanagement.client.http.ListManagementServiceImpl;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Feature;
import java.util.Map;

public class RefDataServiceModule {
    public Client getClient() {
        return ClientBuilder.newClient();
    }
    @jakarta.inject.Named("afRulesClient")
    public WebTarget getApprovalForumRulesTarget(Client client, @Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.rulesservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.rulesservice.base"));
    }
    @jakarta.inject.Named("application")
    public WebTarget getApplicationTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.applicationservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.applicationservice.base").replace("/api",""));
    }
    @jakarta.inject.Named("facility")
    public WebTarget getFacilityTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.facilityservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.facilityservice.base"));
    }
    @jakarta.inject.Named("lumosOrchestration")
    public WebTarget getLumosOrchestrationTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.lumosservice.orchestration.token"));
        client.register(feature);
        return client.target(afSettings.get("af.lumosservice.base"));
    }
    @jakarta.inject.Named("lumosWorkflow")
    public WebTarget getLumosWorkflowTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.lumosservice.workflow.token"));
        client.register(feature);
        return client.target(afSettings.get("af.lumosservice.base"));
    }
    @Named("basePESearch")
    public WebTarget getPETarget(Client client, @Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.counterpartyservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.counterpartyservice.base"));
    }
    @Named("counterparty")
    public WebTarget getCounterpartyTarget(Client client, @Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.counterpartyservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.counterpartyservice.base").replace("/api",""));
    }
    @Named("listmanagement")
    public WebTarget getListManagementTarget(Client client, @Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.listmanagementservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.listmanagementservice.base"));
    }
    @jakarta.inject.Named("refdata")
    public WebTarget getRefDataTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.refdataservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.refdataservice.base"));
    }
    @jakarta.inject.Named("docstash")
    public WebTarget getDocStashTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.docstashservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.docstashservice.base"));
    }
    @jakarta.inject.Named("docgen")
    public WebTarget getDocgenTarget(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        Feature feature = OAuth2ClientSupport.feature(afSettings.get("af.docgenservice.token"));
        client.register(feature);
        return client.target(afSettings.get("af.docgenservice.base"));
    }

    @Singleton
    public ApprovalForumDroolsClientImpl getApprovalForumRulesService(Client client, @Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.rulesservice.base"));
        za.co.rmb.approvalforumrules.security.client.InvocationAuth auth =
                new za.co.rmb.approvalforumrules.security.client.BearerTokenAuth(afSettings.get("af.rulesservice.token"));
        return new ApprovalForumDroolsClientImpl(target, auth);
    }

    @Singleton
    public HttpPESearch getHttpPESearchService(Client client, @Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.counterpartyservice.base").replace("/api",""));
        InvocationAuth auth = new BearerTokenAuth(afSettings.get("af.counterpartyservice.token"));
        return new HttpPESearch(target, auth, false);
    }

    @Singleton
    public CounterpartyServiceImpl getCounterpartyService(Client client, @Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.counterpartyservice.base").replace("/api",""));
        InvocationAuth auth = new BearerTokenAuth(afSettings.get("af.counterpartyservice.token"));
        return new CounterpartyServiceImpl(target, auth, false);
    }

    @Singleton
    public ApplicationServiceImpl getApplicationService(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.applicationservice.base").replace("/api",""));
        InvocationAuth auth = new BearerTokenAuth(afSettings.get("af.applicationservice.token"));
        return new ApplicationServiceImpl(target, auth, false);
    }

    @Singleton
    public FacilityServiceImpl getFacilityService(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.facilityservice.base").replace("/api",""));
        InvocationAuth auth = new BearerTokenAuth(afSettings.get("af.facilityservice.token"));
        return new FacilityServiceImpl(target, auth, false);
    }

    @Singleton
    public ListManagementServiceImpl getListManagementService(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.listmanagementservice.base").replace("/api",""));
        za.co.rmb.listmanagement.security.client.InvocationAuth auth =
                new za.co.rmb.listmanagement.security.client.BearerTokenAuth(afSettings.get("af.listmanagementservice.token"));
        return new ListManagementServiceImpl(target, auth);
    }

    @Singleton
    public RefDataServiceImpl getRefDataService(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.refdataservice.base").replace("/api",""));
        InvocationAuth auth = new BearerTokenAuth(afSettings.get("af.refdataservice.token"));
        return new RefDataServiceImpl(target, auth, false);
    }

    @Singleton
    public DocStashServiceImpl getDocStashService(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.docstashservice.base").replace("/api",""));
        za.co.rmb.docstash.security.client.InvocationAuth auth =
                new za.co.rmb.docstash.security.client.BearerTokenAuth(afSettings.get("af.docstashservice.token"));
        return new DocStashServiceImpl(target, auth);
    }

    @Singleton
    public MailServiceClient getMailServiceClient(Client client, @jakarta.inject.Named("af_settings") Map<String, String> afSettings) {
        WebTarget target = client.target(afSettings.get("af.mailservice.base"));
        return new za.co.rmb.approvalforum.web.mail.SystemTokenMailServiceClient(target, afSettings);
    }

}