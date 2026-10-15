package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "P_Properties")
public class ApprovalForumPPropertiesEntity {

    private int pId;
    private String name;
    private String value;

    @Id
    @Column(name = "P_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getpId() {
        return pId;
    }

    public void setpId(int pId) {
        this.pId = pId;
    }

    @Basic
    @Column(name = "Name", nullable = true, length = 255)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "Value", nullable = true, length = 255)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumPPropertiesEntity that = (ApprovalForumPPropertiesEntity) o;
        return pId == that.pId &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pId, name, value);
    }

}
