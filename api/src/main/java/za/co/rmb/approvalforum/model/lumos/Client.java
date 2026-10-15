package za.co.rmb.approvalforum.model.lumos;

import java.util.List;

public class Client {
    private int id;
    private boolean prospect;
    private String description;
    private boolean creditFund;
    private boolean primeBrokingClient;
    private boolean assetValueCorrelation;
    private String suggestedRatingModel;
    private boolean supervisorySlotting;
    private int highestCountryOfRiskId;
    private String countryGroup;
    private String portfolioCountryGroup;
    private int highestCountryOfOperationId;
    private int highestOperatingIndustryId;
    private int analystSectorTeam_Id;
    private boolean listedCompany;
    private int distressStatusId;
    private String distressStatusReason;
    private String borrowerType;
    private boolean governmentPreferentialWeighting;
    private String financialYearEnd;
    private int loanToValue;
    private int regulatoryAssetClass_Id;
    private int clientSegment_Id;
    private int intergroupType_Id;
    private boolean relatedToOtherFirstrandEntity;
    private int firstrandRelationshipType_Id;
    private int pd;
    private String rating;
    private double lgd;
    private List<IndustryContribution> industryContribution;
    private List<CountryContribution> countryOfRisk;
    private List<CountryContribution> countryOfOperation;
    private String status;
    private String recordStatus;
    private int industryVolatilityGroupActual_Id;
    private int industryVolatilityGroupRecommended_Id;
    private String industryVolatilityMotivation;
    private Integer lexClassification_Id;
    private String lexClassificationDate;
    private boolean lexFi;
    private boolean standalone;
    private String primaryCreditAnalyst;
    private String primaryCreditAnalystId;
    private String primaryCreditAnalystEmail;
    private String relationshipManager;
    private String relationshipManagerId;
    private String relationshipManagerEmail;
    private String sectorTeamLead;
    private String sectorTeamLeadId;
    private String sectorTeamLeadEmail;
    private String reviewAnalyst;
    private String reviewAnalystId;
    private String reviewAnalystEmail;
    private boolean distressRestructure;
    private String commencementDate;
    private String distressRestructureMeetingDate;
    private Integer financialInstitutionType_Id;
    private String lastUpdated;
    private int nonBankPortfolio_Id;
    private int nonBankPortfolioGrouping_Id;
    private boolean solvencyRegulatedInd;
    private String solvencyOverrideReason;
    private int financialInstitutionThreshold;
    private ClientFinancialAttributes clientFinancialAttributes;
    private String complianceIndicatorMs;
    private String complianceIndicatorMsMotivation;
    private Integer riskCategory_Id;
    private Integer segmentationCode_Id;
    private Integer commercialBusinessType_Id;
    private boolean isSensitiveIndustryOrOnExclusionList;
    private String relationshipAnalyst;
    private String relationshipAnalystId;
    private String relationshipAnalystEmail;
    private String creditManager;
    private String creditManagerId;
    private String creditManagerEmail;
    private String establishedDate;
    private String bankIndicator;
    private Integer csaCurrency_Id;
    private Double csaThresholdExistingAmount;
    private Double minimumTransferAmount;
    private Double independentAmount;
    private Double etpLimitAmount;
    private Double mtmAmount;
    private Double etpExposure;
    private Double variationMarginReceived;
    private Double independentAmountReceived;
    private Double netUtilization;
    private Double csaLimit;
    private int wholesalePortfolioId;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isProspect() {
        return prospect;
    }

