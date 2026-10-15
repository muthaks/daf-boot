package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "MQRNM_MeetingQuorumRulesNameMapping")
public class ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity {

    private int mqrnmId;
    private String meetingName;
    private boolean chairpersonApprovalRequired;
    private ApprovalForumMqrMeetingQuorumRulesEntity mqrMeetingQuorumRulesByMqrId;

    @Id
    @Column(name = "MQRNM_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getMqrnmId() {
        return mqrnmId;
    }

    public void setMqrnmId(int mqrnmId) {
        this.mqrnmId = mqrnmId;
    }

    @Basic
    @Column(name = "MeetingName", nullable = false, length = 255)
    public String getMeetingName() {
        return meetingName;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    @Basic
    @Column(name = "ChairApprovalRequired", nullable = false)
    public boolean isChairpersonApprovalRequired() {
        return chairpersonApprovalRequired;
    }

    public void setChairpersonApprovalRequired(boolean chairpersonApprovalRequired) {
        this.chairpersonApprovalRequired = chairpersonApprovalRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity that = (ApprovalForumMqrnmMeetingQuorumRulesNameMappingEntity) o;
        return mqrnmId == that.mqrnmId &&
                Objects.equals(meetingName, that.meetingName) &&
                chairpersonApprovalRequired == that.chairpersonApprovalRequired;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mqrnmId, meetingName, chairpersonApprovalRequired);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MQR_ID", referencedColumnName = "MQR_ID", nullable = false)
    public ApprovalForumMqrMeetingQuorumRulesEntity getMqrMeetingQuorumRulesByMqrId() {
        return mqrMeetingQuorumRulesByMqrId;
    }

    public void setMqrMeetingQuorumRulesByMqrId(ApprovalForumMqrMeetingQuorumRulesEntity mqrMeetingQuorumRulesByMqrId) {
        this.mqrMeetingQuorumRulesByMqrId = mqrMeetingQuorumRulesByMqrId;
    }

}
