package za.co.rmb.approvalforumdrools.engine;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import za.co.rmb.approvalforumdrools.rules.approvalforum.data.ApprovalForumData;
import za.co.rmb.approvalforumdrools.service.ApprovalForumDroolsService;

@Singleton
public class ApprovalForumDroolsEngine extends ApprovalForumDroolsEngineUtil {

    ApprovalForumDroolsService approvalForumDroolsService;

    @Inject
    public ApprovalForumDroolsEngine(ApprovalForumDroolsService approvalForumDroolsService){
        this.approvalForumDroolsService = approvalForumDroolsService;
        reloadRules(approvalForumDroolsService.getRules().getBytes());
    }

    public ApprovalForumDroolsEngine(String rules){
        reloadRules(rules.getBytes());
    }

    public void executeApprovalForumRules(ApprovalForumData data) throws Exception{
	    getApprovalForumRuleSession().execute(data);
	    validateDisplayRuleExecuted(data);
	    validateListData(data);
    }

    public void reloadRules(byte[] rules){
        setApprovalForumRuleSession(getStatelessKieSession(rules));
    }

}