    public void setProspect(boolean prospect) {
        this.prospect = prospect;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCreditFund() {
        return creditFund;
    }

    public void setCreditFund(boolean creditFund) {
        this.creditFund = creditFund;
    }

    public boolean isPrimeBrokingClient() {
        return primeBrokingClient;
    }

    public void setPrimeBrokingClient(boolean primeBrokingClient) {
        this.primeBrokingClient = primeBrokingClient;
    }

    public boolean isAssetValueCorrelation() {
        return assetValueCorrelation;
    }

    public void setAssetValueCorrelation(boolean assetValueCorrelation) {
        this.assetValueCorrelation = assetValueCorrelation;
    }

    public String getSuggestedRatingModel() {
        return suggestedRatingModel;
    }

    public void setSuggestedRatingModel(String suggestedRatingModel) {
        this.suggestedRatingModel = suggestedRatingModel;
    }

    public boolean isSupervisorySlotting() {
        return supervisorySlotting;
    }

    public void setSupervisorySlotting(boolean supervisorySlotting) {
        this.supervisorySlotting = supervisorySlotting;
    }

    public int getHighestCountryOfRiskId() {
        return highestCountryOfRiskId;
    }

    public void setHighestCountryOfRiskId(int highestCountryOfRiskId) {
        this.highestCountryOfRiskId = highestCountryOfRiskId;
    }

    public String getCountryGroup() {
        return countryGroup;
    }

    public void setCountryGroup(String countryGroup) {
        this.countryGroup = countryGroup;
    }

    public String getPortfolioCountryGroup() {
        return portfolioCountryGroup;
    }

    public void setPortfolioCountryGroup(String portfolioCountryGroup) {
        this.portfolioCountryGroup = portfolioCountryGroup;
    }

    public int getHighestCountryOfOperationId() {
        return highestCountryOfOperationId;
    }

    public void setHighestCountryOfOperationId(int highestCountryOfOperationId) {
        this.highestCountryOfOperationId = highestCountryOfOperationId;
    }

    public int getHighestOperatingIndustryId() {
        return highestOperatingIndustryId;
    }

    public void setHighestOperatingIndustryId(int highestOperatingIndustryId) {
        this.highestOperatingIndustryId = highestOperatingIndustryId;
    }

    public int getAnalystSectorTeam_Id() {
        return analystSectorTeam_Id;
    }

    public void setAnalystSectorTeam_Id(int analystSectorTeam_Id) {
        this.analystSectorTeam_Id = analystSectorTeam_Id;
    }

    public boolean isListedCompany() {
        return listedCompany;
    }

    public void setListedCompany(boolean listedCompany) {
        this.listedCompany = listedCompany;
    }

    public int getDistressStatusId() {
        return distressStatusId;
    }

    public void setDistressStatusId(int distressStatusId) {
        this.distressStatusId = distressStatusId;
    }

    public String getDistressStatusReason() {
        return distressStatusReason;
    }

    public void setDistressStatusReason(String distressStatusReason) {
        this.distressStatusReason = distressStatusReason;
    }

    public String getBorrowerType() {
        return borrowerType;
    }

    public void setBorrowerType(String borrowerType) {
        this.borrowerType = borrowerType;
    }

    public boolean isGovernmentPreferentialWeighting() {
        return governmentPreferentialWeighting;
    }

    public void setGovernmentPreferentialWeighting(boolean governmentPreferentialWeighting) {
        this.governmentPreferentialWeighting = governmentPreferentialWeighting;
    }

    public String getFinancialYearEnd() {
        return financialYearEnd;
    }

    public void setFinancialYearEnd(String financialYearEnd) {
        this.financialYearEnd = financialYearEnd;
    }

    public int getLoanToValue() {
        return loanToValue;
    }

    public void setLoanToValue(int loanToValue) {
        this.loanToValue = loanToValue;
    }

    public int getRegulatoryAssetClass_Id() {
        return regulatoryAssetClass_Id;
    }

    public void setRegulatoryAssetClass_Id(int regulatoryAssetClass_Id) {
        this.regulatoryAssetClass_Id = regulatoryAssetClass_Id;
    }

    public int getClientSegment_Id() {
        return clientSegment_Id;
    }

    public void setClientSegment_Id(int clientSegment_Id) {
        this.clientSegment_Id = clientSegment_Id;
    }

    public int getIntergroupType_Id() {
        return intergroupType_Id;
    }

    public void setIntergroupType_Id(int intergroupType_Id) {
        this.intergroupType_Id = intergroupType_Id;
    }

    public boolean isRelatedToOtherFirstrandEntity() {
        return relatedToOtherFirstrandEntity;
    }

    public void setRelatedToOtherFirstrandEntity(boolean relatedToOtherFirstrandEntity) {
        this.relatedToOtherFirstrandEntity = relatedToOtherFirstrandEntity;
    }

    public int getFirstrandRelationshipType_Id() {
        return firstrandRelationshipType_Id;
    }

    public void setFirstrandRelationshipType_Id(int firstrandRelationshipType_Id) {
        this.firstrandRelationshipType_Id = firstrandRelationshipType_Id;
    }

    public int getPd() {
        return pd;
    }

    public void setPd(int pd) {
        this.pd = pd;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public double getLgd() {
        return lgd;
    }

    public void setLgd(double lgd) {
        this.lgd = lgd;
    }

    public List<IndustryContribution> getIndustryContribution() {
        return industryContribution;
    }

    public void setIndustryContribution(List<IndustryContribution> industryContribution) {
        this.industryContribution = industryContribution;
    }

    public List<CountryContribution> getCountryOfRisk() {
        return countryOfRisk;
    }

    public void setCountryOfRisk(List<CountryContribution> countryOfRisk) {
        this.countryOfRisk = countryOfRisk;
    }

    public List<CountryContribution> getCountryOfOperation() {
        return countryOfOperation;
    }

    public void setCountryOfOperation(List<CountryContribution> countryOfOperation) {
        this.countryOfOperation = countryOfOperation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(String recordStatus) {
        this.recordStatus = recordStatus;
    }

    public int getIndustryVolatilityGroupActual_Id() {
        return industryVolatilityGroupActual_Id;
    }

    public void setIndustryVolatilityGroupActual_Id(int industryVolatilityGroupActual_Id) {
        this.industryVolatilityGroupActual_Id = industryVolatilityGroupActual_Id;
    }

    public int getIndustryVolatilityGroupRecommended_Id() {
        return industryVolatilityGroupRecommended_Id;
    }

    public void setIndustryVolatilityGroupRecommended_Id(int industryVolatilityGroupRecommended_Id) {
        this.industryVolatilityGroupRecommended_Id = industryVolatilityGroupRecommended_Id;
    }

    public String getIndustryVolatilityMotivation() {
        return industryVolatilityMotivation;
    }

    public void setIndustryVolatilityMotivation(String industryVolatilityMotivation) {
        this.industryVolatilityMotivation = industryVolatilityMotivation;
    }

    public Integer getLexClassification_Id() {
        return lexClassification_Id;
    }

    public void setLexClassification_Id(Integer lexClassification_Id) {
        this.lexClassification_Id = lexClassification_Id;
    }

    public String getLexClassificationDate() {
        return lexClassificationDate;
    }

    public void setLexClassificationDate(String lexClassificationDate) {
        this.lexClassificationDate = lexClassificationDate;
    }

    public boolean isLexFi() {
        return lexFi;
    }

    public void setLexFi(boolean lexFi) {
        this.lexFi = lexFi;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
    }

    public String getPrimaryCreditAnalyst() {
        return primaryCreditAnalyst;
    }

    public void setPrimaryCreditAnalyst(String primaryCreditAnalyst) {
        this.primaryCreditAnalyst = primaryCreditAnalyst;
    }

    public String getPrimaryCreditAnalystId() {
        return primaryCreditAnalystId;
    }

    public void setPrimaryCreditAnalystId(String primaryCreditAnalystId) {
        this.primaryCreditAnalystId = primaryCreditAnalystId;
    }

    public String getPrimaryCreditAnalystEmail() {
        return primaryCreditAnalystEmail;
    }

    public void setPrimaryCreditAnalystEmail(String primaryCreditAnalystEmail) {
        this.primaryCreditAnalystEmail = primaryCreditAnalystEmail;
    }

    public String getRelationshipManager() {
        return relationshipManager;
    }

    public void setRelationshipManager(String relationshipManager) {
        this.relationshipManager = relationshipManager;
    }

    public String getRelationshipManagerId() {
        return relationshipManagerId;
    }

    public void setRelationshipManagerId(String relationshipManagerId) {
        this.relationshipManagerId = relationshipManagerId;
    }

    public String getRelationshipManagerEmail() {
        return relationshipManagerEmail;
    }

    public void setRelationshipManagerEmail(String relationshipManagerEmail) {
        this.relationshipManagerEmail = relationshipManagerEmail;
    }

    public String getSectorTeamLead() {
        return sectorTeamLead;
    }

    public void setSectorTeamLead(String sectorTeamLead) {
        this.sectorTeamLead = sectorTeamLead;
    }

    public String getSectorTeamLeadId() {
        return sectorTeamLeadId;
    }

    public void setSectorTeamLeadId(String sectorTeamLeadId) {
        this.sectorTeamLeadId = sectorTeamLeadId;
    }

    public String getSectorTeamLeadEmail() {
        return sectorTeamLeadEmail;
    }

    public void setSectorTeamLeadEmail(String sectorTeamLeadEmail) {
        this.sectorTeamLeadEmail = sectorTeamLeadEmail;
    }

    public String getReviewAnalyst() {
        return reviewAnalyst;
    }

    public void setReviewAnalyst(String reviewAnalyst) {
        this.reviewAnalyst = reviewAnalyst;
    }

    public String getReviewAnalystId() {
        return reviewAnalystId;
    }

    public void setReviewAnalystId(String reviewAnalystId) {
        this.reviewAnalystId = reviewAnalystId;
    }

    public String getReviewAnalystEmail() {
        return reviewAnalystEmail;
    }

    public void setReviewAnalystEmail(String reviewAnalystEmail) {
        this.reviewAnalystEmail = reviewAnalystEmail;
    }

    public boolean isDistressRestructure() {
        return distressRestructure;
    }

    public void setDistressRestructure(boolean distressRestructure) {
        this.distressRestructure = distressRestructure;
    }

    public String getCommencementDate() {
        return commencementDate;
    }

    public void setCommencementDate(String commencementDate) {
        this.commencementDate = commencementDate;
    }

    public String getDistressRestructureMeetingDate() {
        return distressRestructureMeetingDate;
    }

    public void setDistressRestructureMeetingDate(String distressRestructureMeetingDate) {
        this.distressRestructureMeetingDate = distressRestructureMeetingDate;
    }

    public Integer getFinancialInstitutionType_Id() {
        return financialInstitutionType_Id;
    }

    public void setFinancialInstitutionType_Id(Integer financialInstitutionType_Id) {
        this.financialInstitutionType_Id = financialInstitutionType_Id;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getNonBankPortfolio_Id() {
        return nonBankPortfolio_Id;
    }

    public void setNonBankPortfolio_Id(int nonBankPortfolio_Id) {
        this.nonBankPortfolio_Id = nonBankPortfolio_Id;
    }

    public int getNonBankPortfolioGrouping_Id() {
        return nonBankPortfolioGrouping_Id;
    }

    public void setNonBankPortfolioGrouping_Id(int nonBankPortfolioGrouping_Id) {
        this.nonBankPortfolioGrouping_Id = nonBankPortfolioGrouping_Id;
    }

    public boolean isSolvencyRegulatedInd() {
        return solvencyRegulatedInd;
    }

    public void setSolvencyRegulatedInd(boolean solvencyRegulatedInd) {
        this.solvencyRegulatedInd = solvencyRegulatedInd;
    }

    public String getSolvencyOverrideReason() {
        return solvencyOverrideReason;
    }

    public void setSolvencyOverrideReason(String solvencyOverrideReason) {
        this.solvencyOverrideReason = solvencyOverrideReason;
    }

    public int getFinancialInstitutionThreshold() {
        return financialInstitutionThreshold;
    }

    public void setFinancialInstitutionThreshold(int financialInstitutionThreshold) {
        this.financialInstitutionThreshold = financialInstitutionThreshold;
    }

    public ClientFinancialAttributes getClientFinancialAttributes() {
        return clientFinancialAttributes;
    }

    public void setClientFinancialAttributes(ClientFinancialAttributes clientFinancialAttributes) {
        this.clientFinancialAttributes = clientFinancialAttributes;
    }

    public String getComplianceIndicatorMs() {
        return complianceIndicatorMs;
    }

    public void setComplianceIndicatorMs(String complianceIndicatorMs) {
        this.complianceIndicatorMs = complianceIndicatorMs;
    }

    public String getComplianceIndicatorMsMotivation() {
        return complianceIndicatorMsMotivation;
    }

    public void setComplianceIndicatorMsMotivation(String complianceIndicatorMsMotivation) {
        this.complianceIndicatorMsMotivation = complianceIndicatorMsMotivation;
    }

    public Integer getRiskCategory_Id() {
        return riskCategory_Id;
    }

    public void setRiskCategory_Id(Integer riskCategory_Id) {
        this.riskCategory_Id = riskCategory_Id;
    }

    public Integer getSegmentationCode_Id() {
        return segmentationCode_Id;
    }

    public void setSegmentationCode_Id(Integer segmentationCode_Id) {
        this.segmentationCode_Id = segmentationCode_Id;
    }

    public Integer getCommercialBusinessType_Id() {
        return commercialBusinessType_Id;
    }

    public void setCommercialBusinessType_Id(Integer commercialBusinessType_Id) {
        this.commercialBusinessType_Id = commercialBusinessType_Id;
    }

    public boolean isSensitiveIndustryOrOnExclusionList() {
        return isSensitiveIndustryOrOnExclusionList;
    }

    public void setSensitiveIndustryOrOnExclusionList(boolean sensitiveIndustryOrOnExclusionList) {
        isSensitiveIndustryOrOnExclusionList = sensitiveIndustryOrOnExclusionList;
    }

    public String getRelationshipAnalyst() {
        return relationshipAnalyst;
    }

    public void setRelationshipAnalyst(String relationshipAnalyst) {
        this.relationshipAnalyst = relationshipAnalyst;
    }

    public String getRelationshipAnalystId() {
        return relationshipAnalystId;
    }

    public void setRelationshipAnalystId(String relationshipAnalystId) {
        this.relationshipAnalystId = relationshipAnalystId;
    }

    public String getRelationshipAnalystEmail() {
        return relationshipAnalystEmail;
    }

    public void setRelationshipAnalystEmail(String relationshipAnalystEmail) {
        this.relationshipAnalystEmail = relationshipAnalystEmail;
    }

    public String getCreditManager() {
        return creditManager;
    }

    public void setCreditManager(String creditManager) {
        this.creditManager = creditManager;
    }

    public String getCreditManagerId() {
        return creditManagerId;
    }

    public void setCreditManagerId(String creditManagerId) {
        this.creditManagerId = creditManagerId;
    }

    public String getCreditManagerEmail() {
        return creditManagerEmail;
    }

    public void setCreditManagerEmail(String creditManagerEmail) {
        this.creditManagerEmail = creditManagerEmail;
    }

    public String getEstablishedDate() {
        return establishedDate;
    }

    public void setEstablishedDate(String establishedDate) {
        this.establishedDate = establishedDate;
    }

    public String getBankIndicator() {
        return bankIndicator;
    }

    public void setBankIndicator(String bankIndicator) {
        this.bankIndicator = bankIndicator;
    }

    public Integer getCsaCurrency_Id() {
        return csaCurrency_Id;
    }

    public void setCsaCurrency_Id(Integer csaCurrency_Id) {
        this.csaCurrency_Id = csaCurrency_Id;
    }

    public Double getCsaThresholdExistingAmount() {
        return csaThresholdExistingAmount;
    }

    public void setCsaThresholdExistingAmount(Double csaThresholdExistingAmount) {
        this.csaThresholdExistingAmount = csaThresholdExistingAmount;
    }

    public Double getMinimumTransferAmount() {
        return minimumTransferAmount;
    }

    public void setMinimumTransferAmount(Double minimumTransferAmount) {
        this.minimumTransferAmount = minimumTransferAmount;
    }

    public Double getIndependentAmount() {
        return independentAmount;
    }

    public void setIndependentAmount(Double independentAmount) {
        this.independentAmount = independentAmount;
    }

    public Double getEtpLimitAmount() {
        return etpLimitAmount;
    }

    public void setEtpLimitAmount(Double etpLimitAmount) {
        this.etpLimitAmount = etpLimitAmount;
    }

    public Double getMtmAmount() {
        return mtmAmount;
    }

    public void setMtmAmount(Double mtmAmount) {
        this.mtmAmount = mtmAmount;
    }

    public Double getEtpExposure() {
        return etpExposure;
    }

    public void setEtpExposure(Double etpExposure) {
        this.etpExposure = etpExposure;
    }

    public Double getVariationMarginReceived() {
        return variationMarginReceived;
    }

    public void setVariationMarginReceived(Double variationMarginReceived) {
        this.variationMarginReceived = variationMarginReceived;
    }

    public Double getIndependentAmountReceived() {
        return independentAmountReceived;
    }

    public void setIndependentAmountReceived(Double independentAmountReceived) {
        this.independentAmountReceived = independentAmountReceived;
    }

    public Double getNetUtilization() {
        return netUtilization;
    }

    public void setNetUtilization(Double netUtilization) {
        this.netUtilization = netUtilization;
    }

    public Double getCsaLimit() {
        return csaLimit;
    }

    public void setCsaLimit(Double csaLimit) {
        this.csaLimit = csaLimit;
    }

    public int getWholesalePortfolioId() {
        return wholesalePortfolioId;
    }

    public void setWholesalePortfolioId(int wholesalePortfolioId) {
        this.wholesalePortfolioId = wholesalePortfolioId;
    }
}
