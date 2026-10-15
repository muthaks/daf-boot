package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(name = "MQR_MeetingQuorumRules")
public class ApprovalForumMqrMeetingQuorumRulesEntity {

    private int mqrId;
    private Timestamp createdOn;
    private String name;

    private Collection<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> mqrnmMeetingQuorumRulesNamesByMqrId;

    @Id
    @Column(name = "MQR_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getMqrId() {
        return mqrId;
    }

    public void setMqrId(int mqrId) {
        this.mqrId = mqrId;
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
    @Column(name = "Name", nullable = false, length = 255)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumMqrMeetingQuorumRulesEntity that = (ApprovalForumMqrMeetingQuorumRulesEntity) o;
        return mqrId == that.mqrId &&
                Objects.equals(createdOn, that.createdOn) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mqrId, createdOn, name);
    }

    @OneToMany(mappedBy = "mqrMeetingQuorumRulesByMqrId")
    public Collection<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> getMqrnmMeetingQuorumRulesNamesByMqrId() {
        return mqrnmMeetingQuorumRulesNamesByMqrId;
    }

    public void setMqrnmMeetingQuorumRulesNamesByMqrId(Collection<ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity> mqrnmMeetingQuorumRulesNamesByMqrId) {
        this.mqrnmMeetingQuorumRulesNamesByMqrId = mqrnmMeetingQuorumRulesNamesByMqrId;
    }

}
