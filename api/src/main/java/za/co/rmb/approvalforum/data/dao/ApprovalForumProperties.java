package za.co.rmb.approvalforum.data.dao;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.inject.Named;
import java.util.Map;

@Singleton
public class ApprovalForumProperties {

    private Map<String, String> appProperties;

    @Inject
    public ApprovalForumProperties(@Named("APP_PROPERTIES") Map<String, String> appProperties){
	    this.appProperties = appProperties;
    }

    public Map<String, String> getAppProperties() {
	return appProperties;
    }

}
