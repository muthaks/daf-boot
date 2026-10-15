package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Audited
@Table(name = "WFC_WorkflowChat")
public class ApprovalForumWfcWorkflowChatEntity {

    private int wfcId;
    private Timestamp createdOn;
    private String userId;
    private String displayName;
    private String chatText;

    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;

    @Id
    @Column(name = "WFC_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWfcId() {
        return wfcId;
    }

    public void setWfcId(int wfcId) {
        this.wfcId = wfcId;
    }

    @Basic
    @Column(name = "CreatedOn", nullable = false)
    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }

    @Basic
    @Column(name = "UserId", nullable = false, length = 255)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "DisplayName", nullable = false, length = 255)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Basic
    @Column(name = "ChatText", nullable = false, length = 2147483647)
    public String getChatText() {
        return chatText;
    }

    public void setChatText(String chatText) {
        this.chatText = chatText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWfcWorkflowChatEntity that = (ApprovalForumWfcWorkflowChatEntity) o;
        return wfcId == that.wfcId &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(chatText, that.chatText) &&
                Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wfcId, createdOn, userId, chatText, displayName);
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
