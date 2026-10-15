package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

/** Reference to minutes / transcript / recording / pack for a meeting (BR-020). */
public class AgendaMeetingRecordDTO {

    private int agmrId;
    private int agmId;
    private String recordType;
    private String name;
    private String location;
    private String createdBy;
    private Timestamp createdOn;

    public int getAgmrId() { return agmrId; }
    public void setAgmrId(int agmrId) { this.agmrId = agmrId; }

    public int getAgmId() { return agmId; }
    public void setAgmId(int agmId) { this.agmId = agmId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedOn() { return createdOn; }
    public void setCreatedOn(Timestamp createdOn) { this.createdOn = createdOn; }

}
