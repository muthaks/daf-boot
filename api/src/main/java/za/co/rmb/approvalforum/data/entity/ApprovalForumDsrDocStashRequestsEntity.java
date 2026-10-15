package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "DSR_DocStashRequests")
public class ApprovalForumDsrDocStashRequestsEntity {

    private int dsrId;
    private Timestamp createdOn;
    private boolean complete;
    private Timestamp completedOn;
    private Timestamp lastTriedOn;
    private String lastTriedMessage;
    private Integer retryCount;
    private Integer wfId;
    private Integer docStashDocumentId;

    @Id
    @Column(name = "DSR_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getDsrId() {
        return dsrId;
    }

    public void setDsrId(int dsrId) {
        this.dsrId = dsrId;
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
    @Column(name = "DocStashDocumentId", nullable = true)
    public Integer getDocStashDocumentId() {
        return docStashDocumentId;
    }

    public void setDocStashDocumentId(Integer docStashDocumentId) {
        this.docStashDocumentId = docStashDocumentId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumDsrDocStashRequestsEntity that = (ApprovalForumDsrDocStashRequestsEntity) o;
        return dsrId == that.dsrId &&
                complete == that.complete &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(completedOn, that.completedOn) &&
                Objects.equals(lastTriedOn, that.lastTriedOn) &&
                Objects.equals(lastTriedMessage, that.lastTriedMessage) &&
                Objects.equals(retryCount, that.retryCount) &&
                Objects.equals(wfId, that.wfId) &&
                Objects.equals(docStashDocumentId, that.docStashDocumentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dsrId, createdOn, complete, completedOn, lastTriedOn, lastTriedMessage, retryCount, wfId, docStashDocumentId);
    }

}
