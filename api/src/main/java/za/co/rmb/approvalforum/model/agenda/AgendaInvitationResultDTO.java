package za.co.rmb.approvalforum.model.agenda;

/**
 * The outcome of one calendar invitation in a Send invitations / Send update run: the meeting's own
 * event for the committee's standing roles (agiId null) or one approved item's event.
 */
public class AgendaInvitationResultDTO {

    public static final String SENT = "Sent";
    public static final String UPDATED = "Updated";
    public static final String UNCHANGED = "Unchanged";
    public static final String SKIPPED = "Skipped";
    public static final String FAILED = "Failed";

    private Integer agiId;
    private String label;
    private String outcome;
    private String message;

    public AgendaInvitationResultDTO() {
    }

    public AgendaInvitationResultDTO(Integer agiId, String label, String outcome, String message) {
        this.agiId = agiId;
        this.label = label;
        this.outcome = outcome;
        this.message = message;
    }

    public Integer getAgiId() { return agiId; }
    public void setAgiId(Integer agiId) { this.agiId = agiId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
