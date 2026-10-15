package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "AFG_ApprovalForumGroup")
public class ApprovalForumAfgApprovalForumGroupEntity {

    private int afgId;
    private String groupName;

    @Id
    @Column(name = "AFG_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAfgId() {
        return afgId;
    }

    public void setAfgId(int afgId) {
        this.afgId = afgId;
    }

    @Basic
    @Column(name = "GroupName", nullable = false, length = 100)
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAfgApprovalForumGroupEntity that = (ApprovalForumAfgApprovalForumGroupEntity) o;
        return afgId == that.afgId &&
                Objects.equals(groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(afgId, groupName);
    }

}
