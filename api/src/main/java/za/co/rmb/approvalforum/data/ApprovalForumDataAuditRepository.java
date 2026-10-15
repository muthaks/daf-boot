package za.co.rmb.approvalforum.data;

import za.co.rmb.approvalforum.data.entity.ApprovalForumWfWorkflowEntity;
import za.co.rmb.approvalforum.data.entity.ApprovalForumWfaWorkflowApprovalsEntity;
import za.co.rmb.approvalforum.model.AuditDetails;
import za.co.rmb.approvalforum.model.LastUpdateDetails;
import za.co.rmb.approvalforum.model.Revision;

import java.util.List;

public interface ApprovalForumDataAuditRepository {

    List<Revision> getWorkflowRevisions(int workflowId);

    List<Revision> getWorkflowApprovalRevisions(int workflowId);

    ApprovalForumWfWorkflowEntity getWorkflowRevision(int workflowId, int revisionId);

    ApprovalForumWfaWorkflowApprovalsEntity getWorkflowApprovalRevision(int workflowId, int revisionId);

    void setAuditDetails(AuditDetails auditDetails);

    LastUpdateDetails getWorkflowLastUpdatedDetails(int workflowId);

    LastUpdateDetails getWorkflowApprovalLastUpdatedDetails(int workflowId);

}
