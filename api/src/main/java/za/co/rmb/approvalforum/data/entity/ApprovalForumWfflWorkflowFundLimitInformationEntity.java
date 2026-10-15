package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFFL_WorkflowFundLimit")
public class ApprovalForumWfflWorkflowFundLimitInformationEntity {

    private int wfflId;
    private Integer wffappId;
    private double cptyLimit;
    private double convertedEquityLimit;
    private double governmentGuaranteeLimit;
    private double juniorDebtLimit;
    private double liquidationLimit;
    private double mezzanineDebtLimit;
    private double preferredSharesLimit;
    private double seniorSecuredLimit;
    private double seniorUnsecuredLimit;
    private double subordinatedSecuredLimit;
    private double subordinatedUnsecuredLimit;
    private double ldiFund;
    private double fixedIncomeFund;
    private double creditFund;
    private double multiAssetFund;
    private double multiManagerFund;
    private Timestamp createdOn;

    @Id
    @Column(name = "WFFL_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfflId() {
        return wfflId;
    }

    public void setWfflId(int wfflId) {
        this.wfflId = wfflId;
    }
    @Basic
    @Column(name = "WFAPP_ID", nullable = false)
    public Integer getWffappId() {
        return wffappId;
    }

    public void setWffappId(Integer wffappId) {
        this.wffappId = wffappId;
    }
    @Basic
    @Column(name = "Counterparty_Limit", nullable = false, precision = 0)
    public double getCptyLimit() {
        return cptyLimit;
    }

    public void setCptyLimit(double cptyLimit) {
        this.cptyLimit = cptyLimit;
    }
    @Basic
    @Column(name = "Converted_Equity_Limit", nullable = false, precision = 0)
    public double getConvertedEquityLimit() {
        return convertedEquityLimit;
    }

    public void setConvertedEquityLimit(double convertedEquityLimit) {
        this.convertedEquityLimit = convertedEquityLimit;
    }
    @Basic
    @Column(name = "Goverment_Guarantee_Limit", nullable = false, precision = 0)
    public double getGovernmentGuaranteeLimit() {
        return governmentGuaranteeLimit;
    }

    public void setGovernmentGuaranteeLimit(double governmentGuaranteeLimit) {
        this.governmentGuaranteeLimit = governmentGuaranteeLimit;
    }
    @Basic
    @Column(name = "Junior_Debt_Limit", nullable = false, precision = 0)
    public double getJuniorDebtLimit() {
        return juniorDebtLimit;
    }

    public void setJuniorDebtLimit(double juniorDebtLimit) {
        this.juniorDebtLimit = juniorDebtLimit;
    }
    @Basic
    @Column(name = "Liquidation_Limit", nullable = false, precision = 0)
    public double getLiquidationLimit() {
        return liquidationLimit;
    }

    public void setLiquidationLimit(double liquidationLimit) {
        this.liquidationLimit = liquidationLimit;
    }
    @Basic
    @Column(name = "Mezzanine_Debt_Limit", nullable = false, precision = 0)
    public double getMezzanineDebtLimit() {
        return mezzanineDebtLimit;
    }

    public void setMezzanineDebtLimit(double mezzanineDebtLimit) {
        this.mezzanineDebtLimit = mezzanineDebtLimit;
    }
    @Basic
    @Column(name = "Preferred_Shares_Limit", nullable = false, precision = 0)
    public double getPreferredSharesLimit() {
        return preferredSharesLimit;
    }

    public void setPreferredSharesLimit(double preferredSharesLimit) {
        this.preferredSharesLimit = preferredSharesLimit;
    }
    @Basic
    @Column(name = "Senior_Secured_Limit", nullable = false, precision = 0)
    public double getSeniorSecuredLimit() {
        return seniorSecuredLimit;
    }

    public void setSeniorSecuredLimit(double seniorSecuredLimit) {
        this.seniorSecuredLimit = seniorSecuredLimit;
    }
    @Basic
    @Column(name = "Senior_Unsecured_Limit", nullable = false, precision = 0)
    public double getSeniorUnsecuredLimit() {
        return seniorUnsecuredLimit;
    }

    public void setSeniorUnsecuredLimit(double seniorUnsecuredLimit) {
        this.seniorUnsecuredLimit = seniorUnsecuredLimit;
    }
    @Basic
    @Column(name = "Subordinated_Secured_Limit", nullable = false, precision = 0)
    public double getSubordinatedSecuredLimit() {
        return subordinatedSecuredLimit;
    }

    public void setSubordinatedSecuredLimit(double subordinatedSecuredLimit) {
        this.subordinatedSecuredLimit = subordinatedSecuredLimit;
    }
    @Basic
    @Column(name = "Subordinated_Unsecured_Limit", nullable = false, precision = 0)
    public double getSubordinatedUnsecuredLimit() {
        return subordinatedUnsecuredLimit;
    }

