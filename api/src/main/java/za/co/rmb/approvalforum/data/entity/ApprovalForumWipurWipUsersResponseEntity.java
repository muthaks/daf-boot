package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "WIPUR_WipUsersResponse")
public class ApprovalForumWipurWipUsersResponseEntity {

    private int wipurId;
    private byte[] usersResponseData;

    private ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId;

    @Id
    @Column(name = "WIPUR_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getWipurId() {
        return wipurId;
    }

    public void setWipurId(int wipurId) {
        this.wipurId = wipurId;
    }

    @Basic
    @Column(name = "UsersResponseData", nullable = true)
    public byte[] getUsersResponseData() {
        return usersResponseData;
    }

    public void setUsersResponseData(byte[] usersResponseData) {
        this.usersResponseData = usersResponseData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumWipurWipUsersResponseEntity that = (ApprovalForumWipurWipUsersResponseEntity) o;
        return wipurId == that.wipurId &&
                Arrays.equals(usersResponseData, that.usersResponseData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(wipurId);
        result = 31 * result + Arrays.hashCode(usersResponseData);
        return result;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WF_ID", referencedColumnName = "WF_ID", nullable = false)
    public ApprovalForumWfWorkflowEntity getWfApprovalForumWorkflowByWfId() {
        return wfApprovalForumWorkflowByWfId;
    }

    public void setWfApprovalForumWorkflowByWfId(ApprovalForumWfWorkflowEntity wfApprovalForumWorkflowByWfId) {
        this.wfApprovalForumWorkflowByWfId = wfApprovalForumWorkflowByWfId;
    }

}
