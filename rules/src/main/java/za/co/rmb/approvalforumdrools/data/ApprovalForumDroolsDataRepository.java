package za.co.rmb.approvalforumdrools.data;

import za.co.rmb.approvalforumdrools.data.entity.ApprovalForumRRulesEntity;

import java.util.List;

public interface ApprovalForumDroolsDataRepository {

    ApprovalForumRRulesEntity getActiveRules();

    void updateRules(ApprovalForumRRulesEntity entity);

    void persistRules(ApprovalForumRRulesEntity entity);

    List<ApprovalForumRRulesEntity> getRulesByFileName(String fileName);

}
