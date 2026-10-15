package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Collection;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFCD_WorkflowCounterpartyDetail")
public class ApprovalForumWfcdWorkflowCounterpartyDetailEntity {

    private int wfcdId;
    private String creditInkombaId;
    private String counterpartyName;
    private String creditAnalyst;
    private String creditAnalystId;
    private String creditAnalystEmail;
    private boolean bank;
    private String financialInstitutionType;
    private boolean prospect;

    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;
    private ApprovalForumWfadWorkflowApplicationDataEntity wfadApprovalForumWorkflowApplicationDataByWfadId;
    private Collection<ApprovalForumWffiWorkflowFacilityInformationEntity> wffiWorkflowFacilityInformationByWffiId;

    @Id
    @Column(name = "WFCD_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfcdId() {
        return wfcdId;
    }

    public void setWfcdId(int wfcdId) {
        this.wfcdId = wfcdId;
    }

    @Basic
    @Column(name = "CreditInkombaId", nullable = false, length = 255)
    public String getCreditInkombaId() {
        return creditInkombaId;
    }

    public void setCreditInkombaId(String creditInkombaId) {
        this.creditInkombaId = creditInkombaId;
    }

    @Basic
    @Column(name = "CounterpartyName", nullable = false, length = 255)
    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    @Basic
    @Column(name = "CreditAnalyst")
    public String getCreditAnalyst() {
        return creditAnalyst;
    }

    public void setCreditAnalyst(String creditAnalyst) {
        this.creditAnalyst = creditAnalyst;
    }

    @Basic
    @Column(name = "CreditAnalystId")
    public String getCreditAnalystId() {
        return creditAnalystId;
    }

    public void setCreditAnalystId(String creditAnalystId) {
        this.creditAnalystId = creditAnalystId;
    }

    @Basic
    @Column(name = "CreditAnalystEmail")
    public String getCreditAnalystEmail() {
        return creditAnalystEmail;
    }

    public void setCreditAnalystEmail(String creditAnalystEmail) {
        this.creditAnalystEmail = creditAnalystEmail;
    }

    @Basic
    @Column(name = "Bank", nullable = false)
    public boolean isBank() {
        return bank;
    }

    public void setBank(boolean bank) {
        this.bank = bank;
    }

    @Basic
    @Column(name = "FinancialInstitutionType")
    public String getFinancialInstitutionType() {
        return financialInstitutionType;
    }

    public void setFinancialInstitutionType(String financialInstitutionType) {
        this.financialInstitutionType = financialInstitutionType;
    }

    @Basic
    @Column(name = "Prospect", nullable = false)
    public boolean isProspect() {
        return prospect;
    }

    public void setProspect(boolean prospect) {
        this.prospect = prospect;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfcdWorkflowCounterpartyDetailEntity that = (ApprovalForumWfcdWorkflowCounterpartyDetailEntity) o;
        return wfcdId == that.wfcdId &&
                Objects.equals(creditInkombaId, that.creditInkombaId) &&
                Objects.equals(counterpartyName, that.counterpartyName) &&
                Objects.equals(creditAnalyst, that.creditAnalyst) &&
                Objects.equals(creditAnalystId, that.creditAnalystId) &&
                Objects.equals(creditAnalystEmail, that.creditAnalystEmail) &&
                Objects.equals(financialInstitutionType, that.financialInstitutionType) &&
                bank == that.bank &&
                prospect == that.prospect;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfcdId, creditInkombaId, counterpartyName, creditAnalyst, creditAnalystId, creditAnalystEmail, bank, financialInstitutionType, prospect);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WF_ID", referencedColumnName = "WF_ID", nullable = false)
    public ApprovalForumWfWorkflowEntity getWfApprovalForumWorkflowByWfId() {
        return wfApprovalForumWorkflowByWfId;
    }

    public void setWfApprovalForumWorkflowByWfId(ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId) {
        this.wfApprovalForumWorkflowByWfId = wfApprovalForumWorkflowByWfId;
    }

    @OneToOne(mappedBy = "wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId", fetch = FetchType.LAZY)
    public ApprovalForumWfadWorkflowApplicationDataEntity getWfadApprovalForumWorkflowApplicationDataByWfadId() {
        return wfadApprovalForumWorkflowApplicationDataByWfadId;
    }

    public void setWfadApprovalForumWorkflowApplicationDataByWfadId(ApprovalForumWfadWorkflowApplicationDataEntity wfadApprovalForumWorkflowApplicationDataByWfadId) {
        this.wfadApprovalForumWorkflowApplicationDataByWfadId = wfadApprovalForumWorkflowApplicationDataByWfadId;
    }

    @OneToMany(mappedBy = "wfcdApprovalForumWorkflowCounterpartyDetailByWfcdId")
    public Collection<ApprovalForumWffiWorkflowFacilityInformationEntity> getWffiWorkflowFacilityInformationByWffiId() {
        return wffiWorkflowFacilityInformationByWffiId;
    }

    public void setWffiWorkflowFacilityInformationByWffiId(Collection<ApprovalForumWffiWorkflowFacilityInformationEntity> wffiWorkflowFacilityInformationByWffiId) {
        this.wffiWorkflowFacilityInformationByWffiId = wffiWorkflowFacilityInformationByWffiId;
    }

}
