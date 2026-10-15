package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.util.Objects;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "RT_RequestType")
@Audited(targetAuditMode = NOT_AUDITED)
public class ApprovalForumRtRequestTypeEntity {

    private int rtId;
    private String type;

    @Id
    @Column(name = "RT_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getRtId() {
        return rtId;
    }

    public void setRtId(int rtId) {
        this.rtId = rtId;
    }

    @Basic
    @Column(name = "Type", nullable = false, length = 255)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumRtRequestTypeEntity that = (ApprovalForumRtRequestTypeEntity) o;
        return rtId == that.rtId &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rtId, type);
    }

}