    public void setSubordinatedUnsecuredLimit(double subordinatedUnsecuredLimit) {
        this.subordinatedUnsecuredLimit = subordinatedUnsecuredLimit;
    }
    @Basic
    @Column(name = "LDI_Fund", nullable = false, precision = 0)
    public double getLdiFund() {
        return ldiFund;
    }

    public void setLdiFund(double ldiFund) {
        this.ldiFund = ldiFund;
    }
    @Basic
    @Column(name = "FixedIncome_Fund", nullable = false, precision = 0)
    public double getFixedIncomeFund() {
        return fixedIncomeFund;
    }

    public void setFixedIncomeFund(double fixedIncomeFund) {
        this.fixedIncomeFund = fixedIncomeFund;
    }
    @Basic
    @Column(name = "Credit_Fund", nullable = false, precision = 0)
    public double getCreditFund() {
        return creditFund;
    }

    public void setCreditFund(double creditFund) {
        this.creditFund = creditFund;
    }
    @Basic
    @Column(name = "MultiAsset_Fund", nullable = false, precision = 0)
    public double getMultiAssetFund() {
        return multiAssetFund;
    }

    public void setMultiAssetFund(double multiAssetFund) {
        this.multiAssetFund = multiAssetFund;
    }
    @Basic
    @Column(name = "MultiManager_Fund", nullable = false, precision = 0)
    public double getMultiManagerFund() {
        return multiManagerFund;
    }

    public void setMultiManagerFund(double multiManagerFund) {
        this.multiManagerFund = multiManagerFund;
    }

    @Basic
    @Column(name = "CreatedOn", nullable = false)
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfflWorkflowFundLimitInformationEntity that = (ApprovalForumWfflWorkflowFundLimitInformationEntity) o;
        return wfflId == that.wfflId &&
                wffappId == that.wffappId &&
                Objects.equals(seniorUnsecuredLimit, that.seniorUnsecuredLimit) &&
                Objects.equals(preferredSharesLimit, that.preferredSharesLimit) &&
                Objects.equals(liquidationLimit, that.liquidationLimit) &&
                Objects.equals(juniorDebtLimit, that.juniorDebtLimit) &&
                Objects.equals(governmentGuaranteeLimit, that.governmentGuaranteeLimit) &&
                Objects.equals(convertedEquityLimit, that.convertedEquityLimit) &&
                Objects.equals(mezzanineDebtLimit, that.mezzanineDebtLimit) &&
                Objects.equals(seniorSecuredLimit, that.seniorSecuredLimit) &&
                Objects.equals(subordinatedSecuredLimit, that.subordinatedSecuredLimit) &&
                Objects.equals(subordinatedUnsecuredLimit, that.subordinatedUnsecuredLimit) &&
                Objects.equals(ldiFund, that.ldiFund) &&
                Objects.equals(fixedIncomeFund, that.fixedIncomeFund) &&
                Objects.equals(creditFund, that.creditFund) &&
                Objects.equals(multiAssetFund, that.multiAssetFund) &&
                Objects.equals(multiManagerFund, that.multiManagerFund) &&
                Objects.equals(createdOn, that.createdOn);
    }


    @Override
    public int hashCode() {
        return Objects.hash(wfflId, wffappId, cptyLimit, convertedEquityLimit, governmentGuaranteeLimit, juniorDebtLimit, liquidationLimit, mezzanineDebtLimit, preferredSharesLimit, seniorSecuredLimit, seniorUnsecuredLimit, subordinatedSecuredLimit, subordinatedUnsecuredLimit, ldiFund, fixedIncomeFund, creditFund, multiAssetFund, multiManagerFund, createdOn);
    }

    @Override
    public String toString() {
        return "ApprovalForumWfflWorkflowFundLimitInformationEntity{" +
                ", wffappId=" + wffappId +
                ", cptyLimit=" + cptyLimit +
                ", convertedEquityLimit=" + convertedEquityLimit +
                ", governmentGuaranteeLimit=" + governmentGuaranteeLimit +
                ", juniorDebtLimit=" + juniorDebtLimit +
                ", liquidationLimit=" + liquidationLimit +
                ", mezzanineDebtLimit=" + mezzanineDebtLimit +
                ", preferredSharesLimit=" + preferredSharesLimit +
                ", seniorSecuredLimit=" + seniorSecuredLimit +
                ", seniorUnsecuredLimit=" + seniorUnsecuredLimit +
                ", subordinatedSecuredLimit=" + subordinatedSecuredLimit +
                ", subordinatedUnsecuredLimit=" + subordinatedUnsecuredLimit +
                ", ldiFund=" + ldiFund +
                ", fixedIncomeFund=" + fixedIncomeFund +
                ", creditFund=" + creditFund +
                ", multiAssetFund=" + multiAssetFund +
                ", multiManagerFund=" + multiManagerFund +
                ", createdOn=" + createdOn +
                '}';
    }

}
