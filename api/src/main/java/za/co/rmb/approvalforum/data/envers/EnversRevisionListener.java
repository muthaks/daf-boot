package za.co.rmb.approvalforum.data.envers;

import org.hibernate.envers.RevisionListener;
import za.co.rmb.approvalforum.data.entity.ApprovalForumRiRevisionInfoEntity;

public class EnversRevisionListener implements RevisionListener {

    public void newRevision(Object revisionEntity) {
        ApprovalForumRiRevisionInfoEntity entity = (ApprovalForumRiRevisionInfoEntity) revisionEntity;
        entity.setDescription("");
        entity.setUserName("");
        entity.setWorkflowId(0);
    }

}
