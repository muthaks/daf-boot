package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFDA_WorkflowData")
public class ApprovalForumWfdaWorkflowDataEntity {

    private int wfdaId;
    private String recommendation;
    private String chairRecommendation;

    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;

    @Id
    @Column(name = "WFDA_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfdaId() {
        return wfdaId;
    }

    public void setWfdaId(int wfdaId) {
        this.wfdaId = wfdaId;
    }

    @Basic
    @Column(name = "Recommendation", nullable = false, length = 2147483647)
    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    @Basic
    @Column(name = "ChairRecommendation", nullable = false, length = 2147483647)
    public String getChairRecommendation() {
        return chairRecommendation;
    }

    public void setChairRecommendation(String chairRecommendation) {
        this.chairRecommendation = chairRecommendation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfdaWorkflowDataEntity that = (ApprovalForumWfdaWorkflowDataEntity) o;
        return wfdaId == that.wfdaId &&
                Objects.equals(recommendation, that.recommendation) &&
                Objects.equals(chairRecommendation, that.chairRecommendation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfdaId, recommendation, chairRecommendation);
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
