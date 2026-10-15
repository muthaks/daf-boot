package za.co.rmb.approvalforum.data.entity;

import org.hibernate.envers.Audited;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Reference to a governance record for a meeting - minutes, transcript, recording, agenda pack
 * or other evidence held in the approved repository (BRS BR-020).
 */
@Entity
@Audited
@Table(name = "AGMR_AgendaMeetingRecord")
public class ApprovalForumAgmrAgendaMeetingRecordEntity {

    private int agmrId;
    private String createdBy;
    private Timestamp createdOn;
    private boolean active;
    private String recordType;
    private String name;
    private String location;

    private ApprovalForumAgmAgendaMeetingEntity agmAgendaMeetingByAgmId;

    @Id
    @Column(name = "AGMR_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgmrId() {
        return agmrId;
    }

    public void setAgmrId(int agmrId) {
        this.agmrId = agmrId;
    }

    @Basic
    @Column(name = "CreatedBy", nullable = false)
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
    @Column(name = "RecordType", nullable = false, length = 50)
    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    @Basic
    @Column(name = "Name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "Location", nullable = false, length = 1000)
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGM_ID", referencedColumnName = "AGM_ID", nullable = false)
    public ApprovalForumAgmAgendaMeetingEntity getAgmAgendaMeetingByAgmId() {
        return agmAgendaMeetingByAgmId;
    }

    public void setAgmAgendaMeetingByAgmId(ApprovalForumAgmAgendaMeetingEntity agmAgendaMeetingByAgmId) {
        this.agmAgendaMeetingByAgmId = agmAgendaMeetingByAgmId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumAgmrAgendaMeetingRecordEntity that = (ApprovalForumAgmrAgendaMeetingRecordEntity) o;
        return agmrId == that.agmrId && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agmrId, location);
    }

}
