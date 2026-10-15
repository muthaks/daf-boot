package za.co.rmb.approvalforumdrools.service;

public interface ApprovalForumDroolsService {

    String getRules();

    void updateRules(String rules, String user, String fileName, String md5Sum);

    boolean hasFileBeenLoadedPreviously(String fileName);

}
