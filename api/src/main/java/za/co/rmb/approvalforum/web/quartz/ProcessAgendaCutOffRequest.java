package za.co.rmb.approvalforum.web.quartz;

import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Feature;
import java.util.Map;

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
