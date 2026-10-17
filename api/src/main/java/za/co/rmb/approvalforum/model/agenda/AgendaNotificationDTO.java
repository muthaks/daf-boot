package za.co.rmb.approvalforum.model.agenda;

import java.sql.Timestamp;

public class AgendaNotificationDTO {

    private int agnId;
    private Integer agiId;
    private Integer agmId;
    private String notificationType;
    private String recipients;
    private String subject;
    private String status;
    private String errorMessage;
    private Timestamp sentOn;
    private String reason;
    private String actionBy;
    private String message;
    private Integer workflowActionId;
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getActionBy() { return actionBy; }
    public void setActionBy(String actionBy) { this.actionBy = actionBy; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getWorkflowActionId() { return workflowActionId; }
    public void setWorkflowActionId(Integer id) { this.workflowActionId = id; }

    public int getAgnId() { return agnId; }
    public void setAgnId(int agnId) { this.agnId = agnId; }

    public Integer getAgiId() { return agiId; }
    public void setAgiId(Integer agiId) { this.agiId = agiId; }

    public Integer getAgmId() { return agmId; }
    public void setAgmId(Integer agmId) { this.agmId = agmId; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Timestamp getSentOn() { return sentOn; }
    public void setSentOn(Timestamp sentOn) { this.sentOn = sentOn; }
}
