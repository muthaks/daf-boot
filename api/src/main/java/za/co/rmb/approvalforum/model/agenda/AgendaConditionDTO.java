package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

/** A condition, action or follow-up on an agenda item (BRS section 6.8). */
public class AgendaConditionDTO {

    private int agcaId;
    private int agiId;
    private String conditionType;
    private String description;
    private String ownerId;
    private String ownerName;
    private Timestamp dueDate;
    private String status;
    private String evidenceRef;
    private String closureComments;
    private String closedBy;
    private Timestamp closedOn;
    private String createdBy;
    private Timestamp createdOn;

    public int getAgcaId() { return agcaId; }
    public void setAgcaId(int agcaId) { this.agcaId = agcaId; }

    public int getAgiId() { return agiId; }
    public void setAgiId(int agiId) { this.agiId = agiId; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }

    public String getClosureComments() { return closureComments; }
    public void setClosureComments(String closureComments) { this.closureComments = closureComments; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public Timestamp getClosedOn() { return closedOn; }
    public void setClosedOn(Timestamp closedOn) { this.closedOn = closedOn; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedOn() { return createdOn; }
    public void setCreatedOn(Timestamp createdOn) { this.createdOn = createdOn; }

}
