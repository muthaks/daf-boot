package za.co.rmb.approvalforum.model.agenda;

/** Asking for a quorum exception (reason), or deciding one (approved, with a note; a decline needs the note). */
public class QuorumExceptionRequest {
    private String reason;
    private Boolean approved;
    private String note;
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean value) { approved = value; }
    public String getNote() { return note; }
    public void setNote(String value) { note = value; }
}
