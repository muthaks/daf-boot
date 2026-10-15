package za.co.rmb.approvalforum.model.lumos;

import java.util.List;

public class ClientFinancialAttributes {

        private String schemeEstablishedDate;
        private List<String> assetManagers;
        private Integer auditor_Id;
        private List<String> participatingEmployers;

        // Getters and Setters

    public String getSchemeEstablishedDate() {
        return schemeEstablishedDate;
    }

    public void setSchemeEstablishedDate(String schemeEstablishedDate) {
        this.schemeEstablishedDate = schemeEstablishedDate;
    }

    public List<String> getAssetManagers() {
        return assetManagers;
    }

    public void setAssetManagers(List<String> assetManagers) {
        this.assetManagers = assetManagers;
    }

    public Integer getAuditor_Id() {
        return auditor_Id;
    }

    public void setAuditor_Id(Integer auditor_Id) {
        this.auditor_Id = auditor_Id;
    }

    public List<String> getParticipatingEmployers() {
        return participatingEmployers;
    }

    public void setParticipatingEmployers(List<String> participatingEmployers) {
        this.participatingEmployers = participatingEmployers;
    }
}
