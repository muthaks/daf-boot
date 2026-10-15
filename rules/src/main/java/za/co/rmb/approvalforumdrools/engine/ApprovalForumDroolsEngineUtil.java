package za.co.rmb.approvalforumdrools.engine;

import org.kie.api.runtime.rule.RuleContext;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforumdrools.rules.approvalforum.data.ApprovalForumData;

import java.util.HashMap;
import java.util.Map;

public abstract class ApprovalForumDroolsEngineUtil {

    private StatelessKieSession approvalForumRuleSession;

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumDroolsEngineUtil.class);

    StatelessKieSession getApprovalForumRuleSession() {
	    return approvalForumRuleSession;
    }

    void setApprovalForumRuleSession(StatelessKieSession approvalForumMetadataSession) {
	    this.approvalForumRuleSession = approvalForumMetadataSession;
    }

    void validateDisplayRuleExecuted(ApprovalForumData data) {
        if (!data.isRuleExecuted()) {
            setDefaultDisplayMetadata();
        }
    }

    void validateListData(ApprovalForumData data) throws Exception{
        Map<Integer, Integer> uniqueRankMap;
        int totalMaxCount;
        boolean listWithMaxNotSetFound;
        if(data.getApprovalForumModelDataList() != null && !data.getApprovalForumModelDataList().isEmpty()) {
            for(ApprovalForumData.ApprovalForumModelData modelData: data.getApprovalForumModelDataList()) {
                totalMaxCount = 0;
                listWithMaxNotSetFound = false;
                uniqueRankMap = new HashMap<>();
                if (modelData.getListModelData() != null && !modelData.getListModelData().isEmpty()) {
                    for (ApprovalForumData.ApprovalForumListModelData listModelData : modelData.getListModelData()) {
                        if (uniqueRankMap.containsKey(listModelData.getRank())) {
                            throw new Exception("Multiple list contains the same rank:" + listModelData.getRank());
                        }
                        uniqueRankMap.put(listModelData.getRank(), listModelData.getRank());
                        if (listModelData.getMinRequired() > listModelData.getMaxRequired() && listModelData.getMaxRequired() > 0) {
                            throw new Exception("Min required set to more than max required:" + listModelData.getMinRequired() + ", " + listModelData.getMaxRequired());
                        }
                        totalMaxCount += listModelData.getMaxRequired();
                        if (listModelData.getMaxRequired() == 0) listWithMaxNotSetFound = true;
                    }
                    if (!listWithMaxNotSetFound && totalMaxCount < modelData.getTotalNumberOfApprovalsRequired()) {
                        throw new Exception("Total of max count is less than total required for quorum");
                    }
                }
            }
        }

    }

    public static void displayRuleTriggered(final RuleContext drools){
        logger.info("rule triggered: " + drools.getRule().getName());
    }

    private void setDefaultDisplayMetadata(){
        logger.info("No rule triggered based on inputs. Going to return an empty array.");
    }

    private KieServices kieServices;

    private void getKieRepository() {
        kieServices=KieServices.Factory.get();
        final KieRepository kieRepository = kieServices.getRepository();
        kieRepository.addKieModule(new KieModule() {
            public ReleaseId getReleaseId() {
                return kieRepository.getDefaultReleaseId();
            }
        });
    }

    StatelessKieSession getStatelessKieSession(byte[] rules){
        getKieRepository();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        kieFileSystem.write("src/main/resources/dafrules.drl", kieServices.getResources().newByteArrayResource(rules));

        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();

        return kieServices.newKieContainer(kieModule.getReleaseId()).newStatelessKieSession();
    }

}
