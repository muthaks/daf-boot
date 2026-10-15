package za.co.rmb.approvalforum.data.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * Audit trail of agenda notification e-mails - one row per attempt, with the outcome
 * (Sent / Failed / Disabled / NoRecipient) so send status is visible in the UI.
 * Not Envers-audited; it is itself the log.
 */
@Entity
@Table(name = "AGN_AgendaNotification")
public class ApprovalForumAgnAgendaNotificationEntity {

    private int agnId;
    private Integer agiId;
    private Integer agmId;
    private String notificationType;
    private String recipients;
    private String subject;
    private String status;
    private String errorMessage;
    private Timestamp sentOn;

    @Id
    @Column(name = "AGN_ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getAgnId() {
        return agnId;
    }

    public void setAgnId(int agnId) {
        this.agnId = agnId;
    }

    @Basic
    @Column(name = "AGI_ID")
    public Integer getAgiId() {
        return agiId;
    }

    public void setAgiId(Integer agiId) {
        this.agiId = agiId;
    }

    @Basic
    @Column(name = "AGM_ID")
    public Integer getAgmId() {
        return agmId;
    }

    public void setAgmId(Integer agmId) {
        this.agmId = agmId;
    }

    @Basic
    @Column(name = "NotificationType")
    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    @Basic
    @Column(name = "Recipients", length = 2000)
    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    @Basic
    @Column(name = "Subject", length = 500)
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Basic
    @Column(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Basic
    @Column(name = "ErrorMessage", length = 2000)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Basic
    @Column(name = "SentOn")
    public Timestamp getSentOn() {
        return sentOn;
    }

    public void setSentOn(Timestamp sentOn) {
        this.sentOn = sentOn;
    }
}
