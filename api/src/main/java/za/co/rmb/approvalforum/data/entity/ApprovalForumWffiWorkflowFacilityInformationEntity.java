package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFFI_WorkflowFacilityInformation")
public class ApprovalForumWffiWorkflowFacilityInformationEntity {

    private int wffiId;
    private String facilityReference;
    private String riskType;
    private String tenor;
    private String currency;
    private double netLimit;
    private double netLimitZar;
    private String facilityDescription;
    private String creditApplicationStatus;
    private String riskTaker;
    private boolean dealSpecific;
    private String proposedRating;
    private double lgd;
    private double lgdZar;
    private String facilityId;
    private String facilityRating;
    private String facilityInvestmentType;
    private String facilityMovementType;
    private String riskTakerGroupCode;

    private ApprovalForumWfcdWorkflowCounterpartyDetailEntity wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId;

    @Id
    @Column(name = "WFFI_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWffiId() {
        return wffiId;
    }

    public void setWffiId(int wffiId) {
        this.wffiId = wffiId;
    }

    @Basic
    @Column(name = "FacilityReference", nullable = true, length = 1024)
    public String getFacilityReference() {
        return facilityReference;
    }

    public void setFacilityReference(String facilityReference) {
        this.facilityReference = facilityReference;
    }

    @Basic
    @Column(name = "RiskType", nullable = false, length = 50)
    public String getRiskType() {
        return riskType;
    }

    public void setRiskType(String riskType) {
        this.riskType = riskType;
    }

    @Basic
    @Column(name = "Tenor", nullable = false, length = 30)
    public String getTenor() {
        return tenor;
    }

    public void setTenor(String tenor) {
        this.tenor = tenor;
    }

    @Basic
    @Column(name = "Currency", nullable = false, length = 50)
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Basic
    @Column(name = "NetLimit", nullable = false, precision = 0)
    public double getNetLimit() {
        return netLimit;
    }

    public void setNetLimit(double netLimit) {
        this.netLimit = netLimit;
    }

    @Basic
    @Column(name = "FacilityDescription", nullable = true, length = 1024)
    public String getFacilityDescription() {
        return facilityDescription;
    }

    public void setFacilityDescription(String facilityDescription) {
        this.facilityDescription = facilityDescription;
    }

    @Basic
    @Column(name = "CreditApplicationStatus", nullable = false, length = 50)
    public String getCreditApplicationStatus() {
        return creditApplicationStatus;
    }

    public void setCreditApplicationStatus(String creditApplicationStatus) {
        this.creditApplicationStatus = creditApplicationStatus;
    }

    @Basic
    @Column(name = "RiskTaker", nullable = false, length = 250)
    public String getRiskTaker() {
        return riskTaker;
    }

    public void setRiskTaker(String riskTaker) {
        this.riskTaker = riskTaker;
    }

    @Basic
    @Column(name = "DealSpecific", nullable = false)
    public boolean isDealSpecific() {
        return dealSpecific;
    }

    public void setDealSpecific(boolean dealSpecific) {
        this.dealSpecific = dealSpecific;
    }

    @Basic
    @Column(name = "ProposedRating", nullable = true, length = 50)
    public String getProposedRating() {
        return proposedRating;
    }

    public void setProposedRating(String proposedRating) {
        this.proposedRating = proposedRating;
    }

    @Basic
    @Column(name = "Lgd", nullable = false, precision = 0)
    public double getLgd() {
        return lgd;
    }

    public void setLgd(double lgd) {
        this.lgd = lgd;
    }

    @Basic
    @Column(name = "FacilityId", nullable = false, length = 50)
    public String getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    @Basic
    @Column(name = "FacilityRating", nullable = true, length = 30)
    public String getFacilityRating() {
        return facilityRating;
    }

    public void setFacilityRating(String facilityRating) {
        this.facilityRating = facilityRating;
    }

    @Basic
    @Column(name = "FacilityInvestmentType", nullable = true, length = 250)
    public String getFacilityInvestmentType() {
        return facilityInvestmentType;
    }

    public void setFacilityInvestmentType(String facilityInvestmentType) {
        this.facilityInvestmentType = facilityInvestmentType;
    }

    @Basic
    @Column(name = "facilityMovementType", nullable = true, length = 250)
    public String getFacilityMovementType() {
        return facilityMovementType;
    }

    public void setFacilityMovementType(String facilityInvestmentType) {
        this.facilityMovementType = facilityInvestmentType;
    }

    @Basic
    @Column(name = "NetLimitZar", nullable = false, precision = 0)
    public double getNetLimitZar() {
        return netLimitZar;
    }

    public void setNetLimitZar(double netLimitZar) {
        this.netLimitZar = netLimitZar;
    }

    @Basic
    @Column(name = "LgdZar", nullable = false, precision = 0)
    public double getLgdZar() {
        return lgdZar;
    }

    public void setLgdZar(double lgdZar) {
        this.lgdZar = lgdZar;
    }

    @Basic
    @Column(name = "RiskTakerGroupCode", nullable = true, length = 3)
    public String getRiskTakerGroupCode() {
        return riskTakerGroupCode;
    }

    public void setRiskTakerGroupCode(String riskTakerGroupingCode) {
        this.riskTakerGroupCode = riskTakerGroupingCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWffiWorkflowFacilityInformationEntity that = (ApprovalForumWffiWorkflowFacilityInformationEntity) o;
        return wffiId == that.wffiId &&
                Double.compare(that.netLimit, netLimit) == 0 &&
                dealSpecific == that.dealSpecific &&
                Double.compare(that.lgd, lgd) == 0 &&
                Double.compare(that.lgdZar, lgdZar) == 0 &&
                Double.compare(that.netLimitZar, netLimitZar) == 0 &&
                Objects.equals(facilityReference, that.facilityReference) &&
                Objects.equals(riskType, that.riskType) &&
                Objects.equals(tenor, that.tenor) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(facilityDescription, that.facilityDescription) &&
                Objects.equals(creditApplicationStatus, that.creditApplicationStatus) &&
                Objects.equals(riskTaker, that.riskTaker) &&
                Objects.equals(proposedRating, that.proposedRating) &&
                Objects.equals(facilityId, that.facilityId) &&
                Objects.equals(facilityRating, that.facilityRating) &&
                Objects.equals(facilityInvestmentType, that.facilityInvestmentType) &&
                Objects.equals(facilityMovementType, that.facilityMovementType) &&
                Objects.equals(riskTakerGroupCode, that.riskTakerGroupCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wffiId, facilityReference, riskType, tenor, riskTakerGroupCode, currency, netLimit, facilityDescription, creditApplicationStatus,
            riskTaker, dealSpecific, proposedRating, lgd, facilityId, facilityRating, facilityInvestmentType, facilityMovementType,netLimitZar, lgd);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WFCD_ID", referencedColumnName = "WFCD_ID", nullable = false)
    public ApprovalForumWfcdWorkflowCounterpartyDetailEntity getWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId() {
        return wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId;
    }

    public void setWfcdApprovalForumWorkflowCounterpartyDetailByWfcdId(ApprovalForumWfcdWorkflowCounterpartyDetailEntity wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId) {
        this.wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId = wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId;
    }

}
