package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "ER_ExternalRequests")
public class ApprovalForumErExternalRequestsEntity {

    private int erId;
    private boolean create;
    private Integer applicationId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean complete;
    private Timestamp completedOn;
    private Timestamp lastTriedOn;
    private String lastTriedMessage;
    private Integer retryCount;
    private Integer wfId;
    private boolean lumos;

    private Integer tenantId;

    @Id
    @Column(name = "ER_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getErId() {
        return erId;
    }

    public void setErId(int erId) {
        this.erId = erId;
    }

    @Basic
    @Column(name = "IsCreate", nullable = false)
    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    @Basic
    @Column(name = "ApplicationId", nullable = true)
    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    @Basic
    @Column(name = "CreatedBy", nullable = false, length = 255)
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    @Column(name = "IsLumos", nullable = false)
    public boolean isLumos() {
        return lumos;
    }

    public void setLumos(boolean lumos) {
        this.lumos = lumos;
    }

    @Basic
    @Column(name = "TenantId", nullable = true)
    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumErExternalRequestsEntity that = (ApprovalForumErExternalRequestsEntity) o;
        return erId == that.erId &&
                create == that.create &&
                complete == that.complete &&
                Objects.equals(applicationId, that.applicationId) &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(completedOn, that.completedOn) &&
                Objects.equals(lastTriedOn, that.lastTriedOn) &&
                Objects.equals(lastTriedMessage, that.lastTriedMessage) &&
                Objects.equals(retryCount, that.retryCount) &&
                Objects.equals(wfId, that.wfId) &&
                lumos == that.lumos &&
                Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(erId, create, applicationId, createdBy, createdOn, complete, completedOn, lastTriedOn, lastTriedMessage, retryCount, wfId, lumos, tenantId);
    }

}
