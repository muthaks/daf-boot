package za.co.rmb.credit.daf.config;

import jakarta.inject.Provider;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import za.co.rmb.approvalforum.web.module.ApprovalForumWebResourcesModule;
import za.co.rmb.approvalforum.web.module.RefDataServiceModule;
import za.co.rmb.approvalforum.web.quartz.SchedulerJobFactory;
import za.co.rmb.approvalforum.web.quartz.SchedulerProvider;
import za.co.rmb.approvalforumrules.client.ApprovalForumDroolsClientImpl;
import za.co.rmb.credit.application.client.http.ApplicationServiceImpl;
import za.co.rmb.credit.authservice.web.client.AuthServiceClient;
import za.co.rmb.credit.facility.client.http.FacilityServiceImpl;
import za.co.rmb.credit.lookupservice.client.http.CounterpartyServiceImpl;
import za.co.rmb.credit.lookupservice.client.http.HttpPESearch;
import za.co.rmb.credit.mailservice.web.client.MailServiceClient;
import za.co.rmb.credit.referencedata.client.RefDataServiceImpl;
import za.co.rmb.docstash.client.http.DocStashServiceImpl;
import za.co.rmb.listmanagement.client.http.ListManagementServiceImpl;

import java.util.Map;

/**
 * The legacy Guice provider methods, exposed as Spring beans.
 *
 * <p>Every bean delegates to the provider method of the legacy module that defined it
 * ({@link ApprovalForumWebResourcesModule}, {@link RefDataServiceModule}, {@link SchedulerProvider}), so
 * settings defaults, environment overrides, base URLs and tokens are the legacy code itself. Names and
 * scopes follow the Guice bindings: {@code @Singleton} providers are singletons, unscoped ones (the JAX-RS
 * {@link Client} and the named {@link WebTarget}s) create a new instance per injection.</p>
 */
@Configuration(proxyBeanMethods = false)
public class LegacyModulesConfig {

    private final ApprovalForumWebResourcesModule webResources = new ApprovalForumWebResourcesModule();
    private final RefDataServiceModule refData = new RefDataServiceModule();

    @Bean
    static LegacyBindings legacyBindings() {
        return new LegacyBindings();
    }

    @Bean
    static org.springframework.beans.factory.config.BeanFactoryPostProcessor guiceCircularProxies() {
        return za.co.rmb.credit.daf.support.GuiceCircularProxies.installer("za.co.rmb.approvalforum.");
    }

    // ApprovalForumWebResourcesModule

    @Bean("af_settings")
    Map<String, String> afSettings() {
        return webResources.getLmsProperties();
    }

    @Bean
    AuthServiceClient authServiceClient() {
        return webResources.getAuthServiceClient(refData.getClient());
    }

    // ApprovalForumPropertiesModule: APP_PROPERTIES is bound in Guice but never injected, so never read

    @Bean("APP_PROPERTIES")
    @Lazy
    @SuppressWarnings("unchecked")
    Map<String, String> appProperties(ApplicationContext context) {
        return ((Provider<Map<String, String>>) context.getBean(
                "za.co.rmb.approvalforum.data.ApprovalForumPropertiesProvider")).get();
    }

    // RefDataServiceModule

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    Client client() {
        return refData.getClient();
    }

    @Bean("afRulesClient")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget afRulesClient(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getApprovalForumRulesTarget(refData.getClient(), afSettings);
    }

    @Bean("application")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget applicationTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getApplicationTarget(refData.getClient(), afSettings);
    }

    @Bean("facility")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget facilityTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getFacilityTarget(refData.getClient(), afSettings);
    }

    @Bean("lumosOrchestration")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget lumosOrchestrationTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getLumosOrchestrationTarget(refData.getClient(), afSettings);
    }

    @Bean("lumosWorkflow")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget lumosWorkflowTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getLumosWorkflowTarget(refData.getClient(), afSettings);
    }

    @Bean("basePESearch")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget basePeSearchTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getPETarget(refData.getClient(), afSettings);
    }

    @Bean("counterparty")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget counterpartyTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getCounterpartyTarget(refData.getClient(), afSettings);
    }

    @Bean("listmanagement")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget listManagementTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getListManagementTarget(refData.getClient(), afSettings);
    }

    @Bean("refdata")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget refDataTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getRefDataTarget(refData.getClient(), afSettings);
    }

    @Bean("docstash")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget docStashTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getDocStashTarget(refData.getClient(), afSettings);
    }

    @Bean("docgen")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WebTarget docgenTarget(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getDocgenTarget(refData.getClient(), afSettings);
    }

    @Bean
    ApprovalForumDroolsClientImpl approvalForumRulesService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getApprovalForumRulesService(refData.getClient(), afSettings);
    }

    @Bean
    HttpPESearch httpPeSearchService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getHttpPESearchService(refData.getClient(), afSettings);
    }

    @Bean
    CounterpartyServiceImpl counterpartyService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getCounterpartyService(refData.getClient(), afSettings);
    }

    @Bean
    ApplicationServiceImpl applicationService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getApplicationService(refData.getClient(), afSettings);
    }

    @Bean
    FacilityServiceImpl facilityService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getFacilityService(refData.getClient(), afSettings);
    }

    @Bean
    ListManagementServiceImpl listManagementService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getListManagementService(refData.getClient(), afSettings);
    }

    @Bean
    RefDataServiceImpl refDataService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getRefDataService(refData.getClient(), afSettings);
    }

    @Bean
    DocStashServiceImpl docStashService(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getDocStashService(refData.getClient(), afSettings);
    }

    @Bean
    MailServiceClient mailServiceClient(@Qualifier("af_settings") Map<String, String> afSettings) {
        return refData.getMailServiceClient(refData.getClient(), afSettings);
    }

    // SchedulerModule: bind(Scheduler).toProvider(SchedulerProvider)

    @Bean
    Scheduler scheduler(SchedulerJobFactory jobFactory) throws SchedulerException {
        return new SchedulerProvider(jobFactory).get();
    }
}
