package za.co.rmb.approvalforum.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.rmb.approvalforum.model.FacilityModel;
import za.co.rmb.approvalforum.model.FacilitySummaryModel;
import za.co.rmb.approvalforum.service.impl.ApprovalForumExternalServiceImpl;

import java.util.List;

public abstract class ApprovalForumServiceUtil {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalForumExternalServiceImpl.class);

    protected FacilitySummaryModel getFacilitySummary(List<FacilityModel> facilityModels) {
        FacilitySummaryModel summaryModel = new FacilitySummaryModel();
        for(FacilityModel facilityModel: facilityModels){
            if(!"Settlement".equalsIgnoreCase(facilityModel.getRiskType()) && ("Credit Hold".equalsIgnoreCase(facilityModel.getFacilityInvestmentType()) ||
                    "Bridge".equalsIgnoreCase(facilityModel.getFacilityInvestmentType()))){
                if("Submitted for Approval".equalsIgnoreCase(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedDcp(summaryModel.getProposedDcp()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedDcp(summaryModel.getApprovedDcp()+facilityModel.getNetLimitZar());
                }
            }
            if("Settlement".equalsIgnoreCase(facilityModel.getRiskType()) && "Credit Hold".equalsIgnoreCase(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equalsIgnoreCase(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedSettle(summaryModel.getProposedSettle()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedSettle(summaryModel.getApprovedSettle()+facilityModel.getNetLimitZar());
                }
            }
            if("Underwriting".equalsIgnoreCase(facilityModel.getFacilityInvestmentType()) ||
                    "Credit Imposed Sell Down".equalsIgnoreCase(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equalsIgnoreCase(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedUnderwritingAndCredit(summaryModel.getProposedUnderwritingAndCredit()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedUnderwritingAndCredit(summaryModel.getApprovedUnderwritingAndCredit()+facilityModel.getNetLimitZar());
                }
            }
            if("Churn".equalsIgnoreCase(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equalsIgnoreCase(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedChurn(summaryModel.getProposedChurn()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedChurn(summaryModel.getApprovedChurn()+facilityModel.getNetLimitZar());
                }
            }
            if("Credit Fund".equalsIgnoreCase(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equalsIgnoreCase(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedCreditFund(summaryModel.getProposedCreditFund()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedCreditFund(summaryModel.getApprovedCreditFund()+facilityModel.getNetLimitZar());
                }
            }
            if("Traded Credit".equalsIgnoreCase(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equalsIgnoreCase(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedTradedCredit(summaryModel.getProposedTradedCredit()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedTradedCredit(summaryModel.getApprovedTradedCredit()+facilityModel.getNetLimitZar());
                }
            }
        }
        return summaryModel;
    }

    protected FacilitySummaryModel getAshburtonFacilitySummary(List<FacilityModel> facilityModels) {
        FacilitySummaryModel summaryModel = new FacilitySummaryModel();
        for(FacilityModel facilityModel: facilityModels){
            logger.info("getAshburtonFacilitySummary({})", facilityModel.getCreditApplicationStatus());
            if(!"Settlement".equals(facilityModel.getRiskType()) && ("Credit Hold".equals(facilityModel.getFacilityInvestmentType()) ||
                    "Bridge".equals(facilityModel.getFacilityInvestmentType()))){
                if("Submitted for Approval".equals(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedDcp(summaryModel.getProposedDcp()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedDcp(summaryModel.getApprovedDcp()+facilityModel.getNetLimitZar());
                }
            }
            if("Settlement".equals(facilityModel.getRiskType()) && "Credit Hold".equals(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equals(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedSettle(summaryModel.getProposedSettle()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedSettle(summaryModel.getApprovedSettle()+facilityModel.getNetLimitZar());
                }
            }
            if("Underwriting".equals(facilityModel.getFacilityInvestmentType()) ||
                    "Credit Imposed Sell Down".equals(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equals(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedUnderwritingAndCredit(summaryModel.getProposedUnderwritingAndCredit()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedUnderwritingAndCredit(summaryModel.getApprovedUnderwritingAndCredit()+facilityModel.getNetLimitZar());
                }
            }
            if("Churn".equals(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equals(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedChurn(summaryModel.getProposedChurn()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedChurn(summaryModel.getApprovedChurn()+facilityModel.getNetLimitZar());
                }
            }
            if("Credit Fund".equals(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equals(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedCreditFund(summaryModel.getProposedCreditFund()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedCreditFund(summaryModel.getApprovedCreditFund()+facilityModel.getNetLimitZar());
                }
            }
            if("Traded Credit".equals(facilityModel.getFacilityInvestmentType())){
                if("Submitted for Approval".equals(facilityModel.getCreditApplicationStatus())){
                    summaryModel.setProposedTradedCredit(summaryModel.getProposedTradedCredit()+facilityModel.getNetLimitZar());
                }else{
                    summaryModel.setApprovedTradedCredit(summaryModel.getApprovedTradedCredit()+facilityModel.getNetLimitZar());
                }
            }
        }
        logger.info("getAshburtonFacilitySummary(END)");
        return summaryModel;
    }
}
