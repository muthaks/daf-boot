package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "MR_MailRequests")
public class ApprovalForumMrMailRequestsEntity {

    private int mrId;
    private Timestamp createdOn;
    private boolean complete;
    private Timestamp completedOn;
    private Timestamp lastTriedOn;
    private String lastTriedMessage;
    private Integer retryCount;
    private Integer wfId;
    private byte[] mailModel;
    private String mailProcess;

    @Id
    @Column(name = "MR_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getMrId() {
        return mrId;
    }

    public void setMrId(int mrId) {
        this.mrId = mrId;
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
    @Column(name = "IsComplete", nullable = false)
    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    @Basic
    @Column(name = "CompletedOn", nullable = true)
    public Timestamp getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(Timestamp completedOn) {
        this.completedOn = completedOn;
    }

    @Basic
    @Column(name = "LastTriedOn", nullable = true)
    public Timestamp getLastTriedOn() {
        return lastTriedOn;
    }

    public void setLastTriedOn(Timestamp lastTriedOn) {
        this.lastTriedOn = lastTriedOn;
    }

    @Basic
    @Column(name = "LastTriedMessage", nullable = true, length = 2147483647)
    public String getLastTriedMessage() {
        return lastTriedMessage;
    }

    public void setLastTriedMessage(String lastTriedMessage) {
        this.lastTriedMessage = lastTriedMessage;
    }

    @Basic
    @Column(name = "RetryCount", nullable = true)
    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    @Basic
    @Column(name = "WF_ID", nullable = true)
    public Integer getWfId() {
        return wfId;
    }

    public void setWfId(Integer wfId) {
        this.wfId = wfId;
    }

    @Basic
    @Column(name = "MailModel", nullable = true)
    public byte[] getMailModel() {
        return mailModel;
    }

    public void setMailModel(byte[] mailModel) {
        this.mailModel = mailModel;
    }

    @Basic
    @Column(name = "MailProcess", nullable = false, length = 255)
    public String getMailProcess() {
        return mailProcess;
    }

    public void setMailProcess(String mailProcess) {
        this.mailProcess = mailProcess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumMrMailRequestsEntity that = (ApprovalForumMrMailRequestsEntity) o;
        return mrId == that.mrId &&
                complete == that.complete &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(completedOn, that.completedOn) &&
                Objects.equals(lastTriedOn, that.lastTriedOn) &&
                Objects.equals(lastTriedMessage, that.lastTriedMessage) &&
                Objects.equals(retryCount, that.retryCount) &&
                Objects.equals(wfId, that.wfId) &&
                Arrays.equals(mailModel, that.mailModel) &&
                Objects.equals(mailProcess, that.mailProcess);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mrId, createdOn, complete, completedOn, lastTriedOn, lastTriedMessage, wfId, retryCount, mailProcess);
        result = 31 * result + Arrays.hashCode(mailModel);
        return result;
    }

}
