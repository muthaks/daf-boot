package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFQ_WorkflowQuorum")
public class ApprovalForumWfqWorkflowQuorumEntity {

    private int wfqId;
    private int totalNumberOfApprovalsRequired;
    private boolean selected;

    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;

    @Id
    @Column(name = "WFQ_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfqId() {
        return wfqId;
    }

    public void setWfqId(int wfqId) {
        this.wfqId = wfqId;
    }

    @Basic
    @Column(name = "TotalNumberOfApprovalsRequired", nullable = false)
    public int getTotalNumberOfApprovalsRequired() {
        return totalNumberOfApprovalsRequired;
    }

    public void setTotalNumberOfApprovalsRequired(int totalNumberOfApprovalsRequired) {
        this.totalNumberOfApprovalsRequired = totalNumberOfApprovalsRequired;
    }

    @Basic
    @Column(name = "Selected", nullable = false)
    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfqWorkflowQuorumEntity that = (ApprovalForumWfqWorkflowQuorumEntity) o;
        return wfqId == that.wfqId &&
                selected == that.selected &&
                totalNumberOfApprovalsRequired == that.totalNumberOfApprovalsRequired;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfqId, totalNumberOfApprovalsRequired, selected);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WF_ID", referencedColumnName = "WF_ID", nullable = false)
    public ApprovalForumWfWorkflowEntity getWfApprovalForumWorkflowByWfId() {
        return wfApprovalForumWorkflowByWfId;
    }

    public void setWfApprovalForumWorkflowByWfId(ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId) {
        this.wfApprovalForumWorkflowByWfId = wfApprovalForumWorkflowByWfId;
    }

}
