package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "R_Rules")
public class ApprovalForumRRulesEntity {

    private int rId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private String rules;
    private String fileName;
    private String md5sum;

    @Id
    @Column(name = "R_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getRId() {
        return rId;
    }

    public void setRId(int rId) {
        this.rId = rId;
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
    @Column(name = "Active", nullable = false)
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Basic
    @Column(name = "Rules", nullable = false, length = 2147483647)
    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    @Basic
    @Column(name = "FileName", nullable = false, length = 255)
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Basic
    @Column(name = "Md5Sum", nullable = false, length = 100)
    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumRRulesEntity that = (ApprovalForumRRulesEntity) o;
        return rId == that.rId &&
                Objects.equals(createdBy, that.createdBy) &&
                Objects.equals(createdOn, that.createdOn) &&
                active == that.active &&
                Objects.equals(rules, that.rules) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(md5sum, that.md5sum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rId, createdBy, createdOn, active, rules, fileName, md5sum);
    }

}
