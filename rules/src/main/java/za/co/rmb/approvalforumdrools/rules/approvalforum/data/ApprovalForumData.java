package za.co.rmb.approvalforumdrools.rules.approvalforum.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApprovalForumData implements Serializable{

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumData.class);

    public ApprovalForumData(){
        setRuleExecuted(false);
        setDefaultData();
    }

    private int idx;

    private boolean ruleExecuted;

    public boolean isRuleExecuted() {
	    return ruleExecuted;
    }

    public void setRuleExecuted(boolean ruleExecuted) {
	    this.ruleExecuted = ruleExecuted;
    }

    //Input fields
    private String meetingRule;
    private String proposalType;
    private String distressStatus;
    private boolean bank;
    private String fit;
    private List<String> riskTakerGroupCode;
    private Map<String,String> facilityMovement;

    public String getMeetingRule() {
        return meetingRule;
    }

    public void setMeetingRule(String meetingRule) {
        this.meetingRule = meetingRule;
    }

    public String getProposalType() {
        return proposalType;
    }

    public void setProposalType(String proposalType) {
        this.proposalType = proposalType;
    }

    public String getDistressStatus() {
        return distressStatus;
    }

    public void setDistressStatus(String distressStatus) {
        this.distressStatus = distressStatus;
    }

    public boolean isBank() {
        return bank;
    }

    public void setBank(boolean bank) {
        this.bank = bank;
    }

    public String getFit() {
        return fit;
    }

    public void setFit(String fit) {
        this.fit = fit;
    }

    public List<String> getRiskTakerGroupCode() {
        return riskTakerGroupCode;
    }

    public void setRiskTakerGroupCode(List<String> riskTakerGroupCode) {
        this.riskTakerGroupCode = riskTakerGroupCode;
    }

    public Map<String, String> getFacilityMovement() {
        return facilityMovement;
    }

    public void setFacilityMovement(Map<String, String> facilityMovement) {
        this.facilityMovement = facilityMovement;
    }

    private void setDefaultData(){
        idx = 0;
        setApprovalForumModelDataList(new ArrayList<>());
        getApprovalForumModelDataList().add(new ApprovalForumModelData());
        setBank(false);
        setFit("");
        setRiskTakerGroupCode(new ArrayList<>());
    }

    //Output fields
    private List<ApprovalForumModelData> approvalForumModelDataList;

    public List<ApprovalForumModelData> getApprovalForumModelDataList() {
        return approvalForumModelDataList;
    }

    public void setApprovalForumModelDataList(List<ApprovalForumModelData> approvalForumModelDataList) {
        this.approvalForumModelDataList = approvalForumModelDataList;
    }

    public void setTotalNumberOfApprovalsRequired(int total){
        getApprovalForumModelDataList().get(idx).setTotalNumberOfApprovalsRequired(total);
    }

    public void addTotalNumberOfApprovalsRequired(int total){
        getApprovalForumModelDataList().get(idx).setTotalNumberOfApprovalsRequired(
                getApprovalForumModelDataList().get(idx).getTotalNumberOfApprovalsRequired()+total);
    }

    public void addTotalNumberOfApprovalsRequiredToAll(int total){
        for(ApprovalForumModelData data: getApprovalForumModelDataList()) {
            data.setTotalNumberOfApprovalsRequired(data.getTotalNumberOfApprovalsRequired() + total);
        }
    }

    public void addListModel(String listName, int minRequired, int maxRequired, int rank){
        validateMinMax(minRequired, maxRequired);
        getApprovalForumModelDataList().get(idx).addToList(new ApprovalForumListModelData(listName, minRequired, maxRequired, rank));
    }

    public void addListModelNextRank(String listName, int minRequired, int maxRequired){
        validateMinMax(minRequired, maxRequired);
        int lastRank = 1;
        if(getApprovalForumModelDataList().get(idx).getListModelData().size() > 0){
            lastRank = getApprovalForumModelDataList().get(idx).getListModelData().get(
                    getApprovalForumModelDataList().get(idx).getListModelData().size()-1).getRank()+1;
        }
        getApprovalForumModelDataList().get(idx).addToList(new ApprovalForumListModelData(listName, minRequired, maxRequired, lastRank));
    }

    private void validateMinMax(int min, int max){
        if(min > 0 && (max != min && max != 0)){
            throw new RuntimeException("Invalid min/max combination. Max must be zero if not equal to min");
        }
        if(min == 0 && max == 0){
            throw new RuntimeException("Invalid min/max combination. Max must be greater than zero min is zero");
        }
    }

    public void popListModelToHighestRank(String listName, int minRequired, int maxRequired){
        for(ApprovalForumModelData data: getApprovalForumModelDataList()) {
            if(data.getListModelData() != null && !data.getListModelData().isEmpty()){
                List<ApprovalForumListModelData> newList = new ArrayList<>();
                newList.add(new ApprovalForumListModelData(listName, minRequired, maxRequired, 1));
                newList.addAll(data.getListModelData());
                data.setListModelData(newList);
            }else {
                if(data.getListModelData() == null){
                    data.setListModelData(Collections.singletonList(new ApprovalForumListModelData(listName, minRequired, maxRequired, 1)));
                }else {
                    data.addToList(new ApprovalForumListModelData(listName, minRequired, maxRequired, 1));
                }
            }
            for(ApprovalForumListModelData modelData: data.getListModelData()){
                if(!listName.equalsIgnoreCase(modelData.getListName())) {
                    modelData.setRank(modelData.getRank() + 1);
                }
            }
        }
    }

    public void addNewModel(){
        idx++;
        getApprovalForumModelDataList().add(new ApprovalForumModelData());
    }

    public boolean isRiskTakerGroupCode(String riskTakerGroupCode){
        return getRiskTakerGroupCode().contains(riskTakerGroupCode);
    }

    public boolean isAnnualReview(){
        return getProposalType().equals("Annual Review");
    }

    public boolean isFacilityMovementChanged(String riskTakerGroupCode){

        String movement = facilityMovement.get(riskTakerGroupCode);

        if(movement != null && movement.equals("Changed")){
            return true;
        }
        return false;
    }

    /**
     * Auto-approval rule based on the meetingRule prefix.
     * If meetingRule starts with 'DELEGATED_AUTHORITY_4'.
     */
    public void applyAutoApprovalRules() {
        String rule = getMeetingRule();

        if (rule != null && rule.startsWith("DELEGATED_AUTHORITY_4")) {
            for (ApprovalForumModelData data : getApprovalForumModelDataList()) {
                data.getTotalNumberOfApprovalsRequired();
            }

            logger.info("Auto-approval applied for meetingRule starting with 'DELEGATED_AUTHORITY_4'");
        }
    }

    public class ApprovalForumModelData {

        public ApprovalForumModelData(){
            setTotalNumberOfApprovalsRequired(0);
            setListModelData(new ArrayList<>());
        }

        private int totalNumberOfApprovalsRequired;
        private List<ApprovalForumListModelData> listModelData;

        public int getTotalNumberOfApprovalsRequired() {
            return totalNumberOfApprovalsRequired;
        }

        public void setTotalNumberOfApprovalsRequired(int totalNumberOfApprovalsRequired) {
            this.totalNumberOfApprovalsRequired = totalNumberOfApprovalsRequired;
        }

        public List<ApprovalForumListModelData> getListModelData() {
            return listModelData;
        }

        public void setListModelData(List<ApprovalForumListModelData> listModelData) {
            this.listModelData = listModelData;
        }

        public void addToList(ApprovalForumListModelData listModelData){
            getListModelData().add(listModelData);
        }

    }

    public class ApprovalForumListModelData {

        private ApprovalForumListModelData(){
            setListName("");
            setMinRequired(0);
            setMaxRequired(0);
            setRank(0);
        }

        private ApprovalForumListModelData(String listName, int minRequired, int maxRequired, int rank){
            setListName(listName);
            setMinRequired(minRequired);
            setMaxRequired(maxRequired);
            setRank(rank);
        }

        private String listName;
        private int minRequired;
        private int maxRequired;
        private int rank;

        public String getListName() {
            return listName;
        }

        public void setListName(String listName) {
            this.listName = listName;
        }

        public int getMinRequired() {
            return minRequired;
        }

        public void setMinRequired(int minRequired) {
            this.minRequired = minRequired;
        }

        public int getMaxRequired() {
            return maxRequired;
        }

        public void setMaxRequired(int maxRequired) {
            this.maxRequired = maxRequired;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

    }

}
