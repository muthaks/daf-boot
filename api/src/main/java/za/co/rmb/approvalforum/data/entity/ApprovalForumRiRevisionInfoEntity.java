package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import za.co.rmb.approvalforum.data.envers.EnversRevisionListener;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "RI_RevisionInfo")
@RevisionEntity(EnversRevisionListener.class)
public class ApprovalForumRiRevisionInfoEntity {

    private int rev;
    private long revTstmp;
    private String userName;
    private String description;
    private int workflowId;

    @Id
    @Column(name = "Rev", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    public int getRev() {
        return rev;
    }

    public void setRev(int rev) {
        this.rev = rev;
    }

    @Basic
    @Column(name = "RevTstmp", nullable = false)
    @RevisionTimestamp
    public long getRevTstmp() {
        return revTstmp;
    }

    public void setRevTstmp(long revTstmp) {
        this.revTstmp = revTstmp;
    }

    @Basic
    @Column(name = "UserName", nullable = false, length = 100)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Basic
    @Column(name = "Description", nullable = false, length = 255)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "WorkflowId", nullable = false)
    public int getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(int workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumRiRevisionInfoEntity that = (ApprovalForumRiRevisionInfoEntity) o;
        return rev == that.rev &&
                revTstmp == that.revTstmp &&
                workflowId == that.workflowId &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rev, revTstmp, userName, description, workflowId);
    }
}
