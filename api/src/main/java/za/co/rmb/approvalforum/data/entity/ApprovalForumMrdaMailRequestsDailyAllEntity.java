package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "MRDA_MailRequestsDailyAll")
public class ApprovalForumMrdaMailRequestsDailyAllEntity {

    private int mrdaId;
    private String sentTo;
    private Timestamp createdOn;
    private boolean complete;
    private Timestamp completedOn;
    private Timestamp lastTriedOn;
    private String lastTriedMessage;
    private Integer retryCount;
    private String approverActionWfIds;
    private String approverNoActionWfIds;
    private String capturerWfIds;
    private byte[] mailModel;
    private String mailProcess;

    @Id
    @Column(name = "MRDA_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getMrdaId() {
        return mrdaId;
    }

    public void setMrdaId(int mrdaId) {
        this.mrdaId = mrdaId;
    }

    @Basic
    @Column(name = "SentTo", nullable = false, length = 255)
    public String getSentTo() {
        return sentTo;
    }

    public void setSentTo(String sentTo) {
        this.sentTo = sentTo;
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
    @Column(name = "ApproverAction_WF_IDs", nullable = true, length = 255)
    public String getApproverActionWfIds() {
        return approverActionWfIds;
    }

    public void setApproverActionWfIds(String approverActionWfIds) {
        this.approverActionWfIds = approverActionWfIds;
    }

    @Basic
    @Column(name = "ApproverNoAction_WF_IDs", nullable = true, length = 255)
    public String getApproverNoActionWfIds() {
        return approverNoActionWfIds;
    }

    public void setApproverNoActionWfIds(String noActionRequiredWorkflows) {
        this.approverNoActionWfIds = noActionRequiredWorkflows;
    }

    @Basic
    @Column(name = "Capturer_WF_IDs", nullable = true, length = 255)
    public String getCapturerWfIds() {
        return capturerWfIds;
    }

    public void setCapturerWfIds(String capturerWfIds) {
        this.capturerWfIds = capturerWfIds;
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
        ApprovalForumMrdaMailRequestsDailyAllEntity that = (ApprovalForumMrdaMailRequestsDailyAllEntity) o;
        return mrdaId == that.mrdaId &&
                complete == that.complete &&
                Objects.equals(sentTo, that.sentTo) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(completedOn, that.completedOn) &&
                Objects.equals(lastTriedOn, that.lastTriedOn) &&
                Objects.equals(lastTriedMessage, that.lastTriedMessage) &&
                Objects.equals(retryCount, that.retryCount) &&
                Objects.equals(approverActionWfIds, that.approverActionWfIds) &&
                Objects.equals(approverNoActionWfIds, that.approverNoActionWfIds) &&
                Objects.equals(capturerWfIds, that.capturerWfIds) &&
                Arrays.equals(mailModel, that.mailModel) &&
                Objects.equals(mailProcess, that.mailProcess);

    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mrdaId, sentTo, createdOn, complete, completedOn, lastTriedOn, lastTriedMessage,
                approverActionWfIds, approverNoActionWfIds, capturerWfIds, mailProcess);
        result = 31 * result + Arrays.hashCode(mailModel);
        return result;
    }

}
