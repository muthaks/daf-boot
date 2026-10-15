package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "WIPRR_WipRulesResponse")
public class ApprovalForumWiprrWipRulesResponseEntity {

    private int wiprrId;
    private byte[] rulesResponseData;

    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;

    @Id
    @Column(name = "WIPRR_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWiprrId() {
        return wiprrId;
    }

    public void setWiprrId(int wiprrId) {
        this.wiprrId = wiprrId;
    }

    @Basic
    @Column(name = "RulesResponseData", nullable = true)
    public byte[] getRulesResponseData() {
        return rulesResponseData;
    }

    public void setRulesResponseData(byte[] rulesResponseData) {
        this.rulesResponseData = rulesResponseData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWiprrWipRulesResponseEntity that = (ApprovalForumWiprrWipRulesResponseEntity) o;
        return wiprrId == that.wiprrId &&
                Arrays.equals(rulesResponseData, that.rulesResponseData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(wiprrId);
        result = 31 * result + Arrays.hashCode(rulesResponseData);
        return result;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WF_ID", referencedColumnName = "WF_ID", nullable = false)
    public ApprovalForumWfWorkflowEntity getWfApprovalForumWorkflowByWfId() {
        return wfApprovalForumWorkflowByWfId;
    }

    public void setWfApprovalForumWorkflowByWfId(ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId) {
        this.wfApprovalForumWorkflowByWfId = wfApprovalForumWorkflowByWfId;
    }

}
