package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Objects;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "AFLMM_ApprovalForumListManagementMapping")
@Audited(targetAuditMode = NOT_AUDITED)
public class ApprovalForumAflmmApprovalForumListManagementMappingEntity {

    private int aflmmId;
    private String listName;
    private int listManagementId;
    private boolean chairpersonList;

    @Id
    @Column(name = "AFLMM_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAflmmId() {
        return aflmmId;
    }

    public void setAflmmId(int aflmmId) {
        this.aflmmId = aflmmId;
    }

    @Basic
    @Column(name = "ListName", nullable = false, length = 255)
    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    @Basic
    @Column(name = "ListManagementId", nullable = false)
    public int getListManagementId() {
        return listManagementId;
    }

    public void setListManagementId(int listManagementId) {
        this.listManagementId = listManagementId;
    }

    @Basic
    @Column(name = "ChairpersonList", nullable = false)
    public boolean isChairpersonList() {
        return chairpersonList;
    }

    public void setChairpersonList(boolean chairpersonList) {
        this.chairpersonList = chairpersonList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAflmmApprovalForumListManagementMappingEntity that = (ApprovalForumAflmmApprovalForumListManagementMappingEntity) o;
        return aflmmId == that.aflmmId &&
                listManagementId == that.listManagementId &&
                Objects.equals(listName, that.listName) &&
                chairpersonList == that.chairpersonList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(aflmmId, listName, listManagementId, chairpersonList);
    }

}
