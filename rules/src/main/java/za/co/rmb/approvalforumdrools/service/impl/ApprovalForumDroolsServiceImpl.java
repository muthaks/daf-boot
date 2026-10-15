package za.co.rmb.approvalforumdrools.service.impl;

import jakarta.inject.Inject;
import za.co.rmb.approvalforumdrools.data.ApprovalForumDroolsDataRepository;
import za.co.rmb.approvalforumdrools.data.entity.ApprovalForumRRulesEntity;
import za.co.rmb.approvalforumdrools.service.ApprovalForumDroolsService;

import java.util.Calendar;
import java.util.List;

public class ApprovalForumDroolsServiceImpl implements ApprovalForumDroolsService {

    private ApprovalForumDroolsDataRepository dataRepository;

    @Inject
    public ApprovalForumDroolsServiceImpl(ApprovalForumDroolsDataRepository dataRepository){
        this.dataRepository = dataRepository;
    }

    @Override
    public String getRules() {
        ApprovalForumRRulesEntity entity = dataRepository.getActiveRules();
        if(entity == null) return "";
        return entity.getRules();
    }

    @Override
    public void updateRules(String rules, String user, String fileName, String md5Sum) {
        ApprovalForumRRulesEntity entity = dataRepository.getActiveRules();
        entity.setActive(false);
        dataRepository.updateRules(entity);
        entity = new ApprovalForumRRulesEntity();
        entity.setActive(true);
        entity.setCreatedBy(user);
        entity.setRules(rules);
        entity.setFileName(fileName);
        entity.setMd5sum(md5Sum);
        entity.setCreatedOn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
        dataRepository.persistRules(entity);
    }

    @Override
    public boolean hasFileBeenLoadedPreviously(String fileName) {
        List<ApprovalForumRRulesEntity> list = dataRepository.getRulesByFileName(fileName);
        return list != null && list.size() > 0;
    }

}
