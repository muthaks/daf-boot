package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

/** One workflow action on an item or meeting (BRS section 6.6). */
public class AgendaWorkflowActionDTO {

    private int agwId;
    private Integer agiId;
    private Integer agmId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String comments;
    private boolean overrideFlag;
    private String overrideReason;
    private String actionBy;
    private Timestamp actionOn;

    public int getAgwId() { return agwId; }
    public void setAgwId(int agwId) { this.agwId = agwId; }

    public Integer getAgiId() { return agiId; }
    public void setAgiId(Integer agiId) { this.agiId = agiId; }

    public Integer getAgmId() { return agmId; }
    public void setAgmId(Integer agmId) { this.agmId = agmId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }

    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public boolean isOverrideFlag() { return overrideFlag; }
    public void setOverrideFlag(boolean overrideFlag) { this.overrideFlag = overrideFlag; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }

    public String getActionBy() { return actionBy; }
    public void setActionBy(String actionBy) { this.actionBy = actionBy; }

    public Timestamp getActionOn() { return actionOn; }
    public void setActionOn(Timestamp actionOn) { this.actionOn = actionOn; }

}
