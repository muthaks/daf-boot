package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "AFADG_ApprovalForumActiveDirectoryGroup")
public class ApprovalForumAfadgApprovalForumActiveDirectoryGroupEntity {

    private int afadgId;
    private String adGroup;

    @Id
    @Column(name = "AFADG_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAfadgId() {
        return afadgId;
    }

    public void setAfadgId(int afadgId) {
        this.afadgId = afadgId;
    }

    @Basic
    @Column(name = "ADGroup", nullable = false, length = 255)
    public String getAdGroup() {
        return adGroup;
    }

    public void setAdGroup(String adGroup) {
        this.adGroup = adGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAfadgApprovalForumActiveDirectoryGroupEntity that = (ApprovalForumAfadgApprovalForumActiveDirectoryGroupEntity) o;
        return afadgId == that.afadgId &&
                Objects.equals(adGroup, that.adGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(afadgId, adGroup);
    }

}
