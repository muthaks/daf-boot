package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFQD_WorkflowQuorumData")
public class ApprovalForumWfqdWorkflowQuorumDataEntity {

    private int wfqdId;
    private int minRequired;
    private int maxRequired;
    private int rank;

    private ApprovalForumAflmmApprovalForumListManagementMappingEntity aflmmApprovalForumListManagementMappingByAflmmId;
    private ApprovalForumWfqWorkflowQuorumEntity wfqApprovalForumWorkflowQuorumByWfqId;

    @Id
    @Column(name = "WFQD_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfqdId() {
        return wfqdId;
    }

    public void setWfqdId(int wfqdId) {
        this.wfqdId = wfqdId;
    }

    @Basic
    @Column(name = "MinRequired", nullable = false)
    public int getMinRequired() {
        return minRequired;
    }

    public void setMinRequired(int minRequired) {
        this.minRequired = minRequired;
    }

    @Basic
    @Column(name = "MaxRequired", nullable = false)
    public int getMaxRequired() {
        return maxRequired;
    }

    public void setMaxRequired(int maxRequired) {
        this.maxRequired = maxRequired;
    }

    @Basic
    @Column(name = "Rank", nullable = false)
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfqdWorkflowQuorumDataEntity that = (ApprovalForumWfqdWorkflowQuorumDataEntity) o;
        return wfqdId == that.wfqdId &&
                minRequired == that.minRequired &&
                maxRequired == that.maxRequired &&
                rank == that.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfqdId, minRequired, maxRequired, rank);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AFLMM_ID", referencedColumnName = "AFLMM_ID", nullable = false)
    public ApprovalForumAflmmApprovalForumListManagementMappingEntity getAflmmApprovalForumListManagementMappingByAflmmId() {
        return aflmmApprovalForumListManagementMappingByAflmmId;
    }

    public void setAflmmApprovalForumListManagementMappingByAflmmId(ApprovalForumAflmmApprovalForumListManagementMappingEntity aflmmApprovalForumListManagementMappingByAflmmId) {
        this.aflmmApprovalForumListManagementMappingByAflmmId = aflmmApprovalForumListManagementMappingByAflmmId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WFQ_ID", referencedColumnName = "WFQ_ID", nullable = false)
    public ApprovalForumWfqWorkflowQuorumEntity getWfqApprovalForumWorkflowQuorumByWfqId() {
        return wfqApprovalForumWorkflowQuorumByWfqId;
    }

    public void setWfqApprovalForumWorkflowQuorumByWfqId(ApprovalForumWfqWorkflowQuorumEntity wfqApprovalForumWorkflowQuorumByWfqId) {
        this.wfqApprovalForumWorkflowQuorumByWfqId = wfqApprovalForumWorkflowQuorumByWfqId;
    }

}
